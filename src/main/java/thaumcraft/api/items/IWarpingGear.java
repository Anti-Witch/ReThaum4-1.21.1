package thaumcraft.api.items;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * @author Azanor
 * 
 * Armor, held items or bauble slot items that implement this interface add warp when equipped or held.
 * 
 * IMPORTANT: 
 * You can also add a "TC.WARP" nbt int tag to an item which serves much the same function 
 * without having to implement this interface. Warp from this interface and the nbt tag stacks.
 */
public interface IWarpingGear {
    
    /**
     * Returns how much warp this item adds while worn or held.
     */
    int getWarp(ItemStack itemstack, Player player);
}




