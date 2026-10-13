package thaumcraft.common.items.tools;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.init.ModItems;

import java.util.List;

/**
 * Pickaxe of the Core - Fire elemental pickaxe.
 * Sets entities on fire when hit.
 * Has built-in Refining and Sounding infusion enchantments.
 */
public class ItemElementalPickaxe extends PickaxeItem {

    public ItemElementalPickaxe() {
        super(ThaumcraftMaterials.TOOLMAT_ELEMENTAL, new Item.Properties()
                .attributes(DiggerItem.createAttributes(ThaumcraftMaterials.TOOLMAT_ELEMENTAL, 1, -2.8f))
                .rarity(Rarity.RARE));
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.THAUMIUM_INGOT.get()) || super.isValidRepairItem(toRepair, repair);
    }

    /**
     * Sets entities on fire when hit with the pickaxe.
     */
    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (!player.level().isClientSide()) {
            // Check if PvP is enabled for player targets
            if (!(entity instanceof Player) || isPvPEnabled(player)) {
                entity.igniteForSeconds(2.0f);
            }
        }
        return super.onLeftClickEntity(stack, player, entity);
    }

    /**
     * Check if PvP is enabled on the server.
     */
    private boolean isPvPEnabled(Player player) {
        if (player.level().getServer() != null) {
            return player.level().getServer().isPvpAllowed();
        }
        return true; // Default to true if we can't check
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("enchantment.thaumcraft.refining").withStyle(style -> style.withColor(0xFFD700)));
        tooltip.add(Component.translatable("enchantment.thaumcraft.sounding").withStyle(style -> style.withColor(0xFFD700)));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
