package thaumcraft.common.items.essentia;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import thaumcraft.init.ModBlocks;

import java.util.List;

/**
 * ItemCentrifuge - BlockItem for the Essentia Centrifuge.
 * Allows placing the Centrifuge block from inventory.
 */
public class ItemCentrifuge extends BlockItem {

    public ItemCentrifuge() {
        super(ModBlocks.CENTRIFUGE.get(), new Properties());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.thaumcraft.centrifuge.desc"));
    }
}
