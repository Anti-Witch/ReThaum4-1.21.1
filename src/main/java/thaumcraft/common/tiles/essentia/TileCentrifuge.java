package thaumcraft.common.tiles.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.tiles.TileThaumcraft;
import thaumcraft.common.tiles.devices.TileBellows;
import thaumcraft.init.ModBlockEntities;

/**
 * TileCentrifuge - Separates compound aspects into their component primal aspects.
 * 
 * Takes essentia from tubes on any horizontal side and outputs the
 * resulting primal aspects upward. Can be sped up with bellows.
 * 
 * Example: Motus (motion) = Aer + Ordo -> outputs 1 Aer and 1 Ordo
 * 
 * Ported from 1.12.2
 */
public class TileCentrifuge extends TileThaumcraft implements IAspectContainer, IEssentiaTransport {

    public static final int MAX_AMOUNT = 10;

    // Current essentia being processed
    public Aspect aspect = null;
    public int amount = 0;

    // Processing state
    public int progress = 0;
    public int maxProgress = 80;

    // Animation (client-side)
    public float spin = 0;
    public float spinLast = 0;

    // Tick counter
    private int tickCount = 0;

    public TileCentrifuge(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public TileCentrifuge(BlockPos pos, BlockState state) {
        this(ModBlockEntities.CENTRIFUGE.get(), pos, state);
    }

    // ==================== NBT ====================

    @Override
    protected void writeSyncNBT(CompoundTag tag) {
        super.writeSyncNBT(tag);
        if (aspect != null) {
            tag.putString("Aspect", aspect.getTag());
        }
        tag.putShort("Amount", (short) amount);
        tag.putShort("Progress", (short) progress);
    }

    @Override
    protected void readSyncNBT(CompoundTag tag) {
        super.readSyncNBT(tag);
        aspect = Aspect.getAspect(tag.getString("Aspect"));
        amount = tag.getShort("Amount");
        progress = tag.getShort("Progress");
    }

    // ==================== Tick ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileCentrifuge tile) {
        tile.tickCount++;

        // Try to pull essentia every few ticks
        if (tile.tickCount % 5 == 0 && tile.amount < MAX_AMOUNT) {
            tile.pullEssentia();
        }

        // Process essentia if we have some
        if (tile.aspect != null && tile.amount > 0 && !tile.aspect.isPrimal()) {
            tile.maxProgress = Math.max(10, 80 - tile.getBellowsBonus() * 10);
            tile.progress++;

            if (tile.progress >= tile.maxProgress) {
                tile.progress = 0;
                tile.processEssentia();
            }
        } else {
            tile.progress = 0;
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileCentrifuge tile) {
        tile.spinLast = tile.spin;

        // Spin faster when processing
        if (tile.aspect != null && tile.amount > 0 && !tile.aspect.isPrimal()) {
            tile.spin += 15.0f + tile.getBellowsBonus() * 5.0f;
        } else if (tile.spin > 0) {
            // Slow down when idle
            tile.spin = Math.max(0, tile.spin - 1.0f);
        }

        if (tile.spin >= 360.0f) {
            tile.spin -= 360.0f;
            tile.spinLast -= 360.0f;
        }
    }

    /**
     * Try to pull essentia from connected tubes on horizontal sides.
     */
    private void pullEssentia() {
        if (level == null) return;

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (amount >= MAX_AMOUNT) break;

            BlockEntity te = level.getBlockEntity(worldPosition.relative(dir));
            if (te instanceof IEssentiaTransport transport) {
                if (!transport.canOutputTo(dir.getOpposite())) continue;

                Aspect available = transport.getEssentiaType(dir.getOpposite());
                if (available == null || available.isPrimal()) continue;

                // Check suction
                if (transport.getSuctionAmount(dir.getOpposite()) < getSuctionAmount(dir)) {
                    // Only pull if we're empty or it matches what we have
                    if (aspect == null || aspect == available) {
                        int taken = transport.takeEssentia(available, 1, dir.getOpposite());
                        if (taken > 0) {
                            addToContainer(available, taken);
                        }
                    }
                }
            }
        }
    }

    /**
     * Process one unit of compound essentia into its components.
     */
    private void processEssentia() {
        if (level == null || aspect == null || aspect.isPrimal()) return;

        Aspect[] components = aspect.getComponents();
        if (components == null || components.length != 2) {
            // Invalid compound - shouldn't happen, but clear it
            amount = 0;
            aspect = null;
            markDirtyAndSync();
            return;
        }

        // Try to output both components. Use non-short-circuit OR so component[1]
        // is always attempted; otherwise the second component is lost when the
        // first succeeds.
        boolean output1 = outputEssentia(components[0]);
        boolean output2 = outputEssentia(components[1]);

        if (output1 | output2) {
            amount--;
            if (amount <= 0) {
                amount = 0;
                aspect = null;
            }

            // Play sound
            level.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS,
                    0.2f, 1.0f + (level.random.nextFloat() - level.random.nextFloat()) * 0.2f);

            markDirtyAndSync();
        }
    }

    /**
     * Try to output an aspect upward to a connected container.
     */
    private boolean outputEssentia(Aspect outputAspect) {
        if (level == null || outputAspect == null) return false;

        BlockEntity te = level.getBlockEntity(worldPosition.above());
        if (te instanceof IEssentiaTransport transport) {
            if (transport.canInputFrom(Direction.DOWN)) {
                int added = transport.addEssentia(outputAspect, 1, Direction.DOWN);
                return added > 0;
            }
        } else if (te instanceof IAspectContainer container) {
            if (container.doesContainerAccept(outputAspect)) {
                int remaining = container.addToContainer(outputAspect, 1);
                return remaining == 0;
            }
        }

        // If we can't output, the essentia is lost (original behavior)
        // Could also add flux here
        return true;
    }

    /**
     * Get speed bonus from adjacent bellows.
     */
    private int getBellowsBonus() {
        if (level == null) return 0;
        return TileBellows.getBellows(level, worldPosition, Direction.Plane.HORIZONTAL.stream().toArray(Direction[]::new));
    }

    // ==================== IAspectContainer ====================

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

        // Only accept compound aspects
        if (tag.isPrimal()) return amt;

        if ((amount < MAX_AMOUNT && tag == aspect) || amount == 0) {
            aspect = tag;
            int added = Math.min(amt, MAX_AMOUNT - amount);
            amount += added;
            amt -= added;
            markDirtyAndSync();
        }
        return amt;
    }

    @Override
    public boolean takeFromContainer(Aspect tag, int amt) {
        // Can't take from centrifuge
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
        // Only accept compound aspects
        return !tag.isPrimal();
    }

    // ==================== IEssentiaTransport ====================

    @Override
    public boolean isConnectable(Direction face) {
        return face != Direction.DOWN;
    }

    @Override
    public boolean canInputFrom(Direction face) {
        // Accept input from horizontal sides only
        return face.getAxis().isHorizontal();
    }

    @Override
    public boolean canOutputTo(Direction face) {
        // Output upward only
        return face == Direction.UP;
    }

    @Override
    public void setSuction(Aspect aspect, int amount) {
        // Centrifuge has fixed suction
    }

    @Override
    public Aspect getSuctionType(Direction face) {
        // No specific suction type - will take any compound
        return null;
    }

    @Override
    public int getSuctionAmount(Direction face) {
        if (face.getAxis().isHorizontal() && amount < MAX_AMOUNT) {
            return 128; // High suction for input
        }
        return 0;
    }

    @Override
    public Aspect getEssentiaType(Direction face) {
        // Don't expose the aspect we're processing
        return null;
    }

    @Override
    public int getEssentiaAmount(Direction face) {
        return 0;
    }

    @Override
    public int takeEssentia(Aspect aspect, int amount, Direction face) {
        // Can't take from centrifuge
        return 0;
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, Direction face) {
        if (canInputFrom(face) && !aspect.isPrimal()) {
            return amount - addToContainer(aspect, amount);
        }
        return 0;
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    // ==================== Getters ====================

    public Aspect getAspect() {
        return aspect;
    }

    public int getAmount() {
        return amount;
    }

    public float getProgress() {
        return maxProgress > 0 ? (float) progress / maxProgress : 0;
    }
}




