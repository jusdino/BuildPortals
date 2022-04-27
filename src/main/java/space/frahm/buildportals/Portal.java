package space.frahm.buildportals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Boat;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;


public class Portal extends AbstractPortal {
    /* TODO: Refactor this and PortalFrame to only actually record the min and max x/y/z interior vectors.
     * Note that we will have to still read configs with more than that, since we want to maintain backward
     * compatibility. We can save over existing portal configs, however, with a smaller set of portal vectors,
     * so long as we allow more than that to be there on load, reduce to what we care about, and don't change
     * the format of the config file.
     */
    // TODO: Break up all the lengthy methods here, get some better code reuse in there.
    private static HashMap<String, HashSet<Vector>> interiors = new HashMap<String, HashSet<Vector>>();
    private static HashSet<Portal> portals = new HashSet<>();
    protected PortalFrame[] frames = {null, null};

    Portal (
        String identifier,
        PortalFrame[] frames) {
        /*
         * Constructor for a portal object, which includes a collection of
         * vectors representing each of the sides of the portals as well as which
         * world each side is in.
         */
        this.identifier = identifier;
        if (frames.length != 2) {
            BuildPortals.logger.warning("Expected an array of length 2, got length " + frames.length);
        }
        this.frames[0] = frames[0];
        this.frames[1] = frames[1];

        for (PortalFrame frame: this.frames) {
            HashSet<Vector> interiorsInWorld = interiors.getOrDefault(frame.world.getName(), new HashSet<>());
            interiorsInWorld.addAll(frame.interior);
            interiors.put(frame.world.getName(), interiorsInWorld);
        }
        Portal.portals.add(this);
        setExteriorsToMaterial(this.frames);
    }

    Portal (
        IncompletePortal incompletePortal1,
        IncompletePortal incompletePortal2
    ) {
        this.identifier = getFreeIdentifier();
        this.frames[0] = (PortalFrame)incompletePortal1.frames[0];
        this.frames[1] = (PortalFrame)incompletePortal2.frames[0];

        incompletePortal1.link();
        incompletePortal2.link();

        interiors.get(frames[0].world.getName()).addAll(frames[0].interior);
        interiors.get(frames[1].world.getName()).addAll(frames[1].interior);
        Portal.portals.add(this);
        setExteriorsToMaterial(this.frames);
    }

    public static boolean isInAPortal(Location loc) {
        return isInAPortal(interiors, loc);
    }

    @Nullable
    public static Portal getPortalFromLocation(Location loc) {
        /* Take the provided location and find a matching portal
        * (if any). Return null for no match.
        */
        if (!isInAPortal(loc)) {
            return null;
        }
        for (Portal portal : portals) {
            if (portal.isInPortal(loc, portal.frames)) {
                return portal;
            }
        }
        return null;
    }

    public static void loadPortalsFromConfig() {
        /* Read the plugin configs to instantiate a Portal object for each listed
        * and add them to the static HashSet.
        */
        Portal.portals = new HashSet<>();
        Portal.interiors = new HashMap<>();
        FileConfiguration config = BuildPortals.config;

        Material mat = Material.getMaterial(BuildPortals.config.getString("PortalMaterial"));
        if (mat == null) {
            BuildPortals.logger.warning("Could not read configured portal material! Aborting portal update.");
            return;
        }
        
        ConfigurationSection portalSection = config.getConfigurationSection("portals");
        if (portalSection == null) {
            BuildPortals.logger.info("No portals data in configurations.");
            return;
        }

        Set<String> portalKeys = portalSection.getKeys(false);
        for (String portalNumber : portalKeys) {
            if (Integer.parseInt(portalNumber) != 0) {
                try {
                    Portal.loadFromConfig(portalNumber);
                } catch (InvalidConfigurationException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    public void teleport(Entity entity) {
        /* Teleport the provided entity to the other side of this portal, based on their
        * current location.
        */
        if (! this.integrityCheck(this.frames)) {
            this.destroy();
            return;
        }
        Location destination = this.getDestination(entity, entity.getLocation());
        if (destination != null) {
            Teleporter.teleport(entity, destination);
        }
    }

    protected void destroy() {
        super.destroy(this.frames);
        // Remove this portal from the static set
        portals.remove(this);

        // Remove the frame vectors from the static set
        for (PortalFrame frame : this.frames) {
            HashSet<Vector> interiorVecs = interiors.get(frame.world.getName());
            for (Vector interiorVec: frame.interior) {
                interiorVecs.remove(interiorVec);
            }
        }

        // Clear the config entry and save
        BuildPortals.config.set("portals." + this.identifier, null);
        BuildPortals.plugin.saveConfig();
    }

    public static Portal loadFromConfig(String portalNumber) throws InvalidConfigurationException {
        FileConfiguration config = BuildPortals.config;
        PortalFrame[] frames = {null, null};

        if (Integer.parseInt(portalNumber) == 0) {
            BuildPortals.logger.severe("Attempted to instantiate a Portal with number 0, which is reserved for IncompletePortals");
            throw new InvalidConfigurationException();
            // TODO: should probably throw an exception here
            // return null;
        }
        Map<Integer, String> indexMap = Map.of(
            0, "A",
            1, "B"
        );
        BuildPortals.logger.info("Loading configuration for portal number: " + portalNumber);
        for (Map.Entry<Integer, String> entry: indexMap.entrySet()) {
            ArrayList<String> interiorStrings;
            ArrayList<String> exteriorStrings;

            String worldName = config.getString("portals." + portalNumber + "." + entry.getValue() + ".world");
            if (worldName == null) {
                BuildPortals.logger.severe("Error reading World name configuration!");
                return null;
            }
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                BuildPortals.logger.warning("Failed to locate world: " + worldName + " skipping portal number " + portalNumber);
                return null;
            }
            String yawString = config.getString("portals." + portalNumber + "." + entry.getValue() + ".yaw");
            if (yawString == null) {
                BuildPortals.logger.info("Error reading yaw A from configuration!");
                return null;
            }
            Float yaw = Float.parseFloat(yawString);

            // Convert string lists to vector lists
            interiorStrings = (ArrayList<String>) config.getStringList("portals." + portalNumber + "." + entry.getValue() + ".vec");

            // Interior vectors
            ArrayList<Vector> newInteriors = new ArrayList<>();
            for (String interiorString : interiorStrings) {
                String[] parts = interiorString.split(",");
                if (parts.length != 3) {
                    BuildPortals.logger.severe("Error reading portal data!");
                    return null;
                }
                Vector vec = new Vector(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
                newInteriors.add(vec);
            }

            // Exterior vectors
            exteriorStrings = (ArrayList<String>) config.getStringList("portals." + portalNumber + "." + entry.getValue() + ".frame");
            ArrayList<Vector> exteriors = new ArrayList<>();

            for (String vectorString : exteriorStrings) {
                String[] parts = vectorString.split(",");
                if (parts.length != 3) {
                    BuildPortals.logger.severe("Error reading frame data!");
                    return null;
                }
                Vector vec = new Vector(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
                exteriors.add(vec);
            }
            frames[entry.getKey()] = new PortalFrame(world, newInteriors, exteriors, yaw);
        }
        return new Portal(portalNumber, frames);
    }

    Location getDestination(Entity entity, Location sourceLoc) {
        /*
         * Returns the location of the portal destination that corresponds
         * to the player's location.
         *
         * Returns Null if the location is not actually in the portal.
         */
        Vector flooredSourceVec = new Vector(sourceLoc.getBlockX(), sourceLoc.getBlockY(), sourceLoc.getBlockZ());
        Vector sourceVec = new Vector(sourceLoc.getX(), sourceLoc.getY(), sourceLoc.getZ());

        // Move source to center of block
        sourceVec.add(blockCenterOffset);

        // Define source and destination vectors
        PortalFrame sourceFrame = null;
        PortalFrame destFrame = null;
        for (PortalFrame thisOne: this.frames) {
            if (sourceLoc.getWorld() == thisOne.world && thisOne.interior.contains(flooredSourceVec)) {
                sourceFrame = thisOne;
            } else {
                destFrame = thisOne;
            }
        }
        if (sourceFrame == null) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Somehow, the source location is in neither portal side!");
            return null;
        }
        Vector forwardVec;
        Vector backwardVec;
        // Define a unit vector in the destination Yaw direction
        switch (destFrame.yaw.intValue()) {
            // South
            case 0:
                forwardVec = new Vector(0, 0, 1);
                backwardVec = new Vector(0, 0, -1);
                break;
            // West
            case 90:
                forwardVec = new Vector(-1, 0, 0);
                backwardVec = new Vector(1, 0, 0);
                break;
            // North
            case 180:
                forwardVec = new Vector(0, 0, -1);
                backwardVec = new Vector(0, 0, 1);
                break;
            // East
            default:
                forwardVec = new Vector(1, 0, 0);
                backwardVec = new Vector(-1, 0, 0);
                break;
        }

        // Count non-solid blocks next to portal blocks
        Location portalLoc;
        Block forwardBlock;
        Block backwardBlock;
        int forwardNonSolidCount = 0;
        int backwardNonSolidCount = 0;
        for (Vector portalVec: destFrame.interior) {
            portalLoc = portalVec.toLocation(destFrame.world);
            forwardBlock = destFrame.world.getBlockAt(portalLoc.getBlockX() + forwardVec.getBlockX(), portalLoc.getBlockY() + forwardVec.getBlockY(), portalLoc.getBlockZ() + forwardVec.getBlockZ());
            backwardBlock = destFrame.world.getBlockAt(portalLoc.getBlockX() + backwardVec.getBlockX(), portalLoc.getBlockY() + backwardVec.getBlockY(), portalLoc.getBlockZ() + backwardVec.getBlockZ());
            if (!forwardBlock.getType().isSolid()) {
                forwardNonSolidCount += 1;
            }
            if (!backwardBlock.getType().isSolid()) {
                backwardNonSolidCount += 1;
            }

        }
        // If 'backwards' face has more non-solid blocks, turn the Yaw around.
        Float destYaw = destFrame.yaw;
        if (backwardNonSolidCount > forwardNonSolidCount) {
            if (destYaw < 180F) {
                destYaw += 360F;
            }
            destYaw -= 180F;
        }

        Integer sourceXmin = null;
        Integer sourceXmax = null;
        Integer sourceYmin = null;
        Integer sourceYmax = null;
        Integer sourceZmin = null;
        Integer sourceZmax = null;

        for (Vector vec: sourceFrame.interior) {
            if (sourceXmax == null || vec.getBlockX() > sourceXmax) {
                sourceXmax = vec.getBlockX();
            }
            if (sourceXmin == null || vec.getBlockX() < sourceXmin) {
                sourceXmin = vec.getBlockX();
            }
            if (sourceYmax == null || vec.getBlockY() > sourceYmax) {
                sourceYmax = vec.getBlockY();
            }
            if (sourceYmin == null || vec.getBlockY() < sourceYmin) {
                sourceYmin = vec.getBlockY();
            }
            if (sourceZmax == null || vec.getBlockZ() > sourceZmax) {
                sourceZmax = vec.getBlockZ();
            }
            if (sourceZmin == null || vec.getBlockZ() < sourceZmin) {
                sourceZmin = vec.getBlockZ();
            }
            
        }

        Integer destXmin = null;
        Integer destXmax = null;
        Integer destYmin = null;
        Integer destYmax = null;
        Integer destZmin = null;
        Integer destZmax = null;

        for (Vector vec: destFrame.interior) {
            if (destXmax == null || vec.getBlockX() > destXmax) {
                destXmax = vec.getBlockX();
            }
            if (destXmin == null || vec.getBlockX() < destXmin) {
                destXmin = vec.getBlockX();
            }
            if (destYmax == null || vec.getBlockY() > destYmax) {
                destYmax = vec.getBlockY();
            }
            if (destYmin == null || vec.getBlockY() < destYmin) {
                destYmin = vec.getBlockY();
            }
            if (destZmax == null || vec.getBlockZ() > destZmax) {
                destZmax = vec.getBlockZ();
            }
            if (destZmin == null || vec.getBlockZ() < destZmin) {
                destZmin = vec.getBlockZ();
            }
            
        }

        // Measure dimensions
        double sourceXwidth;
        double sourceZwidth;
        double destXwidth;
        double destZwidth;
        double sourceTmp;

        // Some source / destination refinements to give a margin inside the portal frame
        double yMargin = 2D;
        double zMargin = 0.3;
        double xMargin = 0.3;
        if (entity instanceof AbstractHorse || entity instanceof Boat) {
            xMargin = 1D;
            zMargin = 1D;
        }

        // Swap source z/x if portals are in different orientations
        if (!sourceFrame.yaw.equals(destFrame.yaw)) {
            // If source is open North/South, dest is open East/West
            if ( sourceFrame.yaw == 0F || sourceFrame.yaw == 180F) {
                xMargin = 0.5;
                sourceXwidth = 0D;
                sourceZwidth = sourceXmax - sourceXmin + 1 - 2*zMargin;
                destXwidth = 0D;
                destZwidth = destZmax - destZmin + 1 - 2*zMargin;
                // If portal is open East/West, dest is open North/South
            } else {
                zMargin = 0.5;
                sourceXwidth = sourceZmax - sourceZmin + 1 - 2*xMargin;
                sourceZwidth = 0D;
                destXwidth = destXmax - destXmin + 1 - 2*xMargin;
                destZwidth = 0D;
            }

            // Adjust sourceVec to an offset from min x/y/z locations
            sourceVec.subtract(new Vector(sourceXmin + zMargin, sourceYmin, sourceZmin + xMargin));
            // Then swap X/Z
            sourceTmp = sourceVec.getZ();
            sourceVec.setZ(sourceVec.getX());
            sourceVec.setX(sourceTmp);
        } else {
            // If source and dest are open North/South
            if ( sourceFrame.yaw == 0F || sourceFrame.yaw == 180F ){
                zMargin = 0.5;
                sourceXwidth = sourceXmax - sourceXmin + 1 - 2*xMargin;
                sourceZwidth = 0D;
                destXwidth = destXmax - destXmin + 1 - 2*xMargin;
                destZwidth = 0D;
                // If source and dest are open East/West
            } else {
                xMargin = 0.5;
                sourceXwidth = 0D;
                sourceZwidth = sourceZmax - sourceZmin + 1 - 2*zMargin;
                destXwidth = 0D;
                destZwidth = destZmax - destZmin + 1 - 2*zMargin;
            }
            // Adjust sourceVec to an offset from min x/y/z locations
            sourceVec.subtract(new Vector(sourceXmin + xMargin, sourceYmin, sourceZmin + zMargin));
        }
        double sourceHeight = sourceYmax - sourceYmin + 1 - yMargin;
        double destHeight = destYmax - destYmin + 1 - yMargin;

        // Shift sourceVec to be sure it is in sourcePortal minus margins
        if (sourceVec.getX() < 0) {
            sourceVec.setX(0D);
        } else if (sourceVec.getX() > sourceXwidth) {
            sourceVec.setX(sourceXwidth);
        }
        if (sourceVec.getY() < 0) {
            sourceVec.setY(0);
        } else if (sourceVec.getY() > sourceHeight) {
            sourceVec.setY(sourceHeight);
        }
        if (sourceVec.getZ() < 0) {
            sourceVec.setZ(0D);
        } else if (sourceVec.getZ() > sourceZwidth) {
            sourceVec.setZ(sourceZwidth);
        }

        // Bail if a portal is too small
        if (sourceHeight < 0 || sourceZwidth < 0 || sourceXwidth < 0 || destHeight < 0 || destZwidth < 0 || destXwidth < 0) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Portal is too small");
            return null;
        }
        // Map location in source portal to location in dest portal
        Vector destVec = new Vector();
        if (sourceXwidth > 0) {
            destVec.setX(xMargin + (sourceVec.getX()/sourceXwidth) * destXwidth);
        } else {
            destVec.setX(xMargin);
        }
        if (sourceHeight > 0) {
            destVec.setY( (sourceVec.getY()/sourceHeight) * destHeight);
        } else {
            destVec.setY(0);
        }
        if (sourceZwidth > 0) {
            destVec.setZ(zMargin +  (sourceVec.getZ()/sourceZwidth) * destZwidth);
        } else {
            destVec.setZ(zMargin);
        }
        Location destLoc = new Location(destFrame.world, destVec.getX(), destVec.getY(), destVec.getZ(), destYaw, 0F);
        destLoc.add(new Vector(destXmin, destYmin, destZmin));

        return destLoc;
    }

    private String getFreeIdentifier() {
        HashSet<String> portalIdentifiers = new HashSet<>();
        for (Portal portal : portals) {
            portalIdentifiers.add(portal.identifier);
        }
        int i = 1;
        while (portalIdentifiers.contains(Integer.toString(i))) {
            i+=1;
        }
        return Integer.toString(i);
    }

    public void saveConfig() {
        String configKey = "portals." + identifier;
        Map<String, Object> portalData = new HashMap<>();
        portalData.put("active", true);
        portalData.put("A.world", frames[0].world.getName());
        portalData.put("A.vec", frames[0].interior);
        portalData.put("A.frame", frames[0].exterior);
        portalData.put("A.yaw", Float.toString(frames[0].yaw));
        portalData.put("B.world", frames[1].world.getName());
        portalData.put("B.vec", frames[1].interior);
        portalData.put("B.frame", frames[1].exterior);
        portalData.put("B.yaw", Float.toString(frames[1].yaw));
        BuildPortals.config.createSection(configKey, portalData);
        BuildPortals.plugin.saveConfig();
    }
}