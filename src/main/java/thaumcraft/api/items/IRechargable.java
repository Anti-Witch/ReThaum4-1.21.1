package thaumcraft.api.items;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * @author Azanor
 * 
 * Items with this interface can be recharged in wand pedestals and similar devices.
 * All values are automatically stored in the items NBT data.
 * 
 * See RechargeHelper for methods to handle actual recharging of the item.
 */
public interface IRechargable {
    
    /**
     * @param stack The item stack
     * @param entity The entity holding the item (may be null, check first)
     * @return How much vis charge this item can hold
     */
    int getMaxCharge(ItemStack stack, LivingEntity entity);
    
    /**
     * @param stack The item stack
     * @param entity The entity holding the item
     * @return When the charge will be displayed in the built-in HUD
     */
    EnumChargeDisplay showInHud(ItemStack stack, LivingEntity entity);
    
    /**
     * Controls when charge is displayed in the HUD.
     */
    enum EnumChargeDisplay {
        /** Never show charge */
        NEVER,
        /** Show whenever charge changes */
        NORMAL,
        /** Show at 0%, 25%, 50%, 75%, or 100% thresholds */
        PERIODIC
    }
}




