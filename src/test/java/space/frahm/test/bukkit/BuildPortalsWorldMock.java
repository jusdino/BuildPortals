package space.frahm.test.bukkit;

import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.Nonnull;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LightningStrike;

import be.seeseemelk.mockbukkit.WorldMock;

import space.frahm.test.bukkit.entity.LightningStrikeMock;

/*
 * Extends WorldMock to provide a dummy value for unimplemented methods that
 * we need.
 */
public class BuildPortalsWorldMock extends WorldMock {
    public Collection<Entity> getNearbyEntities(Location location, double x, double y, double z) {
        return new ArrayList<Entity>();
    }

    public LightningStrike strikeLightningEffect(@Nonnull Location location) {
        return new LightningStrikeMock();
    }

    public void spawnParticle(Particle particle, Location location, int count) {
        return;
    }
}
