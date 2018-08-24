package frahm.justin.mcplugins.buildportals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class BPListener implements Listener{
	private static Logger logger;
	private static Level DEBUG_LEVEL;
	private static Main plugin;
	private static PortalHandler portals;
	private static Teleporter teleporter;
	private static FileConfiguration config;
	private static HashSet<Entity> alreadyOnPortal = new HashSet<>();
	
	public BPListener(Main plugin, PortalHandler portals) {
		BPListener.plugin = plugin;
		BPListener.portals = portals;
		BPListener.logger = plugin.getLogger();
		BPListener.DEBUG_LEVEL = Level.INFO;
		BPListener.teleporter = new Teleporter();
		BPListener.config = plugin.getConfig();
	}

	@EventHandler (ignoreCancelled = true)
	public void onVehicleMove(VehicleMoveEvent event) {
		Vehicle vehicle = event.getVehicle();
		Entity player = vehicle.getPassenger();
		Location loc = event.getFrom();
		loc = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
		if (!portals.isInAPortal(loc)) {
			if (alreadyOnPortal.contains(vehicle) && loc.getChunk().isLoaded()) {
				alreadyOnPortal.remove(vehicle);
				alreadyOnPortal.remove(player);
			}
			return;
		}
		if (alreadyOnPortal.contains(vehicle)) {
			return;
		}
		Location destination = portals.getDestination(vehicle, loc);
		if (null == destination){
			return;
		}
		Entity entity = teleporter.teleport(vehicle, destination);
		if (entity != null) {
			alreadyOnPortal.add(entity);
			alreadyOnPortal.add(player);
		}
	}
	
	@EventHandler (ignoreCancelled = true)
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		Entity vehicle = player.getVehicle();
		if (vehicle != null) {
			return;
		}	
		Location loc = new Location(player.getWorld(), player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ());
		if (!portals.isInAPortal(loc)) {
			alreadyOnPortal.remove(player);
			return;
		}
		if (alreadyOnPortal.contains(player)) {
			//Don't let the player move if their chunk isn't loaded.
			Chunk chunk = loc.getChunk();
			if (!chunk.isLoaded()) {
				event.setCancelled(true);
				return;
			}
			return;
		}
		Location destination = portals.getDestination(player, loc);
		if (null == destination){
			logger.info("Can't get a destination for " + player.getName() + "!");
			return;
		}
		if (teleporter.teleport((Entity)player, destination) == null) {
			return;
		}
		alreadyOnPortal.add(player);
	}

	@EventHandler (priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		onBlockEvent(event.getBlock().getType().name());
	}

	@EventHandler (priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockPhysics(BlockPhysicsEvent event) {
		onBlockEvent(event.getChangedType().name());
	}

	private void onBlockEvent(String name) {
		String frameMaterialName = config.getString("PortalMaterial");
		ArrayList<String> activatorMaterialNames = (ArrayList<String>) config.getStringList("PortalActivators");
		if (! (name.equals(frameMaterialName) || activatorMaterialNames.contains(name))) {
			return;
		}

		logger.log(DEBUG_LEVEL, "onBlockEvent event affecting portal / activator materials");
		new BukkitRunnable() {
			@Override
			public void run() {
				PortalHandler.checkAndUpdate();
			}
		}.runTaskLater(BPListener.plugin, 1);
	}
	
	@EventHandler (ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) throws InvalidConfigurationException {
		/*With every BlockPlaceEvent, register the location, if there is
		 * an 'unlinked' location stored already, pair that location with
		 *the new location as a portal-pair.
		 *This is just warming up for the best type of configuration
		 *management necessary for the plugin
		 */
		logger.log(DEBUG_LEVEL, "Block place event registered");

		//Get relevant info about event
		Block block = event.getBlockPlaced();
		if (!config.getStringList("PortalActivators").contains(block.getType().name())) {
			return;
		}

		logger.log(DEBUG_LEVEL,"Block is a portal activator");
		World world = block.getWorld();
		//Get vectors to actual portal blocks from handler
		ArrayList<String> frameVecs = new ArrayList<>();
		ArrayList<String> activatorVecs = new ArrayList<>();
		ArrayList<String> vectors = new ArrayList<>();
		Float yaw = portals.getCompletePortalVectors(block, frameVecs, activatorVecs, vectors);
		
		if (null == yaw) {
			return;
		}
		logger.log(DEBUG_LEVEL,"Block completes a portal");
		
		Player player;
		player = event.getPlayer();
		if (!player.hasPermission("buildportals.activate")) {
			player.sendMessage("You do not have permission to activate portals!");
			return;
		}
		logger.log(DEBUG_LEVEL,"Player " + player.getDisplayName() + " has appropriate permissions");
		
		boolean unlinkedPortal = config.getBoolean("portals.0." + block.getType().name() + ".active");
		Map<String, Object> newPortal = new HashMap<>();
		logger.log(DEBUG_LEVEL,"This is an unlinked portal");
		
		if (unlinkedPortal) {
			ArrayList<String> vectorsA = (ArrayList<String>) config.getStringList("portals.0." + block.getType().name() + ".vec");
			ArrayList<String> frameVecsA = (ArrayList<String>) config.getStringList("portals.0." + block.getType().name() + ".frame");
			Set<String> portalKeys = config.getConfigurationSection("portals").getKeys(false);
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
			config.set("portals.0." + block.getType().name() + ".active", false);
			config.set("portals.0." + block.getType().name() + ".world", null);
			config.set("portals.0." + block.getType().name() + ".vec", null);
			config.set("portals.0." + block.getType().name() + ".frame", null);
			config.set("portals.0." + block.getType().name() + ".activators", null);
			config.set("portals.0." + block.getType().name() + ".yaw", null);
			config.createSection("portals." + Integer.toString(i), newPortal);
			config.set("portals." + Integer.toString(i) + ".active", true);
			
			//Convert portal interiors to air
			Location portalLoc = null;
			Iterator<String> locIter = vectors.iterator();
			while (locIter.hasNext()) {
				String[] locStr = locIter.next().split(",");
				portalLoc = new Location(block.getWorld(), Double.parseDouble(locStr[0]), Double.parseDouble(locStr[1]), Double.parseDouble(locStr[2]));
				portalLoc.getBlock().setType(Material.AIR);
			}

			if (null != portalLoc) {
				portalLoc.getWorld().strikeLightningEffect(portalLoc);
			}
			locIter = vectorsA.iterator();
			while (locIter.hasNext()) {
				String[] locStr = locIter.next().split(",");
				portalLoc = new Location(Bukkit.getWorld((String) newPortal.get("A.world")), Double.parseDouble(locStr[0]), Double.parseDouble(locStr[1]), Double.parseDouble(locStr[2]));
				portalLoc.getBlock().setType(Material.AIR);
			}
			if (null != portalLoc) {
				portalLoc.getWorld().strikeLightningEffect(portalLoc);
			}
		} else {
			//Save unlinked portal location
			logger.info("Collecting unlinked portal data...");
			newPortal.put("world", block.getWorld().getName());
			newPortal.put("vec", vectors);
			newPortal.put("frame", frameVecs);
			newPortal.put("activators", activatorVecs);
			newPortal.put("yaw", yaw.toString());
			config.createSection("portals.0." + block.getType().name(), newPortal);
			config.set("portals.0." + block.getType().name() + ".active", true);
			
			//Make a visible particle effect
			Location particleLoc;
			int spread;
			int count;
			Random rand = new Random();
			spread = vectors.size();
			count = vectors.size()*100;
			if (count > 500) {
				count = 500;
			}

			for (int j=0; j<count; j++) {
				particleLoc = new Location(block.getWorld(), block.getX() + (rand.nextDouble() * spread), block.getY() + (rand.nextDouble() * spread), block.getZ() + (rand.nextDouble() * spread));
				block.getWorld().spawnParticle(Particle.CRIT_MAGIC, particleLoc, 1);
			}
		}
		logger.info("Saving changes...");
		plugin.saveConfig();
		portals.updatePortals();
	}
}