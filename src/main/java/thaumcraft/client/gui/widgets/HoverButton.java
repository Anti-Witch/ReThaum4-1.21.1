package thaumcraft.client.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.api.aspects.Aspect;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * HoverButton - A button that displays various content types (aspect, item, texture).
 * Shows tooltips on hover.
 * 
 * Ported from GuiHoverButton in 1.12.2
 */
@OnlyIn(Dist.CLIENT)
public class HoverButton extends AbstractWidget {
    
    private final String description;
    private final int color;
    private final Object content; // Can be Aspect, ResourceLocation, ItemStack
    
    public HoverButton(int x, int y, int width, int height, String text, String description, Object content) {
        this(x, y, width, height, text, description, content, 0xFFFFFF);
    }
    
    public HoverButton(int x, int y, int width, int height, String text, String description, Object content, int color) {
        super(x, y, width, height, text != null ? Component.literal(text) : Component.empty());
        this.description = description;
        this.content = content;
        this.color = color;
    }
    
    public String getDescription() {
        return description;
    }
    
    public Object getContent() {
        return content;
    }
    
    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        
        // Calculate hover state
        boolean hovered = isMouseOver(mouseX, mouseY);
        
        // Calculate color
        Color c = new Color(color);
        float brightness = hovered ? 1.0f : 0.9f;
        float alpha = hovered ? 1.0f : 0.9f;
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        // Draw content centered at position
        int drawX = getX() - width / 2;
        int drawY = getY() - height / 2;
        
        if (content instanceof Aspect aspect) {
            // Draw aspect icon
            Color aspectColor = new Color(aspect.getColor());
            if (hovered) {
                RenderSystem.setShaderColor(
                    aspectColor.getRed() / 255.0f,
                    aspectColor.getGreen() / 255.0f,
                    aspectColor.getBlue() / 255.0f,
                    1.0f
                );
            } else {
                RenderSystem.setShaderColor(
                    aspectColor.getRed() / 290.0f,
                    aspectColor.getGreen() / 290.0f,
                    aspectColor.getBlue() / 290.0f,
                    0.9f
                );
            }
            graphics.blit(aspect.getImage(), drawX, drawY, 0, 0, 16, 16, 16, 16);
        } else if (content instanceof ResourceLocation texture) {
            // Draw texture
            RenderSystem.setShaderColor(
                brightness * (c.getRed() / 255.0f),
                brightness * (c.getGreen() / 255.0f),
                brightness * (c.getBlue() / 255.0f),
                alpha
            );
            graphics.blit(texture, drawX, drawY, 0, 0, 16, 16, 16, 16);
        } else if (content instanceof ItemStack stack) {
            // Draw item
            int yOffset = hovered ? -1 : 0;
            graphics.renderItem(stack, drawX, drawY + yOffset);
        }
        
        // Reset color
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
    
    /**
     * Render the tooltip for this button.
     * Call this from the parent screen's render method after rendering all widgets.
     */
    public void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!isMouseOver(mouseX, mouseY)) return;
        
        List<Component> tooltip = new ArrayList<>();
        
        if (content instanceof ItemStack stack) {
            tooltip.addAll(stack.getTooltipLines(
                net.minecraft.world.item.Item.TooltipContext.of(Minecraft.getInstance().level),
                Minecraft.getInstance().player,
                Minecraft.getInstance().options.advancedItemTooltips
                    ? net.minecraft.world.item.TooltipFlag.Default.ADVANCED
                    : net.minecraft.world.item.TooltipFlag.Default.NORMAL));
        } else {
            String text = getMessage().getString();
            if (text != null && !text.isEmpty()) {
                tooltip.add(Component.literal(text));
            }
        }
        
        if (description != null && !description.isEmpty()) {
            tooltip.add(Component.literal(description).withStyle(style -> style.withItalic(true).withColor(0x5555FF)));
        }
        
        if (!tooltip.isEmpty()) {
            graphics.renderComponentTooltip(Minecraft.getInstance().font, tooltip, mouseX, mouseY);
        }
    }
    
    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        // Use centered detection (like original)
        return mouseX >= getX() - width / 2 && mouseX < getX() + width / 2 &&
               mouseY >= getY() - height / 2 && mouseY < getY() + height / 2;
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // HoverButton doesn't respond to clicks by default (display only)
        return false;
    }
    
    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}




