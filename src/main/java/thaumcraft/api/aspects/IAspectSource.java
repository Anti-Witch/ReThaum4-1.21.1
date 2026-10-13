package thaumcraft.api.aspects;

/**
 * @author Azanor
 *
 * This interface is implemented by tile entities (or possibly anything else) like jars
 * so that they can act as an essentia source for blocks like the infusion altar.
 */
public interface IAspectSource extends IAspectContainer {

    /**
     * If this returns true then it will not act as an aspect source.
     * Used to temporarily disable containers from being drained.
     * @return true if this source is blocked/disabled
     */
    boolean isBlocked();
}




