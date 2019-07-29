package frahm.justin.mcplugins.buildportals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.lang.Math;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.PolarBear;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.Villager;
import org.bukkit.entity.minecart.CommandMinecart;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;


class Teleporter {
	private static Logger logger;
	private static Level logLevel;

	Teleporter(BuildPortals plugin) {
		logger = plugin.getLogger();
		logLevel = BuildPortals.logLevel;
	}
	
	Entity teleport(Entity entity, Location destination) {
		logger.log(logLevel, "Entering teleport(Entity, ...) method");
		destination.getChunk().load();
		if (entity instanceof Vehicle) {
			List<Entity> passengers = (entity).getPassengers();
			List<Entity> destPassengers = new ArrayList<>();
			for (Entity passenger: passengers) {
				if ((entity).removePassenger(passenger)) {
					Entity destPassenger = teleport(passenger, destination);
					if ( destPassenger != null) {
						destPassengers.add(destPassenger);
					}
				}
			}
			if (entity instanceof AbstractHorse) {
				entity = teleport((AbstractHorse)entity, destination);
			} else if (entity instanceof Minecart){
				entity = teleport((Minecart)entity, destination);
			} else if (entity instanceof Boat){
				destination.add(0,1,0);
				entity = teleport((Boat)entity, destination);
			} else if (entity instanceof Pig) {
				entity = teleport((Pig) entity, destination);
			}
			if (entity != null) {
				for (Entity passenger: destPassengers) {
					(entity).addPassenger(passenger);
				}
			}
			return entity;
		} else if (entity instanceof Player) {
			Player player;
			player = (Player)entity;
			if (!player.hasPermission("buildportals.teleport")) {
				player.sendMessage("You do not have permission to use portals!");
				return null;
			}
			Location source = player.getLocation();
			ArrayList<LivingEntity> leadees = new ArrayList<>();
			// There doesn't seem to be an easy way to get a collection of leashed entities
			// from the player directly...
			World world = source.getWorld();
			if ( world == null ) {
				return null;
			}
			Collection<Entity> entities = world.getNearbyEntities(source, 11, 11, 11);
			for (Entity ent: entities) {
				if (ent instanceof LivingEntity) {
					if (((LivingEntity)ent).isLeashed() && ((LivingEntity)ent).getLeashHolder() == entity) {
						Entity destEnt = teleport(ent, destination);
						leadees.add((LivingEntity)destEnt);
					}
				}
			}
			
			entity = teleport((Player) entity, destination);
			for (LivingEntity ent: leadees) {
				ent.setLeashHolder(entity);
			}
		} else if (entity instanceof Cow) {
			entity = teleport((Cow) entity, destination);
		} else if (entity instanceof Sheep) {
			entity = teleport((Sheep) entity, destination);
		} else if (entity instanceof PolarBear) {
			entity = teleport((PolarBear) entity, destination);
		} else if (entity instanceof Chicken) {
			entity = teleport((Chicken) entity, destination);
		} else if (entity instanceof Villager) {
			entity = teleport((Villager) entity, destination);
		}
		else {
			//Bail on the teleport for unhandled Entities
			return null;
		}
		return entity;
	}
	
	
	private Minecart teleport(Minecart vehicle, Location destination) {
		logger.log(logLevel, "Entering teleport(Minecart, ...) method");
		World world = destination.getWorld();
		if ( world == null ) {
			return null;
		}
		Minecart destVehicle = world.spawn(destination, vehicle.getClass());
		Vector speedVec = vehicle.getVelocity();
		double speed = Math.sqrt(speedVec.getX()*speedVec.getX() + speedVec.getY()*speedVec.getY() + speedVec.getZ()*speedVec.getZ());
		//Set minimum exit velocity
		if (speed < 0.1) {
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
				destVehicle.remove();
				return null;
			}
		} else if (vehicle instanceof CommandMinecart) {
			((CommandMinecart)vehicle).setCommand(((CommandMinecart)vehicle).getCommand());
		}
		
		vehicle.remove();
		return destVehicle;
	}

	private Boat teleport(Boat vehicle, Location destination) {
		logger.log(logLevel, "Entering teleport(Boat, ...) method");

		World world = destination.getWorld();
		if ( world == null ) {
			return null;
		}
		Boat destVehicle = world.spawn(destination, vehicle.getClass());
		Vector speedVec = vehicle.getVelocity();
		vehicle.remove();
		double speed = Math.sqrt(speedVec.getX()*speedVec.getX() + speedVec.getY()*speedVec.getY() + speedVec.getZ()*speedVec.getZ());
		// Spit the boat out on the other side of the portal
		if (speed < 0.1) {
			speed = 0.1;
		}
		Vector destVec = destination.getDirection().multiply(speed);
		destVehicle.setVelocity(destVec);
		destVehicle.setCustomName(vehicle.getCustomName());
		destVehicle.setGlowing(vehicle.isGlowing());

		return destVehicle;
	}

	private Player teleport(Player player, Location destination) {
		logger.log(logLevel, "Entering teleport(Player, ...) method");
		player.teleport(destination);
		return player;
	}
	
	private AbstractHorse teleport(AbstractHorse horse, Location destination) {
		logger.log(logLevel, "Entering teleport(AbstractHorse, ...) method");
		World world = destination.getWorld();
		if ( world == null ) {
			return null;
		}
		AbstractHorse destHorse = world.spawn(destination, horse.getClass());
		try {
			destHorse.setAge(horse.getAge());
			destHorse.setCustomName(horse.getCustomName());
			destHorse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue());
			destHorse.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(horse.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
			destHorse.getAttribute(Attribute.HORSE_JUMP_STRENGTH).setBaseValue(horse.getAttribute(Attribute.HORSE_JUMP_STRENGTH).getBaseValue());
			destHorse.setJumpStrength(horse.getJumpStrength());
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
	
	
	private Chicken teleport(Chicken chicken, Location destination) {
		logger.log(logLevel, "Entering teleport(Chicken, ...) method");
		World world = destination.getWorld();
		if ( world == null ) {
			return null;
		}
		Chicken destChicken = world.spawn(destination, chicken.getClass());
		try {
			destChicken.setAge(chicken.getAge());
			destChicken.setCustomName(chicken.getCustomName());
			destChicken.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(destChicken.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
			destChicken.setHealth(chicken.getHealth());
			destChicken.setGlowing(chicken.isGlowing());
			chicken.remove();
		} catch (Exception exc){
			destChicken.remove();
			return null;
		} 
		return destChicken;
	}
	
	
	private Cow teleport(Cow cow, Location destination) {
		logger.log(logLevel, "Entering teleport(Cow, ...) method");
		World world = destination.getWorld();
		if ( world == null ) {
			return null;
		}
		Cow destCow = world.spawn(destination, cow.getClass());
		try {
			destCow.setAge(cow.getAge());
			destCow.setCustomName(cow.getCustomName());
			destCow.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(destCow.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
			destCow.setHealth(cow.getHealth());
			destCow.setGlowing(cow.isGlowing());
			cow.remove();
		} catch (Exception exc){
			destCow.remove();
			return null;
		} 
		return destCow;
	}
	
	
	private Pig teleport(Pig pig, Location destination) {
		logger.log(logLevel, "Entering teleport(Pig, ...) method");
		World world = destination.getWorld();
		if ( world == null ) {
			return null;
		}
		Pig destPig = world.spawn(destination, pig.getClass());
		try {
			destPig.setAge(pig.getAge());
			destPig.setCustomName(pig.getCustomName());
			destPig.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(destPig.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
			destPig.setHealth(pig.getHealth());
			destPig.setSaddle(pig.hasSaddle());
			destPig.setGlowing(pig.isGlowing());
			pig.remove();
		} catch (Exception exc){
			destPig.remove();
			return null;
		} 
		return destPig;
	}
	
	
	private Sheep teleport(Sheep sheep, Location destination) {
		logger.log(logLevel, "Entering teleport(Sheep, ...) method");
		World world = destination.getWorld();
		if ( world == null ) {
			return null;
		}
		Sheep destSheep = world.spawn(destination, sheep.getClass());
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
			destSheep.remove();
			return null;
		} 
		return destSheep;
	}
	
	private PolarBear teleport(PolarBear polarbear, Location destination) {
		logger.log(logLevel, "Entering teleport(PolarBear, ...) method");
		World world = destination.getWorld();
		if ( world == null ) {
			return null;
		}
		PolarBear destPolarBear = world.spawn(destination, polarbear.getClass());
		try {
			destPolarBear.setAge(polarbear.getAge());
			destPolarBear.setCustomName(polarbear.getCustomName());
			destPolarBear.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(destPolarBear.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
			destPolarBear.setHealth(polarbear.getHealth());
			destPolarBear.setGlowing(polarbear.isGlowing());
			polarbear.remove();
		} catch (Exception exc){
			destPolarBear.remove();
			return null;
		} 
		return destPolarBear;
	}

	private Villager teleport(Villager villager, Location destination) {
		logger.log(logLevel, "Entering teleport(Villager, ...) method");
		World world = destination.getWorld();
		if ( world == null ) {
			return null;
		}
		Villager destVillager = world.spawn(destination, villager.getClass());
		
		try {
			destVillager.setAge(villager.getAge());
			destVillager.setCustomName(villager.getCustomName());
			destVillager.setHealth(villager.getHealth());
			destVillager.setGlowing(villager.isGlowing());
			destVillager.setProfession(villager.getProfession());
			destVillager.getInventory().setContents(villager.getInventory().getContents());
			destVillager.setRecipes(villager.getRecipes());
			destVillager.setVillagerExperience(villager.getVillagerExperience());
			destVillager.setVillagerLevel(villager.getVillagerLevel());
			destVillager.setVillagerType(villager.getVillagerType());

			villager.remove();
		} catch (Exception exc) {
			destVillager.remove();
			return null;
		}
		return destVillager;
	}
}
