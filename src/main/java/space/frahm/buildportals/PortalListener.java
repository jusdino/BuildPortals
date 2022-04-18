package space.frahm.buildportals;

import java.util.HashSet;
import java.util.List;

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
        Portal portal = Portal.getPortalFromLocation(player.getLocation());
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
        Portal portal = Portal.getPortalFromLocation(loc);
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
    public void onBlockPlace(BlockPlaceEvent event) throws InvalidConfigurationException {
        /* TODO: Update this description
         * With every BlockPlaceEvent, register the location, if there is
         * an 'unlinked' location stored already, pair that location with
         * the new location as a portal-pair.
         * This is just warming up for the best type of configuration
         * management necessary for the plugin
         */
        BuildPortals.logger.log(BuildPortals.logLevel, "Block place event registered");

        // Get relevant info about event
        Block block = event.getBlockPlaced();
        if (!BuildPortals.activatorMaterialNames.contains(block.getType().name())) {
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

        IncompletePortal newIncompletePortal = IncompletePortal.getNewPortalFromBlock(block);
        if (newIncompletePortal == null) {
            return;
        }
        BuildPortals.logger.log(BuildPortals.logLevel, "Block completes a portal");

        IncompletePortal incompletePortal = IncompletePortal.getPortalFromActivatorName(block.getType().name());
        if (incompletePortal == null) {
            newIncompletePortal.saveConfig();
        } else {
            Portal newCompletePortal = new Portal(incompletePortal, newIncompletePortal);
            newCompletePortal.saveConfig();
        }
    }
}