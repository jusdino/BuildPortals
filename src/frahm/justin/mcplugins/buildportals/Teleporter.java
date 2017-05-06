package frahm.justin.mcplugins.buildportals;

import java.util.Arrays;
import java.util.List;
import java.lang.Math;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.minecart.CommandMinecart;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class Teleporter {
	
	public Teleporter() {
		
	}
	
	public Entity teleport(Entity entity, Location destination) {
		destination.getChunk().load();
		if (entity instanceof Vehicle) {
			List<Entity> passengers = ((Vehicle)entity).getPassengers();
			for (Entity passenger: passengers) {
				if (((Vehicle)entity).removePassenger(passenger)) {
					teleport(passenger, destination);
				}
			}
			if (entity instanceof AbstractHorse) {
				entity = teleport((AbstractHorse)entity, destination);
			} else if (entity instanceof Pig) {
				entity = teleport((Pig) entity, destination);
			} else if (entity instanceof Minecart){
				entity = teleport((Minecart)entity, destination);
			}
			if (entity != null) {
				for (Entity passenger: passengers) {
					((Vehicle)entity).addPassenger(passenger);
				}
			}
			return entity;
		}
		if (entity instanceof Player) {
			return teleport((Player) entity, destination);
		} else {
			//Bail on the teleport for unhandled Entities
			return null;
		}
	}
	
	
	public Minecart teleport(Minecart vehicle, Location destination) {
		Minecart destVehicle = destination.getWorld().spawn(destination, vehicle.getClass());
		Vector speedVec = vehicle.getVelocity();
		Double speed = Math.sqrt(speedVec.getX()*speedVec.getX() + speedVec.getY()*speedVec.getY() + speedVec.getZ()*speedVec.getZ());
		//Set minimum exit velocity
		if (speed == 0) {
			speed = 0.1;
		}
		Vector destVec = destination.getDirection().multiply(speed);
		destVehicle.setVelocity(destVec);
		destVehicle.setCustomName(vehicle.getCustomName());
		destVehicle.setDamage(vehicle.getDamage());
		destVehicle.setGlowing(vehicle.isGlowing());
		
		if (vehicle instanceof InventoryHolder) {
			try {
				int size;
				switch (((InventoryHolder) vehicle).getInventory().getType()) {
					case CHEST:	size = 27;
									break;
					case HOPPER:	size = 5;
									break;
					default:		size = 1;
									break;
				}
				ItemStack[] items = Arrays.copyOf(((InventoryHolder)vehicle).getInventory().getContents(), Math.min(((InventoryHolder)vehicle).getInventory().getContents().length, size));
				Bukkit.broadcastMessage(((InventoryHolder)vehicle).getInventory().getType() + ": " + items.length);
				((InventoryHolder)destVehicle).getInventory().setContents(items);
				((InventoryHolder)vehicle).getInventory().clear();
			} catch (Exception exc) {
				Bukkit.broadcastMessage(exc.getMessage());
				destVehicle.remove();
				return null;
			}
		} else if (vehicle instanceof CommandMinecart) {
			((CommandMinecart)vehicle).setCommand(((CommandMinecart)vehicle).getCommand());
		}
		
		vehicle.remove();
		return destVehicle;
	}
	
	public Player teleport(Player player, Location destination) {
		player.teleport(destination);
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
			destHorse.setGlowing(horse.isGlowing());
			
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
			destPig.setGlowing(pig.isGlowing());
			pig.remove();
		} catch (Exception exc){
			Bukkit.broadcastMessage(exc.getMessage());
			destPig.remove();
			return null;
		} 
		return destPig;
	}
}