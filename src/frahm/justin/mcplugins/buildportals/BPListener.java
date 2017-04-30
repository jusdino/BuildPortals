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
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;

public class BPListener implements Listener{
	Logger logger;
	Main plugin;
	PortalHandler portals;
	Teleporter teleporter;
	FileConfiguration config;
	HashSet<Player> alreadyOnPortal = new HashSet<Player>();
	HashMap<Player, Vehicle> teleportedVehicle = new HashMap<Player, Vehicle>();
	
	public BPListener(Main plugin, PortalHandler portals) {
		this.plugin = plugin;
		this.portals = portals;
		this.logger = this.plugin.getLogger();
		teleporter = new Teleporter();
		config = plugin.getConfig();
	}

	@EventHandler (ignoreCancelled = true)
	public void onVehicleMove(VehicleMoveEvent event) {
		Entity passenger = event.getVehicle().getPassengers().get(0);
		if (!(passenger instanceof Player)) {
			return;
		}
		Player player = (Player) event.getVehicle().getPassengers().get(0);
		
		Location loc = new Location(player.getWorld(), player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ());
		//Players in a minecart are listed as 1m below actual, so
		//add 1 if in a minecart.
		if (player.getVehicle() instanceof Minecart) {
			loc.add(0, 1, 0);
		}
		if (!portals.isInAPortal(loc)) {
			if (alreadyOnPortal.contains(player)) {
				alreadyOnPortal.remove(player);
			}
			return;
		}
		Location destination = portals.getDestination(player, loc);
		if (null == destination){
			return;
		}
		alreadyOnPortal.add(player);
		teleporter.teleport(player, destination);
		return;
	}
	
	@EventHandler (ignoreCancelled = true)
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		
		Location loc = new Location(player.getWorld(), player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ());
		//Players in a minecart are listed as 1m below actual, so
		//add 1 if in a minecart.
		if (player.getVehicle() instanceof Minecart) {
			loc.add(0, 1, 0);
		}
		if (!portals.isInAPortal(loc)) {
			if (alreadyOnPortal.contains(player)) {
				alreadyOnPortal.remove(player);
			}
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
		alreadyOnPortal.add(player);
		teleporter.teleport(player, destination);
		return;
	}
	
	@EventHandler (ignoreCancelled = true)
	public void onBlockPhysics(BlockPhysicsEvent event) {
		String frameMaterialName = config.getString("PortalMaterial");
		ArrayList<String> activatorMaterialNames = (ArrayList<String>) config.getStringList("PortalActivators");
		if (event.getChangedType().name() != Material.getMaterial(frameMaterialName).name()) {
			if (!activatorMaterialNames.contains(event.getChangedType().name())) {
				return;
			}
		}
		
		//Check all portals for broken frames
		Location loc = event.getBlock().getLocation();
		String brokenPortal = portals.integrityCheck(loc);
		if (null == brokenPortal || null == loc) {
			return;
		}
		
		loc.getWorld().strikeLightningEffect(loc);
		loc.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, loc, 1);
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
		
		//Get relevant info about event
		Block block = event.getBlockPlaced();
		if (!config.getStringList("PortalActivators").contains(block.getType().name())) {
			return;
		}
		
		World world = block.getWorld();
		//Get vectors to actual portal blocks from handler
		ArrayList<String> frameVecs = new ArrayList<String>();
		ArrayList<String> activatorVecs = new ArrayList<String>();
		ArrayList<String> vectors = new ArrayList<String>();
		Float yaw = portals.getCompletePortalVectors(block, frameVecs, activatorVecs, vectors);
		
		if (null == yaw) {
			return;
		}
		
		Boolean unlinkedPortal = config.getBoolean("portals.0." + block.getType().name() + ".active");
		Map<String, Object> newPortal = new HashMap<String, Object>();
		
		if (unlinkedPortal == true) {
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
			if (null != block) {
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
		}
		logger.info("Saving changes...");
		plugin.saveConfig();
		portals.updatePortals();
	}
}