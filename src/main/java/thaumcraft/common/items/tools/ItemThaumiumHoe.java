package thaumcraft.common.items.tools;

import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.init.ModItems;

/**
 * Thaumium Hoe - Magic-infused iron hoe with better stats.
 */
public class ItemThaumiumHoe extends HoeItem {

    public ItemThaumiumHoe() {
        super(ThaumcraftMaterials.TOOLMAT_THAUMIUM, new Item.Properties()
                .attributes(DiggerItem.createAttributes(ThaumcraftMaterials.TOOLMAT_THAUMIUM, -2, -1.0F)));
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.THAUMIUM_INGOT.get()) || super.isValidRepairItem(toRepair, repair);
    }
}
