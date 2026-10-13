package thaumcraft.common.tiles.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IAspectSource;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.api.blocks.ILabelable;
import thaumcraft.common.items.consumables.ItemLabel;
import thaumcraft.common.tiles.TileThaumcraft;
import thaumcraft.init.ModBlockEntities;
import thaumcraft.init.ModSounds;

/**
 * Alembic tile entity - distills essentia from crucible/smelter below.
 * Stacks on top of furnaces/crucibles and collects specific aspects.
 */
public class TileAlembic extends TileThaumcraft implements IAspectContainer, IAspectSource, IEssentiaTransport, ILabelable {

    public static final int MAX_AMOUNT = 128;

    public Aspect aspect = null;
    public Aspect aspectFilter = null;
    public int amount = 0;
    public int facing = Direction.DOWN.ordinal();
    public boolean aboveFurnace = false;
    
    private Direction facingDir = null;

    public TileAlembic(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public TileAlembic(BlockPos pos, BlockState state) {
        this(ModBlockEntities.ALEMBIC.get(), pos, state);
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
        facingDir = Direction.values()[facing];
    }

    // ==================== IAspectContainer ====================

    @Override
    public AspectList getAspects() {
        return (aspect != null && amount > 0) ? new AspectList().add(aspect, amount) : new AspectList();
    }

    @Override
    public void setAspects(AspectList aspects) {
        // Alembics don't allow direct setting
    }

    @Override
    public int addToContainer(Aspect tag, int amt) {
        if (aspectFilter != null && tag != aspectFilter) {
            return amt;
        }
        if ((amount < MAX_AMOUNT && tag == aspect) || amount == 0) {
            aspect = tag;
            int added = Math.min(amt, MAX_AMOUNT - amount);
            amount += added;
            amt -= added;
        }
        markDirtyAndSync();
        return amt;
    }

    @Override
    public boolean takeFromContainer(Aspect tag, int amt) {
        if (amount == 0 || aspect == null) {
            aspect = null;
            amount = 0;
        }
        if (aspect != null && amount >= amt && tag == aspect) {
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
        return amount > 0 && aspect != null && list.getAmount(aspect) > 0;
    }

    @Override
    public int containerContains(Aspect tag) {
        return (tag == aspect) ? amount : 0;
    }

    @Override
    public boolean doesContainerAccept(Aspect tag) {
        // 1.12.2: alembics accept any aspect; the filter is enforced in addToContainer
        return true;
    }

    @Override
    public boolean isBlocked() {
        // Alembics are never blocked; mirrors TC4 behaviour
        return false;
    }

    // ==================== IEssentiaTransport ====================

    @Override
    public boolean isConnectable(Direction face) {
        return face != Direction.values()[facing] && face != Direction.DOWN;
    }

    @Override
    public boolean canInputFrom(Direction face) {
        return false; // Alembics only receive from below (special handling)
    }

    @Override
    public boolean canOutputTo(Direction face) {
        return face != Direction.values()[facing] && face != Direction.DOWN;
    }

    @Override
    public void setSuction(Aspect aspect, int amount) {
        // Alembics don't have suction
    }

    @Override
    public Aspect getSuctionType(Direction face) {
        return null;
    }

    @Override
    public int getSuctionAmount(Direction face) {
        return 0;
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
        return (canOutputTo(face) && takeFromContainer(aspect, amount)) ? amount : 0;
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, Direction face) {
        return 0; // Alembics don't accept essentia through transport
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    // Uses default isBlocked() from interface

    // ==================== Static Helpers ====================

    /**
     * Process alembics stacked above a position.
     * Called by crucibles/smelters to push essentia up into alembics.
     * 
     * @param level The world
     * @param pos Position of the source (crucible/smelter)
     * @param aspect The aspect to try to add
     * @return true if the aspect was successfully added to an alembic
     */
    public static boolean processAlembics(Level level, BlockPos pos, Aspect aspect) {
        // First pass: find alembics that already have this aspect
        int deep = 1;
        while (true) {
            BlockEntity be = level.getBlockEntity(pos.above(deep));
            if (be instanceof TileAlembic alembic) {
                if (alembic.amount > 0 && alembic.aspect == aspect && alembic.addToContainer(aspect, 1) == 0) {
                    return true;
                }
                deep++;
            } else {
                break;
            }
        }

        // Second pass: find any alembic that can accept this aspect
        deep = 1;
        while (true) {
            BlockEntity be = level.getBlockEntity(pos.above(deep));
            if (be instanceof TileAlembic alembic) {
                if ((alembic.aspectFilter == null || alembic.aspectFilter == aspect) && alembic.addToContainer(aspect, 1) == 0) {
                    return true;
                }
                deep++;
            } else {
                break;
            }
        }

        return false;
    }

    // ==================== Labels (TC4 BlockMetalDevice.onBlockActivated) ====================

    /**
     * Apply an alembic label. Mirrors TC4: a label with an aspect locks the alembic
     * to that aspect; a blank label locks a non-empty alembic to the essentia it
     * already holds. The label is rendered on the side it was applied to.
     */
    @Override
    public boolean applyLabel(Player player, BlockPos pos, Direction side, ItemStack labelStack) {
        // Sneaking is used to remove filters, not to apply them
        if (player.isShiftKeyDown() || aspectFilter != null) {
            return false;
        }

        Aspect labelAspect = (labelStack.getItem() instanceof ItemLabel label)
                ? label.getAspect(labelStack) : null;

        // An empty alembic can only be locked to the aspect written on the label
        if (amount <= 0) {
            if (labelAspect == null) {
                return false;
            }
            aspect = labelAspect;
        }

        // TC4 only lets the wand rotate the alembic to horizontal sides
        if (side.getAxis().isHorizontal()) {
            facing = side.ordinal();
            facingDir = side;
        }
        aspectFilter = aspect;
        markDirtyAndSync();
        if (level != null && !level.isClientSide) {
            level.playSound(null, pos, ModSounds.PAGE.get(), SoundSource.BLOCKS, 1.0F, 0.9F);
        }
        return true;
    }

    /**
     * Remove the aspect filter without touching the alembic contents.
     * The caller is responsible for dropping the label item.
     */
    public void removeAspectFilter() {
        if (aspectFilter != null) {
            aspectFilter = null;
            markDirtyAndSync();
        }
    }

    /**
     * TC4 shift + right-click with an empty hand: empty the alembic.
     */
    public void clearContents() {
        amount = 0;
        aspect = null;
        markDirtyAndSync();
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

    public int getFacing() {
        return facing;
    }

    public void setFacing(int facing) {
        this.facing = facing;
        this.facingDir = Direction.values()[facing];
        markDirtyAndSync();
    }

    // ==================== Rendering ====================

    /**
     * Custom render bounding box for rendering.
     * Note: In 1.20.1, this is accessed via TESR if needed.
     */
    public AABB getCustomRenderBoundingBox() {
        return new AABB(
                worldPosition.getX() - 0.1, worldPosition.getY() - 0.1, worldPosition.getZ() - 0.1,
                worldPosition.getX() + 1.1, worldPosition.getY() + 1.1, worldPosition.getZ() + 1.1
        );
    }
}




