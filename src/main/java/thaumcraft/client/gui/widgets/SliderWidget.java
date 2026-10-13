package thaumcraft.client.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;

/**
 * SliderWidget - A slider control for scrolling or value selection.
 * Can be either horizontal or vertical.
 * 
 * Ported from GuiSliderTC in 1.12.2
 */
@OnlyIn(Dist.CLIENT)
public class SliderWidget extends AbstractWidget {
    
    private static final ResourceLocation TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_base.png");
    
    private float sliderPosition = 0.0f;
    private boolean dragging = false;
    private final float min;
    private final float max;
    private final boolean vertical;
    private final OnValueChange onValueChange;
    
    @FunctionalInterface
    public interface OnValueChange {
        void onValueChange(float value);
    }
    
    public SliderWidget(int x, int y, int width, int height, float min, float max, float defaultValue, 
                       boolean vertical, OnValueChange onValueChange) {
        super(x, y, width, height, Component.empty());
        this.min = min;
        this.max = max;
        this.vertical = vertical;
        this.onValueChange = onValueChange;
        
        // Calculate initial position
        if (max > min) {
            this.sliderPosition = (defaultValue - min) / (max - min);
        }
    }
    
    public float getMin() {
        return min;
    }
    
    public float getMax() {
        return max;
    }
    
    public float getSliderValue() {
        return min + (max - min) * sliderPosition;
    }
    
    public void setSliderValue(float value, boolean notify) {
        if (max > min) {
            sliderPosition = Math.max(0.0f, Math.min(1.0f, (value - min) / (max - min)));
        }
        if (notify && onValueChange != null) {
            onValueChange.onValueChange(getSliderValue());
        }
    }
    
    public float getSliderPosition() {
        return sliderPosition;
    }
    
    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        // Draw track
        graphics.pose().pushPose();
        if (vertical) {
            graphics.pose().translate(getX() + 2, getY(), 0);
            graphics.pose().scale(1.0f, height / 32.0f, 1.0f);
            graphics.blit(TEXTURE, 0, 0, 240, 176, 4, 32);
        } else {
            graphics.pose().translate(getX(), getY() + 2, 0);
            graphics.pose().scale(width / 32.0f, 1.0f, 1.0f);
            graphics.blit(TEXTURE, 0, 0, 208, 176, 32, 4);
        }
        graphics.pose().popPose();
        
        // Draw handle
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        if (vertical) {
            int handleY = (int)(sliderPosition * (height - 8));
            graphics.blit(TEXTURE, getX(), getY() + handleY, 20, 20, 8, 8);
        } else {
            int handleX = (int)(sliderPosition * (width - 8));
            graphics.blit(TEXTURE, getX() + handleX, getY(), 20, 20, 8, 8);
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isValidClickButton(button) && clicked(mouseX, mouseY)) {
            dragging = true;
            updateSliderPosition(mouseX, mouseY);
            return true;
        }
        return false;
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) {
            dragging = false;
            return true;
        }
        return false;
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging) {
            updateSliderPosition(mouseX, mouseY);
            return true;
        }
        return false;
    }
    
    private void updateSliderPosition(double mouseX, double mouseY) {
        float oldValue = getSliderValue();
        
        if (vertical) {
            sliderPosition = (float)((mouseY - getY() - 4) / (height - 8));
        } else {
            sliderPosition = (float)((mouseX - getX() - 4) / (width - 8));
        }
        
        sliderPosition = Math.max(0.0f, Math.min(1.0f, sliderPosition));
        
        float newValue = getSliderValue();
        if (oldValue != newValue && onValueChange != null) {
            onValueChange.onValueChange(newValue);
        }
    }
    
    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}




