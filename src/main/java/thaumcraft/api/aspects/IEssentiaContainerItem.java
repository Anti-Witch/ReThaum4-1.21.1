package thaumcraft.api.aspects;

import net.minecraft.world.item.ItemStack;

/**
 * @author Azanor
 *
 * Used by wispy essences and essentia phials to hold their aspects.
 * Useful for similar item containers that store their aspect information in NBT form so TC
 * automatically picks up the aspects they contain.
 */
public interface IEssentiaContainerItem {

    /**
     * Gets the aspects stored in this item stack.
     * @param itemstack the item stack to check
     * @return the AspectList stored, or null if none
     */
    AspectList getAspects(ItemStack itemstack);

    /**
     * Sets the aspects stored in this item stack.
     * @param itemstack the item stack to modify
     * @param aspects the aspects to store
     */
    void setAspects(ItemStack itemstack, AspectList aspects);

    /**
     * Return true if the contained aspect should not be used to calculate the actual item aspects.
     * For example: jar labels should return true since the label aspect is not part of the jar's value.
     * @return true if contained aspects should be ignored for aspect calculation
     */
    boolean ignoreContainedAspects();
}

/*
 * Example implementation for 1.20.1:
 *
 * @Override
 * public AspectList getAspects(ItemStack itemstack) {
 *     if (itemstack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) {
 *         AspectList aspects = new AspectList();
 *         aspects.readFromNBT(itemstack.getTag());
 *         return aspects.size() > 0 ? aspects : null;
 *     }
 *     return null;
 * }
 *
 * @Override
 * public void setAspects(ItemStack itemstack, AspectList aspects) {
 *     if (!itemstack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) {
 *         itemstack.setTag(new CompoundTag());
 *     }
 *     aspects.writeToNBT(itemstack.getTag());
 * }
 *
 * @Override
 * public boolean ignoreContainedAspects() {
 *     return false;
 * }
 */




