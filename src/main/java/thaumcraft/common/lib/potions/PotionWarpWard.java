package thaumcraft.common.lib.potions;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Warp Ward potion effect.
 * Beneficial effect that:
 * - Increases effectiveness of sanity soap
 * - Reduces chance of warp events
 * 
 * Ported from 1.12.2
 */
public class PotionWarpWard extends MobEffect {
    
    public PotionWarpWard() {
        super(MobEffectCategory.BENEFICIAL, 0xFFFFDD); // Light yellow color
    }
    
    @Override
    public boolean applyEffectTick(LivingEntity target, int amplifier) {
        // Effect is passive - checked by warp and soap code
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return false; // No periodic effect
    }
}




