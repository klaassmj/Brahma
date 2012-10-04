package plugin;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.util.HashMap;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class BrahmaView {
	// GUI Widgets that we will need
		private JFrame frame;
		private JPanel contentPane;
		private JLabel bottomLabel;
		private JList sideList;
		private DefaultListModel<String> listModel;
		private JPanel centerEnvelope;
		
		private final PluginLauncher launcher;
		
		public BrahmaView(PluginLauncher parentLauncher) {
			launcher = parentLauncher;
			
			// Lets create the elements that we will need
			frame = new JFrame("Pluggable Board Application");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			
			contentPane = (JPanel)frame.getContentPane();
			contentPane.setPreferredSize(new Dimension(700, 500));
			bottomLabel = new JLabel("No plugins registered yet!");
			
			listModel = new DefaultListModel<String>();
			sideList = new JList(listModel);
			sideList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			sideList.setLayoutOrientation(JList.VERTICAL);
			JScrollPane scrollPane = new JScrollPane(sideList);
			scrollPane.setPreferredSize(new Dimension(100, 50));
			
			// Create center display area
			centerEnvelope = new JPanel(new BorderLayout());
			centerEnvelope.setBorder(BorderFactory.createLineBorder(Color.black, 5));
			
			// Lets lay them out, contentPane by default has BorderLayout as its layout manager
			contentPane.add(centerEnvelope, BorderLayout.CENTER);
			contentPane.add(scrollPane, BorderLayout.EAST);
			contentPane.add(bottomLabel, BorderLayout.SOUTH);
			
			// Add action listeners
			sideList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
				@Override
				public void valueChanged(ListSelectionEvent e) {
					// If the list is still updating, return
					if(e.getValueIsAdjusting())
						return;
					
					// List has finalized selection, let's process further
					int index = sideList.getSelectedIndex();
					String id = listModel.elementAt(index);
					Plugin plugin = launcher.idToPlugin.get(id);
					
					if(plugin == null || plugin.equals(launcher.currentPlugin))
						return;
					
					// Stop previously running plugin
					if(launcher.currentPlugin != null)
						launcher.currentPlugin.stop();
					
					// The newly selected plugin is our current plugin
					launcher.currentPlugin = plugin;
					
					// Clear previous working area
					centerEnvelope.removeAll();
					
					// Create new working area
					JPanel centerPanel = new JPanel();
					centerEnvelope.add(centerPanel, BorderLayout.CENTER); 
					
					// Ask plugin to layout the working area
					launcher.currentPlugin.layout(centerPanel);
					contentPane.revalidate();
					contentPane.repaint();
					
					// Start the plugin
					launcher.currentPlugin.start();
					
					bottomLabel.setText("The " + launcher.currentPlugin.getId() + " is running!");
				}
			});
		}
		
		public void start() {
			EventQueue.invokeLater(new Runnable() {
				public void run()
				{
					frame.pack();
					frame.setVisible(true);
				}
			});
		}
		
		public void stop() {
			EventQueue.invokeLater(new Runnable() {
				public void run()
				{
					frame.setVisible(false);
				}
			});
		}
		
		public void setBottomLabelText(String text){
			this.bottomLabel.setText(text);
		}
		
		public void addPluginToList(String id){
			listModel.addElement(id);
		}
		
		public void removePluginFromList(String id){
			listModel.removeElement(id);
		}
}
