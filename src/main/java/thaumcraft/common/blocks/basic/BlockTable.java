package thaumcraft.common.blocks.basic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.blocks.BlockTC;

/**
 * Wooden/Stone Table - decorative blocks (TC4 style).
 * In TC4 these are metadata variants of the Table block (meta 0-1).
 * No conversion mechanic - Research Table and Arcane Workbench are crafted separately.
 */
public class BlockTable extends BlockTC {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 14, 16);

    private final boolean isWood;

    public BlockTable(boolean isWood) {
        super(BlockBehaviour.Properties.of()
                .mapColor(isWood ? net.minecraft.world.level.material.MapColor.WOOD : net.minecraft.world.level.material.MapColor.STONE)
                .strength(isWood ? 2.5f : 2.0f)
                .sound(isWood ? SoundType.WOOD : SoundType.STONE)
                .noOcclusion());
        this.isWood = isWood;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH));
    }

    public static BlockTable createWooden() {
        return new BlockTable(true);
    }

    public static BlockTable createStone() {
        return new BlockTable(false);
    }

    /**
     * Whether this is the wooden table variant (only wooden tables can be
     * converted into a research table with an inkwell - TC4 behaviour).
     */
    public boolean isWooden() {
        return isWood;
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void neighborChanged(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        // Tables don't need special neighbor logic
    }

    @Override
    public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.isFaceSturdy(level, pos, Direction.UP);
    }
}
