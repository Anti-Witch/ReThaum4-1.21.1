package thaumcraft.common.lib.potions;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Death Gaze potion effect.
 * Causes the player to apply wither to entities they look at.
 * The actual effect is handled in WarpEvents.checkDeathGaze().
 * 
 * Ported from 1.12.2
 */
public class PotionDeathGaze extends MobEffect {
    
    public PotionDeathGaze() {
        super(MobEffectCategory.HARMFUL, 0x220022); // Dark purple color
    }
    
    @Override
    public boolean applyEffectTick(LivingEntity target, int amplifier) {
        // Effect is handled by WarpEvents.checkDeathGaze()
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return false; // No periodic effect here
    }
}




