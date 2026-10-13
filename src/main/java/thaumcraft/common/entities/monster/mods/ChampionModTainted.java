package thaumcraft.common.entities.monster.mods;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.api.aura.AuraHelper;

/**
 * Tainted Champion Modifier - Champion mobs infected with taint.
 * 
 * Effects:
 * - Converts passive mobs into aggressive ones
 * - Pollutes aura on death
 * - Shows purple taint particles
 * 
 * Type: 0 (Defensive - converts mob behavior)
 */
public class ChampionModTainted implements IChampionModifierEffect {
    
    @Override
    public float performEffect(LivingEntity champion, LivingEntity target, DamageSource source, float amount) {
        // If this is a non-monster mob, convert it to be aggressive
        if (champion instanceof Mob mob && !champion.level().isClientSide) {
            // Ensure the mob has attack damage
            AttributeInstance attackDamage = mob.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attackDamage == null || attackDamage.getBaseValue() <= 0) {
                if (attackDamage != null) {
                    attackDamage.setBaseValue(Math.max(2.0f, (mob.getBbHeight() + mob.getBbWidth()) * 2.0f));
                }
            }
        }
        
        // Check if this damage will kill the champion
        if (!champion.level().isClientSide && champion.getHealth() - amount <= 0) {
            // Pollute aura on death
            AuraHelper.polluteAura(champion.level(), champion.blockPosition(), 5.0f, true);
        }
        
        return amount;
    }
    
    @OnlyIn(Dist.CLIENT)
    @Override
    public void showFX(LivingEntity champion) {
        // Purple taint particles
        double w = champion.level().random.nextFloat() * champion.getBbWidth();
        double d = champion.level().random.nextFloat() * champion.getBbWidth();
        double h = champion.level().random.nextFloat() * champion.getBbHeight();
        
        champion.level().addParticle(ParticleTypes.WITCH,
                champion.getBoundingBox().minX + w,
                champion.getBoundingBox().minY + h,
                champion.getBoundingBox().minZ + d,
                0, -0.01, 0);
    }
    
    @OnlyIn(Dist.CLIENT)
    @Override
    public void preRender(LivingEntity champion) {
        // TODO: Add tainted render layer when client rendering is implemented
    }
}




