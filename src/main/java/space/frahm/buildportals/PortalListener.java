package space.frahm.buildportals;

import java.util.HashSet;
import java.util.UUID;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.InvalidConfigurationException;
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
import org.bukkit.scheduler.BukkitRunnable;


public class PortalListener implements Listener {
    private static HashSet<UUID> alreadyOnPortal = new HashSet<>();

    public PortalListener() {
        super();
    }

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
        Portal portal = Portal.getPortalFromLocation(player.getLocation());
        if (portal == null) {
            alreadyOnPortal.remove(player.getUniqueId());
            return;
        }
        if (alreadyOnPortal.contains(player.getUniqueId())) {
            // Don't let the player move if their chunk isn't loaded.
            Chunk chunk = loc.getChunk();
            if (!chunk.isLoaded()) {
                event.setCancelled(true);
            }
            return;
        }
        BuildPortals.logger.log(BuildPortals.logLevel, "Teleporting " + player);
        new BukkitRunnable() {
            @Override
            public void run() {
                Entity entity = (Player)portal.teleport(player);
                if (entity != null) {
                    alreadyOnPortal.add(entity.getUniqueId());
                    BuildPortals.logger.log(BuildPortals.logLevel, "Added " + vehicle + " to alreadyOnPortal");
                    BuildPortals.logger.log(BuildPortals.logLevel, "alreadyOnPortal: " + alreadyOnPortal);
                }
            }
        }.runTaskLater(BuildPortals.plugin, 1);
    }

    private void vehicleMove(Vehicle vehicle) {
        Location loc = vehicle.getLocation();
        Portal portal = Portal.getPortalFromLocation(loc);
        if (portal == null) {
            if (alreadyOnPortal.contains(vehicle.getUniqueId()) && loc.getChunk().isLoaded()) {
                alreadyOnPortal.remove(vehicle.getUniqueId());
            }
            BuildPortals.logger.log(BuildPortals.logLevel, "Removing " + vehicle + " from alreadyOnPortal");
            return;
        }
        if (alreadyOnPortal.contains(vehicle.getUniqueId())) {
            BuildPortals.logger.log(BuildPortals.logLevel, vehicle + " already in a portal");
            return;
        }

        BuildPortals.logger.log(BuildPortals.logLevel, "Teleporting " + vehicle);
        new BukkitRunnable() {
            @Override
            public void run() {
                Entity entity = (Vehicle)portal.teleport(vehicle);
                if (entity != null) {
                    alreadyOnPortal.remove(vehicle.getUniqueId());
                    alreadyOnPortal.add(entity.getUniqueId());
                    BuildPortals.logger.log(BuildPortals.logLevel, "Added " + vehicle + " to alreadyOnPortal");
                    BuildPortals.logger.log(BuildPortals.logLevel, "alreadyOnPortal: " + alreadyOnPortal);
                }
            }
        }.runTaskLater(BuildPortals.plugin, 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) throws InvalidConfigurationException {
        /* With every BlockPlaceEvent, check if the placed block completes an IncompletePortal
         * Then check if there is a matching IncompletePortal with the same activator.
         * If there is, create a new Portal from the two IncompletePortals.
         */
        BuildPortals.logger.log(BuildPortals.logLevel, "Block place event registered");

        // Get relevant info about event
        Block block = event.getBlockPlaced();
        if (!BuildPortals.activatorMaterials.contains(block.getType())) {
            return;
        }

        BuildPortals.logger.log(BuildPortals.logLevel, "Detected placed block that is a portal activator");
        Player player;
        player = event.getPlayer();
        if (!player.hasPermission("buildportals.activate")) {
            player.sendMessage("You do not have permission to activate portals!");
            return;
        }
        BuildPortals.logger.log(BuildPortals.logLevel, "Player " + player.getDisplayName() + " has appropriate permissions");

        /* We have to grab a handle to the pre-existing incompletePortal before we try to create a new one
         * because creating a new one dislodges the pre-existing one from IncompletePortal's static collection.
         */
        IncompletePortal incompletePortal = IncompletePortal.getPortalFromActivatorName(block.getType().name());
        IncompletePortal newIncompletePortal = IncompletePortal.getNewPortalFromBlock(block);
        if (newIncompletePortal == null) {
            return;
        }
        BuildPortals.logger.log(BuildPortals.logLevel, "Block completes a portal");

        if (incompletePortal == null) {
            newIncompletePortal.saveConfig();
        } else {
            if (!incompletePortal.integrityCheck(incompletePortal.frames)) {
                // If the pre-existing portal was broken, destroy it and save the new one
                incompletePortal.destroy();
                newIncompletePortal.saveConfig();
                return;
            } else {
                // All is well so create a new Portal
                Portal newPortal = new Portal(incompletePortal, newIncompletePortal);
                newPortal.saveConfig();
            }
        }
    }
}