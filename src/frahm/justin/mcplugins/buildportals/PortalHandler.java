package frahm.justin.mcplugins.buildportals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class PortalHandler {

	/*
	 * Holds vectors for each 'portal block' for side A and B of a portal as
	 * well as the worlds that side A and B are in. Includes some members to
	 * facilitate testing portals / mapping origin to destination within that
	 * portal.
	 */
	private class Portal {
		ArrayList<Vector> vectorsA;
		ArrayList<Vector> vectorsB;
		ArrayList<Vector> frameVecsA;
		ArrayList<Vector> frameVecsB;
		World aWorld;
		World bWorld;
		Float yawA;
		Float yawB;
		String identifier;
		final Vector blockCenterOffset = new Vector(0.5, 0, 0.5);

		/*
		 * Constructor for a portal object, which includes a collection of
		 * vectors representing sides A and B of the portals as well as which
		 * world each side is in.
		 */
		public Portal(String identifier, World aWorld, ArrayList<Vector> vectorsA, ArrayList<Vector> frameVecsA, Float yawA, World bWorld, ArrayList<Vector> vectorsB, ArrayList<Vector> frameVecsB, Float yawB) {
			this.identifier = identifier;
			this.aWorld = aWorld;
			this.vectorsA = vectorsA;
			this.frameVecsA = frameVecsA;
			this.yawA = yawA;
			this.bWorld = bWorld;
			this.vectorsB = vectorsB;
			this.frameVecsB = frameVecsB;
			this.yawB = yawB;
//			logger.info("Portal number " + identifier + " created.");
//			logger.info("Portal A, world: " + aWorld.getName());
//			logger.info("       vectorsA: " + vectorsA.toString());
//			logger.info("     frameVecsA: " + frameVecsA.toString());
//			logger.info("Portal B, world: " + bWorld.toString());
//			logger.info("       vectorsB: " + vectorsB.toString());
//			logger.info("     frameVecsB: " + frameVecsB.toString());
		}

		/*
		 * Quick tester to see if given location is within this portal object.
		 * 
		 * NOTE: the given location MUST be based on the floored coordinates.
		 */
		public boolean isInPortal(Location loc) {
			if (loc.getWorld() == aWorld) {
				if (vectorsA.contains(loc.toVector())) {
					return true;
				}
			}
			if (loc.getWorld() == bWorld) {
				return vectorsB.contains(loc.toVector());
			}
			return false;
		}

		/*
		 * Quick tester to see if given location is within this portal frame.
		 * 
		 * NOTE: the given location MUST be based on the floored coordinates.
		 */
		public boolean isInFrame(Location loc) {
			if (loc.getWorld() == aWorld) {
				if (frameVecsA.contains(loc.toVector())) {
					return true;
				}
//				logger.info("isInFrame: " + loc.toVector().toString());
//				logger.info("   not in: " + frameVecsA.toString());
			}
			if (loc.getWorld() == bWorld) {
				return frameVecsB.contains(loc.toVector());
			}
//			logger.info("isInFrame: " + loc.toVector().toString());
//			logger.info("   not in: " + frameVecsB.toString());
			return false;
		}
		
		/*Returns the (String) numeric identifier of the portal */
		public String getID() {
			return identifier;
		}
		
		/*
		 * Returns the location of the portal destination that corresponds
		 * to the player's location.
		 * 
		 * Returns Null if the location is not actually in the portal.
		 */
		public Location getDestination(Player player, Location sourceLoc) {
			Vector sourceVec = new Vector(sourceLoc.getX(), sourceLoc.getY(), sourceLoc.getZ());
			sourceVec.add(blockCenterOffset);
			Iterator<Vector> sourceIter;
			Iterator<Vector> destIter;
			World destWorld;
			Float destYaw;
			
			//If source is in portal A
			if (sourceLoc.getWorld() == aWorld && vectorsA.contains(sourceLoc.toVector())) {
					sourceIter = vectorsA.iterator();
					destIter = vectorsB.iterator();
					destWorld = bWorld;
					destYaw = yawB;
			//If source is in portal B
			} else if (sourceLoc.getWorld() == bWorld && vectorsB.contains(sourceLoc.toVector())) {
					sourceIter = vectorsB.iterator();
					destIter = vectorsA.iterator();
					destWorld = aWorld;
					destYaw = yawA;
			} else {
				return null;
			}
			
			Integer sourceXmin;
			Integer sourceXmax;
			Integer sourceYmin;
			Integer sourceYmax;
			Integer sourceZmin;
			Integer sourceZmax;
			
			Vector vec;
			
			vec = sourceIter.next();
			sourceXmax = sourceXmin = vec.getBlockX();
			sourceYmax = sourceYmin = vec.getBlockY();
			sourceZmax = sourceZmin = vec.getBlockZ();
			
			//Get source portal extremes
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

			Integer destXmin;
			Integer destXmax;
			Integer destYmin;
			Integer destYmax;
			Integer destZmin;
			Integer destZmax;
			
			vec = destIter.next();
			destXmax = destXmin = vec.getBlockX();
			destYmax = destYmin = vec.getBlockY();
			destZmax = destZmin = vec.getBlockZ();
			
			//Get destination portal extremes
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
			
			//Adjust sourceVec to an offset from min x/y/z locations
			sourceVec.subtract(new Vector(sourceXmin, sourceYmin, sourceZmin));
			
			//Measure some portal geometry features
			Integer sourceXwidth;
			Integer sourceZwidth;
			Double sourceTmp;
			
			//Swap source z/x if portals are in different orientations
			if (!yawA.equals(yawB)) {
				sourceZwidth = sourceXmax - sourceXmin + 1;
				sourceXwidth = sourceZmax - sourceZmin + 1;
				sourceTmp = sourceVec.getZ();
				sourceVec.setZ(sourceVec.getX());
				sourceVec.setX(sourceTmp);
			} else {
				sourceXwidth = sourceXmax - sourceXmin + 1;
				sourceZwidth = sourceZmax - sourceZmin + 1;
			}
			Integer sourceHeight = sourceYmax - sourceYmin + 1;
			
			Integer destXwidth = destXmax - destXmin + 1;
			Integer destZwidth = destZmax - destZmin + 1;
			Integer destHeight = destYmax - destYmin + 1;
			
			Vector destVec = new Vector();
			destVec.setX( (sourceVec.getX()/sourceXwidth) * destXwidth);
//			logger.info("destVec.setX: " + sourceVec.getX() + "/" + sourceXwidth + " * " + destXwidth);
			destVec.setY( (sourceVec.getY()/sourceHeight) * destHeight);
//			logger.info("destVec.setY: " + sourceVec.getY() + "/" + sourceHeight + " * " + destHeight);
			destVec.setZ( (sourceVec.getZ()/sourceZwidth) * destZwidth);
//			logger.info("destVec.setZ: " + sourceVec.getZ() + "/" + sourceZwidth + " * " + destZwidth);
			
			//Some destination refinements to give a buffer inside the portal frame
			Double yMaxBuffer = 1.8;
			Double yMinBuffer = 0.0;
			Double xzBuffer = 0.3;
			if (player.getVehicle() instanceof Horse) {
//				logger.info("Horse detected, increasing buffers.");
				yMaxBuffer = 2.15;
				yMinBuffer = 0.0;
				xzBuffer = 1.0;
			}
			if ( destHeight < (yMinBuffer + yMaxBuffer)) {
//				logger.info("Portal is too short. Setting Y to " + yMinBuffer);
				destVec.setY(yMinBuffer);
			} else {
				if (destVec.getY() < yMinBuffer) {
//					logger.info("Destination is too low. Setting Y to " + yMinBuffer);
					destVec.setY(yMinBuffer);
				} else if ( (destHeight - destVec.getY()) < yMaxBuffer ) {
//					logger.info("Destination is too high. Setting Y to " + (destHeight - yMaxBuffer));
					destVec.setY(destHeight - yMaxBuffer);
				}
			}
			if (xzBuffer*2 > destXwidth) {
//				logger.info("Destination X width is too narrow. Setting X to " + destXwidth/2.0);
				destVec.setX(destXwidth/2.0);
			} else {
				if (destVec.getX() < xzBuffer) {
//					logger.info("Destination X is too low. Setting X to " + xzBuffer);
					destVec.setX(xzBuffer);
				} else if ( (destXwidth - destVec.getX()) < xzBuffer ) {
//					logger.info("Destination X is too high. Setting X to " + (destXwidth - xzBuffer));
					destVec.setX(destXwidth - xzBuffer);
				}
			}
			if (xzBuffer*2 > destZwidth) {
//				logger.info("Destination Z is too narrow. Setting Z to " + destZwidth/2.0);
				destVec.setZ(destZwidth/2.0);
			} else {
				if (destVec.getZ() < xzBuffer) {
//					logger.info("Destination Z is too low. Setting Z to " + xzBuffer);
					destVec.setZ(xzBuffer);
				} else if ( (destZwidth - destVec.getZ()) < xzBuffer ) {
//					logger.info("Destination Z is too high. Setting Z to " + (destZwidth - xzBuffer));
					destVec.setZ(destZwidth - xzBuffer);
				}
			}
			
			Location destLoc = new Location(destWorld, destVec.getX(), destVec.getY(), destVec.getZ(), destYaw, 0F);
			
//			logger.info("Teleportation event:");
//			logger.info("Destination portal: " + destXwidth + "/" + destHeight + "/" + destZwidth);
//			logger.info("          vertical: " + destYmin + " - " + destYmax);
//			logger.info("Source portal: " + sourceXwidth + "/" + sourceHeight + "/" + sourceZwidth);
//			logger.info("          vertical: " + sourceYmin + " - " + sourceYmax);
//			logger.info("Destination vector, X: " + destVec.getX() + ", Y: " + destVec.getY() + ", Z: " + destVec.getZ());
//			logger.info("Source vector,      X: " + sourceVec.getX() + ", Y: " + sourceVec.getY() + ", Z: " + sourceVec.getZ());
			destLoc.add(new Vector(destXmin, destYmin, destZmin));
			
			
			return destLoc;
		}
	}

	Main plugin;
	Logger logger;
	FileConfiguration config;

	/*
	 * Map of portal block location sets, keyed by world name Intended for fast
	 * testing of whether a location is in a configured portal/frame.
	 */
	static HashMap<String, HashSet<Vector>> portalBlocks;
	static HashMap<String, HashSet<Vector>> frameBlocks; 

	/*
	 * This set of portal objects is for actual correlating of source portal
	 * locations to destination locations.
	 */
	static HashSet<Portal> portals;

	/* Constructor, pass a handle to the plugin for configuration reading. */
	public PortalHandler(Main plugin) {
		this.plugin = plugin;
		this.logger = plugin.getLogger();
		this.config = plugin.getConfig();
	}
	
	/*
	 * Runs through each portal frame block, checks that they are still the portal
	 * frame material and if not, returns the portal number of the first non-portal
	 * material block it finds.
	 */
	public String integrityCheck(Location loc) {
		String frameMaterialName = config.getString("PortalMaterial");
		Iterator<Vector> frameVecs;
		for (Map.Entry<String, HashSet<Vector>> frameEntries : frameBlocks.entrySet()) {
			String worldName = frameEntries.getKey();
			World world = Bukkit.getWorld(worldName);
			frameVecs = frameEntries.getValue().iterator();
			while (frameVecs.hasNext()) {
				Vector vec = frameVecs.next();
				loc = new Location(world, vec.getX(), vec.getY(), vec.getZ());
				if (loc.getBlock().getType().name() != Material.getMaterial(frameMaterialName).name()) {
					return getPortalFromFrame(loc);
				}
			}
		}
		return null;
	}

	/*
	 * Tests a given (floored) location to see if it is in the boundaries of any
	 * configured portals.
	 */
	public boolean isInAPortal(Location loc) {
		if (!portalBlocks.containsKey(loc.getWorld().getName())){
//			logger.info("No portals in world: " + loc.getWorld().getName());
//			logger.info("Worlds are: " + portalBlocks.toString());
			return false;
		}
		return portalBlocks.get(loc.getWorld().getName()).contains(loc.toVector());
	}

	/*
	 * Tests a given (floored) location to see if it is in the boundaries of any
	 * configured portal frames.
	 */
	public boolean isInAFrame(Location loc) {
		if (!frameBlocks.containsKey(loc.getWorld().getName())){
//			logger.info("No portals in world: " + loc.getWorld().getName());
//			logger.info("Worlds are: " + frameBlocks.toString());
			return false;
		}
//		logger.info("isInAFrame: " + frameBlocks.toString());
		return frameBlocks.get(loc.getWorld().getName()).contains(loc.toVector());
	}

	/*
	 * Tests whether a given block is part of a COMPLETE portal. This includes
	 * the frame blocks as well as the 'activating' blocks placed along the
	 * inside of the bottom of the frame.
	 */
	public Float getCompletePortalVectors(Block block, ArrayList<String> frameVecs, ArrayList<String> vectors) {
		FileConfiguration config = plugin.getConfig();
		Float yaw = null;
		
		if (isInAPortal(block.getLocation())) {
//			logger.info("Activation block set in already active portal. Doing nothing.");
			return null;
		}
//		logger.info("Portal is not in an active portal. Continuing.");
		ArrayList<String> activators;
		activators = (ArrayList<String>) config.getStringList("PortalActivators");
		if (!activators.contains(block.getType().name())) {
//			logger.info("Placed block is not an activator.");
			return null;
		}
//		logger.info("Placed block is an activator block. Continuing.");
		String currentActivatorName = block.getType().name();
		String frameMaterialName = config.getString("PortalMaterial");
		Block frameBaseBlock = block.getLocation().add(new Vector(0, -1 ,0)).getBlock();
		Block firstFrameBaseBlock = frameBaseBlock;

		//x = Easting
		//y = Altitude
		//z = Southing
		//Check if activator block was placed on a frame block
		if ( firstFrameBaseBlock.getType().name() != Material.getMaterial(frameMaterialName).name()) {
//			logger.info("Placed activator block is on " + firstFrameBaseBlock.getType().name() + ", not " + Material.getMaterial(frameMaterialName).name());
			return null;
		}
		
//		logger.info("Placed block is over a portal frame block. Continuing.");
		
		Block activatorBlock = block;
		HashMap<String, Vector> unitVectors = new HashMap<String, Vector>();
		unitVectors.put("NORTH", new Vector(0,0,-1));
		unitVectors.put("SOUTH", new Vector(0, 0,1));
		unitVectors.put("EAST", new Vector(1,0,0));
		unitVectors.put("WEST", new Vector(-1,0,0));
		
		String[] cardinalDirectionsString = new String[] {"NORTH", "SOUTH", "EAST", "WEST"};
		List<String> cardinalDirections = Arrays.asList(cardinalDirectionsString);
		
		//Find other activator blocks
		ArrayList<Block> activatorBlocks = new ArrayList<Block>();
		Iterator<String> unitIter = cardinalDirections.iterator();
		while (unitIter.hasNext()) {
			Vector vec = unitVectors.get(unitIter.next());
			activatorBlock = block;
			while (currentActivatorName == activatorBlock.getType().name()) {
				if (!activatorBlocks.contains(activatorBlock)) {
					activatorBlocks.add(activatorBlock);
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
		ArrayList<String> baseVecs = new ArrayList<String>();
		//Eclipse doesn't like enclosing this in an if (.hasNext()) block
		try {
			activatorBlock = actIter.next();
			//Check for portal base under activator block
			baseBlock = new Location(activatorBlock.getWorld(), activatorBlock.getX(), activatorBlock.getY()-1, activatorBlock.getZ()).getBlock();
			northMost = activatorBlock.getLocation().getBlockZ();
			westMost = activatorBlock.getLocation().getBlockX();
			//Check for portal base under activator block
			if (activatorBlock.getLocation().add(new Vector(0, -1, 0)).getBlock().getType().name() != Material.getMaterial(frameMaterialName).name()) {
//				logger.info("Missing portal base under an activator block.");
				return null;
			}
//			logger.info("Adding base block at: " + baseBlock.getLocation().toVector().toString());
			baseVecs.add(baseBlock.getLocation().toVector().toString());
		} finally {}
		
		while (actIter.hasNext()) {
			activatorBlock = actIter.next();
			//Check for portal base under activator block
			baseBlock = new Location(activatorBlock.getWorld(), activatorBlock.getX(), activatorBlock.getY()-1, activatorBlock.getZ()).getBlock();
			if (baseBlock.getType().name() != Material.getMaterial(frameMaterialName).name()) {
//				logger.info("Missing portal base under an activator block.");
				return null;
			}
//			logger.info("Adding base block at: " + baseBlock.getLocation().toVector().toString());
			baseVecs.add(baseBlock.getLocation().toVector().toString());
			if (activatorBlock.getLocation().getBlockZ() < northMost) {
				northMost = activatorBlock.getLocation().getBlockZ();
			}
			if (activatorBlock.getLocation().getBlockX() < westMost) {
				westMost = activatorBlock.getLocation().getBlockX();
			}
		}
//		logger.info("Northwest activator found at: X=" + westMost + ", Z=" + northMost);
		
		//Find the most southeast coordinate of activator block
		actIter = activatorBlocks.iterator();
		int southMost;
		int eastMost;
		
		//Eclipse doesn't like enclosing this in an if (.hasNext()) {} block
		try {
			activatorBlock = actIter.next();
			southMost = activatorBlock.getLocation().getBlockZ();
			eastMost = activatorBlock.getLocation().getBlockX();
		} finally {}
		
		while (actIter.hasNext()) {
			activatorBlock = actIter.next();
			if (activatorBlock.getLocation().getBlockZ() > southMost) {
				southMost = activatorBlock.getLocation().getBlockZ();
			}
			if (activatorBlock.getLocation().getBlockX() > eastMost) {
				eastMost = activatorBlock.getLocation().getBlockX();
			}
		}
//		logger.info("Southeast activator found at: X=" + eastMost + ", Z=" + southMost);
		
		ArrayList<Block> wallNW = new ArrayList<Block>();
		ArrayList<Block> wallSE = new ArrayList<Block>();
		
		//Look for portal walls North then West of the most Northwest activator block
		Location activatorNW = new Location(block.getWorld(), westMost, block.getLocation().getBlockY(), northMost);
		Location activatorSE = new Location(block.getWorld(), eastMost, block.getLocation().getBlockY(), southMost);
		
//		logger.info("NW activator at: " + activatorNW.toVector().toString());		

		//North/South oriented portal
		//North of activatorNW
		Location testLoc = new Location(activatorNW.getWorld(), activatorNW.getX(), activatorNW.getY(), activatorNW.getZ()-1);
//		logger.info("NW activator at: " + activatorNW.toVector().toString());		
//		logger.info("Look for portal: " + testLoc.toVector().toString());

		if (testLoc.getBlock().getType().name() == Material.getMaterial(frameMaterialName).name()) {
			wallNW.add(testLoc.getBlock());
			//South of activatorSE
//			logger.info("SE activator at: " + activatorSE.toVector().toString());
//			logger.info("Look for portal: " + testLoc.toVector().toString());
			testLoc = new Location(activatorSE.getWorld(), activatorSE.getX(), activatorSE.getY(), activatorSE.getZ()+1);
			if (testLoc.getBlock().getType().name() != Material.getMaterial(frameMaterialName).name()) {
				logger.info("Block at " + testLoc.toVector().toString() + ": " + testLoc.getBlock().getType().name());
				logger.info("Portal is missing a South wall.");
				return null;
			}
			wallSE.add(testLoc.getBlock());
			yaw = 90F;
		} else {
//			logger.info("Block at " + testLoc.toVector().toString() + ": " + testLoc.getBlock().getType().name());
		}

		//East/West oriented portal		
		//West of activatorNW
		testLoc = new Location(activatorNW.getWorld(), activatorNW.getX()-1, activatorNW.getY(), activatorNW.getZ());
//		logger.info("NW activator at: " + activatorNW.toVector().toString());
//		logger.info("Look for portal: " + testLoc.toVector().toString());

		if (testLoc.getBlock().getType().name() == Material.getMaterial(frameMaterialName).name()) {
			wallNW.add(testLoc.getBlock());
			//East of activatorSE
			testLoc = new Location(activatorSE.getWorld(), activatorSE.getX()+1, activatorSE.getY(), activatorSE.getZ());
//			logger.info("SE activator at: " + activatorSE.toVector().toString());
//			logger.info("Look for portal: " + testLoc.toVector().toString());
			if (testLoc.getBlock().getType().name() != Material.getMaterial(frameMaterialName).name()) {
//				logger.info("Block at " + testLoc.toVector().toString() + ": " + testLoc.getBlock().getType().name());
//				logger.info("Portal is missing an East wall.");
				return null;
			}
			wallSE.add(testLoc.getBlock());
			yaw = 180F;
		} else {
//			logger.info("Block at " + testLoc.toVector().toString() + ": " + testLoc.getBlock().getType().name());
		}
		if (wallSE.size() + wallNW.size() < 2) {
//			logger.info("Portal is missing a North/West wall.");
			return null;
		}
		
//		logger.info("Portal walls adjacent to activation blocks found. Continuing.");
		//Find top of North/West wall
		Block nextBlock = wallNW.get(0).getLocation().add(new Vector(0,1,0)).getBlock();
		while (nextBlock.getType().name() == Material.getMaterial(frameMaterialName).name()) {
			wallNW.add(nextBlock);
			nextBlock = nextBlock.getLocation().add(new Vector(0,1,0)).getBlock();
		}

		//Find top of South/East wall
		nextBlock = wallSE.get(0).getLocation().add(new Vector(0,1,0)).getBlock();
		while (nextBlock.getType().name() == Material.getMaterial(frameMaterialName).name()) {
			wallSE.add(nextBlock);
			nextBlock = nextBlock.getLocation().add(new Vector(0,1,0)).getBlock();
		}
		
		int portalHeight = java.lang.Math.min(wallNW.size(), wallSE.size());
//		logger.info("Initial portal height: " + portalHeight);
		Block portalTopBlock;
		Block currentActivatorBlock;
		actIter = activatorBlocks.iterator();
		
		//Adjust portalHeight to fit actual roof height
		currentActivatorBlock = activatorBlocks.get(0);
		for (int i=portalHeight; i>=2; i--) {
			portalTopBlock = new Location(currentActivatorBlock.getWorld(), currentActivatorBlock.getX(), currentActivatorBlock.getY() + i, currentActivatorBlock.getZ()).getBlock();
//			logger.info("Height test: " + i + " Material: " + portalTopBlock.getType().name());
//			logger.info("Test at: " + portalTopBlock.getLocation().toVector().toString());
			if (portalTopBlock.getType().name() == Material.getMaterial(frameMaterialName).name()) {
				portalHeight = i;
//				logger.info("Portal height adjusted to: " + portalHeight);
			}
		}

		//Portal must be at least 2m tall
		if (portalHeight < 2) {
//			logger.info("Portal walls are not tall enough.");
			return null;
		}
//		logger.info("Portal walls found and are tall enough. Continuing.");
		
		//Store portal walls now that portal height is confirmed
		Iterator<Block> NWIter = wallNW.iterator();
		Iterator<Block> SEIter = wallSE.iterator();
		ArrayList<String> NWVecs = new ArrayList<String>();
		ArrayList<String> SEVecs = new ArrayList<String>();
		for (int i=0; i<portalHeight; i++) {
			NWVecs.add(NWIter.next().getLocation().toVector().toString());
			SEVecs.add(SEIter.next().getLocation().toVector().toString());
		}
		
		
		//Check portal roof
		ArrayList<String> roofVecs = new ArrayList<String>();
		while (actIter.hasNext()) {
			currentActivatorBlock = actIter.next();
			for (int i=0; i<portalHeight; i++) {
				vectors.add(new Location(currentActivatorBlock.getWorld(), currentActivatorBlock.getX(), currentActivatorBlock.getY() + i, currentActivatorBlock.getZ()).toVector().toString());
			}
			portalTopBlock = new Location(currentActivatorBlock.getWorld(), currentActivatorBlock.getX(), currentActivatorBlock.getY() + portalHeight, currentActivatorBlock.getZ()).getBlock();
			
			roofVecs.add(portalTopBlock.getLocation().toVector().toString());
//			logger.info("Roof Block: " + portalTopBlock.getLocation().toVector().toString() + ": " + portalTopBlock.getType().name());
			if (portalTopBlock.getType().name() != Material.getMaterial(frameMaterialName).name()) {
//				logger.info("Portal is missing a roof block");
				return null;
			}
		}
		
		//Consolidate all frame vecs lists to passed frameVecs variable
		//ArrayList<String> frameVecs
//		logger.info("Base Vectors: " + baseVecs.toString());
		frameVecs.addAll(baseVecs);
		frameVecs.addAll(roofVecs);
		frameVecs.addAll(NWVecs);
		frameVecs.addAll(SEVecs);
		
//		logger.info("Portal is complete!");
//		logger.info("Interior blocks: " + vectors.toString());
//		logger.info("Frame blocks: " + frameVecs.toString());
		return yaw;
	}

	/*
	 * Reads the configuration state, populating the portalBlocks and other
	 * PortalHandler fields based on the portal configuration.
	 */
	public void updatePortals() {
		portals = new HashSet<Portal>();
		portalBlocks = new HashMap<String, HashSet<Vector>>();
		frameBlocks = new HashMap<String, HashSet<Vector>>();
		
		FileConfiguration config = plugin.getConfig();
		if (null == config) {
//			logger.info("No configurations set!");
			return;
		}
		ConfigurationSection portalSection = config.getConfigurationSection("portals");
		if (null == portalSection) {
//			logger.info("No portals data in configurations.");
			return;
		}
		Set<String> portalKeys = portalSection.getKeys(false);
		if (null == portalKeys) {
			logger.info("No portals defined in portals data.");
			return;
		}
		
		ArrayList<String> activators = (ArrayList<String>) config.getStringList("PortalActivators");
//		portalKeys.remove("0");
//		logger.info("portalKeys: " + portalKeys.toString());
		Iterator<String> configIterator = portalKeys.iterator();
		Iterator<String> vectorsIterator;
		String portalNumber;
		World worldA;
		ArrayList<String> vectorStringsA;
		ArrayList<String> frameStringsA;
		Float yawA;
		World worldB;
		ArrayList<String> vectorStringsB;
		ArrayList<String> frameStringsB;
		Float yawB;
		HashSet<Vector> tempVecSet = new HashSet<Vector>();
		// Read vector string describing each portal, ends A and B
		while (configIterator.hasNext()) {
			portalNumber = configIterator.next();
//			logger.info("portalNumber: " + portalNumber + "==0: " + (portalNumber == "0"));
			if ( Integer.parseInt(portalNumber) != 0) {
				logger.info("Loading configuration for portal number: " + portalNumber);
				vectorStringsA = (ArrayList<String>) config.getStringList("portals." + portalNumber + ".A.vec");
				frameStringsA = (ArrayList<String>) config.getStringList("portals." + portalNumber + ".A.frame");
				vectorStringsB = (ArrayList<String>) config.getStringList("portals." + portalNumber + ".B.vec");
				frameStringsB = (ArrayList<String>) config.getStringList("portals." + portalNumber + ".B.frame");
	
				ArrayList<Vector> vectorsA = new ArrayList<Vector>();
				ArrayList<Vector> vectorsB = new ArrayList<Vector>();
				ArrayList<Vector> frameVecsA = new ArrayList<Vector>();
				ArrayList<Vector> frameVecsB = new ArrayList<Vector>();
				
				String worldAName = config.getString("portals." + portalNumber + ".A.world");
				String yawAString = config.getString("portals." + portalNumber + ".A.yaw");
				if (worldAName == null) {
					logger.info("Error reading World A Name configuration!");
//					logger.info("Attemting to read config for Portal " + portalNumber);
					return;
				}
				worldA = Bukkit.getWorld(worldAName);
				if (yawAString == null) {
					logger.info("Error reading yawA from configuration!");
					return;
				}
				yawA = Float.parseFloat(yawAString);
				
				String worldBName = config.getString("portals." + portalNumber + ".B.world");
				String yawBString = config.getString("portals." + portalNumber + ".B.yaw");
				if (worldBName == null) {
					logger.info("Error reading World B Name configuration!");
					return;
				}
				worldB = Bukkit.getWorld(plugin.config.getString("portals." + portalNumber + ".B.world"));
				if (yawBString == null) {
					logger.info("Error reading yawB from configuration!");
					return;
				}
				yawB = Float.parseFloat(yawBString);
				// Convert string lists for A and B to vector lists
				// Side A vectors
				vectorsIterator = vectorStringsA.iterator();
				while (vectorsIterator.hasNext()) {
					String[] parts = vectorsIterator.next().split(",");
					if (parts.length != 3) {
						logger.info("Error reading portal data!");
						return;
					}
					Vector vec = new Vector();
					vec.setX(Double.parseDouble(parts[0]));
					vec.setY(Double.parseDouble(parts[1]));
					vec.setZ(Double.parseDouble(parts[2]));
					vectorsA.add(vec);
					// Add this vector to the portalBlocks set, keyed by world
					if (portalBlocks.containsKey(worldA.getName())) {
						portalBlocks.get(worldA.getName()).add(vec);
					} else {
						tempVecSet.add(vec);
						portalBlocks.put(worldA.getName(), tempVecSet);
						tempVecSet = new HashSet<Vector>();
					}
				}
				// Side A frame vectors
				vectorsIterator = frameStringsA.iterator();
				while (vectorsIterator.hasNext()) {
					String[] parts = vectorsIterator.next().split(",");
					if (parts.length != 3) {
						logger.info("Error reading frame data!");
						return;
					}
					Vector vec = new Vector();
					vec.setX(Double.parseDouble(parts[0]));
					vec.setY(Double.parseDouble(parts[1]));
					vec.setZ(Double.parseDouble(parts[2]));
					frameVecsA.add(vec);
					// Add this vector to the frameBlocks set, keyed by world
					if (frameBlocks.containsKey(worldA.getName())) {
						frameBlocks.get(worldA.getName()).add(vec);
					} else {
						tempVecSet.add(vec);
						frameBlocks.put(worldA.getName(), tempVecSet);
						tempVecSet = new HashSet<Vector>();
					}
				}
	
				vectorsIterator = vectorStringsB.iterator();
				// Side B vectors
				while (vectorsIterator.hasNext()) {
					String[] parts = vectorsIterator.next().split(",");
					if (parts.length != 3) {
						logger.info("Error reading portal data!");
						return;
					}
					Vector vec = new Vector();
					vec.setX(Double.parseDouble(parts[0]));
					vec.setY(Double.parseDouble(parts[1]));
					vec.setZ(Double.parseDouble(parts[2]));
					vectorsB.add(vec);
					// Add this vector to the portalBlocks set, keyed by world
					if (portalBlocks.containsKey(worldB.getName())) {
						portalBlocks.get(worldB.getName()).add(vec);
					} else {
						tempVecSet.add(vec);
						portalBlocks.put(worldB.getName(), tempVecSet);
						tempVecSet = new HashSet<Vector>();
					}
				}
				// Side B frame vectors
				vectorsIterator = frameStringsB.iterator();
				while (vectorsIterator.hasNext()) {
					String[] parts = vectorsIterator.next().split(",");
					if (parts.length != 3) {
						logger.info("Error reading frame data!");
						return;
					}
					Vector vec = new Vector();
					vec.setX(Double.parseDouble(parts[0]));
					vec.setY(Double.parseDouble(parts[1]));
					vec.setZ(Double.parseDouble(parts[2]));
					frameVecsB.add(vec);
					// Add this vector to the frameBlocks set, keyed by world
					if (frameBlocks.containsKey(worldB.getName())) {
						frameBlocks.get(worldB.getName()).add(vec);
					} else {
						tempVecSet.add(vec);
						frameBlocks.put(worldB.getName(), tempVecSet);
						tempVecSet = new HashSet<Vector>();
					}
				}
				portals.add( new Portal(portalNumber, worldA, vectorsA, frameVecsA, yawA, worldB, vectorsB, frameVecsB, yawB));
				//logger.info("portals: " + portals.toString());
//				logger.info("portalBlocks: " + portalBlocks.toString());
			} else { //portalNumber = 0
//				logger.info("Loading configuration for portal number: " + portalNumber);
				Iterator<String> activatorIter = activators.iterator();
				String activator;
				while (activatorIter.hasNext()) {
					activator = activatorIter.next();
					frameStringsA = (ArrayList<String>) config.getStringList("portals." + portalNumber + "." + activator + ".frame");
		
					ArrayList<Vector> frameVecsA = new ArrayList<Vector>();
					
					String worldAName = config.getString("portals." + portalNumber + "." + activator + ".world");
					String yawAString = config.getString("portals." + portalNumber + "." + activator + ".yaw");
					if (worldAName != null) {
						worldA = Bukkit.getWorld(worldAName);
						if (yawAString == null) {
							logger.info("Error reading yawA from configuration!");
							return;
						}
						yawA = Float.parseFloat(yawAString);
						// Convert string lists for A and B to vector lists
						// Side A frame vectors
						vectorsIterator = frameStringsA.iterator();
						while (vectorsIterator.hasNext()) {
							String[] parts = vectorsIterator.next().split(",");
							if (parts.length != 3) {
								logger.info("Error reading frame data!");
								return;
							}
							Vector vec = new Vector();
							vec.setX(Double.parseDouble(parts[0]));
							vec.setY(Double.parseDouble(parts[1]));
							vec.setZ(Double.parseDouble(parts[2]));
							frameVecsA.add(vec);
							// Add this vector to the frameBlocks set, keyed by world
							if (frameBlocks.containsKey(worldA.getName())) {
								frameBlocks.get(worldA.getName()).add(vec);
							} else {
								tempVecSet.add(vec);
								frameBlocks.put(worldA.getName(), tempVecSet);
								tempVecSet = new HashSet<Vector>();
							}
						}
						portals.add( new Portal(portalNumber + "." + activator, worldA, new ArrayList<Vector>(), frameVecsA, yawA, null, new ArrayList<Vector>(), new ArrayList<Vector>(), 0F));
						//logger.info("portals: " + portals.toString());
	//					logger.info("portalBlocks: " + portalBlocks.toString());
					} //WorldAName != null
				} //activator while loop
			} //portalNumber = 0
		} //portalKeys while loop
	} //Update portals member

	/*
	 * Returns the destination portal block location that corresponds to the
	 * given portal block location. Returns null if the location is not part of
	 * a configured portal.
	 */
	public Location getDestination(Player player, Location loc) {
		Iterator<Portal> portalsIterator = portals.iterator();
		while (portalsIterator.hasNext()) {
			Portal portal = portalsIterator.next();
			if (portal.isInPortal(loc)) {
//				logger.info(player.getName() + " is in portal number " + portal.getID());
				return portal.getDestination(player, loc);
			}
		}
		return null;
	}
	
	public String getPortalFromFrame(Location loc) {
//		logger.info("Checking location: " + loc.toVector().toString());
		Iterator<Portal> portalsIterator = portals.iterator();
		while (portalsIterator.hasNext()) {
			Portal portal = portalsIterator.next();
			if (portal.isInFrame(loc)) {
				return portal.getID();
			}
		}
		
		return null;
	}
}
