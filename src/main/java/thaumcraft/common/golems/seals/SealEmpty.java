package thaumcraft.common.golems.seals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import thaumcraft.Thaumcraft;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.seals.ISealConfigToggles;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.golems.tasks.TaskHandler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

/**
 * SealEmpty - Makes golems empty items from an inventory.
 * 
 * Features:
 * - Item filter with whitelist/blacklist
 * - Leave-one option to prevent fully emptying
 * - Cycle through filter options
 * 
 * Ported from 1.12.2.
 */
public class SealEmpty extends SealFiltered implements ISealConfigToggles {
    
    private int delay;
    private int filterInc = 0;
    private HashMap<Integer, ItemStack> cache = new HashMap<>();
    private ResourceLocation icon;
    protected SealToggle[] props;
    
    public SealEmpty() {
        delay = new Random(System.nanoTime()).nextInt(30);
        icon = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "items/seals/seal_empty");
        props = new SealToggle[] {
            new SealToggle(true, "pmeta", "golem.prop.meta"),
            new SealToggle(true, "pnbt", "golem.prop.nbt"),
            new SealToggle(false, "pore", "golem.prop.ore"),
            new SealToggle(false, "pmod", "golem.prop.mod"),
            new SealToggle(false, "pcycle", "golem.prop.cycle"),
            new SealToggle(false, "pleave", "golem.prop.leave")
        };
    }
    
    @Override
    public String getKey() {
        return "thaumcraft:empty";
    }
    
    @Override
    public void tickSeal(Level level, ISealEntity seal) {
        // Periodic cache cleanup
        if (delay % 100 == 0) {
            Iterator<Integer> it = cache.keySet().iterator();
            while (it.hasNext()) {
                Task task = TaskHandler.getTask(level.dimension(), it.next());
                if (task == null) {
                    it.remove();
                }
            }
        }
        
        if (delay++ % 20 != 0) {
            return;
        }
        
        // Find item to extract
        ItemStack stack = findItemToExtract(level, seal);
        if (stack != null && !stack.isEmpty()) {
            Task task = new Task(seal.getSealPos(), seal.getSealPos().pos);
            task.setPriority(seal.getPriority());
            task.setLifespan((short) 5);
            TaskHandler.addTask(level.dimension(), task);
            cache.put(task.getId(), stack);
        }
    }
    
    /**
     * Find an item in the inventory that matches the filter
     */
    private ItemStack findItemToExtract(Level level, ISealEntity seal) {
        BlockPos pos = seal.getSealPos().pos;
        Direction face = seal.getSealPos().face;
        
        IItemHandler handler = getItemHandler(level, pos, face);
        if (handler == null) return ItemStack.EMPTY;
        
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty() && matchesFilter(stack)) {
                // Check leave-one option
                if (props[5].getValue() && stack.getCount() <= 1) {
                    continue;
                }
                return stack.copy();
            }
        }
        
        return ItemStack.EMPTY;
    }
    
    /**
     * Get item handler at position
     */
    private IItemHandler getItemHandler(Level level, BlockPos pos, Direction face) {
        var blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            var cap = blockEntity.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, blockEntity.getBlockPos(), face);
            if (cap != null) {
                return cap;
            }
        }
        return null;
    }
    
    /**
     * Check if an item matches the filter
     */
    private boolean matchesFilter(ItemStack stack) {
        if (stack.isEmpty()) return false;
        
        // If filter is empty, match based on blacklist mode
        boolean filterEmpty = true;
        for (ItemStack filterStack : filter) {
            if (!filterStack.isEmpty()) {
                filterEmpty = false;
                break;
            }
        }
        
        if (filterEmpty) {
            return blacklist;
        }
        
        // Check each filter slot
        for (ItemStack filterStack : filter) {
            if (!filterStack.isEmpty()) {
                boolean matches = ItemStack.isSameItem(stack, filterStack);
                
                if (matches && props[1].getValue()) {
                    matches = ItemStack.isSameItemSameComponents(stack, filterStack);
                }
                
                if (matches) {
                    return !blacklist;
                }
            }
        }
        
        return blacklist;
    }
    
    @Override
    public boolean onTaskCompletion(Level level, IGolemAPI golem, Task task) {
        ItemStack stack = cache.get(task.getId());
        
        if (stack != null && !stack.isEmpty()) {
            // Check leave-one
            if (props[5].getValue()) {
                int total = countItemsInInventory(level, task.getSealPos().pos, task.getSealPos().face, stack);
                if (total <= stack.getCount()) {
                    stack = stack.copy();
                    stack.setCount(Math.max(0, total - 1));
                }
            }
            
            if (!stack.isEmpty()) {
                int limit = golem.canCarryAmount(stack);
                if (limit > 0) {
                    ItemStack extracted = extractFromInventory(level, task.getSealPos().pos, 
                        task.getSealPos().face, stack, Math.min(limit, stack.getCount()));
                    
                    if (!extracted.isEmpty()) {
                        ItemStack remaining = golem.holdItem(extracted);
                        if (!remaining.isEmpty()) {
                            // Eject overflow
                            spawnItemEntity(level, task.getSealPos().pos.relative(task.getSealPos().face), remaining);
                        }
                        
                        ((Entity) golem).playSound(SoundEvents.ITEM_PICKUP, 0.125f,
                            ((level.random.nextFloat() - level.random.nextFloat()) * 0.7f + 1.0f) * 2.0f);
                        golem.swingArm();
                    }
                }
            }
        }
        
        cache.remove(task.getId());
        filterInc++;
        task.setSuspended(true);
        return true;
    }
    
    private int countItemsInInventory(Level level, BlockPos pos, Direction face, ItemStack stack) {
        IItemHandler handler = getItemHandler(level, pos, face);
        if (handler == null) return 0;
        
        int count = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack slotStack = handler.getStackInSlot(slot);
            if (ItemStack.isSameItemSameComponents(slotStack, stack)) {
                count += slotStack.getCount();
            }
        }
        return count;
    }
    
    private ItemStack extractFromInventory(Level level, BlockPos pos, Direction face, ItemStack match, int amount) {
        IItemHandler handler = getItemHandler(level, pos, face);
        if (handler == null) return ItemStack.EMPTY;
        
        ItemStack result = ItemStack.EMPTY;
        int remaining = amount;
        
        for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
            ItemStack slotStack = handler.getStackInSlot(slot);
            if (ItemStack.isSameItemSameComponents(slotStack, match)) {
                ItemStack extracted = handler.extractItem(slot, remaining, false);
                if (!extracted.isEmpty()) {
                    if (result.isEmpty()) {
                        result = extracted;
                    } else {
                        result.grow(extracted.getCount());
                    }
                    remaining -= extracted.getCount();
                }
            }
        }
        
        return result;
    }
    
    private void spawnItemEntity(Level level, BlockPos pos, ItemStack stack) {
        var itemEntity = new net.minecraft.world.entity.item.ItemEntity(level,
            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        level.addFreshEntity(itemEntity);
    }
    
    @Override
    public boolean canGolemPerformTask(IGolemAPI golem, Task task) {
        ItemStack stack = cache.get(task.getId());
        return stack != null && !stack.isEmpty() && golem.canCarry(stack, true);
    }
    
    @Override
    public boolean canPlaceAt(Level level, BlockPos pos, Direction side) {
        return getItemHandler(level, pos, side) != null;
    }
    
    @Override
    public ResourceLocation getSealIcon() {
        return icon;
    }
    
    @Override
    public int[] getGuiCategories() {
        return new int[] { 1, 0, 4 };
    }
    
    @Override
    public EnumGolemTrait[] getRequiredTags() {
        return null;
    }
    
    @Override
    public EnumGolemTrait[] getForbiddenTags() {
        return new EnumGolemTrait[] { EnumGolemTrait.CLUMSY };
    }
    
    @Override
    public void onTaskStarted(Level level, IGolemAPI golem, Task task) {
    }
    
    @Override
    public void onTaskSuspension(Level level, Task task) {
        cache.remove(task.getId());
    }
    
    @Override
    public void onRemoval(Level level, BlockPos pos, Direction side) {
    }
    
    @Override
    public SealToggle[] getToggles() {
        return props;
    }
    
    @Override
    public void setToggle(int index, boolean value) {
        if (index >= 0 && index < props.length) {
            props[index].setValue(value);
        }
    }
}




