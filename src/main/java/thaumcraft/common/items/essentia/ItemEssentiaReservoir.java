package thaumcraft.common.items.essentia;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * ItemEssentiaReservoir - Item form of the Essentia Reservoir.
 * Large essentia storage container.
 */
public class ItemEssentiaReservoir extends Item {

    public ItemEssentiaReservoir() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.RARE)
                .fireResistant());
    }

    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        tooltip.add(net.minecraft.network.chat.Component.translatable("item.thaumcraft.essentia_reservoir.desc")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
