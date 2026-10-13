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
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.tiles.essentia.TileTube;
import thaumcraft.common.tiles.essentia.TileTubeBuffer;
import thaumcraft.common.tiles.essentia.TileTubeFilter;
import thaumcraft.common.tiles.essentia.TileTubeOneway;
import thaumcraft.common.tiles.essentia.TileTubeRestrict;
import thaumcraft.common.tiles.essentia.TileTubeValve;
import thaumcraft.init.ModBlockEntities;
import thaumcraft.init.ModItems;
import thaumcraft.init.ModSounds;

import javax.annotation.Nullable;

/**
 * Essentia tubes for transporting essentia between containers.
 * Variants:
 * - Normal: Basic transport
 * - Restricted: One-way flow
 * - Filter: Only allows specific aspect
 * - Valve: Can be toggled on/off
 * - Buffer: Stores small amount of essentia
 */
public class BlockTube extends Block implements EntityBlock {

    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    // Center tube shape
    private static final VoxelShape CENTER = Block.box(5.0, 5.0, 5.0, 11.0, 11.0, 11.0);
    
    // Connection shapes for each direction
    private static final VoxelShape NORTH_AABB = Block.box(5.0, 5.0, 0.0, 11.0, 11.0, 5.0);
    private static final VoxelShape SOUTH_AABB = Block.box(5.0, 5.0, 11.0, 11.0, 11.0, 16.0);
    private static final VoxelShape EAST_AABB = Block.box(11.0, 5.0, 5.0, 16.0, 11.0, 11.0);
    private static final VoxelShape WEST_AABB = Block.box(0.0, 5.0, 5.0, 5.0, 11.0, 11.0);
    private static final VoxelShape UP_AABB = Block.box(5.0, 11.0, 5.0, 11.0, 16.0, 11.0);
    private static final VoxelShape DOWN_AABB = Block.box(5.0, 0.0, 5.0, 11.0, 5.0, 11.0);

    public enum TubeType {
        NORMAL,
        RESTRICTED,
        FILTER,
        VALVE,
        BUFFER,
        ONEWAY
    }

    private final TubeType tubeType;

    public BlockTube(TubeType type) {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(0.5f, 5.0f)
                .sound(SoundType.METAL)
                .noOcclusion());
        this.tubeType = type;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false));
    }

    public TubeType getTubeType() {
        return tubeType;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CENTER;
        
        if (state.getValue(NORTH)) shape = Shapes.or(shape, NORTH_AABB);
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, SOUTH_AABB);
        if (state.getValue(EAST)) shape = Shapes.or(shape, EAST_AABB);
        if (state.getValue(WEST)) shape = Shapes.or(shape, WEST_AABB);
        if (state.getValue(UP)) shape = Shapes.or(shape, UP_AABB);
        if (state.getValue(DOWN)) shape = Shapes.or(shape, DOWN_AABB);
        
        return shape;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                   LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        boolean connected = canConnectTo(level, neighborPos, direction.getOpposite());
        return state.setValue(getPropertyForDirection(direction), connected);
    }

    private BooleanProperty getPropertyForDirection(Direction dir) {
        return switch (dir) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }

    private boolean canConnectTo(LevelAccessor level, BlockPos pos, Direction side) {
        BlockEntity te = level.getBlockEntity(pos);
        if (te instanceof IEssentiaTransport transport) {
            return transport.isConnectable(side);
        }
        // Also connect to other tubes
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof BlockTube;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // Special interactions for valve and filter tubes
        if (tubeType == TubeType.VALVE) {
            // TODO: Toggle valve when TileTubeValve is implemented
            return InteractionResult.CONSUME;
        }

        if (tubeType == TubeType.FILTER) {
            // TC4 BlockTube.onBlockActivated: shift + right-click removes the filter.
            // Applying a label is handled by ItemLabel through ILabelable.
            if (!level.isClientSide
                    && player.isShiftKeyDown()
                    && level.getBlockEntity(pos) instanceof TileTubeFilter filter
                    && filter.getFilter() != null) {
                filter.clearFilter();
                Containers.dropItemStack(level,
                        pos.getX() + 0.5D + hit.getDirection().getStepX() / 3.0D,
                        pos.getY() + 0.5D,
                        pos.getZ() + 0.5D + hit.getDirection().getStepZ() / 3.0D,
                        new ItemStack(ModItems.LABEL_BLANK.get()));
                level.playSound(null, pos, ModSounds.PAGE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        // TC4 BlockTube.breakBlock: a filtered tube drops its label
        if (!state.is(newState.getBlock())
                && !level.isClientSide
                && level.getBlockEntity(pos) instanceof TileTubeFilter filter
                && filter.getFilter() != null) {
            Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    new ItemStack(ModItems.LABEL_BLANK.get()));
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return tubeType == TubeType.BUFFER;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        // TODO: Return buffer fill level when TileTubeBuffer is implemented
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (tubeType) {
            case FILTER -> new TileTubeFilter(pos, state);
            case VALVE -> new TileTubeValve(pos, state);
            case BUFFER -> new TileTubeBuffer(pos, state);
            case ONEWAY -> new TileTubeOneway(pos, state);
            case RESTRICTED -> new TileTubeRestrict(pos, state);
            default -> new TileTube(pos, state);
        };
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (tubeType == TubeType.VALVE && type == ModBlockEntities.TUBE_VALVE.get()) {
            if (level.isClientSide) {
                return (lvl, pos, st, be) -> TileTubeValve.clientTickValve(lvl, pos, st, (TileTubeValve) be);
            }
            return (lvl, pos, st, be) -> TileTubeValve.serverTickValve(lvl, pos, st, (TileTubeValve) be);
        }
        if (tubeType == TubeType.BUFFER && type == ModBlockEntities.TUBE_BUFFER.get()) {
            if (!level.isClientSide) {
                return (lvl, pos, st, be) -> TileTubeBuffer.serverTickBuffer(lvl, pos, st, (TileTubeBuffer) be);
            }
            return null;
        }
        if (level.isClientSide) {
            return (lvl, pos, st, be) -> TileTube.clientTick(lvl, pos, st, (TileTube) be);
        }
        return (lvl, pos, st, be) -> TileTube.serverTick(lvl, pos, st, (TileTube) be);
    }

    /**
     * Create a normal essentia tube.
     */
    public static BlockTube createNormal() {
        return new BlockTube(TubeType.NORMAL);
    }

    /**
     * Create a restricted tube (one-way flow).
     */
    public static BlockTube createRestricted() {
        return new BlockTube(TubeType.RESTRICTED);
    }

    /**
     * Create a filter tube (aspect filtering).
     */
    public static BlockTube createFilter() {
        return new BlockTube(TubeType.FILTER);
    }

    /**
     * Create a valve tube (toggleable).
     */
    public static BlockTube createValve() {
        return new BlockTube(TubeType.VALVE);
    }

    /**
     * Create a buffer tube (small storage).
     */
    public static BlockTube createBuffer() {
        return new BlockTube(TubeType.BUFFER);
    }

    /**
     * Create a one-way tube (directional flow only).
     */
    public static BlockTube createOneway() {
        return new BlockTube(TubeType.ONEWAY);
    }
}




