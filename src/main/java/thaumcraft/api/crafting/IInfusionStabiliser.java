package thaumcraft.api.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Blocks that implement this interface act as infusion crafting stabilisers like candles and skulls.
 * 
 * @author Azanor
 * 
 * @deprecated This interface will eventually be combined with IInfusionStabiliserExt.
 * Currently they are separate to preserve compatibility with addon mods.
 */
@Deprecated
public interface IInfusionStabiliser {
    
    /**
     * Returns true if the block can stabilise infusion at the given position.
     * 
     * @param level the world
     * @param pos the position of the stabiliser block
     * @return true if this block can provide stabilisation
     */
    boolean canStabaliseInfusion(Level level, BlockPos pos);
}




