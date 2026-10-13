package thaumcraft.common.world.aura;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.world.taint.TaintHelper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@EventBusSubscriber(modid = Thaumcraft.MODID)
public class AuraScheduler {

    private static final Map<ResourceKey<Level>, Long> lastWorldTime = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<Level>, Float> phaseVis = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<Level>, Float> phaseFlux = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<Level>, Float> phaseMax = new ConcurrentHashMap<>();
    
    private static final float[] phaseTable = { 0.25f, 0.15f, 0.1f, 0.05f, 0.0f, 0.05f, 0.1f, 0.15f };
    private static final float[] maxTable = { 0.15f, 0.05f, 0.0f, -0.05f, -0.15f, -0.05f, 0.0f, 0.05f };
    
    private static final Random rand = new Random();

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide()) return;

        if (event.getLevel() instanceof ServerLevel level) {
            ResourceKey<Level> dim = level.dimension();

            // Only run once per second-ish (20 ticks) per dimension
            long currentTime = System.currentTimeMillis();
            long worldTime = level.getGameTime();

            if (worldTime != lastWorldTime.getOrDefault(dim, -1L)) {
                lastWorldTime.put(dim, worldTime);

                int moonPhase = level.dimensionType().moonPhase(worldTime);
                phaseVis.put(dim, phaseTable[moonPhase]);
                phaseMax.put(dim, 1.0f + maxTable[moonPhase]);
                phaseFlux.put(dim, 0.25f - phaseVis.get(dim));

                AuraWorld auraWorld = AuraHandler.getAuraWorld(dim);
                if (auraWorld != null) {
                    // Process rift triggers first
                    // processRiftTriggers removed - not implemented

                    // Process flux events (negative effects from high flux)
                    // processFluxEvents removed - not implemented

                    // Process aura chunks every 20 ticks (1 second)
                    if (level.getGameTime() % 20 == 0) {
                        processAuraChunks(level, auraWorld);

                        // Process taint spread (restored 2026-08-09)
                        TaintHelper.processTaintSpread(level);
                    }
                }
            }
        }
    }
    
    private static void processAuraChunks(ServerLevel level, AuraWorld auraWorld) {
        ResourceKey<Level> dim = level.dimension();
        float pVis = phaseVis.getOrDefault(dim, 0.25f);
        float pFlux = phaseFlux.getOrDefault(dim, 0.0f);
        float pMax = phaseMax.getOrDefault(dim, 1.0f);
        
        // To avoid ConcurrentModificationException and lag spikes, we iterate a snapshot or use concurrent map features.
        // AuraWorld uses ConcurrentHashMap.
        
        // Logic adaptation: Instead of doing all at once, maybe we should just do it? 
        // 1.12.2 had this running in a separate thread, so it didn't impact TPS directly (unless synchronized).
        // On main thread, we must be fast.
        
        // Optimisation: Process 1/20th of the chunks?
        // For faithful porting, I will try to replicate the logic but throttle it if needed.
        // The original code ran `processAuraChunk` for ALL chunks every 1000ms.
        // So if we run it every 20 ticks (1s), it's the same workload, just on the main thread.
        
        if (level.getGameTime() % 20 != 0) return;

        for (AuraChunk auraChunk : auraWorld.getAuraChunks().values()) {
            processAuraChunk(level, auraWorld, auraChunk, pVis, pFlux, pMax);
        }
    }

    private static void processAuraChunk(ServerLevel level, AuraWorld auraWorld, AuraChunk auraChunk, float phaseVis, float phaseFlux, float phaseMax) {
        if (auraChunk == null) return;
        
        // Random directions
        List<Direction> directions = new ArrayList<>(Arrays.asList(Direction.Plane.HORIZONTAL.stream().toArray(Direction[]::new)));
        Collections.shuffle(directions, rand);
        
        int x = auraChunk.getLoc().x;
        int z = auraChunk.getLoc().z;
        
        float base = auraChunk.getBase() * phaseMax;
        boolean dirty = false;
        
        float currentVis = auraChunk.getVis();
        float currentFlux = auraChunk.getFlux();
        
        AuraChunk neighbourVisChunk = null;
        AuraChunk neighbourFluxChunk = null;
        float lowestVis = Float.MAX_VALUE;
        float lowestFlux = Float.MAX_VALUE;
        
        for (Direction dir : directions) {
            AuraChunk n = auraWorld.getAuraChunkAt(x + dir.getStepX(), z + dir.getStepZ());
            if (n != null) {
                // Check vis equalization
                if ((neighbourVisChunk == null || lowestVis > n.getVis()) && n.getVis() + n.getFlux() < n.getBase() * phaseMax) {
                    neighbourVisChunk = n;
                    lowestVis = n.getVis();
                }
                
                // Check flux equalization
                if (neighbourFluxChunk != null && lowestFlux <= n.getFlux()) {
                    continue;
                }
                neighbourFluxChunk = n;
                lowestFlux = n.getFlux();
            }
        }
        
        // Equalize Vis
        if (neighbourVisChunk != null && lowestVis < currentVis && lowestVis / currentVis < 0.75) {
            float inc = Math.min(currentVis - lowestVis, 1.0f);
            currentVis -= inc;
            neighbourVisChunk.setVis(lowestVis + inc);
            dirty = true;
            markChunkAsDirty(neighbourVisChunk, auraWorld.getDimension());
        }
        
        // Equalize Flux
        if (neighbourFluxChunk != null && currentFlux > Math.max(5.0f, auraChunk.getBase() / 10.0f) && lowestFlux < currentFlux / 1.75) {
            float inc = Math.min(currentFlux - lowestFlux, 1.0f);
            currentFlux -= inc;
            neighbourFluxChunk.setFlux(lowestFlux + inc);
            dirty = true;
            markChunkAsDirty(neighbourFluxChunk, auraWorld.getDimension());
        }
        
        // Regeneration
        if (currentVis + currentFlux < base) {
            float inc = Math.min(base - (currentVis + currentFlux), phaseVis);
            currentVis += inc;
            dirty = true;
        } else if (currentVis > base * 1.25 && rand.nextFloat() < 0.1) {
            currentFlux += phaseFlux;
            currentVis -= phaseFlux;
            dirty = true;
        } else if (currentVis <= base * 0.1 && currentVis >= currentFlux && rand.nextFloat() < 0.1) {
            currentFlux += phaseFlux;
            dirty = true;
        }
        
        if (dirty) {
            auraChunk.setVis(currentVis);
            auraChunk.setFlux(currentFlux);
            markChunkAsDirty(auraChunk, auraWorld.getDimension());
        }
        
        // Rift trigger
        if (currentFlux > base * 0.75 && rand.nextFloat() < currentFlux / 500.0f / 10.0f) {
            AuraHandler.riftTrigger.put(auraWorld.getDimension(), new BlockPos(x * 16 + 8, 0, z * 16 + 8));
        }
    }
    
    private static void markChunkAsDirty(AuraChunk chunk, ResourceKey<Level> dim) {
        if (chunk.isModified()) {
            return;
        }
        ChunkPos pos = chunk.getLoc();
        AuraHandler.dirtyChunks.computeIfAbsent(dim, k -> new CopyOnWriteArrayList<>()).addIfAbsent(pos);
    }
}




