package thaumcraft.common.golems.seals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
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

import java.util.List;
import java.util.Random;

/**
 * SealButcher - Makes golems kill excess animals for farming.
 * 
 * Features:
 * - Only kills when more than 2 of same animal type exist
 * - Doesn't kill baby animals
 * - Doesn't kill tamed animals
 * - Requires FIGHTER and SMART traits
 * 
 * Ported from 1.12.2.
 */
public class SealButcher implements ISeal, ISealConfigArea {
    
    private int delay;
    private boolean wait = false;
    private ResourceLocation icon;
    
    public SealButcher() {
        delay = new Random(System.nanoTime()).nextInt(200);
        icon = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "items/seals/seal_butcher");
    }
    
    @Override
    public String getKey() {
        return "thaumcraft:butcher";
    }
    
    @Override
    public void tickSeal(Level level, ISealEntity seal) {
        if (delay++ % 200 != 0 || wait) {
            return;
        }
        
        AABB area = GolemHelper.getBoundsForArea(seal);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, area);
        
        if (!entities.isEmpty()) {
            for (LivingEntity target : entities) {
                if (isValidTarget(target)) {
                    // Count how many of this type exist
                    int count = 0;
                    for (LivingEntity other : entities) {
                        if (other.getClass() == target.getClass() && isValidTarget(other)) {
                            count++;
                            if (count > 2) break;
                        }
                    }
                    
                    // Only kill if more than 2 exist
                    if (count > 2) {
                        Task task = new Task(seal.getSealPos(), target);
                        task.setPriority(seal.getPriority());
                        task.setLifespan((short) 10);
                        TaskHandler.addTask(level.dimension(), task);
                        wait = true;
                        break;
                    }
                }
            }
        }
    }
    
    /**
     * Check if an entity is a valid butcher target
     */
    private boolean isValidTarget(LivingEntity target) {
        // Must be an animal
        if (!(target instanceof Animal animal)) {
            return false;
        }
        
        // Not hostile mobs
        if (target instanceof Enemy) {
            return false;
        }
        
        // Not tamed animals
        if (target instanceof TamableAnimal tamable && tamable.isTame()) {
            return false;
        }
        
        // Not baby animals
        if (animal.isBaby()) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public void onTaskStarted(Level level, IGolemAPI golem, Task task) {
        if (task.getEntity() != null && 
            task.getEntity() instanceof LivingEntity target && 
            isValidTarget(target)) {
            
            if (golem instanceof Mob mob) {
                mob.setTarget(target);
            }
            golem.addRankXp(1);
        }
        task.setSuspended(true);
        wait = false;
    }
    
    @Override
    public boolean onTaskCompletion(Level level, IGolemAPI golem, Task task) {
        task.setSuspended(true);
        wait = false;
        return true;
    }
    
    @Override
    public boolean canGolemPerformTask(IGolemAPI golem, Task task) {
        return true;
    }
    
    @Override
    public boolean canPlaceAt(Level level, BlockPos pos, Direction side) {
        return !level.isEmptyBlock(pos);
    }
    
    @Override
    public ResourceLocation getSealIcon() {
        return icon;
    }
    
    public int[] getGuiCategories() {
        return new int[] { 2, 0, 4 };
    }
    
    @Override
    public EnumGolemTrait[] getRequiredTags() {
        return new EnumGolemTrait[] { EnumGolemTrait.FIGHTER, EnumGolemTrait.SMART };
    }
    
    @Override
    public EnumGolemTrait[] getForbiddenTags() {
        return null;
    }
    
    @Override
    public void onTaskSuspension(Level level, Task task) {
        wait = false;
    }
    
    @Override
    public void readCustomNBT(CompoundTag nbt) {
    }
    
    @Override
    public void writeCustomNBT(CompoundTag nbt) {
    }
    
    @Override
    public void onRemoval(Level level, BlockPos pos, Direction side) {
        wait = false;
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
}




