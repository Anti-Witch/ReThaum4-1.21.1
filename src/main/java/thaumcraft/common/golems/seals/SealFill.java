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
import net.neoforged.neoforge.items.ItemHandlerHelper;
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
 * SealFill - Makes golems deposit items into an inventory.
 * 
 * Features:
 * - Item filter with whitelist/blacklist
 * - Golems will deposit matching items they are carrying
 * 
 * Ported from 1.12.2.
 */
public class SealFill extends SealFiltered implements ISealConfigToggles {
    
    private int delay;
    private HashMap<Integer, ItemStack> cache = new HashMap<>();
    private ResourceLocation icon;
    protected SealToggle[] props;
    
    public SealFill() {
        delay = new Random(System.nanoTime()).nextInt(30);
        icon = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "items/seals/seal_fill");
        props = new SealToggle[] {
            new SealToggle(true, "pmeta", "golem.prop.meta"),
            new SealToggle(true, "pnbt", "golem.prop.nbt"),
            new SealToggle(false, "pore", "golem.prop.ore"),
            new SealToggle(false, "pmod", "golem.prop.mod")
        };
    }
    
    @Override
    public String getKey() {
        return "thaumcraft:fill";
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
        
        // Check if inventory can accept items
        IItemHandler handler = getItemHandler(level, seal.getSealPos().pos, seal.getSealPos().face);
        if (handler != null && hasSpace(handler)) {
            Task task = new Task(seal.getSealPos(), seal.getSealPos().pos);
            task.setPriority(seal.getPriority());
            task.setLifespan((short) 5);
            TaskHandler.addTask(level.dimension(), task);
        }
    }
    
    /**
     * Check if handler has any empty space
     */
    private boolean hasSpace(IItemHandler handler) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) {
                return true;
            }
        }
        return false;
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
        IItemHandler handler = getItemHandler(level, task.getSealPos().pos, task.getSealPos().face);
        
        if (handler != null) {
            // Try to deposit all matching items the golem is carrying
            for (ItemStack carried : golem.getCarrying()) {
                if (!carried.isEmpty() && matchesFilter(carried)) {
                    ItemStack toInsert = golem.dropItem(carried);
                    if (!toInsert.isEmpty()) {
                        ItemStack remaining = ItemHandlerHelper.insertItemStacked(handler, toInsert, false);
                        
                        if (!remaining.isEmpty()) {
                            // Put back what couldn't be inserted
                            golem.holdItem(remaining);
                        } else {
                            ((Entity) golem).playSound(SoundEvents.ITEM_PICKUP, 0.125f,
                                ((level.random.nextFloat() - level.random.nextFloat()) * 0.7f + 1.0f) * 2.0f);
                            golem.swingArm();
                        }
                    }
                }
            }
        }
        
        task.setSuspended(true);
        return true;
    }
    
    @Override
    public boolean canGolemPerformTask(IGolemAPI golem, Task task) {
        // Check if golem is carrying matching items
        for (ItemStack carried : golem.getCarrying()) {
            if (!carried.isEmpty() && matchesFilter(carried)) {
                return true;
            }
        }
        return false;
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




