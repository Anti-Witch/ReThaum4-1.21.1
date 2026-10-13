package thaumcraft.common.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.common.items.casters.ItemFocus;
import thaumcraft.common.menu.slot.FocusSlot;
import thaumcraft.common.tiles.crafting.TileFocalManipulator;
import thaumcraft.init.ModMenuTypes;
import thaumcraft.init.ModSounds;

/**
 * FocalManipulatorMenu - Menu for the Focal Manipulator block.
 * Used to modify and upgrade foci.
 * 
 * Slot Layout:
 * - 0: Focus slot
 * - 1-27: Player inventory
 * - 28-36: Player hotbar
 */
public class FocalManipulatorMenu extends AbstractContainerMenu {

    private final TileFocalManipulator blockEntity;
    private final Player player;

    // Client constructor
    public FocalManipulatorMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, extraData));
    }

    // Server constructor
    public FocalManipulatorMenu(int containerId, Inventory playerInventory, TileFocalManipulator blockEntity) {
        super(ModMenuTypes.FOCAL_MANIPULATOR.get(), containerId);
        this.blockEntity = blockEntity;
        this.player = playerInventory.player;

        // Slot 0: Focus input
        addSlot(new FocusSlot(blockEntity, 0, 31, 191));

        // Player inventory - positioned vertically on left side (from original)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, row * 18 - 62, 64 + col * 18));
            }
        }

        // Player hotbar - 3x3 arrangement
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new Slot(playerInventory, row + col * 3, row * 18 - 62, col * 18 + 7));
            }
        }
    }

    private static TileFocalManipulator getBlockEntity(Inventory playerInventory, FriendlyByteBuf extraData) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(extraData.readBlockPos());
        if (be instanceof TileFocalManipulator tile) {
            return tile;
        }
        throw new IllegalStateException("Block entity is not a TileFocalManipulator");
    }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (button == 0) {
            if (!blockEntity.startCraft(button, player)) {
                player.level().playSound(player, blockEntity.getBlockPos(), 
                        ModSounds.CRAFT_FAIL.get(), SoundSource.BLOCKS, 0.33f, 1.0f);
            }
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            result = stackInSlot.copy();

            if (index == 0) {
                // Move from focus slot to player inventory
                if (!moveItemStackTo(stackInSlot, 1, 37, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (stackInSlot.getItem() instanceof ItemFocus) {
                // Move focus to focus slot
                if (!moveItemStackTo(stackInSlot, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 1 && index < 28) {
                // Move from inventory to hotbar
                if (!moveItemStackTo(stackInSlot, 28, 37, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 28 && index < 37) {
                // Move from hotbar to inventory
                if (!moveItemStackTo(stackInSlot, 1, 28, false)) {
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

    public TileFocalManipulator getBlockEntity() {
        return blockEntity;
    }
}




