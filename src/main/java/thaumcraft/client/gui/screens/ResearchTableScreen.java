package thaumcraft.client.gui.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchEntry;
import thaumcraft.client.lib.AspectRenderer;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.misc.PacketAspectCombinationToServer;
import thaumcraft.common.lib.network.misc.PacketAspectPlaceToServer;
import thaumcraft.common.lib.research.ResearchNoteData;
import thaumcraft.common.lib.research.ResearchNotes;
import thaumcraft.common.lib.utils.HexUtils;
import thaumcraft.common.menu.ResearchTableMenu;
import thaumcraft.common.tiles.crafting.TileResearchTable;
import thaumcraft.init.ModSounds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * ResearchTableScreen - TC4 hex-grid research minigame GUI.
 * <p>
 * Layout ported 1:1 from OpenTC4 GuiResearchTable (TC4):
 * - Window 255x255, texture guiresearchtable2.png
 *   (0,0,255,167) at (leftPos,topPos) + (0,166,184,88) at (leftPos+40,topPos+167)
 * - Hex canvas centred at (leftPos+169, topPos+83)
 * - Aspect panel 5x5 grid at (leftPos+10, topPos+40), 25 per page, column-major,
 *   with page arrows at (leftPos+27/51, topPos+121)
 * - Combine button (leftPos+35, topPos+139, 32x16), pressed frame (184,168)
 * - Selector previews at (leftPos+13, topPos+139)/(leftPos+71, topPos+139)
 * - Duplicate-copy star at (leftPos+37, topPos+5), copy cost banner on hover
 * - Scribing tools slot (14,10), research note slot (70,10), inventory (48,175)
 *
 * @author Azanor
 */
public class ResearchTableScreen extends AbstractContainerScreen<ResearchTableMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Thaumcraft.MODID, "textures/gui/guiresearchtable2.png");
    private static final ResourceLocation HEX1 = ResourceLocation.fromNamespaceAndPath(
            Thaumcraft.MODID, "textures/gui/hex1.png");
    private static final ResourceLocation HEX2 = ResourceLocation.fromNamespaceAndPath(
            Thaumcraft.MODID, "textures/gui/hex2.png");
    private static final ResourceLocation PARCHMENT = ResourceLocation.fromNamespaceAndPath(
            Thaumcraft.MODID, "textures/misc/parchment3.png");
    private static final ResourceLocation SCRIPT = ResourceLocation.fromNamespaceAndPath(
            Thaumcraft.MODID, "textures/misc/script.png");
    private static final ResourceLocation UNKNOWN = ResourceLocation.fromNamespaceAndPath(
            Thaumcraft.MODID, "textures/aspects/_unknown.png");

    private static final int HEX_SIZE = 9;
    // Canvas half-size in pixels (TC4 parchment is 150x150)
    private static final int CANVAS_HALF = 75;
    // TC4 aspect grid: 5 columns x 5 rows per page, 16px cells
    private static final int ASPECT_PAGE_SIZE = 25;

    // TC4 widget offsets (relative to leftPos/topPos)
    private static final int ASPECT_X = 10;
    private static final int ASPECT_Y = 40;
    private static final int COMBINE_X = 35;
    private static final int COMBINE_Y = 139;
    private static final int SEL1_X = 11;
    private static final int SEL1_Y = 137;
    private static final int SEL2_X = 71;
    private static final int SEL2_Y = 137;
    private static final int ARROW_BACK_X = 27;
    private static final int ARROW_FWD_X = 51;
    private static final int ARROW_Y = 121;
    private static final int DUP_X = 37;
    private static final int DUP_Y = 5;

    private final TileResearchTable tile;
    private final Player player;
    private final IPlayerKnowledge knowledge;

    // Selected aspects for combination
    private Aspect select1 = null;
    private Aspect select2 = null;

    // Drag state
    private int isMouseButtonDown = 0;
    private Aspect draggedAspect = null;

    // Runtime note data
    private ResearchNoteData note = null;

    // Rune decorations
    private final HashMap<String, Rune> runes = new HashMap<>();
    private long lastRuneCheck = 0L;

    // Connection lines between discovered aspects on the hex grid
    private final HashMap<String, HexUtils.Hex[]> lines = new HashMap<>();
    private final ArrayList<String> checked = new ArrayList<>();
    private final ArrayList<String> highlight = new ArrayList<>();

    // Research bonuses
    private final boolean researcher1;
    private final boolean researcher2;
    private final boolean researchDupe;

    // Button cooldown
    private long butcount2 = 0L;

    // Aspect paging (TC4: 25 per page, page/lastPage)
    private int page = 0;
    private int lastPage = 0;

    // Layout anchors (computed on init/resize)
    private int canvasCX = 0;
    private int canvasCY = 0;

    public ResearchTableScreen(ResearchTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.tile = menu.getBlockEntity();
        this.player = playerInventory.player;
        this.knowledge = ThaumcraftCapabilities.getKnowledge(player);

        // Check research bonuses
        researcher1 = knowledge != null && knowledge.isResearchComplete("RESEARCHER1");
        researcher2 = knowledge != null && knowledge.isResearchComplete("RESEARCHER2");
        researchDupe = knowledge != null && knowledge.isResearchComplete("RESEARCHDUPE");
    }

    @Override
    protected void init() {
        this.imageWidth = 255;
        this.imageHeight = 255;
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        this.inventoryLabelY = 999; // Hide default labels
        this.titleLabelY = 999;
        super.init();

        // Center anchor for the hex canvas (TC4: gx+169, gy+83)
        canvasCX = leftPos + 169;
        canvasCY = topPos + 83;
    }

    // ==================== Rendering ====================

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        // Foreground overlays must be drawn with absolute coordinates; renderLabels
        // is translated by (leftPos, topPos), so everything interactive goes here.
        drawForeground(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        drawBackgroundPanel(graphics);
        drawResearchData(graphics, mouseX, mouseY);

        RenderSystem.disableBlend();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Intentionally empty - TC4 draws no labels.
    }

    // ==================== Background ====================

    private void drawBackgroundPanel(GuiGraphics graphics) {
        // Semi-transparent dark overlay behind the GUI
        graphics.fill(0, 0, width, height, 0x88000000);

        // TC4 blit layout: main panel + bottom-left strip for the inventory
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShaderTexture(0, TEXTURE);
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, 255, 167, 256, 256);
        graphics.blit(TEXTURE, leftPos + 40, topPos + 167, 0, 166, 184, 88, 256, 256);

        // Page arrows (only when paging is possible)
        if (page < lastPage) {
            graphics.blit(TEXTURE, leftPos + ARROW_FWD_X, topPos + ARROW_Y, 208, 208, 24, 8, 256, 256);
        }
        if (page > 0) {
            graphics.blit(TEXTURE, leftPos + ARROW_BACK_X, topPos + ARROW_Y, 184, 208, 24, 8, 256, 256);
        }

        // Combine button
        if (select1 != null && select2 != null) {
            graphics.blit(TEXTURE, leftPos + COMBINE_X, topPos + COMBINE_Y, 184, 184, 32, 16, 256, 256);
            if (butcount2 >= System.nanoTime()) {
                graphics.blit(TEXTURE, leftPos + COMBINE_X, topPos + COMBINE_Y, 184, 168, 32, 16, 256, 256);
            } else {
                drawOrb(graphics, leftPos + COMBINE_X + 8, topPos + COMBINE_Y);
            }
        }

        // Duplicate notes star
        if (researchDupe && note != null && note.isComplete()) {
            graphics.blit(TEXTURE, leftPos + DUP_X, topPos + DUP_Y, 232, 200, 24, 24, 256, 256);
        }
    }

    // ==================== Aspect Panel (TC4) ====================

    /**
     * TC4 drawAspects: one 5x5 grid at (leftPos+10, topPos+40), column-major,
     * 25 aspects per page, filled from the player's discovered aspect list.
     */
    private void drawAspectPanel(GuiGraphics graphics) {
        AspectList aspects = discoveredAspects();
        Aspect[] sorted = aspects.getAspectsSorted();
        int count = sorted.length;
        this.lastPage = (count - 20) / 5;
        if (lastPage < 0) {
            lastPage = 0;
        }

        int drawn = 0;
        for (int i = 0; i < count && drawn < ASPECT_PAGE_SIZE; i++) {
            if (i < page * 5) {
                continue;
            }
            Aspect aspect = sorted[i];
            if (aspect == null) {
                continue;
            }
            int xx = drawn / 5 * 16;
            int yy = drawn % 5 * 16;
            int amount = aspects.getAmount(aspect);
            int bonus = tile.bonusAspects.getAmount(aspect);
            boolean faded = amount <= 0 && bonus <= 0;
            AspectRenderer.drawAspect(graphics, leftPos + ASPECT_X + xx, topPos + ASPECT_Y + yy,
                    aspect, amount, bonus, faded ? 0.33f : 1.0f, false);
            drawn++;
        }

        // Clear selections that no longer have any available amount
        if (select1 != null && aspects.getAmount(select1) <= 0 && tile.bonusAspects.getAmount(select1) <= 0) {
            select1 = null;
        }
        if (select2 != null && aspects.getAmount(select2) <= 0 && tile.bonusAspects.getAmount(select2) <= 0) {
            select2 = null;
        }

        // Selection previews (TC4: x+3/y+99 and x+61/y+99 relative to the panel)
        if (select1 != null) {
            AspectRenderer.drawAspect(graphics, leftPos + ASPECT_X + 3, topPos + ASPECT_Y + 99, select1, 0, 0, 1.0f, false);
        }
        if (select2 != null) {
            AspectRenderer.drawAspect(graphics, leftPos + ASPECT_X + 61, topPos + ASPECT_Y + 99, select2, 0, 0, 1.0f, false);
        }
    }

    private AspectList discoveredAspects() {
        return knowledge != null ? knowledge.getResearchPool() : new AspectList();
    }

    /**
     * TC4 getClickedAspect: hit-test the current page of the aspect grid.
     *
     * @param ignoreZero when false only aspects with a positive pool amount (or
     *                   table bonus) can be picked up
     */
    private Aspect getClickedAspect(int mx, int my, boolean ignoreZero) {
        AspectList aspects = discoveredAspects();
        Aspect[] sorted = aspects.getAspectsSorted();
        int count = sorted.length;
        int drawn = 0;
        for (int i = 0; i < count && drawn < ASPECT_PAGE_SIZE; i++) {
            if (i < page * 5) {
                continue;
            }
            Aspect aspect = sorted[i];
            if (aspect == null) {
                continue;
            }
            int xx = drawn / 5 * 16;
            int yy = drawn % 5 * 16;
            if ((ignoreZero || aspects.getAmount(aspect) > 0 || tile.bonusAspects.getAmount(aspect) > 0)
                    && mx >= leftPos + ASPECT_X + xx && my >= topPos + ASPECT_Y + yy
                    && mx < leftPos + ASPECT_X + xx + 16 && my < topPos + ASPECT_Y + yy + 16) {
                return aspect;
            }
            drawn++;
        }
        return null;
    }

    // ==================== Hex Grid ====================

    private void drawResearchData(GuiGraphics graphics, int mouseX, int mouseY) {
        // Refresh note data from the tile entity
        note = ResearchNotes.getData(tile.getItem(TileResearchTable.SLOT_NOTE));
        if (note == null || note.key == null || note.key.isEmpty()) {
            runes.clear();
            drawAspectPanel(graphics);
            return;
        }

        // Draw parchment background (TC4: gx+94, gy+8, 150x150)
        RenderSystem.setShaderTexture(0, PARCHMENT);
        graphics.blit(PARCHMENT, leftPos + ASPECT_X + 84, topPos + ASPECT_Y - 32, 0, 0, CANVAS_HALF * 2, CANVAS_HALF * 2, 150, 150);

        // Update rune decorations
        long time = System.currentTimeMillis();
        if (lastRuneCheck < time) {
            lastRuneCheck = time + 250L;
            Random rand = new Random();
            int k = rand.nextInt(120) - 60;
            int l = rand.nextInt(120) - 60;
            HexUtils.Hex hp = new HexUtils.Pixel(k, l).toHex(HEX_SIZE);
            if (!runes.containsKey(hp.toString()) && !note.hexes.containsKey(hp.toString())) {
                runes.put(hp.toString(), new Rune(hp.q, hp.r, time, time + 15000L + rand.nextInt(10000), rand.nextInt(16)));
            }
        }

        // Draw runes
        if (!runes.isEmpty()) {
            Rune[] rns = runes.values().toArray(new Rune[0]);
            for (Rune rune : rns) {
                if (rune.decay < time) {
                    runes.remove(rune.q + ":" + rune.r);
                } else {
                    HexUtils.Pixel pix = new HexUtils.Hex(rune.q, rune.r).toPixel(HEX_SIZE);
                    float progress = (float) (time - rune.start) / (float) (rune.decay - rune.start);
                    float alpha = 0.5f;
                    if (progress < 0.25f) {
                        alpha = progress * 2.0f;
                    } else if (progress > 0.5f) {
                        alpha = 1.0f - progress;
                    }
                    drawRune(graphics, canvasCX + pix.x, canvasCY + pix.y, rune.rune, alpha * 0.66f);
                }
            }
        }

        // Calculate hex under mouse
        int mouseXRel = mouseX - canvasCX;
        int mouseYRel = mouseY - canvasCY;
        HexUtils.Hex hoverHex = new HexUtils.Pixel(mouseXRel, mouseYRel).toHex(HEX_SIZE);

        // Clear connection data
        lines.clear();
        checked.clear();
        highlight.clear();

        // Check connections for type 1 (knowledge node) hexes the player has discovered
        for (HexUtils.Hex hex : note.hexes.values()) {
            ResearchNotes.HexEntry entry = note.hexEntries.get(hex.toString());
            if (entry != null && entry.type == 1 && entry.aspect != null
                    && ResearchNotes.hasDiscoveredAspect(player, entry.aspect)) {
                checkConnections(hex);
            }
        }

        // Draw connection lines
        for (HexUtils.Hex[] con : lines.values()) {
            HexUtils.Pixel p1 = con[0].toPixel(HEX_SIZE);
            HexUtils.Pixel p2 = con[1].toPixel(HEX_SIZE);
            drawLine(graphics, canvasCX + p1.x, canvasCY + p1.y, canvasCX + p2.x, canvasCY + p2.y);
        }

        // Draw hex grid
        if (!note.isComplete()) {
            for (HexUtils.Hex hex : note.hexes.values()) {
                ResearchNotes.HexEntry entry = note.hexEntries.get(hex.toString());
                if (entry != null && entry.type != 1) {
                    if (hex.equals(hoverHex)) {
                        drawHexHighlight(graphics, hex, canvasCX, canvasCY);
                    }
                    drawHex(graphics, hex, canvasCX, canvasCY);
                } else if (entry != null && entry.type == 1) {
                    // Knowledge node - draw orb
                    drawOrbAtHex(graphics, hex, canvasCX, canvasCY);
                }
            }
        }

        // Draw aspect icons on hexes
        for (HexUtils.Hex hex : note.hexes.values()) {
            ResearchNotes.HexEntry entry = note.hexEntries.get(hex.toString());
            if (entry == null || entry.aspect == null) continue;

            if (!ResearchNotes.hasDiscoveredAspect(player, entry.aspect)) {
                // Unknown aspect - draw black silhouette
                HexUtils.Pixel pix = hex.toPixel(HEX_SIZE);
                RenderSystem.setShaderColor(0.0f, 0.0f, 0.0f, 0.5f);
                RenderSystem.setShaderTexture(0, UNKNOWN);
                graphics.blit(UNKNOWN, (int) ((canvasCX - 8) + pix.x), (int) ((canvasCY - 8) + pix.y), 0, 0, 16, 16, 16, 16);
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            } else if (entry.type != 1 && !highlight.contains(hex.toString())) {
                if (entry.type == 2) {
                    // Placed aspect (type 2) - drawn slightly faded
                    HexUtils.Pixel pix = hex.toPixel(HEX_SIZE);
                    AspectRenderer.drawAspect(graphics, (int) ((canvasCX - 8) + pix.x), (int) ((canvasCY - 8) + pix.y),
                            entry.aspect, 0, 0, 0.66f, false);
                }
            } else {
                // Discovered aspect (type 1) or highlighted
                HexUtils.Pixel pix = hex.toPixel(HEX_SIZE);
                AspectRenderer.drawAspect(graphics, (int) ((canvasCX - 8) + pix.x), (int) ((canvasCY - 8) + pix.y),
                        entry.aspect, 0, 0, 1.0f, false);
            }
        }

        // Aspect panel is drawn on top of the parchment area (TC4 order: after hexes)
        drawAspectPanel(graphics);
    }

    /**
     * Recursively check connections from a hex to its neighbours (TC4 logic:
     * both aspects must be discovered and be components of one another).
     */
    private void checkConnections(HexUtils.Hex hex) {
        checked.add(hex.toString());
        ResearchNotes.HexEntry entry = note.hexEntries.get(hex.toString());
        if (entry == null || entry.aspect == null) return;

        for (int a = 0; a < 6; a++) {
            HexUtils.Hex target = hex.getNeighbour(a);
            ResearchNotes.HexEntry targetEntry = note.hexEntries.get(target.toString());
            if (targetEntry == null || targetEntry.aspect == null) continue;

            if (!checked.contains(target.toString()) && targetEntry.type >= 1) {
                Aspect aspect1 = entry.aspect;
                Aspect aspect2 = targetEntry.aspect;

                boolean has1 = ResearchNotes.hasDiscoveredAspect(player, aspect1);
                boolean has2 = ResearchNotes.hasDiscoveredAspect(player, aspect2);

                if (has1 && has2) {
                    boolean connected = (!aspect1.isPrimal()
                            && ((aspect1.getComponents()[0] == aspect2 || aspect1.getComponents()[1] == aspect2)))
                            || (!aspect2.isPrimal()
                            && ((aspect2.getComponents()[0] == aspect1 || aspect2.getComponents()[1] == aspect1)));

                    if (connected) {
                        String k1 = hex + ":" + target;
                        String k2 = target + ":" + hex;
                        if (!lines.containsKey(k1) && !lines.containsKey(k2)) {
                            lines.put(k1, new HexUtils.Hex[]{hex, target});
                            highlight.add(target.toString());
                        }
                        checkConnections(target);
                    }
                }
            }
        }
    }

    // ==================== Foreground (drag + tooltips) ====================

    private void drawForeground(GuiGraphics graphics, int mouseX, int mouseY) {
        note = ResearchNotes.getData(tile.getItem(TileResearchTable.SLOT_NOTE));

        // Dragged aspect visual (TC4 draws an orb with the aspect colour)
        if (isMouseButtonDown == 1 && draggedAspect != null) {
            int color = draggedAspect.getColor();
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;
            drawOrb(graphics, mouseX - 8, mouseY - 8, r, g, b);
        }

        // Aspect tooltips
        if (drawAspectTooltips(graphics, mouseX, mouseY)) {
            return;
        }

        // Copy-cost banner on hover over the duplicate star (TC4)
        if (researchDupe && note != null && note.isComplete()) {
            int dx = leftPos + DUP_X;
            int dy = topPos + DUP_Y;
            if (mouseX >= dx && mouseY >= dy && mouseX < dx + 24 && mouseY < dy + 24) {
                ResearchEntry rr = ResearchCategories.getResearch(note.key);
                if (rr != null && rr.getRequiredAspects() != null) {
                    graphics.blit(TEXTURE, leftPos + 100, topPos + 21, 184, 224, 48, 16, 256, 256);
                    AspectList al = rr.getRequiredAspects().copy();
                    for (Aspect aspect : al.getAspects()) {
                        al.add(aspect, note.copies);
                    }
                    int count = 0;
                    for (Aspect aspect : al.getAspectsSorted()) {
                        AspectRenderer.drawAspect(graphics, leftPos + 100 + 48 + count * 16, topPos + 21,
                                aspect, al.getAmount(aspect), 0, 1.0f, false);
                        count++;
                    }
                    graphics.drawString(font, Component.translatable("tc.research.copy"),
                            leftPos + 100, topPos + 12, 0xFFFFFF, true);
                }
            }
        }

        // No-ink tooltip (TC4: two lines at gx+157-sx, gy+84)
        ItemStack tools = tile.getItem(TileResearchTable.SLOT_SCRIBING_TOOLS);
        if (note != null && (tools.isEmpty() || tools.getDamageValue() >= tools.getMaxDamage())) {
            List<Component> noink = List.of(
                    Component.translatable("tile.researchtable.noink.0"),
                    Component.translatable("tile.researchtable.noink.1"));
            int sx = Math.max(font.width(noink.get(0)), font.width(noink.get(1))) / 2;
            graphics.renderTooltip(font, noink, java.util.Optional.empty(),
                    leftPos + 157 - sx, topPos + 84);
        }
    }

    /**
     * @return true when a tooltip was drawn
     */
    private boolean drawAspectTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        Aspect aspect = getClickedAspect(mouseX, mouseY, true);
        if (aspect == null) {
            return false;
        }

        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.literal(aspect.getName()));
        tooltip.add(Component.literal(aspect.getLocalizedDescription()));
        graphics.renderTooltip(font, tooltip, java.util.Optional.empty(), mouseX, mouseY);

        // RESEARCHER1 bonus: show component aspects (TC4)
        if (researcher1 && !aspect.isPrimal() && aspect.getComponents() != null) {
            RenderSystem.enableBlend();
            AspectRenderer.drawAspect(graphics, mouseX + 8, mouseY + 8,
                    aspect.getComponents()[0], 0, 0, 1.0f, false);
            AspectRenderer.drawAspect(graphics, mouseX + 26, mouseY + 8,
                    aspect.getComponents()[1], 0, 0, 1.0f, false);
            RenderSystem.disableBlend();
        }
        return true;
    }

    // ==================== Mouse Input ====================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int mx = (int) mouseX;
        int my = (int) mouseY;

        // Combine button
        if (butcount2 <= System.nanoTime() && select1 != null && select2 != null
                && mx >= leftPos + COMBINE_X && my >= topPos + COMBINE_Y
                && mx < leftPos + COMBINE_X + 32 && my < topPos + COMBINE_Y + 16) {
            butcount2 = System.nanoTime() + 200000000L;
            playButtonClick();
            playButtonCombine();
            PacketHandler.sendToServer(new PacketAspectCombinationToServer(
                    tile.getBlockPos(), select1, select2,
                    tile.bonusAspects.getAmount(select1) > 0,
                    tile.bonusAspects.getAmount(select2) > 0));
            return true;
        }

        // Page arrows
        if (page > 0 && mx >= leftPos + ARROW_BACK_X && my >= topPos + ARROW_Y
                && mx < leftPos + ARROW_BACK_X + 24 && my < topPos + ARROW_Y + 8) {
            page--;
            playButtonScroll();
            return true;
        }
        if (page < lastPage && mx >= leftPos + ARROW_FWD_X && my >= topPos + ARROW_Y
                && mx < leftPos + ARROW_FWD_X + 24 && my < topPos + ARROW_Y + 8) {
            page++;
            playButtonScroll();
            return true;
        }

        // Clear selected aspects
        if (select1 != null && mx >= leftPos + SEL1_X && my >= topPos + SEL1_Y
                && mx < leftPos + SEL1_X + 16 && my < topPos + SEL1_Y + 16) {
            select1 = null;
            playButtonAspect();
            return true;
        }
        if (select2 != null && mx >= leftPos + SEL2_X && my >= topPos + SEL2_Y
                && mx < leftPos + SEL2_X + 16 && my < topPos + SEL2_Y + 16) {
            select2 = null;
            playButtonAspect();
            return true;
        }

        // Duplicate note star
        if (researchDupe && note != null && note.isComplete()
                && mx >= leftPos + DUP_X && my >= topPos + DUP_Y
                && mx < leftPos + DUP_X + 24 && my < topPos + DUP_Y + 24) {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 5);
            }
            playButtonClick();
            return true;
        }

        // Click on hex grid to erase a placed aspect
        if (note != null && checkClickedHex(mx, my)) {
            return true;
        }

        // Shift+click on compound aspect -> auto-combine (RESEARCHER2)
        if (hasShiftDown() && researcher2) {
            Aspect aspect = getClickedAspect(mx, my, false);
            if (aspect != null && !aspect.isPrimal() && aspect.getComponents() != null) {
                AspectList pool = discoveredAspects();
                Aspect comp1 = aspect.getComponents()[0];
                Aspect comp2 = aspect.getComponents()[1];
                boolean has1 = pool.getAmount(comp1) > 0 || tile.bonusAspects.getAmount(comp1) > 0;
                boolean has2 = pool.getAmount(comp2) > 0 || tile.bonusAspects.getAmount(comp2) > 0;
                if (has1 && has2) {
                    draggedAspect = null;
                    playButtonCombine();
                    PacketHandler.sendToServer(new PacketAspectCombinationToServer(
                            tile.getBlockPos(), comp1, comp2,
                            tile.bonusAspects.getAmount(comp1) > 0,
                            tile.bonusAspects.getAmount(comp2) > 0));
                    return true;
                }
            }
        }

        // Start dragging an aspect from the aspect grid (TC4: gx+10..+90, gy+40..+120)
        if (mx >= leftPos + ASPECT_X && mx < leftPos + ASPECT_X + 80
                && my >= topPos + ASPECT_Y && my < topPos + ASPECT_Y + 80) {
            Aspect aspect = getClickedAspect(mx, my, false);
            if (aspect != null) {
                playButtonAspect();
                isMouseButtonDown = 1;
                draggedAspect = aspect;
                return true;
            }
        }

        // Do not quick-move on plain left click; let the base class handle slots
        // (shift-click quick-move is handled by vanilla).
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && isMouseButtonDown == 1 && draggedAspect != null) {
            int mx = (int) mouseX;
            int my = (int) mouseY;

            // Dropped on hex grid?
            if (note != null) {
                int mouseXRel = mx - canvasCX;
                int mouseYRel = my - canvasCY;
                HexUtils.Hex hp = new HexUtils.Pixel(mouseXRel, mouseYRel).toHex(HEX_SIZE);
                if (note.hexEntries.containsKey(hp.toString())
                        && note.hexEntries.get(hp.toString()).type == 0) {
                    playButtonCombine();
                    playButtonWrite();
                    PacketHandler.sendToServer(new PacketAspectPlaceToServer(
                            tile.getBlockPos(), (byte) hp.q, (byte) hp.r, draggedAspect));
                    isMouseButtonDown = 0;
                    draggedAspect = null;
                    return true;
                }
            }

            boolean skip = false;

            // Dropped on selection 1? (TC4 drop zone centre: gx+20, gy+146, +/-16)
            if (mx >= leftPos + SEL1_X - 5 && my >= topPos + SEL1_Y - 9
                    && mx < leftPos + SEL1_X + 21 && my < topPos + SEL1_Y + 25) {
                playButtonAspect();
                select1 = draggedAspect;
                skip = true;
            }

            // Dropped on selection 2? (TC4 drop zone centre: gx+79, gy+146)
            if (!skip && mx >= leftPos + SEL2_X - 5 && my >= topPos + SEL2_Y - 9
                    && mx < leftPos + SEL2_X + 21 && my < topPos + SEL2_Y + 25) {
                playButtonAspect();
                select2 = draggedAspect;
                skip = true;
            }

            // Dropped back on the same aspect in the grid -> push into a selector
            if (!skip) {
                Aspect aspect = getClickedAspect(mx, my, false);
                if (aspect == draggedAspect) {
                    if (select1 == null) {
                        select1 = draggedAspect;
                    } else if (select2 == null) {
                        select2 = draggedAspect;
                    }
                }
            }

            isMouseButtonDown = 0;
            draggedAspect = null;
            return true;
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // TC4 wheel paging (ClientProxy.handleMouseInput)
        if (scrollY < 0 && page < lastPage) {
            page++;
            playButtonScroll();
            return true;
        }
        if (scrollY > 0 && page > 0) {
            page--;
            playButtonScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /**
     * Check if a hex on the grid was clicked (for erasing a placed aspect).
     *
     * @return true when an erase packet was sent
     */
    private boolean checkClickedHex(int mx, int my) {
        int mouseX = mx - canvasCX;
        int mouseY = my - canvasCY;
        HexUtils.Hex hp = new HexUtils.Pixel(mouseX, mouseY).toHex(HEX_SIZE);
        if (note.hexEntries.containsKey(hp.toString())
                && note.hexEntries.get(hp.toString()).type == 2) {
            playButtonCombine();
            playButtonErase();
            PacketHandler.sendToServer(new PacketAspectPlaceToServer(
                    tile.getBlockPos(), (byte) hp.q, (byte) hp.r, null));
            return true;
        }
        return false;
    }

    // ==================== Hex Rendering ====================

    private void drawHex(GuiGraphics graphics, HexUtils.Hex hex, int originCX, int originCY) {
        HexUtils.Pixel pix = hex.toPixel(HEX_SIZE);
        int x = (int) (originCX + pix.x) - 8;
        int y = (int) (originCY + pix.y) - 8;

        RenderSystem.setShaderTexture(0, HEX1);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 0.25f);
        RenderSystem.enableBlend();
        graphics.blit(HEX1, x, y, 0, 0, 16, 16, 16, 16);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void drawHexHighlight(GuiGraphics graphics, HexUtils.Hex hex, int originCX, int originCY) {
        HexUtils.Pixel pix = hex.toPixel(HEX_SIZE);
        int x = (int) (originCX + pix.x) - 8;
        int y = (int) (originCY + pix.y) - 8;

        RenderSystem.setShaderTexture(0, HEX2);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
        graphics.blit(HEX2, x, y, 0, 0, 16, 16, 16, 16);
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void drawLine(GuiGraphics graphics, double x1, double y1, double x2, double y2) {
        int count = player.tickCount;
        float alpha = 0.3f + Mth.sin((float) ((float) count + (float) x1)) * 0.3f + 0.3f;

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
        RenderSystem.lineWidth(3.0f);

        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        builder.addVertex(matrix, (float) x1, (float) y1, 0).setColor(0.0f, 0.6f, 0.8f, alpha);
        builder.addVertex(matrix, (float) x2, (float) y2, 0).setColor(0.0f, 0.6f, 0.8f, alpha);
        BufferUploader.drawWithShader(builder.build());

        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableBlend();
    }

    private void drawRune(GuiGraphics graphics, double x, double y, int runeIdx, float alpha) {
        RenderSystem.setShaderTexture(0, SCRIPT);
        RenderSystem.setShaderColor(0.0f, 0.0f, 0.0f, alpha);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.enableBlend();

        // TC4 rotates every rune by 90 degrees (glRotatef(90, 0, 0, -1))
        graphics.pose().pushPose();
        graphics.pose().translate((float) x, (float) y, 0.0f);
        graphics.pose().mulPose(new org.joml.Quaternionf(new AxisAngle4f((float) Math.toRadians(-90.0f), 0.0f, 0.0f, 1.0f)));
        Matrix4f matrix = graphics.pose().last().pose();

        float u = 0.0625f * runeIdx;
        float u2 = u + 0.0625f;

        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.addVertex(matrix, -5.0f, 5.0f, 0).setUv(u2, 1);
        builder.addVertex(matrix, 5.0f, 5.0f, 0).setUv(u2, 0);
        builder.addVertex(matrix, 5.0f, -5.0f, 0).setUv(u, 0);
        builder.addVertex(matrix, -5.0f, -5.0f, 0).setUv(u, 1);
        BufferUploader.drawWithShader(builder.build());
        graphics.pose().popPose();

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private void drawOrbAtHex(GuiGraphics graphics, HexUtils.Hex hex, int originCX, int originCY) {
        HexUtils.Pixel pix = hex.toPixel(HEX_SIZE);
        drawOrb(graphics, (originCX - 8) + pix.x, (originCY - 8) + pix.y);
    }

    private void drawOrb(GuiGraphics graphics, double x, double y) {
        int count = player.tickCount;
        float red = 0.7f + Mth.sin((float) (((float) count + (float) x) / 10.0f)) * 0.15f + 0.15f;
        float green = 0.7f + Mth.sin((float) (((float) count + (float) x + (float) y) / 11.0f)) * 0.15f + 0.15f;
        float blue = 0.7f + Mth.sin((float) (((float) count + (float) y) / 12.0f)) * 0.15f + 0.15f;

        drawOrb(graphics, (int) x, (int) y, red, green, blue);
    }

    private void drawOrb(GuiGraphics graphics, int x, int y) {
        int color = 0xFFFFFF;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        drawOrb(graphics, x, y, r, g, b);
    }

    private void drawOrb(GuiGraphics graphics, int x, int y, float r, float g, float b) {
        int part = player.tickCount % 8;
        float u = 0.5f + (float) part / 8.0f;
        float u2 = u + 0.0624375f;
        float v = 0.5f;
        float v2 = v + 0.0624375f;

        ResourceLocation particleTexture = ResourceLocation.fromNamespaceAndPath(
                "thaumcraft", "textures/misc/particles.png");

        RenderSystem.setShaderTexture(0, particleTexture);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.enableBlend();

        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        builder.addVertex(matrix, x, y + 16, 0).setUv(u2, v2).setColor(r, g, b, 1.0f);
        builder.addVertex(matrix, x + 16, y + 16, 0).setUv(u2, v).setColor(r, g, b, 1.0f);
        builder.addVertex(matrix, x + 16, y, 0).setUv(u, v).setColor(r, g, b, 1.0f);
        builder.addVertex(matrix, x, y, 0).setUv(u, v2).setColor(r, g, b, 1.0f);
        BufferUploader.drawWithShader(builder.build());

        RenderSystem.disableBlend();
    }

    // ==================== Sound Helpers ====================

    private void playButtonClick() {
        if (player != null) {
            player.playSound(ModSounds.CAMERACLACK.get(), 0.4f, 1.0f);
        }
    }

    private void playButtonAspect() {
        if (player != null) {
            player.playSound(ModSounds.HH_OFF.get(), 0.2f, 1.0f + player.level().random.nextFloat() * 0.1f);
        }
    }

    private void playButtonCombine() {
        if (player != null) {
            player.playSound(ModSounds.HH_ON.get(), 0.3f, 1.0f);
        }
    }

    private void playButtonWrite() {
        if (player != null) {
            player.playSound(ModSounds.WRITE.get(), 0.2f, 1.0f);
        }
    }

    private void playButtonErase() {
        if (player != null) {
            player.playSound(ModSounds.ERASE.get(), 0.2f, 1.0f + player.level().random.nextFloat() * 0.1f);
        }
    }

    private void playButtonScroll() {
        if (player != null) {
            player.playSound(ModSounds.KEY.get(), 0.3f, 1.0f);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ==================== Inner Classes ====================

    private static class Rune {
        int q, r;
        long start, decay;
        int rune;

        Rune(int q, int r, long start, long decay, int rune) {
            this.q = q;
            this.r = r;
            this.start = start;
            this.decay = decay;
            this.rune = rune;
        }
    }
}
