package thaumcraft.common.entities.monster.mods;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Vampiric Champion Modifier - Champion mobs that heal from damage dealt.
 * 
 * Effects:
 * - Heals 25% of damage dealt to targets
 * - Shows crimson/blood particles
 * 
 * Type: 1 (Offensive)
 */
public class ChampionModVampire implements IChampionModifierEffect {
    
    @Override
    public float performEffect(LivingEntity champion, LivingEntity target, DamageSource source, float amount) {
        // Heal based on damage dealt
        if (target != null && !champion.level().isClientSide && amount > 0) {
            float healAmount = amount * 0.25f;
            champion.heal(healAmount);
        }
        return amount;
    }
    
    @OnlyIn(Dist.CLIENT)
    @Override
    public void showFX(LivingEntity champion) {
        if (champion.level().random.nextInt(4) != 0) {
            return;
        }
        
        // Crimson/blood particles
        double w = champion.level().random.nextFloat() * champion.getBbWidth();
        double d = champion.level().random.nextFloat() * champion.getBbWidth();
        double h = champion.level().random.nextFloat() * champion.getBbHeight();
        
        champion.level().addParticle(ParticleTypes.DAMAGE_INDICATOR,
                champion.getBoundingBox().minX + w,
                champion.getBoundingBox().minY + h,
                champion.getBoundingBox().minZ + d,
                0, 0.1, 0);
    }
}




