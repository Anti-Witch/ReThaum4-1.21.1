package thaumcraft.common.blocks.world.taint;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.init.ModBlocks;

/**
 * BlockTaintFeature - Taint tendrils/growths that attach to surfaces.
 * 
 * Taint features:
 * - Die (convert to flux goo) when not near a taint seed
 * - Spread taint fibres
 * - Attach to surfaces (have facing direction)
 * - Spawn taint crawlers or release flux when broken
 * - Have a chance to become taint geysers when on top of vertical taint logs
 * - Emit light
 */
public class BlockTaintFeature extends DirectionalBlock implements ITaintBlock {

    @Override
    protected com.mojang.serialization.MapCodec<? extends BlockTaintFeature> codec() {
        return net.minecraft.world.level.block.state.BlockBehaviour.simpleCodec(properties -> new BlockTaintFeature());
    }

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    // Bounding boxes for each facing direction
    private static final VoxelShape SHAPE_DOWN = Block.box(2, 10, 2, 14, 16, 14);
    private static final VoxelShape SHAPE_UP = Block.box(2, 0, 2, 14, 6, 14);
    private static final VoxelShape SHAPE_NORTH = Block.box(2, 2, 10, 14, 14, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(2, 2, 0, 14, 14, 6);
    private static final VoxelShape SHAPE_WEST = Block.box(10, 2, 2, 16, 14, 14);
    private static final VoxelShape SHAPE_EAST = Block.box(0, 2, 2, 6, 14, 14);

    public BlockTaintFeature() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(0.1f)
                .sound(SoundType.SLIME_BLOCK)
                .lightLevel(state -> 10) // 0.625f * 16 в‰€ 10
                .noOcclusion()
                .randomTicks());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
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
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos attachPos = pos.relative(facing.getOpposite());
        BlockState attachState = level.getBlockState(attachPos);
        return attachState.isFaceSturdy(level, attachPos, facing);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                   LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction facing = state.getValue(FACING);
        if (direction == facing.getOpposite() && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public void die(Level level, BlockPos pos, BlockState state) {
        if (ModBlocks.FLUX_GOO != null) {
            level.setBlockAndUpdate(pos, ModBlocks.FLUX_GOO.get().defaultBlockState());
        } else {
            level.removeBlock(pos, false);
        }
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Die if not near a taint seed
        if (!TaintHelper.isNearTaintSeed(level, pos) && random.nextInt(10) == 0) {
            die(level, pos, state);
            return;
        }

        // Spread fibres
        TaintHelper.spreadFibres(level, pos);

        // Chance to become a taint geyser if on top of vertical taint log
        if (random.nextInt(100) == 0) {
            BlockState belowState = level.getBlockState(pos.below());
            if (ModBlocks.TAINT_LOG != null && 
                belowState.is(ModBlocks.TAINT_LOG.get()) &&
                belowState.getValue(BlockTaintLog.AXIS) == Direction.Axis.Y) {
                if (ModBlocks.TAINT_GEYSER != null) {
                    level.setBlockAndUpdate(pos, ModBlocks.TAINT_GEYSER.get().defaultBlockState());
                }
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            // TC4: taint features release flux into the aura when removed
            AuraHelper.polluteAura(level, pos, 1.0f, true);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) {
        // Always droppable with silk touch, but normally drops nothing
        return player.hasCorrectToolForDrops(state);
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 0; // Transparent
    }
}




