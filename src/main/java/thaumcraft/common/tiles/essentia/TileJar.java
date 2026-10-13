package thaumcraft.common.tiles.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectSource;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.api.blocks.ILabelable;
import thaumcraft.common.items.consumables.ItemLabel;
import thaumcraft.common.tiles.TileThaumcraft;
import thaumcraft.init.ModBlockEntities;
import thaumcraft.init.ModSounds;

/**
 * Tile entity for warded jars - stores essentia.
 * Implements IAspectSource for container access and IEssentiaTransport for tube connections.
 */
public class TileJar extends TileThaumcraft implements IAspectSource, IEssentiaTransport, ILabelable {

    public static final int CAPACITY = 250;

    protected Aspect aspect = null;
    protected Aspect aspectFilter = null;
    protected int amount = 0;
    protected int facing = 2; // Direction facing for label
    protected boolean blocked = false; // If jar brace is applied

    private int tickCount = 0;

    public TileJar(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public TileJar(BlockPos pos, BlockState state) {
        this(ModBlockEntities.JAR.get(), pos, state);
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
        tag.putBoolean("Blocked", blocked);
    }

    @Override
    protected void readSyncNBT(CompoundTag tag) {
        super.readSyncNBT(tag);
        aspect = Aspect.getAspect(tag.getString("Aspect"));
        aspectFilter = Aspect.getAspect(tag.getString("AspectFilter"));
        amount = tag.getShort("Amount");
        facing = tag.getByte("Facing");
        blocked = tag.getBoolean("Blocked");
    }

    // ==================== Tick ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileJar tile) {
        if (++tile.tickCount % 5 == 0 && tile.amount < CAPACITY) {
            tile.fillFromAbove();
        }
    }

    /**
     * Try to pull essentia from connected tube above.
     */
    protected void fillFromAbove() {
        if (level == null || level.isClientSide) return;

        var te = level.getBlockEntity(worldPosition.above());
        if (te instanceof IEssentiaTransport transport) {
            if (!transport.canOutputTo(Direction.DOWN)) return;

            Aspect toGet = null;
            if (aspectFilter != null) {
                toGet = aspectFilter;
            } else if (aspect != null && amount > 0) {
                toGet = aspect;
            } else if (transport.getEssentiaAmount(Direction.DOWN) > 0 &&
                       transport.getSuctionAmount(Direction.DOWN) < getSuctionAmount(Direction.UP) &&
                       getSuctionAmount(Direction.UP) >= transport.getMinimumSuction()) {
                toGet = transport.getEssentiaType(Direction.DOWN);
            }

            if (toGet != null && transport.getSuctionAmount(Direction.DOWN) < getSuctionAmount(Direction.UP)) {
                int taken = transport.takeEssentia(toGet, 1, Direction.DOWN);
                if (taken > 0) {
                    addToContainer(toGet, taken);
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
        return face == Direction.UP;
    }

    @Override
    public boolean canInputFrom(Direction face) {
        return face == Direction.UP;
    }

    @Override
    public boolean canOutputTo(Direction face) {
        return face == Direction.UP;
    }

    @Override
    public void setSuction(Aspect aspect, int amount) {
        // Jars don't actively change suction
    }

    @Override
    public int getMinimumSuction() {
        return (aspectFilter != null) ? 64 : 32;
    }

    @Override
    public Aspect getSuctionType(Direction face) {
        return (aspectFilter != null) ? aspectFilter : aspect;
    }

    @Override
    public int getSuctionAmount(Direction face) {
        if (amount >= CAPACITY) return 0;
        return (aspectFilter != null) ? 64 : 32;
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
        return blocked;
    }

    // ==================== Labels (TC4 BlockJar.onBlockActivated) ====================

    /**
     * Apply a jar label. Mirrors TC4: a label with an aspect locks the jar to that
     * aspect; a blank label locks a non-empty jar to the essentia it already holds.
     * The label always ends up on the side of the jar facing the player.
     */
    @Override
    public boolean applyLabel(Player player, BlockPos pos, Direction side, ItemStack labelStack) {
        // Sneaking is used to remove filters, not to apply them
        if (player.isShiftKeyDown() || aspectFilter != null) {
            return false;
        }

        Aspect labelAspect = (labelStack.getItem() instanceof ItemLabel label)
                ? label.getAspect(labelStack) : null;

        // An empty jar can only be locked to the aspect written on the label
        if (amount <= 0) {
            if (labelAspect == null) {
                return false;
            }
            aspect = labelAspect;
        }

        // TC4 calls onBlockPlacedBy() here, which makes the label face the player
        facing = player.getDirection().getOpposite().ordinal();
        aspectFilter = aspect;
        markDirtyAndSync();
        playJarSound(level, worldPosition, 0.4F);
        return true;
    }

    /**
     * Remove the aspect filter without touching the jar contents.
     * The caller is responsible for dropping the label item.
     */
    public void removeAspectFilter() {
        if (aspectFilter != null) {
            aspectFilter = null;
            markDirtyAndSync();
        }
    }

    /**
     * TC4 shift + right-click with an empty hand: empty the jar. The aspect is only
     * cleared when no label is keeping the jar locked to a type.
     */
    public void clearContents() {
        amount = 0;
        if (aspectFilter == null) {
            aspect = null;
        }
        markDirtyAndSync();
    }

    /**
     * Play the TC4 jar sound. Called server side; the sound is broadcast to clients.
     */
    public static void playJarSound(Level level, BlockPos pos, float volume) {
        if (level != null && !level.isClientSide) {
            level.playSound(null, pos, ModSounds.JAR.get(), SoundSource.BLOCKS, volume, 1.0F);
        }
    }

    // ==================== Getters/Setters ====================

    public Aspect getAspect() {
        return aspect;
    }

    public int getAmount() {
        return amount;
    }

    public Aspect getAspectFilter() {
        return aspectFilter;
    }

    public void setAspectFilter(Aspect filter) {
        this.aspectFilter = filter;
        markDirtyAndSync();
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
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




