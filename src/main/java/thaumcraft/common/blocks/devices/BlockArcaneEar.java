package thaumcraft.common.blocks.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.tiles.devices.TileArcaneEar;
import thaumcraft.init.ModBlockEntities;

import javax.annotation.Nullable;

/**
 * Arcane Ear block - detects note block sounds and emits redstone.
 * Can be configured to respond to specific notes and instruments.
 */
public class BlockArcaneEar extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;

    // Shapes follow the TC4 geometry (BlockWoodenDeviceRenderer meta 1):
    // base plate (3/16) + pipe (4..12) + flared bell (8/16..16/16), rotated per FACING.
    private static final VoxelShape SHAPE_UP = Shapes.or(
            Block.box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0),
            Block.box(4.0, 3.0, 4.0, 12.0, 16.0, 12.0),
            Block.box(1.0, 8.0, 1.0, 15.0, 16.0, 15.0));
    private static final VoxelShape SHAPE_DOWN = Shapes.or(
            Block.box(0.0, 13.0, 0.0, 16.0, 16.0, 16.0),
            Block.box(4.0, 0.0, 4.0, 12.0, 13.0, 12.0),
            Block.box(1.0, 0.0, 1.0, 15.0, 8.0, 15.0));
    private static final VoxelShape SHAPE_NORTH = Shapes.or(
            Block.box(0.0, 0.0, 13.0, 16.0, 16.0, 16.0),
            Block.box(4.0, 4.0, 0.0, 12.0, 12.0, 13.0),
            Block.box(1.0, 1.0, 0.0, 15.0, 15.0, 8.0));
    private static final VoxelShape SHAPE_SOUTH = Shapes.or(
            Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 3.0),
            Block.box(4.0, 4.0, 3.0, 12.0, 12.0, 16.0),
            Block.box(1.0, 1.0, 8.0, 15.0, 15.0, 16.0));
    private static final VoxelShape SHAPE_WEST = Shapes.or(
            Block.box(13.0, 0.0, 0.0, 16.0, 16.0, 16.0),
            Block.box(0.0, 4.0, 4.0, 13.0, 12.0, 12.0),
            Block.box(0.0, 1.0, 1.0, 8.0, 15.0, 15.0));
    private static final VoxelShape SHAPE_EAST = Shapes.or(
            Block.box(0.0, 0.0, 0.0, 3.0, 16.0, 16.0),
            Block.box(3.0, 4.0, 4.0, 16.0, 12.0, 12.0),
            Block.box(8.0, 1.0, 1.0, 16.0, 15.0, 15.0));

    private final boolean isToggle;

    public BlockArcaneEar(boolean toggle) {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(1.0f)
                .sound(SoundType.WOOD)
                .noOcclusion());
        this.isToggle = toggle;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.UP)
                .setValue(ENABLED, false));
    }

    public boolean isToggleMode() {
        return isToggle;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ENABLED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        BlockPos attachPos = context.getClickedPos().relative(facing.getOpposite());
        
        if (context.getLevel().getBlockState(attachPos).isFaceSturdy(context.getLevel(), attachPos, facing)) {
            return this.defaultBlockState()
                    .setValue(FACING, facing)
                    .setValue(ENABLED, false);
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        if (!level.isClientSide) {
            BlockEntity te = level.getBlockEntity(pos);
            if (te instanceof TileArcaneEar ear) {
                ear.updateInstrument();
                ear.setToggleMode(isToggle);
            }
        }
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
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos attachPos = pos.relative(facing.getOpposite());
        return level.getBlockState(attachPos).isFaceSturdy(level, attachPos, facing);
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
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide) {
            BlockEntity te = level.getBlockEntity(pos);
            if (te instanceof TileArcaneEar ear) {
                ear.updateInstrument();
            }
        }
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity te = level.getBlockEntity(pos);
        if (te instanceof TileArcaneEar ear) {
            ear.changePitch();
            ear.triggerNote(true);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        BlockEntity te = level.getBlockEntity(pos);
        if (te instanceof TileArcaneEar ear) {
            return ear.getRedstoneOutput();
        }
        return state.getValue(ENABLED) ? 15 : 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return getSignal(state, level, pos, direction);
    }

    @Override
    public boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int data) {
        // Play note sound
        if (id >= 0 && id < 16) {
            float pitch = (float) Math.pow(2.0, (data - 12) / 12.0);
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.BLOCKS, 3.0f, pitch);
            level.addParticle(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 
                    data / 24.0, 0.0, 0.0);
            return true;
        }
        return super.triggerEvent(state, level, pos, id, data);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        TileArcaneEar ear = new TileArcaneEar(ModBlockEntities.ARCANE_EAR.get(), pos, state);
        ear.setToggleMode(isToggle);
        return ear;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return type == ModBlockEntities.ARCANE_EAR.get()
                ? (lvl, pos, st, te) -> TileArcaneEar.serverTick(lvl, pos, st, (TileArcaneEar) te)
                : null;
    }

    /**
     * Create a pulse-mode arcane ear (emits signal for 10 ticks).
     */
    public static BlockArcaneEar createPulse() {
        return new BlockArcaneEar(false);
    }

    /**
     * Create a toggle-mode arcane ear (toggles state on each note).
     */
    public static BlockArcaneEar createToggle() {
        return new BlockArcaneEar(true);
    }
}




