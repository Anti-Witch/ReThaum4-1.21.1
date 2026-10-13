package thaumcraft.api.aspects;

/**
 * @author Azanor
 *
 * Used by blocks like the crucible and alembic to hold their aspects.
 * Tiles extending this interface will have their aspects show up when viewed by goggles of revealing
 */
public interface IAspectContainer {

    /**
     * Gets the aspects currently stored in this container.
     * @return the AspectList containing all aspects
     */
    AspectList getAspects();

    /**
     * Sets the aspects in this container.
     * @param aspects the AspectList to set
     */
    void setAspects(AspectList aspects);

    /**
     * This method is used to determine if a specific aspect can be added to this container.
     * @param aspect the aspect to check
     * @return true if the container accepts this aspect type
     */
    boolean doesContainerAccept(Aspect aspect);

    /**
     * This method is used to add a certain amount of an aspect to the container.
     * @param aspect the aspect to add
     * @param amount the amount to add
     * @return the amount of aspect left over that could not be added
     */
    int addToContainer(Aspect aspect, int amount);

    /**
     * Removes a certain amount of a specific aspect from the container.
     * @param aspect the aspect to remove
     * @param amount the amount to remove
     * @return true if that amount of aspect was available and was removed
     */
    boolean takeFromContainer(Aspect aspect, int amount);

    /**
     * Removes a bunch of different aspects and amounts from the container.
     * @param aspects the AspectList containing the aspects and their amounts
     * @return true if all the aspects and their amounts were available and successfully removed
     * @deprecated Use takeFromContainer(Aspect, int) instead
     */
    @Deprecated
    boolean takeFromContainer(AspectList aspects);

    /**
     * Checks if the container contains the listed amount (or more) of the aspect.
     * @param aspect the aspect to check
     * @param amount the minimum amount required
     * @return true if the container contains at least that amount
     */
    boolean doesContainerContainAmount(Aspect aspect, int amount);

    /**
     * Checks if the container contains all the listed aspects and their amounts.
     * @param aspects the AspectList containing the aspects and their amounts
     * @return true if all aspects are present in sufficient amounts
     * @deprecated Use doesContainerContainAmount(Aspect, int) instead
     */
    @Deprecated
    boolean doesContainerContain(AspectList aspects);

    /**
     * Returns how much of the aspect this container contains.
     * @param aspect the aspect to check
     * @return the amount of that aspect found
     */
    int containerContains(Aspect aspect);
}




