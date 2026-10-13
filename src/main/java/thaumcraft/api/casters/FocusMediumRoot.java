package thaumcraft.api.casters;

/**
 * Marker interface for root mediums that don't require trajectory input.
 * Examples: Touch (uses player's reach), Self (targets the caster)
 */
public abstract class FocusMediumRoot extends FocusMedium {
    
    @Override
    public EnumSupplyType[] mustBeSupplied() {
        return null; // Root mediums generate their own trajectory
    }
    
    @Override
    public EnumSupplyType[] willSupply() {
        return new EnumSupplyType[] { EnumSupplyType.TARGET, EnumSupplyType.TRAJECTORY };
    }
}




