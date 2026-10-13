package thaumcraft.api.potions;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import thaumcraft.api.damagesource.DamageSourceThaumcraft;
import thaumcraft.api.entities.ITaintedMob;

/**
 * Flux Taint potion effect.
 * Damages non-tainted entities while healing tainted ones.
 */
public class PotionFluxTaint extends MobEffect {
    
    public PotionFluxTaint() {
        super(MobEffectCategory.HARMFUL, 0x6600AA);
    }
    
    @Override
    public boolean applyEffectTick(LivingEntity target, int amplifier) {
        // Check if entity is tainted (heals them)
        if (target instanceof ITaintedMob) {
            target.heal(1);
            return true;
        }
        
        // Damage non-undead entities
        if (!target.isInvertedHealAndHarm()) {
            if (target instanceof Player || target.getMaxHealth() > 1) {
                target.hurt(DamageSourceThaumcraft.createTaint(target.level()), 1);
            }
        }
        return true;
    }
    
    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        int k = 40 >> amplifier;
        return k > 0 ? duration % k == 0 : true;
    }
}
