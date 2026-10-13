package thaumcraft.common.golems.seals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.world.entity.player.Player;
import thaumcraft.Thaumcraft;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.GolemHelper;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.seals.ISealConfigArea;
import thaumcraft.api.golems.seals.ISealConfigToggles;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.golems.tasks.TaskHandler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

/**
 * SealBreaker - Makes golems break blocks in an area.
 * 
 * Features:
 * - Configurable break area
 * - Block filter with whitelist/blacklist
 * - Progressive block breaking animation
 * - Requires BREAKER trait
 * 
 * Ported from 1.12.2.
 */
public class SealBreaker extends SealFiltered implements ISealConfigArea, ISealConfigToggles {
    
    private int delay;
    private HashMap<Integer, Long> cache = new HashMap<>();
    private ResourceLocation icon;
    protected SealToggle[] props;
    
    public SealBreaker() {
        delay = new Random(System.nanoTime()).nextInt(42);
        icon = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "items/seals/seal_breaker");
        props = new SealToggle[] {
            new SealToggle(true, "pmeta", "golem.prop.meta")
        };
    }
    
    @Override
    public String getKey() {
        return "thaumcraft:breaker";
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
        
        delay++;
        BlockPos pos = GolemHelper.getPosInArea(seal, delay);
        
        if (!cache.containsValue(pos.asLong()) && isValidBlock(level, pos)) {
            Task task = new Task(seal.getSealPos(), pos);
            task.setPriority(seal.getPriority());
            // Store block hardness * 10 as task data for progressive breaking
            BlockState state = level.getBlockState(pos);
            task.setData((int) (state.getDestroySpeed(level, pos) * 10.0f));
            TaskHandler.addTask(level.dimension(), task);
            cache.put(task.getId(), pos.asLong());
        }
    }
    
    /**
     * Check if a block should be broken
     */
    private boolean isValidBlock(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        
        if (level.isEmptyBlock(pos) || state.getDestroySpeed(level, pos) < 0.0f) {
            return false;
        }
        
        // Check against filter
        ItemStack blockStack = new ItemStack(state.getBlock());
        return matchesFilter(blockStack);
    }
    
    /**
     * Check if item matches the filter
     */
    private boolean matchesFilter(ItemStack stack) {
        if (stack.isEmpty()) return blacklist;
        
        boolean filterEmpty = true;
        for (ItemStack filterStack : filter) {
            if (!filterStack.isEmpty()) {
                filterEmpty = false;
                break;
            }
        }
        
        if (filterEmpty) {
            return true; // No filter = accept all
        }
        
        for (ItemStack filterStack : filter) {
            if (!filterStack.isEmpty()) {
                boolean matches = ItemStack.isSameItem(stack, filterStack);
                if (matches) {
                    return !blacklist;
                }
            }
        }
        
        return blacklist;
    }
    
    @Override
    public boolean onTaskCompletion(Level level, IGolemAPI golem, Task task) {
        BlockState state = level.getBlockState(task.getPos());
        
        if (cache.containsKey(task.getId()) && isValidBlock(level, task.getPos())) {
            golem.swingArm();
            
            int breakSpeed = 21; // Default break speed
            
            if (task.getData() > breakSpeed) {
                // Progressive breaking
                float hardness = state.getDestroySpeed(level, task.getPos()) * 10.0f;
                task.setLifespan((short) Math.max(task.getLifespan(), 10));
                task.setData(task.getData() - breakSpeed);
                
                // Play break sound
                level.playSound(null, task.getPos(), 
                    state.getSoundType().getBreakSound(), 
                    SoundSource.BLOCKS, 
                    (state.getSoundType().getVolume() + 0.7f) / 8.0f, 
                    state.getSoundType().getPitch() * 0.5f);
                
                return false; // Not done yet
            }
            
            // Break the block
            level.destroyBlock(task.getPos(), true);
            golem.addRankXp(1);
            cache.remove(task.getId());
        }
        
        task.setSuspended(true);
        return true;
    }
    
    @Override
    public boolean canGolemPerformTask(IGolemAPI golem, Task task) {
        if (cache.containsKey(task.getId()) && isValidBlock(golem.getGolemWorld(), task.getPos())) {
            return true;
        }
        task.setSuspended(true);
        return false;
    }
    
    @Override
    public void onTaskStarted(Level level, IGolemAPI golem, Task task) {
    }
    
    @Override
    public void onTaskSuspension(Level level, Task task) {
        cache.remove(task.getId());
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
        return new int[] { 2, 1, 3, 0, 4 };
    }
    
    @Override
    public EnumGolemTrait[] getRequiredTags() {
        return new EnumGolemTrait[] { EnumGolemTrait.BREAKER };
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




