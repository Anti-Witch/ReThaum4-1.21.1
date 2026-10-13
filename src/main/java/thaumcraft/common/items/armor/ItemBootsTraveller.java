package thaumcraft.common.items.armor;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.api.items.IRechargable;
import thaumcraft.api.items.RechargeHelper;

import javax.annotation.Nullable;

/**
 * Boots of the Traveller - Magical boots that grant speed and step height.
 * Consumes vis charge while providing the movement bonus.
 *
 * TC4 ItemBootsTraveller.onArmorTick:
 * - while moving forward (not flying): client-side stepHeight = 1.0 (!sneaking),
 *   ground forward boost 0.055 (moveFlying), air control 0.03/0.05
 * - fallDistance -= 0.25 every tick
 * TC4 restored the step height in EventHandlerEntity when sneaking/not worn -
 * here that is an Attributes.STEP_HEIGHT transient modifier (1.0 total, i.e.
 * +0.4 over the vanilla 0.6).
 */
public class ItemBootsTraveller extends ArmorItem implements IRechargable {

    /** TC4 restored stepHeight when the boots were removed mid-movement. */
    private static final ResourceLocation STEP_HEIGHT_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "traveller_boots_step_height");
    private static final double STEP_HEIGHT_BONUS = 0.4D;

    public ItemBootsTraveller() {
        super(ThaumcraftMaterials.ARMORMAT_SPECIAL, Type.BOOTS, 
                new Item.Properties()
                        .stacksTo(1)
                        .durability(350)
                        .rarity(Rarity.RARE));
    }
    
    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(Items.LEATHER) || super.isValidRepairItem(toRepair, repair);
    }
    
    @Nullable
    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return ResourceLocation.parse("thaumcraft:textures/models/bootstraveler.png");
    }
    
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        
        // Only process if worn on feet and entity is a player
        if (!(entity instanceof Player player)) return;
        
        // Check if boots are actually equipped
        ItemStack feetStack = player.getItemBySlot(EquipmentSlot.FEET);
        if (feetStack != stack) {
            if (!level.isClientSide) {
                clearStepHeight(player);
            }
            return;
        }
        
        boolean hasCharge = RechargeHelper.getCharge(stack) > 0;
        
        // Energy management - consume charge periodically
        if (!level.isClientSide() && player.tickCount % 20 == 0) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> {
                int energy = t.getInt("energy");

                if (energy > 0) {
                    energy--;
                } else if (energy <= 0 && RechargeHelper.consumeCharge(stack, player, 1)) {
                    energy = 60; // Recharge internal energy buffer
                }

                t.putInt("energy", energy);
            });
        }

        // TC4: fallDistance -= 0.25 each tick (reduces fall damage)
        if (player.fallDistance > 0.0F) {
            player.fallDistance -= 0.25F;
        }

        boolean movingForward = player.zza > 0.0F;

        // TC4: stepHeight 1.0 (vanilla 0.6 here) while moving forward, not
        // sneaking, not flying. Applied server-side so it is synced.
        if (!level.isClientSide) {
            boolean active = hasCharge && movingForward
                    && !player.isShiftKeyDown() && !player.getAbilities().flying;
            AttributeInstance stepHeight = player.getAttribute(Attributes.STEP_HEIGHT);
            if (stepHeight != null) {
                boolean applied = stepHeight.hasModifier(STEP_HEIGHT_MODIFIER_ID);
                if (active && !applied) {
                    stepHeight.addTransientModifier(new AttributeModifier(
                            STEP_HEIGHT_MODIFIER_ID, STEP_HEIGHT_BONUS,
                            AttributeModifier.Operation.ADD_VALUE));
                } else if (!active && applied) {
                    stepHeight.removeModifier(STEP_HEIGHT_MODIFIER_ID);
                }
            }
        }

        // Apply movement bonuses if we have charge and player is moving forward
        if (hasCharge && !player.getAbilities().flying && movingForward) {
            if (player.onGround()) {
                // Ground speed boost
                float bonus = 0.05f;
                if (player.isInWater()) {
                    bonus /= 4.0f;
                }
                
                // Apply forward movement boost
                Vec3 lookAngle = player.getLookAngle();
                Vec3 horizontalLook = new Vec3(lookAngle.x, 0, lookAngle.z).normalize();
                player.setDeltaMovement(player.getDeltaMovement().add(
                        horizontalLook.x * bonus,
                        0,
                        horizontalLook.z * bonus
                ));
            } else {
                // Air control boost
                if (player.isInWater()) {
                    Vec3 lookAngle = player.getLookAngle();
                    Vec3 horizontalLook = new Vec3(lookAngle.x, 0, lookAngle.z).normalize();
                    player.setDeltaMovement(player.getDeltaMovement().add(
                            horizontalLook.x * 0.025f,
                            0,
                            horizontalLook.z * 0.025f
                    ));
                }
                // Improved air control - add small forward boost while in air
                Vec3 lookAngle = player.getLookAngle();
                Vec3 horizontalLook = new Vec3(lookAngle.x, 0, lookAngle.z).normalize();
                player.setDeltaMovement(player.getDeltaMovement().add(
                        horizontalLook.x * 0.01f,
                        0,
                        horizontalLook.z * 0.01f
                ));
            }
        }
    }

    /** Remove the step height bonus (TC4 restored stepHeight when unworn/sneaking). */
    public static void clearStepHeight(Player player) {
        AttributeInstance stepHeight = player.getAttribute(Attributes.STEP_HEIGHT);
        if (stepHeight != null && stepHeight.hasModifier(STEP_HEIGHT_MODIFIER_ID)) {
            stepHeight.removeModifier(STEP_HEIGHT_MODIFIER_ID);
        }
    }
    
    // ==================== IRechargable Implementation ====================
    
    @Override
    public int getMaxCharge(ItemStack stack, LivingEntity entity) {
        return 240;
    }
    
    @Override
    public EnumChargeDisplay showInHud(ItemStack stack, LivingEntity entity) {
        return EnumChargeDisplay.PERIODIC;
    }
}




