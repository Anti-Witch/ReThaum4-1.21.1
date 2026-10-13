package thaumcraft.common.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.entities.construct.EntityArcaneBore;
import thaumcraft.common.menu.slot.PickaxeSlot;
import thaumcraft.init.ModMenuTypes;

/**
 * ArcaneBoreMenu - Menu for the Arcane Bore entity.
 * 
 * The Arcane Bore is an automated mining construct that requires
 * a pickaxe to operate. The pickaxe determines mining speed and capabilities.
 * 
 * Slot Layout:
 * - 0: Pickaxe slot
 * - 1-27: Player inventory
 * - 28-36: Player hotbar
 */
public class ArcaneBoreMenu extends AbstractContainerMenu {
    
    private final EntityArcaneBore bore;
    
    // Client constructor
    public ArcaneBoreMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, getEntity(playerInventory, extraData));
    }
    
    // Server constructor
    public ArcaneBoreMenu(int containerId, Inventory playerInventory, EntityArcaneBore bore) {
        super(ModMenuTypes.ARCANE_BORE.get(), containerId);
        this.bore = bore;
        
        // Slot 0: Pickaxe slot
        addSlot(new PickaxeSlot(bore, 0, 80, 29));
        
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
    
    private static EntityArcaneBore getEntity(Inventory playerInventory, FriendlyByteBuf extraData) {
        int entityId = extraData.readInt();
        Entity entity = playerInventory.player.level().getEntity(entityId);
        if (entity instanceof EntityArcaneBore bore) {
            return bore;
        }
        throw new IllegalStateException("Entity is not an Arcane Bore");
    }
    
    @Override
    public boolean stillValid(Player player) {
        return bore.isAlive() && bore.distanceTo(player) < 8.0f;
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            result = stackInSlot.copy();
            
            if (index == 0) {
                // Move from pickaxe slot to player inventory
                if (!moveItemStackTo(stackInSlot, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (PickaxeSlot.isValidPickaxe(stackInSlot)) {
                // Move pickaxes to pickaxe slot
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
    
    public EntityArcaneBore getBore() {
        return bore;
    }
}




