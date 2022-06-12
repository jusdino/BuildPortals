import java.util.logging.Level;

import org.bukkit.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import space.frahm.test.bukkit.BuildPortalsWorldMock;
import space.frahm.buildportals.BuildPortals;
import space.frahm.buildportals.Teleporter;

public class TestTeleporter {
    private ServerMock server;
    private WorldMock world0;
    private WorldMock world1;

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

    /*
     * Most of the Entity classes we would want to test are not implemented yet in MockBukkit
     * so we'll have to stick with Player
     */
    @Test
    void testBasics() {
        PlayerMock player = server.addPlayer();
        Location startLocation = new Location(world0, 0.5, 1, 0.5);
        Location endLocation = new Location(world1, 10.5, 1, 0.5);
        player.addAttachment(BuildPortals.plugin, "buildportals.teleport", true);
        player.setLocation(startLocation);
        Teleporter.teleport(player, endLocation);
        player.assertTeleported(endLocation, 0);
    }
}
