package thaumcraft.common.blocks.world.taint;
import thaumcraft.common.lib.utils.TCCompat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.init.ModEffects;
import thaumcraft.init.ModItems;

/**
 * BlockTaintFibre - Taint growth that spreads on surfaces.
 * 
 * Features:
 * - Grows on solid surfaces (floor, walls, ceiling)
 * - Can have crystal growths that drop flux crystals
 * - Applies flux taint effect to entities walking on it
 * - Dies if not near a taint seed
 * - Spreads taint to nearby blocks
 * 
 * Ported from 1.12.2 - simplified connection system
 */
@SuppressWarnings("deprecation")
public class BlockTaintFibre extends Block implements ITaintBlock {
    
    // Connection properties for rendering on different faces
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    
    // Growth variant (for crystal formations)
    public static final BooleanProperty HAS_CRYSTAL = BooleanProperty.create("has_crystal");
    
    // Collision shapes for each face
    protected static final VoxelShape SHAPE_DOWN = Block.box(0, 0, 0, 16, 1, 16);
    protected static final VoxelShape SHAPE_UP = Block.box(0, 15, 0, 16, 16, 16);
    protected static final VoxelShape SHAPE_NORTH = Block.box(0, 0, 0, 16, 16, 1);
    protected static final VoxelShape SHAPE_SOUTH = Block.box(0, 0, 15, 16, 16, 16);
    protected static final VoxelShape SHAPE_WEST = Block.box(0, 0, 0, 1, 16, 16);
    protected static final VoxelShape SHAPE_EAST = Block.box(15, 0, 0, 16, 16, 16);
    
    public BlockTaintFibre() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(1.0f)
                .noOcclusion()
                .pushReaction(PushReaction.DESTROY)
                .randomTicks()
                .ignitedByLava()
                .sound(net.minecraft.world.level.block.SoundType.SLIME_BLOCK));
        
        registerDefaultState(stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false)
                .setValue(HAS_CRYSTAL, false));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, HAS_CRYSTAL);
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = Shapes.empty();
        
        if (state.getValue(DOWN)) shape = Shapes.or(shape, SHAPE_DOWN);
        if (state.getValue(UP)) shape = Shapes.or(shape, SHAPE_UP);
        if (state.getValue(NORTH)) shape = Shapes.or(shape, SHAPE_NORTH);
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, SHAPE_SOUTH);
        if (state.getValue(WEST)) shape = Shapes.or(shape, SHAPE_WEST);
        if (state.getValue(EAST)) shape = Shapes.or(shape, SHAPE_EAST);
        
        // If no connections, return a small shape
        if (shape.isEmpty()) {
            shape = SHAPE_DOWN;
        }
        
        return shape;
    }
    
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty(); // Can walk through
    }
    
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return getStateWithConnections(context.getLevel(), context.getClickedPos());
    }
    
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return getStateWithConnections(level, pos);
    }
    
    /**
     * Calculate connection state based on adjacent solid blocks.
     */
    private BlockState getStateWithConnections(LevelAccessor level, BlockPos pos) {
        boolean down = canConnectTo(level, pos, Direction.DOWN);
        boolean up = canConnectTo(level, pos, Direction.UP);
        boolean north = canConnectTo(level, pos, Direction.NORTH);
        boolean south = canConnectTo(level, pos, Direction.SOUTH);
        boolean west = canConnectTo(level, pos, Direction.WEST);
        boolean east = canConnectTo(level, pos, Direction.EAST);
        
        // Determine if this should have a crystal based on position hash
        boolean hasCrystal = false;
        if (down) {
            long hash = pos.asLong();
            hasCrystal = (hash % 50) == 6; // ~2% chance based on position
        }
        
        return defaultBlockState()
                .setValue(DOWN, down)
                .setValue(UP, up)
                .setValue(NORTH, north)
                .setValue(SOUTH, south)
                .setValue(WEST, west)
                .setValue(EAST, east)
                .setValue(HAS_CRYSTAL, hasCrystal);
    }
    
    /**
     * Check if this block should connect to a face (adjacent block is solid).
     */
    private boolean canConnectTo(LevelAccessor level, BlockPos pos, Direction face) {
        BlockPos adjacentPos = pos.relative(face);
        BlockState adjacentState = level.getBlockState(adjacentPos);
        return adjacentState.isFaceSturdy(level, adjacentPos, face.getOpposite());
    }
    
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        // Must have at least one solid adjacent block
        for (Direction dir : Direction.values()) {
            BlockPos adjacent = pos.relative(dir);
            if (level.getBlockState(adjacent).isFaceSturdy(level, adjacent, dir.getOpposite())) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        // Apply flux taint to non-tainted living entities
        if (!level.isClientSide && entity instanceof LivingEntity living) {
            if (!(living instanceof ITaintedMob) && !living.isInvertedHealAndHarm()) {
                if (level.random.nextInt(750) == 0) {
                    living.addEffect(new MobEffectInstance(ModEffects.FLUX_TAINT, 200, 0, false, true));
                }
            }
        }
    }
    
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Check if we should survive
        if (!hasAnySolidNeighbor(level, pos)) {
            die(level, pos, state);
            return;
        }
        
        // TODO: Check if near taint seed when TaintHelper is implemented
        // if (!TaintHelper.isNearTaintSeed(level, pos)) {
        //     die(level, pos, state);
        //     return;
        // }
        
        // TODO: Spread taint fibers when TaintHelper is implemented
        // TaintHelper.spreadFibres(level, pos);
    }
    
    /**
     * Check if there's at least one non-taint solid block adjacent.
     */
    private boolean hasAnySolidNeighbor(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos adjacent = pos.relative(dir);
            BlockState adjacentState = level.getBlockState(adjacent);
            if (!(adjacentState.getBlock() instanceof ITaintBlock) && 
                    adjacentState.isFaceSturdy(level, adjacent, dir.getOpposite())) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public void die(Level level, BlockPos pos, BlockState state) {
        level.playSound(null, pos, SoundEvents.CHORUS_FLOWER_DEATH, SoundSource.BLOCKS, 
                0.1f, 0.9f + level.random.nextFloat() * 0.2f);
        level.removeBlock(pos, false);
    }
    
    @Override
    public void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropExperience) {
        super.spawnAfterBreak(state, level, pos, tool, dropExperience);
        
        // Drop flux crystal if this has a crystal growth
        if (state.getValue(HAS_CRYSTAL)) {
            int fortune = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(TCCompat.enchantment(level, net.minecraft.world.item.enchantment.Enchantments.FORTUNE), tool);
            if (level.random.nextInt(5) <= fortune) {
                // TC6-removed: popResource(level, pos, new ItemStack(ModItems.FLUX_CRYSTAL.get()));
            }
            // Pollute aura when broken
            AuraHelper.polluteAura(level, pos, 1.0f, true);
        }
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
    
    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }
    
    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(HAS_CRYSTAL) ? 12 : 0;
    }
    
    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 3;
    }
    
    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 3;
    }
    
    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return true;
    }
    
    // ==================== Static Helper Methods ====================
    
    /**
     * Check if a position is only surrounded by taint blocks (no non-taint neighbors).
     */
    public static boolean isOnlyAdjacentToTaint(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos adjacent = pos.relative(dir);
            BlockState adjacentState = level.getBlockState(adjacent);
            // If there's air or non-taint block adjacent, return false
            if (level.isEmptyBlock(adjacent)) {
                return false;
            }
            if (!(adjacentState.getBlock() instanceof ITaintBlock) && 
                !adjacentState.isAir()) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Check if a position is surrounded by taint on multiple sides (hemmed in).
     * Returns true if 4 or more adjacent blocks are taint.
     */
    public static boolean isHemmedByTaint(Level level, BlockPos pos) {
        int taintCount = 0;
        for (Direction dir : Direction.values()) {
            BlockPos adjacent = pos.relative(dir);
            BlockState adjacentState = level.getBlockState(adjacent);
            if (adjacentState.getBlock() instanceof ITaintBlock) {
                taintCount++;
            }
        }
        return taintCount >= 4;
    }
}




