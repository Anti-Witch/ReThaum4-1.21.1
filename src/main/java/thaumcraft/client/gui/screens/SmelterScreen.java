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
import thaumcraft.common.menu.SmelterMenu;

/**
 * SmelterScreen - Client-side GUI for the Alchemical Smelter.
 * 
 * Displays:
 * - Input slot (items with aspects)
 * - Fuel slot
 * - Burn time indicator
 * - Smelting progress
 * - Vis/essentia storage level
 */
@OnlyIn(Dist.CLIENT)
public class SmelterScreen extends AbstractContainerScreen<SmelterMenu> {
    
    private static final ResourceLocation TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_alchemyfurnace.png");
    
    public SmelterScreen(SmelterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
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
        
        // Draw burn time indicator (fire icon)
        if (menu.isBurning()) {
            int burnProgress = menu.getBurnTimeScaled(20);
            // Draw fire from bottom up
            graphics.blit(TEXTURE, x + 80, y + 26 + 20 - burnProgress, 176, 20 - burnProgress, 16, burnProgress);
        }
        
        // Draw cooking progress (vertical bar on right)
        int cookProgress = menu.getCookProgressScaled(46);
        // Draw from bottom up
        graphics.blit(TEXTURE, x + 106, y + 13 + 46 - cookProgress, 216, 46 - cookProgress, 9, cookProgress);
        
        // Draw vis storage level (vertical bar on left)
        int visLevel = getVisScaled(48);
        graphics.blit(TEXTURE, x + 61, y + 12 + 48 - visLevel, 200, 48 - visLevel, 8, visLevel);
        
        // Draw vis bar frame overlay
        graphics.blit(TEXTURE, x + 60, y + 8, 232, 0, 10, 55);
    }
    
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Draw title
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        // Draw inventory label
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }
    
    /**
     * Scale the vis amount to the given scale.
     */
    private int getVisScaled(int scale) {
        int vis = menu.getVis();
        int maxVis = 256; // Max vis storage
        return vis * scale / maxVis;
    }
}




