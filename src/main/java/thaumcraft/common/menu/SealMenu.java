package thaumcraft.common.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.Thaumcraft;
import thaumcraft.api.golems.seals.*;
import thaumcraft.common.golems.seals.SealEntity;
import thaumcraft.common.golems.seals.SealHandler;
import thaumcraft.common.menu.slot.GhostSlot;
import thaumcraft.init.ModMenuTypes;

/**
 * SealMenu - Server-side menu for seal configuration.
 * 
 * Seals can have multiple configuration categories:
 * - CAT_PRIORITY (0): Priority and color settings
 * - CAT_FILTER (1): Item filter slots
 * - CAT_AREA (2): Working area size
 * - CAT_TOGGLES (3): Boolean toggle options
 * - CAT_TAGS (4): Golem tag requirements
 * 
 * This menu handles all seal types through the ISealGui interface.
 */
public class SealMenu extends AbstractContainerMenu {
    
    private final ISealEntity seal;
    private final Player player;
    private final ContainerData data;
    private Container filterContainer;
    private int currentCategory;
    private int[] availableCategories;
    
    // Data indices
    private static final int DATA_PRIORITY = 0;
    private static final int DATA_AREA_X = 1;
    private static final int DATA_AREA_Y = 2;
    private static final int DATA_AREA_Z = 3;
    private static final int DATA_COLOR = 4;
    private static final int DATA_LOCKED = 5;
    private static final int DATA_REDSTONE = 6;
    private static final int DATA_BLACKLIST = 7;
    
    // Client constructor
    public SealMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, readSealFromBuffer(playerInventory.player, extraData));
    }
    
    // Server constructor
    public SealMenu(int containerId, Inventory playerInventory, ISealEntity seal) {
        super(ModMenuTypes.SEAL.get(), containerId);
        this.seal = seal;
        this.player = playerInventory.player;
        
        // Setup data container for syncing
        this.data = new SimpleContainerData(8);
        addDataSlots(this.data);
        
        // Get available categories from seal
        if (seal.getSeal() instanceof ISealGui sealGui) {
            this.availableCategories = sealGui.getGuiCategories();
        } else {
            this.availableCategories = new int[] { ISealGui.CAT_PRIORITY };
        }
        
        this.currentCategory = availableCategories.length > 0 ? availableCategories[0] : 0;
        
        // Setup slots based on category
        setupSlotsForCategory();
        
        // Add player inventory
        addPlayerInventory(playerInventory);
    }
    
    private static ISealEntity readSealFromBuffer(Player player, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        Direction face = Direction.values()[buffer.readByte()];
        SealPos sealPos = new SealPos(pos, face);
        ISealEntity seal = SealHandler.getSealEntity(player.level().dimension(), sealPos);
        if (seal == null) {
            // Create a dummy seal for client - will be synced
            Thaumcraft.LOGGER.warn("Seal not found on client at {}, face {}", pos, face);
            return new SealEntity(player.level(), sealPos, null);
        }
        return seal;
    }
    
    private void setupSlotsForCategory() {
        // Clear existing slots (except player inventory)
        // Note: In 1.20.1, we can't easily clear slots after construction
        // So we set up filter slots only if needed
        
        if (currentCategory == ISealGui.CAT_FILTER && seal.getSeal() instanceof ISealConfigFilter filter) {
            int filterSize = filter.getFilterSize();
            NonNullList<ItemStack> filterInv = filter.getInv();
            
            // Create container for filter slots
            filterContainer = new SimpleContainer(filterSize) {
                @Override
                public void setChanged() {
                    super.setChanged();
                    // Sync filter changes back to seal
                    for (int i = 0; i < getContainerSize(); i++) {
                        filter.setFilterSlot(i, getItem(i));
                    }
                }
            };
            
            // Populate with current filter
            for (int i = 0; i < filterSize; i++) {
                ((SimpleContainer) filterContainer).setItem(i, filterInv.get(i).copy());
            }
            
            // Add filter slots in a grid (max 3x3)
            int cols = Math.min(filterSize, 3);
            int rows = (filterSize + 2) / 3;
            int startX = 88 - (cols * 12);
            int startY = 72 - (rows * 12);
            
            for (int i = 0; i < filterSize; i++) {
                int col = i % 3;
                int row = i / 3;
                addSlot(new GhostSlot(filterContainer, i, startX + col * 24, startY + row * 24));
            }
        }
    }
    
    private void addPlayerInventory(Inventory playerInventory) {
        // Player inventory (3 rows)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 150 + row * 18));
            }
        }
        
        // Player hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 208));
        }
    }
    
    @Override
    public boolean stillValid(Player player) {
        // Check distance to seal
        BlockPos pos = seal.getSealPos().pos;
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }
    
    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        
        // Sync seal data
        data.set(DATA_PRIORITY, seal.getPriority());
        data.set(DATA_AREA_X, seal.getArea().getX());
        data.set(DATA_AREA_Y, seal.getArea().getY());
        data.set(DATA_AREA_Z, seal.getArea().getZ());
        data.set(DATA_COLOR, seal.getColor());
        data.set(DATA_LOCKED, seal.isLocked() ? 1 : 0);
        data.set(DATA_REDSTONE, seal.isRedstoneSensitive() ? 1 : 0);
        
        if (seal.getSeal() instanceof ISealConfigFilter filter) {
            data.set(DATA_BLACKLIST, filter.isBlacklist() ? 1 : 0);
        }
    }
    
    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        return handleButton(buttonId);
    }
    
    /**
     * Handle button clicks from GUI.
     * Button IDs follow the original pattern from SealBaseContainer.
     */
    private boolean handleButton(int buttonId) {
        // Category switch (0-4)
        if (buttonId >= 0 && buttonId < availableCategories.length) {
            currentCategory = availableCategories[buttonId];
            return true;
        }
        
        // Toggle buttons for toggles category
        if (currentCategory == ISealGui.CAT_TOGGLES && seal.getSeal() instanceof ISealConfigToggles toggles) {
            ISealConfigToggles.SealToggle[] sealToggles = toggles.getToggles();
            if (buttonId >= 30 && buttonId < 30 + sealToggles.length) {
                toggles.setToggle(buttonId - 30, true);
                return true;
            }
            if (buttonId >= 60 && buttonId < 60 + sealToggles.length) {
                toggles.setToggle(buttonId - 60, false);
                return true;
            }
        }
        
        // Lock button (25=lock, 26=unlock)
        if (buttonId == 25) {
            seal.setLocked(true);
            return true;
        }
        if (buttonId == 26) {
            seal.setLocked(false);
            return true;
        }
        
        // Redstone sensitivity (27=on, 28=off)
        if (buttonId == 27) {
            seal.setRedstoneSensitive(true);
            return true;
        }
        if (buttonId == 28) {
            seal.setRedstoneSensitive(false);
            return true;
        }
        
        // Blacklist/whitelist (20=blacklist, 21=whitelist)
        if (seal.getSeal() instanceof ISealConfigFilter filter) {
            if (buttonId == 20) {
                filter.setBlacklist(true);
                return true;
            }
            if (buttonId == 21) {
                filter.setBlacklist(false);
                return true;
            }
        }
        
        // Priority buttons (80=decrease, 81=increase)
        if (buttonId == 80 && seal.getPriority() > -5) {
            seal.setPriority((byte) (seal.getPriority() - 1));
            return true;
        }
        if (buttonId == 81 && seal.getPriority() < 5) {
            seal.setPriority((byte) (seal.getPriority() + 1));
            return true;
        }
        
        // Color buttons (82=decrease, 83=increase)
        if (buttonId == 82 && seal.getColor() > 0) {
            seal.setColor((byte) (seal.getColor() - 1));
            return true;
        }
        if (buttonId == 83 && seal.getColor() < 16) {
            seal.setColor((byte) (seal.getColor() + 1));
            return true;
        }
        
        // Area buttons (90-95)
        if (seal.getSeal() instanceof ISealConfigArea) {
            BlockPos area = seal.getArea();
            switch (buttonId) {
                case 90 -> { if (area.getY() > 1) seal.setArea(area.offset(0, -1, 0)); return true; }
                case 91 -> { if (area.getY() < 8) seal.setArea(area.offset(0, 1, 0)); return true; }
                case 92 -> { if (area.getX() > 1) seal.setArea(area.offset(-1, 0, 0)); return true; }
                case 93 -> { if (area.getX() < 8) seal.setArea(area.offset(1, 0, 0)); return true; }
                case 94 -> { if (area.getZ() > 1) seal.setArea(area.offset(0, 0, -1)); return true; }
                case 95 -> { if (area.getZ() < 8) seal.setArea(area.offset(0, 0, 1)); return true; }
            }
        }
        
        return false;
    }
    
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // Handle ghost slot clicks specially
        if (slotId >= 0 && slotId < slots.size()) {
            Slot slot = slots.get(slotId);
            if (slot instanceof GhostSlot ghostSlot) {
                handleGhostSlotClick(ghostSlot, button, clickType, player);
                return;
            }
        }
        super.clicked(slotId, button, clickType, player);
    }
    
    private void handleGhostSlotClick(GhostSlot slot, int button, ClickType clickType, Player player) {
        ItemStack carried = getCarried();
        
        if (button == 1) {
            // Right click - clear slot
            slot.clearGhost();
        } else if (!carried.isEmpty()) {
            // Left click with item - set filter
            ItemStack copy = carried.copy();
            copy.setCount(1);
            slot.setGhostItem(copy);
        } else if (slot.hasItem()) {
            // Left click empty - clear
            slot.clearGhost();
        }
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // No shift-click transfer for seal menu
        return ItemStack.EMPTY;
    }
    
    // ==================== Accessors ====================
    
    public ISealEntity getSeal() {
        return seal;
    }
    
    public int getCurrentCategory() {
        return currentCategory;
    }
    
    public int[] getAvailableCategories() {
        return availableCategories;
    }
    
    public int getPriority() {
        return data.get(DATA_PRIORITY);
    }
    
    public BlockPos getArea() {
        return new BlockPos(data.get(DATA_AREA_X), data.get(DATA_AREA_Y), data.get(DATA_AREA_Z));
    }
    
    public int getColor() {
        return data.get(DATA_COLOR);
    }
    
    public boolean isLocked() {
        return data.get(DATA_LOCKED) != 0;
    }
    
    public boolean isRedstoneSensitive() {
        return data.get(DATA_REDSTONE) != 0;
    }
    
    public boolean isBlacklist() {
        return data.get(DATA_BLACKLIST) != 0;
    }
}




