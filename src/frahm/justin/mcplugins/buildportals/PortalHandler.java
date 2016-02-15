package frahm.justin.mcplugins.buildportals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
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
		World aWorld;
		World bWorld;
		Vector destOffset = new Vector(0.5, 0, 0.5);

		/*
		 * Constructor for a portal object, which includes a collection of
		 * vectors representing sides A and B of the portals as well as which
		 * world each side is in.
		 */
		public Portal(World aWorld, ArrayList<Vector> vectorsA, World bWorld, ArrayList<Vector> vectorsB) {
			this.vectorsA = vectorsA;
			this.aWorld = aWorld;
			this.vectorsB = vectorsB;
			this.bWorld = bWorld;
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
		 * Returns the location of the portal destination block that corresponds
		 * to the source location.
		 * 
		 * Returns Null if the location is not actually in the portal.
		 * 
		 * NOTE: the given location MUST be based on the floored coordinates.
		 */
		public Location getDestination(Location loc) {
			if (loc.getWorld() == aWorld) {
				if (vectorsA.contains(loc.toVector())) {
					Iterator<Vector> iterA = vectorsA.iterator();
					Iterator<Vector> iterB = vectorsB.iterator();
					while (iterA.hasNext()) {
						if (!iterB.hasNext()) {
							iterB = vectorsB.iterator();
						}
						Vector origin = (Vector) iterA.next();
						Vector destination = (Vector) iterB.next();
						if (origin.equals(loc.toVector())) {
							return new Location(bWorld, destination.getBlockX(), destination.getBlockY(),
									destination.getBlockZ()).add(destOffset);
						}
					}
				}
			}
			if (loc.getWorld() == bWorld) {
				if (vectorsB.contains(loc.toVector())) {
					Iterator<Vector> iterB = vectorsB.iterator();
					Iterator<Vector> iterA = vectorsA.iterator();
					while (iterB.hasNext()) {
						if (!iterA.hasNext()) {
							iterA = vectorsA.iterator();
						}
						Vector origin = (Vector) iterB.next();
						Vector destination = (Vector) iterA.next();
						if (origin.equals(loc.toVector())) {
							return new Location(aWorld, destination.getBlockX(), destination.getBlockY(),
									destination.getBlockZ()).add(destOffset);
						}
					}
				}
			}
			return null;
		}
	}

	Main plugin;
	ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();

	/*
	 * Map of portal block location sets, keyed by world name Intended for fast
	 * testing of whether a location is in a configured portal.
	 */
	static HashMap<String, HashSet<Vector>> portalBlocks = new HashMap<String, HashSet<Vector>>();

	/*
	 * This set of portal objects is for actual correlating of source portal
	 * locations to destination locations.
	 */
	static HashSet<Portal> portals = new HashSet<Portal>();

	/* Constructor, pass a handle to the plugin for configuration reading. */
	public PortalHandler(Main plugin) {
		this.plugin = plugin;
	}

	/*
	 * Tests a given (floored) location to see if it is in the boundaries of any
	 * configured portals.
	 */
	public boolean isInAPortal(Location loc) {
		if (!portalBlocks.containsKey(loc.getWorld().getName())){
//			console.sendMessage("No portals in world: " + loc.getWorld().getName());
//			console.sendMessage("Worlds are: " + portalBlocks.toString());
			return false;
		}
		return portalBlocks.get(loc.getWorld().getName()).contains(loc.toVector());
	}

	/*
	 * Tests whether a given block is part of a COMPLETE portal. This includes
	 * the frame blocks as well as the 'activating' blocks placed along the
	 * inside of the bottom of the frame.
	 */
	public ArrayList<String> getCompletePortalVectors(Block block) {
		// TODO: Build tester function to see if given (recently placed)
		// block completes a portal
		FileConfiguration config = plugin.getConfig();
		if (isInAPortal(block.getLocation())) {
			console.sendMessage("Activation block set in already active portal. Doing nothing.");
			return null;
		}
		console.sendMessage("Portal is not in an active portal. Continuing.");
		ArrayList<String> activators;
		activators = (ArrayList<String>) config.getStringList("PortalActivators");
		if (!activators.contains(block.getType().name())) {
			console.sendMessage("Placed block is not an activator.");
			return null;
		}
		console.sendMessage("Placed block is an activator block. Continuing.");
		String currentActivatorName = block.getType().name();
		String frameMaterialName = config.getString("PortalMaterial");
		Block frameBaseBlock = block.getLocation().add(new Vector(0, -1 ,0)).getBlock();
		Block firstFrameBaseBlock = frameBaseBlock;

		//x = Easting
		//y = Altitude
		//z = Southing
		//Check if activator block was placed on a frame block
		if ( frameMaterialName != firstFrameBaseBlock.getType().name()) {
			console.sendMessage("Placed activator block is on " + frameBaseBlock.getType().name() + ", not " + frameMaterialName);
			return null;
		}
		
		console.sendMessage("Placed block is over a portal frame block. Continuing.");
		
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
		
		//Eclipse doesn't like enclosing this in an if (.hasNext()) block
		try {
			activatorBlock = actIter.next();
			northMost = activatorBlock.getLocation().getBlockZ();
			westMost = activatorBlock.getLocation().getBlockX();
			//Check for portal base under activator block
			if (activatorBlock.getLocation().add(new Vector(0, -1, 0)).getBlock().getType().name() != frameMaterialName) {
				console.sendMessage("Missing portal base under an activator block.");
				return null;
			}
		} finally {}
		
		//Find the most northwest coordinate of activator block
		while (actIter.hasNext()) {
			activatorBlock = actIter.next();
			//Check for portal base under activator block
			if (activatorBlock.getLocation().add(new Vector(0, -1, 0)).getBlock().getType().name() != frameMaterialName) {
				console.sendMessage("Missing portal base under an activator block.");
				return null;
			}
			if (activatorBlock.getLocation().getBlockZ() < northMost) {
				northMost = activatorBlock.getLocation().getBlockZ();
			}
			if (activatorBlock.getLocation().getBlockX() < westMost) {
				westMost = activatorBlock.getLocation().getBlockX();
			}
		}
		console.sendMessage("Northwest activator found at: X=" + westMost + ", Z=" + northMost);
		
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
		console.sendMessage("Southeast activator found at: X=" + eastMost + ", Z=" + southMost);
		
		ArrayList<Block> wallNW = new ArrayList<Block>();
		ArrayList<Block> wallSE = new ArrayList<Block>();
		
		//Look for portal walls North then West of the most Northwest activator block
		Location activatorNW = new Location(block.getWorld(), westMost, block.getLocation().getBlockY(), northMost);
		Location activatorSE = new Location(block.getWorld(), eastMost, block.getLocation().getBlockY(), southMost);
		
		console.sendMessage("NW activator at: " + activatorNW.toVector().toString());		

		//North/South oriented portal
		//North of activatorNW
		Location testLoc = new Location(activatorNW.getWorld(), activatorNW.getX(), activatorNW.getY(), activatorNW.getZ()-1);
		console.sendMessage("NW activator at: " + activatorNW.toVector().toString());		
		console.sendMessage("Look for portal: " + testLoc.toVector().toString());
		
		if (testLoc.getBlock().getType().name() == frameMaterialName) {
			wallNW.add(testLoc.getBlock());
			//South of activatorSE
			console.sendMessage("SE activator at: " + activatorSE.toVector().toString());
			console.sendMessage("Look for portal: " + testLoc.toVector().toString());
			testLoc = new Location(activatorSE.getWorld(), activatorSE.getX(), activatorSE.getY(), activatorSE.getZ()+1);
			if (testLoc.getBlock().getType().name() != frameMaterialName) {
				console.sendMessage("Block at " + testLoc.toVector().toString() + ": " + testLoc.getBlock().getType().name());
				console.sendMessage("Portal is missing a South wall.");
				return null;
			}
			wallSE.add(testLoc.getBlock());
		} else {
			console.sendMessage("Block at " + testLoc.toVector().toString() + ": " + testLoc.getBlock().getType().name());
		}

		//East/West oriented portal		
		//West of activatorNW
		testLoc = new Location(activatorNW.getWorld(), activatorNW.getX()-1, activatorNW.getY(), activatorNW.getZ());
		console.sendMessage("NW activator at: " + activatorNW.toVector().toString());
		console.sendMessage("Look for portal: " + testLoc.toVector().toString());

		if (testLoc.getBlock().getType().name() == frameMaterialName) {
			wallNW.add(testLoc.getBlock());
			//East of activatorSE
			testLoc = new Location(activatorSE.getWorld(), activatorSE.getX()+1, activatorSE.getY(), activatorSE.getZ());
			console.sendMessage("SE activator at: " + activatorSE.toVector().toString());
			console.sendMessage("Look for portal: " + testLoc.toVector().toString());
			if (testLoc.getBlock().getType().name() != frameMaterialName) {
				console.sendMessage("Block at " + testLoc.toVector().toString() + ": " + testLoc.getBlock().getType().name());
				console.sendMessage("Portal is missing an East wall.");
				return null;
			}
			wallSE.add(testLoc.getBlock());
		} else {
			console.sendMessage("Block at " + testLoc.toVector().toString() + ": " + testLoc.getBlock().getType().name());
		}
		if (wallSE.size() + wallNW.size() < 2) {
			console.sendMessage("Portal is missing a North/West wall.");
			return null;
		}
		
		console.sendMessage("Portal walls adjacent to activation blocks found. Continuing.");
		//Find top of North/West wall
		Block nextBlock = wallNW.get(0).getLocation().add(new Vector(0,1,0)).getBlock();
		while (nextBlock.getType().name() == frameMaterialName) {
			wallNW.add(nextBlock);
			nextBlock = nextBlock.getLocation().add(new Vector(0,1,0)).getBlock();
		}

		//Find top of South/East wall
		nextBlock = wallSE.get(0).getLocation().add(new Vector(0,1,0)).getBlock();
		while (nextBlock.getType().name() == frameMaterialName) {
			wallSE.add(nextBlock);
			nextBlock = nextBlock.getLocation().add(new Vector(0,1,0)).getBlock();
		}
		
		int portalHeight = java.lang.Math.min(wallNW.size(), wallSE.size());
		console.sendMessage("Initial portal height: " + portalHeight);
		Block portalTopBlock;
		Block currentActivatorBlock;
		actIter = activatorBlocks.iterator();
		ArrayList<String> portalBlocks = new ArrayList<String>();
		
		//Adjust portalHeight to fit actual roof height
		currentActivatorBlock = activatorBlocks.get(0);
		for (int i=portalHeight; i>=2; i--) {
			portalTopBlock = new Location(currentActivatorBlock.getWorld(), currentActivatorBlock.getX(), currentActivatorBlock.getY() + i, currentActivatorBlock.getZ()).getBlock();
			console.sendMessage("Height test: " + i + "Material: " + portalTopBlock.getType().name());
			console.sendMessage("Test at: " + portalTopBlock.getLocation().toVector().toString());
			if (portalTopBlock.getType().name() == frameMaterialName) {
				portalHeight = i;
				console.sendMessage("Portal height adjusted to: " + portalHeight);
			}
		}

		//Portal must be at least 2m tall
		if (portalHeight < 2) {
			console.sendMessage("Portal walls are not tall enough.");
			return null;
		}
		console.sendMessage("Portal walls found and are tall enough. Continuing.");
		
		while (actIter.hasNext()) {
			currentActivatorBlock = actIter.next();
			for (int i=0; i<portalHeight; i++) {
				portalBlocks.add(new Location(currentActivatorBlock.getWorld(), currentActivatorBlock.getX(), currentActivatorBlock.getY() + i, currentActivatorBlock.getZ()).toVector().toString());
			}
			portalTopBlock = new Location(currentActivatorBlock.getWorld(), currentActivatorBlock.getX(), currentActivatorBlock.getY() + portalHeight, currentActivatorBlock.getZ()).getBlock();
			console.sendMessage("Roof Block: " + portalTopBlock.getLocation().toVector().toString() + ": " + portalTopBlock.getType().name());
			if (portalTopBlock.getType().name() != frameMaterialName) {
				console.sendMessage("Portal is missing a roof block");
				return null;
			}
		}
		
		console.sendMessage("Portal is complete!");
		console.sendMessage("Interior blocks: " + portalBlocks.toString());
		return portalBlocks;
	}

	/*
	 * Reads the configuration state, populating the portalBlocks and other
	 * PortalHandler fields based on the portal configuration.
	 */
	public void updatePortals() {
		FileConfiguration config = plugin.getConfig();
		if (null == config) {
			return;
		}
		ConfigurationSection portalSection = config.getConfigurationSection("portals");
		if (null == portalSection) {
			return;
		}
		Set<String> portalKeys = portalSection.getKeys(false);
		if (null == portalKeys) {
			return;
		}
		portalKeys.remove("0");
		console.sendMessage("portalKeys: " + portalKeys.toString());
		Iterator<String> configIterator = portalKeys.iterator();
		Iterator<String> vectorsIterator;
		String portalNumber;
		ArrayList<String> vectorStringsA = new ArrayList<String>();
		ArrayList<String> vectorStringsB = new ArrayList<String>();
		ArrayList<Vector> vectorsA = new ArrayList<Vector>();
		ArrayList<Vector> vectorsB = new ArrayList<Vector>();
		World worldA;
		World worldB;
		HashSet<Vector> tempVecSet = new HashSet<Vector>();
		// Read vector string describing each portal, ends A and B
		while (configIterator.hasNext()) {
			portalNumber = configIterator.next();
			console.sendMessage("Loading configuration for portal number: " + portalNumber);
			vectorStringsA = (ArrayList<String>) config.getStringList("portals." + portalNumber + ".A.vec");
			vectorStringsB = (ArrayList<String>) config.getStringList("portals." + portalNumber + ".B.vec");
			String worldAName = plugin.config.getString("portals." + portalNumber + ".A.world");
			if (worldAName == null) {
				console.sendMessage("Error reading configuration!");
				return;
			}
			worldA = Bukkit.getWorld(worldAName);
			String worldBName = plugin.config.getString("portals." + portalNumber + ".B.world");
			if (worldBName == null) {
				console.sendMessage("Error reading configuration!");
				return;
			}
			worldB = Bukkit.getWorld(plugin.config.getString("portals." + portalNumber + ".B.world"));
			// Convert string lists for A and B to vector lists
			// Side A
			vectorsIterator = vectorStringsA.iterator();
			while (vectorsIterator.hasNext()) {
				String[] parts = vectorsIterator.next().split(",");
				if (parts.length != 3) {
					console.sendMessage("Error reading portal data!");
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

			vectorsIterator = vectorStringsB.iterator();
			// Side B
			while (vectorsIterator.hasNext()) {
				String[] parts = vectorsIterator.next().split(",");
				if (parts.length != 3) {
					console.sendMessage("Error reading portal data!");
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
			portals.add(new Portal(worldA, vectorsA, worldB, vectorsB));
			console.sendMessage("portals: " + portals.toString());
			console.sendMessage("portalBlocks: " + portalBlocks.toString());
		}
	}

	/*
	 * Returns the destination portal block location that corresponds to the
	 * given portal block location. Returns null if the location is not part of
	 * a configured portal.
	 */
	public Location getDestination(Location source) {
		Iterator<Portal> portalsIterator = portals.iterator();
		while (portalsIterator.hasNext()) {
			Portal portal = portalsIterator.next();
			if (portal.isInPortal(source)) {
				return portal.getDestination(source);
			}
		}
		return null;
	}
}
