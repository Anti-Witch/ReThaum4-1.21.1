package thaumcraft.common.items.casters.foci;

import net.minecraft.world.phys.HitResult;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusModSplit;

/**
 * Split Target Modifier - Allows a spell to branch based on targets.
 * Unlike SplitTrajectory which branches trajectories, this branches based on
 * multiple target hits, allowing different effects to be applied to each target.
 * 
 * This is used in the focus crafting system to create branching spell trees.
 */
public class FocusModSplitTarget extends FocusModSplit {

    @Override
    public String getResearch() {
        return "FOCUSSPLIT";
    }

    @Override
    public String getKey() {
        return "thaumcraft.SPLITTARGET";
    }

    @Override
    public int getComplexity() {
        return 4;
    }

    @Override
    public Aspect getAspect() {
        return Aspect.ENTROPY;
    }

    @Override
    public int getSplitCount() {
        // Default split into 2 target branches
        return 2;
    }

    @Override
    public EnumSupplyType[] mustBeSupplied() {
        return new EnumSupplyType[] { EnumSupplyType.TARGET };
    }

    @Override
    public EnumSupplyType[] willSupply() {
        return new EnumSupplyType[] { EnumSupplyType.TARGET };
    }

    @Override
    public HitResult[] supplyTargets() {
        if (getParent() == null) {
            return new HitResult[0];
        }
        
        // Pass through parent targets - the split is handled by the focus system
        // which creates separate execution branches for each child node
        return getParent().supplyTargets();
    }

    @Override
    public boolean execute() {
        return true;
    }
}




