package thaumcraft.api.wands;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

import java.util.List;

/**
 * ItemFocusBasic - Base class for wand foci (TC4 style).
 * Foci are items that can be equipped on wands to provide active abilities.
 */
public abstract class ItemFocusBasic extends Item {

    public ItemFocusBasic(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.RARE));
    }

    // ============================================================
    // FOCUS PACKAGE MANAGEMENT (stored in CustomData)
    // ============================================================

    public static void setPackage(ItemStack focusStack, FocusPackage pkg) {
        CompoundTag tag = pkg.serialize();
        focusStack.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
    }

    public static FocusPackage getPackage(ItemStack focusStack) {
        if (focusStack == null || focusStack.isEmpty()) {
            return null;
        }
        var customData = focusStack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag != null && !tag.isEmpty()) {
                FocusPackage pkg = new FocusPackage();
                pkg.deserialize(tag);
                return pkg;
            }
        }
        return null;
    }

    // ============================================================
    // UPGRADE SYSTEM
    // ============================================================

    public boolean isUpgradedWith(ItemStack focusStack, FocusUpgradeType type) {
        FocusPackage pkg = getPackage(focusStack);
        return pkg != null && pkg.hasUpgrade(type);
    }

    public int getUpgradeLevel(ItemStack focusStack, FocusUpgradeType type) {
        FocusPackage pkg = getPackage(focusStack);
        return pkg != null ? pkg.getUpgradeLevel(type) : 0;
    }

    public void addUpgrade(ItemStack focusStack, FocusUpgradeType type, int level) {
        FocusPackage pkg = getPackage(focusStack);
        if (pkg == null) {
            pkg = new FocusPackage();
        }
        pkg.addUpgrade(type, level);
        setPackage(focusStack, pkg);
    }

    public void removeUpgrade(ItemStack focusStack, FocusUpgradeType type) {
        FocusPackage pkg = getPackage(focusStack);
        if (pkg != null) {
            pkg.removeUpgrade(type);
            setPackage(focusStack, pkg);
        }
    }

    // ============================================================
    // VIS COST
    // ============================================================

    public abstract AspectList getVisCost(ItemStack focusStack);

    public float getVisCostValue(ItemStack focusStack) {
        AspectList cost = getVisCost(focusStack);
        float total = 0;
        for (Aspect aspect : cost.getAspects()) {
            total += cost.getAmount(aspect);
        }
        return total;
    }

    // ============================================================
    // ACTIVATION
    // ============================================================

    public abstract int getActivationCooldown(ItemStack focusStack);

    public boolean isVisCostPerTick(ItemStack focusStack) {
        return true;
    }

    public WandFocusAnimation getAnimation(ItemStack itemStack) {
        return WandFocusAnimation.CHARGE;
    }

    public ItemStack onFocusRightClick(ItemStack itemStack, Level world, Player player, net.minecraft.world.phys.HitResult hitResult) {
        return itemStack;
    }

    public void onUsingFocusTick(ItemStack stack, Player player, int count) {
    }

    public void onPlayerStoppedUsing(ItemStack stack, Level world, Player player, int count) {
    }

    // ============================================================
    // UPGRADES
    // ============================================================

    public abstract FocusUpgradeType[] getPossibleUpgradesByRank(ItemStack itemStack, int rank);

    public boolean canApplyUpgrade(ItemStack focusStack, Player player, FocusUpgradeType type, int rank) {
        return true;
    }

    // ============================================================
    // VISUAL
    // ============================================================

    public int getFocusColor(ItemStack itemStack) {
        return 0xFFFFFF;
    }

    // ============================================================
    // ANIMATION
    // ============================================================

    public enum WandFocusAnimation {
        CHARGE,
        WAVE,
        NONE
    }

    // ============================================================
    // TOOLTIP
    // ============================================================

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        FocusPackage pkg = getPackage(stack);
        if (pkg != null) {
            tooltip.add(Component.translatable("item.thaumcraft.focus.cost", String.format("%.1f", getVisCostValue(stack)))
                    .withStyle(ChatFormatting.GRAY));

            for (FocusUpgradeType type : pkg.getUpgrades()) {
                int level = pkg.getUpgradeLevel(type);
                if (level > 0) {
                    tooltip.add(Component.translatable("focus.upgrade." + type.getName().split("\\.")[3], level)
                            .withStyle(ChatFormatting.DARK_GREEN));
                }
            }
        }
    }
}
