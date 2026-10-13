package thaumcraft.common.items.casters;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.api.casters.ICaster;
import thaumcraft.api.items.IVisDiscountGear;
import thaumcraft.init.ModEffects;
import thaumcraft.init.ModSounds;
import thaumcraft.common.lib.compat.CuriosCompat;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * CasterManager - Manages caster gauntlet functionality.
 * 
 * Responsibilities:
 * - Calculate vis discounts from armor and curios
 * - Handle focus changing/swapping
 * - Track casting cooldowns
 * - Manage area selection for plan focus
 * 
 * Ported to 1.20.1 with Curios API replacing Baubles.
 */
public class CasterManager {
    
    // Cooldown tracking (entity ID -> cooldown end time)
    private static final Map<Integer, Long> cooldownServer = new HashMap<>();
    private static final Map<Integer, Long> cooldownClient = new HashMap<>();
    
    /**
     * Calculate the total vis discount for a player from all equipment.
     * Checks armor slots and Curios-equipped items.
     * 
     * @param player The player to check
     * @return Discount as a fraction (0.0 to 0.5)
     */
    public static float getTotalVisDiscount(Player player) {
        if (player == null) {
            return 0.0f;
        }
        
        int total = 0;
        
        // Check Curios slots (safely using compat layer)
        total += CuriosCompat.getVisDiscountFromCurios(player);
        
        // Check armor slots (0=feet, 1=legs, 2=chest, 3=head)
        for (int slot = 0; slot < 4; slot++) {
            ItemStack armorStack = player.getInventory().armor.get(slot);
            if (!armorStack.isEmpty() && armorStack.getItem() instanceof IVisDiscountGear gear) {
                total += gear.getVisDiscount(armorStack, player);
            }
        }
        
        // Apply vis exhaustion penalty
        // ModEffects.* are DeferredHolder<MobEffect, MobEffect> (i.e. Holder<MobEffect>),
        // so they can be passed directly to getEffect without re-wrapping.
        MobEffectInstance exhaustEffect = player.getEffect(ModEffects.VIS_EXHAUST);
        MobEffectInstance infectiousEffect = player.getEffect(ModEffects.INFECTIOUS_VIS_EXHAUST);
        
        if (exhaustEffect != null || infectiousEffect != null) {
            int level1 = exhaustEffect != null ? exhaustEffect.getAmplifier() : 0;
            int level2 = infectiousEffect != null ? infectiousEffect.getAmplifier() : 0;
            total -= (Math.max(level1, level2) + 1) * 10;
        }
        
        // Cap at 50% discount
        return Math.min(total / 100.0f, 0.5f);
    }
    
    /**
     * Try to consume vis from casters in the player's inventory.
     * Used when the player needs vis but isn't holding a caster.
     */
    public static boolean consumeVisFromInventory(Player player, float cost) {
        for (int slot = player.getInventory().items.size() - 1; slot >= 0; slot--) {
            ItemStack item = player.getInventory().items.get(slot);
            if (!item.isEmpty() && item.getItem() instanceof ICaster caster) {
                if (caster.consumeVis(item, player, cost, true, false)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Change the focus in a caster, swapping with inventory/pouches.
     * 
     * @param casterStack The caster item stack
     * @param level The world
     * @param player The player
     * @param focusKey The focus key to switch to, or "REMOVE" to remove current focus
     */
    public static void changeFocus(ItemStack casterStack, Level level, Player player, String focusKey) {
        if (!(casterStack.getItem() instanceof ICaster caster)) {
            return;
        }
        
        // Build a map of available foci and their locations
        TreeMap<String, Integer> fociMap = new TreeMap<>();
        Map<Integer, Integer> pouchLocations = new HashMap<>(); // pouchId -> slot
        int pouchCount = 0;
        
        // Note: Focus pouches in Curios slots are not currently supported
        // Players should keep focus pouches in their main inventory for focus swapping
        
        // Check player inventory for foci and focus pouches
        for (int slot = 0; slot < 36; slot++) {
            ItemStack item = player.getInventory().items.get(slot);
            if (item.isEmpty()) continue;
            
            if (item.getItem() instanceof ItemFocus focus) {
                String sortKey = focus.getSortingHelper(item);
                if (sortKey != null) {
                    fociMap.put(sortKey, slot);
                }
            }
            
            if (item.getItem() instanceof ItemFocusPouch pouch) {
                pouchCount++;
                pouchLocations.put(pouchCount, slot);
                
                NonNullList<ItemStack> inv = pouch.getInventory(item);
                for (int q = 0; q < inv.size(); q++) {
                    ItemStack focusStack = inv.get(q);
                    if (!focusStack.isEmpty() && focusStack.getItem() instanceof ItemFocus focus) {
                        String sortKey = focus.getSortingHelper(focusStack);
                        if (sortKey != null) {
                            fociMap.put(sortKey, q + pouchCount * 1000);
                        }
                    }
                }
            }
        }
        
        // Handle REMOVE command or empty focus list
        if ("REMOVE".equals(focusKey) || fociMap.isEmpty()) {
            if (caster.getFocus(casterStack) != null) {
                ItemStack currentFocus = caster.getFocusStack(casterStack);
                if (!currentFocus.isEmpty()) {
                    if (addFocusToPouch(player, currentFocus.copy(), pouchLocations) ||
                        player.getInventory().add(currentFocus.copy())) {
                        caster.setFocus(casterStack, ItemStack.EMPTY);
                        player.playSound(ModSounds.TICKS.get(), 0.3f, 0.9f);
                    }
                }
            }
            return;
        }
        
        // Find the next focus to equip
        if (focusKey != null) {
            String newKey = focusKey;
            if (fociMap.get(newKey) == null) {
                newKey = fociMap.higherKey(newKey);
            }
            if (newKey == null || fociMap.get(newKey) == null) {
                newKey = fociMap.firstKey();
            }
            
            int location = fociMap.get(newKey);
            ItemStack newFocus = ItemStack.EMPTY;
            
            if (location >= 0 && location < 1000) {
                // Focus is in player inventory
                newFocus = player.getInventory().items.get(location).copy();
                player.getInventory().items.set(location, ItemStack.EMPTY);
            } else {
                // Focus is in a pouch
                int pouchId = location / 1000;
                int focusSlot = location - pouchId * 1000;
                
                if (pouchLocations.containsKey(pouchId)) {
                    int pouchSlot = pouchLocations.get(pouchId);
                    newFocus = fetchFocusFromPouch(player, focusSlot, pouchSlot);
                }
            }
            
            if (newFocus.isEmpty()) {
                return;
            }
            
            // Play sound
            player.playSound(ModSounds.TICKS.get(), 0.3f, 1.0f);
            
            // Swap current focus with new one
            ItemStack currentFocus = caster.getFocusStack(casterStack);
            if (!currentFocus.isEmpty()) {
                if (!addFocusToPouch(player, currentFocus.copy(), pouchLocations)) {
                    player.getInventory().add(currentFocus.copy());
                }
            }
            caster.setFocus(casterStack, newFocus);
        }
    }
    
    /**
     * Fetch a focus from a pouch, removing it from the pouch.
     */
    private static ItemStack fetchFocusFromPouch(Player player, int focusSlot, int pouchSlot) {
        // Negative slot numbers were used for Curios slots (no longer supported)
        if (pouchSlot < 0 || pouchSlot >= player.getInventory().items.size()) {
            return ItemStack.EMPTY;
        }
        
        ItemStack pouch = player.getInventory().items.get(pouchSlot);
        
        if (pouch.isEmpty() || !(pouch.getItem() instanceof ItemFocusPouch focusPouch)) {
            return ItemStack.EMPTY;
        }
        
        NonNullList<ItemStack> inv = focusPouch.getInventory(pouch);
        if (focusSlot < 0 || focusSlot >= inv.size()) {
            return ItemStack.EMPTY;
        }
        
        ItemStack focus = inv.get(focusSlot);
        if (focus.isEmpty() || !(focus.getItem() instanceof ItemFocus)) {
            return ItemStack.EMPTY;
        }
        
        ItemStack result = focus.copy();
        inv.set(focusSlot, ItemStack.EMPTY);
        focusPouch.setInventory(pouch, inv);
        
        player.getInventory().items.set(pouchSlot, pouch);
        player.getInventory().setChanged();
        
        return result;
    }
    
    /**
     * Try to add a focus to a pouch.
     */
    private static boolean addFocusToPouch(Player player, ItemStack focus, Map<Integer, Integer> pouchLocations) {
        for (Map.Entry<Integer, Integer> entry : pouchLocations.entrySet()) {
            int pouchSlot = entry.getValue();
            
            // Negative slot numbers were used for Curios slots (no longer supported)
            if (pouchSlot < 0 || pouchSlot >= player.getInventory().items.size()) {
                continue;
            }
            
            ItemStack pouch = player.getInventory().items.get(pouchSlot);
            
            if (pouch.isEmpty() || !(pouch.getItem() instanceof ItemFocusPouch focusPouch)) {
                continue;
            }
            
            NonNullList<ItemStack> inv = focusPouch.getInventory(pouch);
            for (int q = 0; q < inv.size(); q++) {
                if (inv.get(q).isEmpty()) {
                    inv.set(q, focus.copy());
                    focusPouch.setInventory(pouch, inv);
                    
                    player.getInventory().items.set(pouchSlot, pouch);
                    player.getInventory().setChanged();
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Toggle misc settings on a caster (area dimensions for PLAN focus).
     */
    public static void toggleMisc(ItemStack casterStack, Level level, Player player, int modifier) {
        if (!(casterStack.getItem() instanceof ICaster caster)) {
            return;
        }
        
        ItemFocus focus = (ItemFocus) caster.getFocus(casterStack);
        if (focus == null) return;
        
        FocusPackage fp = ItemFocus.getPackage(caster.getFocusStack(casterStack));
        if (fp != null && FocusEngine.doesPackageContainElement(fp, "thaumcraft.PLAN")) {
            int dim = getAreaDim(casterStack);
            
            if (modifier == 0) {
                // Cycle area size
                int areaX = getAreaX(casterStack);
                int areaY = getAreaY(casterStack);
                int areaZ = getAreaZ(casterStack);
                int max = getAreaSize(casterStack);
                
                if (dim == 0) {
                    areaX++; areaY++; areaZ++;
                } else if (dim == 1) {
                    areaX++;
                } else if (dim == 2) {
                    areaZ++;
                } else if (dim == 3) {
                    areaY++;
                }
                
                if (areaX > max) areaX = 0;
                if (areaY > max) areaY = 0;
                if (areaZ > max) areaZ = 0;
                
                setAreaX(casterStack, areaX);
                setAreaY(casterStack, areaY);
                setAreaZ(casterStack, areaZ);
            } else if (modifier == 1) {
                // Cycle dimension mode
                dim++;
                if (dim > 3) dim = 0;
                setAreaDim(casterStack, dim);
            }
        }
    }
    
    // === Area selection for PLAN focus ===
    
    private static int getAreaSize(ItemStack stack) {
        // TODO: Check for power-up trait that increases area size
        return 3;
    }
    
    public static int getAreaDim(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd != null) {
            CompoundTag tag = cd.copyTag();
            if (tag.contains("aread")) {
                return tag.getInt("aread");
            }
        }
        return 0;
    }

    public static int getAreaX(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd != null) {
            CompoundTag tag = cd.copyTag();
            if (tag.contains("areax")) {
                return Math.min(tag.getInt("areax"), getAreaSize(stack));
            }
        }
        return getAreaSize(stack);
    }

    public static int getAreaY(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd != null) {
            CompoundTag tag = cd.copyTag();
            if (tag.contains("areay")) {
                return Math.min(tag.getInt("areay"), getAreaSize(stack));
            }
        }
        return getAreaSize(stack);
    }

    public static int getAreaZ(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd != null) {
            CompoundTag tag = cd.copyTag();
            if (tag.contains("areaz")) {
                return Math.min(tag.getInt("areaz"), getAreaSize(stack));
            }
        }
        return getAreaSize(stack);
    }

    public static void setAreaX(ItemStack stack, int area) {
        stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, c -> {
            CompoundTag t = c.copyTag();
            t.putInt("areax", area);
            return CustomData.of(t);
        });
    }

    public static void setAreaY(ItemStack stack, int area) {
        stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, c -> {
            CompoundTag t = c.copyTag();
            t.putInt("areay", area);
            return CustomData.of(t);
        });
    }

    public static void setAreaZ(ItemStack stack, int area) {
        stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, c -> {
            CompoundTag t = c.copyTag();
            t.putInt("areaz", area);
            return CustomData.of(t);
        });
    }

    public static void setAreaDim(ItemStack stack, int dim) {
        stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, c -> {
            CompoundTag t = c.copyTag();
            t.putInt("aread", dim);
            return CustomData.of(t);
        });
    }
    
    // === Cooldown tracking ===
    
    /**
     * Check if an entity is on casting cooldown.
     */
    public static boolean isOnCooldown(LivingEntity entity) {
        Map<Integer, Long> cooldowns = entity.level().isClientSide() ? cooldownClient : cooldownServer;
        Long endTime = cooldowns.get(entity.getId());
        return endTime != null && endTime > System.currentTimeMillis();
    }
    
    /**
     * Get the remaining cooldown time in seconds.
     */
    public static float getCooldown(LivingEntity entity) {
        if (entity.level().isClientSide()) {
            Long endTime = cooldownClient.get(entity.getId());
            if (endTime != null) {
                return Math.max(0, (endTime - System.currentTimeMillis()) / 1000.0f);
            }
        }
        return 0.0f;
    }
    
    /**
     * Set a cooldown for an entity.
     * @param entity The entity
     * @param cooldownTicks Cooldown duration in ticks (0 to clear)
     */
    public static void setCooldown(LivingEntity entity, int cooldownTicks) {
        if (cooldownTicks == 0) {
            cooldownClient.remove(entity.getId());
            cooldownServer.remove(entity.getId());
        } else {
            long endTime = System.currentTimeMillis() + cooldownTicks * 50L;
            if (entity.level().isClientSide()) {
                cooldownClient.put(entity.getId(), endTime);
            } else {
                cooldownServer.put(entity.getId(), endTime);
            }
        }
    }
}




