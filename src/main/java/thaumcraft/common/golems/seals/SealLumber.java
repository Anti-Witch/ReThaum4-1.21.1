package thaumcraft.common.golems.seals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.GolemHelper;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.seals.ISeal;
import thaumcraft.api.golems.seals.ISealConfigArea;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.golems.tasks.TaskHandler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

/**
 * SealLumber - Makes golems chop down trees.
 * 
 * Features:
 * - Configurable lumber area
 * - Detects wood logs
 * - Requires BREAKER and SMART traits
 * 
 * Ported from 1.12.2.
 */
public class SealLumber implements ISeal, ISealConfigArea {
    
    private int delay;
    private HashMap<Integer, Long> cache = new HashMap<>();
    private ResourceLocation icon;
    
    public SealLumber() {
        delay = new Random(System.nanoTime()).nextInt(33);
        icon = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "items/seals/seal_lumber");
    }
    
    @Override
    public String getKey() {
        return "thaumcraft:lumber";
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
        
        if (!cache.containsValue(pos.asLong()) && isWoodLog(level, pos)) {
            Task task = new Task(seal.getSealPos(), pos);
            task.setPriority(seal.getPriority());
            TaskHandler.addTask(level.dimension(), task);
            cache.put(task.getId(), pos.asLong());
        }
    }
    
    /**
     * Check if a block is a wood log
     */
    private boolean isWoodLog(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(BlockTags.LOGS);
    }
    
    @Override
    public boolean onTaskCompletion(Level level, IGolemAPI golem, Task task) {
        if (cache.containsKey(task.getId()) && isWoodLog(level, task.getPos())) {
            golem.swingArm();
            
            // Break the log block
            level.destroyBlock(task.getPos(), true);
            golem.addRankXp(1);
            
            // Continue if more logs above
            BlockPos above = task.getPos().above();
            if (isWoodLog(level, above)) {
                task.setLifespan((short) Math.max(task.getLifespan(), 10));
                return false; // Don't complete yet
            }
            
            cache.remove(task.getId());
        }
        
        task.setSuspended(true);
        return true;
    }
    
    @Override
    public boolean canGolemPerformTask(IGolemAPI golem, Task task) {
        if (cache.containsKey(task.getId()) && isWoodLog(golem.getGolemWorld(), task.getPos())) {
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
    
    public int[] getGuiCategories() {
        return new int[] { 2, 0, 4 };
    }
    
    @Override
    public EnumGolemTrait[] getRequiredTags() {
        return new EnumGolemTrait[] { EnumGolemTrait.BREAKER, EnumGolemTrait.SMART };
    }
    
    @Override
    public EnumGolemTrait[] getForbiddenTags() {
        return null;
    }
    
    @Override
    public void readCustomNBT(CompoundTag nbt) {
    }
    
    @Override
    public void writeCustomNBT(CompoundTag nbt) {
    }
}




