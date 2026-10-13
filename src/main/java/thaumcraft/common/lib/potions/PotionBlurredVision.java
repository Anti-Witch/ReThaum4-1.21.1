package thaumcraft.common.lib.potions;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Blurred Vision potion effect.
 * Causes visual distortion on the client side.
 * The actual rendering effect is handled by client-side shader/overlay code.
 * 
 * Ported from 1.12.2
 */
public class PotionBlurredVision extends MobEffect {
    
    public PotionBlurredVision() {
        super(MobEffectCategory.HARMFUL, 0x888888); // Gray color
    }
    
    @Override
    public boolean applyEffectTick(LivingEntity target, int amplifier) {
        // Effect is visual only, handled on client
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return false; // No periodic effect
    }
}




