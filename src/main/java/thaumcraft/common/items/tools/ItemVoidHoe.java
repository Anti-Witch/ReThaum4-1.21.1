package thaumcraft.common.items.tools;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.api.items.IWarpingGear;
import thaumcraft.init.ModItems;

/**
 * Void Metal Hoe - Powerful but warping hoe that self-repairs.
 */
public class ItemVoidHoe extends HoeItem implements IWarpingGear {

    public ItemVoidHoe() {
        super(ThaumcraftMaterials.TOOLMAT_VOID, new Item.Properties()
                .attributes(DiggerItem.createAttributes(ThaumcraftMaterials.TOOLMAT_VOID, -3, -0.5F))
                .rarity(Rarity.RARE));
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.VOID_METAL_INGOT.get()) || super.isValidRepairItem(toRepair, repair);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        // Self-repair: repair 1 durability every second (20 ticks)
        if (stack.isDamaged() && entity != null && entity.tickCount % 20 == 0 && entity instanceof LivingEntity) {
            stack.setDamageValue(stack.getDamageValue() - 1);
        }
    }

    @Override
    public int getWarp(ItemStack itemstack, Player player) {
        return 1;
    }
}
