package thaumcraft.common.golems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import javax.annotation.Nullable;

import java.util.List;

/**
 * Item representing a golem upgrade.
 * Upgrades enhance golem capabilities (speed, strength, durability, stealth, flight).
 */
public class ItemGolemUpgrade extends Item {
    private final String upgradeType;

    public ItemGolemUpgrade(String upgradeType) {
        this(upgradeType, new Item.Properties().stacksTo(64));
    }

    public ItemGolemUpgrade(String upgradeType, Item.Properties properties) {
        super(properties);
        this.upgradeType = upgradeType;
    }

    public String getUpgradeType() {
        return upgradeType;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.thaumcraft.golem_upgrade." + upgradeType + ".desc")
                .withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, ctx, tooltip, flag);
    }
}
