package thaumcraft.client.gui.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.common.menu.HungryChestMenu;

/**
 * HungryChestScreen - Client-side GUI for the Hungry Chest.
 * Standard 27-slot chest layout, similar to vanilla chest.
 */
@OnlyIn(Dist.CLIENT)
public class HungryChestScreen extends AbstractContainerScreen<HungryChestMenu> {
    
    // Use vanilla chest texture for now, or create custom one
    private static final ResourceLocation TEXTURE = 
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    
    // Hungry chest has 3 rows (same as small chest)
    private static final int ROWS = 3;
    
    public HungryChestScreen(HungryChestMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 114 + ROWS * 18; // 168 for 3 rows
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
        
        // Draw top section (chest slots area)
        graphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, ROWS * 18 + 17);
        
        // Draw bottom section (player inventory)
        graphics.blit(TEXTURE, x, y + ROWS * 18 + 17, 0, 126, this.imageWidth, 96);
    }
    
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Draw title
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        // Draw inventory label
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }
}




