package thaumcraft.api.items;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * @author Azanor
 * 
 * ItemArmor or Baubles/Curios with this interface will grant a discount to the vis 
 * cost of actions the wearer performs with casting wands/gauntlets.
 * 
 * The amount returned is the percentage by which the cost is discounted. 
 * There is a built-in max discount of 50%, but individual items really shouldn't 
 * have a discount more than 5%.
 */
public interface IVisDiscountGear {
    
    /**
     * @param stack The equipped item stack
     * @param player The player wearing this gear
     * @return The percentage discount (0-50) this item provides
     */
    int getVisDiscount(ItemStack stack, Player player);
}




