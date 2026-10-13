package thaumcraft.common.blocks.world.taint;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Interface for taint-related blocks.
 * Blocks implementing this interface are part of the taint system.
 */
public interface ITaintBlock {
    
    /**
     * Called when this taint block should die/be removed.
     * @param level the world
     * @param pos the block position
     * @param state the current block state
     */
    default void die(Level level, BlockPos pos, BlockState state) {
        level.removeBlock(pos, false);
    }
}




