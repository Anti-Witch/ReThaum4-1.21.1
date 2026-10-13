package thaumcraft.api.items;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * @author Azanor
 * 
 * Equipped or held items that extend this class will make nodes or related objects visible in world.
 * 
 * @deprecated
 * Currently nodes do not exist ingame and in future versions this might be removed 
 * and IGoggles (or a capability) will take its place.
 */
@Deprecated
public interface IRevealer {
    
    /**
     * If this method returns true the nodes will be visible.
     */
    boolean showNodes(ItemStack itemstack, LivingEntity player);
}




