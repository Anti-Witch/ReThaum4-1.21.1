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
import thaumcraft.common.menu.slot.AspectSlot;
import thaumcraft.common.tiles.essentia.TileSmelter;
import thaumcraft.init.ModBlocks;
import thaumcraft.init.ModMenuTypes;

/**
 * SmelterMenu - Server-side menu for the Alchemical Smelter.
 * 
 * Slot Layout:
 * - 0: Input slot (items to smelt for aspects)
 * - 1: Fuel slot
 * - 2-28: Player inventory (3 rows of 9)
 * - 29-37: Player hotbar (9 slots)
 * 
 * Data:
 * - 0: furnaceCookTime
 * - 1: furnaceBurnTime
 * - 2: currentItemBurnTime
 * - 3: vis stored
 * - 4: smeltTime
 */
public class SmelterMenu extends AbstractContainerMenu {
    
    public static final int INPUT_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int PLAYER_INV_START = 2;
    public static final int PLAYER_INV_END = 28;
    public static final int PLAYER_HOTBAR_START = 29;
    public static final int PLAYER_HOTBAR_END = 37;
    
    private final TileSmelter blockEntity;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    
    // Client constructor
    public SmelterMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, extraData));
    }
    
    // Server constructor
    public SmelterMenu(int containerId, Inventory playerInventory, TileSmelter blockEntity) {
        super(ModMenuTypes.SMELTER.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        
        // Data container for syncing
        this.data = new SimpleContainerData(5);
        addDataSlots(this.data);
        
        // Input slot - accepts items with aspects
        addSlot(new AspectSlot(blockEntity, TileSmelter.SLOT_INPUT, 80, 8));
        
        // Fuel slot
        addSlot(new Slot(blockEntity, TileSmelter.SLOT_FUEL, 80, 48) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return TileSmelter.isItemFuel(stack);
            }
        });
        
        // Player inventory (3 rows)
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
    
    private static TileSmelter getBlockEntity(Inventory playerInventory, FriendlyByteBuf extraData) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(extraData.readBlockPos());
        if (be instanceof TileSmelter smelter) {
            return smelter;
        }
        throw new IllegalStateException("Block entity is not a TileSmelter");
    }
    
    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        // Sync furnace state
        data.set(0, blockEntity.furnaceCookTime);
        data.set(1, blockEntity.furnaceBurnTime);
        data.set(2, blockEntity.currentItemBurnTime);
        data.set(3, blockEntity.vis);
        data.set(4, blockEntity.smeltTime);
    }
    
    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.SMELTER.get());
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            result = stackInSlot.copy();
            
            // From smelter slots to player inventory
            if (index == INPUT_SLOT || index == FUEL_SLOT) {
                if (!moveItemStackTo(stackInSlot, PLAYER_INV_START, PLAYER_HOTBAR_END + 1, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // From player inventory
            else if (index >= PLAYER_INV_START && index <= PLAYER_HOTBAR_END) {
                // Check if it's fuel first
                if (TileSmelter.isItemFuel(stackInSlot)) {
                    // Try fuel slot first, then input slot
                    if (!moveItemStackTo(stackInSlot, FUEL_SLOT, FUEL_SLOT + 1, false)) {
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
                } else {
                    // Try input slot
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
    
    public TileSmelter getBlockEntity() {
        return blockEntity;
    }
    
    public int getCookProgress() {
        return data.get(0);
    }
    
    public int getBurnTime() {
        return data.get(1);
    }
    
    public int getCurrentBurnTime() {
        return data.get(2);
    }
    
    public int getVis() {
        return data.get(3);
    }
    
    public int getSmeltTime() {
        return data.get(4);
    }
    
    public boolean isBurning() {
        return data.get(1) > 0;
    }
    
    public int getCookProgressScaled(int scale) {
        int smeltTime = getSmeltTime();
        if (smeltTime <= 0) smeltTime = 1;
        return getCookProgress() * scale / smeltTime;
    }
    
    public int getBurnTimeScaled(int scale) {
        int currentBurnTime = getCurrentBurnTime();
        if (currentBurnTime == 0) currentBurnTime = 200;
        return getBurnTime() * scale / currentBurnTime;
    }
}




