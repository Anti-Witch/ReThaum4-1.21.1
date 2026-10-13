package thaumcraft.common.blocks.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

import thaumcraft.common.tiles.devices.TileMirror;
import thaumcraft.init.ModBlockEntities;

/**
 * Magic mirror for item teleportation.
 * Links to another mirror to teleport items between locations.
 * Can also teleport players when configured.
 */
public class BlockMirror extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LINKED = BooleanProperty.create("linked");

    // TC4 (BlockMirror#setBlockBoundsForBlockRender): a 1/16 thick plate flush with the
    // face the mirror hangs on - matching the block model geometry.
    private static final VoxelShape SHAPE_NORTH = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 1.0);
    private static final VoxelShape SHAPE_SOUTH = Block.box(0.0, 0.0, 15.0, 16.0, 16.0, 16.0);
    private static final VoxelShape SHAPE_EAST = Block.box(15.0, 0.0, 0.0, 16.0, 16.0, 16.0);
    private static final VoxelShape SHAPE_WEST = Block.box(0.0, 0.0, 0.0, 1.0, 16.0, 16.0);
    private static final VoxelShape SHAPE_UP = Block.box(0.0, 15.0, 0.0, 16.0, 16.0, 16.0);
    private static final VoxelShape SHAPE_DOWN = Block.box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0);

    public enum MirrorType {
        ITEM,       // Teleports items only
        PLAYER,     // Teleports players (essentia mirror)
        HAND        // Hand mirror - portable
    }

    private final MirrorType mirrorType;

    public BlockMirror(MirrorType type) {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(2.0f)
                .sound(SoundType.GLASS)
                .noOcclusion()
                .lightLevel(state -> state.getValue(LINKED) ? 7 : 0));
        this.mirrorType = type;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LINKED, false));
    }

    public MirrorType getMirrorType() {
        return mirrorType;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LINKED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(LINKED, false);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            case UP -> SHAPE_UP;
            case DOWN -> SHAPE_DOWN;
        };
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        // TODO: Handle mirror linking and configuration when TileMirror is implemented
        // - Right-click with hand mirror to link
        // - Shift-right-click to clear link
        // - Configure teleportation settings

        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileMirror(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == ModBlockEntities.MIRROR_ITEM.get()) {
            return (lvl, pos, st, be) -> TileMirror.serverTick(lvl, pos, st, (TileMirror) be);
        }
        return null;
    }

    /**
     * Create an item mirror (teleports items).
     */
    public static BlockMirror createItem() {
        return new BlockMirror(MirrorType.ITEM);
    }

    /**
     * Create a player mirror (essentia mirror - teleports players).
     */
    public static BlockMirror createPlayer() {
        return new BlockMirror(MirrorType.PLAYER);
    }
}




