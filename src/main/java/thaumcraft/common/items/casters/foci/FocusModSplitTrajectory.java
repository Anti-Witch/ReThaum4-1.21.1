package thaumcraft.common.items.casters.foci;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusModSplit;
import thaumcraft.api.casters.Trajectory;

/**
 * Split Trajectory Modifier - Allows a spell to branch into multiple paths.
 * Unlike Scatter which adds random deviation, this creates separate spell chains
 * that can each have different effects attached.
 * 
 * This is used in the focus crafting system to create branching spell trees.
 */
public class FocusModSplitTrajectory extends FocusModSplit {

    @Override
    public String getResearch() {
        return "FOCUSSPLIT";
    }

    @Override
    public String getKey() {
        return "thaumcraft.SPLITTRAJECTORY";
    }

    @Override
    public int getComplexity() {
        return 5;
    }

    @Override
    public Aspect getAspect() {
        return Aspect.ENTROPY;
    }

    @Override
    public int getSplitCount() {
        // Default split into 2 trajectories
        return 2;
    }

    @Override
    public EnumSupplyType[] mustBeSupplied() {
        return new EnumSupplyType[] { EnumSupplyType.TRAJECTORY };
    }

    @Override
    public EnumSupplyType[] willSupply() {
        return new EnumSupplyType[] { EnumSupplyType.TRAJECTORY };
    }

    @Override
    public Trajectory[] supplyTrajectories() {
        if (getParent() == null) {
            return new Trajectory[0];
        }
        
        // Pass through parent trajectories - the split is handled by the focus system
        // which creates separate execution branches for each child node
        return getParent().supplyTrajectories();
    }

    @Override
    public boolean execute() {
        return true;
    }
}




