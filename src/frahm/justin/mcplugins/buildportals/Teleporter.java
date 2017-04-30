package frahm.justin.mcplugins.buildportals;

import java.util.List;
import java.lang.Math;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class Teleporter {
	
	public Teleporter() {
		
	}
	
	public Entity teleport(Entity entity, Location destination) {
		Bukkit.broadcastMessage("Teleporting Entity");
		if (entity instanceof Player) {
			return teleport((Player) entity, destination);
		} else if (entity instanceof AbstractHorse) {
			return teleport((AbstractHorse) entity, destination);
		} else if (entity instanceof Pig) {
			return teleport((Pig) entity, destination);
		} else if (entity instanceof Vehicle) {
			return teleport((Vehicle) entity, destination);
		} else {
			//Fallback attempt to 'teleport' whatever this is...
			Entity destEntity = destination.getWorld().spawn(destination, entity.getClass());
			entity.remove();
			return destEntity;
		}
	}
	
	public Vehicle teleport(Vehicle vehicle, Location destination) {
		Bukkit.broadcastMessage("Teleporting vehicle");
		Vehicle destVehicle = destination.getWorld().spawn(destination, vehicle.getClass());
		Vector speedVec = vehicle.getVelocity();
		Bukkit.broadcastMessage("Entrance velocity: " + speedVec.toString());
		Double speed = Math.sqrt(speedVec.getX()*speedVec.getX() + speedVec.getY()*speedVec.getY() + speedVec.getZ()*speedVec.getZ());
		Bukkit.broadcastMessage("Entrance Speed: " + speed);
		Vector destVec = destination.getDirection().multiply(speed);
		destVehicle.setVelocity(destVec);
		Bukkit.broadcastMessage("Exit velocity: " + destVec.toString());
		vehicle.remove();
		return destVehicle;
	}
	
	public Player teleport(Player player, Location destination) {
		Vehicle vehicle = (Vehicle) player.getVehicle();
		destination.getChunk().load();
		
		if (vehicle == null) {
			player.teleport(destination);
		} else {
			List<Entity> passengers = vehicle.getPassengers();
			for (Entity passenger: passengers) {
				if (vehicle.removePassenger(passenger)) {
					teleport(passenger, destination);
				}
			}
			vehicle = (Vehicle)teleport(vehicle, destination);
			if (vehicle != null) {
				for (Entity passenger: passengers) {
					vehicle.addPassenger(passenger);
				}
			}
		}
		
		return player;
	}
	
	public AbstractHorse teleport(AbstractHorse horse, Location destination) {
		AbstractHorse destHorse = destination.getWorld().spawn(destination, horse.getClass());
		try {
			destHorse.setAge(horse.getAge());
			destHorse.setCustomName(horse.getCustomName());
			destHorse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue());
			destHorse.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(horse.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
			destHorse.getAttribute(Attribute.HORSE_JUMP_STRENGTH).setBaseValue(horse.getAttribute(Attribute.HORSE_JUMP_STRENGTH).getBaseValue());
			destHorse.setJumpStrength(horse.getJumpStrength());;
			destHorse.setHealth(horse.getHealth());
			destHorse.setMaximumAir(horse.getMaximumAir());
			destHorse.setDomestication(horse.getDomestication());
			destHorse.setMaxDomestication(horse.getMaxDomestication());
			destHorse.setTamed(horse.isTamed());
			
			if (horse instanceof Horse) {
				((Horse)destHorse).setColor(((Horse)horse).getColor());
				((Horse)destHorse).setStyle(((Horse)horse).getStyle());
				((Horse)destHorse).getInventory().setArmor(((Horse)horse).getInventory().getArmor());
				((Horse)destHorse).getInventory().setSaddle(((Horse)horse).getInventory().getSaddle());
				destHorse.setOwner(horse.getOwner());
			} else if (horse.getInventory().contains(Material.SADDLE)) {
				destHorse.getInventory().setItem(0, new ItemStack(Material.SADDLE));
			}
			horse.remove();
		} catch (Exception exc) {
			destHorse.remove();
			return null;
		}
		
		return destHorse;
	}
	
	public Pig teleport(Pig pig, Location destination) {
		Pig destPig = destination.getWorld().spawn(destination,  Pig.class);
		try {
			destPig.setAge(pig.getAge());
			destPig.setCustomName(pig.getCustomName());
			destPig.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(destPig.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
			destPig.setHealth(pig.getHealth());
			destPig.setSaddle(pig.hasSaddle());
			pig.remove();
		} catch (Exception exc){
			destPig.remove();
			return null;
		} 
		return destPig;
	}
}