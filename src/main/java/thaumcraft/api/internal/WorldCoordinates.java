package thaumcraft.api.internal;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * WorldCoordinates - A position in a specific dimension.
 * Used for tracking cross-dimensional positions.
 * 
 * In 1.20.1, dimensions are identified by ResourceKey<Level> rather than integer IDs.
 * For simplicity and compatibility, we store the dimension as a string.
 */
public class WorldCoordinates implements Comparable<WorldCoordinates> {
    
    public BlockPos pos;
    public String dim;
    
    public WorldCoordinates() {
        this.pos = BlockPos.ZERO;
        this.dim = Level.OVERWORLD.location().toString();
    }
    
    public WorldCoordinates(BlockPos pos, String dim) {
        this.pos = pos;
        this.dim = dim;
    }
    
    public WorldCoordinates(BlockPos pos, int dim) {
        this.pos = pos;
        this.dim = String.valueOf(dim);
    }
    
    public WorldCoordinates(BlockPos pos, Level level) {
        this.pos = pos;
        this.dim = level.dimension().location().toString();
    }

    public WorldCoordinates(BlockPos pos, LevelAccessor level) {
        this.pos = pos;
        if (level instanceof Level) {
            this.dim = ((Level) level).dimension().location().toString();
        } else {
            this.dim = Level.OVERWORLD.location().toString();
        }
    }
    
    public WorldCoordinates(BlockEntity tile) {
        this.pos = tile.getBlockPos();
        this.dim = tile.getLevel() != null ? 
                tile.getLevel().dimension().location().toString() : 
                Level.OVERWORLD.location().toString();
    }
    
    public WorldCoordinates(WorldCoordinates other) {
        this.pos = other.pos;
        this.dim = other.dim;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof WorldCoordinates other)) {
            return false;
        }
        return pos.equals(other.pos) && dim.equals(other.dim);
    }
    
    @Override
    public int hashCode() {
        return pos.hashCode() ^ dim.hashCode();
    }
    
    public int compareWorldCoordinate(WorldCoordinates other) {
        if (!dim.equals(other.dim)) {
            return dim.compareTo(other.dim);
        }
        return pos.compareTo(other.pos);
    }
    
    public void set(BlockPos pos, String dim) {
        this.pos = pos;
        this.dim = dim;
    }
    
    public void set(BlockPos pos, Level level) {
        this.pos = pos;
        this.dim = level.dimension().location().toString();
    }
    
    /**
     * Returns the squared distance between this coordinates and the given position.
     */
    public double getDistanceSquared(BlockPos other) {
        return pos.distSqr(other);
    }
    
    /**
     * Return the squared distance to another WorldCoordinates.
     * Note: Only compares positions, ignores dimension.
     */
    public double getDistanceSquaredToWorldCoordinates(WorldCoordinates other) {
        return getDistanceSquared(other.pos);
    }
    
    @Override
    public int compareTo(WorldCoordinates other) {
        return compareWorldCoordinate(other);
    }
    
    /**
     * Read from NBT.
     */
    public void readNBT(CompoundTag nbt) {
        int x = nbt.getInt("w_x");
        int y = nbt.getInt("w_y");
        int z = nbt.getInt("w_z");
        pos = new BlockPos(x, y, z);
        dim = nbt.getString("w_d");
        if (dim.isEmpty()) {
            dim = Level.OVERWORLD.location().toString();
        }
    }
    
    /**
     * Write to NBT.
     */
    public void writeNBT(CompoundTag nbt) {
        nbt.putInt("w_x", pos.getX());
        nbt.putInt("w_y", pos.getY());
        nbt.putInt("w_z", pos.getZ());
        nbt.putString("w_d", dim);
    }

    public int getX() {
        return pos.getX();
    }

    public int getY() {
        return pos.getY();
    }

    public int getZ() {
        return pos.getZ();
    }

    @Override
    public String toString() {
        return "WorldCoordinates{" + pos.toShortString() + " in " + dim + "}";
    }
}




