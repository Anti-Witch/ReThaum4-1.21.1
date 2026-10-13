package thaumcraft.client.lib;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

import java.text.DecimalFormat;
import java.util.List;

/**
 * AspectRenderer - Utility class for rendering aspects in GUIs.
 * Ported from UtilsFX.drawTag() for 1.20.1.
 *
 * This handles rendering aspect icons with their colored tint,
 * optional amount text, and bonus indicators.
 */
@OnlyIn(Dist.CLIENT)
public class AspectRenderer {

    private static final DecimalFormat AMOUNT_FORMATTER = new DecimalFormat("#######.##");
    private static final int ICON_SIZE = 16;

    /**
     * Draw an aspect icon at the specified position (simple version).
     *
     * @param graphics the graphics context
     * @param x X position
     * @param y Y position
     * @param aspect the aspect to render
     */
    public static void drawAspect(GuiGraphics graphics, int x, int y, Aspect aspect) {
        drawAspect(graphics, x, y, aspect, 0, 0, 1.0f, false);
    }

    /**
     * Draw an aspect icon with an amount displayed.
     *
     * @param graphics the graphics context
     * @param x X position
     * @param y Y position
     * @param aspect the aspect to render
     * @param amount the amount to display (0 to hide)
     */
    public static void drawAspect(GuiGraphics graphics, int x, int y, Aspect aspect, float amount) {
        drawAspect(graphics, x, y, aspect, amount, 0, 1.0f, false);
    }

    /**
     * Draw an aspect icon with full options.
     *
     * @param graphics the graphics context
     * @param x X position
     * @param y Y position
     * @param aspect the aspect to render
     * @param amount the amount to display (0 to hide)
     * @param bonus bonus indicator count (0 to hide)
     * @param alpha transparency (0.0 to 1.0)
     * @param grayscale if true, render in grayscale
     */
    public static void drawAspect(GuiGraphics graphics, int x, int y, Aspect aspect,
            float amount, int bonus, float alpha, boolean grayscale) {
        if (aspect == null) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        // Get aspect color components
        int color = aspect.getColor();
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        if (grayscale) {
            r = g = b = 0.1f;
            alpha *= 0.8f;
        }

        // Render the aspect icon
        ResourceLocation texture = aspect.getImage();
        renderColoredTexture(graphics, texture, x, y, ICON_SIZE, ICON_SIZE, r, g, b, alpha);

        // Render amount text
        if (amount > 0) {
            String amountStr = AMOUNT_FORMATTER.format(amount);
            int textX = x + ICON_SIZE - font.width(amountStr);
            int textY = y + ICON_SIZE - font.lineHeight;

            // Draw shadow for readability
            graphics.drawString(font, amountStr, textX + 1, textY + 1, 0x000000, false);
            graphics.drawString(font, amountStr, textX, textY, 0xFFFFFF, false);
        }

        // Render bonus indicator
        if (bonus > 0) {
            // Draw a small star/indicator at top-left
            graphics.drawString(font, "+", x - 2, y - 4, 0xFFFF00, false);
            if (bonus > 1) {
                graphics.drawString(font, String.valueOf(bonus), x + 4, y - 2, 0xFFFFFF, false);
            }
        }
    }

    /**
     * Render a texture with color tinting using the new rendering system.
     */
    private static void renderColoredTexture(GuiGraphics graphics, ResourceLocation texture,
            int x, int y, int width, int height, float r, float g, float b, float a) {

        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Matrix4f matrix = graphics.pose().last().pose();

        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        builder.addVertex(matrix, x, y + height, 0).setUv(0, 1).setColor(r, g, b, a);
        builder.addVertex(matrix, x + width, y + height, 0).setUv(1, 1).setColor(r, g, b, a);
        builder.addVertex(matrix, x + width, y, 0).setUv(1, 0).setColor(r, g, b, a);
        builder.addVertex(matrix, x, y, 0).setUv(0, 0).setColor(r, g, b, a);

        BufferUploader.drawWithShader(builder.build());

        RenderSystem.disableBlend();
    }

    /**
     * Draw an aspect list horizontally starting at the given position.
     *
     * @param graphics the graphics context
     * @param x starting X position
     * @param y Y position
     * @param aspects the aspect list to render
     * @param spacing horizontal spacing between icons
     * @return the ending X position
     */
    public static int drawAspectList(GuiGraphics graphics, int x, int y, AspectList aspects, int spacing) {
        if (aspects == null) return x;

        int currentX = x;
        for (Aspect aspect : aspects.getAspects()) {
            int amount = aspects.getAmount(aspect);
            drawAspect(graphics, currentX, y, aspect, amount);
            currentX += ICON_SIZE + spacing;
        }
        return currentX;
    }

    /**
     * Draw an aspect list centered at the given position.
     *
     * @param graphics the graphics context
     * @param centerX center X position
     * @param y Y position
     * @param aspects the aspect list to render
     * @param spacing horizontal spacing between icons
     */
    public static void drawAspectListCentered(GuiGraphics graphics, int centerX, int y,
            AspectList aspects, int spacing) {
        if (aspects == null || aspects.size() == 0) return;

        int totalWidth = aspects.size() * (ICON_SIZE + spacing) - spacing;
        int startX = centerX - totalWidth / 2;
        drawAspectList(graphics, startX, y, aspects, spacing);
    }

    /**
     * Draw a small aspect icon (8x8) for compact displays.
     *
     * @param graphics the graphics context
     * @param x X position
     * @param y Y position
     * @param aspect the aspect to render
     */
    public static void drawAspectSmall(GuiGraphics graphics, int x, int y, Aspect aspect) {
        if (aspect == null) return;

        int color = aspect.getColor();
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        ResourceLocation texture = aspect.getImage();
        renderColoredTexture(graphics, texture, x, y, 8, 8, r, g, b, 1.0f);
    }

    /**
     * Render an aspect icon at the specified position (alias for drawAspect).
     * Used by ResearchTableScreen.
     *
     * @param graphics the graphics context
     * @param aspect the aspect to render
     * @param x X position
     * @param y Y position
     * @param width icon width
     * @param height icon height
     */
    public static void renderAspectIcon(GuiGraphics graphics, Aspect aspect, int x, int y, int width, int height) {
        if (aspect == null) return;

        int color = aspect.getColor();
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        ResourceLocation texture = aspect.getImage();
        renderColoredTexture(graphics, texture, x, y, width, height, r, g, b, 1.0f);
    }

    /**
     * Get the tooltip for an aspect.
     *
     * @param aspect the aspect
     * @param amount optional amount to show
     * @return tooltip components
     */
    public static List<net.minecraft.network.chat.Component> getAspectTooltip(Aspect aspect, int amount) {
        if (aspect == null) return List.of();

        java.util.ArrayList<net.minecraft.network.chat.Component> tooltip = new java.util.ArrayList<>();

        // Name with color
        int color = aspect.getColor();
        tooltip.add(net.minecraft.network.chat.Component.literal(aspect.getName())
                .withStyle(style -> style.withColor(color)));

        // Description
        tooltip.add(net.minecraft.network.chat.Component.literal(aspect.getLocalizedDescription())
                .withStyle(net.minecraft.ChatFormatting.GRAY));

        // Amount if specified
        if (amount > 0) {
            tooltip.add(net.minecraft.network.chat.Component.literal("Amount: " + amount)
                    .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        }

        // Component aspects for non-primal
        if (!aspect.isPrimal() && aspect.getComponents() != null) {
            Aspect[] components = aspect.getComponents();
            String compStr = components[0].getName() + " + " + components[1].getName();
            tooltip.add(net.minecraft.network.chat.Component.literal("(" + compStr + ")")
                    .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        }

        return tooltip;
    }

    /**
     * Check if mouse is hovering over an aspect icon at the given position.
     *
     * @param x icon X position
     * @param y icon Y position
     * @param mouseX mouse X
     * @param mouseY mouse Y
     * @return true if hovering
     */
    public static boolean isMouseOverAspect(int x, int y, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + ICON_SIZE && mouseY >= y && mouseY < y + ICON_SIZE;
    }

    /**
     * Draw an aspect with tooltip support in a screen.
     * Call this in your screen's render method.
     * Returns the tooltip if hovering, null otherwise.
     *
     * @param graphics the graphics context
     * @param x X position
     * @param y Y position
     * @param aspect the aspect to render
     * @param amount the amount
     * @param mouseX mouse X
     * @param mouseY mouse Y
     * @return tooltip components if hovering, null otherwise
     */
    public static List<net.minecraft.network.chat.Component> drawAspectWithTooltip(
            GuiGraphics graphics, int x, int y, Aspect aspect, int amount, int mouseX, int mouseY) {
        drawAspect(graphics, x, y, aspect, amount);

        if (isMouseOverAspect(x, y, mouseX, mouseY)) {
            return getAspectTooltip(aspect, amount);
        }
        return null;
    }
}