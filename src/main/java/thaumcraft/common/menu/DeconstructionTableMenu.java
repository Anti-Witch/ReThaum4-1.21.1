package thaumcraft.common.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.common.menu.slot.AspectSlot;
import thaumcraft.common.tiles.crafting.TileDeconstructionTable;
import thaumcraft.init.ModBlocks;
import thaumcraft.init.ModMenuTypes;

/**
 * DeconstructionTableMenu - container for the TC4 Deconstruction Table.
 *
 * Ported from OpenTC4 ContainerDeconstructionTable.
 *
 * Slot layout:
 * - 0: input (items that have aspects)
 * - 1-27: player inventory (3 rows of 9)
 * - 28-36: player hotbar
 *
 * Synced data:
 * - 0: breaktime (0..40), used by the GUI progress bar
 *
 * The waiting aspect itself is synced through the block entity
 * (TileThaumcraft.getUpdateTag), exactly like the model needs it for rendering.
 */
public class DeconstructionTableMenu extends AbstractContainerMenu {

    public static final int SLOT_ITEM = 0;

    public static final int PLAYER_INV_START = 1;
    public static final int PLAYER_INV_END = 27;
    public static final int PLAYER_HOTBAR_START = 28;
    public static final int PLAYER_HOTBAR_END = 36;

    /** TC4 button id used by ContainerDeconstructionTable.enchantItem(). */
    public static final int BUTTON_COLLECT_ASPECT = 1;

    private final TileDeconstructionTable blockEntity;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    // Client constructor
    public DeconstructionTableMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, extraData));
    }

    // Server constructor
    public DeconstructionTableMenu(int containerId, Inventory playerInventory, TileDeconstructionTable blockEntity) {
        super(ModMenuTypes.DECONSTRUCTION_TABLE.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        // Live view on the tile's breaktime - broadcastChanges() sends it to the
        // client while the GUI is open (TC4 used ICrafting progress bars).
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return index == 0 ? DeconstructionTableMenu.this.blockEntity.breaktime : 0;
            }

            @Override
            public void set(int index, int value) {
                if (index == 0) {
                    DeconstructionTableMenu.this.blockEntity.breaktime = value;
                }
            }

            @Override
            public int getCount() {
                return 1;
            }
        };
        addDataSlots(this.data);

        // Input slot (TC4: SlotLimitedHasAspects at 64,16)
        addSlot(new AspectSlot(blockEntity, SLOT_ITEM, 64, 16));

        // Player inventory (3 rows of 9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    private static TileDeconstructionTable getBlockEntity(Inventory playerInventory, FriendlyByteBuf extraData) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(extraData.readBlockPos());
        if (be instanceof TileDeconstructionTable table) {
            return table;
        }
        throw new IllegalStateException("Block entity is not a TileDeconstructionTable");
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.DECONSTRUCTION_TABLE.get());
    }

    /**
     * TC4 ContainerDeconstructionTable.enchantItem(): button 1 collects the
     * waiting aspect into the player's research aspect pool.
     */
    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId != BUTTON_COLLECT_ASPECT) {
            return false;
        }
        if (this.blockEntity.aspect == null) {
            return false;
        }

        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
        if (knowledge != null) {
            knowledge.addAspectsToResearchPool(this.blockEntity.aspect, 1);
            if (player instanceof ServerPlayer serverPlayer) {
                knowledge.sync(serverPlayer);
            }
        }

        this.blockEntity.aspect = null;
        this.blockEntity.setChanged();
        this.blockEntity.syncTile(false);
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            result = stackInSlot.copy();

            // From the table's slot into the player inventory
            if (index == SLOT_ITEM) {
                if (!moveItemStackTo(stackInSlot, PLAYER_INV_START, PLAYER_HOTBAR_END + 1, false)) {
                    return ItemStack.EMPTY;
                }
            }
            // From the player inventory into the table
            else if (AspectSlot.hasAspects(stackInSlot)) {
                if (!moveItemStackTo(stackInSlot, SLOT_ITEM, SLOT_ITEM + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= PLAYER_INV_START && index <= PLAYER_INV_END) {
                if (!moveItemStackTo(stackInSlot, PLAYER_HOTBAR_START, PLAYER_HOTBAR_END + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= PLAYER_HOTBAR_START && index <= PLAYER_HOTBAR_END) {
                if (!moveItemStackTo(stackInSlot, PLAYER_INV_START, PLAYER_INV_END + 1, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stackInSlot.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stackInSlot);
        }

        return result;
    }

    // ==================== Accessors ====================

    public TileDeconstructionTable getBlockEntity() {
        return blockEntity;
    }

    public int getBreakTime() {
        return this.data.get(0);
    }

    /** TC4 TileDeconstructionTable.getBreakTimeScaled(). */
    public int getBreakTimeScaled(int scale) {
        return getBreakTime() * scale / TileDeconstructionTable.BREAK_TIME;
    }
}
