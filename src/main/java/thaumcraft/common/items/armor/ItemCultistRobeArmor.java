package thaumcraft.common.items.armor;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.api.items.IVisDiscountGear;
import thaumcraft.api.items.IWarpingGear;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Crimson Cult Robe Armor - Worn by members of the Crimson Cult.
 * Provides small vis discount but causes warp.
 */
public class ItemCultistRobeArmor extends ArmorItem implements IVisDiscountGear, IWarpingGear {
    
    public ItemCultistRobeArmor(Type type) {
        super(ThaumcraftMaterials.ARMORMAT_CULTIST_ROBE, type, 
                new Item.Properties()
                        .stacksTo(1)
                        .rarity(Rarity.UNCOMMON));
    }
    
    // Factory methods
    public static ItemCultistRobeArmor createHelmet() {
        return new ItemCultistRobeArmor(Type.HELMET);
    }
    
    public static ItemCultistRobeArmor createChestplate() {
        return new ItemCultistRobeArmor(Type.CHESTPLATE);
    }
    
    public static ItemCultistRobeArmor createLeggings() {
        return new ItemCultistRobeArmor(Type.LEGGINGS);
    }
    
    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(Items.IRON_INGOT) || super.isValidRepairItem(toRepair, repair);
    }
    
    @Nullable
    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return ResourceLocation.parse("thaumcraft:textures/models/cultist_robe_armor.png");
    }
    
    @Override
    public int getVisDiscount(ItemStack stack, Player player) {
        return 1; // Small discount
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.thaumcraft.vis_discount", getVisDiscount(stack, null))
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("item.thaumcraft.warp_too")
                .withStyle(ChatFormatting.DARK_RED));
        super.appendHoverText(stack, context, tooltip, flag);
    }
    
    @Override
    public int getWarp(ItemStack itemstack, Player player) {
        return 1; // Causes warp
    }
}




