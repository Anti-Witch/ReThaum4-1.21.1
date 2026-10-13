package thaumcraft.common.golems.seals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thaumcraft.api.golems.seals.ISeal;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.seals.SealPos;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.golems.tasks.TaskHandler;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.misc.PacketSealToClient;

import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SealHandler - Manages seal registration, placement, removal, and world ticking.
 * 
 * Ported from 1.12.2. Key changes:
 * - Dimension is now ResourceKey<Level> (stored as String key in map)
 * - EnumFacing -> Direction
 * - world.isRemote -> level.isClientSide
 * - world.isBlockLoaded -> level.isLoaded
 * - Network packets stubbed (TODO: implement when network is ready)
 * - AuraHandler.dirtyChunks integration stubbed (TODO: implement when aura system integrates)
 */
public class SealHandler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SealHandler.class);
    
    // Registered seal types by key
    public static LinkedHashMap<String, ISeal> types = new LinkedHashMap<>();
    
    // Map of dimension -> (SealPos -> SealEntity)
    public static ConcurrentHashMap<String, ConcurrentHashMap<SealPos, SealEntity>> sealEntities = new ConcurrentHashMap<>();
    
    // Tick counter for periodic checks
    private static int tickCount = 0;
    
    /**
     * Get the string key for a dimension
     */
    private static String getDimKey(ResourceKey<Level> dim) {
        return dim.location().toString();
    }
    
    // ==================== Seal Type Registration ====================
    
    /**
     * Register all default Thaumcraft seals.
     * Called during mod initialization.
     */
    public static void registerDefaultSeals() {
        LOGGER.info("Registering Thaumcraft seals...");
        
        // Item transport seals
        registerSeal(new SealPickup());
        registerSeal(new SealEmpty());
        registerSeal(new SealFill());
        registerSeal(new SealProvide());
        
        // Combat seals
        registerSeal(new SealGuard());
        registerSeal(new SealButcher());
        
        // Work seals
        registerSeal(new SealHarvest());
        registerSeal(new SealLumber());
        registerSeal(new SealBreaker());
        registerSeal(new SealBreakerAdvanced());
        
        // Advanced item seals
        registerSeal(new SealPickupAdvanced());
        registerSeal(new SealEmptyAdvanced());
        registerSeal(new SealFillAdvanced());
        registerSeal(new SealGuardAdvanced());
        registerSeal(new SealStock());
        
        // Interaction seals
        registerSeal(new SealUse());
        
        LOGGER.info("Registered {} seals", types.size());
    }
    
    /**
     * Register a seal type. Called during mod init.
     */
    public static void registerSeal(ISeal seal) {
        if (types.containsKey(seal.getKey())) {
            LOGGER.error("Attempting to register Seal [{}] twice. Ignoring.", seal.getKey());
        } else {
            types.put(seal.getKey(), seal);
        }
    }
    
    /**
     * Get all registered seal keys
     */
    public static String[] getRegisteredSeals() {
        return types.keySet().toArray(new String[0]);
    }
    
    /**
     * Get a seal template by key
     */
    public static ISeal getSeal(String key) {
        return types.get(key);
    }
    
    // ==================== Seal Entity Management ====================
    
    /**
     * Get all seals in range of a position
     */
    public static CopyOnWriteArrayList<SealEntity> getSealsInRange(Level level, BlockPos source, int range) {
        CopyOnWriteArrayList<SealEntity> out = new CopyOnWriteArrayList<>();
        ConcurrentHashMap<SealPos, SealEntity> dimSeals = sealEntities.get(getDimKey(level.dimension()));
        
        if (dimSeals != null && !dimSeals.isEmpty()) {
            int rangeSq = range * range;
            for (SealEntity se : dimSeals.values()) {
                if (se.getSeal() != null && se.getSealPos() != null) {
                    if (se.sealPos.pos.distSqr(source) <= rangeSq) {
                        out.add(se);
                    }
                }
            }
        }
        
        return out;
    }
    
    /**
     * Get all seals in a specific chunk
     */
    public static CopyOnWriteArrayList<SealEntity> getSealsInChunk(Level level, ChunkPos chunk) {
        CopyOnWriteArrayList<SealEntity> out = new CopyOnWriteArrayList<>();
        ConcurrentHashMap<SealPos, SealEntity> dimSeals = sealEntities.get(getDimKey(level.dimension()));
        
        if (dimSeals != null && !dimSeals.isEmpty()) {
            for (SealEntity se : dimSeals.values()) {
                if (se.getSeal() != null && se.getSealPos() != null) {
                    ChunkPos sealChunk = new ChunkPos(se.sealPos.pos);
                    if (sealChunk.equals(chunk)) {
                        out.add(se);
                    }
                }
            }
        }
        
        return out;
    }
    
    /**
     * Get a specific seal entity by dimension and position
     */
    public static ISealEntity getSealEntity(ResourceKey<Level> dim, SealPos pos) {
        if (pos == null) return null;
        
        String dimKey = getDimKey(dim);
        sealEntities.computeIfAbsent(dimKey, k -> new ConcurrentHashMap<>());
        
        return sealEntities.get(dimKey).get(pos);
    }
    
    /**
     * Add a new seal to the world
     */
    public static boolean addSealEntity(Level level, BlockPos pos, Direction face, ISeal seal, Player player) {
        String dimKey = getDimKey(level.dimension());
        sealEntities.computeIfAbsent(dimKey, k -> new ConcurrentHashMap<>());
        
        ConcurrentHashMap<SealPos, SealEntity> dimSeals = sealEntities.get(dimKey);
        SealPos sealPos = new SealPos(pos, face);
        
        if (dimSeals.containsKey(sealPos)) {
            return false; // Seal already exists at this position
        }
        
        SealEntity sealEntity = new SealEntity(level, sealPos, seal);
        sealEntity.setOwner(player.getUUID().toString());
        dimSeals.put(sealPos, sealEntity);
        
        if (!level.isClientSide) {
            sealEntity.syncToClient(level);
            markChunkAsDirty(level.dimension(), pos);
        }
        
        return true;
    }
    
    /**
     * Add an existing seal entity (e.g., loaded from NBT)
     */
    public static boolean addSealEntity(Level level, SealEntity seal) {
        if (level == null || seal == null) return false;
        
        String dimKey = getDimKey(level.dimension());
        sealEntities.computeIfAbsent(dimKey, k -> new ConcurrentHashMap<>());
        
        ConcurrentHashMap<SealPos, SealEntity> dimSeals = sealEntities.get(dimKey);
        
        if (dimSeals.containsKey(seal.getSealPos())) {
            return false;
        }
        
        dimSeals.put(seal.getSealPos(), seal);
        
        if (!level.isClientSide) {
            seal.syncToClient(level);
            markChunkAsDirty(level.dimension(), seal.getSealPos().pos);
        }
        
        return true;
    }
    
    /**
     * Remove a seal from the world
     * @param quiet If false, drops the seal item
     */
    public static void removeSealEntity(Level level, SealPos pos, boolean quiet) {
        String dimKey = getDimKey(level.dimension());
        sealEntities.computeIfAbsent(dimKey, k -> new ConcurrentHashMap<>());
        
        ConcurrentHashMap<SealPos, SealEntity> dimSeals = sealEntities.get(dimKey);
        SealEntity seal = dimSeals.remove(pos);
        
        try {
            if (!level.isClientSide && seal != null && seal.seal != null) {
                seal.seal.onRemoval(level, pos.pos, pos.face);
            }
            
            if (!quiet && seal != null && seal.getSeal() != null && !level.isClientSide) {
                // Drop seal item - TC4 has no seal items, so nothing is dropped
                ItemStack sealStack = ItemStack.EMPTY;
                if (!sealStack.isEmpty()) {
                    double x = pos.pos.getX() + 0.5 + pos.face.getStepX() / 1.7;
                    double y = pos.pos.getY() + 0.5 + pos.face.getStepY() / 1.7;
                    double z = pos.pos.getZ() + 0.5 + pos.face.getStepZ() / 1.7;
                    level.addFreshEntity(new ItemEntity(level, x, y, z, sealStack));
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Removing invalid seal at {}", pos.pos);
        }
        
        // Suspend all tasks from this seal
        ConcurrentHashMap<Integer, Task> tasks = TaskHandler.getTasks(level.dimension());
        for (Task task : tasks.values()) {
            if (task.getSealPos() != null && task.getSealPos().equals(pos)) {
                task.setSuspended(true);
            }
        }
        
        if (!level.isClientSide) {
            // Send removal packet to all clients in dimension
            SealEntity removedSeal = new SealEntity(level, pos, null);
            PacketHandler.sendToDimension(new PacketSealToClient(removedSeal), level.dimension());
        }
        
        if (!quiet) {
            markChunkAsDirty(level.dimension(), pos.pos);
        }
    }
    
    // ==================== World Ticking ====================
    
    /**
     * Tick all seal entities in the world
     * Called from world tick event
     */
    public static void tickSealEntities(Level level) {
        String dimKey = getDimKey(level.dimension());
        sealEntities.computeIfAbsent(dimKey, k -> new ConcurrentHashMap<>());
        
        ConcurrentHashMap<SealPos, SealEntity> dimSeals = sealEntities.get(dimKey);
        tickCount++;
        
        for (SealEntity sealEntity : dimSeals.values()) {
            // Only tick loaded chunks
            if (level.isLoaded(sealEntity.sealPos.pos)) {
                try {
                    boolean shouldTick = true;
                    
                    // Periodic check if seal can still exist at position
                    if (tickCount % 20 == 0 && !sealEntity.seal.canPlaceAt(level, sealEntity.sealPos.pos, sealEntity.sealPos.face)) {
                        removeSealEntity(level, sealEntity.sealPos, false);
                        shouldTick = false;
                    }
                    
                    if (shouldTick) {
                        sealEntity.tickSealEntity(level);
                    }
                } catch (Exception e) {
                    LOGGER.error("Error ticking seal at {}, removing", sealEntity.sealPos.pos, e);
                    removeSealEntity(level, sealEntity.sealPos, false);
                }
            }
        }
    }
    
    // ==================== Chunk Dirty Marking ====================
    
    /**
     * Mark a chunk as dirty for saving
     * TODO: Integrate with AuraHandler when ready
     */
    public static void markChunkAsDirty(ResourceKey<Level> dim, BlockPos pos) {
        // TODO: When AuraHandler is implemented:
        // ChunkPos chunkPos = new ChunkPos(pos);
        // AuraHandler.markChunkDirty(dim, chunkPos);
    }
    
    // ==================== Player Sync ====================
    
    /**
     * Sync all seals in a dimension to a specific player.
     * Called when player logs in or changes dimension.
     * 
     * @param player The player to sync seals to
     */
    public static void syncAllSealsToPlayer(ServerPlayer player) {
        ResourceKey<Level> dim = player.level().dimension();
        String dimKey = getDimKey(dim);
        ConcurrentHashMap<SealPos, SealEntity> dimSeals = sealEntities.get(dimKey);
        
        if (dimSeals == null || dimSeals.isEmpty()) {
            return;
        }
        
        LOGGER.debug("Syncing {} seals to player {}", dimSeals.size(), player.getName().getString());
        
        for (SealEntity sealEntity : dimSeals.values()) {
            if (sealEntity.getSeal() != null) {
                PacketHandler.sendToPlayer(new PacketSealToClient(sealEntity), player);
            }
        }
    }
    
    /**
     * Sync all seals in range of a position to a player.
     * Useful for loading seals in specific areas.
     * 
     * @param player The player to sync seals to
     * @param center The center position
     * @param range The range in blocks
     */
    public static void syncSealsInRangeToPlayer(ServerPlayer player, BlockPos center, int range) {
        CopyOnWriteArrayList<SealEntity> seals = getSealsInRange(player.level(), center, range);
        
        for (SealEntity sealEntity : seals) {
            if (sealEntity.getSeal() != null) {
                PacketHandler.sendToPlayer(new PacketSealToClient(sealEntity), player);
            }
        }
    }
    
    // ==================== Cleanup ====================
    
    /**
     * Clear all seals for a dimension (e.g., when unloading)
     */
    public static void clearDimension(ResourceKey<Level> dim) {
        sealEntities.remove(getDimKey(dim));
    }
}




