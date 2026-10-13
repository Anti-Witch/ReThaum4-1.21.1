package thaumcraft.common.tiles.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.init.ModBlockEntities;
import thaumcraft.init.ModBlocks;

/**
 * TileHole - Block entity for the portable hole block.
 * Stores the original block and manages the hole's lifetime.
 * When the countdown expires, the original block is restored.
 * 
 * @author Azanor
 * Ported to 1.20.1
 */
public class TileHole extends TileMemory {
    
    /** Current countdown timer */
    public short countdown = 0;
    
    /** Maximum countdown (hole lifetime in ticks) */
    public short countdownMax = 120;
    
    /** Depth count for creating hole tunnels */
    public byte depth = 0;
    
    /** Direction the hole is propagating */
    public Direction direction = null;
    
    public TileHole(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HOLE.get(), pos, state);
    }
    
    public TileHole(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    
    /**
     * Configure this hole with full parameters.
     */
    public void configure(BlockState originalBlock, short maxTicks, byte depth, Direction direction) {
        setOldBlock(originalBlock);
        this.countdownMax = maxTicks;
        this.depth = depth;
        this.direction = direction;
        this.countdown = 0;
    }
    
    /**
     * Server-side tick method.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, TileHole tile) {
        // First tick: propagate hole if we have depth remaining
        if (tile.countdown == 0 && tile.depth > 1 && tile.direction != null) {
            tile.propagateHole(level);
        }
        
        // Increment countdown
        tile.countdown++;
        
        // Mark dirty periodically for save
        if (tile.countdown % 20 == 0) {
            tile.setChanged();
        }
        
        // When countdown expires, restore the original block
        if (tile.countdown >= tile.countdownMax) {
            tile.restoreBlock(level);
        }
    }
    
    /**
     * Client-side tick for particles.
     */
    public static void clientTick(Level level, BlockPos pos, BlockState state, TileHole tile) {
        // TODO: Add sparkle particles around edges when FXDispatcher is implemented
        tile.spawnParticles(level);
    }
    
    /**
     * Propagate the hole in the direction, creating more holes.
     */
    private void propagateHole(Level level) {
        // Create surrounding holes (3x3 perpendicular to direction)
        Direction.Axis axis = direction.getAxis();
        
        for (int a = 0; a < 9; a++) {
            // Skip center (that's us)
            if (a / 3 == 1 && a % 3 == 1) continue;
            
            BlockPos offsetPos;
            switch (axis) {
                case Y -> offsetPos = worldPosition.offset(-1 + a / 3, 0, -1 + a % 3);
                case Z -> offsetPos = worldPosition.offset(-1 + a / 3, -1 + a % 3, 0);
                case X -> offsetPos = worldPosition.offset(0, -1 + a / 3, -1 + a % 3);
                default -> offsetPos = worldPosition;
            }
            
            createHoleAt(level, offsetPos, (byte) 1, countdownMax, null);
        }
        
        // Create next hole in the chain
        BlockPos nextPos = worldPosition.relative(direction.getOpposite());
        if (!createHoleAt(level, nextPos, (byte) (depth - 1), countdownMax, direction)) {
            // Failed to create next hole, stop chain
            depth = 0;
        }
    }
    
    /**
     * Attempt to create a hole at the given position.
     */
    private boolean createHoleAt(Level level, BlockPos pos, byte depth, short maxTicks, Direction dir) {
        BlockState targetState = level.getBlockState(pos);
        
        // Can't replace air or other holes
        if (targetState.isAir() || targetState.is(ModBlocks.HOLE.get())) {
            return false;
        }
        
        // Check hardness - can't go through bedrock-like blocks
        if (targetState.getDestroySpeed(level, pos) < 0) {
            return false;
        }
        
        // TODO: Check portable hole blacklist
        
        // Place the hole block
        level.setBlock(pos, ModBlocks.HOLE.get().defaultBlockState(), 3);
        
        // Configure the tile entity
        if (level.getBlockEntity(pos) instanceof TileHole holeTile) {
            holeTile.configure(targetState, maxTicks, depth, dir);
            return true;
        }
        
        return false;
    }
    
    /**
     * Restore the original block when the hole expires.
     */
    private void restoreBlock(Level level) {
        level.setBlock(worldPosition, oldBlock, 3);
        
        // Restore tile entity data if we had any
        if (tileEntityCompound != null && level.getBlockEntity(worldPosition) != null) {
            level.getBlockEntity(worldPosition).loadWithComponents(tileEntityCompound, level.registryAccess());
        }
    }
    
    /**
     * Spawn edge particles on client.
     */
    private void spawnParticles(Level level) {
        // TODO: Implement sparkle particles when FXDispatcher is available
        // Particles should appear at the edges where the hole meets solid blocks
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putShort("countdown", countdown);
        tag.putShort("countdownMax", countdownMax);
        tag.putByte("depth", depth);
        tag.putByte("direction", direction == null ? (byte) -1 : (byte) direction.ordinal());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        countdown = tag.getShort("countdown");
        countdownMax = tag.getShort("countdownMax");
        depth = tag.getByte("depth");
        byte dirByte = tag.getByte("direction");
        direction = dirByte >= 0 && dirByte < Direction.values().length ? Direction.values()[dirByte] : null;
    }
}




