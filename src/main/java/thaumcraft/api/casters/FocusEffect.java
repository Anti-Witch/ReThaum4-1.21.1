package thaumcraft.api.casters;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nullable;

/**
 * Base class for focus effects - the final result of a spell cast.
 * Effects are applied at targets supplied by their parent medium.
 */
public abstract class FocusEffect extends FocusNode {
    
    @Override
    public EnumUnitType getType() {
        return EnumUnitType.EFFECT;
    }

    @Override
    public EnumSupplyType[] mustBeSupplied() {
        return new EnumSupplyType[] { EnumSupplyType.TARGET };
    }

    @Override
    public EnumSupplyType[] willSupply() {
        return null;
    }

    /**
     * Execute the effect at the given target.
     * 
     * @param target The hit result from the delivery medium
     * @param trajectory The trajectory used to reach the target (may be null)
     * @param finalPower The final power level after all modifiers
     * @param num The index of this target (for multi-target effects)
     * @return true if the effect was successfully applied
     */
    public abstract boolean execute(HitResult target, @Nullable Trajectory trajectory, float finalPower, int num);
    
    /**
     * Get the damage value to display in tooltips.
     * @param finalPower The power level
     * @return Damage amount for display, or 0 if not applicable
     */
    public float getDamageForDisplay(float finalPower) {
        return 0;
    }
    
    /**
     * Render particle effects for this focus effect.
     * Called on the client side.
     */
    public abstract void renderParticleFX(Level level, double posX, double posY, double posZ, 
                                          double motionX, double motionY, double motionZ);

    /**
     * Called when the spell is first cast, before effects are applied.
     */
    public void onCast(Entity caster) {
        // Override for custom cast behavior
    }
}




