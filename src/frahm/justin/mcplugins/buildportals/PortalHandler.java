package frahm.justin.mcplugins.buildportals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.ConsoleCommandSender;
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
									destination.getBlockZ());
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
									destination.getBlockZ());
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
			return false;
		}
		return portalBlocks.get(loc.getWorld().getName()).contains(loc.toVector());
	}

	/*
	 * Tests whether a given block is part of a COMPLETE portal. This includes
	 * the frame blocks as well as the 'activating' blocks placed along the
	 * inside of the bottom of the frame.
	 */
	public boolean isCompletePortal(Block block) {
		// TODO: Build tester function to see if given (recently placed)
		// block completes a portal
		return true;
	}

	/*
	 * Returns an ArrayList of Vectors representing the vector to every portal
	 * block in the portal's interior.
	 */
	public ArrayList<String> getPortalVectors(Block block) {
		// TODO: Write a real method to replace this tester
		ArrayList<String> vectors = new ArrayList<String>();
		vectors.add(block.getLocation().toVector().toString());
//		vectors.add(block.getLocation().add(0, 1, 0).toVector().toString());
//		vectors.add(block.getLocation().add(1, 0, 0).toVector().toString());
//		vectors.add(block.getLocation().add(1, 1, 0).toVector().toString());

		return vectors;
	}

	/*
	 * Reads the configuration state, populating the portalBlocks and other
	 * PortalHandler fields based on the portal configuration.
	 */
	public void updatePortals() {
		FileConfiguration config = plugin.getConfig();
		Set<String> portalKeys = config.getConfigurationSection("portals").getKeys(false);
		portalKeys.remove("0");
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
			worldA = Bukkit.getWorld(plugin.config.getString("portals." + portalNumber + ".A.world"));
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
