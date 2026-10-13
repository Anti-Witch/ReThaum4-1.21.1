package thaumcraft.common.tiles.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.init.ModBlockEntities;

/**
 * One-way essentia tube - only allows essentia to flow in one direction.
 * Suction and flow are calculated with directionality enabled.
 * 
 * Ported to 1.20.1
 */
public class TileTubeOneway extends TileTube {
    
    public TileTubeOneway(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    
    public TileTubeOneway(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TUBE_ONEWAY.get(), pos, state);
    }
    
    @Override
    protected void calculateSuction(Aspect filter, boolean restrict, boolean directional) {
        // Always use directional suction
        super.calculateSuction(filter, restrict, true);
    }
    
    @Override
    protected void equalizeWithNeighbours(boolean directional) {
        // Always use directional equalization
        super.equalizeWithNeighbours(true);
    }
}




