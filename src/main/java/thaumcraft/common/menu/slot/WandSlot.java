package thaumcraft.common.menu.slot;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.items.wands.WandItem;

public class WandSlot extends Slot {

    public WandSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.getItem() instanceof WandItem;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}
