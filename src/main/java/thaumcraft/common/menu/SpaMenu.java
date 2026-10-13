package thaumcraft.common.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.common.items.consumables.ItemBathSalts;
import thaumcraft.common.menu.slot.LimitedByClassSlot;
import thaumcraft.common.tiles.devices.TileSpa;
import thaumcraft.init.ModMenuTypes;

/**
 * SpaMenu - Menu for the Void Bath (Spa) block.
 * 
 * The Void Bath uses bath salts to apply various effects.
 * When combined with other ingredients, creates different spa effects.
 * 
 * Slot Layout:
 * - 0: Bath salts slot
 * - 1-27: Player inventory
 * - 28-36: Player hotbar
 */
public class SpaMenu extends AbstractContainerMenu {
    
    private final TileSpa blockEntity;
    
    // Client constructor
    public SpaMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, extraData));
    }
    
    // Server constructor
    public SpaMenu(int containerId, Inventory playerInventory, TileSpa blockEntity) {
        super(ModMenuTypes.SPA.get(), containerId);
        this.blockEntity = blockEntity;
        
        // Slot 0: Bath salts
        addSlot(new LimitedByClassSlot(ItemBathSalts.class, blockEntity, 0, 65, 31));
        
        // Player inventory
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
    
    private static TileSpa getBlockEntity(Inventory playerInventory, FriendlyByteBuf extraData) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(extraData.readBlockPos());
        if (be instanceof TileSpa tile) {
            return tile;
        }
        throw new IllegalStateException("Block entity is not a TileSpa");
    }
    
    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (button == 1) {
            blockEntity.toggleMix();
            return true;
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
                // Move from bath salts slot to player inventory
                if (!moveItemStackTo(stackInSlot, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (stackInSlot.getItem() instanceof ItemBathSalts) {
                // Move bath salts to slot
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
    
    public TileSpa getBlockEntity() {
        return blockEntity;
    }
}




