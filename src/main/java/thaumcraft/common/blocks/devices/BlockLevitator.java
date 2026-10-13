package thaumcraft.common.blocks.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.tiles.devices.TileLevitator;
import thaumcraft.init.ModSounds;
import thaumcraft.init.ModBlockEntities;

import javax.annotation.Nullable;

/**
 * BlockLevitator - Arcane Levitator that pushes entities in a direction.
 * 
 * The levitator uses vis from the aura to create a force field that
 * pushes entities (items, mobs, players) in the direction it faces.
 * Right-click to cycle through range settings (4, 8, 16, 32 blocks).
 * Disabled by redstone signal.
 */
public class BlockLevitator extends Block implements EntityBlock {
    
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    
    // Shapes for each facing direction (slightly indented on the back)
    // The block is 7/8 (0.875) of full size in the facing direction
    private static final float INDENT = 0.125f; // 2 pixels indent
    
    private static final VoxelShape SHAPE_DOWN = Block.box(0, 2, 0, 16, 16, 16);
    private static final VoxelShape SHAPE_UP = Block.box(0, 0, 0, 16, 14, 16);
    private static final VoxelShape SHAPE_NORTH = Block.box(0, 0, 2, 16, 16, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(0, 0, 0, 16, 16, 14);
    private static final VoxelShape SHAPE_WEST = Block.box(2, 0, 0, 16, 16, 16);
    private static final VoxelShape SHAPE_EAST = Block.box(0, 0, 0, 14, 16, 16);
    
    public BlockLevitator() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(1.5f)
                .sound(SoundType.WOOD)
                .noOcclusion());
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.UP));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
    
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Place facing the direction the player clicked from (pointing toward player)
        return this.defaultBlockState()
                .setValue(FACING, context.getClickedFace());
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case DOWN -> SHAPE_DOWN;
            case UP -> SHAPE_UP;
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            case EAST -> SHAPE_EAST;
        };
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
    
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TileLevitator levitator) {
            // Right-click to cycle range
            levitator.increaseRange(player);
            level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    ModSounds.KEY.get(), SoundSource.BLOCKS, 0.5f, 1.0f);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
    
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, 
                                 BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        // Levitator responds to redstone - TileLevitator checks hasNeighborSignal
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileLevitator(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, 
                                                                    BlockEntityType<T> type) {
        if (type == ModBlockEntities.LEVITATOR.get()) {
            if (level.isClientSide) {
                return (lvl, pos, st, be) -> TileLevitator.clientTick(lvl, pos, st, (TileLevitator) be);
            } else {
                return (lvl, pos, st, be) -> TileLevitator.serverTick(lvl, pos, st, (TileLevitator) be);
            }
        }
        return null;
    }
}




