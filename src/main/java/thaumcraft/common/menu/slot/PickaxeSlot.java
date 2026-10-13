package thaumcraft.common.menu.slot;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;

/**
 * PickaxeSlot - A mob equipment slot that only accepts pickaxes.
 * 
 * Used for the Arcane Bore to hold its mining tool.
 */
public class PickaxeSlot extends MobEquipmentSlot {
    
    public PickaxeSlot(Mob entity, int slotIndex, int x, int y) {
        super(entity, slotIndex, x, y);
    }
    
    @Override
    public boolean mayPlace(ItemStack stack) {
        return isValidPickaxe(stack);
    }
    
    /**
     * Check if the given item is a valid pickaxe.
     * 
     * @param stack The item stack to check
     * @return true if the stack is a pickaxe item
     */
    public static boolean isValidPickaxe(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        
        // Check if it's a PickaxeItem (vanilla or modded that extends it)
        if (stack.getItem() instanceof PickaxeItem) {
            return true;
        }
        
        // Also check the pickaxes tag for items that might not extend PickaxeItem
        if (stack.is(ItemTags.PICKAXES)) {
            return true;
        }
        
        // Check if the item has "pickaxe" in its tool classes
        // This catches modded pickaxes that use the tool system
        return stack.isCorrectToolForDrops(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
    }
}




