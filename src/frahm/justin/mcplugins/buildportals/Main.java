package frahm.justin.mcplugins.buildportals;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
	ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
	PortalHandler portals;
	FileConfiguration config;

	@Override
	public void onEnable() {
		config = this.getConfig();
		portals = new PortalHandler(this);
		getServer().getPluginManager().registerEvents(new BPListener(this, portals), this);
		//Set default portal building material to emerald blocks
		config.addDefault("PortalMaterial", Material.EMERALD_BLOCK.name());
		/*Set default portal activating material to be:
		 * Redstone Blocks
		 * Gold Blocks
		 * Diamond Blocks
		 */
		ArrayList<String> activators = new ArrayList<String>();
		activators.add(Material.REDSTONE_BLOCK.name());
		activators.add(Material.GOLD_BLOCK.name());
		activators.add(Material.DIAMOND_BLOCK.name());
		config.addDefault("PortalActivators", activators);
		config.options().copyDefaults(true);
		this.saveConfig();
		portals.updatePortals();
	}

	@Override
	public void onDisable() {

	}

	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		//Report current player vector just so I can see the form they take
		if (cmd.getName().equalsIgnoreCase("vec")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage("Only players can ask for their vector.");
				return false;
			}
			Player player = (Player) sender;
			
			//Floored player location
			Location loc = player.getLocation();
			loc.setX(loc.getBlockX());
			loc.setY(loc.getBlockY());
			loc.setZ(loc.getBlockZ());
			
			player.sendMessage("Your floored vector is: " + loc.toVector().toString());
			player.sendMessage("   as a string: " + loc.toString());
			ArrayList<String> vectors = (ArrayList<String>) config.getStringList("Vectors");
			vectors.add(loc.toVector().toString());
			config.set("Vectors", vectors);
			this.saveConfig();
			return true;
		}
		
		if (cmd.getName().equalsIgnoreCase("check")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage("Only players can check their position.");
				return false;
			}
			Player player = (Player) sender;

			//Floored player location
			Location loc = player.getLocation();
			loc.setX(loc.getBlockX());
			loc.setY(loc.getBlockY());
			loc.setZ(loc.getBlockZ());
			
			player.sendMessage("In a portal: " + portals.isInAPortal(loc));
			player.sendMessage("You are at: " + loc.toVector().toString());
		}
		return false;
	}
}
