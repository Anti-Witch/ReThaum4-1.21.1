package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import javax.annotation.Nullable;

/**
 * Base class for Thaumcraft device blocks that have a tile entity and can face different directions.
 */
public abstract class BlockTCDevice extends Block implements EntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;

    public BlockTCDevice(Properties properties) {
        super(properties);
        
        // Set default state based on what properties this block has
        BlockState defaultState = this.stateDefinition.any();
        if (hasFacing()) {
            defaultState = defaultState.setValue(FACING, Direction.NORTH);
        }
        if (hasEnabled()) {
            defaultState = defaultState.setValue(ENABLED, true);
        }
        this.registerDefaultState(defaultState);
    }

    /**
     * Override to return true if this device block should have a FACING property.
     */
    protected boolean hasFacing() {
        return true;
    }

    /**
     * Override to return true if this device block should have an ENABLED property.
     */
    protected boolean hasEnabled() {
        return false;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        if (hasFacing()) {
            builder.add(FACING);
        }
        if (hasEnabled()) {
            builder.add(ENABLED);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = this.defaultBlockState();
        
        if (hasFacing()) {
            Direction facing = context.getPlayer() != null && context.getPlayer().isShiftKeyDown()
                    ? context.getHorizontalDirection()
                    : context.getHorizontalDirection().getOpposite();
            state = state.setValue(FACING, facing);
        }
        
        if (hasEnabled()) {
            state = state.setValue(ENABLED, !context.getLevel().hasNeighborSignal(context.getClickedPos()));
        }
        
        return state;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        
        if (hasEnabled() && !level.isClientSide) {
            boolean shouldBeEnabled = !level.hasNeighborSignal(pos);
            if (state.getValue(ENABLED) != shouldBeEnabled) {
                level.setBlock(pos, state.setValue(ENABLED, shouldBeEnabled), 3);
            }
        }
    }

    @Nullable
    @Override
    public abstract BlockEntity newBlockEntity(BlockPos pos, BlockState state);

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null; // Override in subclasses if ticking is needed
    }

    /**
     * Helper method for creating a ticker.
     */
    @SuppressWarnings("unchecked")
    protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> givenType, BlockEntityType<E> expectedType, BlockEntityTicker<? super E> ticker) {
        return expectedType == givenType ? (BlockEntityTicker<A>) ticker : null;
    }
}




