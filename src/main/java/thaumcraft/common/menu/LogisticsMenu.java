package thaumcraft.common.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import thaumcraft.api.golems.GolemHelper;
import thaumcraft.common.golems.seals.SealEntity;
import thaumcraft.common.golems.seals.SealHandler;
import thaumcraft.common.golems.seals.SealProvide;
import thaumcraft.common.menu.slot.GhostSlot;
import thaumcraft.init.ModMenuTypes;

import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * LogisticsMenu - Server-side menu for the logistics request system.
 * 
 * This menu displays all items available from SealProvide seals within range
 * of the player. Players can select items and request delivery through the
 * logistics network.
 * 
 * Features:
 * - Aggregates items from all nearby SealProvide seals
 * - 81-slot ghost display (9x9 grid)
 * - Scrollable item list
 * - Search functionality
 * - Request items with custom amounts
 * 
 * Ported from 1.12.2 ContainerLogistics.
 */
public class LogisticsMenu extends AbstractContainerMenu {
    
    private final Level level;
    private final Player player;
    private final BlockPos targetPos;
    private final Direction targetSide;
    
    // The container for displaying items (81 slots = 9 columns x 9 rows)
    private final Container displayContainer;
    
    // Map of all available items, keyed by unique identifier
    private TreeMap<String, ItemStack> items = new TreeMap<>();
    
    // Synchronization data
    private final ContainerData data;
    private int start = 0; // Current scroll position
    private int end = 0;   // Max scroll position
    private int lastStart = 0;
    private int lastEnd = 0;
    private int lastTotal = 0;
    private String searchText = "";
    
    private static final int DATA_START = 0;
    private static final int DATA_END = 1;
    
    // Client constructor - extraData may be empty for logistics
    public LogisticsMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, 
             extraData.isReadable() ? extraData.readBlockPos() : playerInventory.player.blockPosition(), 
             extraData.isReadable() ? Direction.values()[extraData.readByte()] : Direction.UP);
    }
    
    // Server constructor
    public LogisticsMenu(int containerId, Inventory playerInventory, BlockPos pos, Direction side) {
        super(ModMenuTypes.LOGISTICS.get(), containerId);
        this.level = playerInventory.player.level();
        this.player = playerInventory.player;
        this.targetPos = pos;
        this.targetSide = side;
        
        // Create 81-slot display container
        this.displayContainer = new SimpleContainer(81) {
            @Override
            public int getMaxStackSize() {
                return Integer.MAX_VALUE; // Allow large stack display
            }
        };
        
        // Setup data container
        this.data = new SimpleContainerData(2);
        addDataSlots(data);
        
        // Add display slots (9x9 grid)
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = row * 9 + col;
                addSlot(new GhostSlot(displayContainer, slotIndex, 19 + col * 19, 19 + row * 19, Integer.MAX_VALUE));
            }
        }
        
        // Initial item refresh
        refreshItemList(true);
    }
    
    /**
     * Refreshes the item list from all nearby SealProvide seals.
     * @param full If true, do a complete refresh; if false, just update display
     */
    public void refreshItemList(boolean full) {
        int newTotal = lastTotal;
        TreeMap<String, ItemStack> tempItems = new TreeMap<>();
        
        if (full) {
            newTotal = 0;
            CopyOnWriteArrayList<SealEntity> seals = SealHandler.getSealsInRange(level, player.blockPosition(), 32);
            
            for (SealEntity seal : seals) {
                if (seal.getSeal() instanceof SealProvide sealProvide && 
                    seal.getOwner().equals(player.getUUID().toString())) {
                    
                    IItemHandler handler = getItemHandler(level, seal.getSealPos().pos, seal.getSealPos().face);
                    if (handler == null) continue;
                    
                    for (int slot = 0; slot < handler.getSlots(); slot++) {
                        ItemStack stack = handler.getStackInSlot(slot).copy();
                        if (stack.isEmpty()) continue;
                        
                        // Check if matches seal filter
                        if (sealProvide.matchesFilters(stack)) {
                            // Apply search filter
                            if (!searchText.isEmpty() && 
                                !stack.getHoverName().getString().toLowerCase().contains(searchText.toLowerCase())) {
                                continue;
                            }
                            
                            // Create unique key for item type
                            String key = stack.getHoverName().getString() + 
                                        "_" + stack.getDamageValue() + 
                                        "_" + ItemStack.hashItemAndComponents(stack);
                            
                            if (tempItems.containsKey(key)) {
                                ItemStack existing = tempItems.get(key);
                                existing.grow(stack.getCount());
                            } else {
                                tempItems.put(key, stack);
                            }
                            newTotal += stack.getCount();
                        }
                    }
                }
            }
        }
        
        if (lastTotal != newTotal || start != lastStart) {
            lastTotal = newTotal;
            if (full) {
                items = tempItems;
            }
            
            // Clear display container
            for (int i = 0; i < displayContainer.getContainerSize(); i++) {
                displayContainer.setItem(i, ItemStack.EMPTY);
            }
            
            // Populate display from scroll position
            int itemIndex = 0;
            int displayIndex = 0;
            for (String key : items.keySet()) {
                itemIndex++;
                if (itemIndex <= start * 9) continue;
                
                displayContainer.setItem(displayIndex, items.get(key).copy());
                displayIndex++;
                if (displayIndex >= displayContainer.getContainerSize()) break;
            }
            
            // Calculate max scroll
            end = Math.max(0, items.size() / 9 - 8);
        }
    }
    
    private IItemHandler getItemHandler(Level level, BlockPos pos, Direction face) {
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, face);
    }
    
    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        switch (buttonId) {
            case 0 -> {
                // Scroll down
                if (start < items.size() / 9 - 8) {
                    start++;
                    refreshItemList(false);
                }
                return true;
            }
            case 1 -> {
                // Scroll up
                if (start > 0) {
                    start--;
                    refreshItemList(false);
                }
                return true;
            }
            case 22 -> {
                // Refresh
                refreshItemList(true);
                return true;
            }
            default -> {
                // Scroll to position (100+)
                if (buttonId >= 100) {
                    int targetScroll = buttonId - 100;
                    if (targetScroll >= 0 && targetScroll <= Math.max(0, items.size() / 9 - 8)) {
                        start = targetScroll;
                        refreshItemList(false);
                    }
                    return true;
                }
            }
        }
        return false;
    }
    
    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        
        data.set(DATA_START, start);
        data.set(DATA_END, end);
        
        lastStart = start;
        lastEnd = end;
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // No shift-click transfer for logistics display
        return ItemStack.EMPTY;
    }
    
    @Override
    public boolean stillValid(Player player) {
        return true;
    }
    
    /**
     * Set search text and refresh
     */
    public void setSearchText(String text) {
        this.searchText = text;
        this.start = 0;
        refreshItemList(true);
    }
    
    /**
     * Request delivery of an item.
     * Called when player clicks the request button.
     * 
     * @param stack The item type to request
     * @param amount The amount to request
     */
    public void requestItem(ItemStack stack, int amount) {
        if (stack.isEmpty() || amount <= 0) return;
        
        // Find a suitable SealProvide that has this item
        CopyOnWriteArrayList<SealEntity> seals = SealHandler.getSealsInRange(level, player.blockPosition(), 32);
        
        for (SealEntity seal : seals) {
            if (seal.getSeal() instanceof SealProvide sealProvide && 
                seal.getOwner().equals(player.getUUID().toString())) {
                
                IItemHandler handler = getItemHandler(level, seal.getSealPos().pos, seal.getSealPos().face);
                if (handler == null) continue;
                
                for (int slot = 0; slot < handler.getSlots(); slot++) {
                    ItemStack slotStack = handler.getStackInSlot(slot);
                    if (ItemStack.isSameItemSameComponents(slotStack, stack)) {
                        // Create provision request to deliver to player
                        GolemHelper.requestProvisioning(
                            level, 
                            player, 
                            stack.copyWithCount(Math.min(amount, slotStack.getCount()))
                        );
                        return;
                    }
                }
            }
        }
    }
    
    // Accessors for client sync
    public int getStart() { return data.get(DATA_START); }
    public int getEnd() { return data.get(DATA_END); }
    public Level getLevel() { return level; }
    public Player getPlayer() { return player; }
    public BlockPos getTargetPos() { return targetPos; }
    public Direction getTargetSide() { return targetSide; }
}




