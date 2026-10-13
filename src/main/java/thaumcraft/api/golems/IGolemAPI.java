package thaumcraft.api.golems;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Interface providing access to golem internals for addon developers.
 * Implemented by EntityThaumcraftGolem.
 */
public interface IGolemAPI {

    /**
     * @return The golem as a LivingEntity
     */
    LivingEntity getGolemEntity();

    /**
     * @return The golem's properties (material, parts, traits, rank)
     */
    IGolemProperties getProperties();

    /**
     * Sets the golem's properties
     * @param prop The new properties
     */
    void setProperties(IGolemProperties prop);

    /**
     * @return The world the golem is in
     */
    Level getGolemWorld();

    /**
     * Causes the golem to hold the itemstack supplied.
     * @param stack The stack to hold
     * @return Anything left over that the golem could not hold. 
     *         If the golem picked up the entire stack this will be an empty stack.
     */
    ItemStack holdItem(ItemStack stack);

    /**
     * Causes the golem to remove an itemstack it is holding. 
     * It does not actually drop the item in the world or place it anywhere - 
     * that is up to whatever is calling this method.
     * @param stack The itemstack that the golem will drop. 
     *              If null/empty is supplied the golem will drop whatever it is holding
     * @return The stack it 'dropped'
     */
    ItemStack dropItem(ItemStack stack);

    /**
     * Checks if the golem has carrying capacity for the given stack
     * @param stack The stack to check capacity for - can be empty
     * @param partial Does the golem only need to have room for part of the stack?
     * @return true if the golem can carry the stack
     */
    boolean canCarry(ItemStack stack, boolean partial);

    /**
     * Checks how much carrying capacity the golem has for the given stack
     * @param stack The stack to check capacity for - can be empty
     * @return The amount the golem can carry
     */
    int canCarryAmount(ItemStack stack);

    /**
     * Checks if the golem is carrying a specific item
     * @param stack The stack to check for
     * @return true if the golem is carrying the item
     */
    boolean isCarrying(ItemStack stack);

    /**
     * @return All items the golem is currently carrying
     */
    NonNullList<ItemStack> getCarrying();

    /**
     * Gives the golem XP towards increasing its rank rating. 
     * Default is usually 1 for completing a task.
     * @param xp The amount of XP to add
     */
    void addRankXp(int xp);

    /**
     * @return The golem's dye color (0-16, 0 = no color)
     */
    byte getGolemColor();

    /**
     * Plays arm swinging animation for attacks and such
     */
    void swingArm();

    /**
     * @return true if the golem is currently in combat mode
     */
    boolean isInCombat();
}




