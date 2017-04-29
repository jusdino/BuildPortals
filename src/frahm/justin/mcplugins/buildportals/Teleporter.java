package frahm.justin.mcplugins.buildportals;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.SkeletonHorse;
import org.bukkit.entity.Vehicle;
import org.bukkit.inventory.ItemStack;

public class Teleporter {
	
	public Teleporter() {
		
	}
	
	public boolean teleport(Player player, Location destination) {
		Vehicle vehicle = (Vehicle) player.getVehicle();
		destination.getChunk().load();
		
		if (vehicle == null) {
//			logger.info("Teleporting " + player.getName());
			player.teleport(destination);
		} else {
//			logger.info("Teleporting " + player.getName() + " with a vehicle.");
			//Update for 1.11
//			vehicle.eject();
			vehicle.removePassenger(player);
			//Don't teleport the vehicle if the player's teleport event was canceled
			if (player.teleport(destination)){
				if (vehicle instanceof AbstractHorse) {
					vehicle = teleport((AbstractHorse) vehicle, destination);
				}
				if (vehicle instanceof Minecart) {
					vehicle = teleport((Minecart) vehicle, destination);
				}
				if (vehicle instanceof Pig) {
					vehicle = teleport((Pig) vehicle, destination);
				}
			}
			//Update for 1.11
			vehicle.addPassenger(player);
//			vehicle.setPassenger(player);
		}
		
		return true;
	}
	
	public Minecart teleport(Minecart minecart, Location destination) {
		Minecart destCart = destination.getWorld().spawn(destination, Minecart.class);
		minecart.remove();
		return destCart;
	}
	
	public AbstractHorse teleport(AbstractHorse horse, Location destination) {
		AbstractHorse destHorse = destination.getWorld().spawn(destination, horse.getClass());
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
		} else if (horse instanceof SkeletonHorse) {
			destHorse.getInventory().setItem(0, new ItemStack(Material.SADDLE));
		}
		horse.remove();
		
		return destHorse;
	}
	
	public Pig teleport(Pig pig, Location destination) {
		Pig destPig = destination.getWorld().spawn(destination,  Pig.class);
		destPig.setAge(pig.getAge());
		destPig.setCustomName(pig.getCustomName());
//		destPig.setMaxHealth(pig.getMaxHealth());
		destPig.setHealth(pig.getHealth());
		destPig.setSaddle(pig.hasSaddle());
		pig.remove();
		return destPig;
	}
}