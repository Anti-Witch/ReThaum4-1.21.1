package thaumcraft.common.items.armor;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.api.items.IGoggles;
import thaumcraft.api.items.IRevealer;
import thaumcraft.api.items.IVisDiscountGear;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Goggles of Revealing - Allows the player to see aura information, 
 * essentia contents of blocks, and provides a small vis discount.
 * 
 * Can be worn as helmet armor or potentially as a Curios bauble.
 */
public class ItemGoggles extends ArmorItem implements IVisDiscountGear, IRevealer, IGoggles {
    
    public ItemGoggles() {
        super(ThaumcraftMaterials.ARMORMAT_SPECIAL, Type.HELMET, 
                new Item.Properties()
                        .stacksTo(1)
                        .durability(350)
                        .rarity(Rarity.RARE));
    }
    
    @Override
    public int getVisDiscount(ItemStack stack, Player player) {
        return 5;
    }
    
    @Override
    public boolean showNodes(ItemStack itemstack, LivingEntity player) {
        return true;
    }
    
    @Override
    public boolean showIngamePopups(ItemStack itemstack, LivingEntity player) {
        return true;
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.thaumcraft.vis_discount", getVisDiscount(stack, null))
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("item.thaumcraft.goggles.desc")
                .withStyle(ChatFormatting.DARK_AQUA));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        // TC4 ItemGoggles.getIsRepairable: gold ingot
        return repair.is(Items.GOLD_INGOT) || super.isValidRepairItem(toRepair, repair);
    }
    
    @Nullable
    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return ResourceLocation.parse("thaumcraft:textures/models/goggles.png");
    }
    
    // TODO: Add Curios integration for wearing as bauble
    // This would require adding the Curios API as a dependency and implementing ICurioItem
}




