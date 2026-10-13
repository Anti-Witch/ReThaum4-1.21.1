package thaumcraft.common.items.essentia;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * ItemEssentiaResonator - Essentia Resonator item.
 * Used to detect and measure essentia in the aura.
 */
public class ItemEssentiaResonator extends Item {

    public ItemEssentiaResonator() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.RARE)
                .fireResistant());
    }

    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        tooltip.add(Component.translatable("item.thaumcraft.essentia_resonator.desc")
                .withStyle(ChatFormatting.GRAY));
    }
}
