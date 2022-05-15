package space.frahm.buildportals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.util.Vector;

public abstract class AbstractPortal {
    protected static final Vector blockCenterOffset = new Vector(0.5, 0, 0.5);
    protected String identifier;

    /* Read the plugin configs to instantiate a Portal object for each listed
     * and add them to the static HashSet.
     * 
     * Note:: Java is super lame and won't allow abstract static methods, so I'm
     * making a 'static abstract' method as a comment here just to remind myself
     * that I want subclasses to implement this method.
     */
    // public abstract void loadPortalsFromConfig();

    /* Inspect the portal materials to determine whether this portal is still intact.
     * Runs through each portal frame block and activator block, checks that they are still the
     * correct material. Returns true for intact, false for not.
     */
    protected boolean integrityCheck(PortalFrame[] frames) {
        for (PortalFrame frame : frames) {
            String frameMaterialName = BuildPortals.config.getString("PortalMaterial");
            BuildPortals.logger.log(BuildPortals.logLevel, "Checking portal frames");
            for (Vector vec : frame.exterior) {
                Location loc = new Location(frame.world, vec.getX(), vec.getY(), vec.getZ());
                if ( ! loc.getBlock().getType().name().equals(Material.getMaterial(frameMaterialName).name())) {
                    BuildPortals.logger.log(BuildPortals.logLevel, "Block is not frame material at " + loc.toString());
                    return false;
                }
            }
        }
        return true;
    }

    /* Make a visible effect to indicate the portal has been destroyed, then
    * update the plugin config to remove this portal.
    */
    protected void destroy(PortalFrame[] frames) {
        for (PortalFrame frame : frames) {
            if (frame.interior.size() > 0) {
                Vector vec = frame.interior.get(0);
                Location loc = new Location(frame.world, vec.getX(), vec.getY(), vec.getZ());
                loc.getWorld().strikeLightningEffect(loc);
                loc.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, loc, 1);
            }
        }
    }

    /* Another 'abstract static method'
     * Instantiate an AbstractPortal instance from a config entry
     */
    //public abstract Portal loadFromConfig(String portalNumber);
    protected void setExteriorsToMaterial(PortalFrame[] frames) {
        // Set frames to configured frame material
        Material mat = Material.getMaterial(BuildPortals.config.getString("PortalMaterial"));
        for (PortalFrame frame: frames) {
            for (Vector frameVec: frame.exterior) {
                Block block = new Location(frame.world, frameVec.getX(), frameVec.getY(), frameVec.getZ()).getBlock();
                block.setType(mat);
            }
        }
    }

    @Nullable
    public static boolean isInAPortal(HashMap<String, HashSet<Vector>> interiors, Location loc) {
        /* This method will be called a lot on *MoveEvents so we want it to be very fast.
        * We maintain a HashSet of all portal blocks just so that we can return the
        * negative case as quickly as possible.
        */
        String worldName = loc.getWorld().getName();
        if (!interiors.containsKey(worldName)){
            return false;
        }
        Location flooredLoc = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        if (!interiors.get(worldName).contains(flooredLoc.toVector())) {
            return false;
        }
        return true;
    }

    public boolean isInPortal(Location loc, PortalFrame[] frames) {
        /* Return true if given location is in this portal */
        World world = loc.getWorld();
        Vector vec = new Vector(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        for (PortalFrame frame: frames) {
            if (world == frame.world && frame.interior.contains(vec)) {
                return true;
            }
        }
        return false;
    }

    protected static Vector vecFromConfigString(String configString) throws InvalidConfigurationException {
        String[] parts = configString.split(",");
        if (parts.length != 3) {
            throw new InvalidConfigurationException("Invalid vector string: " + configString);
        }
        return new Vector(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
    }

    protected static String configStringFromVec(Vector vec) {
        return vec.getX() + "," + vec.getY() + "," + vec.getZ();
    }

    protected static ArrayList<String> configArrayListVecs(ArrayList<Vector> vecs) {
        ArrayList<String> strings = new ArrayList<>();
        vecs.forEach((vec) -> strings.add(configStringFromVec(vec)));
        return strings;
    }
}