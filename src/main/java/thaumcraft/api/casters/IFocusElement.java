package thaumcraft.api.casters;

/**
 * Base interface for all focus elements (effects, mediums, modifiers).
 * Focus elements are combined to create spell effects.
 */
public interface IFocusElement {
    
    /**
     * Unique identifier for this focus element.
     */
    String getKey();
    
    /**
     * Research key required to unlock this element.
     */
    String getResearch();
    
    /**
     * The type of this focus element.
     */
    EnumUnitType getType();
    
    /**
     * Types of focus elements.
     */
    enum EnumUnitType {
        /** Final effect (fire, frost, etc.) */
        EFFECT,
        /** Delivery medium (projectile, touch, etc.) */
        MEDIUM,
        /** Modifier (power, duration, etc.) */
        MOD,
        /** Package container */
        PACKAGE
    }
}




