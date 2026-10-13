package thaumcraft.client.gui.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.menu.ThaumatoriumMenu;
import thaumcraft.common.tiles.crafting.TileThaumatorium;

/**
 * ThaumatoriumScreen - Client-side GUI for the Thaumatorium (automated alchemy).
 * 
 * Displays:
 * - Input/catalyst slot
 * - Stored essentia amounts
 * - Available recipes
 * - Crafting progress
 * - Player inventory
 */
@OnlyIn(Dist.CLIENT)
public class ThaumatoriumScreen extends AbstractContainerScreen<ThaumatoriumMenu> {
    
    private static final ResourceLocation TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_thaumatorium.png");
    
    public ThaumatoriumScreen(ThaumatoriumMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 175;
        this.imageHeight = 216;
        this.inventoryLabelY = this.imageHeight - 94;
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
        int x = this.leftPos;
        int y = this.topPos;
        
        // Draw main background
        graphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        
        TileThaumatorium tile = menu.getBlockEntity();
        if (tile == null) return;
        
        // Draw crafting progress if active
        if (tile.isCrafting()) {
            float progress = tile.getCraftingProgress();
            int barWidth = (int)(progress * 46);
            // Draw progress bar
            graphics.blit(TEXTURE, x + 64, y + 40, 176, 40, barWidth, 6);
        }
        
        // Draw stored essentia indicators
        drawStoredEssentia(graphics, x, y, tile);
    }
    
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Draw title
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        // Draw inventory label
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
        
        TileThaumatorium tile = menu.getBlockEntity();
        if (tile == null) return;
        
        // Draw crafting status
        if (tile.isCrafting()) {
            String status = Component.translatable("gui.thaumcraft.thaumatorium.crafting").getString();
            int progress = (int)(tile.getCraftingProgress() * 100);
            graphics.drawString(this.font, status + " " + progress + "%", 8, 48, 0x404040, false);
        }
    }
    
    /**
     * Draw indicators for stored essentia.
     */
    private void drawStoredEssentia(GuiGraphics graphics, int x, int y, TileThaumatorium tile) {
        AspectList stored = tile.getStoredAspects();
        if (stored == null || stored.size() == 0) return;
        
        int startX = x + 98;
        int startY = y + 24;
        int col = 0;
        int row = 0;
        int count = 0;
        
        for (Aspect aspect : stored.getAspectsSortedByAmount()) {
            if (count >= 8) break; // Max 8 displayed
            
            int amount = stored.getAmount(aspect);
            if (amount <= 0) continue;
            
            int px = startX + col * 18;
            int py = startY + row * 20;
            
            // Draw aspect icon (placeholder - would need actual aspect rendering)
            int color = aspect.getColor();
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;
            
            RenderSystem.setShaderColor(r, g, b, 1.0f);
            graphics.blit(TEXTURE, px, py, 176, 24, 16, 16);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            
            // Draw amount
            String amountStr = String.valueOf(amount);
            graphics.drawString(this.font, amountStr, px + 8 - this.font.width(amountStr) / 2, py + 8, 0xFFFFFF, true);
            
            col++;
            if (col > 1) {
                col = 0;
                row++;
            }
            count++;
        }
    }
}




