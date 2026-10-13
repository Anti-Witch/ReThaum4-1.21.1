package thaumcraft.common.entities.monster.mods;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Armored Champion Modifier - Champion mobs with enhanced armor.
 * 
 * Effects:
 * - Reduces incoming damage by ~24% (19/25)
 * - Bypass damage ignores this reduction
 * - Shows silver/grey metallic particles
 * 
 * Type: 2 (Physical)
 */
public class ChampionModArmored implements IChampionModifierEffect {
    
    @Override
    public float performEffect(LivingEntity champion, LivingEntity target, DamageSource source, float amount) {
        // Reduce damage that can be blocked
        if (!source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)) {
            float reduced = amount * 19.0f;
            amount = reduced / 25.0f;
        }
        return amount;
    }
    
    @OnlyIn(Dist.CLIENT)
    @Override
    public void showFX(LivingEntity champion) {
        if (champion.level().random.nextInt(4) != 0) {
            return;
        }
        
        // Metallic grey particles around the champion
        double w = champion.level().random.nextFloat() * champion.getBbWidth();
        double d = champion.level().random.nextFloat() * champion.getBbWidth();
        double h = champion.level().random.nextFloat() * champion.getBbHeight();
        
        // Use ash particles for a metallic effect
        champion.level().addParticle(ParticleTypes.ASH,
                champion.getBoundingBox().minX + w,
                champion.getBoundingBox().minY + h,
                champion.getBoundingBox().minZ + d,
                0, 0, 0);
    }
}




