package thaumcraft.common.entities.monster.mods;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Dummy Champion Modifier - A placeholder/null modifier with no effects.
 * 
 * Used as a default when no modifier should be applied.
 */
public class ChampionModDummy implements IChampionModifierEffect {
    
    @Override
    public float performEffect(LivingEntity champion, LivingEntity target, DamageSource source, float amount) {
        // No effect
        return amount;
    }
    
    @OnlyIn(Dist.CLIENT)
    @Override
    public void showFX(LivingEntity champion) {
        // No visual effect
    }
}




