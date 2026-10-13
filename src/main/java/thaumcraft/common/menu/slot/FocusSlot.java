package thaumcraft.common.menu.slot;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.items.casters.ItemFocus;

/**
 * FocusSlot - A slot that only accepts focus items.
 * 
 * Used in:
 * - Focal Manipulator for modifying foci
 * - Focus Pouch inventory
 * - Any other container that needs a focus-only slot
 */
public class FocusSlot extends Slot {
    
    private final int limit;
    
    public FocusSlot(Container container, int slot, int x, int y) {
        this(container, slot, x, y, 64);
    }
    
    public FocusSlot(Container container, int slot, int x, int y, int limit) {
        super(container, slot, x, y);
        this.limit = limit;
    }
    
    @Override
    public boolean mayPlace(ItemStack stack) {
        return isValidFocus(stack);
    }
    
    @Override
    public int getMaxStackSize() {
        return limit;
    }
    
    /**
     * Check if the given item stack is a valid focus.
     * 
     * @param stack The item stack to check
     * @return true if the stack contains a focus item
     */
    public static boolean isValidFocus(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof ItemFocus;
    }
}




