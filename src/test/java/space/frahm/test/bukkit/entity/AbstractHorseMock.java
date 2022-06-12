package space.frahm.test.bukkit.entity;

import java.util.UUID;

import javax.annotation.Nonnull;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Horse.Variant;
import org.bukkit.entity.SpawnCategory;
import org.bukkit.inventory.ItemStack;

import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.UnimplementedOperationException;
import be.seeseemelk.mockbukkit.entity.CreatureMock;

public abstract class AbstractHorseMock extends CreatureMock implements AbstractHorse {
    private boolean isTamed;
    private AnimalTamer owner;
    private int loveModeTicks;
    private boolean canBreed;
    private int age;

    protected AbstractHorseMock(ServerMock server, UUID uuid) {
        super(server, uuid);
        isTamed = false;
        loveModeTicks = 0;
        canBreed = false;
        age = 0;
    }

    @Override
    public @Nonnull SpawnCategory getSpawnCategory() {
        return SpawnCategory.ANIMAL;
    }

    @Override
    public boolean isTamed() {
        return isTamed;
    }

    @Override
    public void setTamed(boolean tame) {
        isTamed = tame;
    }

    @Override
    public AnimalTamer getOwner() {
        return owner;
    }

    @Override
    public void setOwner(AnimalTamer tamer) {
        owner = tamer;
    }

    @Override
    public UUID getBreedCause() {
        throw new UnimplementedOperationException();
    }

    @Override
    public void setBreedCause(UUID uuid) {
        throw new UnimplementedOperationException();
    }

    @Override
    public boolean isLoveMode() {
        return loveModeTicks > 0;
    }

    @Override
    public int getLoveModeTicks() {
        return loveModeTicks;
    }

    @Override
    public void setLoveModeTicks(int ticks) {
        loveModeTicks = ticks;
    }

    @Override
    public boolean isBreedItem(ItemStack stack) {
        throw new UnimplementedOperationException();
    }

    @Override
    public boolean isBreedItem(Material material) {
        throw new UnimplementedOperationException();
    }

    @Override
    public void setAgeLock(boolean lock) {
        throw new UnimplementedOperationException();
    }

    @Override
    public boolean getAgeLock() {
        throw new UnimplementedOperationException();
    }

    @Override
    public boolean canBreed() {
        return canBreed;
    }

    @Override
    public void setBreed(boolean breed) {
        canBreed = breed;
    }

    @Override
    public int getAge() {
        return age;
    }

    @Override
    public void setAge(int age) {
        this.age = age;
    }

    @Override
    public void setBaby() {
        throw new UnimplementedOperationException();
    }

    @Override
    public void setAdult() {
        throw new UnimplementedOperationException();
    }

    @Override
    public boolean isAdult() {
        throw new UnimplementedOperationException();
    }

    @Deprecated
    @Override
    public Variant getVariant() {
        throw new UnimplementedOperationException();
    }

    @Deprecated
    @Override
    public void setVariant(Variant variant) {
        throw new UnimplementedOperationException();
    }

    @Override
    public int getDomestication() {
        throw new UnimplementedOperationException();
    }

    @Override
    public void setDomestication(int level) {
        throw new UnimplementedOperationException();
    }

    @Override
    public int getMaxDomestication() {
        throw new UnimplementedOperationException();
    }

    @Override
    public void setMaxDomestication(int level) {
        throw new UnimplementedOperationException();
    }

    @Override
    public double getJumpStrength() {
        return getAttribute(Attribute.HORSE_JUMP_STRENGTH).getValue();
    }

    @Override
    public void setJumpStrength(double strength) {
        getAttribute(Attribute.HORSE_JUMP_STRENGTH).setBaseValue(strength);
    }

    @Override
    public boolean isEatingHaystack() {
        throw new UnimplementedOperationException();
    }

    @Override
    public void setEatingHaystack(boolean eatingHaystack) {
        throw new UnimplementedOperationException();
    }
}
