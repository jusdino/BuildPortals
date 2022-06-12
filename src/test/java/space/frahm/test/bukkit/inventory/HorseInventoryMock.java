package space.frahm.test.bukkit.inventory;

import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.HorseInventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import be.seeseemelk.mockbukkit.UnimplementedOperationException;
import be.seeseemelk.mockbukkit.inventory.InventoryMock;

public class HorseInventoryMock extends InventoryMock implements HorseInventory {
    public HorseInventoryMock(InventoryHolder holder) {
        // TODO: verify the appropriate InventoryType for a HorseInventory somehow
        super(holder, 2, InventoryType.CHEST);
    }

    @Override
    public ItemStack getSaddle() {
        return this.getItem(0);
    }

    @Override
    public void setSaddle(ItemStack stack) {
        this.setItem(0, stack);
    }

    @Override
    public ItemStack getArmor() {
        return this.getItem(1);
    }

    @Override
    public void setArmor(ItemStack stack) {
        this.setItem(1, stack);
    }
}
