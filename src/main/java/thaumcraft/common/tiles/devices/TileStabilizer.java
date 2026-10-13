package thaumcraft.common.tiles.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.common.tiles.TileThaumcraft;
import thaumcraft.init.ModBlockEntities;

/**
 * Stabilizer tile entity - reduces instability of nearby flux rifts and infusion altars.
 * Slowly generates energy by polluting the aura, then uses it to stabilize rifts.
 */
public class TileStabilizer extends TileThaumcraft {

    private static final int RANGE = 8;
    private static final int CAPACITY = 15;
    private static final float POLLUTION_AMOUNT = 0.25f;

    private int ticks = 0;
    private int delay = 0;
    protected int energy = 0;
    private int lastEnergy = 0;

    public TileStabilizer(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public TileStabilizer(BlockPos pos, BlockState state) {
        this(ModBlockEntities.STABILIZER.get(), pos, state);
    }

    // ==================== NBT ====================

    @Override
    protected void writeSyncNBT(CompoundTag tag) {
        super.writeSyncNBT(tag);
        tag.putInt("Energy", energy);
    }

    @Override
    protected void readSyncNBT(CompoundTag tag) {
        super.readSyncNBT(tag);
        energy = Math.min(tag.getInt("Energy"), CAPACITY);
    }

    // ==================== Tick ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileStabilizer tile) {
        tile.ticks++;

        // Slowly generate energy by polluting aura
        if (tile.energy < CAPACITY && tile.ticks % 20 == 0) {
            tile.energy++;
            AuraHelper.polluteAura(level, pos, POLLUTION_AMOUNT, true);
            tile.setChanged();
            tile.syncTile(false);
            level.updateNeighborsAt(pos, state.getBlock());
        }

        // Try to stabilize flux rifts
        if (tile.energy > 0 && tile.delay <= 0 && tile.ticks % 5 == 0) {
            int previousEnergy = tile.energy;
            tile.tryAddStability();
            if (previousEnergy != tile.energy) {
                tile.setChanged();
                tile.syncTile(false);
            }
        }

        if (tile.delay > 0) {
            tile.delay--;
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileStabilizer tile) {
        // Update rendering when energy changes
        if (tile.energy != tile.lastEnergy) {
            level.sendBlockUpdated(pos, state, state, 3);
            tile.lastEnergy = tile.energy;
        }
    }

    /**
     * Try to add stability to nearby flux rifts.
     */
    private void tryAddStability() {
        if (level == null) return;

        AABB area = new AABB(worldPosition).inflate(RANGE);

        for (thaumcraft.common.entities.EntityFluxRift rift :
                level.getEntitiesOfClass(thaumcraft.common.entities.EntityFluxRift.class, area)) {
            if (rift.isRemoved()) continue;
            if (rift.getStability() == thaumcraft.common.entities.EntityFluxRift.EnumStability.VERY_STABLE) continue;

            if (mitigate(1)) {
                rift.addStability(5.0f);
                delay += 5;
                if (energy <= 0) return;
            }
        }
    }

    private Direction getFacing() {
        BlockState state = getBlockState();
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.getValue(BlockStateProperties.FACING);
        }
        return Direction.UP;
    }

    // ==================== Public API ====================

    /**
     * Get the current energy level.
     */
    public int getEnergy() {
        return energy;
    }

    /**
     * Get the maximum capacity.
     */
    public int getCapacity() {
        return CAPACITY;
    }

    /**
     * Try to consume energy for mitigation.
     * Used by infusion matrix and flux rifts to reduce instability.
     * 
     * @param amount Amount of energy to consume
     * @return true if successful
     */
    public boolean mitigate(int amount) {
        if (energy >= amount) {
            energy -= amount;
            if (level != null) {
                level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
            }
            return true;
        }
        return false;
    }

    // ==================== Render Bounds ====================

    /**
     * Custom render bounding box for the spinning stabilizer top.
     */
    public AABB getCustomRenderBoundingBox() {
        return new AABB(
            worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
            worldPosition.getX() + 1, worldPosition.getY() + 1.5, worldPosition.getZ() + 1
        );
    }
}




