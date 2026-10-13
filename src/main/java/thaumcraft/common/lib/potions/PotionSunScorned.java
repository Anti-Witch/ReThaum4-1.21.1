package thaumcraft.common.lib.potions;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Sun Scorned potion effect.
 * Burns the entity when in sunlight, but heals them in darkness.
 * 
 * Ported from 1.12.2
 */
public class PotionSunScorned extends MobEffect {
    
    public PotionSunScorned() {
        super(MobEffectCategory.HARMFUL, 0xFF8800); // Orange color
    }
    
    @Override
    public boolean applyEffectTick(LivingEntity target, int amplifier) {
        if (target.level().isClientSide) return true;

        Level level = target.level();
        float brightness = target.getLightLevelDependentMagicValue();
        BlockPos pos = BlockPos.containing(
                Mth.floor(target.getX()),
                Mth.floor(target.getY()),
                Mth.floor(target.getZ())
        );

        // In bright light with sky visibility - burn
        if (brightness > 0.5f
                && level.random.nextFloat() * 30.0f < (brightness - 0.4f) * 2.0f
                && level.canSeeSky(pos)) {
            target.setRemainingFireTicks(4 * 20);
        }
        // In darkness - heal
        else if (brightness < 0.25f && level.random.nextFloat() > brightness * 2.0f) {
            target.heal(1.0f);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 40 == 0; // Every 2 seconds
    }
}




