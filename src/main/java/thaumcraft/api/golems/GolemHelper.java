package thaumcraft.api.golems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.golems.seals.ISeal;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.seals.SealPos;
import thaumcraft.api.golems.tasks.Task;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helper class for golem-related operations.
 * Provides seal registration, task management, and provisioning requests.
 */
public class GolemHelper {

    // Seal registry
    private static final Map<String, ISeal> sealRegistry = new ConcurrentHashMap<>();
    
    // Seal entity tracking by dimension
    private static final Map<ResourceKey<Level>, Map<SealPos, ISealEntity>> sealEntities = new ConcurrentHashMap<>();
    
    // Task management by dimension
    private static final Map<ResourceKey<Level>, Map<Integer, Task>> tasks = new ConcurrentHashMap<>();
    
    // Provision requests by dimension
    public static final Map<ResourceKey<Level>, ArrayList<ProvisionRequest>> provisionRequests = new HashMap<>();
    
    private static final int LIST_LIMIT = 1000;

    // ==================== Seal Registration ====================

    /**
     * Register a seal type. Call during mod initialization.
     */
    public static void registerSeal(ISeal seal) {
        sealRegistry.put(seal.getKey(), seal);
    }

    /**
     * Get a registered seal by key
     */
    @Nullable
    public static ISeal getSeal(String key) {
        return sealRegistry.get(key);
    }

    /**
     * Get all registered seal keys
     */
    public static Iterable<String> getAllSealKeys() {
        return sealRegistry.keySet();
    }

    // ==================== Seal Entity Management ====================

    /**
     * Get a seal entity at a specific position
     */
    @Nullable
    public static ISealEntity getSealEntity(ResourceKey<Level> dimension, SealPos pos) {
        Map<SealPos, ISealEntity> dimSeals = sealEntities.get(dimension);
        return dimSeals != null ? dimSeals.get(pos) : null;
    }

    /**
     * Add a seal entity to tracking
     */
    public static void addSealEntity(ResourceKey<Level> dimension, ISealEntity entity) {
        sealEntities.computeIfAbsent(dimension, k -> new ConcurrentHashMap<>())
                   .put(entity.getSealPos(), entity);
    }

    /**
     * Remove a seal entity from tracking
     */
    public static void removeSealEntity(ResourceKey<Level> dimension, SealPos pos) {
        Map<SealPos, ISealEntity> dimSeals = sealEntities.get(dimension);
        if (dimSeals != null) {
            dimSeals.remove(pos);
        }
    }

    /**
     * Get all seal entities in a dimension
     */
    public static Map<SealPos, ISealEntity> getSealEntities(ResourceKey<Level> dimension) {
        return sealEntities.computeIfAbsent(dimension, k -> new ConcurrentHashMap<>());
    }

    // ==================== Task Management ====================

    /**
     * Add a task for golems to execute
     */
    public static void addGolemTask(ResourceKey<Level> dimension, Task task) {
        Map<Integer, Task> dimTasks = tasks.computeIfAbsent(dimension, k -> new ConcurrentHashMap<>());
        if (dimTasks.size() > 10000) {
            // Remove oldest task to prevent overflow
            dimTasks.values().stream().findFirst().ifPresent(t -> dimTasks.remove(t.getId()));
        }
        dimTasks.put(task.getId(), task);
    }

    /**
     * Get a specific task by ID
     */
    @Nullable
    public static Task getTask(ResourceKey<Level> dimension, int taskId) {
        Map<Integer, Task> dimTasks = tasks.get(dimension);
        return dimTasks != null ? dimTasks.get(taskId) : null;
    }

    /**
     * Get all tasks in a dimension
     */
    public static Map<Integer, Task> getTasks(ResourceKey<Level> dimension) {
        return tasks.computeIfAbsent(dimension, k -> new ConcurrentHashMap<>());
    }

    /**
     * Remove a task
     */
    public static void removeTask(ResourceKey<Level> dimension, int taskId) {
        Map<Integer, Task> dimTasks = tasks.get(dimension);
        if (dimTasks != null) {
            dimTasks.remove(taskId);
        }
    }

    // ==================== Provisioning ====================

    /**
     * Request an item to be provisioned to a seal
     */
    public static void requestProvisioning(Level level, ISealEntity seal, ItemStack stack) {
        ResourceKey<Level> dim = level.dimension();
        ArrayList<ProvisionRequest> list = provisionRequests.computeIfAbsent(dim, k -> new ArrayList<>());
        ProvisionRequest pr = new ProvisionRequest(seal, stack.copy());
        if (!list.contains(pr)) {
            list.add(pr);
        }
        if (list.size() > LIST_LIMIT) list.remove(0);
    }

    /**
     * Request an item to be provisioned to a block position
     */
    public static void requestProvisioning(Level level, BlockPos pos, Direction side, ItemStack stack) {
        ResourceKey<Level> dim = level.dimension();
        ArrayList<ProvisionRequest> list = provisionRequests.computeIfAbsent(dim, k -> new ArrayList<>());
        ProvisionRequest pr = new ProvisionRequest(pos, side, stack.copy());
        if (!list.contains(pr)) {
            list.add(pr);
        }
        if (list.size() > LIST_LIMIT) list.remove(0);
    }

    /**
     * Request an item to be provisioned to an entity
     */
    public static void requestProvisioning(Level level, Entity entity, ItemStack stack) {
        ResourceKey<Level> dim = level.dimension();
        ArrayList<ProvisionRequest> list = provisionRequests.computeIfAbsent(dim, k -> new ArrayList<>());
        ProvisionRequest pr = new ProvisionRequest(entity, stack.copy());
        if (!list.contains(pr)) {
            list.add(pr);
        }
        if (list.size() > LIST_LIMIT) list.remove(0);
    }

    /**
     * Request an item to be provisioned to a block position with unique identifier
     */
    public static void requestProvisioning(Level level, BlockPos pos, Direction side, ItemStack stack, int ui) {
        ResourceKey<Level> dim = level.dimension();
        ArrayList<ProvisionRequest> list = provisionRequests.computeIfAbsent(dim, k -> new ArrayList<>());
        ProvisionRequest pr = new ProvisionRequest(pos, side, stack.copy());
        pr.setUI(ui);
        if (!list.contains(pr)) {
            list.add(pr);
        }
        if (list.size() > LIST_LIMIT) list.remove(0);
    }

    /**
     * Request an item to be provisioned to an entity with unique identifier
     */
    public static void requestProvisioning(Level level, Entity entity, ItemStack stack, int ui) {
        ResourceKey<Level> dim = level.dimension();
        ArrayList<ProvisionRequest> list = provisionRequests.computeIfAbsent(dim, k -> new ArrayList<>());
        ProvisionRequest pr = new ProvisionRequest(entity, stack.copy());
        pr.setUI(ui);
        if (!list.contains(pr)) {
            list.add(pr);
        }
        if (list.size() > LIST_LIMIT) list.remove(0);
    }

    // ==================== Area Utilities ====================

    /**
     * Get a single block position within a seal's designated area.
     * Useful for incrementing through blocks in the area.
     * @param seal The seal entity
     * @param count A value used to derive a specific position
     * @return A BlockPos within the area
     */
    public static BlockPos getPosInArea(ISealEntity seal, int count) {
        Direction face = seal.getSealPos().face;
        BlockPos area = seal.getArea();
        
        int xx = 1 + (area.getX() - 1) * (face.getStepX() == 0 ? 2 : 1);
        int yy = 1 + (area.getY() - 1) * (face.getStepY() == 0 ? 2 : 1);
        int zz = 1 + (area.getZ() - 1) * (face.getStepZ() == 0 ? 2 : 1);

        int qx = face.getStepX() != 0 ? face.getStepX() : 1;
        int qy = face.getStepY() != 0 ? face.getStepY() : 1;
        int qz = face.getStepZ() != 0 ? face.getStepZ() : 1;

        int y = qy * ((count / zz) / xx) % yy + face.getStepY();
        int x = qx * (count / zz) % xx + face.getStepX();
        int z = qz * count % zz + face.getStepZ();

        return seal.getSealPos().pos.offset(
                x - (face.getStepX() == 0 ? xx / 2 : 0),
                y - (face.getStepY() == 0 ? yy / 2 : 0),
                z - (face.getStepZ() == 0 ? zz / 2 : 0));
    }

    /**
     * Returns the designated seal area as an AABB
     */
    public static AABB getBoundsForArea(ISealEntity seal) {
        SealPos sealPos = seal.getSealPos();
        BlockPos pos = sealPos.pos;
        Direction face = sealPos.face;
        BlockPos area = seal.getArea();

        return new AABB(pos.getX(), pos.getY(), pos.getZ(),
                       pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1)
                .move(face.getStepX(), face.getStepY(), face.getStepZ())
                .expandTowards(
                        face.getStepX() != 0 ? (area.getX() - 1) * face.getStepX() : 0,
                        face.getStepY() != 0 ? (area.getY() - 1) * face.getStepY() : 0,
                        face.getStepZ() != 0 ? (area.getZ() - 1) * face.getStepZ() : 0)
                .inflate(
                        face.getStepX() == 0 ? area.getX() - 1 : 0,
                        face.getStepY() == 0 ? area.getY() - 1 : 0,
                        face.getStepZ() == 0 ? area.getZ() - 1 : 0);
    }
}




