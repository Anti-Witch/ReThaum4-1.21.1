package thaumcraft.common.items.baubles;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvents;

import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

/**
 * Amulet of Vis - A bauble that slowly recharges rechargeable items in the player's inventory.
 * Comes in two variants: found (slower) and crafted (faster).
 *
 * Now a Curios item that lives in the "amulet" slot.
 */
public class ItemAmuletVis extends Item implements ICurioItem {

    private final boolean isCrafted;

    public ItemAmuletVis(boolean crafted) {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(crafted ? Rarity.RARE : Rarity.UNCOMMON));
        this.isCrafted = crafted;
    }

    /**
     * Create the found variant (slower recharge).
     */
    public static ItemAmuletVis createFound() {
        return new ItemAmuletVis(false);
    }

    /**
     * Create the crafted variant (faster recharge).
     */
    public static ItemAmuletVis createCrafted() {
        return new ItemAmuletVis(true);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        // Only process for living entities (e.g. player) in regular inventories.
        if (entity instanceof LivingEntity living) {
            onCurioTick(stack, level, living);
        }
    }

    /**
     * Shared per-tick logic, used both when held in a normal inventory and when equipped in a Curios slot.
     */
    private void onCurioTick(ItemStack stack, Level level, LivingEntity entity) {
        // Only process server-side and for players
        if (level.isClientSide() || !(entity instanceof Player player)) {
            return;
        }

        // Recharge interval: crafted = every 5 ticks, found = every 40 ticks
        int interval = isCrafted ? 5 : 40;
        if (player.tickCount % interval != 0) {
            return;
        }

        // TODO: Scan inventory and recharge IRechargable items
        // This will integrate with RechargeHelper once fully implemented
        // For now, the framework is in place
    }

    // ==================== Curios integration ====================

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return slotContext.identifier().equals("amulet");
    }

    @Override
    public ICurio.SoundInfo getEquipSound(SlotContext slotContext, ItemStack stack) {
        return new ICurio.SoundInfo(SoundEvents.ARMOR_EQUIP_GENERIC.value(), 1.0F, 1.0F);
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity entity = slotContext.entity();
        onCurioTick(stack, entity.level(), entity);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.thaumcraft.amulet_vis.text")
                .withStyle(ChatFormatting.AQUA));

        if (isCrafted) {
            tooltip.add(Component.translatable("item.thaumcraft.amulet_vis.crafted")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
