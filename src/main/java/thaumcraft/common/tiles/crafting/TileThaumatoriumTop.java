package thaumcraft.common.tiles.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectSource;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.menu.ThaumatoriumMenu;
import thaumcraft.common.tiles.TileThaumcraft;
import thaumcraft.init.ModBlockEntities;

import javax.annotation.Nullable;

/**
 * TileThaumatoriumTop - Proxy block entity for the top of the Thaumatorium.
 * 
 * This block entity sits above the main TileThaumatorium and acts as a proxy
 * for essentia transport and inventory access. All operations are delegated
 * to the thaumatorium below.
 * 
 * Features:
 * - Allows essentia tubes to connect from above
 * - Allows hoppers to insert/extract from above
 * - Opens the thaumatorium GUI when interacted with
 * 
 * Ported from 1.12.2
 */
public class TileThaumatoriumTop extends TileThaumcraft implements IAspectSource, IEssentiaTransport, WorldlyContainer, MenuProvider {

    // Cached reference to the thaumatorium below
    @Nullable
    private TileThaumatorium cachedThaumatorium = null;

    public TileThaumatoriumTop(BlockPos pos, BlockState state) {
        super(ModBlockEntities.THAUMATORIUM_TOP.get(), pos, state);
    }

    // ==================== Tick ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileThaumatoriumTop tile) {
        // Refresh cached reference periodically
        if (level.getGameTime() % 20 == 0) {
            tile.cachedThaumatorium = null;
        }
    }

    // ==================== Thaumatorium Reference ====================

    /**
     * Get the thaumatorium below, caching the result.
     */
    @Nullable
    public TileThaumatorium getThaumatorium() {
        if (cachedThaumatorium != null && !cachedThaumatorium.isRemoved()) {
            return cachedThaumatorium;
        }

        if (level != null) {
            BlockEntity below = level.getBlockEntity(worldPosition.below());
            if (below instanceof TileThaumatorium thaumatorium) {
                cachedThaumatorium = thaumatorium;
                return thaumatorium;
            }
        }

        cachedThaumatorium = null;
        return null;
    }

    // ==================== IAspectSource ====================

    @Override
    public AspectList getAspects() {
        TileThaumatorium t = getThaumatorium();
        return t != null ? t.getAspects() : new AspectList();
    }

    @Override
    public void setAspects(AspectList aspects) {
        TileThaumatorium t = getThaumatorium();
        if (t != null) {
            t.setAspects(aspects);
        }
    }

    @Override
    public int addToContainer(Aspect tag, int amt) {
        TileThaumatorium t = getThaumatorium();
        return t != null ? t.addToContainer(tag, amt) : amt;
    }

    @Override
    public boolean takeFromContainer(Aspect tag, int amt) {
        TileThaumatorium t = getThaumatorium();
        return t != null && t.takeFromContainer(tag, amt);
    }

    @Override
    public boolean takeFromContainer(AspectList list) {
        TileThaumatorium t = getThaumatorium();
        return t != null && t.takeFromContainer(list);
    }

    @Override
    public boolean doesContainerContainAmount(Aspect tag, int amt) {
        TileThaumatorium t = getThaumatorium();
        return t != null && t.doesContainerContainAmount(tag, amt);
    }

    @Override
    public boolean doesContainerContain(AspectList list) {
        TileThaumatorium t = getThaumatorium();
        return t != null && t.doesContainerContain(list);
    }

    @Override
    public int containerContains(Aspect tag) {
        TileThaumatorium t = getThaumatorium();
        return t != null ? t.containerContains(tag) : 0;
    }

    @Override
    public boolean doesContainerAccept(Aspect tag) {
        TileThaumatorium t = getThaumatorium();
        return t != null && t.doesContainerAccept(tag);
    }

    // ==================== IEssentiaTransport ====================

    @Override
    public boolean isConnectable(Direction face) {
        TileThaumatorium t = getThaumatorium();
        // Allow connections from sides and above, but not below (that's the main thaumatorium)
        return t != null && face != Direction.DOWN;
    }

    @Override
    public boolean canInputFrom(Direction face) {
        TileThaumatorium t = getThaumatorium();
        return t != null && face != Direction.DOWN;
    }

    @Override
    public boolean canOutputTo(Direction face) {
        return false; // Thaumatorium doesn't output essentia
    }

    @Override
    public void setSuction(Aspect aspect, int amount) {
        TileThaumatorium t = getThaumatorium();
        if (t != null) {
            t.setSuction(aspect, amount);
        }
    }

    @Override
    public Aspect getSuctionType(Direction face) {
        TileThaumatorium t = getThaumatorium();
        return t != null ? t.getSuctionType(face) : null;
    }

    @Override
    public int getSuctionAmount(Direction face) {
        TileThaumatorium t = getThaumatorium();
        return t != null && face != Direction.DOWN ? t.getSuctionAmount(face) : 0;
    }

    @Override
    public Aspect getEssentiaType(Direction face) {
        return null; // Top doesn't store essentia directly
    }

    @Override
    public int getEssentiaAmount(Direction face) {
        return 0;
    }

    @Override
    public int takeEssentia(Aspect aspect, int amount, Direction face) {
        TileThaumatorium t = getThaumatorium();
        return t != null ? t.takeEssentia(aspect, amount, face) : 0;
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, Direction face) {
        TileThaumatorium t = getThaumatorium();
        if (t != null && canInputFrom(face)) {
            return t.addEssentia(aspect, amount, face);
        }
        return 0;
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    @Override
    public boolean isBlocked() {
        return false;
    }

    // ==================== WorldlyContainer ====================

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[] { 0, 1 }; // Input and output slots
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return index == 0; // Can only insert into input slot
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return index == 1; // Can only extract from output slot
    }

    @Override
    public int getContainerSize() {
        TileThaumatorium t = getThaumatorium();
        return t != null ? t.getContainerSize() : 2;
    }

    @Override
    public boolean isEmpty() {
        TileThaumatorium t = getThaumatorium();
        return t == null || t.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        TileThaumatorium t = getThaumatorium();
        return t != null ? t.getItem(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        TileThaumatorium t = getThaumatorium();
        return t != null ? t.removeItem(slot, amount) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        TileThaumatorium t = getThaumatorium();
        return t != null ? t.removeItemNoUpdate(slot) : ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        TileThaumatorium t = getThaumatorium();
        if (t != null) {
            t.setItem(slot, stack);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        TileThaumatorium t = getThaumatorium();
        return t != null && t.stillValid(player);
    }

    @Override
    public void clearContent() {
        TileThaumatorium t = getThaumatorium();
        if (t != null) {
            t.clearContent();
        }
    }

    // ==================== MenuProvider ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.thaumcraft.thaumatorium");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        TileThaumatorium t = getThaumatorium();
        if (t != null) {
            return new ThaumatoriumMenu(containerId, playerInventory, t);
        }
        return null;
    }
}




