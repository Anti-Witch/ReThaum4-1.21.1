package thaumcraft.common.tiles.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.lib.events.EssentiaHandler;
import thaumcraft.common.tiles.TileThaumcraft;
import thaumcraft.init.ModBlockEntities;

/**
 * Essentia Input transport block.
 * 
 * - Connects ONLY from the opposite of its facing direction (tube side)
 * - Has HIGH suction (128) to pull essentia from connected tubes
 * - Takes essentia from tubes and adds it to adjacent IAspectSource containers
 * - Acts as a terminus for essentia transport - doesn't output to tubes
 */
public class TileEssentiaInput extends TileThaumcraft implements IEssentiaTransport {
    
    private int tickCounter = 0;
    
    public TileEssentiaInput(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ESSENTIA_INPUT.get(), pos, state);
    }
    
    /**
     * Called by the block's ticker on server side.
     */
    public void serverTick() {
        if (level == null || level.isClientSide) return;
        
        if (++tickCounter % 5 == 0) {
            fillJar();
        }
    }
    
    /**
     * Get the facing direction from block state.
     */
    private Direction getFacing() {
        BlockState state = getBlockState();
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.getValue(BlockStateProperties.FACING);
        }
        return Direction.UP;
    }
    
    /**
     * Pull essentia from connected tube and add to nearby containers.
     */
    private void fillJar() {
        Direction facing = getFacing();
        Direction inputSide = facing.getOpposite();
        
        BlockEntity te = ThaumcraftApiHelper.getConnectableTile(level, worldPosition, inputSide);
        if (te instanceof IEssentiaTransport ic) {
            // Check if the source can output to us
            if (!ic.canOutputTo(facing)) {
                return;
            }
            
            // Check if source has essentia and we have higher suction
            if (ic.getEssentiaAmount(facing) > 0 
                    && ic.getSuctionAmount(facing) < getSuctionAmount(inputSide) 
                    && getSuctionAmount(inputSide) >= ic.getMinimumSuction()) {
                
                Aspect ta = ic.getEssentiaType(facing);
                if (ta != null) {
                    // Try to add essentia to nearby containers
                    if (EssentiaHandler.addEssentia(this, ta, facing, 16, false, 5)) {
                        // Successfully added, take from source
                        ic.takeEssentia(ta, 1, facing);
                    }
                }
            }
        }
    }
    
    // ==================== IEssentiaTransport Implementation ====================
    
    @Override
    public boolean isConnectable(Direction face) {
        // Only connect from the opposite side of facing (tube input side)
        return face == getFacing().getOpposite();
    }
    
    @Override
    public boolean canInputFrom(Direction face) {
        // Input comes from tubes on the opposite side of facing
        return face == getFacing().getOpposite();
    }
    
    @Override
    public boolean canOutputTo(Direction face) {
        // Never outputs to tubes - this is a terminus
        return false;
    }
    
    @Override
    public void setSuction(Aspect aspect, int amount) {
        // Input doesn't change suction dynamically
    }
    
    @Override
    public int getMinimumSuction() {
        return 0;
    }
    
    @Override
    public Aspect getSuctionType(Direction loc) {
        // Accept any aspect
        return null;
    }
    
    @Override
    public int getSuctionAmount(Direction loc) {
        // High suction to pull essentia from tubes
        return 128;
    }
    
    @Override
    public Aspect getEssentiaType(Direction loc) {
        // Has no internal essentia storage
        return null;
    }
    
    @Override
    public int getEssentiaAmount(Direction loc) {
        return 0;
    }
    
    @Override
    public int takeEssentia(Aspect aspect, int amount, Direction face) {
        // Cannot take essentia from this block
        return 0;
    }
    
    @Override
    public int addEssentia(Aspect aspect, int amount, Direction face) {
        // Accepts essentia and passes it to nearby containers
        // The actual work is done in fillJar()
        return amount;
    }
}




