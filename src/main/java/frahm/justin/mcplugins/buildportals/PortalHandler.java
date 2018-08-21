package frahm.justin.mcplugins.buildportals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

class PortalHandler {

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
		ArrayList<Vector> activatorVecsA;
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
		Portal(String identifier, World aWorld, ArrayList<Vector> vectorsA, ArrayList<Vector> frameVecsA, ArrayList<Vector> activatorVecsA, Float yawA, World bWorld, ArrayList<Vector> vectorsB, ArrayList<Vector> frameVecsB, Float yawB) {
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
		}

		/*
		 * Quick tester to see if given location is within this portal object.
		 *
		 * NOTE: the given location MUST be based on the floored coordinates.
		 */
		boolean isInPortal(Location loc) {
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
		boolean isInFrame(Location loc) {
			if (loc.getWorld() == aWorld) {
				if (frameVecsA.contains(loc.toVector())) {
					return true;
				}
			}
			if (loc.getWorld() == bWorld) {
				return frameVecsB.contains(loc.toVector());
			}
			return false;
		}

		/*
		 * Quick tester to see if the given location is within this portal's activators.
		 *
		 * NOTE: the given location MUST be based on the floored coordinates.
		 * NOTE: only incomplete portals will have activators.
		 */
		boolean isInActivators(Location loc) {
			return loc.getWorld() == aWorld && activatorVecsA.contains(loc.toVector());
		}

		/*Returns the (String) numeric identifier of the portal */
		String getID() {
			return identifier;
		}

		/*
		 * Returns the location of the portal destination that corresponds
		 * to the player's location.
		 *
		 * Returns Null if the location is not actually in the portal.
		 */
		Location getDestination(Entity entity, Location sourceLoc) {
			Vector sourceVec = new Vector(sourceLoc.getX(), sourceLoc.getY(), sourceLoc.getZ());

			//Move source to center of block
			sourceVec.add(blockCenterOffset);
			Iterator<Vector> sourceIter;
			Float sourceYaw;
			Iterator<Vector> destIter;
			ArrayList<Vector> destVectors;
			World destWorld;
			Float destYaw;

			//Define source and destination vectors
			//If source is in portal A
			if (sourceLoc.getWorld() == aWorld && vectorsA.contains(sourceLoc.toVector())) {
					sourceIter = vectorsA.iterator();
					sourceYaw = yawA;
					destVectors = vectorsB;
					destIter = vectorsB.iterator();
					destWorld = bWorld;
					destYaw = yawB;
			//If source is in portal B
			} else if (sourceLoc.getWorld() == bWorld && vectorsB.contains(sourceLoc.toVector())) {
					sourceIter = vectorsB.iterator();
					sourceYaw = yawB;
					destVectors = vectorsA;
					destIter = vectorsA.iterator();
					destWorld = aWorld;
					destYaw = yawA;
			} else {
				return null;
			}
			Vector forwardVec;
			Vector backwardVec;
			//Define a unit vector in the destination Yaw direction
			switch (destYaw.intValue()) {
				//South
				case 0:
					forwardVec = new Vector(0, 0, 1);
					backwardVec = new Vector(0, 0, -1);
					break;
				//West
				case 90:
					forwardVec = new Vector(-1, 0, 0);
					backwardVec = new Vector(1, 0, 0);
					break;
				//North
				case 180:
					forwardVec = new Vector(0, 0, -1);
					backwardVec = new Vector(0, 0, 1);
					break;
				//East
				default:
					forwardVec = new Vector(1, 0, 0);
					backwardVec = new Vector(-1, 0, 0);
					break;
			}

			//Count non-solid blocks next to portal blocks
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
			//If 'backwards' face has more non-solid blocks, turn the Yaw around.
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

			//Bail if a portal is too small
			if (sourceHeight < 0 || sourceZwidth < 0 || sourceXwidth < 0 || destHeight < 0 || destZwidth < 0 || destXwidth < 0) {
				return null;
			}
			//Map location in source portal to location in dest portal
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
	}

	private Main plugin;
	private Logger logger;
	private Level DEBUG_LEVEL;
	private FileConfiguration config;

	/*
	 * Map of portal block location sets, keyed by world name Intended for fast
	 * testing of whether a location is in a configured portal/frame.say feel
	 */
	private static HashMap<String, HashSet<Vector>> portalBlocks;
	private static HashMap<String, HashSet<Vector>> frameBlocks;
	private static HashMap<String, HashSet<Vector>> activatorBlocks;

	/*
	 * This set of portal objects is for actual correlating of source portal
	 * locations to destination locations.
	 */
	private static HashSet<Portal> portals;

	/* Constructor, pass a handle to the plugin for configuration reading. */
	PortalHandler(Main plugin) {
		this.plugin = plugin;
		this.logger = plugin.getLogger();
		this.DEBUG_LEVEL = Level.INFO;
		this.config = plugin.getConfig();

	}

	/*
	 * Runs through each portal frame block and activator block, checks that they are still the
	 * correct material and if not, returns the portal number of the first incorrect
	 * material block it finds.
	 */
	String integrityCheck() { // Location loc) {
		String frameMaterialName = config.getString("PortalMaterial");
		Iterator<Vector> frameVecs;
		Location loc;
		logger.log(DEBUG_LEVEL, "Checking portal frames");
		for (Map.Entry<String, HashSet<Vector>> frameEntries : frameBlocks.entrySet()) {
			String worldName = frameEntries.getKey();
			World world = Bukkit.getWorld(worldName);
			frameVecs = frameEntries.getValue().iterator();
			while (frameVecs.hasNext()) {
				Vector vec = frameVecs.next();
				loc = new Location(world, vec.getX(), vec.getY(), vec.getZ());
				if ( ! loc.getBlock().getType().name().equals(Material.getMaterial(frameMaterialName).name())) {
					logger.log(DEBUG_LEVEL, "Block is not frame material at " + loc.toString());
					return getPortalFromFrame(loc);
				}
				logger.log(DEBUG_LEVEL, loc.getBlock().getType().name() + " at " + loc.toString());
			}
		}
		logger.log(DEBUG_LEVEL, "Checking activators...");
		Iterator<Vector> activatorVecs;
		ArrayList<String> activators = (ArrayList<String>) config.getStringList("PortalActivators");
		for (Map.Entry<String, HashSet<Vector>> activatorEntries : activatorBlocks.entrySet()) {
			String worldName = activatorEntries.getKey();
			World world = Bukkit.getWorld(worldName);
			//logger.info("Checking " + worldName + " activators...");
			activatorVecs = activatorEntries.getValue().iterator();
			while (activatorVecs.hasNext()) {
				Vector vec = activatorVecs.next();
				loc = new Location(world, vec.getX(), vec.getY(), vec.getZ());
				if (!activators.contains(loc.getBlock().getType().name())) {
					logger.log(DEBUG_LEVEL, "Bad activator found at " + loc.toVector().toString() + "!");
					return getPortalFromActivator(loc);
				}
			}
		}
		return null;
	}

	/*
	 * Tests a given (floored) location to see if it is in the boundaries of any
	 * configured portals.
	 */
	boolean isInAPortal(Location loc) {
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
//	public boolean isInAFrame(Location loc) {
//		if (!frameBlocks.containsKey(loc.getWorld().getName())){
////			logger.info("No portals in world: " + loc.getWorld().getName());
////			logger.info("Worlds are: " + frameBlocks.toString());
//			return false;
//		}
////		logger.info("isInAFrame: " + frameBlocks.toString());
//		return frameBlocks.get(loc.getWorld().getName()).contains(loc.toVector());
//	}

	/*
	 * Tests whether a given block is part of a COMPLETE portal. This includes
	 * the frame blocks as well as the 'activating' blocks placed along the
	 * inside of the bottom of the frame.
	 */
	Float getCompletePortalVectors(Block block, ArrayList<String> frameVecs, ArrayList<String> activatorVecs, ArrayList<String> vectors) {
		FileConfiguration config = plugin.getConfig();
		Float yaw = null;

		if (isInAPortal(block.getLocation())) {
			logger.log(DEBUG_LEVEL, "Activation block set in already active portal. Doing nothing.");
			return null;
		}
		logger.log(DEBUG_LEVEL, "Portal is not in an active portal. Continuing.");
		ArrayList<String> activators;
		activators = (ArrayList<String>) config.getStringList("PortalActivators");
		if (!activators.contains(block.getType().name())) {
			logger.log(DEBUG_LEVEL, "Placed block is not an activator.");
			return null;
		}
		logger.log(DEBUG_LEVEL, "Placed block is an activator block. Continuing.");
		String currentActivatorName = block.getType().name();
		String frameMaterialName = config.getString("PortalMaterial");
		Block firstFrameBaseBlock = block.getLocation().add(new Vector(0, -1 ,0)).getBlock();

		//x = Easting
		//y = Altitude
		//z = Southing
		//Check if activator block was placed on a frame block
		if ( ! firstFrameBaseBlock.getType().name().equals(Material.getMaterial(frameMaterialName).name())) {
			logger.log(DEBUG_LEVEL, "Placed activator block is on " + firstFrameBaseBlock.getType().name() + ", not " + Material.getMaterial(frameMaterialName).name());
			return null;
		}

		logger.log(DEBUG_LEVEL, "Placed block is over a portal frame block. Continuing.");
		
		Block activatorBlock;
		HashMap<String, Vector> unitVectors = new HashMap<>();
		unitVectors.put("NORTH", new Vector(0,0,-1));
		unitVectors.put("SOUTH", new Vector(0, 0,1));
		unitVectors.put("EAST", new Vector(1,0,0));
		unitVectors.put("WEST", new Vector(-1,0,0));
		
//		String[] cardinalDirectionsString = new String[] {};
		List<String> cardinalDirections = Arrays.asList("NORTH", "SOUTH", "EAST", "WEST");
		
		//Find other activator blocks
		ArrayList<Block> activatorBlocks = new ArrayList<>();
		for (String direction : cardinalDirections) {
//		Iterator<String> unitIter = cardinalDirections.iterator();
//		while (unitIter.hasNext()) {
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
		try {
//		if (actIter.hasNext()) {
			activatorBlock = actIter.next();
			//Check for portal base under activator block
			baseBlock = new Location(activatorBlock.getWorld(), activatorBlock.getX(), activatorBlock.getY() - 1, activatorBlock.getZ()).getBlock();
			northMost = activatorBlock.getLocation().getBlockZ();
			westMost = activatorBlock.getLocation().getBlockX();
			//Check for portal base under activator block
			if (! activatorBlock.getLocation().add(new Vector(0, -1, 0)).getBlock().getType().name().equals(Material.getMaterial(frameMaterialName).name())) {
				logger.log(DEBUG_LEVEL, "Missing portal base under an activator block.");
				return null;
			}
			logger.log(DEBUG_LEVEL, "Adding base block at: " + baseBlock.getLocation().toVector().toString());
			baseVecs.add(baseBlock.getLocation().toVector().toString());
//		}
		} finally {}
		
		while (actIter.hasNext()) {
			activatorBlock = actIter.next();
			//Check for portal base under activator block
			baseBlock = new Location(activatorBlock.getWorld(), activatorBlock.getX(), activatorBlock.getY()-1, activatorBlock.getZ()).getBlock();
			if ( ! baseBlock.getType().name().equals(Material.getMaterial(frameMaterialName).name())) {
				logger.log(DEBUG_LEVEL, "Missing portal base under an activator block.");
				return null;
			}
			logger.log(DEBUG_LEVEL, "Adding base block at: " + baseBlock.getLocation().toVector().toString());
			baseVecs.add(baseBlock.getLocation().toVector().toString());
			if (activatorBlock.getLocation().getBlockZ() < northMost) {
				northMost = activatorBlock.getLocation().getBlockZ();
			}
			if (activatorBlock.getLocation().getBlockX() < westMost) {
				westMost = activatorBlock.getLocation().getBlockX();
			}
		}
		logger.log(DEBUG_LEVEL, "Northwest activator found at: X=" + westMost + ", Z=" + northMost);
		
		//Find the most southeast coordinate of activator block
		actIter = activatorBlocks.iterator();
		int southMost;
		int eastMost;
		
		//Eclipse doesn't like enclosing this in an if (.hasNext()) {} block
		try {
//		if (actIter.hasNext()) {
			activatorBlock = actIter.next();
			southMost = activatorBlock.getLocation().getBlockZ();
			eastMost = activatorBlock.getLocation().getBlockX();
//		}
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
		logger.log(DEBUG_LEVEL, "Southeast activator found at: X=" + eastMost + ", Z=" + southMost);
		
		ArrayList<Block> wallNW = new ArrayList<>();
		ArrayList<Block> wallSE = new ArrayList<>();
		
		//Look for portal walls North then West of the most Northwest activator block
		Location activatorNW = new Location(block.getWorld(), westMost, block.getLocation().getBlockY(), northMost);
		Location activatorSE = new Location(block.getWorld(), eastMost, block.getLocation().getBlockY(), southMost);

		logger.log(DEBUG_LEVEL, "NW activator at: " + activatorNW.toVector().toString());

		//North/South oriented portal
		//North of activatorNW
		Location testLoc = new Location(activatorNW.getWorld(), activatorNW.getX(), activatorNW.getY(), activatorNW.getZ()-1);
		logger.log(DEBUG_LEVEL, "NW activator at: " + activatorNW.toVector().toString());
		logger.log(DEBUG_LEVEL, "Look for portal: " + testLoc.toVector().toString());

		if (testLoc.getBlock().getType().name().equals(Material.getMaterial(frameMaterialName).name())) {
			wallNW.add(testLoc.getBlock());
			//South of activatorSE
			logger.log(DEBUG_LEVEL, "SE activator at: " + activatorSE.toVector().toString());
			logger.log(DEBUG_LEVEL, "Look for portal: " + testLoc.toVector().toString());
			testLoc = new Location(activatorSE.getWorld(), activatorSE.getX(), activatorSE.getY(), activatorSE.getZ()+1);
			if ( ! testLoc.getBlock().getType().name().equals(Material.getMaterial(frameMaterialName).name())) {
				logger.log(DEBUG_LEVEL, "Block at " + testLoc.toVector().toString() + ": " + testLoc.getBlock().getType().name());
				logger.log(DEBUG_LEVEL, "Portal is missing a South wall.");
				return null;
			}
			wallSE.add(testLoc.getBlock());
			yaw = 90F; //West
		} else {
			logger.log(DEBUG_LEVEL, "Block at " + testLoc.toVector().toString() + ": " + testLoc.getBlock().getType().name());
		}

		//East/West oriented portal
		//West of activatorNW
		testLoc = new Location(activatorNW.getWorld(), activatorNW.getX()-1, activatorNW.getY(), activatorNW.getZ());
		logger.log(DEBUG_LEVEL, "NW activator at: " + activatorNW.toVector().toString());
		logger.log(DEBUG_LEVEL, "Look for portal: " + testLoc.toVector().toString());

		if (testLoc.getBlock().getType().name().equals(Material.getMaterial(frameMaterialName).name())) {
			wallNW.add(testLoc.getBlock());
			//East of activatorSE
			testLoc = new Location(activatorSE.getWorld(), activatorSE.getX()+1, activatorSE.getY(), activatorSE.getZ());
			logger.log(DEBUG_LEVEL, "SE activator at: " + activatorSE.toVector().toString());
			logger.log(DEBUG_LEVEL, "Look for portal: " + testLoc.toVector().toString());
			if ( ! testLoc.getBlock().getType().name().equals(Material.getMaterial(frameMaterialName).name())) {
				logger.log(DEBUG_LEVEL, "Block at " + testLoc.toVector().toString() + ": " + testLoc.getBlock().getType().name());
				logger.log(DEBUG_LEVEL, "Portal is missing an East wall.");
				return null;
			}
			wallSE.add(testLoc.getBlock());
			yaw = 180F; //North
		} else {
			logger.log(DEBUG_LEVEL, "Block at " + testLoc.toVector().toString() + ": " + testLoc.getBlock().getType().name());
		}
		if (wallSE.size() + wallNW.size() < 2) {
			logger.log(DEBUG_LEVEL, "Portal is missing a North/West wall.");
			return null;
		}

		logger.log(DEBUG_LEVEL, "Portal walls adjacent to activation blocks found. Continuing.");
		//Find top of North/West wall
		Block nextBlock = wallNW.get(0).getLocation().add(new Vector(0,1,0)).getBlock();
		while (nextBlock.getType().name().equals(Material.getMaterial(frameMaterialName).name())) {
			wallNW.add(nextBlock);
			nextBlock = nextBlock.getLocation().add(new Vector(0,1,0)).getBlock();
		}
		logger.log(DEBUG_LEVEL, "North/West wall height: " + wallNW.size());

		//Find top of South/East wall
		nextBlock = wallSE.get(0).getLocation().add(new Vector(0,1,0)).getBlock();
		while (nextBlock.getType().name().equals(Material.getMaterial(frameMaterialName).name())) {
			wallSE.add(nextBlock);
			nextBlock = nextBlock.getLocation().add(new Vector(0,1,0)).getBlock();
		}
		logger.log(DEBUG_LEVEL, "South/East wall height: " + wallSE.size());
		
		int portalHeight = java.lang.Math.min(wallNW.size(), wallSE.size());
		logger.log(DEBUG_LEVEL, "Initial portal height: " + portalHeight);
		Block portalTopBlock;
		Block currentActivatorBlock;
		actIter = activatorBlocks.iterator();
		
		//Adjust portalHeight to fit actual roof height
		currentActivatorBlock = activatorBlocks.get(0);
		for (int i=portalHeight; i>=2; i--) {
			portalTopBlock = new Location(currentActivatorBlock.getWorld(), currentActivatorBlock.getX(), currentActivatorBlock.getY() + i, currentActivatorBlock.getZ()).getBlock();
			logger.log(DEBUG_LEVEL, "Height test: " + i + " Material: " + portalTopBlock.getType().name());
			logger.log(DEBUG_LEVEL, "Test at: " + portalTopBlock.getLocation().toVector().toString());
			if (portalTopBlock.getType().name().equals(Material.getMaterial(frameMaterialName).name())) {
				portalHeight = i;
				logger.log(DEBUG_LEVEL, "Portal height adjusted to: " + portalHeight);
			}
		}

		//Portal must be at least 2m tall
		if (portalHeight < 2) {
			logger.log(DEBUG_LEVEL, "Portal walls are not tall enough.");
			return null;
		}
		logger.log(DEBUG_LEVEL, "Portal walls found and are tall enough. Continuing.");
		
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
//			logger.info("Roof Block: " + portalTopBlock.getLocation().toVector().toString() + ": " + portalTopBlock.getType().name());
			if ( ! portalTopBlock.getType().name().equals(Material.getMaterial(frameMaterialName).name())) {
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
	void updatePortals() {
		portals = new HashSet<>();
		portalBlocks = new HashMap<>();
		frameBlocks = new HashMap<>();
		activatorBlocks = new HashMap<>();
		Material mat = null;
		try {
			mat = Material.getMaterial(config.getString("PortalMaterial"));
		} finally {
			if (mat == null) {
				logger.warning("Could not read configured portal material! Aborting portal update.");
				return;
			}
		}
		
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
		ArrayList<String> activatorStringsA;
		Float yawA;
		World worldB;
		ArrayList<String> vectorStringsB;
		ArrayList<String> frameStringsB;
		Float yawB;
		HashSet<Vector> tempVecSet = new HashSet<>();
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
	
				ArrayList<Vector> vectorsA = new ArrayList<>();
				ArrayList<Vector> vectorsB = new ArrayList<>();
				ArrayList<Vector> frameVecsA = new ArrayList<>();
				ArrayList<Vector> frameVecsB = new ArrayList<>();
				
				String worldAName = config.getString("portals." + portalNumber + ".A.world");
				String yawAString = config.getString("portals." + portalNumber + ".A.yaw");
				if (worldAName == null) {
					logger.info("Error reading World A Name configuration!");
//					logger.info("Attemting to read config for Portal " + portalNumber);
					return;
				}
				worldA = Bukkit.getWorld(worldAName);
				if (worldA == null) {
					logger.warning("Failed to locate world: " + worldAName + " skipping portal number " + portalNumber);
					continue;
				}
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
				if (worldB == null) {
					logger.warning("Failed to locate world: " + worldBName + " skipping portal number " + portalNumber);
					continue;
				}if (yawBString == null) {
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
						tempVecSet = new HashSet<>();
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
						tempVecSet = new HashSet<>();
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
						tempVecSet = new HashSet<>();
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
						tempVecSet = new HashSet<>();
					}
				}
				portals.add( new Portal(portalNumber, worldA, vectorsA, frameVecsA, new ArrayList<>(), yawA, worldB, vectorsB, frameVecsB, yawB));
			} else { //portalNumber = 0
				Iterator<String> activatorIter = activators.iterator();
				String activator;
				while (activatorIter.hasNext()) {
					activator = activatorIter.next();
					frameStringsA = (ArrayList<String>) config.getStringList("portals." + portalNumber + "." + activator + ".frame");
					activatorStringsA = (ArrayList<String>) config.getStringList("portals." + portalNumber + "." + activator + ".activators");
		
					ArrayList<Vector> frameVecsA = new ArrayList<>();
					ArrayList<Vector> activatorVecsA = new ArrayList<>();
					
					String worldAName = config.getString("portals." + portalNumber + "." + activator + ".world");
					String yawAString = config.getString("portals." + portalNumber + "." + activator + ".yaw");
					if (worldAName != null) {
						worldA = Bukkit.getWorld(worldAName);
						if (yawAString == null) {
							logger.info("Error reading yawA from configuration!");
							return;
						}
						yawA = Float.parseFloat(yawAString);
						// Convert string lists for A to vector lists
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
								tempVecSet = new HashSet<>();
							}
						}
						// Side A activator vectors
						vectorsIterator = activatorStringsA.iterator();
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
							activatorVecsA.add(vec);
							// Add this vector to the activatorBlocks set, keyed by world
							if (activatorBlocks.containsKey(worldA.getName())) {
								activatorBlocks.get(worldA.getName()).add(vec);
							} else {
								tempVecSet.add(vec);
								activatorBlocks.put(worldA.getName(), tempVecSet);
								tempVecSet = new HashSet<>();
							}
						}
						portals.add( new Portal(portalNumber + "." + activator, worldA, new ArrayList<>(), frameVecsA, activatorVecsA, yawA, null, new ArrayList<>(), new ArrayList<>(), 0F));
						//logger.info("portals: " + portals.toString());
	//					logger.info("portalBlocks: " + portalBlocks.toString());
					} //WorldAName != null
				} //activator while loop
			} //portalNumber = 0
		} //portalKeys while loop
		
		//Set all frame blocks to frame material
		Iterator<Vector> frameVecs;
		for (Map.Entry<String, HashSet<Vector>> frameEntries : frameBlocks.entrySet()) {
			String worldName = frameEntries.getKey();
			World world = Bukkit.getWorld(worldName);
			frameVecs = frameEntries.getValue().iterator();
			while (frameVecs.hasNext()) {
				Vector vec = frameVecs.next();
				Block block = new Location(world, vec.getX(), vec.getY(), vec.getZ()).getBlock();
				block.setType(mat);
			}
		}
	} //Update portals member

	/*
	 * Returns the destination portal block location that corresponds to the
	 * given portal block location. Returns null if the location is not part of
	 * a configured portal.
	 */
	Location getDestination(Entity entity, Location loc) {
		for (Portal portal : portals) {
//		Iterator<Portal> portalsIterator = portals.iterator();
//		while (portalsIterator.hasNext()) {
//			Portal portal = portalsIterator.next();
			if (portal.isInPortal(loc)) {
				logger.log(DEBUG_LEVEL, entity.getName() + " is in portal number " + portal.getID());
				return portal.getDestination(entity, loc);
			}
		}
		return null;
	}

	private String getPortalFromFrame(Location loc) {
//		logger.info("Checking location: " + loc.toVector().toString());
		for (Portal portal : portals) {
			if (portal.isInFrame(loc)) {
				logger.log(DEBUG_LEVEL, "Frame location belongs to portal " + portal.getID());
				return portal.getID();
			}
		}
		return null;
	}
	
	private String getPortalFromActivator(Location loc) {
//		logger.info("Checking location: " + loc.toVector().toString());
		for (Portal portal : portals) {
			if (portal.isInActivators(loc)) {
				logger.log(DEBUG_LEVEL, "Activator location belongs to portal " + portal.getID());
				return portal.getID();
			}
		}
		return null;
	}
}
