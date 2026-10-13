package thaumcraft.common.menu.slot;

import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * MobEquipmentSlot - A slot that interfaces with a mob's held item.
 * 
 * Used for entity-based containers like turrets and the arcane bore
 * where we need to display/modify what an entity is holding.
 * 
 * This slot doesn't use a Container - it directly accesses the mob's equipment.
 */
public class MobEquipmentSlot extends Slot {
    
    protected final Mob entity;
    protected final InteractionHand hand;
    
    public MobEquipmentSlot(Mob entity, int slotIndex, int x, int y) {
        this(entity, slotIndex, x, y, InteractionHand.MAIN_HAND);
    }
    
    public MobEquipmentSlot(Mob entity, int slotIndex, int x, int y, InteractionHand hand) {
        // Pass a dummy container - we override all methods that use it
        super(new DummyContainer(), slotIndex, x, y);
        this.entity = entity;
        this.hand = hand;
    }
    
    @Override
    public ItemStack getItem() {
        return entity.getItemInHand(hand);
    }
    
    @Override
    public void set(ItemStack stack) {
        entity.setItemInHand(hand, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }
    
    @Override
    public void setChanged() {
        // Entity equipment doesn't need markDirty
    }
    
    @Override
    public int getMaxStackSize() {
        return 64;
    }
    
    @Override
    public ItemStack remove(int amount) {
        ItemStack current = getItem();
        if (current.isEmpty()) {
            return ItemStack.EMPTY;
        }
        
        if (current.getCount() <= amount) {
            ItemStack result = current.copy();
            set(ItemStack.EMPTY);
            return result;
        }
        
        ItemStack result = current.split(amount);
        if (current.isEmpty()) {
            set(ItemStack.EMPTY);
        }
        return result;
    }
    
    @Override
    public boolean hasItem() {
        return !getItem().isEmpty();
    }
    
    @Override
    public boolean mayPlace(ItemStack stack) {
        return true;
    }
    
    /**
     * Dummy container for the slot constructor.
     * All actual operations are overridden to use the entity directly.
     */
    private static class DummyContainer implements Container {
        @Override public int getContainerSize() { return 1; }
        @Override public boolean isEmpty() { return true; }
        @Override public ItemStack getItem(int slot) { return ItemStack.EMPTY; }
        @Override public ItemStack removeItem(int slot, int amount) { return ItemStack.EMPTY; }
        @Override public ItemStack removeItemNoUpdate(int slot) { return ItemStack.EMPTY; }
        @Override public void setItem(int slot, ItemStack stack) {}
        @Override public void setChanged() {}
        @Override public boolean stillValid(net.minecraft.world.entity.player.Player player) { return true; }
        @Override public void clearContent() {}
    }
}




