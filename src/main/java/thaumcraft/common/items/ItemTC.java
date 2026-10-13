package thaumcraft.common.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/**
 * Base class for simple Thaumcraft items.
 * Provides common properties and convenience methods.
 */
public class ItemTC extends Item {

    public ItemTC(Properties properties) {
        super(properties);
    }

    public ItemTC() {
        super(new Properties());
    }

    /**
     * Create properties for a basic stackable item.
     */
    public static Properties basicProps() {
        return new Properties();
    }

    /**
     * Create properties for an unstackable item.
     */
    public static Properties unstackable() {
        return new Properties().stacksTo(1);
    }

    /**
     * Create properties for a tool/weapon item.
     */
    public static Properties toolProps() {
        return new Properties().stacksTo(1);
    }

    /**
     * Create properties for an uncommon item.
     */
    public static Properties uncommonProps() {
        return new Properties().rarity(Rarity.UNCOMMON);
    }

    /**
     * Create properties for a rare item.
     */
    public static Properties rareProps() {
        return new Properties().rarity(Rarity.RARE);
    }

    /**
     * Create properties for an epic item.
     */
    public static Properties epicProps() {
        return new Properties().rarity(Rarity.EPIC);
    }
}




