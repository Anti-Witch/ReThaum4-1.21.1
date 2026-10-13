package thaumcraft.api.casters;

import thaumcraft.api.aspects.Aspect;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for focus modifiers that split the spell into multiple targets.
 * Examples: Fork (multiple projectiles), Scatter (cone of targets)
 */
public abstract class FocusModSplit extends FocusMod {
    
    /** Child packages for each split path */
    private List<FocusPackage> splitPackages = new ArrayList<>();
    
    /**
     * Get the number of splits this modifier creates.
     */
    public abstract int getSplitCount();
    
    /**
     * Get the angle between split trajectories (in degrees).
     */
    public float getSplitAngle() {
        return 15.0f;
    }
    
    /**
     * Get the split packages (child focus packages for each trajectory).
     */
    public List<FocusPackage> getSplitPackages() {
        return splitPackages;
    }
    
    /**
     * Set the split packages.
     */
    public void setSplitPackages(List<FocusPackage> packages) {
        this.splitPackages = packages;
    }
    
    @Override
    public EnumSupplyType[] willSupply() {
        return new EnumSupplyType[] { EnumSupplyType.TRAJECTORY };
    }
    
    @Override
    public Aspect getAspect() {
        return Aspect.ENTROPY; // Split modifiers typically use entropy
    }
}




