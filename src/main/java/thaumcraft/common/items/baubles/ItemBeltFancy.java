package thaumcraft.common.items.baubles;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

/**
 * ItemBeltFancy - Fancy Belt.
 * Increases inventory space or provides other utility.
 */
public class ItemBeltFancy extends Item implements ICurioItem {

    public ItemBeltFancy() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON)
                .fireResistant());
    }

    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        tooltip.add(Component.translatable("item.thaumcraft.belt_fancy.desc")
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
    }
}
