package thaumcraft.common.world.aura;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.event.level.ChunkDataEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thaumcraft.Thaumcraft;
import thaumcraft.common.config.ModConfig;

/**
 * AuraChunkHandler - Handles aura generation and persistence for chunks.
 * 
 * This handler:
 * - Generates initial aura for newly created chunks
 * - Loads aura data from chunk NBT
 * - Saves aura data to chunk NBT
 * 
 * Aura is generated based on biome modifiers and nearby chunks.
 */
@EventBusSubscriber(modid=Thaumcraft.MODID)
public class AuraChunkHandler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AuraChunkHandler.class);
    
    // NBT tag names for aura data
    private static final String TAG_THAUMCRAFT = "thaumcraft";
    private static final String TAG_AURA = "aura";
    private static final String TAG_BASE = "base";
    private static final String TAG_VIS = "vis";
    private static final String TAG_FLUX = "flux";
    
    /**
     * Called when chunk data is loaded from disk.
     * Restores aura values from saved NBT data.
     */
    @SubscribeEvent
    public static void onChunkDataLoad(ChunkDataEvent.Load event) {
        if (event.getLevel() == null || event.getLevel().isClientSide()) {
            return;
        }
        
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        
        if (!(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }
        
        CompoundTag data = event.getData();
        ResourceKey<Level> dimension = level.dimension();
        ChunkPos chunkPos = chunk.getPos();
        
        if (data.contains(TAG_THAUMCRAFT)) {
            CompoundTag tcData = data.getCompound(TAG_THAUMCRAFT);
            if (tcData.contains(TAG_AURA)) {
                CompoundTag auraData = tcData.getCompound(TAG_AURA);
                
                short base = auraData.getShort(TAG_BASE);
                float vis = auraData.getFloat(TAG_VIS);
                float flux = auraData.getFloat(TAG_FLUX);
                
                // Restore aura chunk data
                AuraHandler.addAuraChunk(dimension, chunk, base, vis, flux);
                return;
            }
        }
        
        // No saved aura data - this is a newly loaded chunk that needs generation
        // This will be handled by onChunkLoad if the chunk is truly new
    }
    
    /**
     * Called when chunk data is saved to disk.
     * Persists aura values to NBT data.
     */
    @SubscribeEvent
    public static void onChunkDataSave(ChunkDataEvent.Save event) {
        if (event.getLevel() == null || event.getLevel().isClientSide()) {
            return;
        }
        
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        
        ResourceKey<Level> dimension = level.dimension();
        ChunkPos chunkPos = event.getChunk().getPos();
        
        AuraChunk auraChunk = AuraHandler.getAuraChunk(dimension, chunkPos.x, chunkPos.z);
        if (auraChunk == null) {
            return;
        }
        
        CompoundTag data = event.getData();
        
        CompoundTag tcData = data.contains(TAG_THAUMCRAFT) ? 
                data.getCompound(TAG_THAUMCRAFT) : new CompoundTag();
        
        CompoundTag auraData = new CompoundTag();
        auraData.putShort(TAG_BASE, auraChunk.getBase());
        auraData.putFloat(TAG_VIS, auraChunk.getVis());
        auraData.putFloat(TAG_FLUX, auraChunk.getFlux());
        
        tcData.put(TAG_AURA, auraData);
        data.put(TAG_THAUMCRAFT, tcData);
    }
    
    /**
     * Called when a chunk is loaded (both from disk and newly generated).
     * Generates aura for chunks that don't have it yet.
     */
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() == null || event.getLevel().isClientSide()) {
            return;
        }
        
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        
        if (!(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }
        
        // Check if aura generation is enabled
        if (!ModConfig.generateAura) {
            return;
        }
        
        ResourceKey<Level> dimension = level.dimension();
        ChunkPos chunkPos = chunk.getPos();
        
        // Check if this chunk already has aura data
        AuraChunk existingAura = AuraHandler.getAuraChunk(dimension, chunkPos.x, chunkPos.z);
        if (existingAura != null && existingAura.getBase() > 0) {
            // Chunk already has aura, skip generation
            return;
        }
        
        // Generate aura for this chunk
        generateChunkAura(level, chunk);
    }
    
    /**
     * Called when a chunk is unloaded.
     * Cleans up aura chunk data from memory (data is persisted via ChunkDataEvent.Save).
     */
    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() == null || event.getLevel().isClientSide()) {
            return;
        }
        
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        
        ResourceKey<Level> dimension = level.dimension();
        ChunkPos chunkPos = event.getChunk().getPos();
        
        // Remove from memory (already saved by ChunkDataEvent.Save)
        AuraHandler.removeAuraChunk(dimension, chunkPos.x, chunkPos.z);
    }
    
    /**
     * Generate initial aura for a newly created chunk.
     */
    private static void generateChunkAura(ServerLevel level, LevelChunk chunk) {
        RandomSource random = level.random;
        AuraHandler.generateAura(chunk, random);
        
        // Mark as dirty so it gets saved
        ResourceKey<Level> dimension = level.dimension();
        ChunkPos chunkPos = chunk.getPos();
        
        if (!AuraHandler.dirtyChunks.containsKey(dimension)) {
            AuraHandler.dirtyChunks.put(dimension, new java.util.concurrent.CopyOnWriteArrayList<>());
        }
        AuraHandler.dirtyChunks.get(dimension).add(chunkPos);
    }
}




