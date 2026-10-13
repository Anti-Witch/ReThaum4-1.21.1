package thaumcraft.common.blocks.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.init.ModEffects;

/**
 * BlockFluidPure - A purifying liquid that cleanses warp.
 * 
 * Features:
 * - Grants Warp Ward potion effect to players
 * - Consumes itself when used by a player
 * - Duration scales inversely with permanent warp
 * - Sparkle particle effects
 * 
 * Ported from 1.12.2 - simplified from BlockFluidClassic to regular block with level property
 */
public class BlockFluidPure extends Block {
    
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 7);
    
    protected static final VoxelShape[] SHAPES = new VoxelShape[] {
            Block.box(0, 0, 0, 16, 2, 16),   // Level 0
            Block.box(0, 0, 0, 16, 4, 16),   // Level 1
            Block.box(0, 0, 0, 16, 6, 16),   // Level 2
            Block.box(0, 0, 0, 16, 8, 16),   // Level 3
            Block.box(0, 0, 0, 16, 10, 16),  // Level 4
            Block.box(0, 0, 0, 16, 12, 16),  // Level 5
            Block.box(0, 0, 0, 16, 14, 16),  // Level 6
            Block.box(0, 0, 0, 16, 16, 16)   // Level 7
    };
    
    public BlockFluidPure() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.QUARTZ)
                .strength(0.0f)
                .noOcclusion()
                .pushReaction(PushReaction.DESTROY)
                .randomTicks()
                .noCollission()
                .lightLevel(state -> 10)
                .sound(net.minecraft.world.level.block.SoundType.HONEY_BLOCK));
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 7));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(LEVEL)];
    }
    
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty(); // No collision
    }
    
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        int fluidLevel = state.getValue(LEVEL);
        float quantaPercentage = (fluidLevel + 1) / 8.0f;
        
        // Slow down entities based on liquid depth
        float slowFactor = 1.0f - quantaPercentage / 2.0f;
        Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(motion.x * slowFactor, motion.y, motion.z * slowFactor);
        
        // Grant warp ward to players at source blocks (level 7)
        if (!level.isClientSide && fluidLevel == 7 && entity instanceof Player player) {
            // Check if player doesn't already have warp ward active
            if (!player.hasEffect(ModEffects.WARP_WARD)) {
                // Calculate duration based on permanent warp
                IPlayerWarp warpCap = ThaumcraftCapabilities.getWarp(player);
                int permWarp = warpCap != null ? warpCap.get(IPlayerWarp.EnumWarpType.PERMANENT) : 0;
                
                int div = 1;
                if (permWarp > 0) {
                    div = (int) Math.sqrt(permWarp);
                    if (div < 1) div = 1;
                }
                
                // Grant warp ward effect
                int duration = Math.min(32000, 200000 / div);
                player.addEffect(new MobEffectInstance(ModEffects.WARP_WARD, duration, 0, true, true));
                
                // Consume the fluid
                level.removeBlock(pos, false);
                level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
        }
    }
    
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int fluidLevel = state.getValue(LEVEL);
        
        // Spawn sparkle particles
        if (random.nextInt(10) == 0) {
            float height = 0.125f * (8 - fluidLevel);
            
            FXDispatcher.INSTANCE.sparkle(
                    (float)(pos.getX() + random.nextFloat()),
                    (float)(pos.getY() + height),
                    (float)(pos.getZ() + random.nextFloat()),
                    1.0f, 1.0f, 1.0f  // White sparkles
            );
        }
        
        // Occasional pop sound
        if (random.nextInt(50) == 0) {
            double x = pos.getX() + random.nextFloat();
            double y = pos.getY() + 0.5;
            double z = pos.getZ() + random.nextFloat();
            level.playLocalSound(x, y, z, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 
                    0.1f + random.nextFloat() * 0.1f, 0.9f + random.nextFloat() * 0.15f, false);
        }
    }
    
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int fluidLevel = state.getValue(LEVEL);
        
        // Very slow evaporation for non-source blocks
        if (fluidLevel < 7 && random.nextInt(20) == 0) {
            if (fluidLevel == 0) {
                level.removeBlock(pos, false);
            } else {
                level.setBlock(pos, state.setValue(LEVEL, fluidLevel - 1), 2);
            }
        }
    }
    
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, 
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        // Flow down if there's space below
        if (direction == Direction.DOWN && neighborState.isAir()) {
            level.scheduleTick(pos, this, 5);
        }
        return state;
    }
    
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Try to flow down
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        
        if (belowState.isAir()) {
            // Move liquid down
            level.setBlock(below, state, 3);
            level.removeBlock(pos, false);
        } else if (belowState.is(this)) {
            // Merge with liquid below
            int currentLevel = state.getValue(LEVEL);
            int belowLevel = belowState.getValue(LEVEL);
            int totalLevel = currentLevel + belowLevel + 2;
            
            if (totalLevel <= 7) {
                level.setBlock(below, belowState.setValue(LEVEL, totalLevel), 3);
                level.removeBlock(pos, false);
            } else {
                level.setBlock(below, belowState.setValue(LEVEL, 7), 3);
                int remaining = totalLevel - 8;
                if (remaining > 0) {
                    level.setBlock(pos, state.setValue(LEVEL, remaining), 3);
                } else {
                    level.removeBlock(pos, false);
                }
            }
        }
    }
    
    @Override
    public boolean canBeReplaced(BlockState state, net.minecraft.world.item.context.BlockPlaceContext context) {
        return state.getValue(LEVEL) < 4;
    }
    
    /**
     * Create purifying fluid with a specific level.
     */
    public static BlockState withLevel(Block block, int level) {
        return block.defaultBlockState().setValue(LEVEL, Math.min(7, Math.max(0, level)));
    }
}




