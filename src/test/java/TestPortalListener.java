import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import space.frahm.test.bukkit.BuildPortalsWorldMock;
import space.frahm.buildportals.BuildPortals;
import space.frahm.buildportals.Portal;
import space.frahm.buildportals.PortalFrame;

public class TestPortalListener {
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

    @BeforeEach
    public void setUp()
    {
        server = MockBukkit.mock();
        Permission permission = new Permission("buildportals.teleport", PermissionDefault.TRUE);
        server.getPluginManager().addPermission(permission);
        MockBukkit.load(BuildPortals.class);
        world0 = new BuildPortalsWorldMock();
        world0.setName("world");
        world1 = new BuildPortalsWorldMock();
        world1.setName("world1");
        server.addWorld(world0);
        server.addWorld(world1);
        PortalFrame[] frames = {
            new PortalFrame(world0, FRAME_INTERIOR_0, FRAME_EXTERIOR_0, 0),
            new PortalFrame(world1, FRAME_INTERIOR_1, FRAME_EXTERIOR_1, 0)
        };
        new Portal("1", frames);
    }

    @AfterEach
    public void tearDown()
    {
        MockBukkit.unmock();
    }

    /* It seems this gets skipped for UnimplementationOperationException at the time
     * of writing, which causes this test to be skipped. Moving on to lower hanging fruit.
     */
    @Test
    void testOnPlayerMove() {
        PlayerMock player = server.addPlayer();

        Location startLocation = new Location(world0, 0.5, 1, 0.5);
        Location endLocation = new Location(world1, 10.5, 1, 0.5);
        player.setLocation(startLocation);
        PlayerMoveEvent event = new PlayerMoveEvent(player, startLocation, startLocation);
        try {
            /* be.seeseemelk.mockbukkit.UnimplementedOperationException: Not implemented
                at be.seeseemelk.mockbukkit.entity.EntityMock.getVehicle(EntityMock.java:743)
                at space.frahm.buildportals.PortalListener.onPlayerMove(PortalListener.java:40)
             */
            BuildPortals.logLevel = Level.INFO;
            BuildPortals.logger.info("testOnPlayerMove");
            BuildPortals.listener.onPlayerMove(event); // UnimplementationOperationException thrown here
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        // Teleportation is a scheduled task, so we need to cycle the scheduler
        server.getScheduler().performTicks(2L);
        player.assertTeleported(endLocation, 0);
    }
}
