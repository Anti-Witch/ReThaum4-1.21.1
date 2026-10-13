package thaumcraft.common.items.tools;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.init.ModItems;

/**
 * Thaumium Sword - Magic-infused iron sword with better stats.
 */
public class ItemThaumiumSword extends SwordItem {

    public ItemThaumiumSword() {
        super(ThaumcraftMaterials.TOOLMAT_THAUMIUM, new Item.Properties()
                .attributes(SwordItem.createAttributes(ThaumcraftMaterials.TOOLMAT_THAUMIUM, 3, -2.4F)));
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.THAUMIUM_INGOT.get()) || super.isValidRepairItem(toRepair, repair);
    }
}
