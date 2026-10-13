package thaumcraft.common.tiles.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.init.ModBlockEntities;

/**
 * Valve tube - can be toggled open/closed with redstone.
 * When closed, blocks all essentia flow.
 */
public class TileTubeValve extends TileTube {

    public boolean allowFlow = true;
    private boolean wasPoweredLastTick = false;
    
    // Client animation
    public float rotation = 0.0f;

    public TileTubeValve(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public TileTubeValve(BlockPos pos, BlockState state) {
        this(ModBlockEntities.TUBE_VALVE.get(), pos, state);
    }

    // ==================== NBT ====================

    @Override
    protected void writeSyncNBT(CompoundTag tag) {
        super.writeSyncNBT(tag);
        tag.putBoolean("Flow", allowFlow);
        tag.putBoolean("HadPower", wasPoweredLastTick);
    }

    @Override
    protected void readSyncNBT(CompoundTag tag) {
        super.readSyncNBT(tag);
        allowFlow = tag.getBoolean("Flow");
        wasPoweredLastTick = tag.getBoolean("HadPower");
    }

    // ==================== Tick ====================

    public static void serverTickValve(Level level, BlockPos pos, BlockState state, TileTubeValve tile) {
        // Check redstone every 5 ticks
        if (level.getGameTime() % 5 == 0) {
            boolean gettingPower = tile.gettingPower();
            
            // Rising edge - close valve
            if (!tile.wasPoweredLastTick && gettingPower && tile.allowFlow) {
                tile.allowFlow = false;
                level.playSound(null, pos, SoundEvents.WOODEN_TRAPDOOR_CLOSE, SoundSource.BLOCKS,
                        0.7f, 0.9f + level.random.nextFloat() * 0.2f);
                tile.markDirtyAndSync();
            }
            
            // Falling edge - open valve
            if (tile.wasPoweredLastTick && !gettingPower && !tile.allowFlow) {
                tile.allowFlow = true;
                level.playSound(null, pos, SoundEvents.WOODEN_TRAPDOOR_OPEN, SoundSource.BLOCKS,
                        0.7f, 0.9f + level.random.nextFloat() * 0.2f);
                tile.markDirtyAndSync();
            }
            
            tile.wasPoweredLastTick = gettingPower;
        }

        // Call parent tick for essentia transport
        TileTube.serverTick(level, pos, state, tile);
    }

    public static void clientTickValve(Level level, BlockPos pos, BlockState state, TileTubeValve tile) {
        // Animate rotation
        if (!tile.allowFlow && tile.rotation < 360.0f) {
            tile.rotation += 20.0f;
        } else if (tile.allowFlow && tile.rotation > 0.0f) {
            tile.rotation -= 20.0f;
        }

        // Call parent tick
        TileTube.clientTick(level, pos, state, tile);
    }

    // ==================== Redstone ====================

    public boolean gettingPower() {
        return level != null && level.hasNeighborSignal(worldPosition);
    }

    // ==================== Connection Override ====================

    @Override
    public boolean isConnectable(Direction face) {
        // Valve facing direction is not connectable
        return face != facing && super.isConnectable(face);
    }

    @Override
    public void setSuction(Aspect aspect, int amount) {
        // Only propagate suction when valve is open
        if (allowFlow) {
            super.setSuction(aspect, amount);
        }
    }

    // ==================== Getters ====================

    public boolean isOpen() {
        return allowFlow;
    }

    public void setOpen(boolean open) {
        this.allowFlow = open;
        markDirtyAndSync();
    }
}




