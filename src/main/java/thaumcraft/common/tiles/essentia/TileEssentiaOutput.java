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
 * Essentia Output transport block.
 * 
 * - Connects ONLY from the opposite of its facing direction (tube side)
 * - Has NO suction (0) - passively provides essentia to tubes
 * - Drains essentia from adjacent IAspectSource containers and outputs to connected tubes
 * - Acts as a source for essentia transport - doesn't input from tubes
 */
public class TileEssentiaOutput extends TileThaumcraft implements IEssentiaTransport {
    
    private int tickCounter = 0;
    
    public TileEssentiaOutput(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ESSENTIA_OUTPUT.get(), pos, state);
    }
    
    /**
     * Called by the block's ticker on server side.
     */
    public void serverTick() {
        if (level == null || level.isClientSide) return;
        
        if (++tickCounter % 5 == 0) {
            fillBuffer();
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
     * Drain essentia from nearby containers and push to connected tube.
     */
    private void fillBuffer() {
        Direction facing = getFacing();
        Direction outputSide = facing.getOpposite();
        
        BlockEntity te = ThaumcraftApiHelper.getConnectableTile(level, worldPosition, outputSide);
        if (te instanceof IEssentiaTransport ic) {
            // Check if the destination can accept input from us
            if (!ic.canInputFrom(facing)) {
                return;
            }
            
            // Check if destination has suction and wants a specific aspect
            int destSuction = ic.getSuctionAmount(facing);
            Aspect destWants = ic.getSuctionType(facing);
            
            if (destSuction > 0 && destWants != null) {
                // Try to drain the requested aspect from nearby sources
                if (EssentiaHandler.drainEssentiaWithConfirmation(this, destWants, facing, 16, false, 5)) {
                    // Successfully found essentia, try to add to destination
                    if (ic.addEssentia(destWants, 1, facing) > 0) {
                        // Confirm the drain was successful
                        EssentiaHandler.confirmDrain();
                    }
                }
            }
        }
    }
    
    // ==================== IEssentiaTransport Implementation ====================
    
    @Override
    public boolean isConnectable(Direction face) {
        // Only connect from the opposite side of facing (tube output side)
        return face == getFacing().getOpposite();
    }
    
    @Override
    public boolean canInputFrom(Direction face) {
        // Never inputs from tubes - this is a source
        return false;
    }
    
    @Override
    public boolean canOutputTo(Direction face) {
        // Output goes to tubes on the opposite side of facing
        return face == getFacing().getOpposite();
    }
    
    @Override
    public void setSuction(Aspect aspect, int amount) {
        // Output doesn't change suction dynamically
    }
    
    @Override
    public int getMinimumSuction() {
        return 0;
    }
    
    @Override
    public Aspect getSuctionType(Direction loc) {
        // No suction - this is a passive output
        return null;
    }
    
    @Override
    public int getSuctionAmount(Direction loc) {
        // No suction - this is a passive output
        return 0;
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
        // Cannot take essentia directly - it pulls from nearby sources
        return 0;
    }
    
    @Override
    public int addEssentia(Aspect aspect, int amount, Direction face) {
        // Cannot add essentia to this block
        return 0;
    }
}




