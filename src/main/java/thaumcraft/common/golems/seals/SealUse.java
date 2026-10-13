package thaumcraft.common.golems.seals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.GolemHelper;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.seals.ISealConfigToggles;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.golems.GolemInteractionHelper;
import thaumcraft.common.golems.tasks.TaskHandler;

import java.util.Random;

/**
 * SealUse - Makes golems right-click (or left-click) blocks.
 * 
 * This seal directs golems to use items on the marked block.
 * Useful for:
 * - Planting seeds
 * - Interacting with machines
 * - Pressing buttons/levers
 * - Placing blocks
 * 
 * Features:
 * - Left-click or right-click mode
 * - Empty hand or held item mode
 * - Sneaking mode
 * - Wait for air block or solid block mode
 * - Optional provisioning requests
 * 
 * Ported from 1.12.2.
 */
public class SealUse extends SealFiltered implements ISealConfigToggles {
    
    private int delay;
    private int watchedTask = Integer.MIN_VALUE;
    private ResourceLocation icon;
    protected SealToggle[] props;
    
    public SealUse() {
        delay = new Random(System.nanoTime()).nextInt(49);
        icon = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "items/seals/seal_use");
        props = new SealToggle[] {
            new SealToggle(true, "pmeta", "golem.prop.meta"),
            new SealToggle(true, "pnbt", "golem.prop.nbt"),
            new SealToggle(false, "pore", "golem.prop.ore"),
            new SealToggle(false, "pmod", "golem.prop.mod"),
            new SealToggle(false, "pleft", "golem.prop.left"),            // Left-click instead of right-click
            new SealToggle(false, "pempty", "golem.prop.empty"),          // Wait for empty block
            new SealToggle(false, "pemptyhand", "golem.prop.emptyhand"),  // Use empty hand
            new SealToggle(false, "psneak", "golem.prop.sneak"),          // Sneak while clicking
            new SealToggle(false, "ppro", "golem.prop.provision.wl")     // Request provisioning
        };
    }
    
    @Override
    public String getKey() {
        return "thaumcraft:use";
    }
    
    @Override
    public void tickSeal(Level level, ISealEntity seal) {
        if (delay++ % 5 != 0) {
            return;
        }
        
        // Check if there's an active task
        Task oldTask = TaskHandler.getTask(level.dimension(), watchedTask);
        if (oldTask == null || oldTask.isSuspended() || oldTask.isCompleted()) {
            // Check empty block condition if enabled
            boolean isEmpty = level.isEmptyBlock(seal.getSealPos().pos);
            if (props[5].getValue() != isEmpty) {
                return; // Condition not met
            }
            
            // Create new task
            Task task = new Task(seal.getSealPos(), seal.getSealPos().pos);
            task.setPriority(seal.getPriority());
            TaskHandler.addTask(level.dimension(), task);
            watchedTask = task.getId();
        }
    }
    
    @Override
    public void onTaskStarted(Level level, IGolemAPI golem, Task task) {
    }
    
    @Override
    public boolean onTaskCompletion(Level level, IGolemAPI golem, Task task) {
        // Check empty block condition
        boolean isEmpty = level.isEmptyBlock(task.getPos());
        if (props[5].getValue() == isEmpty) {
            // Get item to use
            ItemStack clickStack = ItemStack.EMPTY;
            
            if (!props[6].getValue()) { // Not empty hand mode
                // Get item from golem's carrying
                if (!filter.get(0).isEmpty()) {
                    // Use filter to find matching item
                    clickStack = findMatchingCarried(golem);
                } else if (!golem.getCarrying().isEmpty()) {
                    clickStack = golem.getCarrying().get(0);
                }
            }
            
            if (!clickStack.isEmpty() || props[6].getValue()) {
                // Perform the click
                ItemStack toUse = props[6].getValue() ? ItemStack.EMPTY : clickStack.copy();
                
                if (!clickStack.isEmpty()) {
                    golem.dropItem(clickStack.copy());
                }
                
                GolemInteractionHelper.golemClick(
                    level, golem, task.getPos(), task.getSealPos().face,
                    toUse,
                    props[7].getValue(),  // sneaking
                    !props[4].getValue()  // right-click (inverted left-click toggle)
                );
                
                golem.swingArm();
            }
        }
        
        task.setSuspended(true);
        return true;
    }
    
    /**
     * Find an item from golem's inventory matching the filter.
     */
    private ItemStack findMatchingCarried(IGolemAPI golem) {
        for (ItemStack carried : golem.getCarrying()) {
            if (!carried.isEmpty() && matchesFilter(carried)) {
                return carried;
            }
        }
        return ItemStack.EMPTY;
    }
    
    /**
     * Check if an item matches the filter.
     */
    private boolean matchesFilter(ItemStack stack) {
        if (stack.isEmpty()) return false;
        
        boolean filterEmpty = true;
        for (ItemStack fs : filter) {
            if (!fs.isEmpty()) {
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
                if (matches && props[0].getValue()) {
                    // Match damage/meta
                    matches = stack.getDamageValue() == filterStack.getDamageValue();
                }
                if (matches && props[1].getValue()) {
                    // Match NBT
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
    public boolean canGolemPerformTask(IGolemAPI golem, Task task) {
        // Empty hand mode doesn't require carrying anything
        if (props[6].getValue()) {
            return true;
        }
        
        // Need to be carrying a matching item
        boolean found = !findMatchingCarried(golem).isEmpty();
        
        // Request provisioning if enabled and not found
        if (!found && props[8].getValue() && !blacklist && !filter.get(0).isEmpty()) {
            ISealEntity sealEntity = SealHandler.getSealEntity(golem.getGolemWorld().dimension(), task.getSealPos());
            if (sealEntity != null) {
                ItemStack requested = filter.get(0).copy();
                GolemHelper.requestProvisioning(golem.getGolemWorld(), sealEntity, requested);
            }
        }
        
        return found;
    }
    
    @Override
    public void onTaskSuspension(Level level, Task task) {
    }
    
    @Override
    public boolean canPlaceAt(Level level, BlockPos pos, Direction side) {
        return true; // Can be placed anywhere
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
        return null; // TODO: Implement when GUI system is available
    }
    
    @OnlyIn(Dist.CLIENT)
    @Override
    public Object returnGui(Level level, Player player, BlockPos pos, Direction side, ISealEntity seal) {
        return null; // TODO: Implement when GUI system is available
    }
    
    @Override
    public int[] getGuiCategories() {
        return new int[] { 1, 3, 0, 4 };
    }
    
    @Override
    public EnumGolemTrait[] getRequiredTags() {
        return new EnumGolemTrait[] { EnumGolemTrait.DEFT, EnumGolemTrait.SMART };
    }
    
    @Override
    public EnumGolemTrait[] getForbiddenTags() {
        return null;
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




