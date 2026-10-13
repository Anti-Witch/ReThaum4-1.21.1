package thaumcraft.common.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.entities.monster.EntityPech;
import thaumcraft.common.menu.slot.OutputSlot;
import thaumcraft.init.ModMenuTypes;

/**
 * PechMenu - Menu for trading with tamed Pechs.
 * 
 * Place an item in the input slot and click trade to receive items
 * based on the Pech's inventory and the value of the offered item.
 * 
 * Slot Layout:
 * - 0: Input slot (item to trade)
 * - 1-4: Output slots (traded items from Pech)
 * - 5-31: Player inventory
 * - 32-40: Player hotbar
 */
public class PechMenu extends AbstractContainerMenu implements ContainerListener {
    
    private final EntityPech pech;
    private final Player player;
    private final SimpleContainer tradeInventory;
    
    // Client constructor
    public PechMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, getEntity(playerInventory, extraData));
    }
    
    // Server constructor
    public PechMenu(int containerId, Inventory playerInventory, EntityPech pech) {
        super(ModMenuTypes.PECH_TRADING.get(), containerId);
        this.pech = pech;
        this.player = playerInventory.player;
        
        // Set pech as trading
        pech.trading = true;
        
        // Create trade inventory (1 input + 4 outputs)
        this.tradeInventory = new SimpleContainer(5);
        tradeInventory.addListener(this);
        
        // Slot 0: Input slot
        addSlot(new Slot(tradeInventory, 0, 36, 29));
        
        // Slots 1-4: Output slots (2x2 grid)
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                addSlot(new OutputSlot(tradeInventory, 1 + col + row * 2, 106 + col * 18, 20 + row * 18));
            }
        }
        
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
    
    private static EntityPech getEntity(Inventory playerInventory, FriendlyByteBuf extraData) {
        int entityId = extraData.readInt();
        Entity entity = playerInventory.player.level().getEntity(entityId);
        if (entity instanceof EntityPech pechEntity) {
            return pechEntity;
        }
        throw new IllegalStateException("Entity is not a Pech");
    }
    
    @Override
    public void containerChanged(Container container) {
        broadcastChanges();
    }
    
    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (button == 0) {
            // Trade button clicked
            generateTradeResults();
            return true;
        }
        return false;
    }
    
    /**
     * Generate trade results based on the input item.
     * TODO: Implement full trading logic based on item value and Pech type.
     */
    private void generateTradeResults() {
        if (player.level().isClientSide()) return;
        
        ItemStack input = tradeInventory.getItem(0);
        if (input.isEmpty()) return;
        
        // Check if outputs are empty
        boolean outputsEmpty = true;
        for (int i = 1; i <= 4; i++) {
            if (!tradeInventory.getItem(i).isEmpty()) {
                outputsEmpty = false;
                break;
            }
        }
        if (!outputsEmpty) return;
        
        // Check if Pech values this item
        if (!pech.isValued(input)) return;
        
        // Get value and generate results
        int value = pech.getValue(input);
        
        // TODO: Implement full trading logic with tradeInventory and loot
        // For now, just consume the input
        tradeInventory.removeItem(0, 1);
        
        // Play trade sound
        // TODO: pech.playSound(SoundsTC.pech_trade, 0.4f, 1.0f);
        
        broadcastChanges();
    }
    
    @Override
    public boolean stillValid(Player player) {
        return pech.isAlive() && pech.isTamed() && pech.distanceTo(player) < 8.0f;
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            result = stackInSlot.copy();
            
            if (index == 0) {
                // Move from input slot to player inventory
                if (!moveItemStackTo(stackInSlot, 5, 41, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 1 && index < 5) {
                // Move from output slots to player inventory
                if (!moveItemStackTo(stackInSlot, 5, 41, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 5 && index < 41) {
                // Move from player inventory to input slot
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
    public void removed(Player player) {
        super.removed(player);
        pech.trading = false;
        
        // Return items to player or drop them
        if (!player.level().isClientSide()) {
            for (int i = 0; i < 5; i++) {
                ItemStack stack = tradeInventory.removeItemNoUpdate(i);
                if (!stack.isEmpty()) {
                    player.drop(stack, false);
                }
            }
        }
    }
    
    public EntityPech getPech() {
        return pech;
    }
    
    public Container getTradeInventory() {
        return tradeInventory;
    }
}




