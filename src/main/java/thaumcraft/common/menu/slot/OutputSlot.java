package thaumcraft.common.menu.slot;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * OutputSlot - A slot that only allows taking items, not placing.
 * 
 * Used for:
 * - Crafting output slots
 * - Machine output slots
 * - Any slot where items are produced, not input
 */
public class OutputSlot extends Slot {
    
    public OutputSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }
    
    @Override
    public boolean mayPlace(ItemStack stack) {
        // Cannot place items in output slots
        return false;
    }
}




