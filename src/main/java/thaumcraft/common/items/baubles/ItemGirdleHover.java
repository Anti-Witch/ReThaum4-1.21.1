package thaumcraft.common.items.baubles;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

/**
 * ItemGirdleHover - Girdle of Hovering.
 * Allows the wearer to hover/fly slowly.
 */
public class ItemGirdleHover extends Item implements ICurioItem {

    public ItemGirdleHover() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.RARE)
                .fireResistant());
    }

    public void inventoryTick(ItemStack stack, net.minecraft.world.level.Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && entity instanceof LivingEntity living) {
            // Grant slow falling/hovering effect when worn
            if (selected) {
                living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.SLOW_FALLING, 2, 0, false, false));
            }
        }
        super.inventoryTick(stack, level, entity, slot, selected);
    }

    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        tooltip.add(net.minecraft.network.chat.Component.translatable("item.thaumcraft.girdle_hover.desc")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return slotContext.identifier().equals("belt");
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
