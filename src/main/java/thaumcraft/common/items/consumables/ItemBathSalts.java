package thaumcraft.common.items.consumables;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Bath Salts - Crafting ingredient that has a short despawn time.
 * Used for creating purifying baths.
 */
public class ItemBathSalts extends Item {

    public ItemBathSalts() {
        super(new Item.Properties());
    }

    /**
     * Bath salts dissolve quickly in water - short entity lifespan.
     */
    @Override
    public int getEntityLifespan(ItemStack itemStack, Level level) {
        return 200; // 10 seconds
    }
}




