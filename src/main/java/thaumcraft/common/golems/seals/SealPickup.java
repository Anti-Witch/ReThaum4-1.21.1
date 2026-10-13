package thaumcraft.common.golems.seals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import thaumcraft.Thaumcraft;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.GolemHelper;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.seals.ISealConfigArea;
import thaumcraft.api.golems.seals.ISealConfigToggles;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.golems.EntityThaumcraftGolem;
import thaumcraft.common.golems.tasks.TaskHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * SealPickup - Makes golems pick up dropped items in an area.
 * 
 * Features:
 * - Configurable pickup area
 * - Item filter with whitelist/blacklist
 * - Metadata/NBT/OreDictionary/Mod matching options
 * 
 * Ported from 1.12.2.
 */
public class SealPickup extends SealFiltered implements ISealConfigArea, ISealConfigToggles {
    
    private int delay;
    private HashMap<Integer, Integer> itemEntities = new HashMap<>();
    private ResourceLocation icon;
    protected SealToggle[] props;
    
    public SealPickup() {
        delay = new Random(System.nanoTime()).nextInt(100);
        icon = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "items/seals/seal_pickup");
        props = new SealToggle[] {
            new SealToggle(true, "pmeta", "golem.prop.meta"),
            new SealToggle(true, "pnbt", "golem.prop.nbt"),
            new SealToggle(false, "pore", "golem.prop.ore"),
            new SealToggle(false, "pmod", "golem.prop.mod")
        };
    }
    
    @Override
    public String getKey() {
        return "thaumcraft:pickup";
    }
    
    @Override
    public void tickSeal(Level level, ISealEntity seal) {
        if (delay++ % 5 != 0) {
            return;
        }
        
        AABB area = GolemHelper.getBoundsForArea(seal);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, area);
        
        if (!items.isEmpty()) {
            for (ItemEntity itemEntity : items) {
                if (itemEntity != null && 
                    itemEntity.onGround() && 
                    !itemEntity.hasPickUpDelay() && 
                    !itemEntity.getItem().isEmpty() &&
                    !itemEntities.containsValue(itemEntity.getId())) {
                    
                    // Check if item matches filter
                    if (matchesFilter(itemEntity.getItem())) {
                        Task task = new Task(seal.getSealPos(), itemEntity);
                        task.setPriority(seal.getPriority());
                        itemEntities.put(task.getId(), itemEntity.getId());
                        TaskHandler.addTask(level.dimension(), task);
                        break;
                    }
                }
            }
        }
        
        // Periodic cleanup of dead item references
        if (delay % 100 == 0) {
            Iterator<Integer> it = itemEntities.values().iterator();
            while (it.hasNext()) {
                Entity e = level.getEntity(it.next());
                if (e == null || !e.isAlive()) {
                    try {
                        it.remove();
                    } catch (Exception ignored) {}
                }
            }
        }
    }
    
    /**
     * Check if an item matches the filter
     */
    private boolean matchesFilter(ItemStack stack) {
        if (stack.isEmpty()) return false;
        
        // If filter is empty, match everything (in whitelist mode) or nothing (in blacklist mode)
        boolean filterEmpty = true;
        for (ItemStack filterStack : filter) {
            if (!filterStack.isEmpty()) {
                filterEmpty = false;
                break;
            }
        }
        
        if (filterEmpty) {
            return blacklist; // Empty filter: blacklist=true means accept all, whitelist=false means accept all
        }
        
        // Check each filter slot
        for (ItemStack filterStack : filter) {
            if (!filterStack.isEmpty()) {
                boolean matches = ItemStack.isSameItem(stack, filterStack);
                
                // Check NBT if enabled
                if (matches && props[1].getValue()) {
                    matches = ItemStack.isSameItemSameComponents(stack, filterStack);
                }
                
                if (matches) {
                    return !blacklist; // Found match: return opposite of blacklist
                }
            }
        }
        
        return blacklist; // No match: return blacklist mode (true = accept, false = reject)
    }
    
    @Override
    public boolean onTaskCompletion(Level level, IGolemAPI golem, Task task) {
        ItemEntity itemEntity = getItemEntity(level, task);
        
        if (itemEntity != null && !itemEntity.getItem().isEmpty()) {
            if (matchesFilter(itemEntity.getItem())) {
                ItemStack remaining = golem.holdItem(itemEntity.getItem());
                
                if (!remaining.isEmpty() && remaining.getCount() > 0) {
                    itemEntity.setItem(remaining);
                } else {
                    itemEntity.discard();
                }
                
                ((Entity) golem).playSound(SoundEvents.ITEM_PICKUP, 0.125f, 
                    ((level.random.nextFloat() - level.random.nextFloat()) * 0.7f + 1.0f) * 2.0f);
                golem.swingArm();
            }
        }
        
        task.setSuspended(true);
        itemEntities.remove(task.getId());
        
        // Look for more pickup tasks
        ArrayList<Task> localTasks = TaskHandler.getEntityTasksSorted(level.dimension(), null, (Entity) golem);
        for (Task ticket : localTasks) {
            if (itemEntities.containsKey(ticket.getId()) && 
                ticket.canGolemPerformTask(golem) && 
                ((EntityThaumcraftGolem) golem).isWithinRestriction(ticket.getEntity().blockPosition())) {
                
                ((EntityThaumcraftGolem) golem).setTask(ticket);
                ((EntityThaumcraftGolem) golem).getTask().setReserved(true);
                level.broadcastEntityEvent((Entity) golem, (byte) 5);
                break;
            }
        }
        
        return true;
    }
    
    protected ItemEntity getItemEntity(Level level, Task task) {
        Integer entityId = itemEntities.get(task.getId());
        if (entityId != null) {
            Entity entity = level.getEntity(entityId);
            if (entity instanceof ItemEntity itemEntity) {
                return itemEntity;
            }
        }
        return null;
    }
    
    @Override
    public boolean canGolemPerformTask(IGolemAPI golem, Task task) {
        ItemEntity itemEntity = getItemEntity(golem.getGolemWorld(), task);
        if (itemEntity == null || itemEntity.getItem().isEmpty()) {
            return false;
        }
        if (!itemEntity.isAlive()) {
            task.setSuspended(true);
            return false;
        }
        return golem.canCarry(itemEntity.getItem(), true);
    }
    
    @Override
    public boolean canPlaceAt(Level level, BlockPos pos, Direction side) {
        return !level.isEmptyBlock(pos);
    }
    
    @Override
    public ResourceLocation getSealIcon() {
        return icon;
    }
    
    @Override
    public int[] getGuiCategories() {
        return new int[] { 2, 1, 0, 4 };
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




