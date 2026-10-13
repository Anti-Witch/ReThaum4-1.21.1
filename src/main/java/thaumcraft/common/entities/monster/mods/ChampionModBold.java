package thaumcraft.common.entities.monster.mods;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Bold Champion Modifier - Champion mobs with this modifier are leaders.
 * 
 * Effects:
 * - No special damage modification
 * - Shows golden spark particles
 * - Inspires nearby mobs (visual effect only)
 * 
 * Type: -1 (Universal)
 */
public class ChampionModBold implements IChampionModifierEffect {
    
    @Override
    public float performEffect(LivingEntity champion, LivingEntity target, DamageSource source, float amount) {
        // Bold modifier doesn't modify damage - it's about inspiring nearby mobs
        return 0.0f;
    }
    
    @OnlyIn(Dist.CLIENT)
    @Override
    public void showFX(LivingEntity champion) {
        if (champion.level().random.nextBoolean()) {
            return;
        }
        
        // Golden spark particles around the champion
        double w = champion.level().random.nextFloat() * champion.getBbWidth();
        double d = champion.level().random.nextFloat() * champion.getBbWidth();
        double h = champion.level().random.nextFloat() * champion.getBbHeight() / 3.0;
        
        champion.level().addParticle(ParticleTypes.ELECTRIC_SPARK,
                champion.getBoundingBox().minX + w,
                champion.getBoundingBox().minY + h,
                champion.getBoundingBox().minZ + d,
                0, 0.02, 0);
    }
}




