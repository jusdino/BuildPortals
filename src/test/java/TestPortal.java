import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import mock.BuildPortalsWorldMock;
import space.frahm.buildportals.ActivatedPortalFrame;
import space.frahm.buildportals.BuildPortals;
import space.frahm.buildportals.IncompletePortal;
import space.frahm.buildportals.Portal;
import space.frahm.buildportals.PortalFrame;

public class TestPortal {
    private ServerMock server;
    private WorldMock world0;
    private WorldMock world1;
    private static ArrayList<Vector> FRAME_INTERIOR_0 = new ArrayList<>(Arrays.asList(
        new Vector(0, 1, 0),
        new Vector(0, 2, 0)
    ));
    private static ArrayList<Vector> FRAME_EXTERIOR_0 = new ArrayList<>(Arrays.asList(
        new Vector(0, 0, 0),
        new Vector(1, 1, 0),
        new Vector(-1, 1, 0),
        new Vector(1, 2, 0),
        new Vector(-1, 2, 0),
        new Vector(0, 3, 0)
    ));
    private static ArrayList<Vector> FRAME_ACTIVATORS_0 = new ArrayList<>(Arrays.asList(
        new Vector(0, 1, 0)
    ));
    private static ArrayList<Vector> FRAME_INTERIOR_1 = new ArrayList<>(Arrays.asList(
        new Vector(10, 1, 0),
        new Vector(10, 2, 0)
    ));
    private static ArrayList<Vector> FRAME_EXTERIOR_1 = new ArrayList<>(Arrays.asList(
        new Vector(10, 0, 0),
        new Vector(10, 1, 1),
        new Vector(10, 1, -1),
        new Vector(10, 2, 1),
        new Vector(10, 2, -1),
        new Vector(10, 3, 0)
    ));
    private static ArrayList<Vector> FRAME_ACTIVATORS_1 = new ArrayList<>(Arrays.asList(
        new Vector(10, 1, 0)
    ));

    @BeforeEach
    public void setUp()
    {
        server = MockBukkit.mock();
        MockBukkit.load(BuildPortals.class);
        BuildPortals.logLevel = Level.INFO;
        world0 = new BuildPortalsWorldMock();
        world0.setName("world");
        world1 = new BuildPortalsWorldMock();
        world1.setName("world1");
        server.addWorld(world0);
        server.addWorld(world1);
    }

    @AfterEach
    public void tearDown()
    {
        MockBukkit.unmock();
    }

    @Test
    void testBasics() {
        PlayerMock player = server.addPlayer();
        player.addAttachment(BuildPortals.plugin, "buildportals.teleport", true);
        Location startLocation = new Location(world0, 0.5, 1, 0.5);
        Location endLocation = new Location(world1, 10.5, 1, 0.5);
        PortalFrame[] frames = {
            new PortalFrame(world0, FRAME_INTERIOR_0, FRAME_EXTERIOR_0, 0),
            new PortalFrame(world1, FRAME_INTERIOR_1, FRAME_EXTERIOR_1, 0)
        };
        Portal portal = new Portal("1", frames);
        assertTrue(Portal.isInAPortal(startLocation));
        assertTrue(Portal.getPortalFromLocation(startLocation) == portal);
        player.setLocation(startLocation);
        assertNotNull(portal.teleport(player));
        player.assertTeleported(endLocation, 0);
    }

    @Test
    void testBrokenPortal() {
        PlayerMock player = server.addPlayer();
        player.addAttachment(BuildPortals.plugin, "buildportals.teleport", true);
        Location startLocation = new Location(world0, 0.5, 1, 0.5);
        PortalFrame[] frames = {
            new PortalFrame(world0, FRAME_INTERIOR_0, FRAME_EXTERIOR_0, 0),
            new PortalFrame(world1, FRAME_INTERIOR_1, FRAME_EXTERIOR_1, 0)
        };
        Portal portal = new Portal("1", frames);

        // 'Damage' a frame block
        Block brokenBlock = world0.getBlockAt(frames[0].exterior.get(0).toLocation(frames[0].world));
        brokenBlock.setType(Material.AIR);
        player.setLocation(startLocation);
        // Player is in a portal
        assertTrue(Portal.isInAPortal(player.getLocation()));
        // Should destroy the portal
        assertNull(portal.teleport(player));
        player.assertNotTeleported();
        // Player did not move
        assertEquals(startLocation, player.getLocation());
        // And the portal is destroyed, so the player is no longer in a portal
        assertFalse(Portal.isInAPortal(player.getLocation()));
    }

    @Test
    void testIncompletePortalConstruction() {
        PlayerMock player = server.addPlayer();
        player.addAttachment(BuildPortals.plugin, "buildportals.teleport", true);
        Location startLocation = new Location(world0, 0.5, 1, 0.5);
        Location endLocation = new Location(world1, 10.5, 1, 0.5);
        player.setLocation(startLocation);

        ActivatedPortalFrame frame0 = new ActivatedPortalFrame(world0, FRAME_INTERIOR_0, FRAME_EXTERIOR_0, FRAME_ACTIVATORS_0, 0);
        ActivatedPortalFrame frame1 = new ActivatedPortalFrame(world1, FRAME_INTERIOR_1, FRAME_EXTERIOR_1, FRAME_ACTIVATORS_1, 0);

        IncompletePortal incompletePortal0 = new IncompletePortal("0." + Material.REDSTONE_BLOCK.name(), frame0);
        IncompletePortal incompletePortal1 = new IncompletePortal("0." + Material.REDSTONE_BLOCK.name(), frame1);

        Portal portal = new Portal(incompletePortal0, incompletePortal1);
        // Make sure the IncompletePortal was destroyed
        assertFalse(IncompletePortal.isInAPortal(startLocation));
        assertFalse(IncompletePortal.isInAPortal(endLocation));
        // And the Portal was created properly
        assertTrue(Portal.isInAPortal(startLocation));
        assertTrue(Portal.isInAPortal(endLocation));
        assertEquals(portal, Portal.getPortalFromLocation(startLocation));
        // And teleportation works as expected
        assertNotNull(portal.teleport(player));
        player.assertTeleported(endLocation, 0);
    }
}
