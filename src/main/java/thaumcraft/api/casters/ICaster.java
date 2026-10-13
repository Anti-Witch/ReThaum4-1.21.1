package thaumcraft.api.casters;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Interface for items that can cast spells using foci.
 * Implemented by the Caster Gauntlet and similar items.
 */
public interface ICaster {
    
    /**
     * Get the vis consumption modifier for this caster.
     * Takes into account player's vis discount gear.
     * 
     * @param stack The caster item stack
     * @param player The player using the caster
     * @param crafting Whether this is for crafting (arcane workbench) or casting
     * @return Multiplier for vis consumption (1.0 = normal, 0.5 = 50% cost)
     */
    float getConsumptionModifier(ItemStack stack, Player player, boolean crafting);
    
    /**
     * Attempt to consume vis from the local aura.
     * 
     * @param stack The caster item stack
     * @param player The player using the caster
     * @param amount Base amount of vis to consume
     * @param crafting Whether this is for crafting or casting
     * @param simulate If true, don't actually consume vis
     * @return True if the vis was (or could be) consumed
     */
    boolean consumeVis(ItemStack stack, Player player, float amount, boolean crafting, boolean simulate);
    
    /**
     * Get the focus item currently installed in this caster.
     * 
     * @param stack The caster item stack
     * @return The focus Item, or null if none installed
     */
    Item getFocus(ItemStack stack);
    
    /**
     * Get the focus ItemStack currently installed in this caster.
     * 
     * @param stack The caster item stack
     * @return The focus ItemStack, or null if none installed
     */
    ItemStack getFocusStack(ItemStack stack);
    
    /**
     * Install or remove a focus in this caster.
     * 
     * @param stack The caster item stack
     * @param focus The focus to install, or null/empty to remove
     */
    void setFocus(ItemStack stack, ItemStack focus);
    
    /**
     * Get a block picked by the Equal Trade focus effect.
     * 
     * @param stack The caster item stack
     * @return The picked block ItemStack, or empty if none
     */
    ItemStack getPickedBlock(ItemStack stack);
}




