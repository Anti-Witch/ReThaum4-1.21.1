package thaumcraft.common.world.aura;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Background thread that processes aura simulation for a dimension.
 * 
 * This thread runs once per second and handles:
 * - Vis regeneration based on moon phase
 * - Vis equalization between adjacent chunks
 * - Flux spreading between chunks
 * - Flux rift trigger conditions
 * 
 * Ported from Thaumcraft 1.12.2 to 1.20.1.
 */
public class AuraThread implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuraThread.class);

    /** The dimension this thread manages */
    private final ResourceKey<Level> dimension;
    
    /** Interval between processing cycles in milliseconds */
    private static final long INTERVAL = 1000L;
    
    /** Flag to stop the thread */
    private volatile boolean stop = false;
    
    /** Random number generator for this thread */
    private final Random rand;
    
    /** Moon phase modifiers for vis regeneration */
    private float phaseVis = 0.0f;
    
    /** Moon phase modifiers for flux generation */
    private float phaseFlux = 0.0f;
    
    /** Moon phase modifiers for maximum aura */
    private float phaseMax = 0.0f;
    
    /** Last recorded world time for phase calculation */
    private long lastWorldTime = 0L;
    
    /**
     * Moon phase table for vis regeneration rate.
     * Index corresponds to moon phase (0-7).
     * Full moon = high vis regen, new moon = low vis regen.
     */
    private static final float[] PHASE_VIS_TABLE = { 0.25f, 0.15f, 0.1f, 0.05f, 0.0f, 0.05f, 0.1f, 0.15f };
    
    /**
     * Moon phase table for maximum aura modifier.
     * Index corresponds to moon phase (0-7).
     */
    private static final float[] PHASE_MAX_TABLE = { 0.15f, 0.05f, 0.0f, -0.05f, -0.15f, -0.05f, 0.0f, 0.05f };

    public AuraThread(ResourceKey<Level> dimension) {
        this.dimension = dimension;
        this.rand = new Random(System.currentTimeMillis());
    }

    @Override
    public void run() {
        LOGGER.info("Starting aura thread for dimension {}", dimension.location());
        
        while (!stop) {
            // Check if aura system has any data
            if (AuraHandler.getAuraWorld(dimension) == null) {
                LOGGER.warn("No aura world found for dimension {}!", dimension.location());
                break;
            }
            
            long startTime = System.currentTimeMillis();
            
            try {
                processAuras();
            } catch (Exception e) {
                LOGGER.error("Error processing auras in dimension {}", dimension.location(), e);
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Log warning if processing takes too long
            if (executionTime > INTERVAL) {
                LOGGER.warn("Aura processing took {}ms longer than normal in dimension {}", 
                    executionTime - INTERVAL, dimension.location());
            }
            
            // Sleep for remainder of interval
            try {
                Thread.sleep(Math.max(1L, INTERVAL - executionTime));
            } catch (InterruptedException e) {
                // Thread interrupted, likely shutting down
                break;
            }
        }
        
        LOGGER.info("Stopping aura thread for dimension {}", dimension.location());
        
        // Clean up from thread registry
        try {
            AuraThreadManager.removeThread(dimension);
        } catch (Exception e) {
            LOGGER.error("Error removing aura thread for dimension {}", dimension.location(), e);
        }
    }

    /**
     * Main processing method - called once per second.
     */
    private void processAuras() {
        AuraWorld auraWorld = AuraHandler.getAuraWorld(dimension);
        if (auraWorld == null) {
            stop();
            return;
        }
        
        // Get the server and world
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        
        ServerLevel level = server.getLevel(dimension);
        if (level == null) {
            return;
        }
        
        // Update moon phase modifiers if world time changed
        long worldTime = level.getDayTime();
        if (lastWorldTime != worldTime) {
            lastWorldTime = worldTime;
            updateMoonPhaseModifiers(level);
            
            // Process all loaded aura chunks
            for (AuraChunk auraChunk : auraWorld.getAuraChunks().values()) {
                processAuraChunk(auraWorld, auraChunk, level);
            }
        }
    }

    /**
     * Updates the moon phase modifiers based on current world time.
     */
    private void updateMoonPhaseModifiers(ServerLevel level) {
        int moonPhase = level.getMoonPhase();
        phaseVis = PHASE_VIS_TABLE[moonPhase];
        phaseMax = 1.0f + PHASE_MAX_TABLE[moonPhase];
        phaseFlux = 0.25f - phaseVis;
    }

    /**
     * Process a single aura chunk.
     * Handles vis equalization, flux spreading, and regeneration.
     */
    private void processAuraChunk(AuraWorld auraWorld, AuraChunk auraChunk, ServerLevel level) {
        // Randomize direction order for fairness
        List<Integer> directions = Arrays.asList(0, 1, 2, 3);
        Collections.shuffle(directions, rand);
        
        ChunkPos loc = auraChunk.getLoc();
        if (loc == null) return;
        
        int x = loc.x;
        int z = loc.z;
        
        float base = auraChunk.getBase() * phaseMax;
        boolean dirty = false;
        
        float currentVis = auraChunk.getVis();
        float currentFlux = auraChunk.getFlux();
        
        // Find neighbors for equalization
        AuraChunk neighbourVisChunk = null;
        AuraChunk neighbourFluxChunk = null;
        float lowestVis = Float.MAX_VALUE;
        float lowestFlux = Float.MAX_VALUE;
        
        for (Integer dirIndex : directions) {
            Direction dir = Direction.from2DDataValue(dirIndex);
            AuraChunk neighbor = auraWorld.getAuraChunkAt(x + dir.getStepX(), z + dir.getStepZ());
            
            if (neighbor != null) {
                float neighborBase = neighbor.getBase() * phaseMax;
                
                // Find lowest vis neighbor that has room for more
                if ((neighbourVisChunk == null || lowestVis > neighbor.getVis()) 
                        && neighbor.getVis() + neighbor.getFlux() < neighborBase) {
                    neighbourVisChunk = neighbor;
                    lowestVis = neighbor.getVis();
                }
                
                // Find lowest flux neighbor
                if (neighbourFluxChunk == null || lowestFlux > neighbor.getFlux()) {
                    neighbourFluxChunk = neighbor;
                    lowestFlux = neighbor.getFlux();
                }
            }
        }
        
        // Equalize vis with lowest neighbor (if significant difference)
        if (neighbourVisChunk != null && lowestVis < currentVis && lowestVis / currentVis < 0.75) {
            float transfer = Math.min(currentVis - lowestVis, 1.0f);
            currentVis -= transfer;
            neighbourVisChunk.setVis(lowestVis + transfer);
            dirty = true;
            markChunkAsDirty(neighbourVisChunk);
        }
        
        // Spread flux to lowest neighbor (if this chunk has high flux)
        float fluxThreshold = Math.max(5.0f, auraChunk.getBase() / 10.0f);
        if (neighbourFluxChunk != null && currentFlux > fluxThreshold && lowestFlux < currentFlux / 1.75) {
            float transfer = Math.min(currentFlux - lowestFlux, 1.0f);
            currentFlux -= transfer;
            neighbourFluxChunk.setFlux(lowestFlux + transfer);
            dirty = true;
            markChunkAsDirty(neighbourFluxChunk);
        }
        
        // Regenerate vis if below base capacity
        if (currentVis + currentFlux < base) {
            float regen = Math.min(base - (currentVis + currentFlux), phaseVis);
            currentVis += regen;
            dirty = true;
        }
        // Convert excess vis to flux at high saturation
        else if (currentVis > base * 1.25 && rand.nextFloat() < 0.1) {
            currentFlux += phaseFlux;
            currentVis -= phaseFlux;
            dirty = true;
        }
        // Generate flux when vis is very low
        else if (currentVis <= base * 0.1 && currentVis >= currentFlux && rand.nextFloat() < 0.1) {
            currentFlux += phaseFlux;
            dirty = true;
        }
        
        // Apply changes
        if (dirty) {
            auraChunk.setVis(currentVis);
            auraChunk.setFlux(currentFlux);
            markChunkAsDirty(auraChunk);
        }
        
        // Check for flux rift trigger condition
        if (currentFlux > base * 0.75 && rand.nextFloat() < currentFlux / 5000.0f) {
            BlockPos riftPos = new BlockPos(x * 16 + 8, 64, z * 16 + 8);
            AuraHandler.riftTrigger.put(dimension, riftPos);
        }
    }

    /**
     * Mark a chunk as dirty (needs saving).
     */
    private void markChunkAsDirty(AuraChunk chunk) {
        if (chunk == null || chunk.isModified()) {
            return;
        }
        
        ChunkPos pos = chunk.getLoc();
        if (pos == null) return;
        
        CopyOnWriteArrayList<ChunkPos> dirtyList = AuraHandler.dirtyChunks
            .computeIfAbsent(dimension, k -> new CopyOnWriteArrayList<>());
        
        if (!dirtyList.contains(pos)) {
            dirtyList.add(pos);
        }
    }

    /**
     * Stop this aura thread gracefully.
     */
    public void stop() {
        stop = true;
    }

    /**
     * Check if this thread is stopped.
     */
    public boolean isStopped() {
        return stop;
    }

    /**
     * Get the dimension this thread manages.
     */
    public ResourceKey<Level> getDimension() {
        return dimension;
    }
}




