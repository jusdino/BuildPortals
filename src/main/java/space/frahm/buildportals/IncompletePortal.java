package space.frahm.buildportals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.util.Vector;

public class IncompletePortal extends AbstractPortal {
    private final static String portalNumber = "0";
    private static HashMap<String, HashSet<Vector>> interiors = new HashMap<String, HashSet<Vector>>();
    private static HashMap<String, IncompletePortal> portals = new HashMap<>();
    protected ActivatedPortalFrame[] frames = {null};

    IncompletePortal(
        String identifier,
        ActivatedPortalFrame frame
    ) {
        this.identifier = identifier;
        this.frames[0] = frame;
        portals.put(identifier, this);
        interiors.get(frame.world.getName()).addAll(frame.interior);
    }

    public static IncompletePortal getNewPortalFromBlock(Block block) throws InvalidConfigurationException {
        ActivatedPortalFrame frame = ActivatedPortalFrame.getCompletePortalVectors(block);
        if (frame == null) {
            return null;
        }
        return new IncompletePortal(block.getType().name(), frame);
    }

    public static void loadPortalsFromConfig() {
        portals = new HashMap<>();
        for (String activatorMaterialName: BuildPortals.activatorMaterialNames) {
            loadFromConfig(activatorMaterialName);
        }
    }

    public static boolean isInAPortal(Location loc) {
        return isInAPortal(interiors, loc);
    }

    protected void destroy() {
        super.destroy();

        // Remove this portal from the static set
        portals.remove(this.identifier);

        // Clear the config entry and save
        BuildPortals.config.set("portals." + portalNumber + "." + this.identifier, null);
        BuildPortals.plugin.saveConfig();
    }

    public static IncompletePortal loadFromConfig(String activator) {
        ConfigurationSection config = BuildPortals.config;

        String worldName = config.getString("portals." + portalNumber + "." + activator + ".world");
        if (worldName == null) {
            BuildPortals.logger.severe("Error reading world from configuration for portal " + portalNumber);
            return null;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            BuildPortals.logger.warning("Failed to locate world: " + worldName + " skipping portal number " + portalNumber);
            return null;
        }
        String yawString = config.getString("portals." + portalNumber + "." + activator + ".yaw");
        if (yawString == null) {
            BuildPortals.logger.severe("Error reading yaw from configuration for portal " + portalNumber);
            return null;
        }
        Float yaw = Float.parseFloat(yawString);
        // Convert string lists to vector lists
        ArrayList<String> exteriorStrings = (ArrayList<String>) config.getStringList("portals." + portalNumber + "." + activator + ".frame");
        ArrayList<Vector> exteriorVecs = new ArrayList<>();
        for (String exteriorString : exteriorStrings) {
            String[] parts = exteriorString.split(",");
            if (parts.length != 3) {
                BuildPortals.logger.severe("Error reading frame vectors from configuration for portal " + portalNumber);
                return null;
            }
            Vector vec = new Vector();
            vec.setX(Double.parseDouble(parts[0]));
            vec.setY(Double.parseDouble(parts[1]));
            vec.setZ(Double.parseDouble(parts[2]));
            exteriorVecs.add(vec);
        }
        ArrayList<String> activatorStrings = (ArrayList<String>) config.getStringList("portals." + portalNumber + "." + activator + ".activators");
        ArrayList<Vector> activatorVecs = new ArrayList<>();
        for (String vectorString : activatorStrings) {
            String[] parts = vectorString.split(",");
            if (parts.length != 3) {
                BuildPortals.logger.severe("Error reading activator vectors from configuration for portal " + portalNumber);
                return null;
            }
            Vector vec = new Vector();
            vec.setX(Double.parseDouble(parts[0]));
            vec.setY(Double.parseDouble(parts[1]));
            vec.setZ(Double.parseDouble(parts[2]));
            activatorVecs.add(vec);
        }
        ActivatedPortalFrame frame = new ActivatedPortalFrame(world, new ArrayList<>(), exteriorVecs, activatorVecs, yaw);
        return new IncompletePortal(
            activator,
            frame
        );
    }

    @Nullable
    public static IncompletePortal getPortalFromActivatorName(String activatorName) {
        return portals.get(activatorName);
    }

    public void saveConfig() {
        String configKey = "portals." + portalNumber + "." + identifier;
        Map<String, Object> portalData = new HashMap<>();
        portalData.put("active", false);
        portalData.put("world", frames[0].world.getName());
        portalData.put("vec", frames[0].interior);
        portalData.put("frame", frames[0].exterior);
        portalData.put("activators", frames[0].activators);
        portalData.put("yaw", Float.toString(frames[0].yaw));
        BuildPortals.config.createSection(configKey, portalData);
        BuildPortals.plugin.saveConfig();
    }
}
