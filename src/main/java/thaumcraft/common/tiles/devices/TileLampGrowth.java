package thaumcraft.common.tiles.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.tiles.TileThaumcraft;
import thaumcraft.init.ModBlockEntities;

import java.util.ArrayList;
import java.util.List;

/**
 * Growth lamp tile entity - accelerates plant growth in range.
 * Consumes Herba (plant) essentia to operate.
 */
public class TileLampGrowth extends TileThaumcraft implements IEssentiaTransport {

    private static final int RANGE = 6;
    private static final int MAX_CHARGES = 20;

    private boolean reserve = false;
    public int charges = -1;
    
    // Last grown position for visual feedback
    private int lastX, lastY, lastZ;
    private Block lastBlock;
    
    // Checklist for scanning plants
    private List<BlockPos> checklist = new ArrayList<>();
    private int drawDelay = 0;

    public TileLampGrowth(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public TileLampGrowth(BlockPos pos, BlockState state) {
        this(ModBlockEntities.LAMP_GROWTH.get(), pos, state);
    }

    // ==================== NBT ====================

    @Override
    protected void writeSyncNBT(CompoundTag tag) {
        super.writeSyncNBT(tag);
        tag.putBoolean("Reserve", reserve);
        tag.putInt("Charges", charges);
    }

    @Override
    protected void readSyncNBT(CompoundTag tag) {
        super.readSyncNBT(tag);
        reserve = tag.getBoolean("Reserve");
        charges = tag.getInt("Charges");
    }

    // ==================== Tick ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileLampGrowth tile) {
        // Refill charges
        if (tile.charges <= 0) {
            if (tile.reserve) {
                tile.charges = MAX_CHARGES;
                tile.reserve = false;
                tile.setChanged();
                tile.syncTile(true);
            } else if (tile.drawEssentia()) {
                tile.charges = MAX_CHARGES;
                tile.setChanged();
                tile.syncTile(true);
            }
            
            // Update enabled state
            if (tile.charges <= 0) {
                tile.setEnabled(state, false);
            } else if (!tile.gettingPower()) {
                tile.setEnabled(state, true);
            }
        }

        // Try to fill reserve
        if (!tile.reserve && tile.drawEssentia()) {
            tile.reserve = true;
        }

        // Reset charges to -1 when empty (visual indicator)
        if (tile.charges == 0) {
            tile.charges = -1;
            tile.syncTile(true);
        }

        // Grow plants when charged and not powered
        if (!tile.gettingPower() && tile.charges > 0) {
            tile.updatePlant();
        }
    }

    /**
     * Try to grow a plant in range.
     */
    private void updatePlant() {
        if (level == null || !(level instanceof ServerLevel serverLevel)) return;

        // Build checklist if empty
        if (checklist.isEmpty()) {
            for (int x = -RANGE; x <= RANGE; x++) {
                for (int z = -RANGE; z <= RANGE; z++) {
                    checklist.add(worldPosition.offset(x, RANGE, z));
                }
            }
            // Shuffle using level random
            for (int i = checklist.size() - 1; i > 0; i--) {
                int j = level.random.nextInt(i + 1);
                BlockPos temp = checklist.get(i);
                checklist.set(i, checklist.get(j));
                checklist.set(j, temp);
            }
        }

        // Get next position to check
        if (checklist.isEmpty()) return;
        BlockPos startPos = checklist.remove(0);
        int x = startPos.getX();
        int z = startPos.getZ();

        // Scan downward for plants
        for (int y = startPos.getY(); y >= worldPosition.getY() - RANGE; y--) {
            BlockPos checkPos = new BlockPos(x, y, z);
            
            if (level.isEmptyBlock(checkPos)) continue;
            if (checkPos.distSqr(worldPosition) > RANGE * RANGE) continue;

            BlockState blockState = level.getBlockState(checkPos);
            Block block = blockState.getBlock();

            // Check if it's a growable plant
            if (block instanceof BonemealableBlock growable) {
                // 1.20.1 API: isValidBonemealTarget(LevelReader, BlockPos, BlockState, boolean isClient)
                if (growable.isValidBonemealTarget(level, checkPos, blockState)) {
                    if (growable.isBonemealSuccess(level, level.random, checkPos, blockState)) {
                        growable.performBonemeal(serverLevel, level.random, checkPos, blockState);
                        charges--;
                        lastX = x;
                        lastY = y;
                        lastZ = z;
                        lastBlock = block;
                        return;
                    }
                }
            }
        }
    }

    /**
     * Try to draw essentia from connected transport.
     */
    private boolean drawEssentia() {
        if (++drawDelay % 5 != 0) return false;
        if (level == null) return false;

        Direction facing = getFacing();
        BlockEntity te = ThaumcraftApiHelper.getConnectableTile(level, worldPosition, facing);
        
        if (te instanceof IEssentiaTransport transport) {
            Direction opposite = facing.getOpposite();
            
            if (!transport.canOutputTo(opposite)) return false;
            
            if (transport.getSuctionAmount(opposite) < getSuctionAmount(facing) &&
                transport.takeEssentia(Aspect.PLANT, 1, opposite) == 1) {
                return true;
            }
        }
        
        return false;
    }

    private Direction getFacing() {
        BlockState state = getBlockState();
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.getValue(BlockStateProperties.FACING);
        }
        return Direction.DOWN;
    }

    private void setEnabled(BlockState state, boolean enabled) {
        if (level == null) return;
        if (state.hasProperty(BlockStateProperties.ENABLED)) {
            boolean current = state.getValue(BlockStateProperties.ENABLED);
            if (current != enabled) {
                level.setBlock(worldPosition, state.setValue(BlockStateProperties.ENABLED, enabled), 3);
            }
        }
    }

    protected boolean gettingPower() {
        return level != null && level.hasNeighborSignal(worldPosition);
    }

    // ==================== IEssentiaTransport ====================

    @Override
    public boolean isConnectable(Direction face) {
        return face == getFacing();
    }

    @Override
    public boolean canInputFrom(Direction face) {
        return face == getFacing();
    }

    @Override
    public boolean canOutputTo(Direction face) {
        return false;
    }

    @Override
    public void setSuction(Aspect aspect, int amount) {
        // Not used
    }

    @Override
    public Aspect getSuctionType(Direction face) {
        return Aspect.PLANT;
    }

    @Override
    public int getSuctionAmount(Direction face) {
        if (face == getFacing() && (!reserve || charges <= 0)) {
            return 128;
        }
        return 0;
    }

    @Override
    public Aspect getEssentiaType(Direction face) {
        return null;
    }

    @Override
    public int getEssentiaAmount(Direction face) {
        return 0;
    }

    @Override
    public int takeEssentia(Aspect aspect, int amount, Direction face) {
        return 0;
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, Direction face) {
        return 0;
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }
}




