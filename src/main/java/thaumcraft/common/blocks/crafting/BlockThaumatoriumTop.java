package thaumcraft.common.blocks.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
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
import thaumcraft.common.tiles.crafting.TileThaumatorium;
import thaumcraft.common.tiles.crafting.TileThaumatoriumTop;
import thaumcraft.init.ModBlockEntities;
import thaumcraft.init.ModBlocks;

import javax.annotation.Nullable;

/**
 * BlockThaumatoriumTop - The top half of the Thaumatorium multiblock.
 * 
 * This block sits above the main Thaumatorium and serves as a proxy for:
 * - Essentia transport from tubes connected above
 * - Hopper/automation access from above
 * - Player interaction (opens the same GUI as the bottom)
 * 
 * The block has no drops and is automatically removed when the thaumatorium
 * below is broken.
 * 
 * Ported from 1.12.2
 */
public class BlockThaumatoriumTop extends Block implements EntityBlock {

    // The top is mostly invisible/decorative
    private static final VoxelShape SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 10.0, 12.0);

    public BlockThaumatoriumTop() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.0f)
                .sound(SoundType.METAL)
                .noOcclusion()
                .noLootTable() // No drops - only thaumatorium drops when broken
                .requiresCorrectToolForDrops());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // Rendered as part of the thaumatorium model
        return RenderShape.INVISIBLE;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // Open the thaumatorium GUI below
        BlockEntity below = level.getBlockEntity(pos.below());
        if (below instanceof TileThaumatorium thaumatorium && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(thaumatorium, pos.below());
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState,
                                   LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        // If the block below is removed and isn't a thaumatorium, this block should break
        if (facing == Direction.DOWN) {
            if (!facingState.is(ModBlocks.THAUMATORIUM.get())) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        return super.updateShape(state, facing, facingState, level, currentPos, facingPos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            // Don't drop anything, but break the thaumatorium below
            BlockState below = level.getBlockState(pos.below());
            if (below.is(ModBlocks.THAUMATORIUM.get())) {
                level.destroyBlock(pos.below(), true);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileThaumatoriumTop(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == ModBlockEntities.THAUMATORIUM_TOP.get()) {
            if (!level.isClientSide) {
                return (lvl, pos, st, be) -> TileThaumatoriumTop.serverTick(lvl, pos, st, (TileThaumatoriumTop) be);
            }
        }
        return null;
    }
}




