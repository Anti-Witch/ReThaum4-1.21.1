package thaumcraft.common.tiles.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.lib.research.ResearchManager;
import thaumcraft.common.menu.DeconstructionTableMenu;
import thaumcraft.common.tiles.TileThaumcraftInventory;
import thaumcraft.init.ModBlockEntities;

import javax.annotation.Nullable;

/**
 * TileDeconstructionTable - the TC4 Deconstruction Table.
 *
 * Mechanic (faithful port of OpenTC4 TileDeconstructionTable):
 * - One input slot that only accepts items that have aspects
 *   (TC4's SlotLimitedHasAspects).
 * - While an item sits in the slot the table "chews" on it for 40 ticks
 *   (2 seconds); every finished cycle consumes one item from the stack.
 * - Every consumed item rolls {@code rand.nextInt(80) < primals.visSize()}
 *   where {@code primals} is the item's aspect list reduced to primal aspects.
 *   On a successful roll a single primal aspect is stored in the table.
 * - The stored aspect stops the table until the player collects it in the GUI;
 *   collecting adds one point to the player's research aspect pool
 *   (TC4: PlayerKnowledge.addAspectPool).
 *
 * @author Azanor (original TC4), ported to NeoForge 1.21.1
 */
public class TileDeconstructionTable extends TileThaumcraftInventory implements WorldlyContainer, MenuProvider {

    /** The single input slot. */
    public static final int SLOT_ITEM = 0;

    /** TC4: the table takes 40 ticks to break one item down. */
    public static final int BREAK_TIME = 40;

    /** TC4: {@code world.rand.nextInt(80) < primals.visSize()}. */
    public static final int DISCOVERY_CHANCE = 80;

    /**
     * Block event id used to tell the client that a new aspect appeared.
     * The event parameter carries the aspect index (see {@link #getAspectIndex}):
     * ClientboundBlockEventPacket only has a single byte for the parameter.
     */
    public static final int EVENT_ASPECT_FOUND = 1;

    private static final int[] SLOTS_BOTTOM_AND_SIDES = new int[]{SLOT_ITEM};

    /** The primal aspect currently waiting to be collected (null = none). */
    @Nullable
    public Aspect aspect;

    /** Remaining ticks of the current deconstruction cycle (client-synced via the menu). */
    public int breaktime;

    public TileDeconstructionTable(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DECONSTRUCTION_TABLE.get(), pos, state, 1);
        this.syncedSlots = new int[]{SLOT_ITEM};
    }

    // ==================== Tick ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileDeconstructionTable tile) {
        boolean changed = false;

        if (tile.breaktime == 0 && tile.canBreak()) {
            tile.breaktime = BREAK_TIME;
            changed = true;
        }

        if (tile.breaktime > 0 && tile.canBreak()) {
            tile.breaktime--;
            if (tile.breaktime == 0) {
                tile.breaktime = 0;
                tile.breakItem();
                changed = true;
            }
        } else {
            tile.breaktime = 0;
        }

        if (changed) {
            tile.setChanged();
            tile.syncTile(false);
        }
    }

    /**
     * TC4 canBreak(): there is an item in the slot, no aspect is waiting and the
     * item actually has aspects to break down.
     */
    public boolean canBreak() {
        if (this.aspect != null) {
            return false;
        }
        ItemStack stack = getItem(SLOT_ITEM);
        if (stack.isEmpty()) {
            return false;
        }
        AspectList al = ThaumcraftCraftingManager.getObjectTags(stack);
        return al != null && al.size() != 0;
    }

    /**
     * TC4 breakItem(): consume one item and maybe discover one primal aspect.
     * Note that the item is always consumed, even when the roll fails
     * (that is TC4 behaviour - the table slowly eats the stack).
     */
    public void breakItem() {
        ItemStack stack = getItem(SLOT_ITEM);
        if (stack.isEmpty() || this.aspect != null) {
            return;
        }

        AspectList al = ThaumcraftCraftingManager.getObjectTags(stack);
        if (al == null || al.size() == 0) {
            return;
        }

        AspectList primals = ResearchManager.reduceToPrimals(al);
        if (level != null && primals.visSize() > 0
                && level.random.nextInt(DISCOVERY_CHANCE) < primals.visSize()) {
            Aspect[] aspects = primals.getAspects();
            if (aspects.length > 0) {
                this.aspect = aspects[level.random.nextInt(aspects.length)];
                if (!level.isClientSide) {
                    // Client sparkles in the colour of the discovered aspect
                    level.blockEvent(worldPosition, getBlockState().getBlock(),
                            EVENT_ASPECT_FOUND, getAspectIndex(this.aspect));
                }
            }
        }

        stack.shrink(1);
        if (stack.isEmpty()) {
            setItem(SLOT_ITEM, ItemStack.EMPTY);
        }
        setChanged();
    }

    /**
     * Index of an aspect inside the static aspect registry. Used as the block
     * event parameter (ClientboundBlockEventPacket only transmits one byte per
     * parameter). Both sides build the same index because the aspect registry
     * is static data.
     *
     * @return the index, or -1 if the aspect is unknown
     */
    public static int getAspectIndex(@Nullable Aspect aspect) {
        if (aspect == null) {
            return -1;
        }
        int index = 0;
        for (Aspect candidate : Aspect.aspects.values()) {
            if (candidate == aspect) {
                return index;
            }
            index++;
        }
        return -1;
    }

    /**
     * Reverse of {@link #getAspectIndex(Aspect)}.
     */
    @Nullable
    public static Aspect getAspectByIndex(int index) {
        if (index < 0) {
            return null;
        }
        int current = 0;
        for (Aspect candidate : Aspect.aspects.values()) {
            if (current == index) {
                return candidate;
            }
            current++;
        }
        return null;
    }

    // ==================== NBT ====================

    @Override
    protected void writeSyncNBT(CompoundTag tag) {
        super.writeSyncNBT(tag);
        if (this.aspect != null) {
            tag.putString("Aspect", this.aspect.getTag());
        }
    }

    @Override
    protected void readSyncNBT(CompoundTag tag) {
        super.readSyncNBT(tag);
        this.aspect = tag.contains("Aspect") ? Aspect.getAspect(tag.getString("Aspect")) : null;
    }

    // ==================== Container ====================

    /**
     * The input slot is filled through the GUI (and hoppers), so the block
     * entity data has to be pushed to the clients for the renderer to show
     * the floating item.
     */
    @Override
    public void setItem(int slot, ItemStack stack) {
        ItemStack old = getItem(slot);
        super.setItem(slot, stack);
        if (level != null && !level.isClientSide && !ItemStack.matches(old, stack)) {
            syncTile(false);
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        // TC4 isItemValidForSlot: only items that have aspects
        AspectList al = ThaumcraftCraftingManager.getObjectTags(stack);
        return al != null && al.size() > 0;
    }

    // ==================== WorldlyContainer (TC4 ISidedInventory) ====================

    /**
     * TC4 only exposed the slot to every side except the top.
     */
    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.UP ? new int[0] : SLOTS_BOTTOM_AND_SIDES;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return side != Direction.UP && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    // ==================== MenuProvider ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.thaumcraft.deconstruction_table");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new DeconstructionTableMenu(containerId, playerInventory, this);
    }
}
