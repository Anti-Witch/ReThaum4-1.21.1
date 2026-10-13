package thaumcraft.common.menu.slot;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * LimitedByClassSlot - A slot that only accepts items of a specific class.
 * 
 * Used for slots that should only hold certain types of items, like focus slots
 * in the focus pouch (ItemFocus.class), or wand slots (ItemCaster.class).
 */
public class LimitedByClassSlot extends Slot {
    
    private final Class<?> itemClass;
    private final int limit;
    
    public LimitedByClassSlot(Class<?> itemClass, Container container, int slot, int x, int y) {
        this(itemClass, container, slot, x, y, 64);
    }
    
    public LimitedByClassSlot(Class<?> itemClass, Container container, int slot, int x, int y, int limit) {
        super(container, slot, x, y);
        this.itemClass = itemClass;
        this.limit = limit;
    }
    
    @Override
    public boolean mayPlace(ItemStack stack) {
        return !stack.isEmpty() && itemClass.isAssignableFrom(stack.getItem().getClass());
    }
    
    @Override
    public int getMaxStackSize() {
        return limit;
    }
}




