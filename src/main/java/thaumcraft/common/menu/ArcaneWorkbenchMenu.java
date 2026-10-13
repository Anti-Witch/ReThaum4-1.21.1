package thaumcraft.common.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.common.lib.crafting.ArcaneWorkbenchCraftingContainer;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.menu.slot.ArcaneWorkbenchResultSlot;
import thaumcraft.common.menu.slot.WandSlot;
import thaumcraft.common.tiles.crafting.TileArcaneWorkbench;
import thaumcraft.init.ModMenuTypes;

/**
 * ArcaneWorkbenchMenu - TC4 Authentic Server-side menu for the Arcane Workbench.
 * Layout (TC4 authentic):
 * - Output slot (right-center)
 * - 3x3 Crafting grid (center)
 * - Wand slot (top-right)
 * - Player Inventory (bottom)
 */
public class ArcaneWorkbenchMenu extends AbstractContainerMenu {

    // TC4 authentic slot ordering: Output(0), Wand(1), Grid(2-10), PlayerInv(11-37), Hotbar(38-46)
    public static final int OUTPUT_SLOT = 0;
    public static final int WAND_SLOT = 1;
    public static final int GRID_START = 2;
    public static final int GRID_END = 10;
    public static final int PLAYER_INV_START = 11;
    public static final int PLAYER_INV_END = 37;
    public static final int PLAYER_HOTBAR_START = 38;
    public static final int PLAYER_HOTBAR_END = 46;

    private final TileArcaneWorkbench blockEntity;
    private final ContainerLevelAccess access;
    private final Player player;
    private final ArcaneWorkbenchCraftingContainer craftMatrix;
    private final ResultContainer craftResult;

    // Client constructor
    public ArcaneWorkbenchMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, extraData));
    }

    // Server constructor
    public ArcaneWorkbenchMenu(int containerId, Inventory playerInventory, TileArcaneWorkbench blockEntity) {
        super(ModMenuTypes.ARCANE_WORKBENCH.get(), containerId);
        this.blockEntity = blockEntity;
        this.player = playerInventory.player;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.craftResult = new ResultContainer();
        this.craftMatrix = new ArcaneWorkbenchCraftingContainer(blockEntity, this);

        // TC4 authentic slot ordering:
        // 0: Output slot (right-center)
        this.addSlot(new ArcaneWorkbenchResultSlot(blockEntity, player, this.craftMatrix, this.craftResult, 0, 160, 64));

        // 1: Wand Slot (top-right) - TC4 places wand slot second
        this.addSlot(new WandSlot(this.craftMatrix, TileArcaneWorkbench.SLOT_WAND, 160, 24));

        // 2-10: 3x3 Crafting grid (center) - slots 0-8 of craftMatrix
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 3; ++col) {
                this.addSlot(new Slot(this.craftMatrix, col + row * 3, 40 + col * 24, 40 + row * 24));
            }
        }

        // Player Inventory (3 rows of 9)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 16 + col * 18, 151 + row * 18));
            }
        }

        // Player Hotbar
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 16 + col * 18, 209));
        }

        this.slotsChanged(this.craftMatrix);
    }

    private static TileArcaneWorkbench getBlockEntity(Inventory playerInventory, FriendlyByteBuf extraData) {
        if (extraData != null) {
            var pos = extraData.readBlockPos();
            BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
            if (be instanceof TileArcaneWorkbench tile) {
                return tile;
            }
        }
        return new TileArcaneWorkbench(playerInventory.player.blockPosition(), playerInventory.player.level().getBlockState(playerInventory.player.blockPosition()));
    }

    @Override
    public void slotsChanged(Container container) {
        this.access.execute((level, pos) -> {
            if (!level.isClientSide) {
                var recipe = ThaumcraftCraftingManager.findMatchingArcaneRecipe(this.craftMatrix, this.player);
                if (recipe != null) {
                    this.craftResult.setItem(0, recipe.getResultItem(level.registryAccess()));
                } else {
                    this.craftResult.setItem(0, ItemStack.EMPTY);
                }
            }
        });
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, thaumcraft.init.ModBlocks.ARCANE_WORKBENCH.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index == OUTPUT_SLOT) {
                this.access.execute((level, pos) -> itemstack1.getItem().onCraftedBy(itemstack1, level, player));
                if (!this.moveItemStackTo(itemstack1, PLAYER_INV_START, PLAYER_HOTBAR_END + 1, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(itemstack1, itemstack);
            } else if (index >= PLAYER_INV_START && index <= PLAYER_HOTBAR_END) {
                // From player inventory: try to move to grid or wand
                if (!this.moveItemStackTo(itemstack1, GRID_START, GRID_END + 1, false) &&
                    !this.moveItemStackTo(itemstack1, WAND_SLOT, WAND_SLOT + 1, false)) {
                    if (index < PLAYER_HOTBAR_START) {
                        if (!this.moveItemStackTo(itemstack1, PLAYER_HOTBAR_START, PLAYER_HOTBAR_END + 1, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!this.moveItemStackTo(itemstack1, PLAYER_INV_START, PLAYER_INV_END + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else if (index >= GRID_START && index <= GRID_END) {
                // From grid: move to player inventory
                if (!this.moveItemStackTo(itemstack1, PLAYER_INV_START, PLAYER_HOTBAR_END + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index == WAND_SLOT) {
                // From wand: move to player inventory
                if (!this.moveItemStackTo(itemstack1, PLAYER_INV_START, PLAYER_HOTBAR_END + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, PLAYER_INV_START, PLAYER_HOTBAR_END + 1, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemstack1);
        }
        return itemstack;
    }

    public TileArcaneWorkbench getTile() {
        return this.blockEntity;
    }
}
