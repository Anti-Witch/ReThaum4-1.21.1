package thaumcraft.common.items.consumables;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.capabilities.IPlayerWarp;

/**
 * Zombie Brain - Edible item that gives temporary warp.
 * Eating has a small chance to give permanent warp.
 * 
 * Ported to 1.20.1
 */
public class ItemZombieBrain extends Item {
    
    public ItemZombieBrain() {
        super(new Properties()
                .food(new FoodProperties.Builder()
                        .nutrition(4)
                        .saturationModifier(0.2f)
                        .effect(() -> new MobEffectInstance(MobEffects.HUNGER, 600, 0), 0.8f)
                        .build()));
    }
    
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof Player player) {
            // Small chance for permanent warp, otherwise temporary
            if (level.random.nextFloat() < 0.1f) {
                ThaumcraftApi.internalMethods.addWarpToPlayer(player, 1, IPlayerWarp.EnumWarpType.NORMAL);
            } else {
                ThaumcraftApi.internalMethods.addWarpToPlayer(player, 1 + level.random.nextInt(3), IPlayerWarp.EnumWarpType.TEMPORARY);
            }
        }
        return super.finishUsingItem(stack, level, entity);
    }
}




