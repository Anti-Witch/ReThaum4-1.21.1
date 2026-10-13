package thaumcraft.common.world.aura;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages AuraThread instances for all loaded dimensions.
 * 
 * This class handles:
 * - Starting aura threads when dimensions are loaded
 * - Stopping aura threads when dimensions are unloaded
 * - Stopping all threads on server shutdown
 */
public class AuraThreadManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuraThreadManager.class);

    /** Map of dimension to aura thread */
    private static final Map<ResourceKey<Level>, AuraThread> auraThreads = new ConcurrentHashMap<>();
    
    /** Map of dimension to actual Thread objects */
    private static final Map<ResourceKey<Level>, Thread> threads = new ConcurrentHashMap<>();

    /**
     * Dimensions whose aura world has already been initialized.
     * The legacy background thread is not used anymore (main-thread scheduler),
     * so this set makes {@link #startThread(ResourceKey)} idempotent - otherwise
     * the per-tick guard in ServerEvents would re-initialize every tick.
     */
    private static final java.util.Set<ResourceKey<Level>> handledDimensions = ConcurrentHashMap.newKeySet();

    /**
     * Start an aura thread for a dimension if one doesn't exist.
     * Called when a dimension is loaded and has aura data.
     */
    public static void startThread(ResourceKey<Level> dimension) {
        // NOTE (1.21.1 hardening): aura simulation runs exclusively on the main
        // thread via AuraScheduler (LevelTickEvent). The legacy background
        // AuraThread is intentionally NOT started: mutating AuraChunk.vis/flux
        // from a separate daemon thread is unsafe in NeoForge 1.21.1 (torn reads
        // during ChunkDataEvent.Save, race with the main-thread scheduler) and
        // would also double-process regen/flux. Keep the aura world alive so the
        // main-thread scheduler still has data to process.
        AuraHandler.addAuraWorld(dimension);
        if (handledDimensions.add(dimension)) {
            LOGGER.debug("Aura simulation handled on main thread for {}", dimension.location());
        }
    }

    /**
     * Stop the aura thread for a dimension.
     * Called when a dimension is unloaded.
     */
    public static void stopThread(ResourceKey<Level> dimension) {
        AuraThread auraThread = auraThreads.get(dimension);
        if (auraThread != null) {
            LOGGER.info("Stopping aura thread for dimension {}", dimension.location());
            auraThread.stop();
        }
        
        Thread thread = threads.get(dimension);
        if (thread != null) {
            try {
                thread.interrupt();
                thread.join(2000); // Wait up to 2 seconds for thread to finish
            } catch (InterruptedException e) {
                LOGGER.warn("Interrupted while waiting for aura thread to stop");
            }
        }
        
        removeThread(dimension);
    }

    /**
     * Remove a thread from the registry without stopping it.
     * Called by the thread itself when it stops.
     */
    public static void removeThread(ResourceKey<Level> dimension) {
        auraThreads.remove(dimension);
        threads.remove(dimension);
        handledDimensions.remove(dimension);
    }

    /**
     * Stop all aura threads.
     * Called on server shutdown.
     */
    public static void stopAllThreads() {
        LOGGER.info("Stopping all aura threads ({} active)", auraThreads.size());
        
        // Stop all threads
        for (AuraThread auraThread : auraThreads.values()) {
            auraThread.stop();
        }
        
        // Interrupt and wait for all threads
        for (Thread thread : threads.values()) {
            try {
                thread.interrupt();
            } catch (Exception e) {
                // Ignore
            }
        }
        
        for (Map.Entry<ResourceKey<Level>, Thread> entry : threads.entrySet()) {
            try {
                entry.getValue().join(1000);
            } catch (InterruptedException e) {
                LOGGER.warn("Interrupted while waiting for aura thread {} to stop", entry.getKey().location());
            }
        }
        
        auraThreads.clear();
        threads.clear();
        handledDimensions.clear();
    }

    /**
     * Check if a dimension has an active aura thread.
     */
    public static boolean hasThread(ResourceKey<Level> dimension) {
        if (handledDimensions.contains(dimension)) {
            return true;
        }
        AuraThread thread = auraThreads.get(dimension);
        return thread != null && !thread.isStopped();
    }

    /**
     * Get the aura thread for a dimension.
     */
    public static AuraThread getThread(ResourceKey<Level> dimension) {
        return auraThreads.get(dimension);
    }

    /**
     * Called when a level is loaded.
     * Starts aura thread if aura data exists.
     */
    public static void onLevelLoad(ServerLevel level) {
        ResourceKey<Level> dimension = level.dimension();
        
        // Ensure aura world exists
        AuraHandler.addAuraWorld(dimension);
        
        // Start aura thread
        startThread(dimension);
    }

    /**
     * Called when a level is unloaded.
     * Stops the aura thread for that dimension.
     */
    public static void onLevelUnload(ServerLevel level) {
        ResourceKey<Level> dimension = level.dimension();
        stopThread(dimension);
        AuraHandler.removeAuraWorld(dimension);
    }
}




