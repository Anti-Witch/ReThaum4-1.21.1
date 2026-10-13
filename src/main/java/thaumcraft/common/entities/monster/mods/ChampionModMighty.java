package thaumcraft.common.entities.monster.mods;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Mighty Champion Modifier - Champion mobs with increased damage output.
 * 
 * Effects:
 * - Increases outgoing damage by 50%
 * - Shows red angry particles
 * 
 * Type: -1 (Universal)
 */
public class ChampionModMighty implements IChampionModifierEffect {
    
    @Override
    public float performEffect(LivingEntity champion, LivingEntity target, DamageSource source, float amount) {
        // Increase outgoing damage by 50%
        return amount * 1.5f;
    }
    
    @OnlyIn(Dist.CLIENT)
    @Override
    public void showFX(LivingEntity champion) {
        if (champion.level().random.nextInt(4) != 0) {
            return;
        }
        
        // Red angry particles
        double w = champion.level().random.nextFloat() * champion.getBbWidth();
        double d = champion.level().random.nextFloat() * champion.getBbWidth();
        double h = champion.level().random.nextFloat() * champion.getBbHeight();
        
        champion.level().addParticle(ParticleTypes.ANGRY_VILLAGER,
                champion.getBoundingBox().minX + w,
                champion.getBoundingBox().minY + h,
                champion.getBoundingBox().minZ + d,
                0, 0, 0);
    }
}




