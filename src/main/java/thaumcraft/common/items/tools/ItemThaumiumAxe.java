package thaumcraft.common.items.tools;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.init.ModItems;

/**
 * Thaumium Axe - Magic-infused iron axe with better stats.
 */
public class ItemThaumiumAxe extends AxeItem {

    public ItemThaumiumAxe() {
        super(ThaumcraftMaterials.TOOLMAT_THAUMIUM, new Item.Properties()
                .attributes(DiggerItem.createAttributes(ThaumcraftMaterials.TOOLMAT_THAUMIUM, 6.0F, -3.1F)));
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.THAUMIUM_INGOT.get()) || super.isValidRepairItem(toRepair, repair);
    }
}
