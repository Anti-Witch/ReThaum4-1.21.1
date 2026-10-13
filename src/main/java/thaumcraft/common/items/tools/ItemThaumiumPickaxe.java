package thaumcraft.common.items.tools;

import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.init.ModItems;

/**
 * Thaumium Pickaxe - Magic-infused iron pickaxe with better stats.
 */
public class ItemThaumiumPickaxe extends PickaxeItem {

    public ItemThaumiumPickaxe() {
        super(ThaumcraftMaterials.TOOLMAT_THAUMIUM, new Item.Properties()
                .attributes(DiggerItem.createAttributes(ThaumcraftMaterials.TOOLMAT_THAUMIUM, 1, -2.8F)));
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.THAUMIUM_INGOT.get()) || super.isValidRepairItem(toRepair, repair);
    }
}
