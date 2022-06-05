package space.frahm.buildportals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
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

    public IncompletePortal(
        String identifier,
        ActivatedPortalFrame frame
    ) {
        this.identifier = identifier;
        this.frames[0] = frame;

        // Update the static HashSet for frame.world with new interior vectors
        HashSet<Vector> interiorsInWorld = interiors.getOrDefault(frame.world.getName(), new HashSet<>());
        interiorsInWorld.addAll(frame.interior);
        interiors.put(frame.world.getName(), interiorsInWorld);
        portals.put(identifier, this);
    }

    public static IncompletePortal getNewPortalFromBlock(Block block) throws InvalidConfigurationException {
        ActivatedPortalFrame frame = ActivatedPortalFrame.getCompletePortalVectors(block);
        if (frame == null) {
            return null;
        }
        // Make a pretty particle effect to tell the user they got it right
        Random rand = new Random();
        int count = frame.interior.size()*100;
        if (count > 500) {
                count = 500;
        }
        int spread = frame.interior.size();
        for (int j=0; j<count; j++) {
            Location particleLoc = new Location(block.getWorld(), block.getX() + (rand.nextDouble() * spread), block.getY() + (rand.nextDouble() * spread), block.getZ() + (rand.nextDouble() * spread));
            block.getWorld().spawnParticle(Particle.CRIT_MAGIC, particleLoc, 1);
        }
        return new IncompletePortal(block.getType().name(), frame);
    }

    public static void loadPortalsFromConfig() {
        IncompletePortal.interiors = new HashMap<>();
        IncompletePortal.portals = new HashMap<>();
        for (Material mat : BuildPortals.activatorMaterials) {
            loadFromConfig(mat.name());
        }
    }

    public static boolean isInAPortal(Location loc) {
        return isInAPortal(interiors, loc);
    }

    protected void destroy() {
        super.destroy(this.frames);
        clear();
    }

    protected void link() {
        if (this.frames[0].interior.size() > 0) {
            Vector vec = frames[0].interior.get(0);
            Location loc = new Location(frames[0].world, vec.getX(), vec.getY(), vec.getZ());
            loc.getWorld().strikeLightningEffect(loc);
        }
        Location loc;
        for (Vector vec : frames[0].interior) {
            loc = new Location(frames[0].world, vec.getX(), vec.getY(), vec.getZ());
            loc.getBlock().setType(Material.AIR);
        }
        clear();
    }

    protected void clear() {
        // Remove this portal from the static set
        portals.remove(this.identifier);
        // Remove the frame vectors from the static set
        for (PortalFrame frame : this.frames) {
            HashSet<Vector> interiorVecs = interiors.get(frame.world.getName());
            for (Vector interiorVec: frame.interior) {
                interiorVecs.remove(interiorVec);
            }
        }

        // Clear the config entry and save
        BuildPortals.config.set("portals." + portalNumber + "." + this.identifier, null);
        BuildPortals.plugin.saveConfig();
    }

    @Nullable
    public static IncompletePortal loadFromConfig(String activatorName) {
        ConfigurationSection portalSection = BuildPortals.config.getConfigurationSection("portals." + portalNumber + "." + activatorName);
        if (portalSection == null) {
            return null;
        }
        BuildPortals.logger.info("Loading configuration for incomplete portal: " + activatorName);
        boolean active = portalSection.getBoolean("active");
        if (!active) {
            return null;
        }
        String worldName = portalSection.getString("world");
        if (worldName == null) {
            BuildPortals.logger.severe("Error reading world from configuration for portal " + portalNumber);
            return null;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            BuildPortals.logger.warning("Failed to locate world: " + worldName + " skipping portal number " + portalNumber);
            return null;
        }
        String yawString = portalSection.getString("yaw");
        if (yawString == null) {
            BuildPortals.logger.severe("Error reading yaw from configuration for portal " + portalNumber);
            return null;
        }
        Float yaw = Float.parseFloat(yawString);

        // Exteriors
        ArrayList<String> exteriorStrings = (ArrayList<String>) portalSection.getStringList("frame");
        ArrayList<Vector> exteriorVecs = new ArrayList<>();
        for (String exteriorString : exteriorStrings) {
            try {
                exteriorVecs.add(vecFromConfigString(exteriorString));
                
            } catch (InvalidConfigurationException e) {
                BuildPortals.logger.severe("Error reading frame vectors from configuration for portal " + portalNumber);
                return null;
            }
        }

        // Activators
        ArrayList<String> activatorStrings = (ArrayList<String>) portalSection.getStringList("activators");
        ArrayList<Vector> activatorVecs = new ArrayList<>();
        for (String vectorString : activatorStrings) {
            try {
                activatorVecs.add(vecFromConfigString(vectorString));
                
            } catch (InvalidConfigurationException e) {
                BuildPortals.logger.severe("Error reading activator vectors from configuration for portal " + portalNumber);
                return null;
            }
        }
        ActivatedPortalFrame frame = new ActivatedPortalFrame(world, new ArrayList<>(), exteriorVecs, activatorVecs, yaw);
        return new IncompletePortal(
            activatorName,
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
        portalData.put("vec", configArrayListVecs(frames[0].interior));
        portalData.put("frame", configArrayListVecs(frames[0].exterior));
        portalData.put("activators", configArrayListVecs(frames[0].activators));
        portalData.put("yaw", Float.toString(frames[0].yaw));
        BuildPortals.config.createSection(configKey, portalData);
        BuildPortals.plugin.saveConfig();
    }
}
