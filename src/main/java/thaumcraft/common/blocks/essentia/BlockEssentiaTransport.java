package thaumcraft.common.blocks.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.Level;
import thaumcraft.common.tiles.essentia.TileEssentiaInput;
import thaumcraft.common.tiles.essentia.TileEssentiaOutput;
import thaumcraft.init.ModBlockEntities;

import javax.annotation.Nullable;

/**
 * Essentia transport blocks for connecting to machines that produce or consume essentia.
 * 
 * - Input: Pulls essentia from tubes and adds it to adjacent containers (high suction)
 * - Output: Takes essentia from adjacent sources and pushes to tubes (no suction)
 */
public class BlockEssentiaTransport extends Block implements EntityBlock {
    
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    
    // Bounding boxes for each facing direction (pipe extending from center)
    private static final VoxelShape SHAPE_DOWN = Block.box(4.0, 8.0, 4.0, 12.0, 16.0, 12.0);
    private static final VoxelShape SHAPE_UP = Block.box(4.0, 0.0, 4.0, 12.0, 8.0, 12.0);
    private static final VoxelShape SHAPE_NORTH = Block.box(4.0, 4.0, 8.0, 12.0, 12.0, 16.0);
    private static final VoxelShape SHAPE_SOUTH = Block.box(4.0, 4.0, 0.0, 12.0, 12.0, 8.0);
    private static final VoxelShape SHAPE_WEST = Block.box(8.0, 4.0, 4.0, 16.0, 12.0, 12.0);
    private static final VoxelShape SHAPE_EAST = Block.box(0.0, 4.0, 4.0, 8.0, 12.0, 12.0);
    
    public enum TransportType {
        INPUT,
        OUTPUT
    }
    
    private final TransportType transportType;
    
    public BlockEssentiaTransport(TransportType type) {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(1.0f, 10.0f)
                .sound(SoundType.METAL)
                .noOcclusion());
        this.transportType = type;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.UP));
    }
    
    public TransportType getTransportType() {
        return transportType;
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
    
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Place facing the clicked face
        return this.defaultBlockState().setValue(FACING, context.getClickedFace());
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return switch (facing) {
            case DOWN -> SHAPE_DOWN;
            case UP -> SHAPE_UP;
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            case EAST -> SHAPE_EAST;
        };
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return transportType == TransportType.INPUT 
                ? new TileEssentiaInput(pos, state) 
                : new TileEssentiaOutput(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        
        if (transportType == TransportType.INPUT) {
            return type == ModBlockEntities.ESSENTIA_INPUT.get() 
                    ? (lvl, pos, st, be) -> ((TileEssentiaInput) be).serverTick() 
                    : null;
        } else {
            return type == ModBlockEntities.ESSENTIA_OUTPUT.get() 
                    ? (lvl, pos, st, be) -> ((TileEssentiaOutput) be).serverTick() 
                    : null;
        }
    }
}




