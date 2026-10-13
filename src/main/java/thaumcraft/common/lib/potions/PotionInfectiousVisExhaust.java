package thaumcraft.common.lib.potions;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import thaumcraft.init.ModEffects;

import java.util.List;

/**
 * Infectious Vis Exhaustion potion effect.
 * Spreads vis exhaustion to nearby entities.
 * Amplifier determines how many times it can spread before becoming regular vis exhaust.
 * 
 * Ported from 1.12.2
 */
public class PotionInfectiousVisExhaust extends MobEffect {
    
    public PotionInfectiousVisExhaust() {
        super(MobEffectCategory.HARMFUL, 0x557755); // Gray-green color
    }
    
    @Override
    public boolean applyEffectTick(LivingEntity target, int amplifier) {
        if (target.level().isClientSide) return true;

        Level level = target.level();
        AABB searchBox = target.getBoundingBox().inflate(4.0, 4.0, 4.0);
        List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(LivingEntity.class, searchBox);

        for (LivingEntity entity : nearbyEntities) {
            if (entity == target) continue;

            // Don't infect entities that already have this effect
            if (!entity.hasEffect(ModEffects.INFECTIOUS_VIS_EXHAUST)) {
                if (amplifier > 0) {
                    // Spread with reduced amplifier
                    entity.addEffect(new MobEffectInstance(
                            ModEffects.INFECTIOUS_VIS_EXHAUST,
                            6000,
                            amplifier - 1,
                            false,
                            true
                    ));
                } else {
                    // Spread as regular vis exhaust
                    entity.addEffect(new MobEffectInstance(
                            ModEffects.VIS_EXHAUST,
                            6000,
                            0,
                            false,
                            true
                    ));
                }
            }
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 40 == 0; // Every 2 seconds
    }
}




