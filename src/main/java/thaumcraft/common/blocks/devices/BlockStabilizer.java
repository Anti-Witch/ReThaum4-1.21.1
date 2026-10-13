package thaumcraft.common.blocks.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.tiles.devices.TileStabilizer;
import thaumcraft.init.ModBlockEntities;

import javax.annotation.Nullable;

/**
 * Runic matrix stabilizer for infusion crafting.
 * Provides significant stabilization bonus to nearby infusion altar.
 * More effective than candles but requires essentia to operate.
 */
public class BlockStabilizer extends Block implements EntityBlock {

    // TC4's stabilizer (metadata 9) used the full block bounds even though the
    // base model is only 12/16 high - the pistons extend above it.
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);

    private static final float STABILIZATION_BONUS = 0.25f;

    public BlockStabilizer() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(3.0f)
                .sound(SoundType.STONE)
                .noOcclusion()
                .lightLevel(state -> 4));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /**
     * Returns the stabilization bonus for infusion crafting.
     */
    public float getStabilizationBonus() {
        return STABILIZATION_BONUS;
    }

    /**
     * Check if this stabilizer can currently stabilize infusion.
     * Requires essentia to function.
     */
    public boolean canStabilizeInfusion(Level level, BlockPos pos) {
        // TODO: Check if has essentia when TileStabilizer is implemented
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileStabilizer(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != ModBlockEntities.STABILIZER.get()) {
            return null;
        }
        if (level.isClientSide) {
            return (lvl, pos, st, be) -> TileStabilizer.clientTick(lvl, pos, st, (TileStabilizer) be);
        }
        return (lvl, pos, st, be) -> TileStabilizer.serverTick(lvl, pos, st, (TileStabilizer) be);
    }
}




