package frahm.justin.mcplugins.buildportals;

import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
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
}