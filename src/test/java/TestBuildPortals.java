import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;

import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import be.seeseemelk.mockbukkit.MockBukkit;
import space.frahm.buildportals.BuildPortals;

class TestBuildPortals {
    @BeforeEach
    public void setUp()
    {
        MockBukkit.mock();
        MockBukkit.load(BuildPortals.class);
    }

    @AfterEach
    public void tearDown()
    {
        MockBukkit.unmock();
    }

    @Test
    void testDefaultConfig() {
        Material defaultPortalMaterial = Material.getMaterial(BuildPortals.config.getString("PortalMaterial"));
        assertEquals(Material.EMERALD_BLOCK, defaultPortalMaterial);

        ArrayList<String> expectedActivatorMaterialNames = new ArrayList<String>(Arrays.asList(
            Material.REDSTONE_BLOCK.name(),
            Material.GOLD_BLOCK.name(),
            Material.DIAMOND_BLOCK.name()
        ));
        assertEquals(expectedActivatorMaterialNames, BuildPortals.config.getStringList("PortalActivators"));

        assertEquals(false, BuildPortals.config.getBoolean("Debug"));
    }
}
