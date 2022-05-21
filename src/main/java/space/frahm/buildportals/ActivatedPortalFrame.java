package space.frahm.buildportals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.InvalidConfigurationException;


public class ActivatedPortalFrame extends PortalFrame {
    public final ArrayList<Vector> activators;
    ActivatedPortalFrame(
        World world,
        ArrayList<Vector> interior,
        ArrayList<Vector> exterior,
        ArrayList<Vector> activators,
        float yaw
    ) {
        super(world, interior, exterior, yaw);
        this.activators = activators;
    }

    public static ActivatedPortalFrame getCompletePortalVectors(Block block) throws InvalidConfigurationException {
        /* Tests whether a given block is part of a COMPLETE portal. This includes
         * the frame blocks as well as the 'activating' blocks placed along the
         * inside of the bottom of the frame.
         */
        // TODO: This method could stand review and simplification
        World world = block.getWorld();
        ArrayList<Vector> interior = new ArrayList<>();
        ArrayList<Vector> exterior = new ArrayList<>();
        ArrayList<Vector> activators = new ArrayList<>();
        Float yaw = null;

        Location location = block.getLocation();
        if (Portal.isInAPortal(location) || IncompletePortal.isInAPortal(location)) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Placed block set in already active portal. Doing nothing.");
            return null;
        }
        BuildPortals.logger.log(BuildPortals.logLevel, "Placed block is not in an active portal. Continuing.");
        if (!BuildPortals.activatorMaterials.contains(block.getType())) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Placed block is not an activator.");
            return null;
        }
        BuildPortals.logger.log(BuildPortals.logLevel, "Placed block is an activator block. Continuing.");
        String currentActivatorName = block.getType().name();
        String frameMaterialName = BuildPortals.config.getString("PortalMaterial");
        Block firstFrameBaseBlock = block.getLocation().add(new Vector(0, -1 ,0)).getBlock();

        // x = Easting
        // y = Altitude
        // z = Southing
        // Check if activator block was placed on a frame block
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
        
        List<String> cardinalDirections = Arrays.asList("NORTH", "SOUTH", "EAST", "WEST");
        
        // Find other activator blocks
        ArrayList<Block> activatorBlocks = new ArrayList<>();
        for (String direction : cardinalDirections) {
            Vector vec = unitVectors.get(direction);
            activatorBlock = block;
            while (currentActivatorName.equals(activatorBlock.getType().name())) {
                if (!activatorBlocks.contains(activatorBlock)) {
                    activatorBlocks.add(activatorBlock);
                    activators.add(activatorBlock.getLocation().toVector());
                }
                activatorBlock = activatorBlock.getLocation().add(vec).getBlock();
                }
        }
        
        // Find portal frame wall North or West of most NW activator block
        Iterator<Block> actIter = activatorBlocks.iterator();
        int northMost;
        int westMost;
        
        // Find the most northwest coordinate of activator block
        // Also find and check portal base blocks
        Block baseBlock;
        ArrayList<Vector> baseVecs = new ArrayList<>();
        // Eclipse doesn't like enclosing this in an if (.hasNext()) block
        if ( ! actIter.hasNext()) {
            throw new InvalidConfigurationException("Invalid portal data!");
        }

        activatorBlock = actIter.next();
        // Check for portal base under activator block
        baseBlock = activatorBlock.getLocation().add(new Vector(0, -1, 0)).getBlock();
        // baseBlock = new Location(activatorBlock.getWorld(), activatorBlock.getX(), activatorBlock.getY() - 1, activatorBlock.getZ()).getBlock();
        northMost = activatorBlock.getLocation().getBlockZ();
        westMost = activatorBlock.getLocation().getBlockX();
        //Check for portal base under activator block
        if (! activatorBlock.getLocation().add(new Vector(0, -1, 0)).getBlock().getType().name().equals(Material.getMaterial(frameMaterialName).name())) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Missing portal base under an activator block.");
            return null;
        }
        BuildPortals.logger.log(BuildPortals.logLevel, "Adding base block at: " + baseBlock.getLocation().toVector().toString());
        baseVecs.add(baseBlock.getLocation().toVector());
        
        while (actIter.hasNext()) {
            activatorBlock = actIter.next();
            // Check for portal base under activator block
            baseBlock = new Location(activatorBlock.getWorld(), activatorBlock.getX(), activatorBlock.getY()-1, activatorBlock.getZ()).getBlock();
            if ( ! baseBlock.getType().name().equals(Material.getMaterial(frameMaterialName).name())) {
                BuildPortals.logger.log(BuildPortals.logLevel, "Missing portal base under an activator block.");
                return null;
            }
            BuildPortals.logger.log(BuildPortals.logLevel, "Adding base block at: " + baseBlock.getLocation().toVector().toString());
            baseVecs.add(baseBlock.getLocation().toVector());
            if (activatorBlock.getLocation().getBlockZ() < northMost) {
                northMost = activatorBlock.getLocation().getBlockZ();
            }
            if (activatorBlock.getLocation().getBlockX() < westMost) {
                westMost = activatorBlock.getLocation().getBlockX();
            }
        }
        BuildPortals.logger.log(BuildPortals.logLevel, "Northwest activator found at: X=" + westMost + ", Z=" + northMost);
        
        // Find the most southeast coordinate of activator block
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
        
        // Look for portal walls North then West of the most Northwest activator block
        Location activatorNW = new Location(block.getWorld(), westMost, block.getLocation().getBlockY(), northMost);
        Location activatorSE = new Location(block.getWorld(), eastMost, block.getLocation().getBlockY(), southMost);

        BuildPortals.logger.log(BuildPortals.logLevel, "NW activator at: " + activatorNW.toVector().toString());

        // North/South oriented portal
        // North of activatorNW
        Location testLoc = new Location(activatorNW.getWorld(), activatorNW.getX(), activatorNW.getY(), activatorNW.getZ()-1);
        BuildPortals.logger.log(BuildPortals.logLevel, "NW activator at: " + activatorNW.toVector().toString());
        BuildPortals.logger.log(BuildPortals.logLevel, "Look for portal: " + testLoc.toVector().toString());

        if (testLoc.getBlock().getType().name().equals(Material.getMaterial(frameMaterialName).name())) {
            wallNW.add(testLoc.getBlock());
            // South of activatorSE
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

        // East/West oriented portal
        // West of activatorNW
        testLoc = new Location(activatorNW.getWorld(), activatorNW.getX()-1, activatorNW.getY(), activatorNW.getZ());
        BuildPortals.logger.log(BuildPortals.logLevel, "NW activator at: " + activatorNW.toVector().toString());
        BuildPortals.logger.log(BuildPortals.logLevel, "Look for portal: " + testLoc.toVector().toString());

        if (testLoc.getBlock().getType().name().equals(Material.getMaterial(frameMaterialName).name())) {
            wallNW.add(testLoc.getBlock());
            // East of activatorSE
            testLoc = new Location(activatorSE.getWorld(), activatorSE.getX()+1, activatorSE.getY(), activatorSE.getZ());
            BuildPortals.logger.log(BuildPortals.logLevel, "SE activator at: " + activatorSE.toVector().toString());
            BuildPortals.logger.log(BuildPortals.logLevel, "Look for portal: " + testLoc.toVector().toString());
            if ( ! testLoc.getBlock().getType().name().equals(Material.getMaterial(frameMaterialName).name())) {
                BuildPortals.logger.log(BuildPortals.logLevel, "Block at " + testLoc.toVector().toString() + ": " + testLoc.getBlock().getType().name());
                BuildPortals.logger.log(BuildPortals.logLevel, "Portal is missing an East wall.");
                return null;
            }
            wallSE.add(testLoc.getBlock());
            yaw = 180F; // North
        } else {
            BuildPortals.logger.log(BuildPortals.logLevel, "Block at " + testLoc.toVector().toString() + ": " + testLoc.getBlock().getType().name());
        }
        if (wallSE.size() + wallNW.size() < 2) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Portal is missing a North/West wall.");
            return null;
        }

        BuildPortals.logger.log(BuildPortals.logLevel, "Portal walls adjacent to activation blocks found. Continuing.");
        // Find top of North/West wall
        Block nextBlock = wallNW.get(0).getLocation().add(new Vector(0,1,0)).getBlock();
        while (nextBlock.getType().name().equals(Material.getMaterial(frameMaterialName).name())) {
            wallNW.add(nextBlock);
            nextBlock = nextBlock.getLocation().add(new Vector(0,1,0)).getBlock();
        }
        BuildPortals.logger.log(BuildPortals.logLevel, "North/West wall height: " + wallNW.size());

        // Find top of South/East wall
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
        
        // Adjust portalHeight to fit actual roof height
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

        // Portal must be at least 2m tall
        if (portalHeight < 2) {
            BuildPortals.logger.log(BuildPortals.logLevel, "Portal walls are not tall enough.");
            return null;
        }
        BuildPortals.logger.log(BuildPortals.logLevel, "Portal walls found and are tall enough. Continuing.");
        
        // Store portal walls now that portal height is confirmed
        Iterator<Block> NWIter = wallNW.iterator();
        Iterator<Block> SEIter = wallSE.iterator();
        ArrayList<Vector> NWVecs = new ArrayList<>();
        ArrayList<Vector> SEVecs = new ArrayList<>();
        for (int i=0; i<portalHeight; i++) {
            NWVecs.add(NWIter.next().getLocation().toVector());
            SEVecs.add(SEIter.next().getLocation().toVector());
        }
        
        
        // Check portal roof
        ArrayList<Vector> roofVecs = new ArrayList<>();
        while (actIter.hasNext()) {
            currentActivatorBlock = actIter.next();
            for (int i=0; i<portalHeight; i++) {
                interior.add(new Vector(currentActivatorBlock.getX(), currentActivatorBlock.getY() + i, currentActivatorBlock.getZ()));
            }
            portalTopBlock = new Location(currentActivatorBlock.getWorld(), currentActivatorBlock.getX(), currentActivatorBlock.getY() + portalHeight, currentActivatorBlock.getZ()).getBlock();
            
            roofVecs.add(portalTopBlock.getLocation().toVector());
            // BuildPortals.logger.info("Roof Block: " + portalTopBlock.getLocation().toVector().toString() + ": " + portalTopBlock.getType().name());
            if ( ! portalTopBlock.getType().name().equals(Material.getMaterial(frameMaterialName).name())) {
                // BuildPortals.logger.info("Portal is missing a roof block");
                return null;
            }
        }
        
        // Consolidate all frame vecs lists to passed frameVecs variable
        // ArrayList<String> frameVecs
        // BuildPortals.logger.info("Base Vectors: " + baseVecs.toString());
        exterior.addAll(baseVecs);
        exterior.addAll(roofVecs);
        exterior.addAll(NWVecs);
        exterior.addAll(SEVecs);
        
        // BuildPortals.logger.info("Portal is complete!");
        // BuildPortals.logger.info("Interior blocks: " + vectors.toString());
        // BuildPortals.logger.info("Frame blocks: " + frameVecs.toString());
        return new ActivatedPortalFrame(world, interior, exterior, activators, yaw);
    }
}
