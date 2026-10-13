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
 * Venomous Champion Modifier - Champion mobs that poison targets.
 * 
 * Effects:
 * - Applies Poison II for 8 seconds on hit
 * - Shows poison particles
 * 
 * Type: 1 (Offensive)
 */
public class ChampionModPoison implements IChampionModifierEffect {
    
    @Override
    public float performEffect(LivingEntity champion, LivingEntity target, DamageSource source, float amount) {
        // Apply poison to target
        if (target != null && !target.level().isClientSide) {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 160, 1)); // 8 seconds, level II
        }
        return amount;
    }
    
    @OnlyIn(Dist.CLIENT)
    @Override
    public void showFX(LivingEntity champion) {
        if (champion.level().random.nextInt(3) != 0) {
            return;
        }
        
        // Poison particles (dark green)
        double w = champion.level().random.nextFloat() * champion.getBbWidth();
        double d = champion.level().random.nextFloat() * champion.getBbWidth();
        double h = champion.level().random.nextFloat() * champion.getBbHeight();
        
        champion.level().addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.1f, 0.5f, 0.1f),
                champion.getBoundingBox().minX + w,
                champion.getBoundingBox().minY + h,
                champion.getBoundingBox().minZ + d,
                0, 0, 0);
    }
}




