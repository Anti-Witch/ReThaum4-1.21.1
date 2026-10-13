package thaumcraft.common.items.resources;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/**
 * Simple material item for crafting components.
 * Used for things like quicksilver, thaumium ingots, void metal, etc.
 */
public class ItemMaterial extends Item {

    public ItemMaterial(Properties properties) {
        super(properties);
    }

    public ItemMaterial() {
        super(new Properties());
    }

    /**
     * Create a basic material (stackable to 64).
     */
    public static ItemMaterial basic() {
        return new ItemMaterial();
    }

    /**
     * Create an uncommon material.
     */
    public static ItemMaterial uncommon() {
        return new ItemMaterial(new Properties().rarity(Rarity.UNCOMMON));
    }

    /**
     * Create a rare material.
     */
    public static ItemMaterial rare() {
        return new ItemMaterial(new Properties().rarity(Rarity.RARE));
    }

    /**
     * Create an epic material.
     */
    public static ItemMaterial epic() {
        return new ItemMaterial(new Properties().rarity(Rarity.EPIC));
    }
}




