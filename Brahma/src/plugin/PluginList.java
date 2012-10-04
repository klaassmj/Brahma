package plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;

public class PluginList implements Runnable {
	private PluginMonitor pluginMonitor;
	public HashMap<Path, Plugin> pathToPlugin;
	public PluginLauncher launcher;

	public PluginList(PluginLauncher pluginLauncher) throws IOException {
		this.pathToPlugin = new HashMap<Path, Plugin>();
		pluginMonitor = new PluginMonitor(this, FileSystems.getDefault().getPath("plugins"), false);
		launcher = pluginLauncher;
	}
	
	@Override
	public void run() {
		// First load existing plugins if any
		try {
			Path pluginDir = FileSystems.getDefault().getPath("plugins");
			File pluginFolder = pluginDir.toFile();
			File[] files = pluginFolder.listFiles();
			if(files != null) {
				for(File f : files) {
					pluginMonitor.loadBundle(f.toPath());
				}
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		// Listen for newly added plugins
		pluginMonitor.processEvents();
	}
	
	
}
