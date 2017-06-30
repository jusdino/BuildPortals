package frahm.justin.mcplugins.buildportals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.lang.Math;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
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
			List<Entity> destPassengers = new ArrayList<Entity>();
			for (Entity passenger: passengers) {
				if (((Vehicle)entity).removePassenger(passenger)) {
					Entity destPassenger = teleport(passenger, destination) ;
					if ( destPassenger != null) {
						destPassengers.add(destPassenger);
					}
				}
			}
			if (entity instanceof AbstractHorse) {
				entity = teleport((AbstractHorse)entity, destination);
			} else if (entity instanceof Minecart){
				entity = teleport((Minecart)entity, destination);
			}
			if (entity != null) {
				for (Entity passenger: destPassengers) {
					((Vehicle)entity).addPassenger(passenger);
				}
			}
			return entity;
		} else if (entity instanceof Pig) {
			entity = teleport((Pig) entity, destination);
		} else if (entity instanceof Cow) {
			Bukkit.broadcastMessage("Teleporting cow...");
			entity = teleport((Cow) entity, destination);
		} else if (entity instanceof Sheep) {
			entity = teleport((Sheep) entity, destination);
		} else if (entity instanceof Chicken) {
			entity = teleport((Chicken) entity, destination);
		} else if (entity instanceof Player) {
			entity = teleport((Player) entity, destination);
		} else {
			//Bail on the teleport for unhandled Entities
			return null;
		}
		return entity;
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
	
	
	public Chicken teleport(Chicken chicken, Location destination) {
		Chicken destChicken = destination.getWorld().spawn(destination, chicken.getClass());
		try {
			destChicken.setAge(chicken.getAge());
			destChicken.setCustomName(chicken.getCustomName());
			destChicken.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(destChicken.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
			destChicken.setHealth(chicken.getHealth());
			destChicken.setGlowing(chicken.isGlowing());
			chicken.remove();
		} catch (Exception exc){
			Bukkit.broadcastMessage(exc.getMessage());
			destChicken.remove();
			return null;
		} 
		return destChicken;
	}
	
	
	public Cow teleport(Cow cow, Location destination) {
		Cow destCow = destination.getWorld().spawn(destination, cow.getClass());
		try {
			destCow.setAge(cow.getAge());
			destCow.setCustomName(cow.getCustomName());
			destCow.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(destCow.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
			destCow.setHealth(cow.getHealth());
			destCow.setGlowing(cow.isGlowing());
			cow.remove();
		} catch (Exception exc){
			Bukkit.broadcastMessage(exc.getMessage());
			destCow.remove();
			return null;
		} 
		return destCow;
	}
	
	
	public Pig teleport(Pig pig, Location destination) {
		Pig destPig = destination.getWorld().spawn(destination, pig.getClass());
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
	
	
	public Sheep teleport(Sheep sheep, Location destination) {
		Sheep destSheep = destination.getWorld().spawn(destination, sheep.getClass());
		try {
			destSheep.setAge(sheep.getAge());
			destSheep.setCustomName(sheep.getCustomName());
			destSheep.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(destSheep.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
			destSheep.setHealth(sheep.getHealth());
			destSheep.setGlowing(sheep.isGlowing());
			destSheep.setColor(sheep.getColor());
			destSheep.setSheared(sheep.isSheared());
			sheep.remove();
		} catch (Exception exc){
			Bukkit.broadcastMessage(exc.getMessage());
			destSheep.remove();
			return null;
		} 
		return destSheep;
	}
}