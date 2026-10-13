package thaumcraft.api.potions;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Vis Exhaustion potion effect.
 * Prevents vis regeneration and drains stored vis.
 */
public class PotionVisExhaust extends MobEffect {
    
    public PotionVisExhaust() {
        super(MobEffectCategory.HARMFUL, 0x4444FF);
    }
    
    @Override
    public boolean applyEffectTick(LivingEntity target, int amplifier) {
        // TODO: Drain vis from player's wand/caster when vis system is implemented
        return true;
    }
    
    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }
}
