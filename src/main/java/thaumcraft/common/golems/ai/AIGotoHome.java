package thaumcraft.common.golems.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.golems.EntityThaumcraftGolem;

import java.util.EnumSet;

/**
 * AIGotoHome - AI goal for golems to return to their home position when idle.
 * 
 * When a golem has no active task and is far from home, this goal will
 * guide it back toward its home position. Uses random intermediate
 * positions for very long distances to avoid pathing issues.
 * 
 * Ported from 1.12.2.
 */
public class AIGotoHome extends Goal {
    
    protected EntityThaumcraftGolem golem;
    private double movePosX;
    private double movePosY;
    private double movePosZ;
    protected int idleCounter;
    
    public AIGotoHome(EntityThaumcraftGolem golem) {
        this.golem = golem;
        this.idleCounter = 10;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }
    
    @Override
    public boolean canUse() {
        // Don't run too frequently
        if (idleCounter > 0) {
            --idleCounter;
            return false;
        }
        idleCounter = 50;
        
        // Must have a home position
        if (!golem.hasRestriction()) {
            return false;
        }
        
        BlockPos homePos = golem.getRestrictCenter();
        double distSq = golem.distanceToSqr(Vec3.atCenterOf(homePos));
        
        // Already close enough to home
        if (distSq < 5.0) {
            return false;
        }
        
        // Within reasonable distance - go directly
        if (distSq <= 1024.0) { // 32 blocks
            movePosX = homePos.getX() + 0.5;
            movePosY = homePos.getY();
            movePosZ = homePos.getZ() + 0.5;
            return true;
        }
        
        // Very far away - use random position toward home
        Vec3 homeVec = Vec3.atCenterOf(homePos);
        Vec3 randomPos = DefaultRandomPos.getPosTowards(golem, 16, 7, homeVec, Math.PI / 4);
        
        if (randomPos == null) {
            return false;
        }
        
        movePosX = randomPos.x;
        movePosY = randomPos.y;
        movePosZ = randomPos.z;
        return true;
    }
    
    @Override
    public void start() {
        golem.getNavigation().moveTo(movePosX, movePosY, movePosZ, golem.getGolemMoveSpeed());
    }
    
    @Override
    public boolean canContinueToUse() {
        // Stop if we got a task, finished pathing, or got close enough to home
        if (golem.getTask() != null) {
            return false;
        }
        if (golem.getNavigation().isDone()) {
            return false;
        }
        if (!golem.hasRestriction()) {
            return false;
        }
        
        BlockPos homePos = golem.getRestrictCenter();
        double distSq = golem.distanceToSqr(Vec3.atCenterOf(homePos));
        return distSq > 3.0;
    }
    
    @Override
    public void stop() {
        idleCounter = 50;
        golem.getNavigation().stop();
    }
}




