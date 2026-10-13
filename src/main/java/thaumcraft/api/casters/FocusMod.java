package thaumcraft.api.casters;

import thaumcraft.api.aspects.Aspect;

/**
 * Base class for focus modifiers - nodes that alter the behavior of effects.
 * Modifiers can change power, split targets, add riders, etc.
 */
public abstract class FocusMod extends FocusNode {

    @Override
    public EnumUnitType getType() {
        return EnumUnitType.MOD;
    }
    
    /**
     * Execute the modifier's effect on the spell chain.
     * @return true if the spell should continue processing
     */
    public abstract boolean execute();
    
    @Override
    public Aspect getAspect() {
        return null; // Most modifiers don't have a specific aspect
    }

    @Override
    public EnumSupplyType[] mustBeSupplied() {
        return null; // Modifiers are pass-through
    }

    @Override
    public EnumSupplyType[] willSupply() {
        return null; // Modifiers are pass-through
    }
}




