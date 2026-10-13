package thaumcraft.common.items.tools;

import net.minecraft.world.item.ItemStack;
import thaumcraft.api.items.IScribeTools;
import thaumcraft.common.items.ItemTC;

/**
 * Scribing Tools - used in the research table to write research notes.
 * Has durability that depletes with use.
 */
public class ItemScribingTools extends ItemTC implements IScribeTools {

    public ItemScribingTools() {
        super(new Properties()
                .stacksTo(1)
                .durability(100));
    }

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
