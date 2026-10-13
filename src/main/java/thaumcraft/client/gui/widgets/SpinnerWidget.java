package thaumcraft.client.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.api.casters.NodeSetting;

/**
 * SpinnerWidget - A value spinner for focus node settings.
 * Shows left/right arrows with the current value text in the middle.
 * 
 * Ported from GuiFocusSettingSpinnerButton in 1.12.2
 */
@OnlyIn(Dist.CLIENT)
public class SpinnerWidget extends AbstractWidget {
    
    private static final ResourceLocation TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_base.png");
    
    private final NodeSetting setting;
    private final OnValueChange onValueChange;
    
    @FunctionalInterface
    public interface OnValueChange {
        void onValueChange(NodeSetting setting, int newValue);
    }
    
    public SpinnerWidget(int x, int y, int width, NodeSetting setting, OnValueChange onValueChange) {
        super(x, y, width, 10, Component.literal(setting.getLocalizedName()));
        this.setting = setting;
        this.onValueChange = onValueChange;
    }
    
    public NodeSetting getSetting() {
        return setting;
    }
    
    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        
        // Hover state affects color
        boolean hovered = isHoveredOrFocused();
        float brightness = hovered ? 1.0f : 0.9f;
        
        RenderSystem.setShaderColor(brightness, brightness, brightness, brightness);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        // Draw left arrow
        graphics.blit(TEXTURE, getX(), getY(), 20, 0, 10, 10);
        
        // Draw right arrow
        graphics.blit(TEXTURE, getX() + width, getY(), 30, 0, 10, 10);
        
        // Reset color
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        
        // Draw value text centered between arrows
        String valueText = setting.getValueText();
        int textX = getX() + (width + 10) / 2 - font.width(valueText) / 2;
        graphics.drawString(font, valueText, textX, getY() + 1, 0xFFFFFF, true);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !active) return false;
        
        // Check if clicked on left arrow (decrement)
        if (mouseX >= getX() && mouseX < getX() + 10 && 
            mouseY >= getY() && mouseY < getY() + height) {
            setting.decrement();
            if (onValueChange != null) {
                onValueChange.onValueChange(setting, setting.getValue());
            }
            return true;
        }
        
        // Check if clicked on right arrow (increment)
        if (mouseX >= getX() + width && mouseX < getX() + width + 10 && 
            mouseY >= getY() && mouseY < getY() + height) {
            setting.increment();
            if (onValueChange != null) {
                onValueChange.onValueChange(setting, setting.getValue());
            }
            return true;
        }
        
        return false;
    }
    
    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}




