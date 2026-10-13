package thaumcraft.common.items.armor;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.api.items.IGoggles;
import thaumcraft.api.items.IRevealer;
import thaumcraft.init.ModItems;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Fortress Armor - Heavy battle mage armor.
 * The helmet can have goggles attached and different mask variants.
 * Provides bonus armor when wearing multiple pieces.
 */
public class ItemFortressArmor extends ArmorItem implements IGoggles, IRevealer {
    
    public ItemFortressArmor(Type type) {
        super(ThaumcraftMaterials.ARMORMAT_FORTRESS, type, 
                new Item.Properties()
                        .stacksTo(1)
                        .rarity(Rarity.RARE));
    }
    
    // Factory methods
    public static ItemFortressArmor createHelmet() {
        return new ItemFortressArmor(Type.HELMET);
    }
    
    public static ItemFortressArmor createChestplate() {
        return new ItemFortressArmor(Type.CHESTPLATE);
    }
    
    public static ItemFortressArmor createLeggings() {
        return new ItemFortressArmor(Type.LEGGINGS);
    }
    
    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.THAUMIUM_INGOT.get()) || super.isValidRepairItem(toRepair, repair);
    }
    
    @Nullable
    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return ResourceLocation.parse("thaumcraft:textures/models/fortress_armor.png");
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // Show attached goggles
        if (hasGoggles(stack)) {
            tooltip.add(Component.translatable("item.thaumcraft.goggles.name")
                    .withStyle(ChatFormatting.DARK_PURPLE));
        }
        
        // Show mask variant
        if (hasMask(stack)) {
            int maskType = getMaskType(stack);
            tooltip.add(Component.translatable("item.thaumcraft.fortress_helm.mask." + maskType)
                    .withStyle(ChatFormatting.GOLD));
        }
        
        super.appendHoverText(stack, context, tooltip, flag);
    }
    
    // ==================== Goggles Attachment ====================
    
    /**
     * Check if this helmet has goggles attached.
     */
    public static boolean hasGoggles(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        return cd != null && cd.contains("goggles");
    }
    
    /**
     * Attach goggles to this helmet.
     */
    public static void attachGoggles(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putBoolean("goggles", true));
    }
    
    /**
     * Remove goggles from this helmet.
     */
    public static void removeGoggles(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd != null) {
            stack.set(DataComponents.CUSTOM_DATA, cd.update(t -> t.remove("goggles")));
        }
    }
    
    // ==================== Mask System ====================
    
    /**
     * Check if this helmet has a mask.
     */
    public static boolean hasMask(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        return cd != null && cd.contains("mask");
    }
    
    /**
     * Get the mask type (0-3 for different variants).
     */
    public static int getMaskType(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        return cd != null ? cd.copyTag().getInt("mask") : 0;
    }
    
    /**
     * Set the mask type.
     */
    public static void setMask(ItemStack stack, int maskType) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putInt("mask", maskType));
    }
    
    // ==================== IGoggles/IRevealer Implementation ====================
    
    @Override
    public boolean showNodes(ItemStack itemstack, LivingEntity player) {
        return hasGoggles(itemstack) && this.getType() == Type.HELMET;
    }
    
    @Override
    public boolean showIngamePopups(ItemStack itemstack, LivingEntity player) {
        return hasGoggles(itemstack) && this.getType() == Type.HELMET;
    }
    
    // ==================== Set Bonus Calculation ====================
    
    /**
     * Count how many fortress armor pieces the player is wearing.
     */
    public static int countFortressPieces(Player player) {
        int count = 0;
        for (ItemStack armor : player.getArmorSlots()) {
            if (!armor.isEmpty() && armor.getItem() instanceof ItemFortressArmor) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Calculate bonus armor from wearing multiple fortress pieces.
     * Wearing 2+ pieces grants bonus armor.
     */
    public static int getSetBonus(Player player) {
        int pieces = countFortressPieces(player);
        if (pieces >= 2) {
            return pieces - 1; // +1 armor per piece after the first
        }
        return 0;
    }
}




