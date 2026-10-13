package thaumcraft.common.golems.tasks;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.golems.EntityThaumcraftGolem;
import thaumcraft.common.golems.seals.SealHandler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TaskHandler - Manages per-dimension task queues for golems.
 * Tasks are created by seals and claimed by golems to perform work.
 * 
 * Ported from 1.12.2. Key changes:
 * - Dimension is now ResourceKey<Level> instead of int
 * - Uses String dimension key for map storage
 */
public class TaskHandler {
    
    private static final int TASK_LIMIT = 10000;
    
    // Map of dimension -> (task ID -> task)
    // Uses dimension path string as key since ResourceKey doesn't have stable hashCode for ConcurrentHashMap
    public static ConcurrentHashMap<String, ConcurrentHashMap<Integer, Task>> tasks = new ConcurrentHashMap<>();
    
    /**
     * Get the string key for a dimension
     */
    private static String getDimKey(ResourceKey<Level> dim) {
        return dim.location().toString();
    }
    
    /**
     * Add a task to the dimension's task queue
     */
    public static void addTask(ResourceKey<Level> dim, Task task) {
        String dimKey = getDimKey(dim);
        tasks.computeIfAbsent(dimKey, k -> new ConcurrentHashMap<>());
        
        ConcurrentHashMap<Integer, Task> dimTasks = tasks.get(dimKey);
        
        // Evict oldest task if over limit
        if (dimTasks.size() > TASK_LIMIT) {
            try {
                Iterator<Task> iter = dimTasks.values().iterator();
                if (iter.hasNext()) {
                    iter.next();
                    iter.remove();
                }
            } catch (Exception ignored) {}
        }
        
        dimTasks.put(task.getId(), task);
    }
    
    /**
     * Get a specific task by ID
     */
    public static Task getTask(ResourceKey<Level> dim, int id) {
        return getTasks(dim).get(id);
    }
    
    /**
     * Get all tasks for a dimension
     */
    public static ConcurrentHashMap<Integer, Task> getTasks(ResourceKey<Level> dim) {
        String dimKey = getDimKey(dim);
        return tasks.computeIfAbsent(dimKey, k -> new ConcurrentHashMap<>());
    }
    
    /**
     * Get block-targeted tasks sorted by distance and priority.
     * Tasks closer to the golem and with higher priority come first.
     * 
     * @param dim The dimension
     * @param golemUUID Optional UUID filter - if not null, only tasks assigned to this golem or unassigned
     * @param golem The golem entity (for distance calculation)
     * @return Sorted list of available tasks
     */
    public static ArrayList<Task> getBlockTasksSorted(ResourceKey<Level> dim, UUID golemUUID, Entity golem) {
        ConcurrentHashMap<Integer, Task> dimTasks = getTasks(dim);
        ArrayList<Task> out = new ArrayList<>();
        
        taskLoop:
        for (Task task : dimTasks.values()) {
            // Skip reserved tasks
            if (task.isReserved()) continue;
            
            // Only block tasks (type 0)
            if (task.getType() != 0) continue;
            
            // Check golem UUID filter
            if (golemUUID != null && task.getGolemUUID() != null && !golemUUID.equals(task.getGolemUUID())) {
                continue;
            }
            
            // Insert sorted by adjusted distance (distance - priority bonus)
            if (out.isEmpty()) {
                out.add(task);
            } else {
                double d = task.getPos().distToCenterSqr(golem.getX(), golem.getY(), golem.getZ());
                d -= task.getPriority() * 256; // Priority bonus
                
                for (int i = 0; i < out.size(); i++) {
                    double d2 = out.get(i).getPos().distToCenterSqr(golem.getX(), golem.getY(), golem.getZ());
                    d2 -= out.get(i).getPriority() * 256;
                    
                    if (d < d2) {
                        out.add(i, task);
                        continue taskLoop;
                    }
                }
                out.add(task);
            }
        }
        
        return out;
    }
    
    /**
     * Get entity-targeted tasks sorted by distance and priority.
     * Tasks targeting dead entities are automatically suspended.
     * 
     * @param dim The dimension
     * @param golemUUID Optional UUID filter
     * @param golem The golem entity
     * @return Sorted list of available entity tasks
     */
    public static ArrayList<Task> getEntityTasksSorted(ResourceKey<Level> dim, UUID golemUUID, Entity golem) {
        ConcurrentHashMap<Integer, Task> dimTasks = getTasks(dim);
        ArrayList<Task> out = new ArrayList<>();
        
        taskLoop:
        for (Task task : dimTasks.values()) {
            // Skip reserved tasks
            if (task.isReserved()) continue;
            
            // Only entity tasks (type 1)
            if (task.getType() != 1) continue;
            
            // Check golem UUID filter
            if (golemUUID != null && task.getGolemUUID() != null && !golemUUID.equals(task.getGolemUUID())) {
                continue;
            }
            
            // Check if target entity is still valid
            if (task.getEntity() == null || !task.getEntity().isAlive()) {
                task.setSuspended(true);
                continue;
            }
            
            // Insert sorted by adjusted distance
            if (out.isEmpty()) {
                out.add(task);
            } else {
                double d = task.getPos().distToCenterSqr(golem.getX(), golem.getY(), golem.getZ());
                d -= task.getPriority() * 256;
                
                for (int i = 0; i < out.size(); i++) {
                    double d2 = out.get(i).getPos().distToCenterSqr(golem.getX(), golem.getY(), golem.getZ());
                    d2 -= out.get(i).getPriority() * 256;
                    
                    if (d < d2) {
                        out.add(i, task);
                        continue taskLoop;
                    }
                }
                out.add(task);
            }
        }
        
        return out;
    }
    
    /**
     * Complete a task and notify the seal
     */
    public static void completeTask(Task task, EntityThaumcraftGolem golem) {
        if (task.isCompleted() || task.isSuspended()) {
            return;
        }
        
        ISealEntity sealEntity = SealHandler.getSealEntity(golem.level().dimension(), task.getSealPos());
        if (sealEntity != null) {
            boolean completed = sealEntity.getSeal().onTaskCompletion(golem.level(), golem, task);
            task.setCompletion(completed);
        } else {
            task.setCompletion(true);
        }
    }
    
    /**
     * Clear suspended or expired tasks from the world's dimension.
     * Called periodically during world tick.
     */
    public static void clearSuspendedOrExpiredTasks(Level level) {
        ResourceKey<Level> dim = level.dimension();
        ConcurrentHashMap<Integer, Task> dimTasks = getTasks(dim);
        ConcurrentHashMap<Integer, Task> remaining = new ConcurrentHashMap<>();
        
        for (Task task : dimTasks.values()) {
            if (!task.isSuspended() && task.getLifespan() > 0) {
                // Decrement lifespan
                task.setLifespan((short) (task.getLifespan() - 1));
                remaining.put(task.getId(), task);
            } else {
                // Notify seal of task suspension
                ISealEntity sealEntity = SealHandler.getSealEntity(dim, task.getSealPos());
                if (sealEntity != null) {
                    sealEntity.getSeal().onTaskSuspension(level, task);
                }
            }
        }
        
        // Replace task map with remaining tasks
        tasks.put(getDimKey(dim), remaining);
    }
    
    /**
     * Remove all tasks for a dimension (e.g., when unloading)
     */
    public static void clearDimension(ResourceKey<Level> dim) {
        tasks.remove(getDimKey(dim));
    }
    
    /**
     * Remove a specific task
     */
    public static void removeTask(ResourceKey<Level> dim, int taskId) {
        getTasks(dim).remove(taskId);
    }
}




