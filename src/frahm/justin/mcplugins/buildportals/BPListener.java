package frahm.justin.mcplugins.buildportals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class BPListener implements Listener{
	Logger logger;
	
	Main plugin;
	PortalHandler portals;
	FileConfiguration config;
	HashSet<Player> alreadyOnPortal = new HashSet<Player>();
	
	public BPListener(Main plugin, PortalHandler portals) {
		this.plugin = plugin;
		this.portals = portals;
		this.logger = this.plugin.getLogger();
		config = plugin.getConfig();
	}
	
	@EventHandler (ignoreCancelled = true)
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		
		Location loc = new Location(player.getWorld(), player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ());
		logger.info(player.getName() + " moved: " + loc.toVector().toString());
		//Players in a minecart are listed as 1m below actual, so
		//add 1 if in a minecart.
		if (player.getVehicle() instanceof Minecart) {
			logger.info(player.getName() + " is in a minecart, adding 1m to Y.");
			loc.add(0, 1, 0);
		}
		if (!portals.isInAPortal(loc)) {
			if (alreadyOnPortal.contains(player)) {
//				logger.info(player.getDisplayName() + " is out of the portal.");
				alreadyOnPortal.remove(player);
			}
			return;
		}
		logger.info(player.getName() + " is in a portal.");
		if (alreadyOnPortal.contains(player)) {
			logger.info(player.getName() + " hasn't left yet. Ignoring.");
			return;
		}
		Location destination = portals.getDestination(player, loc);
		if (null == destination){
			logger.info("Can't get a destination for " + player.getName() + "!");
			return;
		}
		alreadyOnPortal.add(player);
		
		Vehicle vehicle = (Vehicle) player.getVehicle();
		if (vehicle == null) {
			logger.info("Teleporting " + player.getName());
			player.teleport(destination);
		} else {
			logger.info("Teleporting " + player.getName() + " with a vehicle.");
			vehicle.eject();
			player.teleport(destination);
			vehicle.teleport(player.getLocation());
			vehicle.setPassenger(player);
		}
	}
	
	@EventHandler (ignoreCancelled = true)
	public void onBlockPhysics(BlockPhysicsEvent event) {
//		logger.info("Block physics registered.");
		String frameMaterialName = config.getString("PortalMaterial");
		if (event.getChangedType().name() != Material.getMaterial(frameMaterialName).name()) {
//			logger.info("Block is " + event.getChangedType().name() + " not " + frameMaterialName);
			return;
		}
		
		//Check all portals for broken frames
		Location loc = event.getBlock().getLocation();
		String brokenPortal = portals.integrityCheck(loc);
		if (null == brokenPortal || null == loc) {
			return;
		}
		
		loc.getWorld().strikeLightningEffect(loc);
		loc.getWorld().playEffect(loc, Effect.EXPLOSION_HUGE, 100, 5);
		logger.info("Clearing portal number " + brokenPortal);
		config.set("portals." + brokenPortal, null);
		plugin.saveConfig();
		portals.updatePortals();
	}
	
	@EventHandler (ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		/*With every BlockPlaceEvent, register the location, if there is
		 * an 'unlinked' location stored already, pair that location with
		 *the new location as a portal-pair.
		 *This is just warming up for the best type of configuration
		 *management necessary for the plugin
		 */
		
//		logger.info("Block Place registered.");
		
		//Get relevant info about event
//		logger.info("Looking up relevant event details...");
		Block block = event.getBlockPlaced();
		if (!config.getStringList("PortalActivators").contains(block.getType().name())) {
//			logger.info(block.getType().name() + " placed. No action taken.");
			return;
		}
		
		World world = block.getWorld();
//		logger.info(block.getType().name() + " placed. Continuing tests.");
		//Get vectors to actual portal blocks from handler
		ArrayList<String> frameVecs = new ArrayList<String>();
		ArrayList<String> vectors = new ArrayList<String>();
		Float yaw = portals.getCompletePortalVectors(block, frameVecs, vectors);
		
		if (null == yaw) {
//			logger.info("This block does NOT complete a portal. No action taken.");
			return;
		}
		
//		logger.info("This block completes a portal. Saving location!");
		
		Boolean unlinkedPortal = config.getBoolean("portals.0." + block.getType().name() + ".active");
		Map<String, Object> newPortal = new HashMap<String, Object>();
		
		if (unlinkedPortal == true) {
			ArrayList<String> vectorsA = (ArrayList<String>) config.getStringList("portals.0." + block.getType().name() + ".vec");
			ArrayList<String> frameVecsA = (ArrayList<String>) config.getStringList("portals.0." + block.getType().name() + ".frame");
			Set<String> portalKeys = config.getConfigurationSection("portals").getKeys(false);
//			logger.info("portalKeys: " + portalKeys.toString());
			int i = 1;
			while (portalKeys.contains(Integer.toString(i))) {
				i+=1;
			}
			logger.info("Saving new portal, number " + Integer.toString(i));
			newPortal.put("A.world", config.getString("portals.0." + block.getType().name() + ".world"));
			newPortal.put("A.vec", vectorsA);
			newPortal.put("A.frame", frameVecsA);
			newPortal.put("A.yaw", config.getString("portals.0." + block.getType().name() + ".yaw"));
			newPortal.put("B.world", world.getName());
			newPortal.put("B.vec", vectors);
			newPortal.put("B.frame", frameVecs);
			newPortal.put("B.yaw", yaw.toString());
//			logger.info("Applying changes to portal " + Integer.toString(i)); // + ": " + newPortal.toString());
			config.set("portals.0." + block.getType().name() + ".active", false);
			config.set("portals.0." + block.getType().name() + ".world", null);
			config.set("portals.0." + block.getType().name() + ".vec", null);
			config.set("portals.0." + block.getType().name() + ".frame", null);
			config.set("portals.0." + block.getType().name() + ".yaw", null);
			config.createSection("portals." + Integer.toString(i), newPortal);
			config.set("portals." + Integer.toString(i) + ".active", true);
			
			//Convert portal interiors to air
			Location portalLoc = null;
			Iterator<String> locIter = vectors.iterator();
			while (locIter.hasNext()) {
				String[] locStr = locIter.next().split(",");
//				logger.info("Portal B Interior block: " + locStr[0] + ", " + locStr[1] + ", " + locStr[2]);
				portalLoc = new Location(block.getWorld(), Double.parseDouble(locStr[0]), Double.parseDouble(locStr[1]), Double.parseDouble(locStr[2]));
				portalLoc.getBlock().setType(Material.AIR);
			}

			Location particleLoc;
			int spread;
			int count;
			int range;
			Random rand = new Random();
			if (null != portalLoc) {
//				logger.info("Generating particle effect at portal B.");
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
//				logger.info("Portal A Interior block: " + locStr[0] + ", " + locStr[1] + ", " + locStr[2]);
				portalLoc = new Location(Bukkit.getWorld((String) newPortal.get("A.world")), Double.parseDouble(locStr[0]), Double.parseDouble(locStr[1]), Double.parseDouble(locStr[2]));
				portalLoc.getBlock().setType(Material.AIR);
			}
			if (null != portalLoc) {
//				logger.info("Generating particle effect at portal A.");
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
			logger.info("Collecting unlinked portal data...");
			newPortal.put("world", block.getWorld().getName());
			newPortal.put("vec", vectors);
			newPortal.put("frame", frameVecs);
			newPortal.put("yaw", yaw.toString());
//			logger.info("Applying changes to portal 0: " + newPortal.toString());
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
		logger.info("Saving changes...");
		plugin.saveConfig();
		portals.updatePortals();
	}
}