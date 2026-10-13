package thaumcraft.common.tiles.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.common.tiles.TileThaumcraft;
import thaumcraft.init.ModBlockEntities;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * TileWaterJug - A device that converts vis into water and distributes it to nearby fluid handlers.
 * 
 * Functionality:
 * - Converts vis from the aura into water (stored in internal tank)
 * - Scans nearby area for fluid handlers (crucibles, other containers, cauldrons)
 * - Automatically fills nearby water-accepting blocks/tiles
 * - Can only be drained from the top (not filled externally)
 * 
 * Ported to 1.20.1
 */
public class TileWaterJug extends TileThaumcraft {
    
    public static final int TANK_CAPACITY = 1000;
    private static final int SCAN_RANGE = 2; // 5x3x5 area (x-2 to x+2, y-1 to y+1, z-2 to z+2)
    
    // Internal fluid storage
    public final FluidTank tank = new FluidTank(TANK_CAPACITY) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == Fluids.WATER;
        }
    };
    // Scanning state
    private int zone = 0;
    private int counter = 0;
    private List<Integer> handlers = new ArrayList<>();
    
    // Client-side visualization state
    private int visualZone = 0;
    private int visualCounter = 0;
    
    public TileWaterJug(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WATER_JUG.get(), pos, state);
    }
    
    // ==================== NBT ====================
    
    @Override
    protected void writeSyncNBT(CompoundTag tag) {
        super.writeSyncNBT(tag);
        tank.writeToNBT(getRegistryProvider(), tag);
    }
    
    @Override
    protected void readSyncNBT(CompoundTag tag) {
        super.readSyncNBT(tag);
        tank.readFromNBT(getRegistryProvider(), tag);
    }
    
    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        handlers.clear();
        if (tag.contains("handlers")) {
            int[] arr = tag.getIntArray("handlers");
            for (int h : arr) {
                handlers.add(h);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putIntArray("handlers", handlers.stream().mapToInt(i -> i).toArray());
    }
    
    // ==================== Tick ====================
    
    public static void serverTick(Level level, BlockPos pos, BlockState state, TileWaterJug tile) {
        tile.counter++;
        
        if (tile.counter % 5 != 0) {
            return;
        }
        
        // Scan zone for new fluid handlers
        tile.zone++;
        int scanX = (tile.zone / 5) % 5;
        int scanY = (tile.zone / 5 / 5) % 3;
        int scanZ = tile.zone % 5;
        
        BlockPos scanPos = pos.offset(scanX - SCAN_RANGE, scanY - 1, scanZ - SCAN_RANGE);
        
        if (tile.isValidFluidTarget(level, scanPos)) {
            if (!tile.handlers.contains(tile.zone)) {
                tile.handlers.add(tile.zone);
                tile.setChanged();
            }
        }
        
        // Try to fill registered handlers
        int handlerIndex = 0;
        while (handlerIndex < tile.handlers.size() && tile.tank.getFluidAmount() >= 25) {
            int handlerZone = tile.handlers.get(handlerIndex);
            int hx = (handlerZone / 5) % 5;
            int hy = (handlerZone / 5 / 5) % 3;
            int hz = handlerZone % 5;
            
            BlockPos handlerPos = pos.offset(hx - SCAN_RANGE, hy - 1, hz - SCAN_RANGE);
            
            if (!tile.tryFillTarget(level, handlerPos, handlerZone)) {
                // Handler no longer valid, remove it
                tile.handlers.remove(handlerIndex);
                tile.setChanged();
                continue;
            }
            handlerIndex++;
        }
        
        // Refill tank from aura vis
        if (tile.tank.getFluidAmount() < TANK_CAPACITY) {
            float visNeeded = (TANK_CAPACITY - tile.tank.getFluidAmount()) / 1000.0f;
            if (visNeeded > 0.1f) {
                visNeeded = 0.1f;
            }
            
            float visDrained = AuraHelper.drainVis(level, pos, visNeeded, false);
            int waterGenerated = (int)(1000.0f * visDrained);
            
            if (waterGenerated > 0) {
                tile.tank.fill(new FluidStack(Fluids.WATER, waterGenerated), IFluidHandler.FluidAction.EXECUTE);
                tile.setChanged();
                
                // Sync when tank becomes full
                if (tile.tank.getFluidAmount() >= TANK_CAPACITY) {
                    tile.syncTile(false);
                }
            }
        }
    }
    
    public static void clientTick(Level level, BlockPos pos, BlockState state, TileWaterJug tile) {
        tile.counter++;
        
        // Handle water trail visual effects
        if (tile.visualCounter > 0) {
            if (tile.visualCounter % 5 == 0) {
                int vx = (tile.visualZone / 5) % 5;
                int vy = (tile.visualZone / 5 / 5) % 3;
                int vz = tile.visualZone % 5;
                
                // TODO: FXDispatcher.INSTANCE.waterTrailFx(pos, pos.offset(vx - 2, vy - 1, vz - 2), tile.counter, 0x2870DA, 0.1f);
            }
            tile.visualCounter--;
        }
    }
    
    /**
     * Check if a position has a valid fluid handler that can accept water.
     */
    private boolean isValidFluidTarget(Level level, BlockPos targetPos) {
        BlockState targetState = level.getBlockState(targetPos);
        
        // Check for cauldron
        if (targetState.is(Blocks.CAULDRON) || targetState.is(Blocks.WATER_CAULDRON)) {
            return true;
        }
        
        // Check for fluid handler capability
        BlockEntity te = level.getBlockEntity(targetPos);
        if (te != null) {
            IFluidHandler cap = Capabilities.FluidHandler.BLOCK.getCapability(level, targetPos, targetState, te, Direction.UP);
            return cap != null;
        }
        
        return false;
    }
    
    /**
     * Try to fill a target with water.
     * @return true if the target is still valid (even if not filled this tick)
     */
    private boolean tryFillTarget(Level level, BlockPos targetPos, int zoneId) {
        BlockState targetState = level.getBlockState(targetPos);
        
        // Handle cauldron
        if (targetState.is(Blocks.CAULDRON)) {
            if (tank.getFluidAmount() >= 333) {
                level.setBlock(targetPos, Blocks.WATER_CAULDRON.defaultBlockState()
                        .setValue(LayeredCauldronBlock.LEVEL, 1), 2);
                tank.drain(333, IFluidHandler.FluidAction.EXECUTE);
                level.blockEvent(worldPosition, getBlockState().getBlock(), 1, zoneId);
                setChanged();
                syncTile(false);
            }
            return true;
        }
        
        if (targetState.is(Blocks.WATER_CAULDRON)) {
            int currentLevel = targetState.getValue(LayeredCauldronBlock.LEVEL);
            if (currentLevel < 3 && tank.getFluidAmount() >= 333) {
                level.setBlock(targetPos, targetState.setValue(LayeredCauldronBlock.LEVEL, currentLevel + 1), 2);
                tank.drain(333, IFluidHandler.FluidAction.EXECUTE);
                level.blockEvent(worldPosition, getBlockState().getBlock(), 1, zoneId);
                setChanged();
                syncTile(false);
            }
            return true;
        }
        
        // Handle fluid handler capability
        BlockEntity te = level.getBlockEntity(targetPos);
        if (te != null) {
            IFluidHandler cap = Capabilities.FluidHandler.BLOCK.getCapability(level, targetPos, targetState, te, Direction.UP);
            if (cap != null) {
                IFluidHandler handler = cap;
                if (handler != null) {
                    int filled = handler.fill(new FluidStack(Fluids.WATER, 25), IFluidHandler.FluidAction.EXECUTE);
                    if (filled > 0) {
                        tank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                        level.blockEvent(worldPosition, getBlockState().getBlock(), 1, zoneId);
                        setChanged();
                        syncTile(false);
                    }
                }
                return true;
            }
        }
        
        return false;
    }
    
    // ==================== Block Events (for client visuals) ====================
    
    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == 1) {
            if (level != null && level.isClientSide) {
                visualZone = param;
                visualCounter = 5;
            }
            return true;
        }
        return super.triggerEvent(id, param);
    }
    
    // ==================== Fluid Capability ====================
    
    /**
     * Custom fluid handler that only allows draining from top.
     */
    private class WaterJugFluidHandler implements IFluidHandler {
        
        @Override
        public int getTanks() {
            return 1;
        }
        
        @Nonnull
        @Override
        public FluidStack getFluidInTank(int tankIndex) {
            return tank.getFluid();
        }
        
        @Override
        public int getTankCapacity(int tankIndex) {
            return TANK_CAPACITY;
        }
        
        @Override
        public boolean isFluidValid(int tankIndex, @Nonnull FluidStack stack) {
            return false; // Cannot fill from outside
        }
        
        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0; // Cannot fill from outside
        }
        
        @Nonnull
        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (!resource.getFluid().isSame(Fluids.WATER)) {
                return FluidStack.EMPTY;
            }
            boolean wasFull = tank.getFluidAmount() >= TANK_CAPACITY;
            FluidStack drained = tank.drain(resource, action);
            
            if (action.execute()) {
                setChanged();
                if (wasFull && tank.getFluidAmount() < TANK_CAPACITY) {
                    syncTile(false);
                }
            }
            return drained;
        }
        
        @Nonnull
        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            boolean wasFull = tank.getFluidAmount() >= TANK_CAPACITY;
            FluidStack drained = tank.drain(maxDrain, action);
            
            if (action.execute()) {
                setChanged();
                if (wasFull && tank.getFluidAmount() < TANK_CAPACITY) {
                    syncTile(false);
                }
            }
            return drained;
        }
    }
    
// ==================== Accessors ====================
    
    public int getWaterLevel() {
        return tank.getFluidAmount();
    }
    
    public boolean isFull() {
        return tank.getFluidAmount() >= TANK_CAPACITY;
    }
}




