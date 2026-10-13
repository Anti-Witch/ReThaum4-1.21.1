package thaumcraft.common.blocks.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.init.ModBlocks;

/**
 * Brain Box - Attaches to the Thaumatorium to enable automated crafting.
 * 
 * This block can only be placed on the side of a Thaumatorium or ThaumatoriumTop block.
 * When the adjacent thaumatorium is removed, this block drops as an item.
 * 
 * The brain box allows the Thaumatorium to auto-select recipes and craft items
 * when the required essentia is available.
 */
public class BlockBrainBox extends Block {
    
    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.Plane.HORIZONTAL);
    
    // TC4 BlockMetalDeviceRenderer (meta 12): a 3..13 cube plus a 4x4x3 mounting
    // post that sticks out of the side the brain box is attached to (FACING).
    private static final VoxelShape CORE = Block.box(3, 3, 3, 13, 13, 13);
    private static final VoxelShape SHAPE_NORTH = Shapes.or(CORE, Block.box(6, 6, 0, 10, 10, 3));
    private static final VoxelShape SHAPE_SOUTH = Shapes.or(CORE, Block.box(6, 6, 13, 10, 10, 16));
    private static final VoxelShape SHAPE_WEST = Shapes.or(CORE, Block.box(0, 6, 6, 3, 10, 10));
    private static final VoxelShape SHAPE_EAST = Shapes.or(CORE, Block.box(13, 6, 6, 16, 10, 10));
    
    public BlockBrainBox() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(1.0f, 10.0f)
                .sound(SoundType.METAL)
                .noOcclusion());
        
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_EAST;
        };
    }
    
    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return false;
    }
    
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos targetPos = context.getClickedPos().relative(facing);
        Level level = context.getLevel();
        
        // Can only place on the side of a thaumatorium
        if (isThaumatoriumBlock(level.getBlockState(targetPos))) {
            return defaultBlockState().setValue(FACING, facing);
        }
        
        // Try other horizontal directions
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos checkPos = context.getClickedPos().relative(dir);
            if (isThaumatoriumBlock(level.getBlockState(checkPos))) {
                return defaultBlockState().setValue(FACING, dir);
            }
        }
        
        return null; // Can't place if not adjacent to thaumatorium
    }
    
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                   LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        Direction facing = state.getValue(FACING);
        
        // Check if the attached thaumatorium is still there
        if (direction == facing) {
            if (!isThaumatoriumBlock(neighborState)) {
                // Thaumatorium removed - drop this block
                return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
            }
        }
        
        return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
    }
    
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        Direction facing = state.getValue(FACING);
        BlockPos thaumatoriumPos = pos.relative(facing);
        
        // Check if attached thaumatorium is still valid
        if (!isThaumatoriumBlock(level.getBlockState(thaumatoriumPos))) {
            level.destroyBlock(pos, true);
        }
    }
    
    @Override
    public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos thaumatoriumPos = pos.relative(facing);
        return isThaumatoriumBlock(level.getBlockState(thaumatoriumPos));
    }
    
    /**
     * Checks if a block state is a thaumatorium or thaumatorium top block.
     */
    private boolean isThaumatoriumBlock(BlockState state) {
        return state.is(ModBlocks.THAUMATORIUM.get()) || state.is(ModBlocks.THAUMATORIUM_TOP.get());
    }
    
    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) {
        return true;
    }
    
    // Factory method
    public static BlockBrainBox create() {
        return new BlockBrainBox();
    }
}




