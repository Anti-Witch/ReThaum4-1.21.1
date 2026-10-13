package thaumcraft.common.blocks.devices;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.api.crafting.IInfusionStabiliserExt;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.tiles.devices.TileStabilizer;
import thaumcraft.init.ModBlocks;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Inlay block - golden wire decoration that connects to pedestals and stabilizers.
 * Works like redstone wire but for infusion stabilization energy.
 * 
 * When placed near a Stabilizer block, it will conduct energy along its path
 * to connected pedestals, providing a small stability bonus for infusion crafting.
 * 
 * The charge property (0-15) indicates how much stabilization energy is flowing.
 */
public class BlockInlay extends Block implements IInfusionStabiliserExt {

    // Connection properties for each direction
    public static final EnumProperty<EnumAttachPosition> NORTH = EnumProperty.create("north", EnumAttachPosition.class);
    public static final EnumProperty<EnumAttachPosition> EAST = EnumProperty.create("east", EnumAttachPosition.class);
    public static final EnumProperty<EnumAttachPosition> SOUTH = EnumProperty.create("south", EnumAttachPosition.class);
    public static final EnumProperty<EnumAttachPosition> WEST = EnumProperty.create("west", EnumAttachPosition.class);
    public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, 15);

    // Bounding boxes for different connection states
    protected static final VoxelShape[] WIRE_AABB = new VoxelShape[]{
        // No connections: center box
        Block.box(3.0, 0.0, 3.0, 13.0, 1.0, 13.0),
        // North only
        Block.box(3.0, 0.0, 0.0, 13.0, 1.0, 13.0),
        // East only
        Block.box(3.0, 0.0, 3.0, 16.0, 1.0, 13.0),
        // North + East
        Block.box(3.0, 0.0, 0.0, 16.0, 1.0, 13.0),
        // South only
        Block.box(3.0, 0.0, 3.0, 13.0, 1.0, 16.0),
        // North + South
        Block.box(3.0, 0.0, 0.0, 13.0, 1.0, 16.0),
        // East + South
        Block.box(3.0, 0.0, 3.0, 16.0, 1.0, 16.0),
        // North + East + South
        Block.box(3.0, 0.0, 0.0, 16.0, 1.0, 16.0),
        // West only
        Block.box(0.0, 0.0, 3.0, 13.0, 1.0, 13.0),
        // North + West
        Block.box(0.0, 0.0, 0.0, 13.0, 1.0, 13.0),
        // East + West
        Block.box(0.0, 0.0, 3.0, 16.0, 1.0, 13.0),
        // North + East + West
        Block.box(0.0, 0.0, 0.0, 16.0, 1.0, 13.0),
        // South + West
        Block.box(0.0, 0.0, 3.0, 13.0, 1.0, 16.0),
        // North + South + West
        Block.box(0.0, 0.0, 0.0, 13.0, 1.0, 16.0),
        // East + South + West
        Block.box(0.0, 0.0, 3.0, 16.0, 1.0, 16.0),
        // All four directions
        Block.box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0)
    };

    public BlockInlay() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.GOLD)
                .sound(SoundType.METAL)
                .strength(0.5f)
                .noCollission()
                .lightLevel(state -> 1));
        
        registerDefaultState(stateDefinition.any()
                .setValue(NORTH, EnumAttachPosition.NONE)
                .setValue(EAST, EnumAttachPosition.NONE)
                .setValue(SOUTH, EnumAttachPosition.NONE)
                .setValue(WEST, EnumAttachPosition.NONE)
                .setValue(CHARGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, CHARGE);
    }

    // ==================== Shape ====================

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return WIRE_AABB[getAABBIndex(state)];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    private int getAABBIndex(BlockState state) {
        int i = 0;
        boolean north = state.getValue(NORTH) != EnumAttachPosition.NONE;
        boolean east = state.getValue(EAST) != EnumAttachPosition.NONE;
        boolean south = state.getValue(SOUTH) != EnumAttachPosition.NONE;
        boolean west = state.getValue(WEST) != EnumAttachPosition.NONE;

        if (north || (south && !east && !west)) {
            i |= 1 << Direction.NORTH.get2DDataValue();
        }
        if (east || (west && !north && !south)) {
            i |= 1 << Direction.EAST.get2DDataValue();
        }
        if (south || (north && !east && !west)) {
            i |= 1 << Direction.SOUTH.get2DDataValue();
        }
        if (west || (east && !north && !south)) {
            i |= 1 << Direction.WEST.get2DDataValue();
        }
        return i;
    }

    // ==================== Placement ====================

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return getConnectionState(context.getLevel(), defaultBlockState(), context.getClickedPos());
    }

    private BlockState getConnectionState(BlockGetter level, BlockState state, BlockPos pos) {
        state = state.setValue(WEST, getAttachPosition(level, pos, Direction.WEST));
        state = state.setValue(EAST, getAttachPosition(level, pos, Direction.EAST));
        state = state.setValue(NORTH, getAttachPosition(level, pos, Direction.NORTH));
        state = state.setValue(SOUTH, getAttachPosition(level, pos, Direction.SOUTH));
        return state;
    }

    private EnumAttachPosition getAttachPosition(BlockGetter level, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.relative(direction);
        BlockState neighborState = level.getBlockState(neighborPos);

        if (canConnectTo(neighborState, direction, level, neighborPos)) {
            return EnumAttachPosition.SIDE;
        }
        if (isSourceBlock(level, neighborPos)) {
            return EnumAttachPosition.EXT;
        }
        return EnumAttachPosition.NONE;
    }

    protected static boolean canConnectTo(BlockState state, @Nullable Direction side, BlockGetter level, BlockPos pos) {
        Block block = state.getBlock();
        // Can connect to other inlay blocks or pedestals
        if (block instanceof BlockInlay) {
            return true;
        }
        if (block instanceof BlockPedestal) {
            return true;
        }
        return false;
    }

    // ==================== Block Updates ====================

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                   LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction.getAxis().isHorizontal()) {
            state = state.setValue(getPropertyForDirection(direction), 
                    getAttachPosition(level, pos, direction));
        }
        
        if (!state.canSurvive(level, pos)) {
            return defaultBlockState(); // Will be replaced by air via canSurvive
        }
        
        return state;
    }

    private EnumProperty<EnumAttachPosition> getPropertyForDirection(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            default -> NORTH;
        };
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!level.isClientSide && !oldState.is(state.getBlock())) {
            updateSurroundingInlay(level, pos, state);
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                notifyInlayNeighborsOfStateChange(level, pos.relative(dir));
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            super.onRemove(state, level, pos, newState, isMoving);
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                level.updateNeighborsAt(pos.relative(dir), this);
            }
            updateSurroundingInlay(level, pos, state);
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                notifyInlayNeighborsOfStateChange(level, pos.relative(dir));
            }
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide) {
            if (state.canSurvive(level, pos)) {
                updateSurroundingInlay(level, pos, state);
            } else {
                dropResources(state, level, pos);
                level.removeBlock(pos, false);
            }
        }
    }

    // ==================== Charge Propagation ====================

    public static void notifyInlayNeighborsOfStateChange(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        
        if (block instanceof BlockInlay || block instanceof BlockPedestal) {
            level.updateNeighborsAt(pos, block);
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                level.updateNeighborsAt(pos.relative(dir), block);
            }
        }
    }

    public static BlockState updateSurroundingInlay(Level level, BlockPos pos, BlockState state) {
        Set<BlockPos> blocksNeedingUpdate = Sets.newHashSet();
        state = calculateChanges(level, pos, pos, state, blocksNeedingUpdate);
        List<BlockPos> list = Lists.newArrayList(blocksNeedingUpdate);
        for (BlockPos blockpos : list) {
            level.updateNeighborsAt(blockpos, level.getBlockState(pos).getBlock());
        }
        return state;
    }

    public static int getMaxStrength(Level level, BlockPos pos, int strength) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        
        if (!(block instanceof BlockInlay) && !(block instanceof BlockPedestal)) {
            return strength;
        }
        
        if (!state.hasProperty(CHARGE)) {
            // Pedestals do not carry a charge property; they merely relay it
            return strength;
        }
        int charge = state.getValue(CHARGE);
        return Math.max(charge, strength);
    }

    public static int getSourceStrength(BlockGetter level, BlockPos pos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            int energy = getSourceStrengthAt(level, pos.relative(dir));
            if (energy > 0) {
                return energy;
            }
        }
        return 0;
    }

    public static int getSourceStrengthAt(BlockGetter level, BlockPos pos) {
        if (isSourceBlock(level, pos)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TileStabilizer stabilizer) {
                return stabilizer.getEnergy();
            }
        }
        return 0;
    }

    public static boolean isSourceBlock(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        // Check if it's a stabilizer block
        if (ModBlocks.STABILIZER != null && ModBlocks.STABILIZER.isBound()) {
            return state.is(ModBlocks.STABILIZER.get());
        }
        return state.getBlock() instanceof BlockStabilizer;
    }

    public static BlockState calculateChanges(Level level, BlockPos pos1, BlockPos pos2, 
                                               BlockState state, Set<BlockPos> blocksNeedingUpdate) {
        BlockState originalState = state;
        int current = state.getValue(CHARGE);
        int max = 0;
        
        max = getMaxStrength(level, pos2, max);
        int source = getSourceStrength(level, pos1);
        if (source > 0 && source > max - 1) {
            max = source;
        }
        
        int neighbour = 0;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = pos1.relative(dir);
            boolean isDifferent = neighborPos.getX() != pos2.getX() || neighborPos.getZ() != pos2.getZ();
            if (isDifferent) {
                neighbour = getMaxStrength(level, neighborPos, neighbour);
            }
        }
        
        if (neighbour > max) {
            max = neighbour - 1;
        } else if (max > 0) {
            max--;
        } else {
            max = 0;
        }
        
        if (source > max - 1) {
            max = source;
        }
        
        if (current != max) {
            state = state.setValue(CHARGE, max);
            if (level.getBlockState(pos1) == originalState) {
                level.setBlock(pos1, state, 2);
            }
            blocksNeedingUpdate.add(pos1);
            for (Direction dir : Direction.values()) {
                blocksNeedingUpdate.add(pos1.relative(dir));
            }
        }
        
        return state;
    }

    // ==================== Rotation/Mirror ====================

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_180 -> state
                    .setValue(NORTH, state.getValue(SOUTH))
                    .setValue(EAST, state.getValue(WEST))
                    .setValue(SOUTH, state.getValue(NORTH))
                    .setValue(WEST, state.getValue(EAST));
            case COUNTERCLOCKWISE_90 -> state
                    .setValue(NORTH, state.getValue(EAST))
                    .setValue(EAST, state.getValue(SOUTH))
                    .setValue(SOUTH, state.getValue(WEST))
                    .setValue(WEST, state.getValue(NORTH));
            case CLOCKWISE_90 -> state
                    .setValue(NORTH, state.getValue(WEST))
                    .setValue(EAST, state.getValue(NORTH))
                    .setValue(SOUTH, state.getValue(EAST))
                    .setValue(WEST, state.getValue(SOUTH));
            default -> state;
        };
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return switch (mirror) {
            case LEFT_RIGHT -> state
                    .setValue(NORTH, state.getValue(SOUTH))
                    .setValue(SOUTH, state.getValue(NORTH));
            case FRONT_BACK -> state
                    .setValue(EAST, state.getValue(WEST))
                    .setValue(WEST, state.getValue(EAST));
            default -> super.mirror(state, mirror);
        };
    }

    // ==================== Visual Effects ====================

    @OnlyIn(Dist.CLIENT)
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int charge = state.getValue(CHARGE);
        if (charge > 0 && random.nextInt(20 - charge) == 0) {
            Direction face = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            if (getAttachPosition(level, pos, face) != EnumAttachPosition.NONE) {
                double x = pos.getX() + 0.5 + random.nextGaussian() * 0.08;
                double y = pos.getY() + 0.025;
                double z = pos.getZ() + 0.5 + random.nextGaussian() * 0.08;
                double vx = face.getStepX() / 70.0 * (1.0 - random.nextFloat() * 0.1);
                double vz = face.getStepZ() / 70.0 * (1.0 - random.nextFloat() * 0.1);
                
                float r = Mth.randomBetweenInclusive(random, 150, 200) / 255.0f;
                float g = Mth.randomBetweenInclusive(random, 0, 200) / 255.0f;
                
                // Sparkle effect using FXDispatcher when available
                // Convert RandomSource to Random for FXDispatcher compatibility
                if (FXDispatcher.INSTANCE != null) {
                    Random javaRandom = new Random(random.nextLong());
                    FXDispatcher.INSTANCE.drawLineSparkle(javaRandom, x, y, z, vx, 0.0, vz, 
                            0.33f, r, g, g / 2.0f, 0, 1.0f, 0.0f, 16);
                }
            }
        }
    }

    /**
     * Returns the color multiplier based on charge level.
     * Used by the color handler for rendering.
     */
    @OnlyIn(Dist.CLIENT)
    public static int colorMultiplier(int charge) {
        float f = charge / 15.0f;
        float brightness = f * 0.5f + 0.5f;
        if (charge == 0) {
            brightness = 0.3f;
        }
        int color = Mth.clamp((int)(brightness * 255.0f), 0, 255);
        return 0xFF000000 | color << 16 | color << 8 | color;
    }

    // ==================== IInfusionStabiliserExt ====================

    @Override
    public boolean canStabaliseInfusion(Level level, BlockPos pos) {
        return true;
    }

    @Override
    public float getStabilizationAmount(Level level, BlockPos pos) {
        return 0.025f; // Small stabilization bonus per inlay piece
    }

    // ==================== Factory ====================

    public static BlockInlay create() {
        return new BlockInlay();
    }

    // ==================== Attachment Position Enum ====================

    public enum EnumAttachPosition implements StringRepresentable {
        SIDE("side"),
        NONE("none"),
        EXT("ext");

        private final String name;

        EnumAttachPosition(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return getSerializedName();
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}




