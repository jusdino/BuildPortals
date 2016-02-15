# BuildPortals
Bukkit/Spigot plugin to allow players to build their own inter-world portals

Feb 15, 2015:

Status:

This project is still in progress. The end goal is to have a plugin that will support player-built (command-less) portals of variable size that can teleport players between worlds.

Currently, it is 70% functional; it allows players to build portals, links source and destination portals, and correctly tracks configurations, mapping unevenly sized portals for predictable teleportation to the other side.

Use:

Once installed, any player can build a portal using a designated portal building material (Emerald Blocks by default) in a nether-portal-like rectangle, with at least large enough interior for a player to walk through and place an 'activator block' (any of Redstone blocks, Gold blocks or Diamond blocks by default) on each bottom block along the interior of the portal. They then build another portal where they would like to connect, placing matching 'activator block's in this portal. Once two complete and like-activated portals are built, the plugin converts the portal interior (including activator blocks) to air and links the portals! Each portal can be built by any player, in any world. Portals activated with each material can be started independantly with no conflict issues.

Planned updates:

-Handling portal destruction (currently, once established, portals are permanant).
-Handling player pitch/yaw when teleporting
-Support for 'vehicle' class objects (horses, minecarts, etc.) to be teleported with a player.

If someone happens across this project, feel free to critique. I welcome tips/criticism/suggestions!
