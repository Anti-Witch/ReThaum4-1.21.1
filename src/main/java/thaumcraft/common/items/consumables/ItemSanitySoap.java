package thaumcraft.common.items.consumables;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.init.ModEffects;
import thaumcraft.init.ModSounds;

/**
 * Sanity Soap - Use to scrub away your warp.
 * Removes temporary warp completely and reduces normal warp.
 * More effective when used in purifying fluid.
 */
public class ItemSanitySoap extends Item {

    public ItemSanitySoap() {
        super(new Item.Properties());
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 100; // 5 seconds of scrubbing
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        int ticksUsed = getUseDuration(stack, entity) - remainingUseDuration;

        // Stop after 95 ticks
        if (ticksUsed > 95) {
            entity.stopUsingItem();
        }

        // Play scrubbing sounds and particles on client
        if (level.isClientSide()) {
            if (level.random.nextFloat() < 0.2f) {
                level.playLocalSound(entity.getX(), entity.getY(), entity.getZ(),
                        SoundEvents.CHORUS_FLOWER_DEATH, SoundSource.PLAYERS,
                        0.1f, 1.5f + level.random.nextFloat() * 0.2f, false);
            }
            // TODO: Add bubble particles (FXDispatcher.crucibleBubble)
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        int ticksUsed = getUseDuration(stack, entity) - timeLeft;

        // Only apply effect if used long enough
        if (ticksUsed > 95 && entity instanceof Player player) {
            stack.shrink(1);

            if (!level.isClientSide()) {
                IPlayerWarp warp = ThaumcraftCapabilities.getWarp(player);
                if (warp != null) {
                    int amountToRemove = 1;

                    // Check for Warp Ward potion effect - adds +1
                    if (player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.WARP_WARD.get()))) {
                        amountToRemove++;
                    }
                    
                    // TODO: Check if standing in purifying fluid - adds +1

                    // Remove normal warp
                    if (warp.get(IPlayerWarp.EnumWarpType.NORMAL) > 0) {
                        warp.add(IPlayerWarp.EnumWarpType.NORMAL, -amountToRemove);
                    }

                    // Remove all temporary warp
                    int tempWarp = warp.get(IPlayerWarp.EnumWarpType.TEMPORARY);
                    if (tempWarp > 0) {
                        warp.add(IPlayerWarp.EnumWarpType.TEMPORARY, -tempWarp);
                    }

                    if (player instanceof ServerPlayer serverPlayer) {
                        warp.sync(serverPlayer);
                    }
                }
            } else {
                // Play success sound and particles on client
                level.playLocalSound(entity.getX(), entity.getY(), entity.getZ(),
                        ModSounds.CRAFT_START.get(), SoundSource.PLAYERS,
                        0.25f, 1.0f, false);
                // TODO: Add more bubble particles
            }
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        // This gets called if use duration completes - redirect to releaseUsing
        releaseUsing(stack, level, entity, 0);
        return stack;
    }
}




