package thaumcraft.common.golems.ai;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.Node;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.golems.EntityThaumcraftGolem;
import thaumcraft.common.golems.tasks.TaskHandler;

import java.util.List;

/**
 * AIGotoEntity - AI goal for golems to navigate to entity-targeted tasks.
 * 
 * Handles tasks where the target is an entity (e.g., attacking mobs, 
 * following players, interacting with animals).
 * 
 * Ported from 1.12.2.
 */
public class AIGotoEntity extends AIGoto {
    
    public AIGotoEntity(EntityThaumcraftGolem golem) {
        super(golem, (byte) 1);
    }
    
    @Override
    public void tick() {
        super.tick();
        // Look at target entity while moving
        if (golem.getLookControl() != null && golem.getTask() != null && golem.getTask().getEntity() != null) {
            golem.getLookControl().setLookAt(
                golem.getTask().getEntity(), 
                10.0f, 
                (float) golem.getMaxHeadXRot()
            );
        }
    }
    
    @Override
    protected void moveTo() {
        if (golem.getNavigation() != null && golem.getTask() != null && golem.getTask().getEntity() != null) {
            golem.getNavigation().moveTo(golem.getTask().getEntity(), golem.getGolemMoveSpeed());
        }
    }
    
    @Override
    protected boolean findDestination() {
        List<Task> list = TaskHandler.getEntityTasksSorted(golem.level().dimension(), golem.getUUID(), golem);
        
        for (Task task : list) {
            if (task.getEntity() == null || !task.getEntity().isAlive()) {
                continue;
            }
            
            if (areGolemTagsValidForTask(task) && 
                task.canGolemPerformTask(golem) && 
                golem.isWithinRestriction(task.getEntity().blockPosition()) &&
                isValidDestination(golem.level(), task.getEntity().blockPosition()) && 
                canEasilyReach(task.getEntity())) {
                
                golem.setTask(task);
                golem.getTask().setReserved(true);
                
                // Set minimum distance based on entity width
                minDist = 3.5 + task.getEntity().getBbWidth() / 2.0f * (task.getEntity().getBbWidth() / 2.0f);
                
                // Show emote for task acceptance
                golem.level().broadcastEntityEvent(golem, (byte) 5);
                
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if the golem can easily reach the target entity via pathfinding.
     */
    private boolean canEasilyReach(Entity entity) {
        // If already very close, we can reach it
        if (golem.distanceToSqr(entity) < minDist) {
            return true;
        }
        
        // Try to find a path to the entity
        Path path = golem.getNavigation().createPath(entity, 1);
        if (path == null) {
            return false;
        }
        
        Node endNode = path.getEndNode();
        if (endNode == null) {
            return false;
        }
        
        // Check if the path end point is close enough to the entity
        int dx = endNode.x - Mth.floor(entity.getX());
        int dz = endNode.z - Mth.floor(entity.getZ());
        return dx * dx + dz * dz < minDist;
    }
}




