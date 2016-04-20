package frahm.justin.mcplugins.buildportals;

import org.bukkit.Location;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Pig;

public class VehicleHandler {
	
	public VehicleHandler() {
		
	}
	
	public Minecart teleport(Minecart minecart, Location destination) {
		Minecart destCart = destination.getWorld().spawn(destination, Minecart.class);
		minecart.remove();
		return destCart;
	}
	
	public Horse teleport(Horse horse, Location destination) {
		Horse destHorse = destination.getWorld().spawn(destination, Horse.class);
		destHorse.setAge(horse.getAge());
		destHorse.setColor(horse.getColor());
		destHorse.setCustomName(horse.getCustomName());
		//No setSpeed in API...
		destHorse.setJumpStrength(horse.getJumpStrength());;
		destHorse.setMaxHealth(horse.getMaxHealth());
		destHorse.setHealth(horse.getHealth());
		destHorse.setMaximumAir(horse.getMaximumAir());
		destHorse.setOwner(horse.getOwner());
		destHorse.setStyle(horse.getStyle());
		destHorse.setVariant(horse.getVariant());
		destHorse.getInventory().setArmor(horse.getInventory().getArmor());
		destHorse.getInventory().setSaddle(horse.getInventory().getSaddle());
		horse.remove();
		
		return destHorse;
	}
	
	public Pig teleport(Pig pig, Location destination) {
		Pig destPig = destination.getWorld().spawn(destination,  Pig.class);
		destPig.setAge(pig.getAge());
		destPig.setCustomName(pig.getCustomName());
		destPig.setMaxHealth(pig.getMaxHealth());
		destPig.setHealth(pig.getHealth());
		destPig.setSaddle(pig.hasSaddle());
		pig.remove();
		return destPig;
	}
}