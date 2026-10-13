package thaumcraft.common.lib.potions;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Unnatural Hunger potion effect.
 * Rapidly drains food/saturation, making the player constantly hungry.
 * Can only be cured by eating rotten flesh or zombie brain.
 * 
 * Ported from 1.12.2
 */
public class PotionUnnaturalHunger extends MobEffect {
    
    public PotionUnnaturalHunger() {
        super(MobEffectCategory.HARMFUL, 0x884422); // Brown color
    }
    
    @Override
    public boolean applyEffectTick(LivingEntity target, int amplifier) {
        if (target.level().isClientSide) return true;

        if (target instanceof Player player) {
            // Add exhaustion to drain food faster
            // 0.025 per tick at amplifier 0, scales with amplifier
            player.causeFoodExhaustion(0.025f * (amplifier + 1));
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true; // Every tick
    }
}




