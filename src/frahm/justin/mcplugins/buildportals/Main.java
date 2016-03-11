package frahm.justin.mcplugins.buildportals;

import java.util.ArrayList;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
	PortalHandler portals;
	FileConfiguration config;
	Logger logger;

	@Override
	public void onEnable() {
		config = this.getConfig();
		portals = new PortalHandler(this);
		logger = this.getLogger();
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
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if (cmd.getName().equalsIgnoreCase("buildportals") || cmd.getName().equalsIgnoreCase("bp")) {
			if (args.length < 1) {
				return false;
			}
			switch (args[0].toLowerCase()) {
				case "check":
					if (!(sender instanceof Player)) {
						sender.sendMessage("Only a player can check their location.");
						return false;
					}
					Player player = (Player) sender;
					Location loc = player.getLocation().getBlock().getLocation();
					if (player.getVehicle() instanceof Minecart) {
						loc.add(0,1,0);
					}
					sender.sendMessage("Your location is: " + loc.toVector().toString());
					Boolean inPortal = portals.isInAPortal(loc);
					if (inPortal) {
						player.sendMessage("You ARE in a portal!");
					} else {
						player.sendMessage("You ARE NOT in a portal.");
					}
					return true;
				case "setmaterial":
					Material mat = null;
					try {
						mat = Material.getMaterial(args[1].toUpperCase());
					} catch (NullPointerException exc) {
						sender.sendMessage("You must specify a material.");
					} catch (ArrayIndexOutOfBoundsException exc) {
						sender.sendMessage("You must specify a material.");
					} finally {
						if (mat == null) {
							sender.sendMessage("Material name invalid.");
							sender.sendMessage("Setting portal material failed.");
							logger.warning("Setting portal material failed.");
							return false;
						}
					}
					if (!mat.isBlock()) {
						sender.sendMessage("Material must be a placeable block type.");
						sender.sendMessage("Setting portal material failed.");
						logger.warning("Setting portal material failed.");
						return false;
					}
					sender.sendMessage("Setting portal material to " + mat.name());
					logger.info("Setting portal material to " + mat.name());
					config.set("PortalMaterial",mat.name());
					this.saveConfig();
					sender.sendMessage("Converting existing portals to " + mat.name());
					logger.info("Converting existing portals to " + mat.name());
					portals.updatePortals();
				default:
					return false;
			}
			
		}
		return true;
	}
}