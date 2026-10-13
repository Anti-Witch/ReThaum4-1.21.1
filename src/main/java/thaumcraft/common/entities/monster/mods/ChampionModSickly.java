package thaumcraft.common.entities.monster.mods;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Sickly Champion Modifier - Champion mobs that spread weakness.
 * 
 * Effects:
 * - Applies Weakness II for 10 seconds on hit
 * - Shows green sickly particles
 * 
 * Type: 1 (Offensive)
 */
public class ChampionModSickly implements IChampionModifierEffect {
    
    @Override
    public float performEffect(LivingEntity champion, LivingEntity target, DamageSource source, float amount) {
        // Apply weakness to target
        if (target != null && !target.level().isClientSide) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1)); // 10 seconds, level II
        }
        return amount;
    }
    
    @OnlyIn(Dist.CLIENT)
    @Override
    public void showFX(LivingEntity champion) {
        if (champion.level().random.nextInt(3) != 0) {
            return;
        }
        
        // Green sickly particles
        double w = champion.level().random.nextFloat() * champion.getBbWidth();
        double d = champion.level().random.nextFloat() * champion.getBbWidth();
        double h = champion.level().random.nextFloat() * champion.getBbHeight();
        
        champion.level().addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.2f, 0.8f, 0.2f),
                champion.getBoundingBox().minX + w,
                champion.getBoundingBox().minY + h,
                champion.getBoundingBox().minZ + d,
                0, 0, 0);
    }
}




