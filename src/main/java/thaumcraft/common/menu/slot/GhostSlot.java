package thaumcraft.common.menu.slot;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * GhostSlot - A filter slot that doesn't actually hold items.
 * 
 * Used for:
 * - Item filters in seal configuration
 * - Recipe inputs in autocrafting
 * - Any "template" slot where you want to specify an item type
 * 
 * Items are copied in (not moved) and cannot be taken out normally.
 * The displayed item is just a visual representation.
 */
public class GhostSlot extends Slot {
    
    private final int maxStackSize;
    
    public GhostSlot(Container container, int slot, int x, int y) {
        this(container, slot, x, y, Integer.MAX_VALUE);
    }
    
    public GhostSlot(Container container, int slot, int x, int y, int maxStackSize) {
        super(container, slot, x, y);
        this.maxStackSize = maxStackSize;
    }
    
    @Override
    public int getMaxStackSize() {
        return maxStackSize;
    }
    
    @Override
    public boolean mayPickup(Player player) {
        // Can't take items out of ghost slots normally
        return false;
    }
    
    @Override
    public boolean mayPlace(ItemStack stack) {
        // Allow placing any item as a filter
        return true;
    }
    
    /**
     * Set the ghost item directly (for network sync/initialization)
     */
    public void setGhostItem(ItemStack stack) {
        set(stack.copy());
    }
    
    /**
     * Clear the ghost slot
     */
    public void clearGhost() {
        set(ItemStack.EMPTY);
    }
}




