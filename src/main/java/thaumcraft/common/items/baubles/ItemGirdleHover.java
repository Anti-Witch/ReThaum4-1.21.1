package thaumcraft.common.items.baubles;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

/**
 * Thaumostatic Girdle (TC4 ItemGirdleHover).
 *
 * Belt bauble. TC4 ItemGirdleHover.onWornTick only reduced the wearer's
 * fall distance by 0.33 per tick (which softens, but does not remove, fall
 * damage). It also has a synergy with the Thaumostatic Harness: while worn in
 * the belt slot the harness flies faster (see
 * {@link thaumcraft.common.items.armor.Hover}) but burns fuel 20% faster.
 */
public class ItemGirdleHover extends Item implements ICurioItem {

    /** TC4: fallDistance -= 0.33 each tick while worn. */
    private static final float FALL_REDUCTION = 0.33F;

    public ItemGirdleHover() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.RARE));
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        // TC4 onWornTick: only the fall distance is touched, no potion effects
        if (slotContext.entity() instanceof Player player && player.fallDistance > 0.0F) {
            player.fallDistance -= FALL_REDUCTION;
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.thaumcraft.girdle_hover.desc")
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return slotContext.identifier().equals("belt");
    }

    @Override
    public ICurio.SoundInfo getEquipSound(SlotContext slotContext, ItemStack stack) {
        return new ICurio.SoundInfo(SoundEvents.ARMOR_EQUIP_GENERIC.value(), 1.0F, 1.0F);
    }
}
