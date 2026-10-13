package thaumcraft.common.tiles.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.common.tiles.TileThaumcraft;
import thaumcraft.init.ModBlockEntities;

import java.util.List;

/**
 * Hungry chest tile entity - automatically collects nearby dropped items.
 * Has 27 slots like a regular chest.
 */
public class TileHungryChest extends TileThaumcraft implements Container {

    public static final int SIZE = 27;
    public static final double PICKUP_RANGE = 2.5;

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    
    // Animation state
    public float lidAngle = 0.0f;
    public float prevLidAngle = 0.0f;
    public int numPlayersUsing = 0;
    
    private int tickCount = 0;

    public TileHungryChest(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public TileHungryChest(BlockPos pos, BlockState state) {
        this(ModBlockEntities.HUNGRY_CHEST.get(), pos, state);
    }

    // ==================== NBT ====================

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, items, provider);
    }

    @Override
    public void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, provider);
    }

    // ==================== Tick ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileHungryChest tile) {
        tile.tickCount++;
        
        // Try to pick up nearby items every few ticks
        if (tile.tickCount % 10 == 0) {
            tile.pickupNearbyItems();
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileHungryChest tile) {
        tile.prevLidAngle = tile.lidAngle;
        
        // Animate lid
        if (tile.numPlayersUsing > 0 && tile.lidAngle < 1.0f) {
            tile.lidAngle += 0.1f;
            if (tile.lidAngle > 1.0f) tile.lidAngle = 1.0f;
        }
        if (tile.numPlayersUsing == 0 && tile.lidAngle > 0.0f) {
            tile.lidAngle -= 0.1f;
            if (tile.lidAngle < 0.0f) tile.lidAngle = 0.0f;
        }
    }

    /**
     * Pick up items dropped nearby.
     */
    private void pickupNearbyItems() {
        if (level == null) return;

        AABB pickupBox = new AABB(
                worldPosition.getX() - PICKUP_RANGE,
                worldPosition.getY() - PICKUP_RANGE,
                worldPosition.getZ() - PICKUP_RANGE,
                worldPosition.getX() + 1 + PICKUP_RANGE,
                worldPosition.getY() + 1 + PICKUP_RANGE,
                worldPosition.getZ() + 1 + PICKUP_RANGE
        );

        List<ItemEntity> itemEntities = level.getEntitiesOfClass(ItemEntity.class, pickupBox);
        
        for (ItemEntity itemEntity : itemEntities) {
            if (itemEntity.isRemoved() || itemEntity.hasPickUpDelay()) continue;
            
            ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty()) continue;

            // Try to insert item
            ItemStack remaining = insertItem(stack);
            
            if (remaining.isEmpty()) {
                // Fully consumed
                itemEntity.discard();
                playPickupSound();
            } else if (remaining.getCount() < stack.getCount()) {
                // Partially consumed
                itemEntity.setItem(remaining);
                playPickupSound();
            }
        }
    }

    /**
     * Try to insert an item into the chest.
     * @return remaining items that couldn't be inserted
     */
    public ItemStack insertItem(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;

        ItemStack toInsert = stack.copy();

        // First, try to stack with existing items
        for (int i = 0; i < SIZE; i++) {
            ItemStack slot = items.get(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, toInsert)) {
                int space = slot.getMaxStackSize() - slot.getCount();
                if (space > 0) {
                    int toAdd = Math.min(space, toInsert.getCount());
                    slot.grow(toAdd);
                    toInsert.shrink(toAdd);
                    if (toInsert.isEmpty()) {
                        setChanged();
                        return ItemStack.EMPTY;
                    }
                }
            }
        }

        // Then, try to place in empty slots
        for (int i = 0; i < SIZE; i++) {
            if (items.get(i).isEmpty()) {
                items.set(i, toInsert);
                setChanged();
                return ItemStack.EMPTY;
            }
        }

        // Return what couldn't be inserted
        return toInsert;
    }

    private void playPickupSound() {
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.2f, 
                    0.8f + level.random.nextFloat() * 0.4f);
        }
    }

    // ==================== Container ====================

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) return false;
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

    // ==================== Open/Close ====================

    @Override
    public void startOpen(Player player) {
        if (!player.isSpectator()) {
            if (numPlayersUsing < 0) numPlayersUsing = 0;
            numPlayersUsing++;
            level.blockEvent(worldPosition, getBlockState().getBlock(), 1, numPlayersUsing);
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
            level.updateNeighborsAt(worldPosition.below(), getBlockState().getBlock());
        }
    }

    @Override
    public void stopOpen(Player player) {
        if (!player.isSpectator()) {
            numPlayersUsing--;
            level.blockEvent(worldPosition, getBlockState().getBlock(), 1, numPlayersUsing);
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
            level.updateNeighborsAt(worldPosition.below(), getBlockState().getBlock());
        }
    }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == 1) {
            numPlayersUsing = param;
            return true;
        }
        return super.triggerEvent(id, param);
    }

    // ==================== Drop Items ====================

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




