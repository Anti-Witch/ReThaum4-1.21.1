package thaumcraft.common.blocks.basic;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import thaumcraft.common.blocks.BlockTC;

/**
 * Obsidian Totem pillar segment (TC4 blockCosmeticSolid meta 0).
 * Texture selection mirrors TC4 BlockCosmeticSolid.getIcon:
 * - totem above            -> shaded sides (obsidiantotembaseshaded)
 * - topmost with totem below -> body sides (obsidiantotem1-4, pseudo-random)
 * - standalone             -> base sides (obsidiantotembase)
 * Top/bottom faces always use obsidiantile.
 */
public class BlockObsidianTotem extends BlockTC {

    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 3);

    public BlockObsidianTotem() {
        this(props());
    }

    public BlockObsidianTotem(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(UP, false)
                .setValue(DOWN, false)
                .setValue(VARIANT, 0));
    }

    public static BlockBehaviour.Properties props() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(30.0f, 999.0f)
                .sound(SoundType.STONE);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UP, DOWN, VARIANT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        return defaultBlockState()
                .setValue(UP, isConnected(context.getLevel().getBlockState(pos.above())))
                .setValue(DOWN, isConnected(context.getLevel().getBlockState(pos.below())))
                .setValue(VARIANT, RandomSource.create(pos.asLong()).nextInt(4));
    }

    @Override
    protected BlockState updateShape(BlockState state, net.minecraft.core.Direction direction,
                                     BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == net.minecraft.core.Direction.UP) {
            return state.setValue(UP, isConnected(neighborState));
        }
        if (direction == net.minecraft.core.Direction.DOWN) {
            return state.setValue(DOWN, isConnected(neighborState));
        }
        return state;
    }

    private static boolean isConnected(BlockState state) {
        return state.getBlock() instanceof BlockObsidianTotem;
    }
}
