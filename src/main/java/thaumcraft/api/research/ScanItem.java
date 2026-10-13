package thaumcraft.api.research;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A scan thing that checks for specific items.
 */
public class ScanItem implements IScanThing {
    
    private final String research;
    private final ItemStack stack;
    
    /**
     * Create a new item scanner.
     * @param research The research key to unlock
     * @param stack The item stack to match (ignores count)
     */
    public ScanItem(String research, ItemStack stack) {
        this.research = research;
        this.stack = stack;
    }
    
    @Override
    public boolean checkThing(Player player, Object obj) {
        if (obj == null) return false;
        
        ItemStack target = ItemStack.EMPTY;
        
        if (obj instanceof ItemStack is) {
            target = is;
        } else if (obj instanceof ItemEntity itemEntity && !itemEntity.getItem().isEmpty()) {
            target = itemEntity.getItem();
        }
        
        if (target.isEmpty()) return false;
        
        // Compare item types (ignore count, optionally compare NBT)
        return ItemStack.isSameItem(target, stack);
    }
    
    @Override
    public String getResearchKey(Player player, Object object) {
        return research;
    }
}




