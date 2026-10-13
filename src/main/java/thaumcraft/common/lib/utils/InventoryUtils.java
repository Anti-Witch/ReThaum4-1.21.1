package thaumcraft.common.lib.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import thaumcraft.api.ThaumcraftInvHelper;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Utility class for inventory manipulation.
 * Ported to 1.20.1 from 1.12.2.
 * 
 * Key changes:
 * - OreDictionary replaced with Tags
 * - EnumFacing -> Direction
 * - EntityEquipmentSlot -> EquipmentSlot
 * - EntityItem -> ItemEntity
 * - EntityPlayer -> Player
 * - IInventory -> Container
 */
@SuppressWarnings("deprecation")
public class InventoryUtils {
    
    /**
     * Copy a stack with its max stack size.
     */
    public static ItemStack copyMaxedStack(ItemStack stack) {
        return copyLimitedStack(stack, stack.getMaxStackSize());
    }
    
    /**
     * Copy a stack with a limited count.
     */
    public static ItemStack copyLimitedStack(ItemStack stack, int limit) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack s = stack.copy();
        if (s.getCount() > limit) {
            s.setCount(limit);
        }
        return s;
    }
    
    /**
     * Consume items from adjacent inventories or the player's inventory.
     */
    public static boolean consumeItemsFromAdjacentInventoryOrPlayer(Level level, BlockPos pos, Player player, 
            boolean simulate, ItemStack... items) {
        // First check if all items are available
        for (ItemStack stack : items) {
            boolean found = checkAdjacentChests(level, pos, stack);
            if (!found) {
                found = isPlayerCarryingAmount(player, stack, true);
            }
            if (!found) {
                return false;
            }
        }
        
        // If not simulating, actually consume the items
        if (!simulate) {
            for (ItemStack stack : items) {
                if (!consumeFromAdjacentChests(level, pos, stack.copy())) {
                    consumePlayerItem(player, stack, true, true);
                }
            }
        }
        return true;
    }
    
    /**
     * Check if adjacent chests contain the specified item stack.
     */
    public static boolean checkAdjacentChests(Level level, BlockPos pos, ItemStack itemStack) {
        int needed = itemStack.getCount();
        for (Direction face : Direction.values()) {
            if (face != Direction.UP) {
                needed -= ThaumcraftInvHelper.countTotalItemsIn(level, pos.relative(face), 
                        face.getOpposite(), itemStack.copy(), ThaumcraftInvHelper.InvFilter.BASEORE);
                if (needed <= 0) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Consume items from adjacent chests.
     */
    public static boolean consumeFromAdjacentChests(Level level, BlockPos pos, ItemStack itemStack) {
        for (Direction face : Direction.values()) {
            if (face != Direction.UP && !itemStack.isEmpty()) {
                ItemStack removed = removeStackFrom(level, pos.relative(face), face.getOpposite(), 
                        itemStack, ThaumcraftInvHelper.InvFilter.BASEORE, false);
                itemStack.shrink(removed.getCount());
                if (itemStack.isEmpty()) {
                    break;
                }
            }
        }
        return itemStack.isEmpty();
    }
    
    /**
     * Eject a stack from a position in a direction.
     */
    public static void ejectStackAt(Level level, BlockPos pos, Direction side, ItemStack out) {
        ejectStackAt(level, pos, side, out, false);
    }
    
    /**
     * Eject a stack with optional smart insertion to adjacent inventories.
     */
    public static ItemStack ejectStackAt(Level level, BlockPos pos, Direction side, ItemStack out, boolean smart) {
        // Try to insert into adjacent inventory first
        out = ThaumcraftInvHelper.insertStackAt(level, pos.relative(side), side.getOpposite(), out, false);
        
        if (smart && ThaumcraftInvHelper.getItemHandlerAt(level, pos.relative(side), side.getOpposite()) != null) {
            return out;
        }
        
        // If there's still items left, spawn as entity
        if (!out.isEmpty()) {
            BlockPos spawnPos = pos;
            if (level.getBlockState(pos.relative(side)).isCollisionShapeFullBlock(level, pos.relative(side))) {
                spawnPos = pos.relative(side.getOpposite());
            }
            
            double x = spawnPos.getX() + 0.5 + side.getStepX();
            double y = spawnPos.getY() + side.getStepY();
            double z = spawnPos.getZ() + 0.5 + side.getStepZ();
            
            ItemEntity entityItem = new ItemEntity(level, x, y, z, out);
            entityItem.setDeltaMovement(
                    0.3 * side.getStepX(),
                    0.3 * side.getStepY(),
                    0.3 * side.getStepZ()
            );
            level.addFreshEntity(entityItem);
        }
        return ItemStack.EMPTY;
    }
    
    /**
     * Remove a stack from an inventory at a position.
     */
    public static ItemStack removeStackFrom(Level level, BlockPos pos, Direction side, ItemStack stack, 
            ThaumcraftInvHelper.InvFilter filter, boolean simulate) {
        return removeStackFrom(ThaumcraftInvHelper.getItemHandlerAt(level, pos, side), stack, filter, simulate);
    }
    
    /**
     * Remove a stack from an IItemHandler.
     */
    public static ItemStack removeStackFrom(@Nullable IItemHandler inventory, ItemStack stack, 
            ThaumcraftInvHelper.InvFilter filter, boolean simulate) {
        int amount = stack.getCount();
        int removed = 0;
        
        if (inventory != null) {
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                ItemStack slotStack = inventory.getStackInSlot(slot);
                if (areItemStacksEqual(stack, slotStack, filter)) {
                    int toExtract = Math.min(amount - removed, slotStack.getCount());
                    ItemStack extracted = inventory.extractItem(slot, toExtract, simulate);
                    if (!extracted.isEmpty()) {
                        removed += extracted.getCount();
                    }
                }
                if (removed >= amount) {
                    break;
                }
            }
        }
        
        if (removed == 0) {
            return ItemStack.EMPTY;
        }
        
        ItemStack result = stack.copy();
        result.setCount(removed);
        return result;
    }
    
    /**
     * Count item entities in range matching a stack.
     */
    public static int countStackInWorld(Level level, BlockPos pos, ItemStack stack, double range, 
            ThaumcraftInvHelper.InvFilter filter) {
        int count = 0;
        List<ItemEntity> entities = EntityUtils.getEntitiesInRange(level, pos, range, ItemEntity.class);
        for (ItemEntity ei : entities) {
            ItemStack itemStack = ei.getItem();
            if (!itemStack.isEmpty() && areItemStacksEqual(stack, itemStack, filter)) {
                count += itemStack.getCount();
            }
        }
        return count;
    }
    
    /**
     * Drop all items from a container at a position.
     */
    public static void dropItems(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof Container container) {
            Containers.dropContents(level, pos, container);
        }
    }
    
    /**
     * Consume an item from the player's inventory.
     */
    public static boolean consumePlayerItem(Player player, ItemStack item, boolean nocheck, boolean useTagMatch) {
        if (!nocheck && !isPlayerCarryingAmount(player, item, useTagMatch)) {
            return false;
        }
        
        int count = item.getCount();
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            ItemStack slotStack = player.getInventory().items.get(slot);
            ThaumcraftInvHelper.InvFilter filter = new ThaumcraftInvHelper.InvFilter(false, !NBTHelper.hasTag(item), useTagMatch, false)
                    .setRelaxedNBT();
            
            if (areItemStacksEqual(slotStack, item, filter)) {
                if (slotStack.getCount() > count) {
                    slotStack.shrink(count);
                    count = 0;
                } else {
                    count -= slotStack.getCount();
                    player.getInventory().items.set(slot, ItemStack.EMPTY);
                }
                if (count <= 0) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Consume a specific item from the player's inventory by item and count.
     */
    public static boolean consumePlayerItem(Player player, Item item, int amount) {
        if (!isPlayerCarryingAmount(player, new ItemStack(item, amount), false)) {
            return false;
        }
        
        int remaining = amount;
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            ItemStack slotStack = player.getInventory().items.get(slot);
            if (slotStack.is(item)) {
                if (slotStack.getCount() > remaining) {
                    slotStack.shrink(remaining);
                    remaining = 0;
                } else {
                    remaining -= slotStack.getCount();
                    player.getInventory().items.set(slot, ItemStack.EMPTY);
                }
                if (remaining <= 0) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Check if the player is carrying enough of an item.
     */
    public static boolean isPlayerCarryingAmount(Player player, ItemStack stack, boolean useTagMatch) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        
        int needed = stack.getCount();
        ThaumcraftInvHelper.InvFilter filter = new ThaumcraftInvHelper.InvFilter(false, !NBTHelper.hasTag(stack), useTagMatch, false)
                .setRelaxedNBT();
        
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            ItemStack slotStack = player.getInventory().items.get(slot);
            if (areItemStacksEqual(slotStack, stack, filter)) {
                needed -= slotStack.getCount();
                if (needed <= 0) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Check which hand the player is holding an item in.
     */
    @Nullable
    public static EquipmentSlot isHoldingItem(Player player, Item item) {
        if (player == null || item == null) {
            return null;
        }
        if (!player.getMainHandItem().isEmpty() && player.getMainHandItem().is(item)) {
            return EquipmentSlot.MAINHAND;
        }
        if (!player.getOffhandItem().isEmpty() && player.getOffhandItem().is(item)) {
            return EquipmentSlot.OFFHAND;
        }
        return null;
    }
    
    /**
     * Check which hand the player is holding an item of a specific class.
     */
    @Nullable
    public static EquipmentSlot isHoldingItem(Player player, Class<?> itemClass) {
        if (player == null || itemClass == null) {
            return null;
        }
        if (!player.getMainHandItem().isEmpty() && itemClass.isAssignableFrom(player.getMainHandItem().getItem().getClass())) {
            return EquipmentSlot.MAINHAND;
        }
        if (!player.getOffhandItem().isEmpty() && itemClass.isAssignableFrom(player.getOffhandItem().getItem().getClass())) {
            return EquipmentSlot.OFFHAND;
        }
        return null;
    }
    
    /**
     * Get the player inventory slot containing a matching item.
     */
    public static int getPlayerSlotFor(Player player, ItemStack stack) {
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack slotStack = player.getInventory().items.get(i);
            if (!slotStack.isEmpty() && ItemStack.isSameItemSameComponents(stack, slotStack)) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Check if two stacks are exactly equal (item, NBT).
     */
    public static boolean stackEqualExact(ItemStack stack1, ItemStack stack2) {
        return ItemStack.isSameItemSameComponents(stack1, stack2);
    }
    
    /**
     * Strict item stack comparison.
     */
    public static boolean areItemStacksEqualStrict(ItemStack stack0, ItemStack stack1) {
        return areItemStacksEqual(stack0, stack1, ThaumcraftInvHelper.InvFilter.STRICT);
    }
    
    /**
     * Compare two item stacks with filter options.
     * 
     * @param stack0 First stack
     * @param stack1 Second stack
     * @param filter Comparison filter options
     * @return true if stacks match according to filter
     */
    public static boolean areItemStacksEqual(ItemStack stack0, ItemStack stack1, ThaumcraftInvHelper.InvFilter filter) {
        // Handle null/empty cases
        if (stack0 == null && stack1 != null) return false;
        if (stack0 != null && stack1 == null) return false;
        if (stack0 == null) return true; // Both null
        if (stack0.isEmpty() && !stack1.isEmpty()) return false;
        if (!stack0.isEmpty() && stack1.isEmpty()) return false;
        if (stack0.isEmpty()) return true; // Both empty
        
        // Mod-based matching (compare namespaces)
        if (filter.useMod) {
            String mod0 = stack0.getItem().builtInRegistryHolder().key().location().getNamespace();
            String mod1 = stack1.getItem().builtInRegistryHolder().key().location().getNamespace();
            return mod0.equals(mod1);
        }
        
        // Tag-based matching (replacement for OreDictionary)
        if (filter.useOre && !stack0.isEmpty()) {
            // Check if they share any item tags
            for (var tag : stack0.getTags().toList()) {
                if (stack1.is(tag)) {
                    return true;
                }
            }
        }
        
        // NBT comparison
        boolean nbtMatch = true;
        if (!filter.igNBT) {
            if (filter.relaxedNBT) {
                nbtMatch = ThaumcraftInvHelper.areItemStackTagsEqualRelaxed(stack0, stack1);
            } else {
                nbtMatch = ItemStack.isSameItemSameComponents(stack0, stack1) || 
                           (!NBTHelper.hasTag(stack0) && !NBTHelper.hasTag(stack1));
            }
        }
        
        // Damage comparison (legacy, items don't have damage metadata in 1.20.1)
        boolean damageMatch = filter.igDmg || 
                stack0.getDamageValue() == stack1.getDamageValue() ||
                !stack0.isDamageableItem();
        
        return stack0.is(stack1.getItem()) && damageMatch && nbtMatch;
    }
    
    /**
     * Find the first matching item from a filter list in an inventory.
     */
    public static ItemStack findFirstMatchFromFilter(NonNullList<ItemStack> filterStacks, boolean blacklist,
            IItemHandler inv, Direction face, ThaumcraftInvHelper.InvFilter filter) {
        return findFirstMatchFromFilter(filterStacks, blacklist, inv, face, filter, false);
    }
    
    /**
     * Find the first matching item with optional leave-one behavior.
     */
    public static ItemStack findFirstMatchFromFilter(NonNullList<ItemStack> filterStacks, boolean blacklist,
            IItemHandler inv, Direction face, ThaumcraftInvHelper.InvFilter filter, boolean leaveOne) {
        slotLoop:
        for (int slot = 0; slot < inv.getSlots(); slot++) {
            ItemStack slotStack = inv.getStackInSlot(slot);
            if (slotStack.isEmpty() || slotStack.getCount() <= 0) continue;
            
            if (leaveOne && ThaumcraftInvHelper.countTotalItemsIn(inv, slotStack, filter) < 2) {
                continue;
            }
            
            boolean allow = false;
            boolean allEmpty = true;
            
            for (ItemStack filterStack : filterStacks) {
                if (filterStack == null || filterStack.isEmpty()) continue;
                allEmpty = false;
                
                boolean matches = areItemStacksEqual(filterStack.copy(), slotStack.copy(), filter);
                
                if (blacklist) {
                    if (matches) continue slotLoop;
                    allow = true;
                } else {
                    if (matches) return slotStack;
                }
            }
            
            if (blacklist && (allow || allEmpty)) {
                return slotStack;
            }
        }
        return ItemStack.EMPTY;
    }
    
    /**
     * Check if an item matches the filter list.
     */
    public static boolean matchesFilters(NonNullList<ItemStack> filterList, boolean blacklist,
            ItemStack stack, ThaumcraftInvHelper.InvFilter filter) {
        if (stack == null || stack.isEmpty()) return false;
        
        boolean allow = false;
        boolean allEmpty = true;
        
        for (ItemStack filterStack : filterList) {
            if (filterStack == null || filterStack.isEmpty()) continue;
            allEmpty = false;
            
            boolean matches = areItemStacksEqual(filterStack.copy(), stack.copy(), filter);
            
            if (blacklist) {
                if (matches) return false;
                allow = true;
            } else {
                if (matches) return true;
            }
        }
        
        return blacklist && (allow || allEmpty);
    }
    
    /**
     * Drop harvested items at a position.
     */
    public static void dropHarvestsAtPos(Level level, BlockPos pos, List<ItemStack> items) {
        dropHarvestsAtPos(level, pos, items, false, 0, null);
    }
    
    /**
     * Drop harvested items with optional follow behavior.
     */
    public static void dropHarvestsAtPos(Level level, BlockPos pos, List<ItemStack> items,
            boolean followItem, int color, @Nullable Entity target) {
        if (level.isClientSide()) return;
        if (!level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS)) return;
        
        for (ItemStack item : items) {
            if (item.isEmpty()) continue;
            
            float spread = 0.5f;
            double x = pos.getX() + level.random.nextFloat() * spread + (1.0f - spread) * 0.5;
            double y = pos.getY() + level.random.nextFloat() * spread + (1.0f - spread) * 0.5;
            double z = pos.getZ() + level.random.nextFloat() * spread + (1.0f - spread) * 0.5;
            
            ItemEntity entityItem;
            if (followItem && target != null) {
                // TODO: Use EntityFollowingItem when ported
                entityItem = new ItemEntity(level, x, y, z, item);
            } else {
                entityItem = new ItemEntity(level, x, y, z, item);
            }
            
            entityItem.setDefaultPickUpDelay();
            level.addFreshEntity(entityItem);
        }
    }
    
    /**
     * Drop an item at a block position.
     */
    public static void dropItemAtPos(Level level, ItemStack item, BlockPos pos) {
        if (!level.isClientSide() && !item.isEmpty()) {
            ItemEntity entityItem = new ItemEntity(level, 
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, item.copy());
            level.addFreshEntity(entityItem);
        }
    }
    
    /**
     * Drop an item at an entity's position.
     */
    public static void dropItemAtEntity(Level level, ItemStack item, Entity entity) {
        if (!level.isClientSide() && !item.isEmpty()) {
            ItemEntity entityItem = new ItemEntity(level,
                    entity.getX(), entity.getY() + entity.getEyeHeight() / 2.0f, entity.getZ(), 
                    item.copy());
            level.addFreshEntity(entityItem);
        }
    }
    
    /**
     * Drop all items from a container at an entity's position.
     */
    public static void dropItemsAtEntity(Level level, BlockPos pos, Entity entity) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof Container container) || level.isClientSide()) {
            return;
        }
        
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack item = container.getItem(i);
            if (!item.isEmpty()) {
                ItemEntity entityItem = new ItemEntity(level,
                        entity.getX(), entity.getY() + entity.getEyeHeight() / 2.0f, entity.getZ(),
                        item.copy());
                level.addFreshEntity(entityItem);
                container.setItem(i, ItemStack.EMPTY);
            }
        }
    }
    
    /**
     * Cycle through ingredient alternatives for display purposes.
     * Used in recipe GUIs to animate through multiple valid inputs.
     */
    public static ItemStack cycleItemStack(Object input) {
        return cycleItemStack(input, 0);
    }
    
    /**
     * Cycle through ingredient alternatives with a counter offset.
     */
    public static ItemStack cycleItemStack(Object input, int counter) {
        ItemStack result = ItemStack.EMPTY;
        
        if (input instanceof Ingredient ingredient) {
            ItemStack[] stacks = ingredient.getItems();
            if (stacks.length > 0) {
                int idx = (int) ((counter + System.currentTimeMillis() / 1000L) % stacks.length);
                result = cycleItemStack(stacks[idx], counter + 1);
            }
        } else if (input instanceof ItemStack[] stacks) {
            if (stacks.length > 0) {
                int idx = (int) ((counter + System.currentTimeMillis() / 1000L) % stacks.length);
                result = cycleItemStack(stacks[idx], counter + 1);
            }
        } else if (input instanceof ItemStack stack) {
            result = stack;
            // Handle damageable items - cycle through damage values for display
            if (!stack.isEmpty() && stack.isDamageableItem()) {
                int interval = 5000 / stack.getMaxDamage();
                int damage = (int) ((counter + System.currentTimeMillis() / interval) % stack.getMaxDamage());
                ItemStack cycled = stack.copy();
                cycled.setDamageValue(damage);
                result = cycled;
            }
        } else if (input instanceof List<?> list) {
            if (!list.isEmpty() && list.get(0) instanceof ItemStack) {
                @SuppressWarnings("unchecked")
                List<ItemStack> stacks = (List<ItemStack>) list;
                int idx = (int) ((counter + System.currentTimeMillis() / 1000L) % stacks.size());
                result = cycleItemStack(stacks.get(idx), counter + 1);
            }
        } else if (input instanceof TagKey<?> tag) {
            // Handle item tags (replacement for ore dictionary strings)
            @SuppressWarnings("unchecked")
            TagKey<Item> itemTag = (TagKey<Item>) tag;
            var items = net.minecraft.core.registries.BuiltInRegistries.ITEM.getTag(itemTag);
            if (items.isPresent()) {
                var list = items.get().stream().toList();
                if (!list.isEmpty()) {
                    int idx = (int) ((counter + System.currentTimeMillis() / 1000L) % list.size());
                    result = new ItemStack(list.get(idx).value());
                }
            }
        }
        
        return result;
    }
    
    /**
     * Get an IItemHandler from a block entity.
     */
    @Nullable
    public static IItemHandler getItemHandler(Level level, BlockPos pos, Direction side) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) {
            return be.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, be.getBlockPos(), side);
        }
        return null;
    }
}




