package thaumcraft.common.world.aura;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.common.lib.utils.PosXY;
import thaumcraft.common.world.biomes.BiomeHandler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central handler for all aura-related operations.
 * Manages aura data for all dimensions and provides methods for querying and modifying vis/flux.
 */
public class AuraHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuraHandler.class);

    /** Maximum base aura value for a chunk */
    public static final int AURA_CEILING = 500;

    /** Aura data storage per dimension */
    private static final ConcurrentHashMap<ResourceKey<Level>, AuraWorld> auras = new ConcurrentHashMap<>();

    /** Chunks that have been modified and need syncing */
    public static final ConcurrentHashMap<ResourceKey<Level>, CopyOnWriteArrayList<ChunkPos>> dirtyChunks = new ConcurrentHashMap<>();

    /** Positions where flux rifts should be triggered */
    public static final ConcurrentHashMap<ResourceKey<Level>, BlockPos> riftTrigger = new ConcurrentHashMap<>();

    // ==================== World Management ====================

    public static AuraWorld getAuraWorld(ResourceKey<Level> dimension) {
        return auras.get(dimension);
    }

    public static AuraChunk getAuraChunk(ResourceKey<Level> dimension, int x, int z) {
        AuraWorld world = auras.get(dimension);
        if (world == null) {
            addAuraWorld(dimension);
            world = auras.get(dimension);
        }
        return world != null ? world.getAuraChunkAt(x, z) : null;
    }

    public static void addAuraWorld(ResourceKey<Level> dimension) {
        if (!auras.containsKey(dimension)) {
            auras.put(dimension, new AuraWorld(dimension));
            LOGGER.info("Creating aura cache for dimension {}", dimension.location());
        }
    }

    public static void removeAuraWorld(ResourceKey<Level> dimension) {
        auras.remove(dimension);
        LOGGER.info("Removing aura cache for dimension {}", dimension.location());
    }

    public static void addAuraChunk(ResourceKey<Level> dimension, LevelChunk chunk, short base, float vis, float flux) {
        AuraWorld aw = auras.computeIfAbsent(dimension, AuraWorld::new);
        aw.setAuraChunk(new PosXY(chunk.getPos().x, chunk.getPos().z), new AuraChunk(chunk, base, vis, flux));
    }

    public static void removeAuraChunk(ResourceKey<Level> dimension, int x, int z) {
        AuraWorld aw = auras.get(dimension);
        if (aw != null) {
            aw.removeAuraChunk(x, z);
        }
    }

    // ==================== Aura Queries ====================

    /**
     * Gets the total aura (vis + flux) at the given position.
     */
    public static float getTotalAura(Level level, BlockPos pos) {
        AuraChunk ac = getAuraChunk(level.dimension(), pos.getX() >> 4, pos.getZ() >> 4);
        return ac != null ? (ac.getVis() + ac.getFlux()) : 0.0f;
    }

    /**
     * Gets the flux saturation ratio (flux / base) at the given position.
     */
    public static float getFluxSaturation(Level level, BlockPos pos) {
        AuraChunk ac = getAuraChunk(level.dimension(), pos.getX() >> 4, pos.getZ() >> 4);
        if (ac != null && ac.getBase() > 0) {
            return ac.getFlux() / ac.getBase();
        }
        return 0.0f;
    }

    /**
     * Gets the current vis at the given position.
     */
    public static float getVis(Level level, BlockPos pos) {
        AuraChunk ac = getAuraChunk(level.dimension(), pos.getX() >> 4, pos.getZ() >> 4);
        return ac != null ? ac.getVis() : 0.0f;
    }

    /**
     * Gets the current flux at the given position.
     */
    public static float getFlux(Level level, BlockPos pos) {
        AuraChunk ac = getAuraChunk(level.dimension(), pos.getX() >> 4, pos.getZ() >> 4);
        return ac != null ? ac.getFlux() : 0.0f;
    }

    /**
     * Gets the base aura level at the given position.
     */
    public static int getAuraBase(Level level, BlockPos pos) {
        AuraChunk ac = getAuraChunk(level.dimension(), pos.getX() >> 4, pos.getZ() >> 4);
        return ac != null ? ac.getBase() : 0;
    }

    /**
     * Checks if aura should be preserved (below 10% and player has research).
     * If player is null, assumes research is complete.
     */
    public static boolean shouldPreserveAura(Level level, Player player, BlockPos pos) {
        int base = getAuraBase(level, pos);
        if (base <= 0) return false;
        
        float visRatio = getVis(level, pos) / base;
        if (visRatio >= 0.1f) return false;
        
        if (player == null) return true;
        
        return ThaumcraftCapabilities.isResearchComplete(player, "AURAPRESERVE");
    }

    // ==================== Aura Modification ====================

    /**
     * Adds vis to the aura at the given position.
     */
    public static void addVis(Level level, BlockPos pos, float amount) {
        if (amount <= 0.0f) return;
        try {
            AuraChunk ac = getAuraChunk(level.dimension(), pos.getX() >> 4, pos.getZ() >> 4);
            modifyVisInChunk(ac, amount, true);
        } catch (Exception ignored) {}
    }

    /**
     * Adds flux to the aura at the given position.
     */
    public static void addFlux(Level level, BlockPos pos, float amount) {
        if (amount <= 0.0f) return;
        try {
            AuraChunk ac = getAuraChunk(level.dimension(), pos.getX() >> 4, pos.getZ() >> 4);
            modifyFluxInChunk(ac, amount, true);
        } catch (Exception ignored) {}
    }

    /**
     * Drains vis from the aura at the given position.
     * @return how much was actually drained
     */
    public static float drainVis(Level level, BlockPos pos, float amount, boolean simulate) {
        try {
            AuraChunk ac = getAuraChunk(level.dimension(), pos.getX() >> 4, pos.getZ() >> 4);
            if (ac == null) return 0.0f;
            
            float available = ac.getVis();
            if (amount > available) {
                amount = available;
            }
            
            if (modifyVisInChunk(ac, -amount, !simulate)) {
                return amount;
            }
        } catch (Exception ignored) {}
        return 0.0f;
    }

    /**
     * Drains flux from the aura at the given position.
     * @return how much was actually drained
     */
    public static float drainFlux(Level level, BlockPos pos, float amount, boolean simulate) {
        try {
            AuraChunk ac = getAuraChunk(level.dimension(), pos.getX() >> 4, pos.getZ() >> 4);
            if (ac == null) return 0.0f;
            
            float available = ac.getFlux();
            if (amount > available) {
                amount = available;
            }
            
            if (modifyFluxInChunk(ac, -amount, !simulate)) {
                return amount;
            }
        } catch (Exception ignored) {}
        return 0.0f;
    }

    /**
     * Modifies vis in a chunk.
     * @return true if the chunk was valid and modification was possible
     */
    public static boolean modifyVisInChunk(AuraChunk ac, float amount, boolean apply) {
        if (ac == null) return false;
        if (apply) {
            ac.setVis(Math.max(0.0f, ac.getVis() + amount));
        }
        return true;
    }

    /**
     * Modifies flux in a chunk.
     * @return true if the chunk was valid and modification was possible
     */
    private static boolean modifyFluxInChunk(AuraChunk ac, float amount, boolean apply) {
        if (ac == null) return false;
        if (apply) {
            ac.setFlux(Math.max(0.0f, ac.getFlux() + amount));
        }
        return true;
    }

    // ==================== World Generation ====================

    /**
     * Generates initial aura for a newly loaded chunk.
     * Base aura is determined by the biome's aura modifier.
     */
    public static void generateAura(LevelChunk chunk, RandomSource rand) {
        Level level = chunk.getLevel();
        BlockPos center = new BlockPos(chunk.getPos().x * 16 + 8, 50, chunk.getPos().z * 16 + 8);
        
        // Get biome aura modifier (TODO: implement BiomeHandler)
        float life = getBiomeAuraModifier(level, center);
        
        // Average with neighboring chunks
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = center.relative(dir, 16);
            life += getBiomeAuraModifier(level, neighborPos);
        }
        life /= 5.0f;
        
        // Add some random variation
        float noise = (float)(1.0 + rand.nextGaussian() * 0.1);
        short base = (short)(life * AURA_CEILING * noise);
        base = (short) Mth.clamp(base, 0, AURA_CEILING);
        
        addAuraChunk(level.dimension(), chunk, base, base, 0.0f);
    }

    /**
     * Gets the aura modifier for the biome at the given position.
     * Uses BiomeHandler to look up biome-specific aura values.
     */
    private static float getBiomeAuraModifier(Level level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        return BiomeHandler.getAuraModifier(biome);
    }
}




