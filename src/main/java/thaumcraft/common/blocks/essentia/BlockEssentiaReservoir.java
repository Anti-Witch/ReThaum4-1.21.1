package thaumcraft.common.blocks.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.tiles.essentia.TileEssentiaReservoir;
import thaumcraft.init.ModBlockEntities;

import javax.annotation.Nullable;

/**
 * Essentia Reservoir - A large tank for storing essentia.
 * 
 * Stores 500 units of essentia (double a normal jar).
 * Can connect to tubes from any side.
 * Adjacent reservoirs will balance their contents.
 */
public class BlockEssentiaReservoir extends Block implements EntityBlock {

    private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0);

    public BlockEssentiaReservoir() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(2.0f)
                .sound(SoundType.METAL)
                .noOcclusion());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof TileEssentiaReservoir reservoir) {
            // TODO: Implement interaction
            // - Right-click with phial to fill/drain
            // - Apply label for filtering
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            // TODO: Spawn flux pollution for lost essentia
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof TileEssentiaReservoir reservoir) {
            return (int) (reservoir.getFillPercent() * 15);
        }
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEssentiaReservoir(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return type == ModBlockEntities.ESSENTIA_RESERVOIR.get() ?
                (lvl, pos, st, te) -> TileEssentiaReservoir.serverTick(lvl, pos, st, (TileEssentiaReservoir) te) : null;
    }
}




