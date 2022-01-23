package space.frahm.buildportals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;


public class PortalListener implements Listener {
    private static HashSet<Entity> alreadyOnPortal = new HashSet<>();

    PortalListener() {}

    @EventHandler(ignoreCancelled = true)
    public void onVehicleMove(VehicleMoveEvent event) {
        Vehicle vehicle = event.getVehicle();
        BuildPortals.logger.log(BuildPortals.logLevel, "Vehicle move: " + vehicle.toString());
        vehicleMove(vehicle);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Entity vehicle = player.getVehicle();
        BuildPortals.logger.log(BuildPortals.logLevel, "Player move: " + player.getDisplayName());
        if (vehicle != null) {
            if (vehicle instanceof AbstractHorse || vehicle instanceof Pig) {
                // For Horses or Pigs, a player can fire a PlayerMoveEvent, but we
                // want to defer to the vehicle move logic.
                BuildPortals.logger.log(BuildPortals.logLevel, "On horse: " + player.getDisplayName());
                vehicleMove((Vehicle)vehicle);
            }
            return;
        }
        Location loc = player.getLocation();
        Portal portal = Portal.isInAPortal(player.getLocation());
        if (portal == null) {
            alreadyOnPortal.remove(player);
            return;
        }
        if (alreadyOnPortal.contains(player)) {
            // Don't let the player move if their chunk isn't loaded.
            Chunk chunk = loc.getChunk();
            if (!chunk.isLoaded()) {
                event.setCancelled(true);
            }
            return;
        }
        alreadyOnPortal.add(player);
        portal.teleport(player);
    }

    private void vehicleMove(Vehicle vehicle) {
        List<Entity> passengers = vehicle.getPassengers();
        Location loc = vehicle.getLocation();
        Portal portal = Portal.isInAPortal(loc);
        if (portal == null) {
            if (alreadyOnPortal.contains(vehicle) && loc.getChunk().isLoaded()) {
                alreadyOnPortal.remove(vehicle);
                alreadyOnPortal.removeAll(passengers);
            }
            return;
        }
        if (alreadyOnPortal.contains(vehicle)) {
            return;
        }

        portal.teleport(vehicle);
        alreadyOnPortal.add(vehicle);
        alreadyOnPortal.addAll(passengers);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        /*With every BlockPlaceEvent, register the location, if there is
         * an 'unlinked' location stored already, pair that location with
         *the new location as a portal-pair.
         *This is just warming up for the best type of configuration
         *management necessary for the plugin
         */
        // TODO: Get all this portal logic out of the listener and into Portal
        /* TODO: Don't use config as some awkward data pass-through for reinstantiating
           portals. Instead instantiate a Portal directly, then save to config.
         */
        /* TODO: Clean up all the lengthy methods here, get some better code reuse in there. */
        BuildPortals.logger.log(BuildPortals.logLevel, "Block place event registered");

        // Get relevant info about event
        Block block = event.getBlockPlaced();
        if (!BuildPortals.activatorMaterialNames.contains(block.getType().name())) {
            return;
        }

        BuildPortals.logger.log(BuildPortals.logLevel, "Block is a portal activator");
        World world = block.getWorld();
        // Get vectors to actual portal blocks from handler
        ArrayList<String> frameVecs = new ArrayList<>();
        ArrayList<String> activatorVecs = new ArrayList<>();
        ArrayList<String> vectors = new ArrayList<>();
        Float yaw = Portal.getCompletePortalVectors(block, frameVecs, activatorVecs, vectors);

        if (null == yaw) {
            return;
        }
        BuildPortals.logger.log(BuildPortals.logLevel, "Block completes a portal");

        Player player;
        player = event.getPlayer();
        if (!player.hasPermission("buildportals.activate")) {
            player.sendMessage("You do not have permission to activate portals!");
            return;
        }
        BuildPortals.logger.log(BuildPortals.logLevel, "Player " + player.getDisplayName() + " has appropriate permissions");

        boolean unlinkedPortal = BuildPortals.config.getBoolean("portals.0." + block.getType().name() + ".active");
        Map<String, Object> newPortal = new HashMap<>();
        BuildPortals.logger.log(BuildPortals.logLevel, "This is an unlinked portal");

        if (unlinkedPortal) {
            ArrayList<String> vectorsA = (ArrayList<String>) BuildPortals.config.getStringList("portals.0." + block.getType().name() + ".vec");
            ArrayList<String> frameVecsA = (ArrayList<String>) BuildPortals.config.getStringList("portals.0." + block.getType().name() + ".frame");
            ConfigurationSection portalsSection = BuildPortals.config.getConfigurationSection("portals");
            if ( portalsSection == null ) {
                return;
            }
            Set<String> portalKeys = portalsSection.getKeys(false);
            int i = 1;
            while (portalKeys.contains(Integer.toString(i))) {
                i+=1;
            }
            BuildPortals.logger.info("Saving new portal, number " + Integer.toString(i));
            newPortal.put("A.world", BuildPortals.config.getString("portals.0." + block.getType().name() + ".world"));
            newPortal.put("A.vec", vectorsA);
            newPortal.put("A.frame", frameVecsA);
            newPortal.put("A.yaw", BuildPortals.config.getString("portals.0." + block.getType().name() + ".yaw"));
            newPortal.put("B.world", world.getName());
            newPortal.put("B.vec", vectors);
            newPortal.put("B.frame", frameVecs);
            newPortal.put("B.yaw", yaw.toString());
            BuildPortals.config.set("portals.0." + block.getType().name() + ".active", false);
            BuildPortals.config.set("portals.0." + block.getType().name() + ".world", null);
            BuildPortals.config.set("portals.0." + block.getType().name() + ".vec", null);
            BuildPortals.config.set("portals.0." + block.getType().name() + ".frame", null);
            BuildPortals.config.set("portals.0." + block.getType().name() + ".activators", null);
            BuildPortals.config.set("portals.0." + block.getType().name() + ".yaw", null);
            BuildPortals.config.createSection("portals." + Integer.toString(i), newPortal);
            BuildPortals.config.set("portals." + Integer.toString(i) + ".active", true);

            // Convert portal interiors to air
            Location portalLoc = null;
            for (String locs : vectors) {
                String[] locStr = locs.split(",");
                portalLoc = new Location(block.getWorld(), Double.parseDouble(locStr[0]), Double.parseDouble(locStr[1]), Double.parseDouble(locStr[2]));
                portalLoc.getBlock().setType(Material.AIR);
            }
            if (null != portalLoc) {
                portalLoc.getWorld().strikeLightningEffect(portalLoc);
            }
            for (String locs : vectorsA) {
                String[] locStr = locs.split(",");
                portalLoc = new Location(Bukkit.getWorld((String) newPortal.get("A.world")), Double.parseDouble(locStr[0]), Double.parseDouble(locStr[1]), Double.parseDouble(locStr[2]));
                portalLoc.getBlock().setType(Material.AIR);
            }
            if (null != portalLoc) {
                portalLoc.getWorld().strikeLightningEffect(portalLoc);
            }
        } else {
            // Save unlinked portal location
            BuildPortals.logger.info("Collecting unlinked portal data...");
            newPortal.put("world", block.getWorld().getName());
            newPortal.put("vec", vectors);
            newPortal.put("frame", frameVecs);
            newPortal.put("activators", activatorVecs);
            newPortal.put("yaw", yaw.toString());
            BuildPortals.config.createSection("portals.0." + block.getType().name(), newPortal);
            BuildPortals.config.set("portals.0." + block.getType().name() + ".active", true);

            // Make a visible particle effect
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
        BuildPortals.logger.info("Saving changes...");
        BuildPortals.plugin.saveConfig();
        Portal.loadPortalsFromConfig();
    }
}