package thaumcraft.common.items.armor;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import thaumcraft.api.items.IVisDiscountGear;
import thaumcraft.api.items.IWarpingGear;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Crimson Cult Boots - Standard iron-quality boots worn by cultists.
 * Shared between robe and plate variants.
 * TC4 ItemCultistBoots: vis discount 1% and warp 1.
 */
public class ItemCultistBoots extends ArmorItem implements IVisDiscountGear, IWarpingGear {
    
    public ItemCultistBoots() {
        super(ArmorMaterials.IRON, Type.BOOTS, 
                new Item.Properties()
                        .stacksTo(1)
                        .rarity(Rarity.UNCOMMON));
    }
    
    @Override
    public int getVisDiscount(ItemStack stack, Player player) {
        return 1;
    }

    @Override
    public int getWarp(ItemStack stack, Player player) {
        return 1;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.thaumcraft.vis_discount", getVisDiscount(stack, null))
                .withStyle(ChatFormatting.DARK_PURPLE));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(Items.IRON_INGOT) || super.isValidRepairItem(toRepair, repair);
    }
    
    @Nullable
    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return ResourceLocation.parse("thaumcraft:textures/models/cultistboots.png");
    }
}




