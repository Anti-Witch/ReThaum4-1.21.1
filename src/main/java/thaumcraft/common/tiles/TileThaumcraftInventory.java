package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Base class for Thaumcraft tile entities that have an inventory.
 */
public abstract class TileThaumcraftInventory extends TileThaumcraft implements Container {

    protected NonNullList<ItemStack> items;
    protected int[] syncedSlots = new int[0];

    public TileThaumcraftInventory(BlockEntityType<?> type, BlockPos pos, BlockState state, int inventorySize) {
        super(type, pos, state);
        this.items = NonNullList.withSize(inventorySize, ItemStack.EMPTY);
    }

    // ==================== Container Implementation ====================

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    // ==================== NBT Serialization ====================

    @Override
    protected void writeSyncNBT(CompoundTag tag) {
        super.writeSyncNBT(tag);
        ContainerHelper.saveAllItems(tag, items, getRegistryProvider());
    }

    @Override
    protected void readSyncNBT(CompoundTag tag) {
        super.readSyncNBT(tag);
        ContainerHelper.loadAllItems(tag, items, getRegistryProvider());
    }

    // ==================== Drop Items ====================

    /**
     * Drop all inventory contents into the world.
     */
    public void dropContents() {
        if (level != null && !level.isClientSide) {
            for (ItemStack stack : items) {
                if (!stack.isEmpty()) {
                    net.minecraft.world.Containers.dropItemStack(level, 
                            worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
                }
            }
            clearContent();
        }
    }
}




