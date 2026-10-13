package thaumcraft.api.items;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * @author Azanor
 * 
 * Equipped or held items that extend this class will be able to perform most functions that 
 * goggles of revealing can.
 */
public interface IGoggles {
    
    /**
     * If this method returns true things like block essentia contents will be shown.
     */
    boolean showIngamePopups(ItemStack itemstack, LivingEntity player);
}




