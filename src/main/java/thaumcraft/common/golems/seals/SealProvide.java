package thaumcraft.common.golems.seals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import thaumcraft.Thaumcraft;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.GolemHelper;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.ProvisionRequest;
import thaumcraft.api.golems.seals.ISealConfigToggles;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.golems.EntityThaumcraftGolem;
import thaumcraft.common.golems.tasks.TaskHandler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

/**
 * SealProvide - Makes golems fulfill item provisioning requests.
 * 
 * This seal monitors provision requests from other seals/entities and 
 * dispatches golems to deliver items from the attached inventory.
 * 
 * Features:
 * - 9-slot item filter
 * - Leave-one option
 * - Single-item mode
 * 
 * Ported from 1.12.2.
 */
public class SealProvide extends SealFiltered implements ISealConfigToggles {
    
    private int delay;
    private ResourceLocation icon;
    protected SealToggle[] props;
    
    public SealProvide() {
        delay = new Random(System.nanoTime()).nextInt(88);
        icon = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "items/seals/seal_provider");
        props = new SealToggle[] {
            new SealToggle(true, "pmeta", "golem.prop.meta"),
            new SealToggle(true, "pnbt", "golem.prop.nbt"),
            new SealToggle(false, "pore", "golem.prop.ore"),
            new SealToggle(false, "pmod", "golem.prop.mod"),
            new SealToggle(false, "psing", "golem.prop.single"),
            new SealToggle(false, "pleave", "golem.prop.leave")
        };
    }
    
    @Override
    public String getKey() {
        return "thaumcraft:provider";
    }
    
    @Override
    public int getFilterSize() {
        return 9;
    }
    
    @Override
    public void tickSeal(Level level, ISealEntity seal) {
        var dimKey = level.dimension();
        
        // Periodic cleanup of invalid provision requests
        if (delay % 100 == 0 && GolemHelper.provisionRequests.containsKey(dimKey)) {
            Iterator<ProvisionRequest> it = GolemHelper.provisionRequests.get(dimKey).iterator();
            while (it.hasNext()) {
                ProvisionRequest pr = it.next();
                if (pr.isInvalid() || 
                    pr.getLinkedTask() == null || 
                    pr.getLinkedTask().isSuspended() || 
                    pr.getLinkedTask().isCompleted() || 
                    pr.getTimeout() < System.currentTimeMillis()) {
                    it.remove();
                }
            }
        }
        
        if (delay++ % 20 != 0) {
            return;
        }
        
        IItemHandler inv = getItemHandler(level, seal.getSealPos().pos, seal.getSealPos().face);
        if (inv == null) return;
        
        if (!GolemHelper.provisionRequests.containsKey(dimKey)) return;
        
        ArrayList<ProvisionRequest> requests = GolemHelper.provisionRequests.get(dimKey);
        
        for (ProvisionRequest pr : requests) {
            if (pr.isInvalid()) continue;
            if (pr.getLinkedTask() != null) continue; // Already being handled
            
            // Check distance
            BlockPos sealPos = seal.getSealPos().pos;
            boolean inRange = false;
            
            if (pr.getSeal() != null && pr.getSeal().getSealPos().pos.distSqr(sealPos) < 4096) {
                inRange = true;
            } else if (pr.getEntity() != null && 
                       sealPos.distToCenterSqr(pr.getEntity().getX(), pr.getEntity().getY(), pr.getEntity().getZ()) < 4096) {
                inRange = true;
            } else if (pr.getPos() != null && sealPos.distSqr(pr.getPos()) < 4096) {
                inRange = true;
            }
            
            if (!inRange) continue;
            
            // Check if we have the requested item and it matches our filter
            if (matchesFilters(pr.getStack()) && countItems(inv, pr.getStack()) > (props[5].getValue() ? 1 : 0)) {
                Task task = new Task(seal.getSealPos(), seal.getSealPos().pos);
                task.setPriority(pr.getSeal() != null ? pr.getSeal().getPriority() : (byte) 5);
                task.setLifespan((short) (pr.getSeal() != null ? 10 : 31000));
                TaskHandler.addTask(level.dimension(), task);
                pr.setLinkedTask(task);
                task.setLinkedProvision(pr);
                break;
            }
        }
    }
    
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
    
    private int countItems(IItemHandler handler, ItemStack stack) {
        int count = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack slotStack = handler.getStackInSlot(i);
            if (ItemStack.isSameItemSameComponents(slotStack, stack)) {
                count += slotStack.getCount();
            }
        }
        return count;
    }
    
    /**
     * Check if an item stack matches the seal's filter configuration.
     * Public so it can be used by LogisticsMenu.
     */
    public boolean matchesFilters(ItemStack stack) {
        if (stack.isEmpty()) return false;
        
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
        ProvisionRequest pr = task.getLinkedProvision();
        
        if (pr != null) {
            if (task.getData() == 0) {
                // Phase 1: Pick up items from inventory
                IItemHandler inv = getItemHandler(level, task.getSealPos().pos, task.getSealPos().face);
                if (inv != null) {
                    ItemStack stack = pr.getStack().copy();
                    
                    if (props[4].getValue()) {
                        stack.setCount(1); // Single item mode
                    }
                    
                    // Leave-one check
                    if (props[5].getValue()) {
                        int count = countItems(inv, stack);
                        if (count <= stack.getCount()) {
                            stack.setCount(Math.max(0, count - 1));
                        }
                    }
                    
                    if (!stack.isEmpty()) {
                        int limit = golem.canCarryAmount(stack);
                        if (limit > 0) {
                            ItemStack extracted = extractFromInventory(inv, stack, Math.min(limit, stack.getCount()));
                            if (!extracted.isEmpty()) {
                                ItemStack remaining = golem.holdItem(extracted);
                                if (!remaining.isEmpty()) {
                                    spawnItemEntity(level, task.getSealPos().pos.relative(task.getSealPos().face), remaining);
                                }
                                
                                ((Entity) golem).playSound(SoundEvents.ITEM_PICKUP, 0.125f,
                                    ((level.random.nextFloat() - level.random.nextFloat()) * 0.7f + 1.0f) * 2.0f);
                                golem.addRankXp(1);
                                golem.swingArm();
                                
                                // Create delivery task
                                if (pr.getEntity() != null || pr.getPos() != null) {
                                    Task deliveryTask;
                                    if (pr.getEntity() != null) {
                                        deliveryTask = new Task(task.getSealPos(), pr.getEntity());
                                    } else {
                                        deliveryTask = new Task(task.getSealPos(), pr.getPos());
                                    }
                                    deliveryTask.setPriority(task.getPriority());
                                    deliveryTask.setData(pr.getEntity() != null ? 1 : 2);
                                    deliveryTask.setLifespan((short) 31000);
                                    TaskHandler.addTask(level.dimension(), deliveryTask);
                                    pr.setLinkedTask(deliveryTask);
                                    deliveryTask.setLinkedProvision(pr);
                                }
                            }
                        }
                    }
                }
            } else {
                // Phase 2: Deliver items to destination
                ItemStack toDeliver = pr.getStack();
                ItemStack dropped = golem.dropItem(toDeliver);
                
                if (task.getData() == 1 && pr.getEntity() != null) {
                    // Drop at entity
                    spawnItemEntity(level, pr.getEntity().blockPosition(), dropped);
                } else if (task.getData() == 2 && pr.getPos() != null) {
                    // Insert into inventory
                    IItemHandler destInv = getItemHandler(level, pr.getPos(), pr.getSide());
                    if (destInv != null) {
                        ItemStack remaining = insertIntoInventory(destInv, dropped);
                        if (!remaining.isEmpty()) {
                            golem.holdItem(remaining);
                        }
                    } else {
                        spawnItemEntity(level, pr.getPos().relative(pr.getSide()), dropped);
                    }
                }
                
                ((Entity) golem).playSound(SoundEvents.ITEM_PICKUP, 0.125f,
                    ((level.random.nextFloat() - level.random.nextFloat()) * 0.7f + 1.0f) * 1.0f);
                golem.swingArm();
                pr.setInvalid(true);
            }
        }
        
        task.setSuspended(true);
        return true;
    }
    
    private ItemStack extractFromInventory(IItemHandler handler, ItemStack match, int amount) {
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
    
    private ItemStack insertIntoInventory(IItemHandler handler, ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < handler.getSlots() && !remaining.isEmpty(); slot++) {
            remaining = handler.insertItem(slot, remaining, false);
        }
        return remaining;
    }
    
    private void spawnItemEntity(Level level, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) return;
        var itemEntity = new net.minecraft.world.entity.item.ItemEntity(level,
            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        level.addFreshEntity(itemEntity);
    }
    
    @Override
    public boolean canGolemPerformTask(IGolemAPI golem, Task task) {
        ProvisionRequest pr = task.getLinkedProvision();
        if (pr == null) return false;
        
        // Check if within home range
        boolean inRange = false;
        if (golem instanceof EntityThaumcraftGolem tcGolem) {
            if (pr.getSeal() != null) {
                inRange = tcGolem.isWithinRestriction(pr.getSeal().getSealPos().pos);
            } else if (pr.getEntity() != null) {
                inRange = tcGolem.isWithinRestriction(pr.getEntity().blockPosition());
            } else if (pr.getPos() != null) {
                inRange = tcGolem.isWithinRestriction(pr.getPos());
            }
        }
        
        if (!inRange) return false;
        
        if (task.getData() == 0) {
            // Pickup phase: need to be able to carry and not already carrying
            return pr.getStack() != null && !golem.isCarrying(pr.getStack()) && golem.canCarry(pr.getStack(), true);
        } else {
            // Delivery phase: need to be carrying the item
            return pr.getStack() != null && golem.isCarrying(pr.getStack());
        }
    }
    
    @Override
    public void onTaskStarted(Level level, IGolemAPI golem, Task task) {
    }
    
    @Override
    public void onTaskSuspension(Level level, Task task) {
        if (task.getLinkedProvision() != null) {
            task.getLinkedProvision().setLinkedTask(null);
        }
        task.setLinkedProvision(null);
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
    public void onRemoval(Level level, BlockPos pos, Direction side) {
    }
    
    @Override
    public Object returnContainer(Level level, Player player, BlockPos pos, Direction side, ISealEntity seal) {
        return null;
    }
    
    @OnlyIn(Dist.CLIENT)
    @Override
    public Object returnGui(Level level, Player player, BlockPos pos, Direction side, ISealEntity seal) {
        return null;
    }
    
    @Override
    public int[] getGuiCategories() {
        return new int[] { 1, 3, 0, 4 };
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




