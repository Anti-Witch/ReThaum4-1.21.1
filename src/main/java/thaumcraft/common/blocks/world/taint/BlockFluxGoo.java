package thaumcraft.common.blocks.world.taint;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
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
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.common.entities.monster.EntityThaumicSlime;
import thaumcraft.init.ModBlocks;
import thaumcraft.init.ModEffects;
import thaumcraft.init.ModEntities;

/**
 * BlockFluxGoo - A semi-fluid taint block that spreads vis exhaustion.
 * 
 * Features:
 * - Slows entities and applies vis exhaustion
 * - Feeds Thaumic Slimes that touch it
 * - Can spawn Thaumic Slimes when enough goo accumulates
 * - Slowly evaporates, polluting the aura
 * - Can turn into taint fibers when nearly depleted
 * 
 * Ported from 1.12.2 - simplified from BlockFluidFinite to regular block with level property
 */
public class BlockFluxGoo extends Block implements ITaintBlock {
    
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
    
    public BlockFluxGoo() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(0.5f)
                .noOcclusion()
                .pushReaction(PushReaction.DESTROY)
                .randomTicks()
                .noCollission()
                .sound(net.minecraft.world.level.block.SoundType.SLIME_BLOCK));
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
        return Shapes.empty(); // No collision - entities can walk through
    }
    
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        int gooLevel = state.getValue(LEVEL);
        
        // Thaumic Slimes feed on flux goo
        if (entity instanceof EntityThaumicSlime slime) {
            if (slime.getSize() < gooLevel && level.random.nextBoolean()) {
                slime.setSize(slime.getSize() + 1, true);
                if (gooLevel > 1) {
                    level.setBlock(pos, state.setValue(LEVEL, gooLevel - 1), 2);
                } else {
                    level.removeBlock(pos, false);
                }
            }
            return;
        }
        
        // Slow down other entities
        float slowFactor = 1.0f - (gooLevel / 8.0f);
        Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(motion.x * slowFactor, motion.y, motion.z * slowFactor);
        
        // Apply vis exhaustion to living entities
        if (entity instanceof LivingEntity living) {
            MobEffectInstance effect = new MobEffectInstance(
                    ModEffects.VIS_EXHAUST,
                    600,
                    gooLevel / 3, 
                    true, 
                    true
            );
            living.addEffect(effect);
        }
    }
    
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int gooLevel = state.getValue(LEVEL);
        
        // Chance to spawn Thaumic Slime
        if (gooLevel >= 2 && gooLevel < 6 && level.isEmptyBlock(pos.above()) && random.nextInt(50) == 0) {
            level.removeBlock(pos, false);
            EntityThaumicSlime slime = new EntityThaumicSlime(ModEntities.THAUMIC_SLIME.get(), level);
            slime.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0f, 0.0f);
            slime.setSize(1, true);
            level.addFreshEntity(slime);
            level.playSound(null, pos, SoundEvents.SLIME_SQUISH, SoundSource.BLOCKS, 1.0f, 1.0f);
            return;
        }
        
        // Larger goo spawns bigger slimes
        if (gooLevel >= 6 && level.isEmptyBlock(pos.above()) && random.nextInt(50) == 0) {
            level.removeBlock(pos, false);
            EntityThaumicSlime slime = new EntityThaumicSlime(ModEntities.THAUMIC_SLIME.get(), level);
            slime.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0f, 0.0f);
            slime.setSize(2, true);
            level.addFreshEntity(slime);
            level.playSound(null, pos, SoundEvents.SLIME_SQUISH, SoundSource.BLOCKS, 1.0f, 1.0f);
            return;
        }
        
        // Evaporation
        if (random.nextInt(4) == 0) {
            if (gooLevel == 0) {
                // At minimum level, either pollute and vanish or become taint fiber
                if (random.nextBoolean()) {
                    AuraHelper.polluteAura(level, pos, 1.0f, true);
                    level.removeBlock(pos, false);
                } else {
                    // Turn into taint fiber
                    level.setBlock(pos, ModBlocks.TAINT_FIBRE.get().defaultBlockState(), 3);
                }
            } else {
                // Reduce level and pollute
                level.setBlock(pos, state.setValue(LEVEL, gooLevel - 1), 2);
                AuraHelper.polluteAura(level, pos, 1.0f, true);
            }
        }
    }
    
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!level.isClientSide && !oldState.is(this)) {
            level.scheduleTick(pos, this, 5);
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, 
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        // Flow down if there's space below
        if (direction == Direction.DOWN && neighborState.isAir()) {
            // Schedule a tick to handle flowing
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
            // Move goo down
            level.setBlock(below, state, 3);
            level.removeBlock(pos, false);
        } else if (belowState.is(this)) {
            // Merge with goo below (LEVEL tracks quanta - 1, cap 7)
            int currentLevel = state.getValue(LEVEL);
            int belowLevel = belowState.getValue(LEVEL);
            int totalLevel = currentLevel + belowLevel + 1;
            
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
     * Create flux goo with a specific level.
     */
    public static BlockState withLevel(Block block, int level) {
        return block.defaultBlockState().setValue(LEVEL, Math.min(7, Math.max(0, level)));
    }
}




