package thaumcraft.common.golems.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.golems.EntityThaumcraftGolem;
import thaumcraft.common.golems.seals.SealHandler;
import thaumcraft.common.golems.tasks.TaskHandler;

import java.util.Arrays;
import java.util.EnumSet;

/**
 * AIGoto - Base class for golem task navigation AI.
 * 
 * Handles finding tasks, moving to destinations, and completing tasks.
 * Subclasses implement specific behavior for block vs entity targets.
 * 
 * Ported from 1.12.2.
 */
public abstract class AIGoto extends Goal {
    
    protected EntityThaumcraftGolem golem;
    protected int taskCounter;
    protected byte type;
    protected int cooldown;
    protected double minDist;
    private BlockPos prevRamble;
    protected BlockPos targetBlock;
    protected int pause;
    
    public AIGoto(EntityThaumcraftGolem golem, byte type) {
        this.golem = golem;
        this.type = type;
        this.taskCounter = -1;
        this.minDist = 4.0;
        this.pause = 0;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }
    
    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        cooldown = 5;
        
        // Don't start if already have a task
        if (golem.getTask() != null && !golem.getTask().isSuspended()) {
            return false;
        }
        
        targetBlock = null;
        boolean start = findDestination();
        
        // Notify seal that task started
        if (start && golem.getTask() != null && golem.getTask().getSealPos() != null) {
            ISealEntity sealEntity = SealHandler.getSealEntity(golem.level().dimension(), golem.getTask().getSealPos());
            if (sealEntity != null) {
                sealEntity.getSeal().onTaskStarted(golem.level(), golem, golem.getTask());
            }
        }
        
        return start;
    }
    
    @Override
    public void start() {
        moveTo();
        taskCounter = 0;
    }
    
    /**
     * Move toward the target
     */
    protected abstract void moveTo();
    
    @Override
    public boolean canContinueToUse() {
        return taskCounter >= 0 && 
               taskCounter <= 1000 && 
               golem.getTask() != null && 
               !golem.getTask().isSuspended() && 
               isValidDestination(golem.level(), golem.getTask().getPos());
    }
    
    @Override
    public void tick() {
        Task task = golem.getTask();
        if (task == null) return;
        
        if (pause-- <= 0) {
            double dist;
            if (task.getType() == 0) {
                // Block task
                BlockPos targetPos = (targetBlock != null) ? targetBlock : task.getPos();
                dist = golem.distanceToSqr(Vec3.atCenterOf(targetPos));
            } else {
                // Entity task
                dist = golem.distanceToSqr(task.getEntity());
            }
            
            if (dist > minDist) {
                // Still moving toward target
                task.setCompletion(false);
                taskCounter++;
                
                if (taskCounter % 5 == 0) {
                    BlockPos currentPos = golem.blockPosition();
                    if (prevRamble != null && prevRamble.equals(currentPos)) {
                        // Stuck, try random position toward target
                        Vec3 targetVec = Vec3.atCenterOf(task.getPos());
                        Vec3 randomPos = DefaultRandomPos.getPosTowards(golem, 6, 4, targetVec, Math.PI / 4);
                        if (randomPos != null) {
                            golem.getNavigation().moveTo(randomPos.x, randomPos.y, randomPos.z, golem.getGolemMoveSpeed());
                        }
                    } else {
                        moveTo();
                    }
                    prevRamble = currentPos;
                }
            } else {
                // Reached target, complete task
                TaskHandler.completeTask(task, golem);
                
                if (task.isCompleted()) {
                    if (taskCounter >= 0) {
                        taskCounter = 0;
                    }
                    pause = 0;
                } else {
                    pause = 10;
                    taskCounter++;
                }
                taskCounter--;
            }
        }
    }
    
    @Override
    public void stop() {
        Task task = golem.getTask();
        if (task != null) {
            // Show emote if task incomplete
            if (!task.isCompleted() && task.isReserved()) {
                golem.level().broadcastEntityEvent(golem, (byte) 6);
            }
            
            // Mark completed tasks as suspended
            if (task.isCompleted() && !task.isSuspended()) {
                task.setSuspended(true);
            }
            
            task.setReserved(false);
        }
    }
    
    /**
     * Find a task to execute
     */
    protected abstract boolean findDestination();
    
    /**
     * Check if destination is still valid
     */
    protected boolean isValidDestination(Level level, BlockPos pos) {
        return true;
    }
    
    /**
     * Check if golem's traits are valid for the task's seal
     */
    protected boolean areGolemTagsValidForTask(Task task) {
        ISealEntity sealEntity = SealHandler.getSealEntity(golem.level().dimension(), task.getSealPos());
        if (sealEntity == null || sealEntity.getSeal() == null) {
            return true;
        }
        
        // Check locked seal ownership
        if (sealEntity.isLocked() && !golem.getOwnerUUID().toString().equals(sealEntity.getOwner())) {
            return false;
        }
        
        // Check required traits
        EnumGolemTrait[] required = sealEntity.getSeal().getRequiredTags();
        if (required != null && !golem.getProperties().getTraits().containsAll(Arrays.asList(required))) {
            return false;
        }
        
        // Check forbidden traits
        EnumGolemTrait[] forbidden = sealEntity.getSeal().getForbiddenTags();
        if (forbidden != null) {
            for (EnumGolemTrait trait : forbidden) {
                if (golem.getProperties().getTraits().contains(trait)) {
                    return false;
                }
            }
        }
        
        return true;
    }
}




