package thaumcraft.common.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.common.tiles.crafting.TileThaumatorium;
import thaumcraft.init.ModBlocks;
import thaumcraft.init.ModMenuTypes;

/**
 * ThaumatoriumMenu - Server-side menu for the Thaumatorium (automated alchemy machine).
 * 
 * Slot Layout:
 * - 0: Input/catalyst slot
 * - 1-27: Player inventory (3 rows of 9)
 * - 28-36: Player hotbar (9 slots)
 * 
 * Data:
 * - 0: Crafting progress (0-100)
 * - 1: Is crafting (0 or 1)
 */
public class ThaumatoriumMenu extends AbstractContainerMenu {
    
    public static final int INPUT_SLOT = 0;
    public static final int PLAYER_INV_START = 1;
    public static final int PLAYER_INV_END = 27;
    public static final int PLAYER_HOTBAR_START = 28;
    public static final int PLAYER_HOTBAR_END = 36;
    
    private final TileThaumatorium blockEntity;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final Player player;
    
    // Client constructor
    public ThaumatoriumMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, extraData));
    }
    
    // Server constructor
    public ThaumatoriumMenu(int containerId, Inventory playerInventory, TileThaumatorium blockEntity) {
        super(ModMenuTypes.THAUMATORIUM.get(), containerId);
        this.blockEntity = blockEntity;
        this.player = playerInventory.player;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        
        // Data container for syncing
        this.data = new SimpleContainerData(2);
        addDataSlots(this.data);
        
        // Input slot (catalyst)
        addSlot(new Slot(blockEntity, TileThaumatorium.INPUT_SLOT, 55, 24));
        
        // Player inventory (3 rows)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 135 + row * 18));
            }
        }
        
        // Player hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 193));
        }
    }
    
    private static TileThaumatorium getBlockEntity(Inventory playerInventory, FriendlyByteBuf extraData) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(extraData.readBlockPos());
        if (be instanceof TileThaumatorium thaumatorium) {
            return thaumatorium;
        }
        throw new IllegalStateException("Block entity is not a TileThaumatorium");
    }
    
    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        // Sync crafting state
        data.set(0, blockEntity.craftingProgress);
        data.set(1, blockEntity.crafting ? 1 : 0);
    }
    
    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.THAUMATORIUM.get());
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            result = stackInSlot.copy();
            
            // From input slot to player inventory
            if (index == INPUT_SLOT) {
                if (!moveItemStackTo(stackInSlot, PLAYER_INV_START, PLAYER_HOTBAR_END + 1, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // From player inventory
            else if (index >= PLAYER_INV_START && index <= PLAYER_HOTBAR_END) {
                // Try to move to input slot
                if (!moveItemStackTo(stackInSlot, INPUT_SLOT, INPUT_SLOT + 1, false)) {
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
    
    // ==================== Accessors ====================
    
    public TileThaumatorium getBlockEntity() {
        return blockEntity;
    }
    
    public int getCraftingProgress() {
        return data.get(0);
    }
    
    public boolean isCrafting() {
        return data.get(1) != 0;
    }
}




