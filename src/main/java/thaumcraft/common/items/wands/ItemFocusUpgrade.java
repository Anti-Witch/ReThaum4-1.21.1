package thaumcraft.common.items.wands;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import thaumcraft.api.wands.FocusUpgradeType;

/**
 * Focus Upgrade - An item that upgrades a wand focus with a specific upgrade type.
 * The upgrade type is provided at construction time (see ModItems registration).
 *
 * NOTE: This class has no direct counterpart in the reference 1.20.1 project
 * (which uses a FocusEngine-based system). It is a minimal placeholder created
 * to satisfy the Thaumcraft 4-style wand registration in ModItems. It only stores
 * the upgrade type and holds the item properties; no upgrade logic is implemented.
 */
public class ItemFocusUpgrade extends Item {

    private final FocusUpgradeType type;

    public ItemFocusUpgrade(FocusUpgradeType type) {
        super(new Item.Properties()
                .stacksTo(64)
                .rarity(Rarity.UNCOMMON));
        this.type = type;
    }

    /**
     * Get the upgrade type carried by this item.
     */
    public FocusUpgradeType getType() {
        return type;
    }

    // A focus upgrade is never enchantable or repairable.
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }

    @Override
    public boolean isRepairable(ItemStack stack) {
        return false;
    }
}
