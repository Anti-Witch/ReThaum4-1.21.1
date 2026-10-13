package thaumcraft.common.items.essentia;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import thaumcraft.common.blocks.essentia.BlockEssentiaTransport;

import java.util.List;

/**
 * ItemEssentiaTransfuser - Item form of Essentia Transfuser (Input/Output).
 * Transfers essentia between jars and tubes.
 */
public class ItemEssentiaTransfuser extends Item {

    private final BlockEssentiaTransport.TransportType transportType;

    public ItemEssentiaTransfuser(BlockEssentiaTransport.TransportType type) {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON)
                .fireResistant());
        this.transportType = type;
    }

    public BlockEssentiaTransport.TransportType getTransportType() {
        return transportType;
    }

    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        String descKey = switch (transportType) {
            case INPUT -> "item.thaumcraft.essentia_input.desc";
            case OUTPUT -> "item.thaumcraft.essentia_output.desc";
        };
        tooltip.add(net.minecraft.network.chat.Component.translatable(descKey)
                .withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
