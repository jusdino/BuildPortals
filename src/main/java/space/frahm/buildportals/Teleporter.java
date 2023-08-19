package space.frahm.buildportals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;

public class Teleporter {

    public static Entity teleport(Entity entity, @Nonnull Location destination) {
        BuildPortals.logger.log(BuildPortals.logLevel, "Entering teleport(Entity, ...) method");
        if (entity instanceof Vehicle) {
            List<Entity> passengers = (entity).getPassengers();
            List<Entity> destPassengers = new ArrayList<>();
            for (Entity passenger: passengers) {
                if (entity.removePassenger(passenger)) {
                    Entity destPassenger = teleport(passenger, destination);
                    if ( destPassenger != null) {
                        destPassengers.add(destPassenger);
                    }
                }
            }
            if (entity instanceof Boat){
                // Keeps the boat floating in water
                destination.add(0,1,0);
            }
            entity = cloneTeleport(entity, destination);
            if (entity != null) {
                for (Entity passenger: destPassengers) {
                    (entity).addPassenger(passenger);
                }
            }
            return entity;
        } else if (entity instanceof Player) {
            entity = teleport((Player) entity, destination);
        } else {
            entity = cloneTeleport(entity, destination);
        }
        return entity;
    }
    
    private static Player teleport(Player player, @Nonnull Location destination) {
        BuildPortals.logger.log(BuildPortals.logLevel, "Entering teleport(Player, ...) method");
        if (!player.hasPermission("buildportals.teleport")) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Player " + player.getName() + " does not have permission to use a portal");
            player.sendMessage("You do not have permission to use portals!");
            return null;
        }
        Location source = player.getLocation();
        ArrayList<LivingEntity> leadees = new ArrayList<>();
        // There doesn't seem to be an easy way to get a collection of leashed entities
        // from the player directly...
        World world = source.getWorld();
        if ( world == null ) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Player's world is " + world + "!");
            return null;
        }
        Collection<Entity> entities = world.getNearbyEntities(source, 11, 11, 11);
        for (Entity ent: entities) {
            if (ent instanceof LivingEntity) {
                if (((LivingEntity)ent).isLeashed() && ((LivingEntity)ent).getLeashHolder() == player) {
                    Entity destEnt = teleport(ent, destination);
                    if (destEnt != null) {
                        leadees.add((LivingEntity) destEnt);
                    }
                }
            }
        }

        player.teleport(destination);
        for (LivingEntity ent: leadees) {
            ent.setLeashHolder(player);
        }
        return player;
    }
    
    private static Entity cloneTeleport(Entity entity, Location destination) {
        BuildPortals.logger.log(BuildPortals.logLevel, "Entering teleport(Entity, ...) method");
        Cloner cloner = new Cloner();
        Entity destEntity = cloner.clone(entity, destination);
        if (destEntity != null) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Removing " + entity);
            entity.remove();
        }
        return destEntity;
    }
}
