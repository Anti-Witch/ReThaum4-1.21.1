package thaumcraft.common.menu;

import net.minecraft.core.NonNullList;
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
import thaumcraft.common.items.casters.ItemFocus;
import thaumcraft.common.items.casters.ItemFocusPouch;
import thaumcraft.common.menu.slot.FocusSlot;
import thaumcraft.init.ModMenuTypes;

/**
 * FocusPouchMenu - Menu for the Focus Pouch item.
 * Stores up to 18 foci in a 6x3 grid.
 * 
 * Slot Layout:
 * - 0-17: Focus pouch slots (6x3)
 * - 18-44: Player inventory (3x9)
 * - 45-53: Player hotbar
 */
public class FocusPouchMenu extends AbstractContainerMenu implements ContainerListener {
    
    public static final int POUCH_SIZE = 18;
    
    private final Player player;
    private final ItemStack pouch;
    private final InteractionHand hand;
    private final int blockedSlot;
    private final SimpleContainer pouchInventory;
    
    // Client constructor
    public FocusPouchMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readEnum(InteractionHand.class));
    }
    
    // Server constructor
    public FocusPouchMenu(int containerId, Inventory playerInventory, InteractionHand hand) {
        super(ModMenuTypes.FOCUS_POUCH.get(), containerId);
        this.player = playerInventory.player;
        this.hand = hand;
        this.pouch = player.getItemInHand(hand);
        
        // Calculate blocked slot to prevent moving the pouch itself
        // Main hand: current item slot + player inventory offset
        // Off hand: -1 (not blocked since it's not in the regular inventory)
        this.blockedSlot = (hand == InteractionHand.MAIN_HAND) ? 
                playerInventory.selected + POUCH_SIZE + 27 : -1;
        
        // Create and populate pouch inventory
        this.pouchInventory = new SimpleContainer(POUCH_SIZE) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }
            
            @Override
            public boolean canPlaceItem(int slot, ItemStack stack) {
                return !stack.isEmpty() && stack.getItem() instanceof ItemFocus;
            }
        };
        pouchInventory.addListener(this);
        
        // Load existing contents from pouch NBT
        if (pouch.getItem() instanceof ItemFocusPouch pouchItem) {
            NonNullList<ItemStack> contents = pouchItem.getInventory(pouch);
            for (int i = 0; i < Math.min(contents.size(), POUCH_SIZE); i++) {
                pouchInventory.setItem(i, contents.get(i));
            }
        }
        
        // Add pouch slots (6 columns x 3 rows)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 6; col++) {
                int index = col + row * 6;
                // FocusSlot with stack limit of 1
                addSlot(new FocusSlot(pouchInventory, index, 37 + col * 18, 51 + row * 18, 1));
            }
        }
        
        // Add player inventory (3 rows of 9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 151 + row * 18));
            }
        }
        
        // Add player hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 209));
        }
    }
    
    @Override
    public void containerChanged(Container container) {
        broadcastChanges();
    }
    
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // Prevent clicking on the pouch itself
        if (slotId == blockedSlot) {
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Prevent moving the pouch
        if (index == blockedSlot) {
            return ItemStack.EMPTY;
        }
        
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            result = stackInSlot.copy();
            
            if (index < POUCH_SIZE) {
                // Move from pouch to player inventory
                if (!moveItemStackTo(stackInSlot, POUCH_SIZE, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Move from player inventory to pouch (only foci)
                if (stackInSlot.getItem() instanceof ItemFocus) {
                    if (!moveItemStackTo(stackInSlot, 0, POUCH_SIZE, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
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
        return player.getItemInHand(hand).getItem() instanceof ItemFocusPouch;
    }
    
    @Override
    public void removed(Player player) {
        super.removed(player);
        
        // Save contents back to the pouch
        if (!player.level().isClientSide && pouch.getItem() instanceof ItemFocusPouch pouchItem) {
            NonNullList<ItemStack> contents = NonNullList.withSize(POUCH_SIZE, ItemStack.EMPTY);
            for (int i = 0; i < POUCH_SIZE; i++) {
                contents.set(i, pouchInventory.getItem(i));
            }
            pouchItem.setInventory(pouch, contents);
            
            // Update the held item
            player.setItemInHand(hand, pouch);
        }
    }
    
    public Container getPouchInventory() {
        return pouchInventory;
    }
}




