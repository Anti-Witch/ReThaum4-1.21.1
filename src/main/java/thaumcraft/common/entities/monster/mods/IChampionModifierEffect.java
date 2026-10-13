package thaumcraft.common.entities.monster.mods;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Interface for Champion Modifier effects.
 * Champion mobs are enhanced enemies with special abilities that spawn in the world.
 * Each modifier provides unique combat behaviors and visual effects.
 * 
 * Ported to 1.20.1
 */
public interface IChampionModifierEffect {
    
    /**
     * Called when the champion mob takes or deals damage.
     * Can modify the damage amount or trigger special effects.
     * 
     * @param champion The champion mob entity
     * @param target The target entity (may be null)
     * @param source The damage source
     * @param amount The original damage amount
     * @return The modified damage amount
     */
    float performEffect(LivingEntity champion, LivingEntity target, DamageSource source, float amount);
    
    /**
     * Called each tick on the client to show visual effects.
     * @param champion The champion mob entity
     */
    @OnlyIn(Dist.CLIENT)
    void showFX(LivingEntity champion);
    
    /**
     * Called before rendering the champion mob.
     * Can be used to add render layers or modify rendering.
     * @param champion The champion mob entity
     */
    @OnlyIn(Dist.CLIENT)
    default void preRender(LivingEntity champion) {
        // Default implementation does nothing
    }
}




