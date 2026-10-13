package thaumcraft.common.tiles.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.init.ModBlockEntities;

/**
 * Restricted essentia tube - limits essentia flow rate.
 * Suction is calculated with restriction enabled, reducing flow speed.
 * 
 * Ported to 1.20.1
 */
public class TileTubeRestrict extends TileTube {
    
    public TileTubeRestrict(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    
    public TileTubeRestrict(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TUBE_RESTRICT.get(), pos, state);
    }
    
    @Override
    protected void calculateSuction(Aspect filter, boolean restrict, boolean directional) {
        // Always use restricted suction
        super.calculateSuction(filter, true, directional);
    }
}




