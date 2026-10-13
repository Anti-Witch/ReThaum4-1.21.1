package thaumcraft.api.casters;

/**
 * Base class for focus mediums - delivery methods for spell effects.
 * Mediums determine how and where effects are applied (projectile, touch, cloud, etc.).
 */
public abstract class FocusMedium extends FocusNode {
    
    @Override
    public EnumUnitType getType() {
        return EnumUnitType.MEDIUM;
    }

    @Override
    public EnumSupplyType[] mustBeSupplied() {
        // Root mediums (like Touch) don't need trajectory input
        return this instanceof FocusMediumRoot ? null : new EnumSupplyType[] { EnumSupplyType.TRAJECTORY };
    }
    
    @Override
    public EnumSupplyType[] willSupply() {
        return new EnumSupplyType[] { EnumSupplyType.TARGET };
    }
    
    /**
     * Whether this medium has an intermediary step (like a projectile in flight).
     */
    public boolean hasIntermediary() {
        return false;
    }

    /**
     * Execute the medium's delivery mechanism.
     * @param trajectory The trajectory to follow
     * @return true if execution should continue
     */
    public boolean execute(Trajectory trajectory) {
        return true;
    }
}




