package thaumcraft.common.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.common.tiles.devices.TileHungryChest;
import thaumcraft.init.ModBlocks;
import thaumcraft.init.ModMenuTypes;

/**
 * HungryChestMenu - Server-side menu for the Hungry Chest.
 * Standard 27-slot chest layout (3 rows of 9 slots).
 * 
 * Slot Layout:
 * - 0-26: Chest inventory (3 rows of 9)
 * - 27-53: Player inventory (3 rows of 9)
 * - 54-62: Player hotbar (9 slots)
 */
public class HungryChestMenu extends AbstractContainerMenu {
    
    public static final int CHEST_SIZE = 27;
    public static final int CHEST_ROWS = 3;
    
    public static final int CHEST_START = 0;
    public static final int CHEST_END = 26;
    public static final int PLAYER_INV_START = 27;
    public static final int PLAYER_INV_END = 53;
    public static final int PLAYER_HOTBAR_START = 54;
    public static final int PLAYER_HOTBAR_END = 62;
    
    private final TileHungryChest blockEntity;
    private final ContainerLevelAccess access;
    
    // Client constructor
    public HungryChestMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, extraData));
    }
    
    // Server constructor
    public HungryChestMenu(int containerId, Inventory playerInventory, TileHungryChest blockEntity) {
        super(ModMenuTypes.HUNGRY_CHEST.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        
        // Notify chest that a player opened it (for lid animation)
        blockEntity.startOpen(playerInventory.player);
        
        // Chest inventory (3 rows of 9)
        for (int row = 0; row < CHEST_ROWS; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(blockEntity, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }
        
        // Player inventory (3 rows of 9)
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
    
    private static TileHungryChest getBlockEntity(Inventory playerInventory, FriendlyByteBuf extraData) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(extraData.readBlockPos());
        if (be instanceof TileHungryChest chest) {
            return chest;
        }
        throw new IllegalStateException("Block entity is not a TileHungryChest");
    }
    
    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.HUNGRY_CHEST.get());
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            result = stackInSlot.copy();
            
            // From chest to player inventory
            if (index < CHEST_SIZE) {
                if (!moveItemStackTo(stackInSlot, PLAYER_INV_START, PLAYER_HOTBAR_END + 1, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // From player inventory to chest
            else if (index >= PLAYER_INV_START && index <= PLAYER_HOTBAR_END) {
                if (!moveItemStackTo(stackInSlot, CHEST_START, CHEST_END + 1, false)) {
                    // Move between inventory and hotbar
                    if (index < PLAYER_HOTBAR_START) {
                        if (!moveItemStackTo(stackInSlot, PLAYER_HOTBAR_START, PLAYER_HOTBAR_END + 1, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else {
                        if (!moveItemStackTo(stackInSlot, PLAYER_INV_START, PLAYER_HOTBAR_START, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
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
        // Notify chest that player closed it (for lid animation)
        blockEntity.stopOpen(player);
    }
    
    // ==================== Accessors ====================
    
    public TileHungryChest getBlockEntity() {
        return blockEntity;
    }
}




