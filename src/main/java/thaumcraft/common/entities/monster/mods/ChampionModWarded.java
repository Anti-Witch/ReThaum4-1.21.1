package thaumcraft.common.entities.monster.mods;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Warded Champion Modifier - Champion mobs with magic resistance.
 * 
 * Effects:
 * - Reduces magic damage by 75%
 * - Shows enchantment glint particles
 * 
 * Type: 0 (Defensive)
 */
public class ChampionModWarded implements IChampionModifierEffect {
    
    @Override
    public float performEffect(LivingEntity champion, LivingEntity target, DamageSource source, float amount) {
        // Reduce magic damage significantly
        if (source.is(DamageTypeTags.WITCH_RESISTANT_TO) || source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            amount *= 0.25f;
        }
        return amount;
    }
    
    @OnlyIn(Dist.CLIENT)
    @Override
    public void showFX(LivingEntity champion) {
        if (champion.level().random.nextInt(4) != 0) {
            return;
        }
        
        // Enchantment/magic particles
        double w = champion.level().random.nextFloat() * champion.getBbWidth();
        double d = champion.level().random.nextFloat() * champion.getBbWidth();
        double h = champion.level().random.nextFloat() * champion.getBbHeight();
        
        champion.level().addParticle(ParticleTypes.ENCHANT,
                champion.getBoundingBox().minX + w,
                champion.getBoundingBox().minY + h,
                champion.getBoundingBox().minZ + d,
                0, 0.1, 0);
    }
}




