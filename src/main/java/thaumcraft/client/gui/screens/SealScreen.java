package thaumcraft.client.gui.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.seals.*;
import thaumcraft.common.menu.SealMenu;
import thaumcraft.common.menu.slot.GhostSlot;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * SealScreen - Client-side GUI for seal configuration.
 * 
 * Allows configuration of seals through multiple category tabs:
 * - Priority: Priority and color settings
 * - Filter: Item filter slots  
 * - Area: Working area size
 * - Toggles: Boolean toggle options
 * - Tags: Required/forbidden golem traits
 * 
 * Ported from 1.12.2 SealBaseGUI.
 */
@OnlyIn(Dist.CLIENT)
public class SealScreen extends AbstractContainerScreen<SealMenu> {
    
    private static final ResourceLocation TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_base.png");
    
    private int middleX;
    private int middleY;
    private int currentCategory;
    private int[] categories;
    
    // Category buttons
    private final List<CategoryButton> categoryButtons = new ArrayList<>();
    
    public SealScreen(SealMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 232;
        this.middleX = imageWidth / 2;
        this.middleY = (imageHeight - 72) / 2 - 8;
        this.categories = menu.getAvailableCategories();
        this.currentCategory = menu.getCurrentCategory();
    }
    
    @Override
    protected void init() {
        super.init();
        setupCategories();
    }
    
    private void setupCategories() {
        clearWidgets();
        categoryButtons.clear();
        
        // Setup category buttons in arc
        int c = 0;
        float slice = 60.0f / categories.length;
        float start = -180.0f + (categories.length - 1) * slice / 2.0f;
        slice = Mth.clamp(slice, 12.0f, 24.0f);
        
        for (int cat : categories) {
            if (categories.length > 1) {
                int xx = (int)(Mth.cos((start - c * slice) / 180.0f * (float)Math.PI) * 86.0f);
                int yy = (int)(Mth.sin((start - c * slice) / 180.0f * (float)Math.PI) * 86.0f);
                
                final int catIndex = c;
                CategoryButton btn = new CategoryButton(
                    leftPos + middleX + xx - 8, 
                    topPos + middleY + yy - 8, 
                    cat,
                    currentCategory == cat,
                    b -> switchCategory(catIndex)
                );
                categoryButtons.add(btn);
                addRenderableWidget(btn);
            }
            c++;
        }
        
        // Add redstone button
        int xxRedstone = (int)(Mth.cos((start - c * slice) / 180.0f * (float)Math.PI) * 86.0f);
        int yyRedstone = (int)(Mth.sin((start - c * slice) / 180.0f * (float)Math.PI) * 86.0f);
        addRenderableWidget(Button.builder(Component.literal("R"), btn -> toggleRedstone())
                .bounds(leftPos + middleX + xxRedstone - 8, topPos + middleY + yyRedstone - 8, 16, 16)
                .build());
        
        // Setup category-specific buttons
        switch (currentCategory) {
            case ISealGui.CAT_PRIORITY -> setupPriorityCategory();
            case ISealGui.CAT_FILTER -> setupFilterCategory();
            case ISealGui.CAT_AREA -> setupAreaCategory();
            case ISealGui.CAT_TOGGLES -> setupTogglesCategory();
            case ISealGui.CAT_TAGS -> setupTagsCategory();
        }
    }
    
    private void setupPriorityCategory() {
        // Priority +/- buttons
        addRenderableWidget(Button.builder(Component.literal("-"), btn -> adjustPriority(-1))
                .bounds(leftPos + middleX - 19, topPos + middleY - 22, 14, 14)
                .build());
        addRenderableWidget(Button.builder(Component.literal("+"), btn -> adjustPriority(1))
                .bounds(leftPos + middleX + 5, topPos + middleY - 22, 14, 14)
                .build());
        
        // Color +/- buttons
        addRenderableWidget(Button.builder(Component.literal("<"), btn -> adjustColor(-1))
                .bounds(leftPos + middleX + 6, topPos + middleY - 1, 14, 14)
                .build());
        addRenderableWidget(Button.builder(Component.literal(">"), btn -> adjustColor(1))
                .bounds(leftPos + middleX + 29, topPos + middleY - 1, 14, 14)
                .build());
        
        // Lock button
        if (menu.getSeal().getOwner().equals(minecraft.player.getUUID().toString())) {
            addRenderableWidget(Button.builder(Component.literal("L"), btn -> toggleLock())
                    .bounds(leftPos + middleX - 40, topPos + middleY - 8, 16, 16)
                    .build());
        }
    }
    
    private void setupFilterCategory() {
        ISealEntity seal = menu.getSeal();
        if (seal.getSeal() instanceof ISealConfigFilter filter) {
            int size = filter.getFilterSize();
            int sy = 16 + (size - 1) / 3 * 12;
            
            // Blacklist/whitelist toggle
            addRenderableWidget(Button.builder(Component.literal("B/W"), btn -> toggleBlacklist())
                    .bounds(leftPos + middleX - 8, topPos + middleY + (size - 1) / 3 * 24 - sy + 27, 16, 16)
                    .build());
        }
    }
    
    private void setupAreaCategory() {
        // Y +/-
        addRenderableWidget(Button.builder(Component.literal("-"), btn -> adjustArea(0, -1, 0))
                .bounds(leftPos + middleX - 19, topPos + middleY - 30, 14, 14)
                .build());
        addRenderableWidget(Button.builder(Component.literal("+"), btn -> adjustArea(0, 1, 0))
                .bounds(leftPos + middleX + 5, topPos + middleY - 30, 14, 14)
                .build());
        
        // X +/-
        addRenderableWidget(Button.builder(Component.literal("-"), btn -> adjustArea(-1, 0, 0))
                .bounds(leftPos + middleX - 19, topPos + middleY - 5, 14, 14)
                .build());
        addRenderableWidget(Button.builder(Component.literal("+"), btn -> adjustArea(1, 0, 0))
                .bounds(leftPos + middleX + 5, topPos + middleY - 5, 14, 14)
                .build());
        
        // Z +/-
        addRenderableWidget(Button.builder(Component.literal("-"), btn -> adjustArea(0, 0, -1))
                .bounds(leftPos + middleX - 19, topPos + middleY + 20, 14, 14)
                .build());
        addRenderableWidget(Button.builder(Component.literal("+"), btn -> adjustArea(0, 0, 1))
                .bounds(leftPos + middleX + 5, topPos + middleY + 20, 14, 14)
                .build());
    }
    
    private void setupTogglesCategory() {
        ISealEntity seal = menu.getSeal();
        if (seal.getSeal() instanceof ISealConfigToggles toggles) {
            ISealConfigToggles.SealToggle[] props = toggles.getToggles();
            int spacing = props.length < 4 ? 16 : props.length < 6 ? 14 : 12;
            int startY = topPos + middleY - (props.length - 1) * spacing / 2;
            
            for (int i = 0; i < props.length; i++) {
                final int toggleIndex = i;
                ISealConfigToggles.SealToggle prop = props[i];
                addRenderableWidget(Button.builder(
                        Component.translatable(prop.getName()).append(": " + (prop.getValue() ? "ON" : "OFF")),
                        btn -> toggleProp(toggleIndex))
                        .bounds(leftPos + middleX - 50, startY + i * spacing, 100, 14)
                        .build());
            }
        }
    }
    
    private void setupTagsCategory() {
        // Tags category is display-only, no buttons needed
    }
    
    // Button actions
    private void switchCategory(int catIndex) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, catIndex);
        }
        currentCategory = categories[catIndex];
        setupCategories();
    }
    
    private void adjustPriority(int delta) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, delta > 0 ? 81 : 80);
        }
    }
    
    private void adjustColor(int delta) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, delta > 0 ? 83 : 82);
        }
    }
    
    private void adjustArea(int dx, int dy, int dz) {
        if (minecraft != null && minecraft.gameMode != null) {
            int buttonId = 90;
            if (dy != 0) buttonId = dy > 0 ? 91 : 90;
            else if (dx != 0) buttonId = dx > 0 ? 93 : 92;
            else if (dz != 0) buttonId = dz > 0 ? 95 : 94;
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        }
    }
    
    private void toggleLock() {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, menu.isLocked() ? 26 : 25);
        }
    }
    
    private void toggleRedstone() {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, menu.isRedstoneSensitive() ? 28 : 27);
        }
    }
    
    private void toggleBlacklist() {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, menu.isBlacklist() ? 21 : 20);
        }
    }
    
    private void toggleProp(int index) {
        if (minecraft != null && minecraft.gameMode != null) {
            ISealEntity seal = menu.getSeal();
            if (seal.getSeal() instanceof ISealConfigToggles toggles) {
                boolean currentValue = toggles.getToggles()[index].getValue();
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, currentValue ? 60 + index : 30 + index);
            }
        }
        setupCategories(); // Refresh to update button text
    }
    
    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
        // Handle ghost slot clicks
        if (slot instanceof GhostSlot ghostSlot) {
            ItemStack carried = menu.getCarried();
            
            if (mouseButton == 1) {
                // Right click - clear
                ghostSlot.clearGhost();
            } else if (!carried.isEmpty()) {
                // Left click with item - set filter
                ItemStack copy = carried.copy();
                copy.setCount(1);
                ghostSlot.setGhostItem(copy);
            } else if (ghostSlot.hasItem()) {
                // Left click empty - clear
                ghostSlot.clearGhost();
            }
            return;
        }
        super.slotClicked(slot, slotId, mouseButton, type);
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
        
        // Draw circular background
        graphics.blit(TEXTURE, leftPos + middleX - 80, topPos + middleY - 80, 96, 0, 160, 160);
        
        // Draw player inventory section
        graphics.blit(TEXTURE, leftPos, topPos + 143, 0, 167, 176, 89);
        
        // Draw category title
        String categoryName = getCategoryName(currentCategory);
        graphics.drawCenteredString(font, categoryName, leftPos + middleX, topPos + middleY - 64, 0xFFFFFF);
        
        // Draw category-specific content
        switch (currentCategory) {
            case ISealGui.CAT_PRIORITY -> renderPriorityCategory(graphics, mouseX, mouseY);
            case ISealGui.CAT_FILTER -> renderFilterCategory(graphics);
            case ISealGui.CAT_AREA -> renderAreaCategory(graphics);
            case ISealGui.CAT_TAGS -> renderTagsCategory(graphics);
        }
    }
    
    private void renderPriorityCategory(GuiGraphics graphics, int mouseX, int mouseY) {
        // Color indicator
        graphics.blit(TEXTURE, leftPos + middleX + 17, topPos + middleY + 3, 2, 18, 12, 12);
        int color = menu.getColor();
        if (color >= 1 && color <= 16) {
            DyeColor dye = DyeColor.byId(color - 1);
            Color c = new Color(dye.getFireworkColor());
            RenderSystem.setShaderColor(c.getRed() / 255.0f, c.getGreen() / 255.0f, c.getBlue() / 255.0f, 1.0f);
            graphics.blit(TEXTURE, leftPos + middleX + 20, topPos + middleY + 6, 74, 31, 6, 6);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
        
        // Priority label and value
        graphics.drawCenteredString(font, Component.translatable("golem.prop.priority"), 
                leftPos + middleX, topPos + middleY - 28, 0xBBCC9F);
        graphics.drawCenteredString(font, String.valueOf(menu.getPriority()), 
                leftPos + middleX, topPos + middleY - 16, 0xFFFFFF);
        
        // Owner label
        if (menu.getSeal().getOwner().equals(minecraft.player.getUUID().toString())) {
            graphics.drawCenteredString(font, Component.translatable("golem.prop.owner"), 
                    leftPos + middleX, topPos + middleY + 32, 0xBBCC9F);
        }
    }
    
    private void renderFilterCategory(GuiGraphics graphics) {
        ISealEntity seal = menu.getSeal();
        if (seal.getSeal() instanceof ISealConfigFilter filter) {
            int size = filter.getFilterSize();
            int sx = 16 + (size - 1) % 3 * 12;
            int sy = 16 + (size - 1) / 3 * 12;
            
            // Draw filter slot backgrounds
            for (int i = 0; i < size; i++) {
                int x = i % 3;
                int y = i / 3;
                graphics.blit(TEXTURE, 
                        leftPos + middleX + x * 24 - sx, 
                        topPos + middleY + y * 24 - sy, 
                        0, 56, 32, 32);
            }
        }
    }
    
    private void renderAreaCategory(GuiGraphics graphics) {
        BlockPos area = menu.getArea();
        
        // Labels
        graphics.drawCenteredString(font, Component.translatable("button.caption.y"), 
                leftPos + middleX, topPos + middleY - 33, 0xDDDDDD);
        graphics.drawCenteredString(font, Component.translatable("button.caption.x"), 
                leftPos + middleX, topPos + middleY - 9, 0xDDDDDD);
        graphics.drawCenteredString(font, Component.translatable("button.caption.z"), 
                leftPos + middleX, topPos + middleY + 15, 0xDDDDDD);
        
        // Values
        graphics.drawCenteredString(font, String.valueOf(area.getY()), 
                leftPos + middleX, topPos + middleY - 24, 0xFFFFFF);
        graphics.drawCenteredString(font, String.valueOf(area.getX()), 
                leftPos + middleX, topPos + middleY, 0xFFFFFF);
        graphics.drawCenteredString(font, String.valueOf(area.getZ()), 
                leftPos + middleX, topPos + middleY + 24, 0xFFFFFF);
    }
    
    private void renderTagsCategory(GuiGraphics graphics) {
        ISealEntity seal = menu.getSeal();
        
        graphics.drawCenteredString(font, Component.translatable("button.caption.required"), 
                leftPos + middleX, topPos + middleY - 26, 0xDDDDDD);
        graphics.drawCenteredString(font, Component.translatable("button.caption.forbidden"), 
                leftPos + middleX, topPos + middleY + 6, 0xDDDDDD);
        
        // Draw required tags
        EnumGolemTrait[] required = seal.getSeal().getRequiredTags();
        if (required != null && required.length > 0) {
            int startX = leftPos + middleX - (required.length - 1) * 9;
            for (int i = 0; i < required.length; i++) {
                // Draw trait icon (placeholder - would need trait icons)
                graphics.drawString(font, required[i].name().substring(0, 1), 
                        startX + i * 18, topPos + middleY - 12, 0x00FF00);
            }
        }
        
        // Draw forbidden tags  
        EnumGolemTrait[] forbidden = seal.getSeal().getForbiddenTags();
        if (forbidden != null && forbidden.length > 0) {
            int startX = leftPos + middleX - (forbidden.length - 1) * 9;
            for (int i = 0; i < forbidden.length; i++) {
                graphics.drawString(font, forbidden[i].name().substring(0, 1), 
                        startX + i * 18, topPos + middleY + 20, 0xFF0000);
            }
        }
    }
    
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Don't draw default labels
    }
    
    private String getCategoryName(int category) {
        return switch (category) {
            case ISealGui.CAT_PRIORITY -> "Priority";
            case ISealGui.CAT_FILTER -> "Filter";
            case ISealGui.CAT_AREA -> "Area";
            case ISealGui.CAT_TOGGLES -> "Options";
            case ISealGui.CAT_TAGS -> "Tags";
            default -> "Settings";
        };
    }
    
    /**
     * Custom button for category tabs
     */
    private static class CategoryButton extends Button {
        private final int category;
        private final boolean active;
        
        public CategoryButton(int x, int y, int category, boolean active, OnPress onPress) {
            super(x, y, 16, 16, Component.empty(), onPress, Button.DEFAULT_NARRATION);
            this.category = category;
            this.active = active;
        }
        
        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            // Draw category button background
            int color = active ? 0xFFFFFF : 0x808080;
            graphics.fill(getX(), getY(), getX() + width, getY() + height, color | 0x80000000);
            graphics.drawString(Minecraft.getInstance().font, 
                    String.valueOf(category), getX() + 4, getY() + 4, color);
        }
    }
}




