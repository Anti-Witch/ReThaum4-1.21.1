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
import thaumcraft.common.menu.slot.OutputSlot;
import thaumcraft.common.tiles.crafting.TileGolemBuilder;
import thaumcraft.init.ModBlocks;
import thaumcraft.init.ModMenuTypes;

/**
 * GolemBuilderMenu - Server-side menu for the Golem Builder block.
 * 
 * Slots:
 * - 0: Output slot (crafted golem placer item)
 * - 1-27: Player inventory (3 rows of 9)
 * - 28-36: Player hotbar (9 slots)
 * 
 * Data:
 * - 0: Current vis cost
 * - 1: Maximum vis cost (when all parts selected)
 */
public class GolemBuilderMenu extends AbstractContainerMenu {
    
    private final TileGolemBuilder blockEntity;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    
    // Client constructor (from network)
    public GolemBuilderMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, extraData));
    }
    
    // Server constructor
    public GolemBuilderMenu(int containerId, Inventory playerInventory, TileGolemBuilder blockEntity) {
        super(ModMenuTypes.GOLEM_BUILDER.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        
        // Create data container for syncing cost values
        this.data = new SimpleContainerData(2);
        addDataSlots(this.data);
        
        // Output slot
        addSlot(new OutputSlot(blockEntity, 0, 160, 104));
        
        // Player inventory (3 rows)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 24 + col * 18, 142 + row * 18));
            }
        }
        
        // Player hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 24 + col * 18, 200));
        }
    }
    
    private static TileGolemBuilder getBlockEntity(Inventory playerInventory, FriendlyByteBuf extraData) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(extraData.readBlockPos());
        if (be instanceof TileGolemBuilder tileGolemBuilder) {
            return tileGolemBuilder;
        }
        throw new IllegalStateException("Block entity is not a TileGolemBuilder at " + extraData.readBlockPos());
    }
    
    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.GOLEM_BUILDER.get());
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(slotIndex);
        
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            result = stackInSlot.copy();
            
            // Output slot (0)
            if (slotIndex == 0) {
                // Move from output to player inventory
                if (!moveItemStackTo(stackInSlot, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stackInSlot, result);
            } else {
                // Move from player inventory - nothing to move to output
                // This menu doesn't have input slots
                return ItemStack.EMPTY;
            }
            
            if (stackInSlot.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        
        return result;
    }
    
    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        // Sync cost data
        data.set(0, blockEntity.cost);
        data.set(1, blockEntity.maxCost);
    }
    
    // ==================== Accessors ====================
    
    public TileGolemBuilder getBlockEntity() {
        return blockEntity;
    }
    
    public int getCost() {
        return data.get(0);
    }
    
    public int getMaxCost() {
        return data.get(1);
    }
    
    /**
     * Handle button clicks from the GUI.
     * Button IDs:
     * - 0-3: Material selection (cycle)
     * - 4-7: Head selection (cycle)
     * - 8-11: Arms selection (cycle)
     * - 12-15: Legs selection (cycle)
     * - 16-19: Addon selection (cycle)
     * - 99: Craft button
     */
    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId == 99) {
            // Craft button pressed
            blockEntity.tryCraft(player);
            return true;
        }
        
        // Part selection buttons
        int partType = buttonId / 4;
        int direction = (buttonId % 4 == 0) ? 1 : -1; // Simple cycle
        
        switch (partType) {
            case 0 -> blockEntity.cycleMaterial(direction);
            case 1 -> blockEntity.cycleHead(direction);
            case 2 -> blockEntity.cycleArms(direction);
            case 3 -> blockEntity.cycleLegs(direction);
            case 4 -> blockEntity.cycleAddon(direction);
        }
        
        return true;
    }
}




