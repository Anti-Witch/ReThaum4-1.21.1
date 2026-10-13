package thaumcraft.common.items.baubles;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * Curiosity Band - A headband that provides research bonuses.
 * When worn, the player gains bonus research points when scanning things.
 *
 * TODO: Integrate with research/scanning system for actual bonuses.
 */
public class ItemCuriosityBand extends Item implements ICurioItem {

    public ItemCuriosityBand() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.RARE));
    }

    /**
     * Check if a player is wearing the curiosity band.
     * Used by the scanning system to apply research bonuses.
     */
    public static boolean isWearingBand(net.minecraft.world.entity.player.Player player) {
        // Check inventory for the band
        // TODO: Check Curios head slot when integrated
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() instanceof ItemCuriosityBand) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the research bonus multiplier when wearing the band.
     */
    public static float getResearchBonus() {
        return 1.25f; // 25% bonus research points
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return slotContext.identifier().equals("head");
    }

    @Override
    public ICurio.SoundInfo getEquipSound(SlotContext slotContext, ItemStack stack) {
        return new ICurio.SoundInfo(SoundEvents.ARMOR_EQUIP_GENERIC.value(), 1.0F, 1.0F);
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
    }
}
