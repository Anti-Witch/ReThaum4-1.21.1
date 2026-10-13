package thaumcraft.common.lib.potions;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import thaumcraft.common.blocks.world.taint.BlockFluxGoo;
import thaumcraft.init.ModBlocks;

/**
 * Thaumarhia potion effect.
 * Causes flux goo to spawn at the entity's location periodically.
 * 
 * Ported from 1.12.2
 */
public class PotionThaumarhia extends MobEffect {
    
    public PotionThaumarhia() {
        super(MobEffectCategory.HARMFUL, 0x9900FF); // Purple color
    }
    
    @Override
    public boolean applyEffectTick(LivingEntity target, int amplifier) {
        if (target.level().isClientSide) return true;

        Level level = target.level();
        BlockPos pos = target.blockPosition();

        // 1 in 15 chance to spawn flux goo at entity position
        if (level.random.nextInt(15) == 0 && level.isEmptyBlock(pos)) {
            level.setBlockAndUpdate(pos, BlockFluxGoo.withLevel(ModBlocks.FLUX_GOO.get(), 2 + level.random.nextInt(3)));
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 0; // Every second
    }
}




