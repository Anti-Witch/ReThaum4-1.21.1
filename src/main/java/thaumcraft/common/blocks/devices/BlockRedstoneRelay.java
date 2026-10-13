package thaumcraft.common.blocks.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.tiles.devices.TileRedstoneRelay;
import thaumcraft.init.ModBlockEntities;

import javax.annotation.Nullable;

/**
 * Redstone Relay block - converts redstone signal strength.
 * Has two clickable dials: input threshold and output strength.
 * Only outputs when input signal >= threshold.
 */
public class BlockRedstoneRelay extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;

    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0);

    public BlockRedstoneRelay() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.NONE)
                .strength(0.0f)
                .sound(SoundType.WOOD)
                .noOcclusion()
                .noCollission());
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ENABLED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ENABLED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (!canSurviveOn(context.getLevel(), context.getClickedPos().below())) {
            return null;
        }
        Direction facing = context.getPlayer() != null && context.getPlayer().isShiftKeyDown()
                ? context.getHorizontalDirection()
                : context.getHorizontalDirection().getOpposite();
        return this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(ENABLED, false);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide) {
            if (shouldBePowered(level, pos, state)) {
                level.scheduleTick(pos, this, 1);
            }
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return canSurviveOn(level, pos.below());
    }

    private boolean canSurviveOn(LevelReader level, BlockPos pos) {
        return level.getBlockState(pos).isFaceSturdy(level, pos, Direction.UP);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide) {
            if (!canSurvive(state, level, pos)) {
                dropResources(state, level, pos);
                level.removeBlock(pos, false);
                for (Direction dir : Direction.values()) {
                    level.updateNeighborsAt(pos.relative(dir), this);
                }
            } else {
                updateState(level, pos, state);
            }
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean shouldBePowered = shouldBePowered(level, pos, state);
        boolean isPowered = state.getValue(ENABLED);
        
        if (isPowered && !shouldBePowered) {
            level.setBlock(pos, state.setValue(ENABLED, false), 2);
            notifyNeighbors(level, pos, state);
        } else if (!isPowered && shouldBePowered) {
            level.setBlock(pos, state.setValue(ENABLED, true), 2);
            notifyNeighbors(level, pos, state);
        }
    }

    private void updateState(Level level, BlockPos pos, BlockState state) {
        boolean shouldBePowered = shouldBePowered(level, pos, state);
        boolean isPowered = state.getValue(ENABLED);
        
        if (isPowered != shouldBePowered && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, 2);
        }
    }

    private boolean shouldBePowered(Level level, BlockPos pos, BlockState state) {
        int threshold = 1;
        BlockEntity te = level.getBlockEntity(pos);
        if (te instanceof TileRedstoneRelay relay) {
            threshold = relay.getInputThreshold();
        }
        return calculateInputStrength(level, pos, state) >= threshold;
    }

    private int calculateInputStrength(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        BlockPos inputPos = pos.relative(facing);
        int power = level.getSignal(inputPos, facing);
        
        if (power >= 15) return power;
        
        BlockState inputState = level.getBlockState(inputPos);
        if (inputState.is(Blocks.REDSTONE_WIRE)) {
            return Math.max(power, inputState.getValue(RedStoneWireBlock.POWER));
        }
        return power;
    }

    private void notifyNeighbors(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        BlockPos outputPos = pos.relative(facing.getOpposite());
        level.neighborChanged(outputPos, this, pos);
        level.updateNeighborsAtExceptFromFacing(outputPos, this, facing);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getAbilities().mayBuild) {
            return InteractionResult.PASS;
        }

        BlockEntity te = level.getBlockEntity(pos);
        if (te instanceof TileRedstoneRelay relay) {
            // Determine which dial was clicked based on hit position
            Vec3 hitVec = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
            Direction facing = state.getValue(FACING);
            
            boolean clickedOutput = isOutputDialHit(hitVec, facing);
            boolean clickedInput = isInputDialHit(hitVec, facing);
            
            if (clickedOutput) {
                if (!level.isClientSide) {
                    relay.increaseOutput();
                    level.playSound(null, pos, SoundEvents.WOODEN_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.5f, 1.0f);
                    updateState(level, pos, state);
                    notifyNeighbors(level, pos, state);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            } else if (clickedInput) {
                if (!level.isClientSide) {
                    relay.increaseInput();
                    level.playSound(null, pos, SoundEvents.WOODEN_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.5f, 1.0f);
                    updateState(level, pos, state);
                    notifyNeighbors(level, pos, state);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return InteractionResult.PASS;
    }

    private boolean isOutputDialHit(Vec3 hit, Direction facing) {
        // Output dial is towards the back (output side)
        return switch (facing) {
            case NORTH -> hit.z > 0.5 && hit.x > 0.3 && hit.x < 0.7;
            case SOUTH -> hit.z < 0.5 && hit.x > 0.3 && hit.x < 0.7;
            case WEST -> hit.x > 0.5 && hit.z > 0.3 && hit.z < 0.7;
            case EAST -> hit.x < 0.5 && hit.z > 0.3 && hit.z < 0.7;
            default -> false;
        };
    }

    private boolean isInputDialHit(Vec3 hit, Direction facing) {
        // Input dial is towards the front (input side)
        return switch (facing) {
            case NORTH -> hit.z < 0.5 && hit.x > 0.3 && hit.x < 0.7;
            case SOUTH -> hit.z > 0.5 && hit.x > 0.3 && hit.x < 0.7;
            case WEST -> hit.x < 0.5 && hit.z > 0.3 && hit.z < 0.7;
            case EAST -> hit.x > 0.5 && hit.z > 0.3 && hit.z < 0.7;
            default -> false;
        };
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!isMoving && !state.is(newState.getBlock())) {
            super.onRemove(state, level, pos, newState, isMoving);
            if (state.getValue(ENABLED)) {
                for (Direction dir : Direction.values()) {
                    level.updateNeighborsAt(pos.relative(dir), this);
                }
            }
        }
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (!state.getValue(ENABLED)) return 0;
        
        Direction facing = state.getValue(FACING);
        if (direction != facing) return 0;
        
        BlockEntity te = level.getBlockEntity(pos);
        if (te instanceof TileRedstoneRelay relay) {
            return relay.getOutputStrength();
        }
        return 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return getSignal(state, level, pos, direction);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileRedstoneRelay(ModBlockEntities.REDSTONE_RELAY.get(), pos, state);
    }
}




