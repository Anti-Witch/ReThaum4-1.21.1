package thaumcraft.common.entities.monster.mods;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Grim Champion Modifier - Champion mobs that apply wither effect.
 * 
 * Effects:
 * - Applies Wither II for 5 seconds on hit
 * - Shows dark/black soul particles
 * 
 * Type: 1 (Offensive)
 */
public class ChampionModGrim implements IChampionModifierEffect {
    
    @Override
    public float performEffect(LivingEntity champion, LivingEntity target, DamageSource source, float amount) {
        // Apply wither effect to target
        if (target != null && !target.level().isClientSide) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1)); // 5 seconds, level II
        }
        return amount;
    }
    
    @OnlyIn(Dist.CLIENT)
    @Override
    public void showFX(LivingEntity champion) {
        if (champion.level().random.nextInt(3) != 0) {
            return;
        }
        
        // Dark soul particles
        double w = champion.level().random.nextFloat() * champion.getBbWidth();
        double d = champion.level().random.nextFloat() * champion.getBbWidth();
        double h = champion.level().random.nextFloat() * champion.getBbHeight();
        
        champion.level().addParticle(ParticleTypes.SOUL,
                champion.getBoundingBox().minX + w,
                champion.getBoundingBox().minY + h,
                champion.getBoundingBox().minZ + d,
                0, 0.02, 0);
    }
}




