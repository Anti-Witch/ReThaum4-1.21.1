package thaumcraft.common.tiles.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.tiles.devices.TileBellows;
import thaumcraft.init.ModBlockEntities;

/**
 * Buffer tube - stores up to 10 units of mixed essentia.
 * Can be used as a small essentia buffer/reservoir.
 * Bellows can increase suction strength.
 */
public class TileTubeBuffer extends TileTube implements IAspectContainer {

    public static final int MAX_AMOUNT = 10;

    public AspectList aspects = new AspectList();
    
    // Choke state per side: 0=normal, 1=weak suction, 2=no suction
    public byte[] chokedSides = { 0, 0, 0, 0, 0, 0 };
    
    private int tickCount = 0;
    private int bellows = -1;

    public TileTubeBuffer(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public TileTubeBuffer(BlockPos pos, BlockState state) {
        this(ModBlockEntities.TUBE_BUFFER.get(), pos, state);
    }

    // ==================== NBT ====================

    @Override
    protected void writeSyncNBT(CompoundTag tag) {
        // Don't call super - buffer has different sync data
        aspects.writeToNBT(tag);
        
        byte[] sides = new byte[6];
        for (int i = 0; i < 6; i++) {
            sides[i] = (byte) (openSides[i] ? 1 : 0);
        }
        tag.putByteArray("Open", sides);
        tag.putByteArray("Choke", chokedSides);
        tag.putInt("Side", facing.ordinal());
    }

    @Override
    protected void readSyncNBT(CompoundTag tag) {
        // Don't call super - buffer has different sync data
        aspects.readFromNBT(tag);
        
        byte[] sides = tag.getByteArray("Open");
        if (sides != null && sides.length == 6) {
            for (int i = 0; i < 6; i++) {
                openSides[i] = sides[i] == 1;
            }
        }
        
        byte[] choke = tag.getByteArray("Choke");
        if (choke != null && choke.length == 6) {
            chokedSides = choke;
        } else {
            chokedSides = new byte[] { 0, 0, 0, 0, 0, 0 };
        }
        
        facing = Direction.values()[tag.getInt("Side")];
    }

    // ==================== Tick ====================

    public static void serverTickBuffer(Level level, BlockPos pos, BlockState state, TileTubeBuffer tile) {
        tile.tickCount++;
        
        // Update bellows count periodically
        if (tile.bellows < 0 || tile.tickCount % 20 == 0) {
            tile.updateBellows();
        }

        // Fill buffer from neighbors
        if (tile.tickCount % 5 == 0 && tile.aspects.visSize() < MAX_AMOUNT) {
            tile.fillBuffer();
        }
    }

    private void updateBellows() {
        bellows = TileBellows.getBellows(level, worldPosition, Direction.values());
    }

    private void fillBuffer() {
        for (Direction dir : Direction.values()) {
            if (!canInputFrom(dir)) continue;

            BlockEntity te = ThaumcraftApiHelper.getConnectableTile(level, worldPosition, dir);
            if (te instanceof IEssentiaTransport transport) {
                Direction opposite = dir.getOpposite();
                
                if (transport.getEssentiaAmount(opposite) > 0 
                        && transport.getSuctionAmount(opposite) < getSuctionAmount(dir)
                        && getSuctionAmount(dir) >= transport.getMinimumSuction()) {
                    
                    Aspect toTake = transport.getEssentiaType(opposite);
                    int taken = transport.takeEssentia(toTake, 1, opposite);
                    if (taken > 0) {
                        addToContainer(toTake, taken);
                        return;
                    }
                }
            }
        }
    }

    // ==================== IAspectContainer ====================

    @Override
    public AspectList getAspects() {
        return aspects;
    }

    @Override
    public void setAspects(AspectList aspects) {
        // Not used
    }

    @Override
    public int addToContainer(Aspect tag, int amount) {
        if (amount != 1) return amount;
        
        if (aspects.visSize() < MAX_AMOUNT) {
            aspects.add(tag, amount);
            markDirtyAndSync();
            return 0;
        }
        return amount;
    }

    @Override
    public boolean takeFromContainer(Aspect tag, int amount) {
        if (aspects.getAmount(tag) >= amount) {
            aspects.remove(tag, amount);
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
    public boolean doesContainerContainAmount(Aspect tag, int amount) {
        return aspects.getAmount(tag) >= amount;
    }

    @Override
    public boolean doesContainerContain(AspectList list) {
        return false;
    }

    @Override
    public int containerContains(Aspect tag) {
        return aspects.getAmount(tag);
    }

    @Override
    public boolean doesContainerAccept(Aspect tag) {
        return true;
    }

    // ==================== IEssentiaTransport Override ====================

    @Override
    public void setSuction(Aspect aspect, int amount) {
        // Buffer doesn't propagate suction
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    @Override
    public Aspect getSuctionType(Direction face) {
        return null;
    }

    @Override
    public int getSuctionAmount(Direction face) {
        if (chokedSides[face.ordinal()] == 2) {
            return 0;
        }
        if (bellows <= 0 || chokedSides[face.ordinal()] == 1) {
            return 1;
        }
        return bellows * 32;
    }

    @Override
    public Aspect getEssentiaType(Direction face) {
        Aspect[] aspectArray = aspects.getAspects();
        if (aspectArray.length > 0 && level != null) {
            return aspectArray[level.random.nextInt(aspectArray.length)];
        }
        return null;
    }

    @Override
    public int getEssentiaAmount(Direction face) {
        return aspects.visSize();
    }

    @Override
    public int takeEssentia(Aspect aspect, int amount, Direction face) {
        if (!canOutputTo(face)) return 0;

        // Check if there's a higher suction elsewhere first
        BlockEntity te = ThaumcraftApiHelper.getConnectableTile(level, worldPosition, face);
        int requestingSuction = 0;
        if (te instanceof IEssentiaTransport transport) {
            requestingSuction = transport.getSuctionAmount(face.getOpposite());
        }

        // Check all other sides for higher suction
        for (Direction dir : Direction.values()) {
            if (dir == face || !canOutputTo(dir)) continue;

            BlockEntity otherTe = ThaumcraftApiHelper.getConnectableTile(level, worldPosition, dir);
            if (otherTe instanceof IEssentiaTransport transport) {
                Direction opposite = dir.getOpposite();
                int otherSuction = transport.getSuctionAmount(opposite);
                Aspect otherSuctionType = transport.getSuctionType(opposite);
                
                // If another side has higher suction for this aspect, don't output here
                if ((otherSuctionType == aspect || otherSuctionType == null) 
                        && requestingSuction < otherSuction 
                        && getSuctionAmount(dir) < otherSuction) {
                    return 0;
                }
            }
        }

        // Take the requested amount (capped by available)
        int available = aspects.getAmount(aspect);
        if (amount > available) amount = available;
        
        return takeFromContainer(aspect, amount) ? amount : 0;
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, Direction face) {
        return canInputFrom(face) ? (amount - addToContainer(aspect, amount)) : 0;
    }

    // ==================== Choke Management ====================

    /**
     * Cycle the choke state of a side.
     * 0 = normal, 1 = weak suction, 2 = no suction
     */
    public void cycleChoke(Direction side) {
        chokedSides[side.ordinal()]++;
        if (chokedSides[side.ordinal()] > 2) {
            chokedSides[side.ordinal()] = 0;
        }
        markDirtyAndSync();
    }

    public int getChokeState(Direction side) {
        return chokedSides[side.ordinal()];
    }
}




