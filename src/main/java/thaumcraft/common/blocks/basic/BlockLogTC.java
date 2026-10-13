package thaumcraft.common.blocks.basic;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;

/**
 * Log blocks for greatwood and silverwood trees.
 */
public class BlockLogTC extends RotatedPillarBlock {

    public BlockLogTC(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(AXIS, Direction.Axis.Y));
    }

    /**
     * Creates greatwood log.
     */
    public static BlockLogTC createGreatwood() {
        return new BlockLogTC(
            Properties.of()
                .mapColor(state -> state.getValue(AXIS) == Direction.Axis.Y ? 
                    MapColor.COLOR_BROWN : MapColor.PODZOL)
                .strength(2.0f)
                .sound(SoundType.WOOD)
        );
    }

    /**
     * Creates silverwood log.
     */
    public static BlockLogTC createSilverwood() {
        return new BlockLogTC(
            Properties.of()
                .mapColor(state -> state.getValue(AXIS) == Direction.Axis.Y ? 
                    MapColor.QUARTZ : MapColor.WOOL)
                .strength(2.0f)
                .sound(SoundType.WOOD)
                .lightLevel(state -> 7)  // Silverwood glows slightly
        );
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(AXIS, context.getClickedFace().getAxis());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }
}




