package thaumcraft.common.blocks.world.taint;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;
import thaumcraft.init.ModBlocks;

/**
 * BlockTaintLog - Tainted wood logs with axis rotation property.
 * 
 * Taint logs:
 * - Die (convert to flux goo) when not near a taint seed
 * - Spread taint fibres
 * - Can sustain leaves and count as wood
 * - Have the same AXIS property as vanilla logs
 */
public class BlockTaintLog extends RotatedPillarBlock implements ITaintBlock {

    public BlockTaintLog() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(3.0f, 100.0f)
                .sound(SoundType.SLIME_BLOCK)
                .randomTicks());
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.Y));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(AXIS, context.getClickedFace().getAxis());
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 4;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 4;
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
        if (!TaintHelper.isNearTaintSeed(level, pos)) {
            die(level, pos, state);
        } else {
            TaintHelper.spreadFibres(level, pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            // Trigger leaf decay in a radius around the removed log
            int radius = 4;
            int checkRadius = radius + 1;
            
            if (level.isAreaLoaded(pos, checkRadius)) {
                for (BlockPos checkPos : BlockPos.betweenClosed(
                        pos.offset(-radius, -radius, -radius), 
                        pos.offset(radius, radius, radius))) {
                    BlockState checkState = level.getBlockState(checkPos);
                    // In 1.20.1, leaves handle their own decay via randomTick
                    // We can trigger a block update to speed it up
                    if (checkState.is(net.minecraft.tags.BlockTags.LEAVES)) {
                        level.scheduleTick(checkPos.immutable(), checkState.getBlock(), 1);
                    }
                }
            }
            
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }
}




