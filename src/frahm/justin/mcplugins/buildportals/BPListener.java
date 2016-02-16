package frahm.justin.mcplugins.buildportals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
//import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
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
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class BPListener implements Listener{
	ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
	Main plugin;
	PortalHandler portals;
	FileConfiguration config;
	HashSet<Player> alreadyOnPortal = new HashSet<Player>();
	
	public BPListener(Main plugin, PortalHandler portals) {
		this.plugin = plugin;
		this.portals = portals;
		config = plugin.getConfig();
	}
	
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		Location loc = new Location(player.getWorld(), player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ());
		if (!portals.isInAPortal(loc)) {
			if (alreadyOnPortal.contains(player)) {
				console.sendMessage(player.getDisplayName() + " is out of the portal.");
				alreadyOnPortal.remove(player);
			}
			return;
		}
		if (alreadyOnPortal.contains(player)) {
			return;
		}
		Location destination = portals.getDestination(loc);
		if (null == destination){
			return;
		}
		alreadyOnPortal.add(player);
		player.teleport(destination);
	}
	
	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		console.sendMessage("Block break registered.");
		Location loc = event.getBlock().getLocation();
		if (!portals.isInAFrame(loc)) {
			console.sendMessage("Block is not in a frame.");
			return;
		}
		
		console.sendMessage("Block is in a frame!");
		String portalNumber = portals.getPortalFromFrame(loc);
		if (null == portalNumber) {
			console.sendMessage("portalNumber returned as NULL!");
			return;
		}
		loc.getWorld().strikeLightningEffect(loc);
		loc.getWorld().playEffect(loc, Effect.EXPLOSION_HUGE, 100, 5);
		console.sendMessage("Clearing portal number " + portalNumber);
		config.set("portals." + portalNumber, null);
		plugin.saveConfig();
		portals.updatePortals();
	}
	
	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		/*With every BlockPlaceEvent, register the location, if there is
		 * an 'unlinked' location stored already, pair that location with
		 *the new location as a portal-pair.
		 *This is just warming up for the best type of configuration
		 *management necessary for the plugin
		 */
		
		console.sendMessage("Block Place registered.");
		
		//Get relevant info about event
		console.sendMessage("Looking up relevant event details...");
		Block block = event.getBlockPlaced();
		if (!config.getStringList("PortalActivators").contains(block.getType().name())) {
			console.sendMessage(block.getType().name() + " placed. No action taken.");
			return;
		}
		
		World world = block.getWorld();
		console.sendMessage(block.getType().name() + " placed. Continuing tests.");
		//Get vectors to actual portal blocks from handler
		ArrayList<String> frameVecs = new ArrayList<String>();
		ArrayList<String> vectors = portals.getCompletePortalVectors(block, frameVecs);
		
		if (null == vectors) {
			console.sendMessage("This block does NOT complete a portal. No action taken.");
			return;
		}
		
		console.sendMessage("This block completes a portal. Saving location!");
		
		Boolean unlinkedPortal = config.getBoolean("portals.0." + block.getType().name() + ".active");
		Map<String, Object> newPortal = new HashMap<String, Object>();
		
		if (unlinkedPortal == true) {
			ArrayList<String> vectorsA = (ArrayList<String>) config.getStringList("portals.0." + block.getType().name() + ".vec");
			ArrayList<String> frameVecsA = (ArrayList<String>) config.getStringList("portals.0." + block.getType().name() + ".frame");
			Set<String> portalKeys = config.getConfigurationSection("portals").getKeys(false);
			console.sendMessage("portalKeys: " + portalKeys.toString());
			int i = 1;
			while (portalKeys.contains(Integer.toString(i))) {
				i+=1;
			}
			console.sendMessage("Saving new portal, number " + Integer.toString(i));
			newPortal.put("A.world", config.getString("portals.0." + block.getType().name() + ".world"));
			newPortal.put("A.vec", vectorsA);
			newPortal.put("A.frame", frameVecsA);
			newPortal.put("B.world", world.getName());
			newPortal.put("B.vec", vectors);
			newPortal.put("B.frame", frameVecs);
			console.sendMessage("Applying changes to portal " + Integer.toString(i) + ": " + newPortal.toString());
			config.set("portals.0." + block.getType().name() + ".active", false);
			config.set("portals.0." + block.getType().name() + ".world", null);
			config.set("portals.0." + block.getType().name() + ".vec", null);
			config.set("portals.0." + block.getType().name() + ".frame", null);
			config.createSection("portals." + Integer.toString(i), newPortal);
			config.set("portals." + Integer.toString(i) + ".active", true);
			
			//Convert portal interiors to air
			Location portalLoc = null;
			Iterator<String> locIter = vectors.iterator();
			while (locIter.hasNext()) {
				String[] locStr = locIter.next().split(",");
				console.sendMessage("Portal B Interior block: " + locStr[0] + ", " + locStr[1] + ", " + locStr[2]);
				portalLoc = new Location(block.getWorld(), Double.parseDouble(locStr[0]), Double.parseDouble(locStr[1]), Double.parseDouble(locStr[2]));
				portalLoc.getBlock().setType(Material.AIR);
			}

			Location particleLoc;
			int spread;
			int count;
			int range;
			Random rand = new Random();
			if (null != portalLoc) {
				console.sendMessage("Generating particle effect at portal B.");
				portalLoc.getWorld().strikeLightningEffect(portalLoc);
				spread = vectors.size();
				count = vectors.size()*200;
				if (count > 1000) {
					count = 1000;
				}
				range = vectors.size() * 10;
				
				for (int j=0; j<count; j++) {
					particleLoc = new Location(portalLoc.getWorld(), portalLoc.getX() + (rand.nextDouble() * spread), portalLoc.getY() + (rand.nextDouble() * spread), portalLoc.getZ() + (rand.nextDouble() * spread));
					portalLoc.getWorld().playEffect(particleLoc, Effect.MAGIC_CRIT, range);
				}
			}
			locIter = vectorsA.iterator();
			while (locIter.hasNext()) {
				String[] locStr = locIter.next().split(",");
				console.sendMessage("Portal A Interior block: " + locStr[0] + ", " + locStr[1] + ", " + locStr[2]);
				portalLoc = new Location(Bukkit.getWorld((String) newPortal.get("A.world")), Double.parseDouble(locStr[0]), Double.parseDouble(locStr[1]), Double.parseDouble(locStr[2]));
				portalLoc.getBlock().setType(Material.AIR);
			}
			if (null != portalLoc) {
				console.sendMessage("Generating particle effect at portal A.");
				portalLoc.getWorld().strikeLightningEffect(portalLoc);
				spread = vectorsA.size();
				count = vectorsA.size()*200;
				if (count > 1000) {
					count = 1000;
				}
				range = vectors.size()*10;
				
				for (int j=0; j<count; j++) {
					particleLoc = new Location(portalLoc.getWorld(), portalLoc.getX() + (rand.nextDouble() * spread), portalLoc.getY() + (rand.nextDouble() * spread), portalLoc.getZ() + (rand.nextDouble() * spread));
					portalLoc.getWorld().playEffect(particleLoc, Effect.MAGIC_CRIT, range);
				}
			}
		} else {
			//Save unlinked portal location
			console.sendMessage("Collecting unlinked portal data...");
			newPortal.put("world", block.getWorld().getName());
			newPortal.put("vec", vectors);
			newPortal.put("frame", frameVecs);
			console.sendMessage("Applying changes to portal 0: " + newPortal.toString());
			config.createSection("portals.0." + block.getType().name(), newPortal);
			config.set("portals.0." + block.getType().name() + ".active", true);
			Location particleLoc;
			int spread;
			int count;
			int range;
			Random rand = new Random();
			if (null != block) {
				spread = vectors.size();
				count = vectors.size()*100;
				if (count > 500) {
					count = 500;
				}
				range = vectors.size() * 10;
				
				for (int j=0; j<count; j++) {
					particleLoc = new Location(block.getWorld(), block.getX() + (rand.nextDouble() * spread), block.getY() + (rand.nextDouble() * spread), block.getZ() + (rand.nextDouble() * spread));
					block.getWorld().playEffect(particleLoc, Effect.MAGIC_CRIT, range);
				}
			}
		}
		console.sendMessage("Saving changes...");
		plugin.saveConfig();
		portals.updatePortals();
	}
}