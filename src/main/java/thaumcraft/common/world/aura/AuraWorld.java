package thaumcraft.common.world.aura;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import thaumcraft.common.lib.utils.PosXY;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores all aura chunk data for a single dimension.
 */
public class AuraWorld {

    private final ResourceKey<Level> dimension;
    private final ConcurrentHashMap<PosXY, AuraChunk> auraChunks;

    public AuraWorld(ResourceKey<Level> dimension) {
        this.dimension = dimension;
        this.auraChunks = new ConcurrentHashMap<>();
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    public ConcurrentHashMap<PosXY, AuraChunk> getAuraChunks() {
        return auraChunks;
    }

    public AuraChunk getAuraChunkAt(int x, int z) {
        return getAuraChunkAt(new PosXY(x, z));
    }

    public AuraChunk getAuraChunkAt(PosXY loc) {
        return auraChunks.get(loc);
    }

    public void setAuraChunk(PosXY loc, AuraChunk chunk) {
        auraChunks.put(loc, chunk);
    }

    public void removeAuraChunk(PosXY loc) {
        auraChunks.remove(loc);
    }

    public void removeAuraChunk(int x, int z) {
        auraChunks.remove(new PosXY(x, z));
    }
}




