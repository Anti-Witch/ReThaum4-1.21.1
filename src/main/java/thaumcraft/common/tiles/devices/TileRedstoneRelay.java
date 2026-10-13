package thaumcraft.common.tiles.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import thaumcraft.common.tiles.TileThaumcraft;
import thaumcraft.init.ModBlockEntities;

/**
 * Redstone Relay tile entity - converts redstone signal strength.
 * Takes input at one strength and outputs at another strength.
 * Useful for creating complex redstone circuits.
 */
public class TileRedstoneRelay extends TileThaumcraft {

    private int inputThreshold = 1;   // Minimum input signal to activate
    private int outputStrength = 15;  // Output signal strength when active

    public TileRedstoneRelay(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public TileRedstoneRelay(BlockPos pos, BlockState state) {
        this(ModBlockEntities.REDSTONE_RELAY.get(), pos, state);
    }

    // ==================== NBT ====================

    @Override
    protected void writeSyncNBT(CompoundTag tag) {
        super.writeSyncNBT(tag);
        tag.putByte("In", (byte) inputThreshold);
        tag.putByte("Out", (byte) outputStrength);
    }

    @Override
    protected void readSyncNBT(CompoundTag tag) {
        super.readSyncNBT(tag);
        setInputThreshold(tag.getByte("In"));
        setOutputStrength(tag.getByte("Out"));
    }

    // ==================== Configuration ====================

    /**
     * Increase input threshold (cycles 1-15).
     */
    public void increaseInput() {
        if (level == null || level.isClientSide()) return;
        
        inputThreshold++;
        if (inputThreshold > 15) {
            inputThreshold = 1;
        }
        setChanged();
        syncTile(false);
    }

    /**
     * Increase output strength (cycles 1-15).
     */
    public void increaseOutput() {
        if (level == null || level.isClientSide()) return;
        
        outputStrength++;
        if (outputStrength > 15) {
            outputStrength = 1;
        }
        setChanged();
        syncTile(false);
    }

    // ==================== Getters/Setters ====================

    public int getInputThreshold() {
        return inputThreshold;
    }

    public void setInputThreshold(int value) {
        inputThreshold = Math.max(1, Math.min(15, value));
    }

    public int getOutputStrength() {
        return outputStrength;
    }

    public void setOutputStrength(int value) {
        outputStrength = Math.max(1, Math.min(15, value));
    }

    /**
     * Calculate output based on input signal.
     * Returns outputStrength if input >= inputThreshold, otherwise 0.
     */
    public int calculateOutput(int inputSignal) {
        return inputSignal >= inputThreshold ? outputStrength : 0;
    }

    // ==================== Direction ====================

    public Direction getFacing() {
        BlockState state = getBlockState();
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.getValue(BlockStateProperties.FACING);
        }
        return Direction.NORTH;
    }

    /**
     * Get the input direction (opposite of facing).
     */
    public Direction getInputDirection() {
        return getFacing().getOpposite();
    }

    /**
     * Get the output direction (same as facing).
     */
    public Direction getOutputDirection() {
        return getFacing();
    }

    // ==================== Hit Detection ====================

    /**
     * Get the bounding box for the input dial (for click detection).
     */
    public AABB getInputDialBounds() {
        Direction facing = getFacing();
        double minX, minY, minZ, maxX, maxY, maxZ;
        
        // Base dial position relative to block center
        // Input dial is on the left side when looking at the front
        switch (facing) {
            case SOUTH -> {
                minX = worldPosition.getX() + 0.125;
                maxX = worldPosition.getX() + 0.375;
                minZ = worldPosition.getZ() + 0.125;
                maxZ = worldPosition.getZ() + 0.375;
            }
            case WEST -> {
                minX = worldPosition.getX() + 0.125;
                maxX = worldPosition.getX() + 0.375;
                minZ = worldPosition.getZ() + 0.625;
                maxZ = worldPosition.getZ() + 0.875;
            }
            case NORTH -> {
                minX = worldPosition.getX() + 0.625;
                maxX = worldPosition.getX() + 0.875;
                minZ = worldPosition.getZ() + 0.625;
                maxZ = worldPosition.getZ() + 0.875;
            }
            case EAST -> {
                minX = worldPosition.getX() + 0.625;
                maxX = worldPosition.getX() + 0.875;
                minZ = worldPosition.getZ() + 0.125;
                maxZ = worldPosition.getZ() + 0.375;
            }
            default -> {
                minX = worldPosition.getX() + 0.125;
                maxX = worldPosition.getX() + 0.375;
                minZ = worldPosition.getZ() + 0.125;
                maxZ = worldPosition.getZ() + 0.375;
            }
        }
        
        minY = worldPosition.getY() + 0.0625;
        maxY = worldPosition.getY() + 0.25;
        
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Get the bounding box for the output dial (for click detection).
     */
    public AABB getOutputDialBounds() {
        Direction facing = getFacing();
        double minX, minY, minZ, maxX, maxY, maxZ;
        
        // Output dial is on the right side when looking at the front
        switch (facing) {
            case SOUTH -> {
                minX = worldPosition.getX() + 0.375;
                maxX = worldPosition.getX() + 0.625;
                minZ = worldPosition.getZ() + 0.625;
                maxZ = worldPosition.getZ() + 0.875;
            }
            case WEST -> {
                minX = worldPosition.getX() + 0.125;
                maxX = worldPosition.getX() + 0.375;
                minZ = worldPosition.getZ() + 0.375;
                maxZ = worldPosition.getZ() + 0.625;
            }
            case NORTH -> {
                minX = worldPosition.getX() + 0.375;
                maxX = worldPosition.getX() + 0.625;
                minZ = worldPosition.getZ() + 0.125;
                maxZ = worldPosition.getZ() + 0.375;
            }
            case EAST -> {
                minX = worldPosition.getX() + 0.625;
                maxX = worldPosition.getX() + 0.875;
                minZ = worldPosition.getZ() + 0.375;
                maxZ = worldPosition.getZ() + 0.625;
            }
            default -> {
                minX = worldPosition.getX() + 0.375;
                maxX = worldPosition.getX() + 0.625;
                minZ = worldPosition.getZ() + 0.625;
                maxZ = worldPosition.getZ() + 0.875;
            }
        }
        
        minY = worldPosition.getY() + 0.0625;
        maxY = worldPosition.getY() + 0.25;
        
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }
}




