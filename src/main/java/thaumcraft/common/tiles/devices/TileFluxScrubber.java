package thaumcraft.common.tiles.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.common.tiles.TileThaumcraft;
import thaumcraft.init.ModBlockEntities;

/**
 * TileFluxScrubber - Removes flux from the aura and converts it to vitium essentia.
 * 
 * Features:
 * - Slowly drains flux from the local aura
 * - Converts flux into vitium (flux) essentia
 * - Outputs vitium to connected tubes/containers
 * - Higher flux levels increase processing speed
 * 
 * Ported from 1.12.2
 */
public class TileFluxScrubber extends TileThaumcraft implements IEssentiaTransport {

    public static final int MAX_FLUX_STORE = 100;
    public static final int OUTPUT_RATE = 5;

    // Stored flux essentia waiting to be output
    public int storedFlux = 0;

    // Processing state
    public boolean active = false;
    private int tickCount = 0;

    // Animation (client-side)
    public float rotation = 0;
    public float rotationPrev = 0;

    public TileFluxScrubber(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public TileFluxScrubber(BlockPos pos, BlockState state) {
        this(ModBlockEntities.FLUX_SCRUBBER.get(), pos, state);
    }

    // ==================== NBT ====================

    @Override
    protected void writeSyncNBT(CompoundTag tag) {
        super.writeSyncNBT(tag);
        tag.putInt("StoredFlux", storedFlux);
        tag.putBoolean("Active", active);
    }

    @Override
    protected void readSyncNBT(CompoundTag tag) {
        super.readSyncNBT(tag);
        storedFlux = tag.getInt("StoredFlux");
        active = tag.getBoolean("Active");
    }

    // ==================== Tick ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileFluxScrubber tile) {
        tile.tickCount++;

        boolean wasActive = tile.active;

        // Check flux levels and scrub
        if (tile.tickCount % 10 == 0) {
            tile.scrubFlux();
        }

        // Output stored flux essentia
        if (tile.tickCount % OUTPUT_RATE == 0 && tile.storedFlux > 0) {
            tile.outputEssentia();
        }

        // Update active state
        tile.active = tile.storedFlux > 0 || AuraHelper.getFlux(level, pos) > 0.5f;

        if (wasActive != tile.active) {
            tile.markDirtyAndSync();
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileFluxScrubber tile) {
        tile.rotationPrev = tile.rotation;

        if (tile.active) {
            // Spin when active
            float speed = 2.0f + (tile.storedFlux / (float) MAX_FLUX_STORE) * 5.0f;
            tile.rotation += speed;

            if (tile.rotation >= 360.0f) {
                tile.rotation -= 360.0f;
                tile.rotationPrev -= 360.0f;
            }
        }
    }

    /**
     * Scrub flux from the local aura.
     */
    private void scrubFlux() {
        if (level == null || storedFlux >= MAX_FLUX_STORE) return;

        float flux = AuraHelper.getFlux(level, worldPosition);
        if (flux > 0.1f) {
            // Drain flux from aura
            float toDrain = Math.min(flux, Math.min(2.0f, MAX_FLUX_STORE - storedFlux));
            float drained = AuraHelper.drainFlux(level, worldPosition, toDrain, false);
            
            if (drained > 0) {
                storedFlux += (int) Math.ceil(drained);
                
                // Play scrubbing sound occasionally
                if (level.random.nextInt(5) == 0) {
                    level.playSound(null, worldPosition, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP,
                            SoundSource.BLOCKS, 0.3f, 0.5f + level.random.nextFloat() * 0.3f);
                }
                
                markDirtyAndSync();
            }
        }
    }

    /**
     * Output stored flux as vitium essentia to connected containers.
     */
    private void outputEssentia() {
        if (level == null || storedFlux <= 0) return;

        for (Direction dir : Direction.values()) {
            if (storedFlux <= 0) break;

            BlockEntity te = level.getBlockEntity(worldPosition.relative(dir));
            if (te instanceof IEssentiaTransport transport) {
                if (transport.canInputFrom(dir.getOpposite())) {
                    int added = transport.addEssentia(Aspect.FLUX, 1, dir.getOpposite());
                    if (added > 0) {
                        storedFlux--;
                        markDirtyAndSync();
                        return; // Only output one per cycle
                    }
                }
            }
        }
    }

    // ==================== IEssentiaTransport ====================

    @Override
    public boolean isConnectable(Direction face) {
        return true;
    }

    @Override
    public boolean canInputFrom(Direction face) {
        return false; // Only outputs
    }

    @Override
    public boolean canOutputTo(Direction face) {
        return true;
    }

    @Override
    public void setSuction(Aspect aspect, int amount) {
        // No suction - only outputs
    }

    @Override
    public Aspect getSuctionType(Direction face) {
        return null;
    }

    @Override
    public int getSuctionAmount(Direction face) {
        return 0;
    }

    @Override
    public Aspect getEssentiaType(Direction face) {
        return storedFlux > 0 ? Aspect.FLUX : null;
    }

    @Override
    public int getEssentiaAmount(Direction face) {
        return storedFlux;
    }

    @Override
    public int takeEssentia(Aspect aspect, int amount, Direction face) {
        if (aspect == Aspect.FLUX && storedFlux >= amount) {
            storedFlux -= amount;
            markDirtyAndSync();
            return amount;
        }
        return 0;
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, Direction face) {
        return 0; // Can't add essentia
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    // ==================== Getters ====================

    public int getStoredFlux() {
        return storedFlux;
    }

    public float getStoredFluxPercent() {
        return (float) storedFlux / MAX_FLUX_STORE;
    }

    public boolean isActive() {
        return active;
    }
}




