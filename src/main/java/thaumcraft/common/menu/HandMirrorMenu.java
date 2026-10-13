package thaumcraft.common.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.items.tools.ItemHandMirror;
import thaumcraft.init.ModMenuTypes;

/**
 * HandMirrorMenu - Menu for the Hand Mirror item.
 * 
 * The hand mirror allows teleporting items to a linked Essentia Mirror.
 * When an item is placed in the slot, it's automatically transported.
 * 
 * Slot Layout:
 * - 0: Transport slot (accepts any item)
 * - 1-27: Player inventory
 * - 28-36: Player hotbar
 */
public class HandMirrorMenu extends AbstractContainerMenu implements ContainerListener {
    
    private final Player player;
    private final ItemStack mirror;
    private final InteractionHand hand;
    private final SimpleContainer transportSlot;
    
    // Client constructor
    public HandMirrorMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readEnum(InteractionHand.class));
    }
    
    // Server constructor
    public HandMirrorMenu(int containerId, Inventory playerInventory, InteractionHand hand) {
        super(ModMenuTypes.HAND_MIRROR.get(), containerId);
        this.player = playerInventory.player;
        this.hand = hand;
        this.mirror = player.getItemInHand(hand);
        
        // Create transport slot container
        this.transportSlot = new SimpleContainer(1);
        transportSlot.addListener(this);
        
        // Slot 0: Transport slot
        addSlot(new Slot(transportSlot, 0, 80, 24));
        
        // Add player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        
        // Add player hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }
    
    @Override
    public void containerChanged(Container container) {
        ItemStack stackInSlot = transportSlot.getItem(0);
        
        // Don't allow placing the mirror itself in the slot
        if (!stackInSlot.isEmpty() && ItemStack.isSameItem(stackInSlot, mirror)) {
            // Close the menu (player put the mirror in its own slot)
            player.closeContainer();
            return;
        }
        
        // Try to transport the item
        if (!player.level().isClientSide && !stackInSlot.isEmpty()) {
            ItemStack toTransport = stackInSlot.copy();
            transportSlot.setItem(0, ItemStack.EMPTY);
            
            if (ItemHandMirror.transport(mirror, toTransport, player, player.level())) {
                // Successfully transported
                broadcastChanges();
            } else {
                // Failed to transport, put item back
                transportSlot.setItem(0, toTransport);
            }
        }
    }
    
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // Prevent clicking on hand mirror in hotbar
        if (slotId >= 0) {
            Slot slot = slots.get(slotId);
            if (slot != null && slot.hasItem()) {
                ItemStack stackInSlot = slot.getItem();
                if (stackInSlot.getItem() instanceof ItemHandMirror) {
                    return;
                }
            }
        }
        super.clicked(slotId, button, clickType, player);
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            
            // Don't allow shift-clicking the hand mirror
            if (stackInSlot.getItem() instanceof ItemHandMirror) {
                return ItemStack.EMPTY;
            }
            
            result = stackInSlot.copy();
            
            if (index == 0) {
                // Move from transport slot to player inventory
                if (!moveItemStackTo(stackInSlot, 1, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Move from player inventory to transport slot
                if (!moveItemStackTo(stackInSlot, 0, 1, false)) {
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
    
    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(hand).getItem() instanceof ItemHandMirror;
    }
    
    @Override
    public void removed(Player player) {
        super.removed(player);
        
        // Drop any item left in the transport slot
        if (!player.level().isClientSide) {
            ItemStack remaining = transportSlot.removeItemNoUpdate(0);
            if (!remaining.isEmpty()) {
                player.drop(remaining, false);
            }
        }
    }
}




