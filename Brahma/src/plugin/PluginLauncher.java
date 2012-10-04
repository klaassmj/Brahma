package plugin;

import java.util.HashMap;

public class PluginLauncher {
	public HashMap<String, Plugin> idToPlugin;
	public Plugin currentPlugin;
	private BrahmaView view;
	
	private PluginList list;
	
	public PluginLauncher(){
		view = new BrahmaView(this);
		idToPlugin = new HashMap<String, Plugin>();
		
		try {
			this.list = new PluginList(this);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		view.start();
		Thread thread = new Thread(this.list);
		thread.start();
		
		
	}
	
	public void addPlugin(Plugin plugin) {
		this.idToPlugin.put(plugin.getId(), plugin);
		view.addPluginToList(plugin.getId());
		view.setBottomLabelText("The " + plugin.getId() + " plugin has been recently added!");
	}
	
	public void removePlugin(String id) {
		Plugin plugin = this.idToPlugin.remove(id);
		view.removePluginFromList(id);
		
		// Stop the plugin if it is still running
		plugin.stop();

		view.setBottomLabelText("The " + plugin.getId() + " plugin has been recently removed!");
	}

}
