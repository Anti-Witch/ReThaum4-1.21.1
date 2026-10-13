package thaumcraft.common.items.baubles;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.HashMap;
import java.util.UUID;

/**
 * Cloud Ring - A ring that allows double-jumping while in the air.
 * When the player presses jump while airborne, they get a boost upward.
 */
public class ItemCloudRing extends Item implements ICurioItem {

    // Track which players have used their double jump
    private static final HashMap<UUID, Boolean> jumpUsed = new HashMap<>();

    public ItemCloudRing() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.RARE));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (!(entity instanceof Player player)) {
            return;
        }

        UUID playerId = player.getUUID();

        // Reset jump when on ground
        if (player.onGround()) {
            jumpUsed.remove(playerId);
            return;
        }

        // The actual double-jump logic would need client-side input handling
        // For now, we provide a passive slow-fall effect when falling
        if (!level.isClientSide() && !player.onGround() && !player.isInWater()) {
            Vec3 motion = player.getDeltaMovement();

            // If falling, reduce fall speed
            if (motion.y < -0.1) {
                // Apply slow fall effect
                player.setDeltaMovement(motion.x, Math.max(motion.y, -0.4), motion.z);
                player.fallDistance = Math.max(0, player.fallDistance - 0.5f);
            }
        }
    }

    /**
     * Called when player attempts to double-jump (from key input handler).
     * This would be triggered by client-side input detection.
     */
    public static boolean tryDoubleJump(Player player) {
        UUID playerId = player.getUUID();

        if (!player.onGround() && !player.isInWater() && !jumpUsed.getOrDefault(playerId, false)) {
            jumpUsed.put(playerId, true);

            // Apply upward boost
            double boost = 0.75;
            if (player.hasEffect(MobEffects.JUMP)) {
                boost += (player.getEffect(MobEffects.JUMP).getAmplifier() + 1) * 0.1;
            }

            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x, boost, motion.z);

            // Forward boost if sprinting
            if (player.isSprinting()) {
                float yaw = player.getYRot() * 0.017453292f;
                player.setDeltaMovement(
                        player.getDeltaMovement().add(
                                -Math.sin(yaw) * 0.2,
                                0,
                                Math.cos(yaw) * 0.2
                        )
                );
            }

            player.fallDistance = 0.0f;
            player.hasImpulse = true;

            return true;
        }
        return false;
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return slotContext.identifier().equals("ring");
    }

    @Override
    public ICurio.SoundInfo getEquipSound(SlotContext slotContext, ItemStack stack) {
        return new ICurio.SoundInfo(SoundEvents.ARMOR_EQUIP_GENERIC.value(), 1.0F, 1.0F);
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        inventoryTick(stack, slotContext.entity().level(), slotContext.entity(), slotContext.index(), false);
    }
}
