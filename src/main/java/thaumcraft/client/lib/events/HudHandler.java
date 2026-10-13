package thaumcraft.client.lib.events;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.casters.ICaster;
import thaumcraft.common.items.casters.ItemFocus;
import thaumcraft.common.items.tools.ItemSanityChecker;
import thaumcraft.common.items.tools.ItemThaumometer;
import thaumcraft.common.world.aura.AuraChunk;

import java.awt.Color;
import java.text.DecimalFormat;

/**
 * HudHandler - Renders Thaumcraft HUD overlays.
 * 
 * Displays:
 * - Thaumometer aura gauge (vis/flux levels)
 * - Caster gauntlet vis gauge and focus info
 * - Sanity checker warp levels
 * 
 * Ported from 1.12.2 to use 1.20.1 GUI overlay system.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Thaumcraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class HudHandler {

    private static final ResourceLocation HUD_TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/hud.png");
    
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#######.#");
    
    // Current aura data (updated by packets from server)
    public static AuraChunk currentAura = new AuraChunk(null, (short) 0, 0.0f, 0.0f);
    
    // Max vis for gauge scaling
    private static final float MAX_VIS = 500.0f;

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "thaumcraft_hud"),
                (graphics, deltaTracker) -> {
                    Minecraft mc = Minecraft.getInstance();
                    Player player = mc.player;
                    
                    if (player == null || mc.options.hideGui) return;
                    
                    int yOffset = 0;
                    float partialTick = deltaTracker.getGameTimeDeltaTicks();
                    
                    for (int hand = 0; hand < 2; hand++) {
                        ItemStack stack = hand == 0 ? player.getMainHandItem() : player.getOffhandItem();

                        if (stack.isEmpty()) continue;

                        if (stack.getItem() instanceof thaumcraft.common.items.wands.WandItem) {
                            renderWandHud(graphics, mc, player, stack, yOffset, partialTick);
                            yOffset += 36;
                        } else if (stack.getItem() instanceof ICaster) {
                            renderCasterHud(graphics, mc, player, stack, yOffset, partialTick);
                            yOffset += 36;
                        } else if (stack.getItem() instanceof ItemThaumometer) {
                            // TC4 has no aura gauge HUD for the thaumometer
                            // (OpenTC4 ClientTickEventsFML only draws the wand
                            // dial and the sanity meter from hud.png). The port
                            // had a made-up gauge with a stray white baseline
                            // stripe and shift-numbers; removed.
                            // renderThaumometerHud(graphics, mc, player, yOffset, partialTick);
                        } else if (stack.getItem() instanceof ItemSanityChecker) {
                            // TC4 ClientTickEventsFML.renderSanityHud: while the
                            // sanity checker is held, warp bars are shown in the
                            // top-left corner.
                            renderSanityHud(graphics, mc, player);
                        }
                    }
                    
                    // Radial focus menu while holding F
                    if (KeyHandler.isRadialMenuActive() && mc.screen == null) {
                        renderRadialMenu(graphics, mc, player);
                    }
                }
        );
        Thaumcraft.LOGGER.info("Registered Thaumcraft HUD overlay");
    }
    
    /**
     * Render the thaumometer aura gauge HUD.
     */
    private static void renderThaumometerHud(GuiGraphics graphics, Minecraft mc, Player player, 
                                              int yOffset, float partialTicks) {
        
        int x = 2;
        int y = yOffset + 2;
        
        // Get aura values
        float base = currentAura != null ? currentAura.getBase() : 100;
        float vis = currentAura != null ? currentAura.getVis() : 50;
        float flux = currentAura != null ? currentAura.getFlux() : 10;
        
        // Normalize to 0-1 range
        float visNorm = Mth.clamp(vis / MAX_VIS, 0, 1);
        float fluxNorm = Mth.clamp(flux / MAX_VIS, 0, 1);
        float baseNorm = Mth.clamp(base / MAX_VIS, 0, 1);
        
        // Scale if total exceeds 1
        if (visNorm + fluxNorm > 1) {
            float scale = 1.0f / (visNorm + fluxNorm);
            visNorm *= scale;
            fluxNorm *= scale;
        }
        
        int gaugeHeight = 64;
        int gaugeWidth = 8;
        
        // Draw background frame
        RenderSystem.setShaderTexture(0, HUD_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        // Draw the gauge frame (from texture)
        graphics.blit(HUD_TEXTURE, x, y, 72, 48, 16, 80);
        
        // Draw vis bar (purple)
        if (visNorm > 0) {
            int visHeight = (int) (gaugeHeight * visNorm);
            int visY = y + 10 + (gaugeHeight - visHeight);
            drawColoredRect(graphics, x + 5, visY, gaugeWidth, visHeight, 0xB0664499);
            
            // Animated shimmer effect
            float shimmer = (player.tickCount + partialTicks) % 64;
            drawColoredRect(graphics, x + 5, visY, gaugeWidth, visHeight, 
                    0x40FFFFFF, shimmer / 64f);
        }
        
        // Draw flux bar (dark purple) below vis
        if (fluxNorm > 0) {
            int fluxHeight = (int) (gaugeHeight * fluxNorm);
            int fluxY = y + 10 + (int)(gaugeHeight * (1 - visNorm - fluxNorm));
            drawColoredRect(graphics, x + 5, fluxY, gaugeWidth, fluxHeight, 0xB0331144);
        }
        
        // Draw base marker line
        int baseY = y + 8 + (int)((1 - baseNorm) * gaugeHeight);
        drawColoredRect(graphics, x + 2, baseY, 14, 2, 0xFFFFFFFF);
        
        // Draw values if sneaking
        if (player.isShiftKeyDown()) {
            Font font = mc.font;
            graphics.pose().pushPose();
            graphics.pose().scale(0.5f, 0.5f, 1.0f);
            
            int textX = (x + 18) * 2;
            int visTextY = (y + 20) * 2;
            int fluxTextY = (y + 40) * 2;
            
            graphics.drawString(font, DECIMAL_FORMAT.format(vis), textX, visTextY, 0xEE99FF, false);
            graphics.drawString(font, DECIMAL_FORMAT.format(flux), textX, fluxTextY, 0xAA33BB, false);
            
            graphics.pose().popPose();
        }
        
        RenderSystem.disableBlend();
    }
    
    /**
     * Render the TC4-style wand dial HUD: a circular gauge (hud.png) with one
     * radial vis bar per stored aspect, gain/loss arrows, focus cost markers
     * and the installed focus item rendered in the centre of the dial.
     * Ported from OpenTC4 ClientTickEventsFML.renderCastingWandHud.
     */
    private static void renderWandHud(GuiGraphics graphics, Minecraft mc, Player player,
                                      ItemStack wandStack, int yOffset, float partialTicks) {
        thaumcraft.common.items.wands.WandItem wand =
                (thaumcraft.common.items.wands.WandItem) wandStack.getItem();

        int dialX = 2;
        int dialY = yOffset + 2;
        int centerX = dialX + 16;   // dial is 32px on screen (64px texture at 0.5)
        int centerY = dialY + 16;

        // Per-aspect vis storage on the wand
        net.minecraft.nbt.CompoundTag vis = wandStack.get(thaumcraft.init.ModDataComponents.WAND_VIS.get());
        if (vis == null || vis.isEmpty()) {
            // Still draw the dial + focus so the player sees the tool state
        }

        // Per-aspect capacity: the wand's own max vis (rod capacity, sceptre
        // bonus included; staff rods already carry their larger capacity - see
        // ConfigWands). TC4: int max = wand.getMaxVis(wandstack).
        float maxPerAspect = wand.getMaxVis(wandStack);

        RenderSystem.setShaderTexture(0, HUD_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Dial background
        graphics.pose().pushPose();
        graphics.pose().scale(0.5f, 0.5f, 1.0f);
        graphics.blit(HUD_TEXTURE, dialX * 2, dialY * 2, 0, 0, 64, 64);
        graphics.pose().popPose();

        // Track vis changes for gain/loss arrows (per aspect)
        long now = System.currentTimeMillis();
        if (now > nextWandSync) {
            lastWandVis.clear();
            if (vis != null) {
                for (String key : vis.getAllKeys()) {
                    lastWandVis.put(key, vis.getFloat(key));
                }
            }
            nextWandSync = now + 1000L;
        }

        ItemStack focusStack = wand.getFocusStack(wandStack);
        thaumcraft.common.items.casters.ItemFocus focus =
                (focusStack != null && !focusStack.isEmpty()
                        && focusStack.getItem() instanceof thaumcraft.common.items.casters.ItemFocus f)
                        ? f : null;

        // TC4 ItemWandCasting.getAllVis merges every primal aspect into the
        // list (0 when the wand has never been charged), so the dial always
        // shows all six capsules. The port used to iterate only the NBT keys,
        // which left a brand-new wand with an empty ring.
        int count = 0;
        if (maxPerAspect > 0) {
            for (Aspect aspect : Aspect.getPrimalAspects()) {
                float amt = (vis != null) ? vis.getFloat(aspect.getTag()) : 0.0f;

                graphics.pose().pushPose();
                graphics.pose().translate(centerX, centerY, 0);
                // TC4 ClientTickEventsFML.renderCastingWandHud:
                //   if (!Config.dialBottom) glRotatef(90, 0,0,1);
                //   glRotatef(-15 + count*24, 0,0,1);
                //   glTranslatef(0, -32, 0); glScaled(0.5)
                graphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90f));
                graphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-15f + count * 24f));
                graphics.pose().translate(0, -32, 0);
                graphics.pose().scale(0.5f, 0.5f, 1.0f);

                // Colored fill bar (hud.png 104,0 8x30), height by fill ratio
                int loc = (int) (30.0f * Math.min(amt / maxPerAspect, 1.0f));
                if (loc > 0) {
                    Color ac = new Color(aspect.getColor());
                    RenderSystem.setShaderColor(ac.getRed() / 255f, ac.getGreen() / 255f,
                            ac.getBlue() / 255f, 0.8f);
                    graphics.blit(HUD_TEXTURE, -4, 35 - loc, 104, 0, 8, loc);
                    RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                }

                // Dial tick mark
                graphics.blit(HUD_TEXTURE, -8, -3, 72, 0, 16, 42);

                // Gain / loss arrow
                Float old = lastWandVis.get(aspect.getTag());
                if (old != null) {
                    if (old > amt) {
                        graphics.blit(HUD_TEXTURE, -4, -8, 128, 0, 8, 8);
                    } else if (old < amt) {
                        graphics.blit(HUD_TEXTURE, -4, -8, 120, 0, 8, 8);
                    }
                }

                // Exact amounts while sneaking (TC4 shows the per-aspect vis;
                // the port stores vis directly in "points", so no /100 scaling)
                if (player.isShiftKeyDown()) {
                    graphics.pose().pushPose();
                    graphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90f));
                    String msg = String.valueOf((int) amt);
                    graphics.drawString(mc.font, msg, -32, -4, 0xFFFFFF, false);
                    graphics.pose().popPose();
                    RenderSystem.setShaderTexture(0, HUD_TEXTURE);
                }

                graphics.pose().popPose();
                count++;
            }
        }

        // Installed focus item in the centre of the dial (TC4 behaviour)
        if (focusStack != null && !focusStack.isEmpty()) {
            graphics.renderItem(focusStack, centerX - 8, centerY - 8);
            // Total cast cost while sneaking (focus cost is flat, not per-aspect)
            if (player.isShiftKeyDown() && focus != null) {
                float cost = focus.getVisCost(focusStack);
                if (cost > 0) {
                    float mod = wand.getConsumptionModifier(wandStack, player, false);
                    String costMsg = DECIMAL_FORMAT.format(cost * mod);
                    graphics.drawString(mc.font, costMsg, centerX + 10, centerY + 10, 0xFFFFFF, false);
                }
            }
        }

        RenderSystem.disableBlend();
    }

    /** Previous vis values for the gain/loss arrows (aspect tag -> amount). */
    private static final java.util.Map<String, Float> lastWandVis = new java.util.HashMap<>();
    private static long nextWandSync = 0L;

    /**
     * Render the caster gauntlet HUD.
     */
    private static void renderCasterHud(GuiGraphics graphics, Minecraft mc, Player player,
                                         ItemStack casterStack, int yOffset, float partialTicks) {
        
        ICaster caster = (ICaster) casterStack.getItem();
        
        int x = 2;
        int y = yOffset + 2;
        
        // Get vis for the gauge: the wand's internal storage, or the aura
        float maxVis;
        float currentVis;
        if (casterStack.getItem() instanceof thaumcraft.common.items.wands.WandItem wand) {
            maxVis = wand.getMaxVis(casterStack);
            currentVis = wand.getVis(casterStack);
        } else {
            maxVis = currentAura != null ? currentAura.getBase() : 100;
            currentVis = currentAura != null ? currentAura.getVis() : 50;
        }
        
        RenderSystem.setShaderTexture(0, HUD_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        // Draw dial background (scaled to 0.5)
        graphics.pose().pushPose();
        graphics.pose().scale(0.5f, 0.5f, 1.0f);
        graphics.blit(HUD_TEXTURE, x * 2, y * 2, 0, 0, 64, 64);
        graphics.pose().popPose();
        
        // Draw vis gauge
        int gaugeHeight = 30;
        float visRatio = Mth.clamp(currentVis / Math.max(maxVis, 1), 0, 1);
        int filledHeight = (int) (gaugeHeight * visRatio);
        
        // Vis bar position (to the right of the dial)
        int barX = x + 34;
        int barY = y + 2;
        
        // Draw gauge background
        graphics.pose().pushPose();
        graphics.pose().scale(0.5f, 0.5f, 1.0f);
        graphics.blit(HUD_TEXTURE, barX * 2, barY * 2, 72, 0, 16, 42);
        graphics.pose().popPose();
        
        // Draw vis fill with aspect color
        Color visColor = new Color(Aspect.ENERGY.getColor());
        int fillY = barY + 3 + (int)((1 - visRatio) * 15);
        drawColoredRect(graphics, barX + 2, fillY, 4, (int)(15 * visRatio), 
                (visColor.getRed() << 16) | (visColor.getGreen() << 8) | visColor.getBlue() | 0xCC000000);
        
        // Draw focus if equipped
        ItemStack focusStack = caster.getFocusStack(casterStack);
        if (focusStack != null && !focusStack.isEmpty() && focusStack.getItem() instanceof ItemFocus focus) {
            // Render focus item
            graphics.renderItem(focusStack, x + 4, y + 4);
            
            // Show vis cost if sneaking
            if (player.isShiftKeyDown()) {
                float visCost = focus.getVisCost(focusStack);
                if (visCost > 0) {
                    float mod = caster.getConsumptionModifier(casterStack, player, false);
                    String costStr = DECIMAL_FORMAT.format(visCost * mod);
                    
                    graphics.pose().pushPose();
                    graphics.pose().scale(0.5f, 0.5f, 1.0f);
                    int textX = (x + 24) * 2;
                    int textY = (y + 24) * 2;
                    graphics.drawString(mc.font, costStr, textX, textY, 0xFFFFFF, false);
                    graphics.pose().popPose();
                }
            }
        }
        
        // Show current vis amount if sneaking
        if (player.isShiftKeyDown()) {
            graphics.pose().pushPose();
            graphics.pose().scale(0.5f, 0.5f, 1.0f);
            String visStr = DECIMAL_FORMAT.format(currentVis);
            graphics.drawString(mc.font, visStr, (barX - 8) * 2, (barY + 22) * 2, 0xFFFFFF, false);
            graphics.pose().popPose();
        }
        
        RenderSystem.disableBlend();
    }
    
    /**
     * Render the radial focus menu (visible while F is held).
     * Draws all foci from the player's inventory (and focus pouches) in a
     * circle around the screen centre; the currently installed focus is
     * highlighted. Selection is done by tapping F to cycle.
     */
    private static void renderRadialMenu(GuiGraphics graphics, Minecraft mc, Player player) {
        java.util.List<ItemStack> foci = new java.util.ArrayList<>();
        for (int slot = 0; slot < 36; slot++) {
            ItemStack item = player.getInventory().items.get(slot);
            if (item.isEmpty()) continue;
            if (item.getItem() instanceof ItemFocus) {
                foci.add(item);
            } else if (item.getItem() instanceof thaumcraft.common.items.casters.ItemFocusPouch pouch) {
                var inv = pouch.getInventory(item);
                if (inv != null) {
                    for (ItemStack focusStack : inv) {
                        if (!focusStack.isEmpty() && focusStack.getItem() instanceof ItemFocus) {
                            foci.add(focusStack);
                        }
                    }
                }
            }
        }

        if (foci.isEmpty()) {
            return;
        }

        ItemStack currentFocus = null;
        for (int hand = 0; hand < 2; hand++) {
            ItemStack stack = hand == 0 ? player.getMainHandItem() : player.getOffhandItem();
            if (!stack.isEmpty() && stack.getItem() instanceof ICaster caster) {
                ItemStack f = caster.getFocusStack(stack);
                if (f != null && !f.isEmpty()) {
                    currentFocus = f;
                }
            }
        }

        int centerX = mc.getWindow().getGuiScaledWidth() / 2;
        int centerY = mc.getWindow().getGuiScaledHeight() / 2;
        int radius = 56;
        int count = Math.min(foci.size(), 12);

        graphics.fill(centerX - 80, centerY - 80, centerX + 80, centerY + 80, 0x55000000);

        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2 * i) / count - Math.PI / 2;
            int x = centerX + (int) (Math.cos(angle) * radius) - 8;
            int y = centerY + (int) (Math.sin(angle) * radius) - 8;

            graphics.fill(x - 2, y - 2, x + 20, y + 20, 0xAA202020);
            if (currentFocus != null && ItemStack.isSameItemSameComponents(currentFocus, foci.get(i))) {
                graphics.fill(x - 3, y - 3, x + 21, y + 21, 0xFFFFFFFF);
            }
            graphics.renderItem(foci.get(i), x, y);
        }

        String hint = net.minecraft.client.gui.screens.Screen.hasShiftDown()
                ? "" : net.minecraft.network.chat.Component.translatable("thaumcraft.radial.hint").getString();
        if (!hint.isEmpty()) {
            graphics.drawString(mc.font, hint, centerX - mc.font.width(hint) / 2, centerY + radius + 8, 0xFFFFFF);
        }
    }

    /**
     * Draw a colored rectangle.
     */
    private static void drawColoredRect(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + height, color);
    }
    
    /**
     * Draw a colored rectangle with alpha based on animation.
     */
    private static void drawColoredRect(GuiGraphics graphics, int x, int y, int width, int height, 
                                         int color, float alpha) {
        int a = (int)(((color >> 24) & 0xFF) * alpha);
        int finalColor = (a << 24) | (color & 0x00FFFFFF);
        graphics.fill(x, y, x + width, y + height, finalColor);
    }
    
    /**
     * Update the current aura data (called from packet handler).
     */
    public static void updateAura(AuraChunk aura) {
        currentAura = aura;
    }
    
    /**
     * Update aura values directly.
     */
    public static void updateAura(float base, float vis, float flux) {
        if (currentAura == null) {
            currentAura = new AuraChunk(null, (short) 0, vis, flux);
        }
        currentAura.setBase((short) base);
        currentAura.setVis(vis);
        currentAura.setFlux(flux);
    }

    /**
     * TC4 ClientTickEventsFML.renderSanityHud - vertical warp meter drawn in
     * the top-left corner while the Sanity Checker is held.
     *
     * Geometry and colours are copied from the original:
     * - background frame: hud.png (152,0,20x76) at (1,1)
     * - temporary warp: pink (1.0,0.5,1.0) strip at (200, gap)
     * - normal (sticky) warp: (0.75,0,0.75) strip
     * - permanent warp: (0.5,0,0.5) strip
     * - overlay (markers): hud.png (176,0,20x76)
     * - cracked brain icon when the meter is full: hud.png (216,0,20x16)
     */
    private static void renderSanityHud(GuiGraphics graphics, Minecraft mc, Player player) {
        IPlayerWarp warp = ThaumcraftCapabilities.getWarp(player);
        if (warp == null) return;

        int p = warp.get(IPlayerWarp.EnumWarpType.PERMANENT);
        int s = warp.get(IPlayerWarp.EnumWarpType.NORMAL);
        int t = warp.get(IPlayerWarp.EnumWarpType.TEMPORARY);

        float tw = p + s + t;
        float mod = 1.0F;
        if (tw > 100.0F) {
            mod = 100.0F / tw;
            tw = 100.0F;
        }

        int gap = (int) ((100.0F - tw) / 100.0F * 48.0F);
        int wt = (int) (t / 100.0F * 48.0F * mod);
        int ws = (int) (s / 100.0F * 48.0F * mod);

        // Frame
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(HUD_TEXTURE, 1, 1, 152, 0, 20, 76);

        // Temporary warp (pink)
        if (t > 0) {
            RenderSystem.setShaderColor(1.0F, 0.5F, 1.0F, 1.0F);
            graphics.blit(HUD_TEXTURE, 7, 21 + gap, 200, gap, 8, wt + gap);
        }
        // Normal (sticky) warp
        if (s > 0) {
            RenderSystem.setShaderColor(0.75F, 0.0F, 0.75F, 1.0F);
            graphics.blit(HUD_TEXTURE, 7, 21 + wt + gap, 200, wt + gap, 8, wt + ws + gap);
        }
        // Permanent warp
        if (p > 0) {
            RenderSystem.setShaderColor(0.5F, 0.0F, 0.5F, 1.0F);
            graphics.blit(HUD_TEXTURE, 7, 21 + wt + ws + gap, 200, wt + ws + gap, 8, 48);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // Overlay (threshold markers)
        graphics.blit(HUD_TEXTURE, 1, 1, 176, 0, 20, 76);

        // Cracked brain when the meter is completely filled
        if (tw >= 100.0F) {
            graphics.blit(HUD_TEXTURE, 1, 1, 216, 0, 20, 16);
        }
    }
}




