package java.frahm.justin.mcplugins.buildportals;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
	ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();

	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(new BPListener(this), this);
		//FileConfiguration config = this.getConfig();
		//config.addDefault("youAreAwesome", true);
		//config.options().copyDefaults(true);
		this.saveConfig();
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
			player.sendMessage("Your vector is: " + player.getLocation().toVector().toString());
			player.sendMessage("   as a string: " + player.getLocation().toString());
			return true;
		}
		return false;
	}
}
