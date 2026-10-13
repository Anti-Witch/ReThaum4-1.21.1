package thaumcraft.api.research;

import net.minecraft.world.entity.player.Player;

/**
 * Interface for objects that can be scanned with the Thaumometer.
 * Implement this to add custom scanning behavior.
 */
public interface IScanThing {

    /**
     * Checks if the passed object matches what this scan thing is looking for.
     * The passed in obj can be an Entity, a BlockPos, an ItemStack, or null 
     * if nothing was actually clicked on.
     *
     * @param player the player doing the scanning
     * @param obj the object being scanned
     * @return true if this scan thing matches the object
     */
    boolean checkThing(Player player, Object obj);

    /**
     * Gets the research key that will be unlocked if the object is scanned.
     * This need not be an actual defined research item - any text string will do,
     * though note that some characters like '@' have special meanings within 
     * the research system.
     * 
     * It's common to use "!" as a prefix for fake research keys.
     * You can then use this research key (fake or otherwise) as a parent for 
     * research or for whatever purpose.
     *
     * @param player the player doing the scanning
     * @param object the object being scanned
     * @return the research key to unlock
     */
    String getResearchKey(Player player, Object object);

    /**
     * Called when the scan is successful.
     * Override this to perform additional actions on successful scan.
     *
     * @param player the player who scanned
     * @param object the object that was scanned
     */
    default void onSuccess(Player player, Object object) {
        // Default implementation does nothing
    }
}




