package thaumcraft.client.gui.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.init.ModSounds;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.misc.PacketLogisticsRequestToServer;
import thaumcraft.common.lib.network.misc.PacketMiscStringToServer;
import thaumcraft.common.menu.LogisticsMenu;

/**
 * LogisticsScreen - Client-side GUI for the logistics request system.
 * 
 * Displays all items available from SealProvide seals within range and
 * allows players to request delivery of specific items.
 * 
 * Features:
 * - 9x9 grid of available items
 * - Scrollbar for large inventories
 * - Search box for filtering
 * - Amount selector for requesting specific quantities
 * - Request button to initiate delivery
 * 
 * Ported from 1.12.2 GuiLogistics.
 */
@OnlyIn(Dist.CLIENT)
public class LogisticsScreen extends AbstractContainerScreen<LogisticsMenu> {
    
    private static final ResourceLocation TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_logistics.png");
    
    // Currently selected slot index (-1 = none)
    private int selectedSlot = -1;
    private ItemStack selectedStack = ItemStack.EMPTY;
    
    // Scroll state
    private int lastScrollPos = 0;
    private float scrollbarValue = 0;
    
    // Request amount
    private int stackSize = 1;
    private int lastStackSize = 1;
    
    // Refresh timer
    private long lastUpdateTime = 0;
    
    // UI components
    private EditBox searchField;
    private Button requestButton;
    private Button decreaseButton;
    private Button increaseButton;
    
    public LogisticsScreen(LogisticsMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 215;
        this.imageHeight = 215;
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Decrease button (-)
        decreaseButton = Button.builder(Component.literal("-"), btn -> adjustStackSize(-1))
                .bounds(leftPos + 13, topPos + 195, 10, 10)
                .build();
        addRenderableWidget(decreaseButton);
        
        // Increase button (+)
        increaseButton = Button.builder(Component.literal("+"), btn -> adjustStackSize(1))
                .bounds(leftPos + 57, topPos + 195, 10, 10)
                .build();
        addRenderableWidget(increaseButton);
        
        // Request button
        requestButton = Button.builder(Component.translatable("tc.logistics.request"), btn -> requestItems())
                .bounds(leftPos + 116, topPos + 200, 40, 13)
                .build();
        addRenderableWidget(requestButton);
        
        // Scroll up button
        addRenderableWidget(Button.builder(Component.literal("в–І"), btn -> scroll(-1))
                .bounds(leftPos + 195, topPos + 16, 10, 10)
                .build());
        
        // Scroll down button
        addRenderableWidget(Button.builder(Component.literal("в–ј"), btn -> scroll(1))
                .bounds(leftPos + 195, topPos + 180, 10, 10)
                .build());
        
        // Search field
        searchField = new EditBox(font, leftPos + 143, topPos + 196, 55, font.lineHeight, Component.empty());
        searchField.setMaxLength(10);
        searchField.setBordered(true);
        searchField.setVisible(true);
        searchField.setTextColor(16777215);
        searchField.setResponder(this::onSearchTextChanged);
        addRenderableWidget(searchField);
        
        // Hide buttons when no selection
        updateButtonVisibility();
    }
    
    private void updateButtonVisibility() {
        boolean hasSelection = selectedSlot >= 0 && !selectedStack.isEmpty();
        decreaseButton.visible = hasSelection;
        increaseButton.visible = hasSelection;
        requestButton.visible = hasSelection;
    }
    
    private void adjustStackSize(int delta) {
        if (selectedSlot < 0 || selectedStack.isEmpty()) return;
        
        stackSize += delta;
        stackSize = Math.max(1, Math.min(stackSize, selectedStack.getCount()));
    }
    
    private void scroll(int direction) {
        if (minecraft != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, direction > 0 ? 0 : 1);
        }
    }
    
    private void onSearchTextChanged(String text) {
        // Send search text to server
        PacketHandler.sendToServer(new PacketMiscStringToServer(0, text));
    }
    
    private void requestItems() {
        if (selectedSlot < 0 || selectedStack.isEmpty()) return;
        
        // Send request to server
        PacketHandler.sendToServer(new PacketLogisticsRequestToServer(
                menu.getTargetPos(),
                menu.getTargetSide(),
                selectedStack.copy(),
                stackSize
        ));
        
        // Play sound
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.CLACK.get(), 1.0f));
        }
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
    
    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        
        // Draw main background
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        
        // Draw selection highlight
        if (selectedSlot >= 0 && selectedSlot < 81) {
            int selX = selectedSlot % 9;
            int selY = selectedSlot / 9;
            graphics.blit(TEXTURE, leftPos + 17 + selX * 19, topPos + 17 + selY * 19, 222, 46, 20, 20);
        }
    }
    
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Periodic refresh
        long currentTime = System.currentTimeMillis();
        if (currentTime > lastUpdateTime) {
            lastUpdateTime = currentTime + 1000L;
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 22);
            }
        }
        
        // Update scroll position from server data
        int serverStart = menu.getStart();
        if (serverStart != lastScrollPos) {
            lastScrollPos = serverStart;
            scrollbarValue = serverStart;
        }
        
        // Validate selection
        if (selectedSlot >= 0) {
            Slot slot = menu.getSlot(selectedSlot);
            if (slot == null || !slot.hasItem()) {
                selectedSlot = -1;
                selectedStack = ItemStack.EMPTY;
            } else if (!ItemStack.isSameItemSameComponents(selectedStack, slot.getItem())) {
                // Item changed, try to find it again
                selectedSlot = -1;
                for (int i = 0; i < 81; i++) {
                    Slot s = menu.getSlot(i);
                    if (s != null && ItemStack.isSameItemSameComponents(selectedStack, s.getItem())) {
                        selectedSlot = i;
                        break;
                    }
                }
                if (selectedSlot < 0) {
                    selectedStack = ItemStack.EMPTY;
                }
            }
        }
        
        updateButtonVisibility();
        
        // Draw stack size if selected
        if (selectedSlot >= 0 && !selectedStack.isEmpty()) {
            String sizeText = String.valueOf(stackSize);
            int textWidth = font.width(sizeText);
            graphics.drawString(font, sizeText, 83 - textWidth / 2, 196, 0x333333, false);
        }
        
        // Draw search hint
        if (!searchField.isFocused() && searchField.getValue().isEmpty()) {
            graphics.drawString(font, Component.translatable("tc.logistics.search"), 
                    143 - leftPos, 197 - topPos, 0x222222, false);
        }
    }
    
    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
        // Handle slot click for selection
        if (slot != null && slotId >= 0 && slotId < 81 && slot.hasItem()) {
            if (minecraft != null) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.CLACK.get(), 0.66f));
            }
            selectedSlot = slotId;
            selectedStack = slot.getItem().copy();
            stackSize = 1;
            lastStackSize = 1;
        }
        
        // Don't call super - we don't want normal slot interactions
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY < 0) {
            scroll(1); // Scroll down
        } else if (scrollY > 0) {
            scroll(-1); // Scroll up
        }
        return true;
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchField.isFocused()) {
            return searchField.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchField.isFocused()) {
            return searchField.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }
}




