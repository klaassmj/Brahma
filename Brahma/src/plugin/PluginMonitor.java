package plugin;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static java.nio.file.StandardWatchEventKinds.*;
import java.nio.file.attribute.*;
import static java.nio.file.LinkOption.*;

public class PluginMonitor {
	private final WatchService watcher;
    private final Map<WatchKey,Path> keys;
    private final boolean recursive;
    private boolean trace = false;
    
    // C.R. Change
    private PluginList list;

    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>)event;
    }

    /**
     * Register the given directory with the WatchService
     */
    private void register(Path dir) throws IOException {
        WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        if (trace) {
            Path prev = keys.get(key);
            if (prev == null) {
                System.out.format("register: %s\n", dir);
            } else {
                if (!dir.equals(prev)) {
                    System.out.format("update: %s -> %s\n", prev, dir);
                }
            }
        }
        keys.put(key, dir);
    }

    /**
     * Register the given directory, and all its sub-directories, with the
     * WatchService.
     */
    private void registerAll(final Path start) throws IOException {
        // register directory and sub-directories
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                throws IOException
            {
                register(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Creates a WatchService and registers the given directory
     */
    PluginMonitor(PluginList list, Path dir, boolean recursive) throws IOException {
    	this.list = list;
        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<WatchKey,Path>();
        this.recursive = recursive;

        if (recursive) {
            System.out.format("Scanning %s ...\n", dir);
            registerAll(dir);
            System.out.println("Done.");
        } else {
            register(dir);
        }

        // enable trace after initial registration
        this.trace = true;
    }

    /**
     * Process all events for keys queued to the watcher
     */
    void processEvents() {
        for (;;) {

            // wait for key to be signalled
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException x) {
                return;
            }

            Path dir = keys.get(key);
            if (dir == null) {
                System.err.println("WatchKey not recognized!!");
                continue;
            }

            for (WatchEvent<?> event: key.pollEvents()) {
                WatchEvent.Kind kind = event.kind();

                // TBD - provide example of how OVERFLOW event is handled
                if (kind == OVERFLOW) {
                    continue;
                }

                // Context for directory entry event is the file name of entry
                WatchEvent<Path> ev = cast(event);
                Path name = ev.context();
                Path child = dir.resolve(name);

                // print out event
                System.out.format("%s: %s\n", event.kind().name(), child);
                
                // C.R. Changes
            	if(this.list != null) {
            		try {
                        if(kind == ENTRY_CREATE) {
                        	loadBundle(child);
                        }
                        else if(kind == ENTRY_DELETE) {
                        	Plugin plugin = this.list.pathToPlugin.remove(child);
                        }
            		}
            		catch(Exception e) {
            			e.printStackTrace();
            		}
            	}

                // if directory is created, and watching recursively, then
                // register it and its sub-directories
                if (recursive && (kind == ENTRY_CREATE)) {
                    try {
                        if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                            registerAll(child);
                        }
                    } catch (IOException x) {
                        // ignore to keep sample readbale
                    }
                }
            }

            // reset key and remove from set if directory no longer accessible
            boolean valid = key.reset();
            if (!valid) {
                keys.remove(key);

                // all directories are inaccessible
                if (keys.isEmpty()) {
                    break;
                }
            }
        }
    }

	public void loadBundle(Path child) throws IOException,
			MalformedURLException, ClassNotFoundException,
			InstantiationException, IllegalAccessException {
		// Get hold of the jar file
		File jarBundle = child.toFile();
		JarFile jarFile = new JarFile(jarBundle);
		
		// Get the manifest file in the jar file
		Manifest mf = jarFile.getManifest();
		Attributes mainAttribs = mf.getMainAttributes();
		
		// Get hold of the Plugin-Class attribute and load the class
		String className = mainAttribs.getValue("Plugin-Class");
		URL[] urls = new URL[]{child.toUri().toURL()};
		ClassLoader classLoader = new URLClassLoader(urls);
		Class<?> pluginClass = classLoader.loadClass(className);
		
		// Create a new instance of the plugin class and add to the core
		Plugin plugin = (Plugin)pluginClass.newInstance();
		this.list.pathToPlugin.put(child, plugin);
		this.list.launcher.addPlugin(plugin);
		
		jarFile.close();
	}
    
    static void usage() {
        System.err.println("usage: java PluginMonitor [-r] dir");
        System.exit(-1);
    }

    public static void main(String[] args) throws IOException {
        // parse arguments
        if (args.length == 0 || args.length > 2)
            usage();
        boolean recursive = false;
        int dirArg = 0;
        if (args[0].equals("-r")) {
            if (args.length < 2)
                usage();
            recursive = true;
            dirArg++;
        }

        // register directory and process its events
        Path dir = Paths.get(args[dirArg]);
        new PluginMonitor(null, dir, recursive).processEvents(); // C.R. Change - Added null parameter
    }
}
