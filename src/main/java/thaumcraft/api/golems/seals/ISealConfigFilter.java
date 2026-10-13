package thaumcraft.api.golems.seals;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

/**
 * Interface for seals that support item filtering.
 * Provides filter inventory and blacklist/whitelist functionality.
 */
public interface ISealConfigFilter {

    /**
     * @return The filter inventory
     */
    NonNullList<ItemStack> getInv();

    /**
     * @return Size limits for each filter slot
     */
    NonNullList<Integer> getSizes();

    /**
     * @return Number of filter slots
     */
    int getFilterSize();

    /**
     * Get the item in a filter slot
     */
    ItemStack getFilterSlot(int index);

    /**
     * Get the size limit for a filter slot
     */
    int getFilterSlotSize(int index);

    /**
     * Set the item in a filter slot
     */
    void setFilterSlot(int index, ItemStack stack);

    /**
     * Set the size limit for a filter slot
     */
    void setFilterSlotSize(int index, int size);

    /**
     * @return true if filter is a blacklist, false if whitelist
     */
    boolean isBlacklist();

    /**
     * Set blacklist/whitelist mode
     */
    void setBlacklist(boolean blacklist);

    /**
     * @return true if this filter supports stack size limiters
     */
    boolean hasStacksizeLimiters();
}




