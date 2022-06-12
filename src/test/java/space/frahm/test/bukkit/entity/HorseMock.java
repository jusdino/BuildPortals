package space.frahm.test.bukkit.entity;

import java.util.UUID;

import org.bukkit.entity.Horse;
import org.bukkit.inventory.HorseInventory;

import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.UnimplementedOperationException;
import space.frahm.test.bukkit.inventory.HorseInventoryMock;

public class HorseMock extends AbstractHorseMock implements Horse {
    private Color color;
    private Style style;
    private HorseInventory inventory;

    public HorseMock(ServerMock server, UUID uuid) {
        super(server, uuid);
        inventory = new HorseInventoryMock(this);
    }

    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public void setColor(Color color) {
        this.color = color;
    }

    @Override
    public Style getStyle() {
        return style;
    }

    @Override
    public void setStyle(Style style) {
        this.style = style;
    }

    @Override
    public boolean isCarryingChest() {
        throw new UnimplementedOperationException();
    }

    @Override
    public void setCarryingChest(boolean chest) {
        throw new UnimplementedOperationException();
    }

    @Override
    public HorseInventory getInventory() {
        return inventory;
    }
}
