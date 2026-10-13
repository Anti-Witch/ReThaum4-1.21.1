package thaumcraft.common.tiles.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectSource;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.tiles.TileThaumcraft;
import thaumcraft.init.ModBlockEntities;

/**
 * TileEssentiaReservoir - A large essentia storage tank.
 * 
 * Features:
 * - Stores 500 essentia (double a normal jar)
 * - Connects to tubes from any horizontal side
 * - Can be filtered with a label
 * - Multiple reservoirs can be stacked for multiblock storage
 * 
 * Ported from 1.12.2
 */
public class TileEssentiaReservoir extends TileThaumcraft implements IAspectSource, IEssentiaTransport {

    public static final int CAPACITY = 500;

    protected Aspect aspect = null;
    protected Aspect aspectFilter = null;
    protected int amount = 0;
    protected int facing = 2; // Label facing

    private int tickCount = 0;

    public TileEssentiaReservoir(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public TileEssentiaReservoir(BlockPos pos, BlockState state) {
        this(ModBlockEntities.ESSENTIA_RESERVOIR.get(), pos, state);
    }

    // ==================== NBT ====================

    @Override
    protected void writeSyncNBT(CompoundTag tag) {
        super.writeSyncNBT(tag);
        if (aspect != null) {
            tag.putString("Aspect", aspect.getTag());
        }
        if (aspectFilter != null) {
            tag.putString("AspectFilter", aspectFilter.getTag());
        }
        tag.putShort("Amount", (short) amount);
        tag.putByte("Facing", (byte) facing);
    }

    @Override
    protected void readSyncNBT(CompoundTag tag) {
        super.readSyncNBT(tag);
        aspect = Aspect.getAspect(tag.getString("Aspect"));
        aspectFilter = Aspect.getAspect(tag.getString("AspectFilter"));
        amount = tag.getShort("Amount");
        facing = tag.getByte("Facing");
    }

    // ==================== Tick ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEssentiaReservoir tile) {
        if (++tile.tickCount % 5 == 0 && tile.amount < CAPACITY) {
            tile.pullFromConnections();
        }

        // Balance with adjacent reservoirs
        if (tile.tickCount % 20 == 0) {
            tile.balanceWithNeighbors();
        }
    }

    /**
     * Try to pull essentia from connected tubes.
     */
    private void pullFromConnections() {
        if (level == null || level.isClientSide) return;

        for (Direction dir : Direction.values()) {
            if (amount >= CAPACITY) break;

            BlockEntity te = level.getBlockEntity(worldPosition.relative(dir));
            if (te instanceof IEssentiaTransport transport) {
                if (!transport.canOutputTo(dir.getOpposite())) continue;

                Aspect toGet = null;
                if (aspectFilter != null) {
                    toGet = aspectFilter;
                } else if (aspect != null && amount > 0) {
                    toGet = aspect;
                } else if (transport.getEssentiaAmount(dir.getOpposite()) > 0 &&
                           transport.getSuctionAmount(dir.getOpposite()) < getSuctionAmount(dir)) {
                    toGet = transport.getEssentiaType(dir.getOpposite());
                }

                if (toGet != null && transport.getSuctionAmount(dir.getOpposite()) < getSuctionAmount(dir)) {
                    int taken = transport.takeEssentia(toGet, 1, dir.getOpposite());
                    if (taken > 0) {
                        addToContainer(toGet, taken);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Balance essentia with adjacent reservoirs of the same type.
     */
    private void balanceWithNeighbors() {
        if (level == null || level.isClientSide || amount <= 0) return;

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockEntity te = level.getBlockEntity(worldPosition.relative(dir));
            if (te instanceof TileEssentiaReservoir neighbor) {
                // Only balance if same aspect (or neighbor is empty)
                if ((neighbor.aspect == null || neighbor.aspect == aspect) &&
                    neighbor.amount < amount - 1 &&
                    (neighbor.aspectFilter == null || neighbor.aspectFilter == aspect)) {
                    
                    // Transfer one unit to balance
                    if (takeFromContainer(aspect, 1)) {
                        neighbor.addToContainer(aspect, 1);
                    }
                    return; // Only balance once per tick
                }
            }
        }
    }

    // ==================== IAspectSource ====================

    @Override
    public AspectList getAspects() {
        AspectList list = new AspectList();
        if (aspect != null && amount > 0) {
            list.add(aspect, amount);
        }
        return list;
    }

    @Override
    public void setAspects(AspectList aspects) {
        if (aspects != null && aspects.size() > 0) {
            Aspect[] sorted = aspects.getAspectsSortedByAmount();
            if (sorted.length > 0) {
                aspect = sorted[0];
                amount = aspects.getAmount(sorted[0]);
            }
        }
    }

    @Override
    public int addToContainer(Aspect tag, int amt) {
        if (amt == 0) return amt;

        // Check filter
        if (aspectFilter != null && tag != aspectFilter) {
            return amt;
        }

        if ((amount < CAPACITY && tag == aspect) || amount == 0) {
            aspect = tag;
            int added = Math.min(amt, CAPACITY - amount);
            amount += added;
            amt -= added;
            markDirtyAndSync();
        }
        return amt;
    }

    @Override
    public boolean takeFromContainer(Aspect tag, int amt) {
        if (amount >= amt && tag == aspect) {
            amount -= amt;
            if (amount <= 0) {
                aspect = null;
                amount = 0;
            }
            markDirtyAndSync();
            return true;
        }
        return false;
    }

    @Override
    public boolean takeFromContainer(AspectList list) {
        return false;
    }

    @Override
    public boolean doesContainerContainAmount(Aspect tag, int amt) {
        return amount >= amt && tag == aspect;
    }

    @Override
    public boolean doesContainerContain(AspectList list) {
        for (Aspect a : list.getAspects()) {
            if (amount > 0 && a == aspect) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int containerContains(Aspect tag) {
        return (tag == aspect) ? amount : 0;
    }

    @Override
    public boolean doesContainerAccept(Aspect tag) {
        return aspectFilter == null || tag.equals(aspectFilter);
    }

    // ==================== IEssentiaTransport ====================

    @Override
    public boolean isConnectable(Direction face) {
        return true; // Connects from all sides
    }

    @Override
    public boolean canInputFrom(Direction face) {
        return true;
    }

    @Override
    public boolean canOutputTo(Direction face) {
        return true;
    }

    @Override
    public void setSuction(Aspect aspect, int amount) {
        // Fixed suction
    }

    @Override
    public int getMinimumSuction() {
        return (aspectFilter != null) ? 80 : 48;
    }

    @Override
    public Aspect getSuctionType(Direction face) {
        return (aspectFilter != null) ? aspectFilter : aspect;
    }

    @Override
    public int getSuctionAmount(Direction face) {
        if (amount >= CAPACITY) return 0;
        return (aspectFilter != null) ? 80 : 48;
    }

    @Override
    public Aspect getEssentiaType(Direction face) {
        return aspect;
    }

    @Override
    public int getEssentiaAmount(Direction face) {
        return amount;
    }

    @Override
    public int takeEssentia(Aspect aspect, int amount, Direction face) {
        if (canOutputTo(face) && takeFromContainer(aspect, amount)) {
            return amount;
        }
        return 0;
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, Direction face) {
        if (canInputFrom(face)) {
            return amount - addToContainer(aspect, amount);
        }
        return 0;
    }

    @Override
    public boolean isBlocked() {
        return false;
    }

    // ==================== Getters/Setters ====================

    public Aspect getAspect() {
        return aspect;
    }

    public int getAmount() {
        return amount;
    }

    public float getFillPercent() {
        return (float) amount / CAPACITY;
    }

    public Aspect getAspectFilter() {
        return aspectFilter;
    }

    public void setAspectFilter(Aspect filter) {
        this.aspectFilter = filter;
        markDirtyAndSync();
    }

    public int getFacing() {
        return facing;
    }

    public void setFacing(int facing) {
        this.facing = facing;
        markDirtyAndSync();
    }
}




