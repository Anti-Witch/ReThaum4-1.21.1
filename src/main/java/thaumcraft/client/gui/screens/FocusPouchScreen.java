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
import thaumcraft.common.menu.FocusPouchMenu;

/**
 * FocusPouchScreen - Client-side GUI for the Focus Pouch.
 * 
 * Displays a 6x3 grid of focus slots plus the player inventory.
 */
@OnlyIn(Dist.CLIENT)
public class FocusPouchScreen extends AbstractContainerScreen<FocusPouchMenu> {
    
    private static final ResourceLocation TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_focuspouch.png");
    
    public FocusPouchScreen(FocusPouchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 175;
        this.imageHeight = 232;
        // Adjust inventory label position for taller GUI
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
    }
    
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Draw title centered above pouch slots
        graphics.drawString(this.font, this.title, 
                (this.imageWidth - this.font.width(this.title)) / 2, 
                6, 0x404040, false);
        // Draw inventory label
        graphics.drawString(this.font, this.playerInventoryTitle, 
                8, this.inventoryLabelY, 0x404040, false);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Prevent using hotbar keys to move items (could move the pouch itself)
        if (this.minecraft != null && this.minecraft.options.keyHotbarSlots[this.menu.getSlot(0).getContainerSlot()].matches(keyCode, scanCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}




