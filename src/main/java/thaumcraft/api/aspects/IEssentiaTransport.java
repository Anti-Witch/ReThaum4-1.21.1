package thaumcraft.api.aspects;

import net.minecraft.core.Direction;

/**
 * @author Azanor
 * 
 * This interface is used by tiles that use or transport essentia.
 * Only tiles that implement this interface will be able to connect to 
 * essentia tubes or other thaumic devices.
 */
public interface IEssentiaTransport {

    /**
     * Is this tile able to connect to other essentia users/sources on the specified side?
     * @param direction the direction to check
     * @return true if connectable on that side
     */
    boolean isConnectable(Direction direction);

    /**
     * Is this side used to input essentia?
     * @param direction the direction to check
     * @return true if this side can receive essentia
     */
    boolean canInputFrom(Direction direction);

    /**
     * Is this side used to output essentia?
     * @param direction the direction to check
     * @return true if this side can output essentia
     */
    boolean canOutputTo(Direction direction);

    /**
     * Sets the amount of suction this block will apply.
     * Suction is how essentia transport determines flow direction.
     * @param aspect the aspect type to apply suction for (null for any)
     * @param amount the suction strength
     */
    void setSuction(Aspect aspect, int amount);

    /**
     * Returns the type of suction this block is applying.
     * @param direction the direction from where the suction is being checked
     * @return the aspect type, or null if the suction is untyped (will draw first available)
     */
    Aspect getSuctionType(Direction direction);

    /**
     * Returns the strength of suction this block is applying.
     * @param direction the direction from where the suction is being checked
     * @return the suction amount
     */
    int getSuctionAmount(Direction direction);

    /**
     * Remove the specified amount of essentia from this transport tile.
     * @param aspect the aspect type to take
     * @param amount the amount to take
     * @param direction the direction from which essentia is being taken
     * @return how much was actually taken
     */
    int takeEssentia(Aspect aspect, int amount, Direction direction);

    /**
     * Add the specified amount of essentia to this transport tile.
     * @param aspect the aspect type to add
     * @param amount the amount to add
     * @param direction the direction from which essentia is being added
     * @return how much was actually added
     */
    int addEssentia(Aspect aspect, int amount, Direction direction);

    /**
     * What type of essentia this contains.
     * @param direction the direction to check
     * @return the aspect type contained, or null if empty
     */
    Aspect getEssentiaType(Direction direction);

    /**
     * How much essentia this block contains.
     * @param direction the direction to check
     * @return the amount of essentia
     */
    int getEssentiaAmount(Direction direction);

    /**
     * Essentia will not be drawn from this container unless the suction exceeds this amount.
     * @return the minimum suction required
     */
    int getMinimumSuction();

    /**
     * Whether this transport tile is "blocked" (e.g., has a jar brace preventing suction).
     * @return true if blocked
     */
    default boolean isBlocked() {
        return false;
    }
}




