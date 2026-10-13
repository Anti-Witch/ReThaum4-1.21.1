package thaumcraft.common.items.consumables;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;

/**
 * Triple Meat Treat - Special food item made from three types of meat.
 * Provides good nutrition and has a chance to grant regeneration.
 * Can be eaten even when not hungry.
 * 
 * Ported to 1.20.1
 */
public class ItemTripleMeatTreat extends Item {
    
    public ItemTripleMeatTreat() {
        super(new Properties()
                .food(new FoodProperties.Builder()
                        .nutrition(6)
                        .saturationModifier(0.8f)
                        .alwaysEdible() // Can eat even when full
                        .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 100, 0), 0.66f)
                        .build()));
    }
}




