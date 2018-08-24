# BuildPortals
Bukkit/Spigot server plugin to allow players to build their own inter-world portals without the use of commands. These portals support teleporting players on horses as well as minecarts.

## August 23, 2018: Updates in progress!
* Boat teleporting in progress!
* Update for 1.13 in progress!

Status:

This project is at bukkit.org / curse at:
https://mods.curse.com/bukkit-plugins/minecraft/buildportals

and at spigotmc.org:
https://www.spigotmc.org/resources/buildportals.21922/

## Features:
### Livestock teleporting:
* Horses
* Cows
* Pigs
* Sheep
* All of the above while attached to player via lead
* All of the above whilst in a minecart (if they fit)
* Horses/Pigs whilst riding them as well

### Minecart teleporting:
* minecarts with players
* empty minecarts
* command minecarts
* minecarts with chests
* minecarts with hoppers
* minecarts with TNT
* minecarts with Villagers or animals

## Use:

Once installed, any player can build a teleportation portal by doing:
 1) Using a designated portal building material (Emerald Blocks by default) to make a nether-portal-like rectangle, with interior at least large enough for a player to walk through and;
 2) Place an 'activator block' (any of Redstone blocks, Gold blocks or Diamond blocks by default) on each bottom block along the interior of the portal.
 3) Then build another portal where they would like to connect, placing matching 'activator block's in this portal.
 
Once two complete and like-activated portals are built, the plugin converts the portal interior (including activator blocks) to air and links the portals! Each portal can be built by any player, in any world. Portals activated with each material can be started independently with no conflict issues.

The plugin does support in-game configuration changes now:

 * '/BP SetMaterial \<MaterialName\>' - You can change the portal frame material. This will convert all existing portals to the new material and also allow building new portals from the new named material.
 
 * '/BP AddActivator \<MaterialName\>' - You can add a new activator material. This allows activating portals with a new block.
 
 * '/BP RemoveActivator \<MaterialName\>' - You can remove an activator material. This will disallow activating portals with the named material.
 
 * '/BP ListActivators \<MaterialName\>' - This will list all currently configured activator materials. 

## Permissions:
####  buildportals.*:
    description: Gives access to editing portal and activator materials.
    default: op
####  buildportals.listmaterial:
    description: Gives access to view the portal material.
    default: true
####  buildportals.listactivators:
    description: Give access to view the activator material list.
    default: true
####  buildportals.activate:
    description: Allows activating a new portal.
    default: true
####  buildportals.teleport:
    description: Allows using a portal.
    default: true


If someone happens across this project, feel free to critique. I welcome tips/criticism/suggestions!
