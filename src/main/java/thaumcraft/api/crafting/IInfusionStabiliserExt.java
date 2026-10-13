package thaumcraft.api.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Extended interface for infusion stabilisers with configurable stabilisation amounts.
 * 
 * Blocks implementing this interface can provide custom stability values and
 * have custom symmetry penalty logic.
 * 
 * @author Azanor
 */
public interface IInfusionStabiliserExt extends IInfusionStabiliser {
    
    /**
     * Returns how much this object stabilizes infusion.
     * As a baseline, both candles and skulls provide 0.1f.
     * 
     * The amount returned is for a symmetrical pair of the objects, 
     * not for each object in the pair.
     * The same amount will be subtracted if the pair isn't symmetrical.
     * 
     * @param level the world
     * @param pos the position of the stabiliser block
     * @return the stabilization amount (typically 0.1f for standard stabilisers)
     */
    float getStabilizationAmount(Level level, BlockPos pos);
    
    /**
     * Use this method to do an additional check for symmetry if the default checks are passed.
     * If true, the penalty will not be getStabilizationAmount, but whatever is returned by getSymmetryPenalty.
     * 
     * @param level the world
     * @param pos1 the first block
     * @param pos2 the second block as determined by symmetry
     * @return true if a custom symmetry penalty should be applied
     */
    default boolean hasSymmetryPenalty(Level level, BlockPos pos1, BlockPos pos2) {
        return false;
    }
    
    /**
     * Returns the custom symmetry penalty to apply when hasSymmetryPenalty returns true.
     * 
     * @param level the world
     * @param pos the position of the stabiliser block
     * @return the symmetry penalty amount (default 0)
     */
    default float getSymmetryPenalty(Level level, BlockPos pos) {
        return 0;
    }
}




