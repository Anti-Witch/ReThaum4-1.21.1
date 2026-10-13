package thaumcraft.common.blocks.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import thaumcraft.client.fx.FXDispatcher;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Nitor blocks - floating magical light sources.
 * 16 color variants available.
 * Renders invisibly - actual visuals come from particle effects.
 */
public class BlockNitor extends Block {

    private static final VoxelShape SHAPE = Block.box(5.0, 5.0, 5.0, 11.0, 11.0, 11.0);
    
    private final DyeColor color;

    public BlockNitor(DyeColor color) {
        super(BlockBehaviour.Properties.of()
                .mapColor(getMapColorForDye(color))
                .strength(0.1f)
                .sound(SoundType.WOOL)
                .lightLevel(state -> 15)
                .noOcclusion()
                .noCollission()
                .replaceable());
        this.color = color;
    }

    private static MapColor getMapColorForDye(DyeColor dye) {
        return switch (dye) {
            case WHITE -> MapColor.SNOW;
            case ORANGE -> MapColor.COLOR_ORANGE;
            case MAGENTA -> MapColor.COLOR_MAGENTA;
            case LIGHT_BLUE -> MapColor.COLOR_LIGHT_BLUE;
            case YELLOW -> MapColor.COLOR_YELLOW;
            case LIME -> MapColor.COLOR_LIGHT_GREEN;
            case PINK -> MapColor.COLOR_PINK;
            case GRAY -> MapColor.COLOR_GRAY;
            case LIGHT_GRAY -> MapColor.COLOR_LIGHT_GRAY;
            case CYAN -> MapColor.COLOR_CYAN;
            case PURPLE -> MapColor.COLOR_PURPLE;
            case BLUE -> MapColor.COLOR_BLUE;
            case BROWN -> MapColor.COLOR_BROWN;
            case GREEN -> MapColor.COLOR_GREEN;
            case RED -> MapColor.COLOR_RED;
            case BLACK -> MapColor.COLOR_BLACK;
        };
    }

    public DyeColor getColor() {
        return color;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // Nitor is invisible - it's rendered via particles
        return RenderShape.INVISIBLE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!level.isClientSide()) return;
        // Spawn Thaumcraft colored flame particles tinted by the nitor's dye color
        double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.3;
        double y = pos.getY() + 0.5 + (random.nextDouble() - 0.5) * 0.3;
        double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.3;

        int color = this.color.getTextureDiffuseColor();
        FXDispatcher.INSTANCE.drawNitorCore(x, y, z, 0.0, 0.0, 0.0);
        FXDispatcher.INSTANCE.drawNitorFlames(x, y, z, 0.0, 0.0, 0.0, color, 0);
    }

    /**
     * Create a nitor of a specific color.
     */
    public static BlockNitor create(DyeColor color) {
        return new BlockNitor(color);
    }
}




