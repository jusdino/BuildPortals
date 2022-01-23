package space.frahm.buildportals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Boat;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;

public class Portal {
    /* TODO: Split this up into at least:
       - AbstractPortal
         - Common ancestor for In/Complete
       - IncompletePortal
         - Has only one side portal/frame/activator blocks
       - CompletePortal
         - Has Array[2] portal/frame blocks
     */
    /* TODO: Clean up all the lengthy methods here, get some better code reuse in there. */
    private static HashMap<String, HashSet<Vector>> portalBlocks = new HashMap<String, HashSet<Vector>>();
    private static HashSet<Portal> portals = new HashSet<>();
    private static final Vector blockCenterOffset = new Vector(0.5, 0, 0.5);

    private ArrayList<Vector> vectorsA;
    private ArrayList<Vector> vectorsB;
    private ArrayList<Vector> frameVecsA;
    private ArrayList<Vector> frameVecsB;
    private ArrayList<Vector> activatorVecsA;
    private World aWorld;
    private World bWorld;
    private Float yawA;
    private Float yawB;
    private String identifier;

    Portal (
        String identifier,
        World aWorld,
        ArrayList<Vector> vectorsA,
        ArrayList<Vector> frameVecsA,
        ArrayList<Vector> activatorVecsA,
        Float yawA,
        World bWorld,
        ArrayList<Vector> vectorsB,
        ArrayList<Vector> frameVecsB,
        Float yawB) {
        /*
         * Constructor for a portal object, which includes a collection of
         * vectors representing sides A and B of the portals as well as which
         * world each side is in.
         */
        this.identifier = identifier;
        this.aWorld = aWorld;
        this.vectorsA = vectorsA;
        this.frameVecsA = frameVecsA;
        this.activatorVecsA = activatorVecsA;
        this.yawA = yawA;
        this.bWorld = bWorld;
        this.vectorsB = vectorsB;
        this.frameVecsB = frameVecsB;
        this.yawB = yawB;

        // Set frames to configured frame material
        Material mat = Material.getMaterial(BuildPortals.config.getString("PortalMaterial"));
        for (Vector frameVec : this.frameVecsA) {
            Block block = new Location(this.aWorld, frameVec.getX(), frameVec.getY(), frameVec.getZ()).getBlock();
            block.setType(mat);
        }
        if (this.bWorld != null) {
            for (Vector frameVec : this.frameVecsB) {
                Block block = new Location(this.bWorld, frameVec.getX(), frameVec.getY(), frameVec.getZ()).getBlock();
                block.setType(mat);
            }
        }
    }

    public static void loadPortalsFromConfig() {
        /* Read the plugin configs to instantiate a Portal object for each listed
        * and add them to the static HashSet.
        */
        portals = new HashSet<>();
        portalBlocks = new HashMap<>();
        FileConfiguration config = BuildPortals.config;

        Material mat = Material.getMaterial(config.getString("PortalMaterial"));
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
            Portal portal = Portal.loadFromConfig(portalNumber);
            if (portal != null) {
                portals.add(portal);
            }
        }
    }

    @Nullable
    public static Portal getPortalFromLocation(Location loc) {
        /* Take the provided location and find a matching portal
        * (if any). Return null for no match.
        */
        for (Portal portal : Portal.portals) {
            if (portal.isInPortal(loc)) {
                return portal;
            }
        }
        return null;
    }

    @Nullable
    public static Portal isInAPortal(Location loc) {
        /* This method will be called a lot on *MoveEvents so we want it to be very fast.
        * We maintain a HashSet of all portal blocks just so that we can return the
        * negative case as quickly as possible.
        */
        if (!portalBlocks.containsKey(loc.getWorld().getName())){
            return null;
        }
        Location flooredLoc = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        if (!portalBlocks.get(flooredLoc.getWorld().getName()).contains(flooredLoc.toVector())) {
            return null;
        }
        return Portal.getPortalFromLocation(flooredLoc);
    }

    public boolean isInPortal(Location loc) {
        /* Return true if given location is in this portal */
        World world = loc.getWorld();
        Vector vec = new Vector(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        if (world == aWorld && vectorsA.contains(vec)) {
            return true;
        }
        if (world == bWorld && vectorsB.contains(vec)) {
            return true;
        }
        return false;
    }

    public void teleport(Entity entity) {
        /* Teleport the provided entity to the other side of this portal, based on their
        * current location.
        */
        if (! this.integrityCheck()) {
            this.destroy();
            return;
        }
        Location destination = this.getDestination(entity, entity.getLocation());
        if (destination != null) {
            Teleporter.teleport(entity, destination);
        }
    }

    private boolean integrityCheck() {
        /* Inspect the portal materials to determine whether this portal is still intact.
         * Runs through each portal frame block and activator block, checks that they are still the
         * correct material and if not, returns the portal number of the first incorrect
         * material block it finds.
         */
        String frameMaterialName = BuildPortals.config.getString("PortalMaterial");
        BuildPortals.logger.log(BuildPortals.logLevel, "Checking portal frames");
        for (Vector vec : frameVecsA) {
            Location loc = new Location(aWorld, vec.getX(), vec.getY(), vec.getZ());
            if ( ! loc.getBlock().getType().name().equals(Material.getMaterial(frameMaterialName).name())) {
                BuildPortals.logger.log(BuildPortals.logLevel, "Block is not frame material at " + loc.toString());
                return false;
            }
        }
        for (Vector vec : frameVecsB) {
            Location loc = new Location(bWorld, vec.getX(), vec.getY(), vec.getZ());
            if ( ! loc.getBlock().getType().name().equals(Material.getMaterial(frameMaterialName).name())) {
                BuildPortals.logger.log(BuildPortals.logLevel, "Block is not frame material at " + loc.toString());
                return false;
            }
        }
        ArrayList<String> activators = (ArrayList<String>) BuildPortals.config.getStringList("PortalActivators");
        for (Vector vec : activatorVecsA) {
            Location loc = new Location(bWorld, vec.getX(), vec.getY(), vec.getZ());
            if (!activators.contains(loc.getBlock().getType().name())) {
                BuildPortals.logger.log(BuildPortals.logLevel, "Bad activator found at " + loc.toVector().toString() + "!");
                return false;
            }
        }
        return true;
    }

    private void destroy() {
        /* Make a visible effect to indicate the portal has been destroyed, then
        * update the plugin config to remove this portal.
        */
        if (this.vectorsA.size() > 0) {
            Vector vec = this.vectorsA.get(0);
            Location loc = new Location(this.aWorld, vec.getX(), vec.getY(), vec.getZ());
            loc.getWorld().strikeLightningEffect(loc);
            loc.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, loc, 1);
        }
        if (this.vectorsB.size() > 0) {
            Vector vec = this.vectorsB.get(0);
            Location loc = new Location(this.bWorld, vec.getX(), vec.getY(), vec.getZ());
            loc.getWorld().strikeLightningEffect(loc);
            loc.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, loc, 1);
        }
        BuildPortals.logger.info("Clearing portal number " + this.identifier);
        // Remove this portal from the static set
        portals.remove(this);

        // Remove the frame vectors from the static set
        HashSet<Vector> frameVecs = portalBlocks.get(aWorld.getName());
        for (Vector destroyedVec: frameVecsA) {
            frameVecs.remove(destroyedVec);
        }
        frameVecs = portalBlocks.get(bWorld.getName());
        for (Vector destroyedVec: frameVecsB) {
            frameVecs.remove(destroyedVec);
        }

        // Clear the config entry and save
        BuildPortals.config.set("portals." + this.identifier, null);
        BuildPortals.plugin.saveConfig();
    }

    public static Portal loadFromConfig(String portalNumber) {
        FileConfiguration config = BuildPortals.config;

        ArrayList<String> activators = BuildPortals.activatorMaterialNames;
        World worldA;
        ArrayList<String> vectorStringsA;
        ArrayList<String> frameStringsA;
        ArrayList<String> activatorStringsA;
        Float yawA;
        World worldB;
        ArrayList<String> vectorStringsB;
        ArrayList<String> frameStringsB;
        Float yawB;
        HashSet<Vector> tempVecSet = new HashSet<>();

        Portal new_portal = null;

        if (Integer.parseInt(portalNumber) != 0) {
            BuildPortals.logger.info("Loading configuration for portal number: " + portalNumber);
            vectorStringsA = (ArrayList<String>) config.getStringList("portals." + portalNumber + ".A.vec");
            frameStringsA = (ArrayList<String>) config.getStringList("portals." + portalNumber + ".A.frame");
            vectorStringsB = (ArrayList<String>) config.getStringList("portals." + portalNumber + ".B.vec");
            frameStringsB = (ArrayList<String>) config.getStringList("portals." + portalNumber + ".B.frame");

            ArrayList<Vector> vectorsA = new ArrayList<>();
            ArrayList<Vector> vectorsB = new ArrayList<>();
            ArrayList<Vector> frameVecsA = new ArrayList<>();
            ArrayList<Vector> frameVecsB = new ArrayList<>();

            String worldAName = config.getString("portals." + portalNumber + ".A.world");
            String yawAString = config.getString("portals." + portalNumber + ".A.yaw");

            if (worldAName == null) {
                BuildPortals.logger.severe("Error reading World A Name configuration!");
                return null;
            }
            worldA = Bukkit.getWorld(worldAName);
            if (worldA == null) {
                BuildPortals.logger.warning("Failed to locate world: " + worldAName + " skipping portal number " + portalNumber);
                return null;
            }
            if (yawAString == null) {
                BuildPortals.logger.info("Error reading yawA from configuration!");
                return null;
            }
            yawA = Float.parseFloat(yawAString);

            String worldBName = config.getString("portals." + portalNumber + ".B.world");
            String yawBString = config.getString("portals." + portalNumber + ".B.yaw");
            if (worldBName == null) {
                BuildPortals.logger.severe("Error reading World B Name configuration!");
                return null;
            }
            worldB = Bukkit.getWorld(config.getString("portals." + portalNumber + ".B.world"));
            if (worldB == null) {
                BuildPortals.logger.warning("Failed to locate world: " + worldBName + " skipping portal number " + portalNumber);
                return null;
            }
            if (yawBString == null) {
                BuildPortals.logger.severe("Error reading yawB from configuration!");
                return null;
            }
            yawB = Float.parseFloat(yawBString);
            // Convert string lists for A and B to vector lists
            // Side A vectors
            for (String vectorString : vectorStringsA) {
                String[] parts = vectorString.split(",");
                if (parts.length != 3) {
                    BuildPortals.logger.severe("Error reading portal data!");
                    return null;
                }
                Vector vec = new Vector();
                vec.setX(Double.parseDouble(parts[0]));
                vec.setY(Double.parseDouble(parts[1]));
                vec.setZ(Double.parseDouble(parts[2]));
                vectorsA.add(vec);
                // Add this vector to the portalBlocks set, keyed by world
                if (Portal.portalBlocks.containsKey(worldA.getName())) {
                    Portal.portalBlocks.get(worldA.getName()).add(vec);
                } else {
                    tempVecSet.add(vec);
                    Portal.portalBlocks.put(worldA.getName(), tempVecSet);
                    tempVecSet = new HashSet<>();
                }
            }
            // Side A frame vectors
            for (String vectorString : frameStringsA) {
                String[] parts = vectorString.split(",");
                if (parts.length != 3) {
                    BuildPortals.logger.severe("Error reading frame data!");
                    return null;
                }
                Vector vec = new Vector();
                vec.setX(Double.parseDouble(parts[0]));
                vec.setY(Double.parseDouble(parts[1]));
                vec.setZ(Double.parseDouble(parts[2]));
                frameVecsA.add(vec);
            }

            // Side B vectors
            for (String vectorString : vectorStringsB) {
                String[] parts = vectorString.split(",");
                if (parts.length != 3) {
                    BuildPortals.logger.severe("Error reading portal data!");
                    return null;
                }
                Vector vec = new Vector();
                vec.setX(Double.parseDouble(parts[0]));
                vec.setY(Double.parseDouble(parts[1]));
                vec.setZ(Double.parseDouble(parts[2]));
                vectorsB.add(vec);
                // Add this vector to the portalBlocks set, keyed by world
                if (Portal.portalBlocks.containsKey(worldB.getName())) {
                    Portal.portalBlocks.get(worldB.getName()).add(vec);
                } else {
                    tempVecSet.add(vec);
                    Portal.portalBlocks.put(worldB.getName(), tempVecSet);
                    tempVecSet = new HashSet<>();
                }
            }
            // Side B frame vectors
            for (String vectorString : frameStringsB) {
                String[] parts = vectorString.split(",");
                if (parts.length != 3) {
                    BuildPortals.logger.severe("Error reading frame data!");
                    return null;
                }
                Vector vec = new Vector();
                vec.setX(Double.parseDouble(parts[0]));
                vec.setY(Double.parseDouble(parts[1]));
                vec.setZ(Double.parseDouble(parts[2]));
                frameVecsB.add(vec);
            }
            new_portal = new Portal(portalNumber, worldA, vectorsA, frameVecsA, new ArrayList<>(), yawA, worldB, vectorsB, frameVecsB, yawB);
        } else { //portalNumber = 0
            for (String activator : activators) {
                frameStringsA = (ArrayList<String>) config.getStringList("portals." + portalNumber + "." + activator + ".frame");
                activatorStringsA = (ArrayList<String>) config.getStringList("portals." + portalNumber + "." + activator + ".activators");

                ArrayList<Vector> frameVecsA = new ArrayList<>();
                ArrayList<Vector> activatorVecsA = new ArrayList<>();

                String worldAName = config.getString("portals." + portalNumber + "." + activator + ".world");
                String yawAString = config.getString("portals." + portalNumber + "." + activator + ".yaw");
                if (worldAName != null) {
                    worldA = Bukkit.getWorld(worldAName);
                    if (worldA == null) {
                        BuildPortals.logger.warning("Failed to locate world: " + worldAName + " skipping portal number " + portalNumber);
                        return null;
                    }
                    if (yawAString == null) {
                        BuildPortals.logger.severe("Error reading yawA from configuration!");
                        return null;
                    }
                    yawA = Float.parseFloat(yawAString);
                    // Convert string lists for A to vector lists
                    // Side A frame vectors
                    for (String vectorString : frameStringsA) {
                        String[] parts = vectorString.split(",");
                        if (parts.length != 3) {
                            BuildPortals.logger.severe("Error reading frame data!");
                            return null;
                        }
                        Vector vec = new Vector();
                        vec.setX(Double.parseDouble(parts[0]));
                        vec.setY(Double.parseDouble(parts[1]));
                        vec.setZ(Double.parseDouble(parts[2]));
                        frameVecsA.add(vec);
                    }
                    // Side A activator vectors
                    for (String vectorString : activatorStringsA) {
                        String[] parts = vectorString.split(",");
                        if (parts.length != 3) {
                            BuildPortals.logger.severe("Error reading frame data!");
                            return null;
                        }
                        Vector vec = new Vector();
                        vec.setX(Double.parseDouble(parts[0]));
                        vec.setY(Double.parseDouble(parts[1]));
                        vec.setZ(Double.parseDouble(parts[2]));
                        activatorVecsA.add(vec);
                    }
                    new_portal = new Portal(portalNumber + "." + activator, worldA, new ArrayList<>(), frameVecsA, activatorVecsA, yawA, null, new ArrayList<>(), new ArrayList<>(), 0F);
                } //WorldAName != null
            } //activator while loop
        } //portalNumber = 0
        return new_portal;
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
        Iterator<Vector> sourceIter;
        Float sourceYaw;
        Iterator<Vector> destIter;
        ArrayList<Vector> destVectors;
        World destWorld;
        Float destYaw;

        // Define source and destination vectors
        // If source is in portal A
        if (sourceLoc.getWorld() == aWorld && vectorsA.contains(flooredSourceVec)) {
            sourceIter = vectorsA.iterator();
            sourceYaw = yawA;
            destVectors = vectorsB;
            destIter = vectorsB.iterator();
            destWorld = bWorld;
            destYaw = yawB;
            // If source is in portal B
        } else if (sourceLoc.getWorld() == bWorld && vectorsB.contains(flooredSourceVec)) {
            sourceIter = vectorsB.iterator();
            sourceYaw = yawB;
            destVectors = vectorsA;
            destIter = vectorsA.iterator();
            destWorld = aWorld;
            destYaw = yawA;
        } else {
            BuildPortals.logger.log(BuildPortals.logLevel, "Somehow, the source location is in neither portal side!");
            return null;
        }
        Vector forwardVec;
        Vector backwardVec;
        // Define a unit vector in the destination Yaw direction
        switch (destYaw.intValue()) {
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
        while (destIter.hasNext()) {
            portalLoc = destIter.next().toLocation(destWorld);
            forwardBlock = destWorld.getBlockAt(portalLoc.getBlockX() + forwardVec.getBlockX(), portalLoc.getBlockY() + forwardVec.getBlockY(), portalLoc.getBlockZ() + forwardVec.getBlockZ());
            backwardBlock = destWorld.getBlockAt(portalLoc.getBlockX() + backwardVec.getBlockX(), portalLoc.getBlockY() + backwardVec.getBlockY(), portalLoc.getBlockZ() + backwardVec.getBlockZ());
            if (!forwardBlock.getType().isSolid()) {
                forwardNonSolidCount += 1;
            }
            if (!backwardBlock.getType().isSolid()) {
                backwardNonSolidCount += 1;
            }
        }
        destIter = destVectors.iterator();
        // If 'backwards' face has more non-solid blocks, turn the Yaw around.
        if (backwardNonSolidCount > forwardNonSolidCount) {
            if (destYaw < 180F) {
                destYaw += 360F;
            }
            destYaw -= 180F;
        }

        int sourceXmin;
        int sourceXmax;
        int sourceYmin;
        int sourceYmax;
        int sourceZmin;
        int sourceZmax;

        Vector vec;
        vec = sourceIter.next();
        sourceXmax = sourceXmin = vec.getBlockX();
        sourceYmax = sourceYmin = vec.getBlockY();
        sourceZmax = sourceZmin = vec.getBlockZ();

        // Get source portal extremes
        while (sourceIter.hasNext()) {
            vec = sourceIter.next();

            if (vec.getBlockX() > sourceXmax) {
                sourceXmax = vec.getBlockX();
            }
            if (vec.getBlockX() < sourceXmin) {
                sourceXmin = vec.getBlockX();
            }
            if (vec.getBlockY() > sourceYmax) {
                sourceYmax = vec.getBlockY();
            }
            if (vec.getBlockY() < sourceYmin) {
                sourceYmin = vec.getBlockY();
            }
            if (vec.getBlockZ() > sourceZmax) {
                sourceZmax = vec.getBlockZ();
            }
            if (vec.getBlockZ() < sourceZmin) {
                sourceZmin = vec.getBlockZ();
            }
        }

        int destXmin;
        int destXmax;
        int destYmin;
        int destYmax;
        int destZmin;
        int destZmax;

        vec = destIter.next();
        destXmax = destXmin = vec.getBlockX();
        destYmax = destYmin = vec.getBlockY();
        destZmax = destZmin = vec.getBlockZ();

        // Get destination portal extremes
        while (destIter.hasNext()) {
            vec = destIter.next();

            if (vec.getBlockX() > destXmax) {
                destXmax = vec.getBlockX();
            }
            if (vec.getBlockX() < destXmin) {
                destXmin = vec.getBlockX();
            }
            if (vec.getBlockY() > destYmax) {
                destYmax = vec.getBlockY();
            }
            if (vec.getBlockY() < destYmin) {
                destYmin = vec.getBlockY();
            }
            if (vec.getBlockZ() > destZmax) {
                destZmax = vec.getBlockZ();
            }
            if (vec.getBlockZ() < destZmin) {
                destZmin = vec.getBlockZ();
            }
        }

        //Measure dimensions
        double sourceXwidth;
        double sourceZwidth;
        double destXwidth;
        double destZwidth;
        double sourceTmp;

        //Some source / destination refinements to give a margin inside the portal frame
        double yMargin = 2D;
        double zMargin = 0.3;
        double xMargin = 0.3;
        if (entity instanceof AbstractHorse || entity instanceof Boat) {
            xMargin = 1D;
            zMargin = 1D;
        }

        //Swap source z/x if portals are in different orientations
        if (!yawA.equals(yawB)) {
            //If source is open North/South, dest is open East/West
            if ( sourceYaw == 0F || sourceYaw == 180F) {
                xMargin = 0.5;
                sourceXwidth = 0D;
                sourceZwidth = sourceXmax - sourceXmin + 1 - 2*zMargin;
                destXwidth = 0D;
                destZwidth = destZmax - destZmin + 1 - 2*zMargin;
                //If portal is open East/West, dest is open North/South
            } else {
                zMargin = 0.5;
                sourceXwidth = sourceZmax - sourceZmin + 1 - 2*xMargin;
                sourceZwidth = 0D;
                destXwidth = destXmax - destXmin + 1 - 2*xMargin;
                destZwidth = 0D;
            }

            //Adjust sourceVec to an offset from min x/y/z locations
            sourceVec.subtract(new Vector(sourceXmin + zMargin, sourceYmin, sourceZmin + xMargin));
            //Then swap X/Z
            sourceTmp = sourceVec.getZ();
            sourceVec.setZ(sourceVec.getX());
            sourceVec.setX(sourceTmp);
        } else {
            //If source and dest are open North/South
            if ( sourceYaw == 0F || sourceYaw == 180F ){
                zMargin = 0.5;
                sourceXwidth = sourceXmax - sourceXmin + 1 - 2*xMargin;
                sourceZwidth = 0D;
                destXwidth = destXmax - destXmin + 1 - 2*xMargin;
                destZwidth = 0D;
                //If source and dest are open East/West
            } else {
                xMargin = 0.5;
                sourceXwidth = 0D;
                sourceZwidth = sourceZmax - sourceZmin + 1 - 2*zMargin;
                destXwidth = 0D;
                destZwidth = destZmax - destZmin + 1 - 2*zMargin;
            }
            //Adjust sourceVec to an offset from min x/y/z locations
            sourceVec.subtract(new Vector(sourceXmin + xMargin, sourceYmin, sourceZmin + zMargin));
        }
        double sourceHeight = sourceYmax - sourceYmin + 1 - yMargin;
        double destHeight = destYmax - destYmin + 1 - yMargin;

        //Shift sourceVec to be sure it is in sourcePortal minus margins
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
        Location destLoc = new Location(destWorld, destVec.getX(), destVec.getY(), destVec.getZ(), destYaw, 0F);
        destLoc.add(new Vector(destXmin, destYmin, destZmin));

        return destLoc;
    }

    public static Float getCompletePortalVectors(Block block, ArrayList<String> frameVecs, ArrayList<String> activatorVecs, ArrayList<String> vectors) throws InvalidConfigurationException {
        /* Tests whether a given block is part of a COMPLETE portal. This includes
         * the frame blocks as well as the 'activating' blocks placed along the
         * inside of the bottom of the frame.
         */
        Float yaw = null;

        if (isInAPortal(block.getLocation()) != null) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Activation block set in already active portal. Doing nothing.");
            return null;
        }
        BuildPortals.logger.log(BuildPortals.logLevel, "Portal is not in an active portal. Continuing.");
        ArrayList<String> activators;
        activators = (ArrayList<String>) BuildPortals.config.getStringList("PortalActivators");
        if (!activators.contains(block.getType().name())) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Placed block is not an activator.");
            return null;
        }
        BuildPortals.logger.log(BuildPortals.logLevel, "Placed block is an activator block. Continuing.");
        String currentActivatorName = block.getType().name();
        String frameMaterialName = BuildPortals.config.getString("PortalMaterial");
        Block firstFrameBaseBlock = block.getLocation().add(new Vector(0, -1 ,0)).getBlock();

        //x = Easting
        //y = Altitude
        //z = Southing
        //Check if activator block was placed on a frame block
        if ( ! firstFrameBaseBlock.getType().name().equals(Material.getMaterial(frameMaterialName).name())) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Placed activator block is on " + firstFrameBaseBlock.getType().name() + ", not " + Material.getMaterial(frameMaterialName).name());
            return null;
        }

        BuildPortals.logger.log(BuildPortals.logLevel, "Placed block is over a portal frame block. Continuing.");
        
        Block activatorBlock;
        HashMap<String, Vector> unitVectors = new HashMap<>();
        unitVectors.put("NORTH", new Vector(0,0,-1));
        unitVectors.put("SOUTH", new Vector(0, 0,1));
        unitVectors.put("EAST", new Vector(1,0,0));
        unitVectors.put("WEST", new Vector(-1,0,0));
        
//        String[] cardinalDirectionsString = new String[] {};
        List<String> cardinalDirections = Arrays.asList("NORTH", "SOUTH", "EAST", "WEST");
        
        //Find other activator blocks
        ArrayList<Block> activatorBlocks = new ArrayList<>();
        for (String direction : cardinalDirections) {
//        Iterator<String> unitIter = cardinalDirections.iterator();
//        while (unitIter.hasNext()) {
            Vector vec = unitVectors.get(direction);
            activatorBlock = block;
            while (currentActivatorName.equals(activatorBlock.getType().name())) {
                if (!activatorBlocks.contains(activatorBlock)) {
                    activatorBlocks.add(activatorBlock);
                    activatorVecs.add(activatorBlock.getLocation().toVector().toString());
                }
                activatorBlock = activatorBlock.getLocation().add(vec).getBlock();
                }
        }
        
        //Find portal frame wall North or West of most NW activator block
        Iterator<Block> actIter = activatorBlocks.iterator();
        int northMost;
        int westMost;
        
        //Find the most northwest coordinate of activator block
        //Also find and check portal base blocks
        Block baseBlock;
        ArrayList<String> baseVecs = new ArrayList<>();
        //Eclipse doesn't like enclosing this in an if (.hasNext()) block
        if ( ! actIter.hasNext()) {
            throw new InvalidConfigurationException("Invalid portal data!");
        }

        activatorBlock = actIter.next();
        //Check for portal base under activator block
        baseBlock = new Location(activatorBlock.getWorld(), activatorBlock.getX(), activatorBlock.getY() - 1, activatorBlock.getZ()).getBlock();
        northMost = activatorBlock.getLocation().getBlockZ();
        westMost = activatorBlock.getLocation().getBlockX();
        //Check for portal base under activator block
        if (! activatorBlock.getLocation().add(new Vector(0, -1, 0)).getBlock().getType().name().equals(Material.getMaterial(frameMaterialName).name())) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Missing portal base under an activator block.");
            return null;
        }
        BuildPortals.logger.log(BuildPortals.logLevel, "Adding base block at: " + baseBlock.getLocation().toVector().toString());
        baseVecs.add(baseBlock.getLocation().toVector().toString());
        
        while (actIter.hasNext()) {
            activatorBlock = actIter.next();
            //Check for portal base under activator block
            baseBlock = new Location(activatorBlock.getWorld(), activatorBlock.getX(), activatorBlock.getY()-1, activatorBlock.getZ()).getBlock();
            if ( ! baseBlock.getType().name().equals(Material.getMaterial(frameMaterialName).name())) {
                BuildPortals.logger.log(BuildPortals.logLevel, "Missing portal base under an activator block.");
                return null;
            }
            BuildPortals.logger.log(BuildPortals.logLevel, "Adding base block at: " + baseBlock.getLocation().toVector().toString());
            baseVecs.add(baseBlock.getLocation().toVector().toString());
            if (activatorBlock.getLocation().getBlockZ() < northMost) {
                northMost = activatorBlock.getLocation().getBlockZ();
            }
            if (activatorBlock.getLocation().getBlockX() < westMost) {
                westMost = activatorBlock.getLocation().getBlockX();
            }
        }
        BuildPortals.logger.log(BuildPortals.logLevel, "Northwest activator found at: X=" + westMost + ", Z=" + northMost);
        
        //Find the most southeast coordinate of activator block
        actIter = activatorBlocks.iterator();
        int southMost;
        int eastMost;

        if ( ! actIter.hasNext()) {
            throw new InvalidConfigurationException("Invalid portal data!");
        }
        activatorBlock = actIter.next();
        southMost = activatorBlock.getLocation().getBlockZ();
        eastMost = activatorBlock.getLocation().getBlockX();
        
        while (actIter.hasNext()) {
            activatorBlock = actIter.next();
            if (activatorBlock.getLocation().getBlockZ() > southMost) {
                southMost = activatorBlock.getLocation().getBlockZ();
            }
            if (activatorBlock.getLocation().getBlockX() > eastMost) {
                eastMost = activatorBlock.getLocation().getBlockX();
            }
        }
        BuildPortals.logger.log(BuildPortals.logLevel, "Southeast activator found at: X=" + eastMost + ", Z=" + southMost);
        
        ArrayList<Block> wallNW = new ArrayList<>();
        ArrayList<Block> wallSE = new ArrayList<>();
        
        //Look for portal walls North then West of the most Northwest activator block
        Location activatorNW = new Location(block.getWorld(), westMost, block.getLocation().getBlockY(), northMost);
        Location activatorSE = new Location(block.getWorld(), eastMost, block.getLocation().getBlockY(), southMost);

        BuildPortals.logger.log(BuildPortals.logLevel, "NW activator at: " + activatorNW.toVector().toString());

        //North/South oriented portal
        //North of activatorNW
        Location testLoc = new Location(activatorNW.getWorld(), activatorNW.getX(), activatorNW.getY(), activatorNW.getZ()-1);
        BuildPortals.logger.log(BuildPortals.logLevel, "NW activator at: " + activatorNW.toVector().toString());
        BuildPortals.logger.log(BuildPortals.logLevel, "Look for portal: " + testLoc.toVector().toString());

        if (testLoc.getBlock().getType().name().equals(Material.getMaterial(frameMaterialName).name())) {
            wallNW.add(testLoc.getBlock());
            //South of activatorSE
            BuildPortals.logger.log(BuildPortals.logLevel, "SE activator at: " + activatorSE.toVector().toString());
            BuildPortals.logger.log(BuildPortals.logLevel, "Look for portal: " + testLoc.toVector().toString());
            testLoc = new Location(activatorSE.getWorld(), activatorSE.getX(), activatorSE.getY(), activatorSE.getZ()+1);
            if ( ! testLoc.getBlock().getType().name().equals(Material.getMaterial(frameMaterialName).name())) {
                BuildPortals.logger.log(BuildPortals.logLevel, "Block at " + testLoc.toVector().toString() + ": " + testLoc.getBlock().getType().name());
                BuildPortals.logger.log(BuildPortals.logLevel, "Portal is missing a South wall.");
                return null;
            }
            wallSE.add(testLoc.getBlock());
            yaw = 90F; //West
        } else {
            BuildPortals.logger.log(BuildPortals.logLevel, "Block at " + testLoc.toVector().toString() + ": " + testLoc.getBlock().getType().name());
        }

        //East/West oriented portal
        //West of activatorNW
        testLoc = new Location(activatorNW.getWorld(), activatorNW.getX()-1, activatorNW.getY(), activatorNW.getZ());
        BuildPortals.logger.log(BuildPortals.logLevel, "NW activator at: " + activatorNW.toVector().toString());
        BuildPortals.logger.log(BuildPortals.logLevel, "Look for portal: " + testLoc.toVector().toString());

        if (testLoc.getBlock().getType().name().equals(Material.getMaterial(frameMaterialName).name())) {
            wallNW.add(testLoc.getBlock());
            //East of activatorSE
            testLoc = new Location(activatorSE.getWorld(), activatorSE.getX()+1, activatorSE.getY(), activatorSE.getZ());
            BuildPortals.logger.log(BuildPortals.logLevel, "SE activator at: " + activatorSE.toVector().toString());
            BuildPortals.logger.log(BuildPortals.logLevel, "Look for portal: " + testLoc.toVector().toString());
            if ( ! testLoc.getBlock().getType().name().equals(Material.getMaterial(frameMaterialName).name())) {
                BuildPortals.logger.log(BuildPortals.logLevel, "Block at " + testLoc.toVector().toString() + ": " + testLoc.getBlock().getType().name());
                BuildPortals.logger.log(BuildPortals.logLevel, "Portal is missing an East wall.");
                return null;
            }
            wallSE.add(testLoc.getBlock());
            yaw = 180F; //North
        } else {
            BuildPortals.logger.log(BuildPortals.logLevel, "Block at " + testLoc.toVector().toString() + ": " + testLoc.getBlock().getType().name());
        }
        if (wallSE.size() + wallNW.size() < 2) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Portal is missing a North/West wall.");
            return null;
        }

        BuildPortals.logger.log(BuildPortals.logLevel, "Portal walls adjacent to activation blocks found. Continuing.");
        //Find top of North/West wall
        Block nextBlock = wallNW.get(0).getLocation().add(new Vector(0,1,0)).getBlock();
        while (nextBlock.getType().name().equals(Material.getMaterial(frameMaterialName).name())) {
            wallNW.add(nextBlock);
            nextBlock = nextBlock.getLocation().add(new Vector(0,1,0)).getBlock();
        }
        BuildPortals.logger.log(BuildPortals.logLevel, "North/West wall height: " + wallNW.size());

        //Find top of South/East wall
        nextBlock = wallSE.get(0).getLocation().add(new Vector(0,1,0)).getBlock();
        while (nextBlock.getType().name().equals(Material.getMaterial(frameMaterialName).name())) {
            wallSE.add(nextBlock);
            nextBlock = nextBlock.getLocation().add(new Vector(0,1,0)).getBlock();
        }
        BuildPortals.logger.log(BuildPortals.logLevel, "South/East wall height: " + wallSE.size());
        
        int portalHeight = java.lang.Math.min(wallNW.size(), wallSE.size());
        BuildPortals.logger.log(BuildPortals.logLevel, "Initial portal height: " + portalHeight);
        Block portalTopBlock;
        Block currentActivatorBlock;
        actIter = activatorBlocks.iterator();
        
        //Adjust portalHeight to fit actual roof height
        currentActivatorBlock = activatorBlocks.get(0);
        for (int i=portalHeight; i>=2; i--) {
            portalTopBlock = new Location(currentActivatorBlock.getWorld(), currentActivatorBlock.getX(), currentActivatorBlock.getY() + i, currentActivatorBlock.getZ()).getBlock();
            BuildPortals.logger.log(BuildPortals.logLevel, "Height test: " + i + " Material: " + portalTopBlock.getType().name());
            BuildPortals.logger.log(BuildPortals.logLevel, "Test at: " + portalTopBlock.getLocation().toVector().toString());
            if (portalTopBlock.getType().name().equals(Material.getMaterial(frameMaterialName).name())) {
                portalHeight = i;
                BuildPortals.logger.log(BuildPortals.logLevel, "Portal height adjusted to: " + portalHeight);
            }
        }

        //Portal must be at least 2m tall
        if (portalHeight < 2) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Portal walls are not tall enough.");
            return null;
        }
        BuildPortals.logger.log(BuildPortals.logLevel, "Portal walls found and are tall enough. Continuing.");
        
        //Store portal walls now that portal height is confirmed
        Iterator<Block> NWIter = wallNW.iterator();
        Iterator<Block> SEIter = wallSE.iterator();
        ArrayList<String> NWVecs = new ArrayList<>();
        ArrayList<String> SEVecs = new ArrayList<>();
        for (int i=0; i<portalHeight; i++) {
            NWVecs.add(NWIter.next().getLocation().toVector().toString());
            SEVecs.add(SEIter.next().getLocation().toVector().toString());
        }
        
        
        //Check portal roof
        ArrayList<String> roofVecs = new ArrayList<>();
        while (actIter.hasNext()) {
            currentActivatorBlock = actIter.next();
            for (int i=0; i<portalHeight; i++) {
                vectors.add(new Location(currentActivatorBlock.getWorld(), currentActivatorBlock.getX(), currentActivatorBlock.getY() + i, currentActivatorBlock.getZ()).toVector().toString());
            }
            portalTopBlock = new Location(currentActivatorBlock.getWorld(), currentActivatorBlock.getX(), currentActivatorBlock.getY() + portalHeight, currentActivatorBlock.getZ()).getBlock();
            
            roofVecs.add(portalTopBlock.getLocation().toVector().toString());
//            BuildPortals.logger.info("Roof Block: " + portalTopBlock.getLocation().toVector().toString() + ": " + portalTopBlock.getType().name());
            if ( ! portalTopBlock.getType().name().equals(Material.getMaterial(frameMaterialName).name())) {
//                BuildPortals.logger.info("Portal is missing a roof block");
                return null;
            }
        }
        
        //Consolidate all frame vecs lists to passed frameVecs variable
        //ArrayList<String> frameVecs
//        BuildPortals.logger.info("Base Vectors: " + baseVecs.toString());
        frameVecs.addAll(baseVecs);
        frameVecs.addAll(roofVecs);
        frameVecs.addAll(NWVecs);
        frameVecs.addAll(SEVecs);
        
//        BuildPortals.logger.info("Portal is complete!");
//        BuildPortals.logger.info("Interior blocks: " + vectors.toString());
//        BuildPortals.logger.info("Frame blocks: " + frameVecs.toString());
        return yaw;
    }
}