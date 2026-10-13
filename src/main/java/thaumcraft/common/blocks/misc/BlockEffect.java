package thaumcraft.common.blocks.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.entities.IEldritchMob;

/**
 * BlockEffect - Invisible effect blocks that apply effects to entities.
 * Used for effectSap (wither/slow/hunger), effectShock (damage/slow), effectGlimmer (light).
 * 
 * @author Azanor
 * Ported to 1.20.1
 */
public class BlockEffect extends Block {
    
    public enum EffectType {
        SAP,      // Wither, slowness, hunger - from eldritch creatures
        SHOCK,    // Electric damage and slowness
        GLIMMER   // Just provides light, no effect on entities
    }
    
    private final EffectType effectType;
    
    public BlockEffect(EffectType type) {
        super(BlockBehaviour.Properties.of()
                .replaceable()
                .noCollission()
                .instabreak()
                .noLootTable()
                .air()
                .lightLevel(state -> type == EffectType.GLIMMER ? 15 : 7)
                .randomTicks()
                .pushReaction(PushReaction.DESTROY));
        this.effectType = type;
    }
    
    public EffectType getEffectType() {
        return effectType;
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }
    
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }
    
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide) return;
        
        if (effectType == EffectType.SHOCK) {
            if (entity instanceof LivingEntity living) {
                // Deal magic damage
                entity.hurt(level.damageSources().magic(), 1.0f);
                // Apply slowness
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 0, true, true));
            }
            // Small chance to disappear
            if (level.random.nextInt(100) == 0) {
                level.removeBlock(pos, false);
            }
        } else if (effectType == EffectType.SAP) {
            // Eldritch mobs are immune to sap
            if (entity instanceof IEldritchMob) return;
            
            if (entity instanceof LivingEntity living && !living.hasEffect(MobEffects.WITHER)) {
                // Apply wither, slowness, and hunger
                living.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0, true, true));
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, true, true));
                living.addEffect(new MobEffectInstance(MobEffects.HUNGER, 40, 1, true, true));
            }
        }
        // Glimmer has no entity effect
    }
    
    @Override
    public void tick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, RandomSource random) {
        super.tick(state, level, pos, random);
        
        // Glimmer blocks persist, others decay
        if (effectType != EffectType.GLIMMER) {
            level.removeBlock(pos, false);
        }
    }
    
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // TODO: Add particle effects when FXDispatcher is implemented
        // For shock: spark particles
        // For sap: purple spark particles
        // Glimmer: no particles
    }
    
    // ==================== Factory Methods ====================
    
    public static BlockEffect createSap() {
        return new BlockEffect(EffectType.SAP);
    }
    
    public static BlockEffect createShock() {
        return new BlockEffect(EffectType.SHOCK);
    }
    
    public static BlockEffect createGlimmer() {
        return new BlockEffect(EffectType.GLIMMER);
    }
}




