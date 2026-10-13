package thaumcraft.common.entities.monster.mods;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Spined Champion Modifier - Champion mobs with thorns effect.
 * 
 * Effects:
 * - Reflects 25% of melee damage back to attacker
 * - Does not reflect projectile or magic damage
 * - Shows purple thorn particles
 * 
 * Type: 2 (Physical)
 */
public class ChampionModSpined implements IChampionModifierEffect {
    
    @Override
    public float performEffect(LivingEntity champion, LivingEntity target, DamageSource source, float amount) {
        // Reflect damage back to melee attackers
        if (target != null && !source.is(DamageTypeTags.IS_PROJECTILE) && 
                !source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            // Reflect 25% of incoming damage
            float reflectedDamage = amount * 0.25f;
            if (reflectedDamage > 0.5f) {
                target.hurt(champion.damageSources().thorns(champion), reflectedDamage);
            }
        }
        return amount;
    }
    
    @OnlyIn(Dist.CLIENT)
    @Override
    public void showFX(LivingEntity champion) {
        if (champion.level().random.nextInt(3) != 0) {
            return;
        }
        
        // Purple thorn-like particles
        double w = champion.level().random.nextFloat() * champion.getBbWidth();
        double d = champion.level().random.nextFloat() * champion.getBbWidth();
        double h = champion.level().random.nextFloat() * champion.getBbHeight();
        
        champion.level().addParticle(ParticleTypes.WITCH,
                champion.getBoundingBox().minX + w,
                champion.getBoundingBox().minY + h,
                champion.getBoundingBox().minZ + d,
                0, 0.02, 0);
    }
}




