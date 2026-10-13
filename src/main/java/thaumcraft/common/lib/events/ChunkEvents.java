package thaumcraft.common.lib.events;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.event.level.ChunkDataEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.Thaumcraft;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.common.golems.seals.SealEntity;
import thaumcraft.common.golems.seals.SealHandler;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.misc.PacketSealToClient;

/**
 * ChunkEvents - Handles chunk-based data persistence and syncing.
 * 
 * Responsibilities:
 * - Save seals to chunk NBT data
 * - Load seals from chunk NBT data
 * - Sync seals to players when they start watching a chunk
 * 
 * Ported from 1.12.2. Key changes:
 * - NBTTagCompound -> CompoundTag
 * - NBTTagList -> ListTag
 * - world.provider.getDimension() -> level.dimension()
 * - ChunkDataEvent APIs updated for 1.20.1
 * - ChunkWatchEvent.Watch -> ChunkWatchEvent.Watch with updated API
 */
@EventBusSubscriber(modid = Thaumcraft.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ChunkEvents {
    
    private static final String THAUMCRAFT_DATA_KEY = "Thaumcraft";
    private static final String SEALS_KEY = "seals";
    
    /**
     * Save chunk data - seals are written to chunk NBT
     */
    @SubscribeEvent
    public static void onChunkSave(ChunkDataEvent.Save event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide()) {
            ChunkPos chunkPos = event.getChunk().getPos();
            
            CompoundTag thaumcraftData = new CompoundTag();
            
            // Save seals in this chunk
            ListTag sealList = new ListTag();
            for (ISealEntity seal : SealHandler.getSealsInChunk(level, chunkPos)) {
                if (seal.getSeal() != null) {
                    CompoundTag sealNbt = seal.writeNBT();
                    sealList.add(sealNbt);
                }
            }
            
            if (!sealList.isEmpty()) {
                thaumcraftData.put(SEALS_KEY, sealList);
            }
            
            // Only write data if we have something to save
            if (!thaumcraftData.isEmpty()) {
                event.getData().put(THAUMCRAFT_DATA_KEY, thaumcraftData);
            }
            
            // Clean up seals from unloaded chunks to prevent memory leaks
            // Only do this if the chunk is actually being unloaded (not just saved)
            if (event.getChunk() instanceof LevelChunk levelChunk && !levelChunk.isUnsaved()) {
                // Chunk is being unloaded, remove seals from memory
                for (ISealEntity seal : SealHandler.getSealsInChunk(level, chunkPos)) {
                    SealHandler.removeSealEntity(level, seal.getSealPos(), true);
                }
            }
        }
    }
    
    /**
     * Load chunk data - seals are read from chunk NBT
     */
    @SubscribeEvent
    public static void onChunkLoad(ChunkDataEvent.Load event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide()) {
            CompoundTag data = event.getData();
            
            if (data.contains(THAUMCRAFT_DATA_KEY, Tag.TAG_COMPOUND)) {
                CompoundTag thaumcraftData = data.getCompound(THAUMCRAFT_DATA_KEY);
                
                // Load seals
                if (thaumcraftData.contains(SEALS_KEY, Tag.TAG_LIST)) {
                    ListTag sealList = thaumcraftData.getList(SEALS_KEY, Tag.TAG_COMPOUND);
                    
                    for (int i = 0; i < sealList.size(); i++) {
                        CompoundTag sealNbt = sealList.getCompound(i);
                        
                        try {
                            SealEntity seal = new SealEntity();
                            seal.readNBT(sealNbt);
                            
                            if (seal.getSeal() != null && seal.getSealPos() != null) {
                                SealHandler.addSealEntity(level, seal);
                            }
                        } catch (Exception e) {
                            Thaumcraft.LOGGER.error("Failed to load seal from chunk data", e);
                        }
                    }
                }
            }
            
            // TODO: When Aura system is implemented, load aura data here as well
            // if (thaumcraftData.contains("base")) {
            //     short base = thaumcraftData.getShort("base");
            //     float vis = thaumcraftData.getFloat("vis");
            //     float flux = thaumcraftData.getFloat("flux");
            //     AuraHandler.addAuraChunk(level.dimension(), event.getChunk(), base, vis, flux);
            // }
        }
    }
    
    /**
     * When a player starts watching a chunk, sync all seals in that chunk to them
     */
    @SubscribeEvent
    public static void onChunkWatch(ChunkWatchEvent.Watch event) {
        ServerPlayer player = event.getPlayer();
        Level level = player.level();
        ChunkPos chunkPos = event.getPos();
        
        // Send all seals in the chunk to the player
        for (ISealEntity seal : SealHandler.getSealsInChunk(level, chunkPos)) {
            if (seal.getSeal() != null) {
                PacketHandler.sendToPlayer(new PacketSealToClient(seal), player);
            }
        }
    }
    
    /**
     * When a player stops watching a chunk, we could optionally clean up client-side data
     * For now, we don't need to do anything special here
     */
    @SubscribeEvent
    public static void onChunkUnWatch(ChunkWatchEvent.UnWatch event) {
        // Optional: Could send a packet to remove seals from client cache
        // For now, client keeps the data until the next login/dimension change
    }
}




