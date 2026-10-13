package thaumcraft.common.menu.slot;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.Potions;

/**
 * PotionSlot - A slot that only accepts valid potions.
 * 
 * Used in the Potion Sprayer to accept potions that can be dispersed.
 * Filters out water, mundane, thick, and awkward potions.
 */
public class PotionSlot extends Slot {
    
    private final int limit;
    
    public PotionSlot(Container container, int slot, int x, int y) {
        this(container, slot, x, y, 64);
    }
    
    public PotionSlot(Container container, int slot, int x, int y, int limit) {
        super(container, slot, x, y);
        this.limit = limit;
    }
    
    @Override
    public boolean mayPlace(ItemStack stack) {
        return isValidPotion(stack);
    }
    
    @Override
    public int getMaxStackSize() {
        return limit;
    }
    
    /**
     * Check if the given item stack is a valid potion for the sprayer.
     * Accepts regular potions, splash potions, and lingering potions.
     * Rejects water, mundane, thick, and awkward potions.
     * 
     * @param stack The item stack to check
     * @return true if the stack is a valid potion
     */
    public static boolean isValidPotion(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        
        // Check if it's a potion item
        if (stack.getItem() != Items.POTION && 
            stack.getItem() != Items.SPLASH_POTION && 
            stack.getItem() != Items.LINGERING_POTION) {
            return false;
        }
        
        // Check if it has actual effects
        try {
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents == null || contents.potion().isEmpty()) {
                return false;
            }
            var potion = contents.potion().get();
            return potion != Potions.WATER && 
                   potion != Potions.MUNDANE && 
                   potion != Potions.THICK && 
                   potion != Potions.AWKWARD;
        } catch (Exception e) {
            return false;
        }
    }
}




