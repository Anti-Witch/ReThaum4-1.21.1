package thaumcraft.common.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.api.items.IScribeTools;
import thaumcraft.common.items.ItemResearchNotes;
import thaumcraft.common.tiles.crafting.TileResearchTable;
import thaumcraft.init.ModBlocks;
import thaumcraft.init.ModMenuTypes;

/**
 * ResearchTableMenu - Server-side menu for the Research Table (TC4 hex-grid).
 *
 * Slots:
 * - 0: Scribing tools
 * - 1: Research notes
 * - 2-28: Player inventory (3 rows of 9)
 * - 29-37: Player hotbar (9 slots)
 *
 * The full-screen ResearchTableScreen renders all slots itself; this menu only
 * provides the inventory contents lookup, quick-move logic and the note
 * duplication button (ID 5). Positions are nominal (used for quick-move
 * indices only).
 *
 * PORTED from OpenTC4 ContainerResearchTable (TC4) - hex-grid mechanic is
 * TC4-only, not present in TC6/ref.
 */
public class ResearchTableMenu extends AbstractContainerMenu {

    private final TileResearchTable blockEntity;
    private final ContainerLevelAccess access;

    // Client constructor (from network)
    public ResearchTableMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, extraData));
    }

    // Server constructor
    public ResearchTableMenu(int containerId, Inventory playerInventory, TileResearchTable blockEntity) {
        super(ModMenuTypes.RESEARCH_TABLE.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        // Classic TC4 Research Table slot layout (relative to guiLeft/guiTop centered on screen)
        // Scribing tools slot (top-left, TC4: 14,10)
        addSlot(new Slot(blockEntity, 0, 14, 10) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof IScribeTools;
            }
        });

        // Research notes slot (TC4: 70,10)
        addSlot(new Slot(blockEntity, 1, 70, 10) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ItemResearchNotes;
            }
        });

        // Player inventory (3x9), TC4: x=48, rows y=175/193/211
        int invX = 48;
        int invY = 175;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, invX + col * 18, invY + row * 18));
            }
        }

        // Player hotbar (1x9), TC4: y=233
        int hotbarY = 233;
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, invX + col * 18, hotbarY));
        }
    }

    private static TileResearchTable getBlockEntity(Inventory playerInventory, FriendlyByteBuf extraData) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(extraData.readBlockPos());
        if (be instanceof TileResearchTable tile) {
            return tile;
        }
        throw new IllegalStateException("Block entity is not a TileResearchTable");
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.RESEARCH_TABLE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(slotIndex);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            result = stackInSlot.copy();

            // From table slots to player
            if (slotIndex < 2) {
                if (!moveItemStackTo(stackInSlot, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // From player inventory
            else {
                // Scribing tools
                if (stackInSlot.getItem() instanceof IScribeTools) {
                    if (!moveItemStackTo(stackInSlot, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                }
                // Research notes
                else if (stackInSlot.getItem() instanceof ItemResearchNotes) {
                    if (!moveItemStackTo(stackInSlot, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                }
                // Between inventory and hotbar
                else if (slotIndex < 29) {
                    if (!moveItemStackTo(stackInSlot, 29, 38, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (!moveItemStackTo(stackInSlot, 2, 29, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return result;
    }

    // ==================== Button Handling ====================

    /**
     * Handle the note-duplication button press.
     * Button ID 5 = duplicate note (sent via handleInventoryButtonClick).
     */
    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId == 5) {
            blockEntity.duplicate(player);
            return true;
        }
        return false;
    }

    // ==================== Accessors ====================

    public TileResearchTable getBlockEntity() {
        return blockEntity;
    }

    public boolean hasScribingTools() {
        ItemStack tools = blockEntity.getItem(TileResearchTable.SLOT_SCRIBING_TOOLS);
        return !tools.isEmpty() && tools.getItem() instanceof IScribeTools;
    }

    // ==================== Custom Slots ====================

    /** Slot for scribing tools */
    private static class ScribingToolsSlot extends Slot {
        public ScribingToolsSlot(TileResearchTable container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof IScribeTools;
        }
    }

    /** Slot for research notes */
    private static class NoteSlot extends Slot {
        public NoteSlot(TileResearchTable container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof ItemResearchNotes;
        }
    }
}