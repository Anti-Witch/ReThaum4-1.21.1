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
import thaumcraft.client.lib.AspectRenderer;
import thaumcraft.common.menu.DeconstructionTableMenu;
import thaumcraft.init.ModSounds;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * DeconstructionTableScreen - client GUI for the TC4 Deconstruction Table.
 *
 * Ported from OpenTC4 GuiDeconstructionTable:
 * - gui_decontable.png (176x166), progress bar sprite at (176,0) 9x46
 * - the waiting aspect is drawn at (64,48); hovering shows its tooltip and
 *   clicking sends button 1 to the menu (collect aspect into the research pool)
 * - TC4 drew no title/inventory labels on this GUI
 */
@OnlyIn(Dist.CLIENT)
public class DeconstructionTableScreen extends AbstractContainerScreen<DeconstructionTableMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_decontable.png");

    /** Position of the aspect icon inside the GUI (TC4: k+64, l+48). */
    private static final int ASPECT_X = 64;
    private static final int ASPECT_Y = 48;

    public DeconstructionTableScreen(DeconstructionTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Nullable
    private Aspect getAspect() {
        return this.menu.getBlockEntity() == null ? null : this.menu.getBlockEntity().aspect;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        // 1.21: AbstractContainerScreen no longer renders slot tooltips itself
        this.renderTooltip(graphics, mouseX, mouseY);

        Aspect aspect = getAspect();
        if (aspect != null && AspectRenderer.isMouseOverAspect(leftPos + ASPECT_X, topPos + ASPECT_Y, mouseX, mouseY)) {
            graphics.renderTooltip(this.font,
                    AspectRenderer.getAspectTooltip(aspect, 0),
                    Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        int x = this.leftPos;
        int y = this.topPos;

        // Main panel
        graphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // Deconstruction progress (TC4: k+93, l+15+46-i1, 176, 46-i1, 9, i1)
        int progress = this.menu.getBreakTimeScaled(46);
        if (progress > 0) {
            graphics.blit(TEXTURE, x + 93, y + 15 + 46 - progress, 176, 46 - progress, 9, progress);
        }

        // Waiting aspect
        Aspect aspect = getAspect();
        if (aspect != null) {
            AspectRenderer.drawAspect(graphics, x + ASPECT_X, y + ASPECT_Y, aspect);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // TC4's GuiDeconstructionTable had an empty drawGuiContainerForegroundLayer:
        // the wooden GUI texture carries no title text.
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Aspect aspect = getAspect();
        if (button == 0 && aspect != null
                && AspectRenderer.isMouseOverAspect(leftPos + ASPECT_X, topPos + ASPECT_Y,
                        (int) mouseX, (int) mouseY)) {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId,
                        DeconstructionTableMenu.BUTTON_COLLECT_ASPECT);
                playButtonAspect();
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * TC4 GuiDeconstructionTable.playButtonAspect(): "thaumcraft:hhoff".
     */
    private void playButtonAspect() {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.playSound(ModSounds.HH_OFF.get(), 0.2f,
                    1.0f + this.minecraft.player.level().random.nextFloat() * 0.1f);
        }
    }
}
