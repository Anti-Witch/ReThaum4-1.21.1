package thaumcraft.common.items.tools;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

/**
 * Sanity Checker (TC4 ItemSanityChecker).
 *
 * In TC4 this item has no right-click behaviour at all - holding it simply
 * makes the warp meter HUD appear (see HudHandler.renderSanityHud). The old
 * chat spam on use was a port-only placeholder and has been removed.
 */
public class ItemSanityChecker extends Item {

    public ItemSanityChecker() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.thaumcraft.sanity_checker.desc").withStyle(style -> style.withColor(0x808080)));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
