package space.frahm.buildportals;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class BuildPortals extends JavaPlugin {
    // Hacky way to turn on DEBUG for plugin only
    public static Level logLevel;

    static String frameMaterialName;
    public static ArrayList<String> activatorMaterialNames;

    public static FileConfiguration config;
    public static Logger logger;
    public static BuildPortals plugin;

    @Override
    public void onEnable() {
        plugin = this;
        config = this.getConfig();
        logger = this.getLogger();
        logLevel = Level.OFF;
        // Set default portal building material to emerald blocks
        config.addDefault("PortalMaterial", Material.EMERALD_BLOCK.name());
        /* Set default portal activating material to be:
         * - Redstone Blocks
         * - Gold Blocks
         * - Diamond Blocks
         */
        ArrayList<String> activators = new ArrayList<>();
        activators.add(Material.REDSTONE_BLOCK.name());
        activators.add(Material.GOLD_BLOCK.name());
        activators.add(Material.DIAMOND_BLOCK.name());
        config.addDefault("PortalActivators", activators);
        config.addDefault("Debug", false);
        config.options().copyDefaults(true);
        this.saveConfig();

        boolean debug = config.getBoolean("Debug");
        if (debug) {
            logLevel = Level.INFO;
            logger.log(logLevel, "Debug logs on");
        }

        frameMaterialName = config.getString("PortalMaterial");
        logger.log(logLevel, "Portal frame material set to " + frameMaterialName);
        activatorMaterialNames = (ArrayList<String>) config.getStringList("PortalActivators");
        logger.log(logLevel, "Portal activators set to " + activatorMaterialNames);

        getServer().getPluginManager().registerEvents(new PortalListener(), this);
        Portal.loadPortalsFromConfig();
        IncompletePortal.loadPortalsFromConfig();
    }

    @Override
    public void onDisable() {
        saveConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (cmd.getName().equalsIgnoreCase("buildportals") || cmd.getName().equalsIgnoreCase("bp")) {
            if (args.length < 1) {
                return false;
            }
            switch (args[0].toLowerCase()) {
                case "version":
                    sender.sendMessage("This is BuildPortals version " + this.getDescription().getVersion());
                        return true;
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
                    if (player.getVehicle() instanceof Boat) {
                        loc.add(0,2,0);
                    } 
                    sender.sendMessage("Your location is: " + loc.toVector().toString());
                    if (Portal.isInAPortal(loc)) {
                        player.sendMessage("You ARE in a portal!");
                    } else {
                        player.sendMessage("You ARE NOT in a portal.");
                    }
                    return true;
                case "setmaterial":
                    if ( ! sender.hasPermission("buildportals.*") ) {
                        sender.sendMessage("You do not have permission to use this command.");
                        return true;
                    }
                    Material mat = null;
                    try {
                        mat = Material.getMaterial(args[1].toUpperCase());
                    } catch (NullPointerException | ArrayIndexOutOfBoundsException exc) {
                        sender.sendMessage("You must specify a material.");
                    }
                    if (mat == null) {
                        sender.sendMessage("Material name invalid.");
                        sender.sendMessage("Setting portal material failed.");
                        logger.warning("Setting portal material failed.");
                        return false;
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
                    Portal.loadPortalsFromConfig();
                    return true;
                case "listmaterial":
                    if (sender.hasPermission("buildportals.listmaterial")) {
                        String matName = config.getString("PortalMaterial");
                        sender.sendMessage("Portal material is: " + matName);
                        return true;
                    } else {
                        sender.sendMessage("You do not have permission to use this command.");
                        return true;
                    }
                case "addactivator":
                    if ( ! sender.hasPermission("buildportals.*")) {
                        sender.sendMessage("You do not have permission to use this command.");
                        return true;
                    }
                    ArrayList<String> activators;
                    mat = null;
                    activators = (ArrayList<String>) config.getStringList("PortalActivators");
                    try {
                        mat = Material.getMaterial(args[1].toUpperCase());
                    } catch (NullPointerException | ArrayIndexOutOfBoundsException exc) {
                        sender.sendMessage("You must specify a material.");
                    }
                    if (mat == null) {
                        sender.sendMessage("Material name invalid.");
                        sender.sendMessage("Adding activator material failed.");
                        logger.warning("Adding activator material failed.");
                        return false;
                    }
                    if (!mat.isBlock()) {
                        sender.sendMessage("Material must be a placeable block type.");
                        sender.sendMessage("Adding activator material failed.");
                        logger.warning("Adding activator material failed.");
                        return false;
                    }
                    if (activators.contains(mat.name())) {
                        sender.sendMessage("That is already an activator material.");
                        logger.warning(sender.getName() + " attempted to add an already configured activator material.");
                        return false;
                    }
                    activators.add(mat.name());
                    sender.sendMessage("Adding " + mat.name() + " as an activator.");
                    logger.info("Adding " + mat.name() + " as an activator.");
                    config.set("PortalActivators", activators);
                    this.saveConfig();
                    return true;
                case "removeactivator":
                    if ( ! sender.hasPermission("buildportals.*")) {
                        sender.sendMessage("You do not have permission to use this command.");
                        return true;
                    }
                    String matName = null;
                    activators = (ArrayList<String>) config.getStringList("PortalActivators");
                    try {
                        matName = args[1].toUpperCase();
                    } catch (NullPointerException | ArrayIndexOutOfBoundsException exc) {
                        sender.sendMessage("You must specify a material.");
                    }
                    if (matName == null) {
                        sender.sendMessage("Removing activator material failed.");
                        logger.warning("Removing activator material failed.");
                        return false;
                    }
                    if (!activators.contains(matName)) {
                        sender.sendMessage("That is not an activator material.");
                        logger.warning(sender.getName() + " attempted to remove to an unconfigured activator material.");
                        return false;
                    }
                    activators.remove(matName);
                    sender.sendMessage("Removing " + matName + " from activators.");
                    logger.info("Removing " + matName + " from activators.");
                    config.set("PortalActivators", activators);
                    this.saveConfig();
                    return true;
                case "listactivators":
                    if (sender.hasPermission("buildportals.listactivators")) {
                    activators = (ArrayList<String>) config.getStringList("PortalActivators");
                    sender.sendMessage("Activators are: " + activators.toString());
                    return true;
                    } else {
                        sender.sendMessage("You do not have permission to use this command.");
                        return true;
                    }
                default:
                    sender.sendMessage("BuildPortals command usage:");
                    sender.sendMessage("  /BP Version ");
                    sender.sendMessage("    Returns the BuildPortals Version info.");
                    sender.sendMessage("  /BP Check ");
                    sender.sendMessage("    Returns whether you are currently in a portal.");
                    sender.sendMessage("  /BP SetMaterial <Material_Name>");
                    sender.sendMessage("    Sets the material from which portals should be built. Note that this will change all existing portals to this material.");
                    sender.sendMessage("  /BP AddActivator <Material_Name>");
                    sender.sendMessage("    Adds to the list of activator materials.");
                    sender.sendMessage("  /BP RemoveActivator <Material_Name>");
                    sender.sendMessage("    Removes an activator material.");
                    sender.sendMessage("  /BP ListMaterial");
                    sender.sendMessage("    Lists the configured portal material.");
                    sender.sendMessage("  /BP ListActivators");
                    sender.sendMessage("    Lists all configured activator materials.");
                    return false;
            }
        }
        return true;
    }
}