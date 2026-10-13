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

import java.awt.*;
import java.util.function.Consumer;

/**
 * ImageButton - A button that displays an image from a texture atlas.
 * Supports hover effects and custom colors.
 * 
 * Ported from GuiImageButton in 1.12.2
 */
@OnlyIn(Dist.CLIENT)
public class ImageButton extends AbstractWidget {
    
    private final ResourceLocation texture;
    private final int texX;
    private final int texY;
    private final int texWidth;
    private final int texHeight;
    private final String description;
    private final int color;
    private final Consumer<ImageButton> onPress;
    
    private boolean buttonActive = true;
    
    public ImageButton(int x, int y, int width, int height, String text, String description,
                      ResourceLocation texture, int texX, int texY, int texWidth, int texHeight,
                      Consumer<ImageButton> onPress) {
        this(x, y, width, height, text, description, texture, texX, texY, texWidth, texHeight, 0xFFFFFF, onPress);
    }
    
    public ImageButton(int x, int y, int width, int height, String text, String description,
                      ResourceLocation texture, int texX, int texY, int texWidth, int texHeight,
                      int color, Consumer<ImageButton> onPress) {
        super(x, y, width, height, text != null ? Component.translatable(text) : Component.empty());
        this.texture = texture;
        this.texX = texX;
        this.texY = texY;
        this.texWidth = texWidth;
        this.texHeight = texHeight;
        this.description = description;
        this.color = color;
        this.onPress = onPress;
    }
    
    public void setButtonActive(boolean active) {
        this.buttonActive = active;
    }
    
    public boolean isButtonActive() {
        return buttonActive;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        
        // Calculate hover state
        boolean hovered = isMouseOver(mouseX, mouseY);
        
        // Calculate color and alpha
        Color c = new Color(color);
        float brightness = 0.9f;
        float alpha = 1.0f;
        
        if (hovered) {
            brightness = 1.0f;
            alpha = 1.0f;
        }
        if (!buttonActive) {
            brightness = 0.5f;
            alpha = 0.9f;
        }
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(
            brightness * (c.getRed() / 255.0f),
            brightness * (c.getGreen() / 255.0f),
            brightness * (c.getBlue() / 255.0f),
            alpha
        );
        
        // Draw centered on position
        int drawX = getX() - texWidth / 2;
        int drawY = getY() - texHeight / 2;
        graphics.blit(texture, drawX, drawY, texX, texY, texWidth, texHeight);
        
        // Reset color
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        
        // Draw text if present
        String text = getMessage().getString();
        if (text != null && !text.isEmpty()) {
            int textColor = 0xFFFFFF;
            if (!active) {
                textColor = 0xA0A0A0;
            } else if (hovered) {
                textColor = 0xFFFFA0;
            }
            
            graphics.pose().pushPose();
            graphics.pose().translate(getX(), getY(), 0);
            graphics.pose().scale(0.5f, 0.5f, 1.0f);
            graphics.drawCenteredString(font, Component.translatable(text), 0, -4, textColor);
            graphics.pose().popPose();
        }
    }
    
    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        // Use centered click detection (like original)
        return mouseX >= getX() - width / 2 && mouseX < getX() + width / 2 &&
               mouseY >= getY() - height / 2 && mouseY < getY() + height / 2;
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (buttonActive && active && visible && isMouseOver(mouseX, mouseY)) {
            if (onPress != null) {
                onPress.accept(this);
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




