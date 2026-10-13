package thaumcraft.common.blocks.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
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
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.server.level.ServerPlayer;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.tiles.crafting.TileDeconstructionTable;
import thaumcraft.init.ModBlockEntities;

import javax.annotation.Nullable;

/**
 * BlockDeconstructionTable - the TC4 Deconstruction Table.
 *
 * In TC4 this was metadata 14 of the Table block (BlockTable). The port uses
 * one block per device, so the deconstruction table got its own block.
 * Crafted with an arcane recipe (research DECONSTRUCTOR).
 */
public class BlockDeconstructionTable extends Block implements EntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    /** Same shape as the arcane workbench - a table with a top and four legs. */
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 15.0, 16.0);

    public BlockDeconstructionTable() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.5f)
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
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof TileDeconstructionTable table && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(table, pos);
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof TileDeconstructionTable table) {
                table.dropContents();
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileDeconstructionTable(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return type == ModBlockEntities.DECONSTRUCTION_TABLE.get()
                ? (lvl, pos, blockState, tile) -> TileDeconstructionTable.serverTick(lvl, pos, blockState, (TileDeconstructionTable) tile)
                : null;
    }

    @Override
    public boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        if (id == TileDeconstructionTable.EVENT_ASPECT_FOUND && level.isClientSide) {
            // Aspect discovered - sprinkle particles in the colour of the aspect
            Aspect aspect = TileDeconstructionTable.getAspectByIndex(param);
            if (aspect != null) {
                int color = aspect.getColor();
                float r = ((color >> 16) & 0xFF) / 255.0f;
                float g = ((color >> 8) & 0xFF) / 255.0f;
                float b = (color & 0xFF) / 255.0f;
                for (int i = 0; i < 8; i++) {
                    double x = pos.getX() + 0.25 + level.random.nextDouble() * 0.5;
                    double y = pos.getY() + 1.0 + level.random.nextDouble() * 0.35;
                    double z = pos.getZ() + 0.25 + level.random.nextDouble() * 0.5;
                    FXDispatcher.INSTANCE.sparkle((float) x, (float) y, (float) z, r, g, b);
                }
            }
            return true;
        }
        return super.triggerEvent(state, level, pos, id, param);
    }
}
