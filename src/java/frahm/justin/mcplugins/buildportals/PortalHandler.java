package java.frahm.justin.mcplugins.buildportals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
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
						if (origin == loc.toVector()) {
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
						if (origin == loc.toVector()) {
							return new Location(aWorld, destination.getBlockX(), destination.getBlockY(),
									destination.getBlockZ());
						}
					}
				}
			}
			return null;
		}
	}

	static Main plugin;

	/*
	 * Map of portal block location sets, keyed by world name Intended for fast
	 * testing of whether a location is in a configured portal.
	 */
	static HashMap<String, HashSet<Vector>> portalBlocks = new HashMap<>();

	/*
	 * This set of portal objects is for actual correlating of source portal
	 * locations to destination locations.
	 */
	static HashSet<Portal> portals = new HashSet<>();

	/* Constructor, pass a handle to the plugin for configuration reading. */
	public PortalHandler(Main plugin) {
		this.plugin = plugin;
	}

	/*
	 * Tests a given location to see if it is in the boundaries of any
	 * configured portals.
	 */
	public boolean isInAPortal(Location loc) {
		return portalBlocks.get(loc.getWorld()).contains(loc.toVector());
	}

	/*
	 * Tests whether a given block is part of a COMPLETE portal. This includes
	 * the frame blocks as well as the 'activating' blocks placed along the
	 * inside of the bottom of the frame.
	 */
	public boolean isCompletePortal(Block block) {
		// TODO: Build tester function to see if given (recently placed)
		// block completes a portal
		return false;
	}

	/*
	 * Reads the configuration state, populating the portalBlocks and other
	 * PortalHandler fields based on the portal configuration.
	 */
	public void updatePortals() {
		FileConfiguration config = plugin.getConfig()
		Set<String> portalKeys = config.getConfigurationSection("portals").getKeys(false);
		Iterator<String> configIterator = portalKeys.iterator();
		while (configIterator.hasNext()) {
			/*TODO: Read config section, assemble data into two ArrayList
			 * objects which represent the vectors for each portal block on side
			 * A and B, construct a Portal object, add that object to the
			 * portals HashSet.
			 */
			
		}
	}

	/*
	 * Returns the destination portal block location that corresponds to the
	 * given portal block location. Returns null if the location is not part of
	 * a configured portal.
	 */
	public Location getDestination(Location source) {
		Location flooredSource = new Location(source.getWorld(), source.getBlockX(), source.getBlockY(),
				source.getBlockZ());
		Iterator<Portal> portalsIterator = portals.iterator();
		while (portalsIterator.hasNext()) {
			Portal portal = portalsIterator.next();
			if (portal.isInPortal(flooredSource)) {
				return portal.getDestination(flooredSource);
			}
		}
		return null;
	}
}
