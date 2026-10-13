package thaumcraft.common.blocks.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
import thaumcraft.api.damagesource.DamageSourceThaumcraft;
import thaumcraft.client.fx.FXDispatcher;

/**
 * BlockFluidDeath - A corrosive, deadly liquid that damages entities.
 * 
 * Features:
 * - Damages entities that touch it (dissolve damage)
 * - Slows entities down
 * - Slowly evaporates
 * - Visual bubbling effects
 * 
 * Ported from 1.12.2 - simplified from BlockFluidClassic to regular block with level property
 */
public class BlockFluidDeath extends Block {
    
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 3);
    
    protected static final VoxelShape[] SHAPES = new VoxelShape[] {
            Block.box(0, 0, 0, 16, 4, 16),   // Level 0 (25%)
            Block.box(0, 0, 0, 16, 8, 16),   // Level 1 (50%)
            Block.box(0, 0, 0, 16, 12, 16),  // Level 2 (75%)
            Block.box(0, 0, 0, 16, 16, 16)   // Level 3 (100%)
    };
    
    public BlockFluidDeath() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(0.0f)
                .noOcclusion()
                .pushReaction(PushReaction.DESTROY)
                .randomTicks()
                .noCollission()
                .lightLevel(state -> 3)
                .sound(net.minecraft.world.level.block.SoundType.SLIME_BLOCK));
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 3));
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
        return Shapes.empty(); // No collision - entities sink
    }
    
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        int fluidLevel = state.getValue(LEVEL);
        float quantaPercentage = (fluidLevel + 1) / 4.0f;
        
        // Slow down entities based on liquid depth
        float slowFactor = 1.0f - quantaPercentage / 2.0f;
        Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(motion.x * slowFactor, motion.y, motion.z * slowFactor);
        
        // Deal dissolve damage to living entities
        if (!level.isClientSide && entity instanceof LivingEntity living) {
            float damage = (4 - fluidLevel) + 1; // More damage at lower levels (concentrated)
            living.hurt(DamageSourceThaumcraft.createDissolve(level), damage);
        }
    }
    
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Spawn bubbling particles
        if (random.nextInt(20) == 0) {
            int fluidLevel = state.getValue(LEVEL);
            float height = 0.1f + 0.25f * (fluidLevel + 1);
            float h = random.nextFloat() * 0.075f;
            
            FXDispatcher.INSTANCE.slimyBubbleFX(
                    pos.getX() + random.nextFloat(),
                    pos.getY() + height,
                    pos.getZ() + random.nextFloat(),
                    0.075f + h,
                    0.3f - random.nextFloat() * 0.1f,  // R (purple-ish)
                    0.0f,                               // G
                    0.4f + random.nextFloat() * 0.1f   // B
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
        
        // Slow evaporation
        if (random.nextInt(10) == 0) {
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
            
            if (totalLevel <= 3) {
                level.setBlock(below, belowState.setValue(LEVEL, totalLevel), 3);
                level.removeBlock(pos, false);
            } else {
                level.setBlock(below, belowState.setValue(LEVEL, 3), 3);
                int remaining = totalLevel - 4;
                if (remaining >= 0) {
                    level.setBlock(pos, state.setValue(LEVEL, remaining), 3);
                } else {
                    level.removeBlock(pos, false);
                }
            }
        }
    }
    
    @Override
    public boolean canBeReplaced(BlockState state, net.minecraft.world.item.context.BlockPlaceContext context) {
        return state.getValue(LEVEL) < 2;
    }
    
    /**
     * Create liquid death with a specific level.
     */
    public static BlockState withLevel(Block block, int level) {
        return block.defaultBlockState().setValue(LEVEL, Math.min(3, Math.max(0, level)));
    }
}




