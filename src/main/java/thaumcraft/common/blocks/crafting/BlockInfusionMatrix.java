package thaumcraft.common.blocks.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

import thaumcraft.common.tiles.crafting.TileInfusionMatrix;
import thaumcraft.init.ModBlockEntities;

/**
 * The infusion altar's central matrix block.
 * This is the heart of the infusion crafting system.
 * Renders invisibly - actual visuals are handled by the tile entity renderer.
 */
public class BlockInfusionMatrix extends Block implements EntityBlock {

    // TC4: the matrix is a full solid block (rendered invisibly by the tile renderer)
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);

    public BlockInfusionMatrix() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(5.0f, 100.0f)
                .sound(SoundType.STONE)
                .noOcclusion()
                .lightLevel(state -> 10));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // Matrix is rendered by tile entity special renderer
        return RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileInfusionMatrix(pos, state);
    }

    /**
     * TC4: breaking the Runic Matrix while an infusion is running detonates the altar.
     */
    @Override
    public void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TileInfusionMatrix matrix && matrix.crafting) {
                level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        2.0f, net.minecraft.world.level.Level.ExplosionInteraction.BLOCK);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(net.minecraft.world.level.Level level, BlockState state, BlockEntityType<T> type) {
        if (type == ModBlockEntities.INFUSION_MATRIX.get()) {
            if (level.isClientSide) {
                return (lvl, pos, st, be) -> TileInfusionMatrix.clientTick(lvl, pos, st, (TileInfusionMatrix) be);
            } else {
                return (lvl, pos, st, be) -> TileInfusionMatrix.serverTick(lvl, pos, st, (TileInfusionMatrix) be);
            }
        }
        return null;
    }
}




