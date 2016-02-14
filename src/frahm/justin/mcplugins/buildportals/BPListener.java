package frahm.justin.mcplugins.buildportals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
//import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
//import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class BPListener implements Listener{
	ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
	Main plugin;
	PortalHandler portals;
	FileConfiguration config;
	
	public BPListener(Main plugin, PortalHandler portals) {
		this.plugin = plugin;
		this.portals = portals;
		config = plugin.getConfig();
	}
	
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		//TODO: test if player is in a portal, get destination, teleport
		Location loc = new Location(player.getWorld(), player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ());
		if (!portals.isInAPortal(loc)) {
			return;
		}
		Location destination = portals.getDestination(loc);
		if (null == destination){
			return;
		}
		player.teleport(destination);
	}
	
	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		/*With every BlockPlaceEvent, register the location, if there is
		 * an 'unlinked' location stored already, pair that location with
		 *the new location as a portal-pair.
		 *This is just warming up for the best type of configuration
		 *management necessary for the plugin
		 */
		
		console.sendMessage("Registered BlockPlaceEvent!");
		
		//Get relevant info about event
		console.sendMessage("Looking up relevant event details...");
		Block block = event.getBlockPlaced();
		if (!config.getStringList("PortalActivators").contains(block.getType().name())) {
			console.sendMessage(block.getType().name() + " placed. No action taken.");
			return;
		}
		
		World world = block.getWorld();
		console.sendMessage(block.getType().name() + " placed. Continuing tests.");
		if (!portals.isCompletePortal(block)) {
			console.sendMessage("This block does NOT complete a portal. No action taken.");
			return;
		}
		
		console.sendMessage("This block completes a portal. Saving location!");
		//Floored block location
		Location loc = block.getLocation();
		loc.setX(loc.getBlockX());
		loc.setY(loc.getBlockY());
		loc.setZ(loc.getBlockZ());
		
		Boolean unlinkedPortal = config.getBoolean("portals.0.active");
		Map<String, Object> newPortal = new HashMap<String, Object>();
		
		//Get vectors to actual portal blocks from handler
		ArrayList<String> vectors = portals.getPortalVectors(block);
		
		if (unlinkedPortal == true) {
			ArrayList<String> vectorsA = (ArrayList<String>) config.getStringList("portals.0.vec");
			console.sendMessage("Linking new portal pair...");
			Set<String> portalKeys = config.getConfigurationSection("portals").getKeys(false);
			console.sendMessage("portalKeys: " + portalKeys.toString());
			int i = 1;
			while (portalKeys.contains(Integer.toString(i))) {
				i+=1;
			}
			newPortal.put("A.world", config.getString("portals.0.world"));
			//TODO: Verify that world still exists
			newPortal.put("A.vec", vectorsA);
			newPortal.put("B.world", world.getName());
			newPortal.put("B.vec", vectors);
			console.sendMessage("Applying changes to portal " + Integer.toString(i) + ": " + newPortal.toString());
			config.set("portals.0.active", false);
			config.set("portals.0.world", null);
			config.set("portals.0.vec", null);
			config.createSection("portals." + Integer.toString(i), newPortal);
			config.set("portals." + Integer.toString(i) + ".active", true);
			
			portals.updatePortals();
			
			//Convert portal interiors to air
			Location portalLoc;
			Iterator<String> locIter = vectors.iterator();
			while (locIter.hasNext()) {
				String[] locStr = locIter.next().split(",");
				console.sendMessage("Portal B Interior block: " + locStr[0] + ", " + locStr[1] + ", " + locStr[2]);
				portalLoc = new Location(block.getWorld(), Double.parseDouble(locStr[0]), Double.parseDouble(locStr[1]), Double.parseDouble(locStr[2]));
				portalLoc.getBlock().setType(Material.AIR);
			}
			locIter = vectorsA.iterator();
			while (locIter.hasNext()) {
				String[] locStr = locIter.next().split(",");
				console.sendMessage("Portal A Interior block: " + locStr[0] + ", " + locStr[1] + ", " + locStr[2]);
				portalLoc = new Location(Bukkit.getWorld((String) newPortal.get("A.world")), Double.parseDouble(locStr[0]), Double.parseDouble(locStr[1]), Double.parseDouble(locStr[2]));
				portalLoc.getBlock().setType(Material.AIR);
			}
		} else {
			//Save unlinked portal location
			console.sendMessage("Collecting unlinked portal data...");
			newPortal.put("world", loc.getWorld().getName());
			newPortal.put("vec", vectors);
			console.sendMessage("Applying changes to portal 0: " + newPortal.toString());
			config.createSection("portals.0", newPortal);
			config.set("portals.0.active", true);
		}
		console.sendMessage("Saving changes...");
		plugin.saveConfig();
	}
}