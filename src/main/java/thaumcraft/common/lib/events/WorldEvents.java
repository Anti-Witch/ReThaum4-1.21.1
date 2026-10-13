package thaumcraft.common.lib.events;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.level.NoteBlockEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.Thaumcraft;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.world.aura.AuraHandler;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * WorldEvents - Handles world-related events.
 * 
 * Features:
 * - Initialize aura system when worlds load
 * - Clean up aura data when worlds unload  
 * - Block placement restrictions near bosses
 * - Note block event tracking for arcane ear
 * 
 * Ported from Thaumcraft 1.12.2 to 1.20.1
 */
@EventBusSubscriber(modid = Thaumcraft.MODID, bus = EventBusSubscriber.Bus.GAME)
public class WorldEvents {
    
    public static final WorldEvents INSTANCE = new WorldEvents();
    
    // Note block events for arcane ear, keyed by dimension
    public static final HashMap<String, ArrayList<NoteBlockData>> noteBlockEvents = new HashMap<>();
    
    /**
     * Handle world load - initialize aura system.
     */
    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            AuraHandler.addAuraWorld(serverLevel.dimension());
            ThaumcraftCraftingManager.captureRecipeData(serverLevel);
            Thaumcraft.LOGGER.debug("Loaded aura for dimension: {}", serverLevel.dimension().location());
        }
    }
    
    /**
     * Handle world save.
     */
    @SubscribeEvent
    public static void onWorldSave(LevelEvent.Save event) {
        // Aura data is saved with chunk data, nothing special needed here
    }
    
    /**
     * Handle world unload - clean up aura data.
     */
    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            String dimKey = serverLevel.dimension().location().toString();

            // Clean up seal entities for this dimension
            // TODO: SealHandler.sealEntities.remove(dimKey);

            // Clean up aura data
            AuraHandler.removeAuraWorld(serverLevel.dimension());

            // Release cached recipe/registry references
            ThaumcraftCraftingManager.clearRecipeData();

            // Clean up note block events
            noteBlockEvents.remove(dimKey);

            Thaumcraft.LOGGER.debug("Unloaded aura for dimension: {}", dimKey);
        }
    }
    
    /**
     * Handle block placement - prevent near active bosses.
     */
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof Level level)) return;
        if (level.isClientSide()) return;
        
        if (event.getEntity() instanceof Player player) {
            BlockPos pos = event.getPos();
            if (isNearActiveBoss(level, player, pos)) {
                event.setCanceled(true);
            }
        }
    }
    
    /**
     * Check if position is near an active boss that restricts building.
     * TODO: Implement when boss entities are ported
     */
    private static boolean isNearActiveBoss(Level level, Player player, BlockPos pos) {
        // Would check for:
        // - Eldritch Guardian
        // - Crimson Knight
        // - Other Thaumcraft bosses
        return false;
    }
    
    /**
     * Handle note block play events for arcane ear.
     */
    @SubscribeEvent
    public static void onNoteBlockPlay(NoteBlockEvent.Play event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        
        String dimKey = serverLevel.dimension().location().toString();
        
        noteBlockEvents.computeIfAbsent(dimKey, k -> new ArrayList<>());
        ArrayList<NoteBlockData> list = noteBlockEvents.get(dimKey);
        
        BlockPos pos = event.getPos();
        NoteBlockData data = new NoteBlockData(
                pos.getX(), pos.getY(), pos.getZ(),
                event.getInstrument().ordinal(),
                event.getVanillaNoteId()
        );
        
        list.add(data);
        
        // Clean up old events (keep last 100)
        while (list.size() > 100) {
            list.remove(0);
        }
    }
    
    /**
     * Get and clear note block events for a dimension.
     * Used by TileArcaneEar.
     */
    public static ArrayList<NoteBlockData> getAndClearNoteBlockEvents(String dimKey) {
        ArrayList<NoteBlockData> events = noteBlockEvents.remove(dimKey);
        return events != null ? events : new ArrayList<>();
    }
    
    /**
     * Data class for note block events.
     */
    public static class NoteBlockData {
        public final int x, y, z;
        public final int instrument;
        public final int note;
        
        public NoteBlockData(int x, int y, int z, int instrument, int note) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.instrument = instrument;
            this.note = note;
        }
    }
}




