package thaumcraft.common.entities.monster.mods;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Undying Champion Modifier - Champion mobs with regeneration.
 * 
 * Effects:
 * - Regenerates 0.5 health per second
 * - Shows heart particles
 * 
 * Type: 0 (Defensive)
 */
public class ChampionModUndying implements IChampionModifierEffect {
    
    @Override
    public float performEffect(LivingEntity champion, LivingEntity target, DamageSource source, float amount) {
        // Regenerate health periodically
        if (!champion.level().isClientSide && champion.tickCount % 20 == 0) {
            float currentHealth = champion.getHealth();
            float maxHealth = champion.getMaxHealth();
            if (currentHealth < maxHealth) {
                champion.setHealth(Math.min(currentHealth + 0.5f, maxHealth));
            }
        }
        return amount;
    }
    
    @OnlyIn(Dist.CLIENT)
    @Override
    public void showFX(LivingEntity champion) {
        if (champion.level().random.nextInt(10) != 0) {
            return;
        }
        
        // Heart particles
        double w = champion.level().random.nextFloat() * champion.getBbWidth();
        double d = champion.level().random.nextFloat() * champion.getBbWidth();
        double h = champion.level().random.nextFloat() * champion.getBbHeight();
        
        champion.level().addParticle(ParticleTypes.HEART,
                champion.getBoundingBox().minX + w,
                champion.getBoundingBox().minY + h,
                champion.getBoundingBox().minZ + d,
                0, 0.1, 0);
    }
}




