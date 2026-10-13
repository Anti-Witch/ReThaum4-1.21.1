package thaumcraft.common.blocks.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import thaumcraft.common.tiles.crafting.TileResearchTable;
import thaumcraft.init.ModBlockEntities;
import thaumcraft.init.ModBlocks;

import javax.annotation.Nullable;

/**
 * Research table for performing research and completing research notes.
 * Placed on top of two tables to form a larger workstation.
 */
public class BlockResearchTable extends Block implements EntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    // Table shape - flat surface
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0.0, 12.0, 0.0, 16.0, 16.0, 16.0),  // Table top
            Block.box(1.0, 0.0, 1.0, 4.0, 12.0, 4.0),     // Leg 1
            Block.box(12.0, 0.0, 1.0, 15.0, 12.0, 4.0),   // Leg 2
            Block.box(1.0, 0.0, 12.0, 4.0, 12.0, 15.0),   // Leg 3
            Block.box(12.0, 0.0, 12.0, 15.0, 12.0, 15.0)  // Leg 4
    );

    public BlockResearchTable() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.0f)
                .sound(SoundType.WOOD)
                .noOcclusion());
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.RESEARCH_TABLE.get(), TileResearchTable::serverTick);
    }

    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> givenType, BlockEntityType<E> expectedType, BlockEntityTicker<? super E> ticker) {
        return expectedType == givenType ? (BlockEntityTicker<A>) ticker : null;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // Multiblock: requires a second research table placed adjacent
        boolean hasPartner = false;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(pos.relative(d)).is(ModBlocks.RESEARCH_TABLE.get())) {
                hasPartner = true;
                break;
            }
        }
        if (!hasPartner) {
            player.sendSystemMessage(Component.literal("Поставте два столи дослідження поруч, щоб відкрити робоче місце"));
            return InteractionResult.FAIL;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TileResearchTable tile) {
            // Sync the player's research knowledge (aspect pool etc.) right
            // before the table GUI opens, so the client always shows the same
            // pool the server works with. Login-time sync alone can be missed
            // on the client (packet sent before the local player exists).
            if (player instanceof ServerPlayer serverPlayer) {
                thaumcraft.api.capabilities.IPlayerKnowledge knowledge =
                        thaumcraft.api.capabilities.ThaumcraftCapabilities.getKnowledge(player);
                if (knowledge != null) {
                    knowledge.sync(serverPlayer);
                }
                serverPlayer.openMenu(tile, pos);
            }
            return InteractionResult.CONSUME;
        }
        
        return InteractionResult.PASS;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        // TODO: Initialize research table data
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof TileResearchTable tile) {
                tile.dropContents();
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileResearchTable(pos, state);
    }
}




