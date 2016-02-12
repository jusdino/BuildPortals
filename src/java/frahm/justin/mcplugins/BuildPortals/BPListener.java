package java.frahm.justin.mcplugins.BuildPortals;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
//import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
//import org.bukkit.event.player.PlayerJoinEvent;

public class BPListener implements Listener{
	ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
	Main plugin;
	
	public BPListener(Main given_plugin) {
		plugin = given_plugin;
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		//With every BlockPlaceEvent, register the location, if there is
		//an 'unlinked' location stored already, pair that location with
		//0the new location as a portal-pair.
		//This is just warming up for the best type of configuration
		//management necessary for the plugin.
		
		//TODO: Filter block place events for final blocks in building
		//      a new portal.
		
		console.sendMessage("Registered BlockPlaceEvent!");
		ConfigurationSection config = plugin.getConfig();
		
		//Get relevant info about event
		console.sendMessage("Looking up relevant event details...");
		Block block = event.getBlockPlaced();
		World world = block.getWorld();
		Location loc = block.getLocation();
		Boolean unlinkedPortal = config.getBoolean("portals.0.active");
		Map<String,String> newPortal = new HashMap<>();
		if (unlinkedPortal == true) {
			console.sendMessage("Linking new portal pair...");
			Set<String> portalKeys = config.getConfigurationSection("portals").getKeys(false);
			console.sendMessage("portalKeys: " + portalKeys.toString());
			int i = 1;
			while (portalKeys.contains(Integer.toString(i))) {
				i+=1;
			}
			newPortal.put("A.world", config.getString("portals.0.world"));
			//TODO: Verify that world exists
			newPortal.put("A.x", config.getString("portals.0.x"));
			newPortal.put("A.y", config.getString("portals.0.y"));
			newPortal.put("A.z", config.getString("portals.0.z"));
			newPortal.put("B.world", world.getName());
			newPortal.put("B.x", Integer.toString(loc.getBlockX()));
			newPortal.put("B.y", Integer.toString(loc.getBlockY()));
			newPortal.put("B.z", Integer.toString(loc.getBlockZ()));
			console.sendMessage("Applying changes to portal " + Integer.toString(i) + ": " + newPortal.toString());
			config.set("portals.0.active", false);
			config.set("portals.0.world", null);
			config.set("portals.0.x", null);
			config.set("portals.0.y", null);
			config.set("portals.0.z", null);
			config.createSection("portals." + Integer.toString(i), newPortal);
			config.set("portals." + Integer.toString(i) + ".active", true);
		} else {
			console.sendMessage("Collecting unlinked portal data...");
			newPortal.put("world", loc.getWorld().getName());
			newPortal.put("x", Integer.toString(loc.getBlockX()));
			newPortal.put("y", Integer.toString(loc.getBlockY()));
			newPortal.put("z", Integer.toString(loc.getBlockZ()));
			console.sendMessage("Applying changes to portal 0: " + newPortal.toString());
			config.createSection("portals.0", newPortal);
			config.set("portals.0.active", true);
			//configPortals.set("0", true);
		}
		console.sendMessage("Saving changes...");
		plugin.saveConfig();
	}
//	@EventHandler
//	public void onPlayerJoin(PlayerJoinEvent event) {
//		Bukkit.broadcastMessage("BlankPlugin.MyListener says WELCOME!");
//		console.sendMessage("Main noticed a player join!");
//		
//		Player player = event.getPlayer();
//		List<String> awesomeList = plugin.getConfig().getStringList("awesomePlayers");
//		if (awesomeList.contains(player.getUniqueId().toString())){
//			player.sendMessage("You are awesome!");
//		} else {
//			player.sendMessage("You are NOT awesome!");
//		}
//	}
}