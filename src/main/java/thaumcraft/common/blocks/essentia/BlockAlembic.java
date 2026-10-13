package thaumcraft.common.blocks.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import thaumcraft.common.tiles.essentia.TileAlembic;
import thaumcraft.init.ModBlockEntities;
import thaumcraft.init.ModItems;
import thaumcraft.init.ModSounds;

import javax.annotation.Nullable;

/**
 * Alembic for distilling essentia from items in a crucible.
 * Placed on top of or adjacent to a crucible to collect essentia.
 * Can be labeled to filter specific aspects.
 */
public class BlockAlembic extends Block implements EntityBlock {

    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);

    private static final int MAX_ESSENTIA = 64;

    public BlockAlembic() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(1.0f)
                .sound(SoundType.WOOD)
                .noOcclusion());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof TileAlembic alembic) {
            if (player.isShiftKeyDown()) {
                // Shift + right-click removes the label / filter
                if (alembic.getAspectFilter() != null) {
                    if (!level.isClientSide) {
                        alembic.removeAspectFilter();
                        dropLabel(level, pos, hit.getDirection());
                        level.playSound(null, pos, ModSounds.PAGE.get(), SoundSource.BLOCKS, 1.0F, 1.1F);
                    }
                    return InteractionResult.SUCCESS;
                }
                // Shift + right-click with an empty hand empties the alembic
                if (player.getMainHandItem().isEmpty()) {
                    if (!level.isClientSide) {
                        alembic.clearContents();
                        level.playSound(null, pos, ModSounds.BUBBLE.get(), SoundSource.BLOCKS, 0.5F,
                                1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.3F);
                    }
                    return InteractionResult.SUCCESS;
                }
            }
            // Labels are applied by ItemLabel through ILabelable, phials by ItemPhial
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Spawn a label item where the player removed it (TC4 drops itemResource:13).
     */
    private static void dropLabel(Level level, BlockPos pos, Direction side) {
        Containers.dropItemStack(level,
                pos.getX() + 0.5D + side.getStepX() / 3.0D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D + side.getStepZ() / 3.0D,
                new ItemStack(ModItems.LABEL_BLANK.get()));
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        // TC4 BlockMetalDevice.breakBlock: a labelled alembic drops its label
        if (!state.is(newState.getBlock())
                && !level.isClientSide
                && level.getBlockEntity(pos) instanceof TileAlembic alembic
                && alembic.getAspectFilter() != null) {
            dropLabel(level, pos, Direction.UP);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        // TODO: Return fill level when TileAlembic is implemented
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileAlembic(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // TODO: Return ticker when TileAlembic is implemented
        return null;
    }
}




