package thaumcraft.api.items;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.aura.AuraHelper;

/**
 * Helper class for managing rechargeable item charge levels.
 */
public class RechargeHelper {
    
    private static final String CHARGE_KEY = "tc_charge";
    
    /**
     * Get the current charge level of a rechargeable item.
     */
    public static int getCharge(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IRechargable)) {
            return 0;
        }
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null ? customData.copyTag().getInt(CHARGE_KEY) : 0;
    }
    
    /**
     * Get the current charge as a ratio (0.0 to 1.0).
     */
    public static float getChargeRatio(ItemStack stack, LivingEntity entity) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IRechargable rechargable)) {
            return 0.0f;
        }
        int max = rechargable.getMaxCharge(stack, entity);
        if (max <= 0) return 0.0f;
        return (float) getCharge(stack) / max;
    }
    
    /**
     * Set the charge level of a rechargeable item.
     */
    public static void setCharge(ItemStack stack, int charge, LivingEntity entity) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IRechargable rechargable)) {
            return;
        }
        int max = rechargable.getMaxCharge(stack, entity);
        charge = Math.max(0, Math.min(charge, max));
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        tag.putInt(CHARGE_KEY, charge);
        stack.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
    }
    
    /**
     * Add charge to a rechargeable item.
     * @return The amount actually added
     */
    public static int addCharge(ItemStack stack, int amount, LivingEntity entity) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IRechargable rechargable)) {
            return 0;
        }
        int current = getCharge(stack);
        int max = rechargable.getMaxCharge(stack, entity);
        int newCharge = Math.min(current + amount, max);
        int added = newCharge - current;
        
        if (added > 0) {
            setCharge(stack, newCharge, entity);
        }
        return added;
    }
    
    /**
     * Consume charge from a rechargeable item.
     * @return True if the charge was successfully consumed
     */
    public static boolean consumeCharge(ItemStack stack, LivingEntity entity, int amount) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IRechargable)) {
            return false;
        }
        int current = getCharge(stack);
        if (current < amount) {
            return false;
        }
        setCharge(stack, current - amount, entity);
        return true;
    }
    
    /**
     * Check if the item has enough charge.
     */
    public static boolean hasCharge(ItemStack stack, int amount) {
        return getCharge(stack) >= amount;
    }
    
    /**
     * Fully recharge an item.
     */
    public static void fullRecharge(ItemStack stack, LivingEntity entity) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IRechargable rechargable)) {
            return;
        }
        setCharge(stack, rechargable.getMaxCharge(stack, entity), entity);
    }
    
    /**
     * Recharge an item from the ambient aura.
     * 
     * @param level The world
     * @param stack The item to recharge
     * @param pos Position to drain vis from
     * @param entity The entity holding the item (can be null)
     * @param maxAmount Maximum vis to drain per call
     * @return The amount actually recharged
     */
    public static float rechargeItem(Level level, ItemStack stack, BlockPos pos, LivingEntity entity, int maxAmount) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IRechargable rechargable)) {
            return 0;
        }
        
        int current = getCharge(stack);
        int max = rechargable.getMaxCharge(stack, entity);
        
        if (current >= max) {
            return 0;
        }
        
        int needed = Math.min(maxAmount, max - current);
        
        // Drain vis from aura
        float drained = AuraHelper.drainVis(level, pos, needed / 100.0f, false);
        if (drained <= 0) {
            return 0;
        }
        
        int toAdd = (int)(drained * 100);
        if (toAdd > 0) {
            setCharge(stack, current + toAdd, entity);
        }
        
        return drained;
    }
}




