package thaumcraft.common.menu.slot;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;

/**
 * AspectSlot - A slot that only accepts items that have aspects.
 * 
 * Used for:
 * - Smelter input (items to break down into essentia)
 * - Any other device that processes items for their aspect content
 */
public class AspectSlot extends Slot {
    
    public AspectSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }
    
    @Override
    public boolean mayPlace(ItemStack stack) {
        return hasAspects(stack);
    }
    
    /**
     * Check if an item has aspects associated with it.
     * 
     * @param stack The item to check
     * @return true if the item has aspects
     */
    public static boolean hasAspects(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        
        // Check if it's an essentia container (phials, crystals, etc.)
        if (stack.getItem() instanceof IEssentiaContainerItem container) {
            var aspects = container.getAspects(stack);
            return aspects != null && aspects.size() > 0;
        }
        
        // Check the aspect registry for this item
        var aspects = ThaumcraftCraftingManager.getObjectTags(stack);
        return aspects != null && aspects.size() > 0;
    }
}




