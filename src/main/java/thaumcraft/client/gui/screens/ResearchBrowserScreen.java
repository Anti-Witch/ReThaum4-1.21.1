package thaumcraft.client.gui.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.api.research.ResearchEntry;
import thaumcraft.client.lib.AspectRenderer;
import thaumcraft.common.config.ConfigResearch;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.playerdata.PacketSyncProgressToServer;
import thaumcraft.common.lib.network.playerdata.PacketSyncResearchFlagsToServer;
import thaumcraft.init.ModSounds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ResearchBrowserScreen - the Thaumonomicon research browser.
 *
 * <p>This is a 1:1 port of TC4's {@code GuiResearchBrowser} (plus the geometry
 * tweaks TC4Tweaks applies for the tabs), rewritten from scratch so that the
 * rendering matches the original mod:
 *
 * <ul>
 *   <li><b>Window</b>: a fixed 256x230 pane centred on the screen. The frame is
 *       the classic 3x3 nine-slice taken from {@code gui_research.png}
 *       (16x17 corners, texture region 256x230) - see
 *       {@code DrawResearchBrowserBorders#drawResearchBrowserBorders}.</li>
 *   <li><b>Background</b>: TC4 does <i>not</i> tile the category background. It
 *       draws a single 224x196 quad inside the window and scrolls the UV window
 *       over the (512x512) parchment sheet:
 *       {@code vx = (locX - guiMapTop) / |guiMapTop - guiMapBottom| * 288} and
 *       {@code vy = (locY - guiMapLeft) / |guiMapLeft - guiMapRight| * 316}.
 *       TC4 samples it with {@code drawTexturedModalRect(..., vx/2, vy/2, 112, 98)}
 *       under a 2x scale, i.e. a 224x196 texel window displayed 1:1.</li>
 *   <li><b>Links</b>: {@code drawLine} - a wiggling, 3px thick poly-line. Dark
 *       grey for finished research, green when the other end is finished and
 *       blue when neither end is known yet.</li>
 *   <li><b>Nodes</b>: 26x26 frames cut from {@code gui_research.png} at v=230
 *       (round u=54, hidden u=86, hex/secondary u=110, hidden hex u=230,
 *       plain u=0) plus the spiky u=26 overlay, with the 16x16 entry icon at
 *       +3/+3.</li>
 *   <li><b>Tabs</b>: 24x24 tab art (u=152 selected / u=176 unselected) hanging
 *       off both sides of the window, mirrored on the right hand side, with the
 *       dark u=200/u=224 overlay drawn over unselected tabs. Hovering a tab
 *       shows the category name (TC4 has no other "bottom panel").</li>
 * </ul>
 *
 * <p>The map position (guiMapX/guiMapY) is persisted between openings through
 * {@link #lastX}/{@link #lastY}, which {@link ResearchPageScreen} also uses when
 * returning from a research entry page.
 */
@OnlyIn(Dist.CLIENT)
public class ResearchBrowserScreen extends Screen {

    // ==================== TC4 texture / layout constants ====================

    /** TC4's {@code gui_research.png}: window border, node frames and tab art. */
    private static final ResourceLocation FRAMES =
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_research.png");
    /** TC4's {@code ParticleEngine.particleTexture}, used for the "new research" sparkle. */
    private static final ResourceLocation PARTICLES =
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/particles.png");

    /** Full browser pane, exactly like TC4's {@code getResearchBrowserWidth()/Height()}. */
    public static final int PANE_WIDTH = 256;
    public static final int PANE_HEIGHT = 230;
    /** Border thickness of the nine-slice frame ({@code BORDER_WIDTH/HEIGHT} in TC4). */
    public static final int BORDER_WIDTH = 16;
    public static final int BORDER_HEIGHT = 17;
    /** Inner (scrollable) area of the pane: 224x196 in TC4. */
    public static final int VIEW_WIDTH = PANE_WIDTH - 2 * BORDER_WIDTH;
    public static final int VIEW_HEIGHT = PANE_HEIGHT - 2 * BORDER_HEIGHT;

    /** TC4 draws gui_research.png with 1/256 UVs. */
    private static final int TEX_SIZE = 256;
    /** Category backgrounds (gui_researchback*.png) are 512x512 sheets. */
    private static final int BG_SHEET = 512;
    /** Tab icon spacing / maximum number of tabs per side (TC4Tweaks: height / 24). */
    private static final int TAB_SIZE = 24;
    private static final int TABS_PER_SIDE = 9;

    // ==================== Map position (TC4: guiMapX/guiMapY) ====================

    /** Interpolated (rendered) map position. */
    protected double curMapX;
    protected double curMapY;
    /** Current map position. */
    protected double guiMapX;
    protected double guiMapY;
    /** Target map position (smooth scrolling lerps guiMapX/guiMapY towards this). */
    protected double tempMapX;
    protected double tempMapY;

    /** Last map position, kept between openings (TC4's lastX/lastY). */
    public static double lastX = -9999.0;
    public static double lastY = -9999.0;
    private static final double UNSET = -9999.0;

    /** Map bounds for the current category (TC4's guiMapTop/guiMapLeft/Bottom/Right). */
    private int mapMinX;
    private int mapMinY;
    private int mapMaxX;
    private int mapMaxY;

    // ==================== Pane placement ====================

    private int paneX;
    private int paneY;
    private int viewX;
    private int viewY;

    // ==================== Input state ====================

    private boolean isDragging;
    private int lastMouseX;
    private int lastMouseY;

    // ==================== Research data ====================

    private final List<ResearchEntry> research = new ArrayList<>();
    /** Lookup of every registered entry, rebuilt in {@link #updateResearch()}. */
    private final Map<String, ResearchEntry> entryLookup = new HashMap<>();
    /** Entries flagged as "new" (sparkle on the node). */
    private final List<String> highlightedResearch = new ArrayList<>();
    /** Categories containing new research (sparkle on the tab). */
    private final List<String> highlightedTabs = new ArrayList<>();

    static String selectedCategory;
    private ResearchEntry currentHighlight;
    private Player player;
    private IPlayerKnowledge knowledge;

    // ==================== Feedback ====================

    private long popuptime = 0L;
    private String popupmessage = "";
    private long t = 0L;

    public ResearchBrowserScreen() {
        this(lastX, lastY);
    }

    public ResearchBrowserScreen(double x, double y) {
        super(Component.translatable("gui.thaumcraft.thaumonomicon"));
        this.tempMapX = this.guiMapX = this.curMapX = x;
        this.tempMapY = this.guiMapY = this.curMapY = y;
    }

    // ==================== Setup ====================

    @Override
    protected void init() {
        super.init();
        this.player = Minecraft.getInstance().player;
        this.knowledge = this.player != null ? ThaumcraftCapabilities.getKnowledge(this.player) : null;

        this.paneX = (this.width - PANE_WIDTH) / 2;
        this.paneY = (this.height - PANE_HEIGHT) / 2;
        this.viewX = this.paneX + BORDER_WIDTH;
        this.viewY = this.paneY + BORDER_HEIGHT;

        this.updateResearch();

        // Software renderers (Mesa llvmpipe, no GPU) apply linear filtering /
        // mipmaps to these 512x512 GUI sheets, which makes the whole
        // Thaumonomicon look blurry. 1.7.10 sampled them with NEAREST, so force
        // that explicitly.
        net.minecraft.client.renderer.texture.TextureManager textureManager =
                Minecraft.getInstance().getTextureManager();
        for (ResearchCategory category : ResearchCategories.getCategories()) {
            if (category.background != null) {
                textureManager.getTexture(category.background).setFilter(false, false);
            }
            if (category.background2 != null) {
                textureManager.getTexture(category.background2).setFilter(false, false);
            }
        }
        textureManager.getTexture(FRAMES).setFilter(false, false);
        textureManager.getTexture(PARTICLES).setFilter(false, false);
    }

    /**
     * Rebuilds the research list and the map bounds for the selected category.
     * TC4's {@code GuiResearchBrowser#updateResearch()}.
     */
    public void updateResearch() {
        this.updateResearch(false);
    }

    private void updateResearch(boolean recenter) {
        this.research.clear();
        this.entryLookup.clear();
        this.highlightedResearch.clear();
        this.highlightedTabs.clear();

        if (this.player == null) {
            this.player = Minecraft.getInstance().player;
        }
        this.knowledge = this.player != null ? ThaumcraftCapabilities.getKnowledge(this.player) : null;

        for (ResearchCategory cat : ResearchCategories.researchCategories.values()) {
            for (ResearchEntry entry : cat.research.values()) {
                this.entryLookup.put(entry.getKey(), entry);
            }
        }

        // TC4 picks the first visible tab if none is selected yet.
        List<String> tabs = getVisibleTabs();
        if (selectedCategory == null
                || ResearchCategories.getResearchCategory(selectedCategory) == null
                || (!tabs.isEmpty() && !tabs.contains(selectedCategory))) {
            selectedCategory = tabs.isEmpty() ? "BASICS" : tabs.get(0);
        }

        ResearchCategory cat = ResearchCategories.getResearchCategory(selectedCategory);
        if (cat != null) {
            this.research.addAll(cat.research.values());
        }
        this.computeMapBounds(cat);

        // TC4 clamps the view when the category changes so the new tree is visible.
        this.guiMapX = Mth.clamp(this.guiMapX, this.mapMinX, Math.max(this.mapMinX, this.mapMaxX - 1));
        this.guiMapY = Mth.clamp(this.guiMapY, this.mapMinY, Math.max(this.mapMinY, this.mapMaxY - 1));
        this.tempMapX = this.guiMapX;
        this.tempMapY = this.guiMapY;
        this.curMapX = this.guiMapX;
        this.curMapY = this.guiMapY;

        // First opening, or switching to another category: show its research.
        if (recenter || lastX == UNSET || lastY == UNSET) {
            this.centerOnCategory();
        }

        // TC4 marks freshly unlocked research with a sparkle (highlightedItem).
        if (this.knowledge != null) {
            for (ResearchEntry entry : this.research) {
                if (this.knowledge.hasResearchFlag(entry.getKey(), IPlayerKnowledge.EnumResearchFlag.RESEARCH)) {
                    this.highlightedResearch.add(entry.getKey());
                    if (!this.highlightedTabs.contains(entry.getCategory())) {
                        this.highlightedTabs.add(entry.getCategory());
                    }
                }
            }
        }
    }

    /** TC4: {@code guiMapTop/Left/Bottom/Right} from the category's display bounds. */
    private void computeMapBounds(ResearchCategory cat) {
        if (cat == null) {
            this.mapMinX = -85;
            this.mapMinY = -112;
        } else {
            this.mapMinX = cat.minDisplayColumn * 24 - 85;
            this.mapMinY = cat.minDisplayRow * 24 - 112;
            this.mapMaxX = cat.maxDisplayColumn * 24 - 112;
            this.mapMaxY = cat.maxDisplayRow * 24 - 61;
        }
        // Guard against empty/degenerate categories so the clamp maths stay sane.
        if (this.mapMaxX <= this.mapMinX) {
            this.mapMaxX = this.mapMinX + VIEW_WIDTH;
        }
        if (this.mapMaxY <= this.mapMinY) {
            this.mapMaxY = this.mapMinY + VIEW_HEIGHT;
        }
    }

    /**
     * Centres the viewport on the research that is actually shown (TC4's own
     * default position simply centres the whole category, hidden entries
     * included, which can leave the viewport on empty parchment). Falls back to
     * the category bounds when nothing is visible yet.
     */
    private void centerOnCategory() {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (ResearchEntry entry : this.research) {
            if (!this.isShown(entry)) {
                continue;
            }
            minX = Math.min(minX, entry.getDisplayColumn() * 24);
            maxX = Math.max(maxX, entry.getDisplayColumn() * 24);
            minY = Math.min(minY, entry.getDisplayRow() * 24);
            maxY = Math.max(maxY, entry.getDisplayRow() * 24);
        }
        if (minX == Integer.MAX_VALUE) {
            // Nothing visible: fall back to the middle of the category bounds.
            this.guiMapX = this.tempMapX = this.curMapX = (this.mapMinX + this.mapMaxX) / 2.0;
            this.guiMapY = this.tempMapY = this.curMapY = (this.mapMinY + this.mapMaxY) / 2.0;
            return;
        }
        double centerX = (minX + maxX) / 2.0 - VIEW_WIDTH / 2.0 + 11;
        double centerY = (minY + maxY) / 2.0 - VIEW_HEIGHT / 2.0 + 11;
        this.guiMapX = this.tempMapX = this.curMapX =
                Mth.clamp(centerX, this.mapMinX, Math.max(this.mapMinX, this.mapMaxX - 1));
        this.guiMapY = this.tempMapY = this.curMapY =
                Mth.clamp(centerY, this.mapMinY, Math.max(this.mapMinY, this.mapMaxY - 1));
    }

    /**
     * The categories whose tabs are currently shown. TC4 hides the ELDRITCH tab
     * until the eldritch epiphany (ELDRITCHMINOR) has been reached; every other
     * TC4 category tab is always available (TC4 categories have no unlock
     * research of their own).
     */
    private List<String> getVisibleTabs() {
        List<String> tabs = new ArrayList<>();
        for (String key : ResearchCategories.researchCategories.keySet()) {
            ResearchCategory cat = ResearchCategories.getResearchCategory(key);
            if (cat == null) {
                continue;
            }
            if ("ELDRITCH".equals(key) && !isResearchComplete("ELDRITCHMINOR")) {
                continue;
            }
            if (!isCoreCategory(key) && cat.researchKey != null && !isResearchComplete(cat.researchKey)) {
                continue;
            }
            tabs.add(key);
            if (tabs.size() >= TABS_PER_SIDE * 2) {
                break;
            }
        }
        return tabs;
    }

    private static boolean isCoreCategory(String key) {
        for (String tc : ConfigResearch.TC_CATEGORIES) {
            if (tc.equals(key)) {
                return true;
            }
        }
        return false;
    }

    private boolean isResearchComplete(String key) {
        return this.knowledge != null && this.knowledge.isResearchComplete(key);
    }

    private boolean isResearchKnown(String key) {
        return this.knowledge != null && this.knowledge.isResearchKnown(key);
    }

    /**
     * TC4's virtual research ("!trigger" entries) never shows up in the browser.
     */
    private boolean isVirtual(ResearchEntry entry) {
        return entry.getKey().startsWith("!");
    }

    /**
     * TC4 node visibility: completed or known research always shows, hidden
     * research only shows once it has been discovered or flagged as "new"
     * (TC4 tests {@code completedResearch.contains("@" + key)} for that).
     * Concealed research (and "lost" research, which TC4 only allows to be
     * discovered by scanning) additionally requires every parent to be done.
     */
    private boolean isShown(ResearchEntry entry) {
        if (entry == null || this.isVirtual(entry)) {
            return false;
        }
        if (this.isResearchComplete(entry.getKey()) || this.isResearchKnown(entry.getKey())) {
            return true;
        }
        if (this.hasNewFlag(entry.getKey())) {
            return true;
        }
        if (entry.hasMeta(ResearchEntry.EnumResearchMeta.CONCEALED)
                || entry.hasMeta(ResearchEntry.EnumResearchMeta.LOST)) {
            return this.canUnlockResearch(entry);
        }
        return !entry.hasMeta(ResearchEntry.EnumResearchMeta.HIDDEN);
    }

    /** TC4's "@key" marker: freshly unlocked/seen research is drawn even if hidden. */
    private boolean hasNewFlag(String key) {
        return this.knowledge != null
                && (this.knowledge.hasResearchFlag(key, IPlayerKnowledge.EnumResearchFlag.RESEARCH)
                || this.knowledge.hasResearchFlag(key, IPlayerKnowledge.EnumResearchFlag.PAGE));
    }

    /**
     * TC4's {@code canUnlockResearch}: all parents - visible and hidden - must
     * be completed. The "@N" suffix some data sets use is ignored.
     */
    private boolean canUnlockResearch(ResearchEntry entry) {
        if (!parentsComplete(entry.getParents(), entry.getParentsClean())) {
            return false;
        }
        return parentsComplete(entry.getParentsHidden(), entry.getParentsHiddenClean());
    }

    private boolean parentsComplete(String[] rawParents, String[] cleanParents) {
        if (rawParents == null || rawParents.length == 0) {
            return true;
        }
        for (int i = 0; i < rawParents.length; i++) {
            String raw = rawParents[i];
            if (raw == null || raw.isEmpty() || raw.startsWith("~")) {
                // "~parent" is a hidden requirement: it is checked by the
                // parents_hidden list instead.
                continue;
            }
            String parent = cleanParents != null && i < cleanParents.length ? cleanParents[i] : raw;
            if (parent.startsWith("!")) {
                // "!" keys are scan/unlock triggers - knowing them is enough.
                if (!this.isResearchKnown(parent)) {
                    return false;
                }
                continue;
            }
            if (!this.isResearchComplete(parent)) {
                return false;
            }
        }
        return true;
    }

    private ResearchEntry getEntry(String key) {
        if (key == null) {
            return null;
        }
        ResearchEntry entry = this.entryLookup.get(key);
        return entry != null ? entry : ResearchCategories.getResearch(key);
    }

    @Override
    public void onClose() {
        lastX = this.guiMapX;
        lastY = this.guiMapY;
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ==================== Ticking ====================

    @Override
    public void tick() {
        super.tick();
        this.curMapX = this.guiMapX;
        this.curMapY = this.guiMapY;
        // TC4's updateScreen(): exponential ease towards the target position.
        double dx = this.tempMapX - this.guiMapX;
        double dy = this.tempMapY - this.guiMapY;
        if (dx * dx + dy * dy < 4.0) {
            this.guiMapX += dx;
            this.guiMapY += dy;
        } else {
            this.guiMapX += dx * 0.85;
            this.guiMapY += dy * 0.85;
        }
    }

    // ==================== Input ====================

    /** TC4 drag region: a little wider than the visible viewport. */
    private boolean isInDragArea(double mouseX, double mouseY) {
        return mouseX >= this.paneX + 8 && mouseX < this.paneX + 8 + VIEW_WIDTH
                && mouseY >= this.viewY && mouseY < this.viewY + VIEW_HEIGHT;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.popuptime = System.currentTimeMillis() - 1L;

        if (button == 0 && this.isInDragArea(mouseX, mouseY)) {
            this.isDragging = true;
            this.lastMouseX = (int) mouseX;
            this.lastMouseY = (int) mouseY;
        }

        if (button == 0) {
            // --- research node click (TC4 mouseClicked) ---
            if (this.currentHighlight != null) {
                ResearchEntry entry = this.currentHighlight;
                if (!this.isResearchComplete(entry.getKey()) && this.canUnlockResearch(entry)) {
                    // Start/continue the research and open its page.
                    PacketHandler.sendToServer(new PacketSyncProgressToServer(entry.getKey(), true));
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new ResearchPageScreen(entry, null, this.guiMapX, this.guiMapY));
                    }
                    this.popuptime = System.currentTimeMillis() + 3000L;
                    this.popupmessage = Component.translatable("tc.research.popup",
                            entry.getLocalizedName()).getString();
                    return true;
                } else if (this.isResearchKnown(entry.getKey())) {
                    // View an existing entry: clear the "new" flags and sync them.
                    if (this.knowledge != null) {
                        this.knowledge.clearResearchFlag(entry.getKey(), IPlayerKnowledge.EnumResearchFlag.RESEARCH);
                        this.knowledge.clearResearchFlag(entry.getKey(), IPlayerKnowledge.EnumResearchFlag.PAGE);
                    }
                    PacketHandler.sendToServer(new PacketSyncResearchFlagsToServer(entry.getKey(), false, false, false));
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new ResearchPageScreen(entry, null, this.guiMapX, this.guiMapY));
                    }
                    return true;
                }
            }

            // --- tab click (TC4 mouseClicked) ---
            if (this.handleTabClick(mouseX, mouseY)) {
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleTabClick(double mouseX, double mouseY) {
        List<String> tabs = getVisibleTabs();
        int count = 0;
        boolean rightSide = false;
        for (String key : tabs) {
            if (count == TABS_PER_SIDE) {
                count = 0;
                rightSide = true;
            }
            int tabX = rightSide ? this.paneX + PANE_WIDTH : this.paneX - TAB_SIZE;
            int tabY = this.paneY + count * TAB_SIZE;
            if (mouseX >= tabX && mouseX < tabX + TAB_SIZE
                    && mouseY >= tabY && mouseY < tabY + TAB_SIZE) {
                selectedCategory = key;
                this.updateResearch(true);
                this.playButtonClick();
                return true;
            }
            count++;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isDragging && button == 0) {
            // TC4: guiMapX -= mx - mouseX
            this.guiMapX -= mouseX - this.lastMouseX;
            this.guiMapY -= mouseY - this.lastMouseY;
            this.lastMouseX = (int) mouseX;
            this.lastMouseY = (int) mouseY;
            this.tempMapX = this.guiMapX;
            this.tempMapY = this.guiMapY;
            this.clampMap();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.isDragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /** TC4 clamps the map position to the category bounds while dragging. */
    private void clampMap() {
        if (this.guiMapX < this.mapMinX) {
            this.guiMapX = this.mapMinX;
        }
        if (this.guiMapY < this.mapMinY) {
            this.guiMapY = this.mapMinY;
        }
        if (this.guiMapX >= this.mapMaxX) {
            this.guiMapX = this.mapMaxX - 1;
        }
        if (this.guiMapY >= this.mapMaxY) {
            this.guiMapY = this.mapMaxY - 1;
        }
    }

    /** TC4 plays "thaumcraft:cameraclack" when a tab is selected. */
    private void playButtonClick() {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.playSound(ModSounds.CAMERACLACK.get(), 0.4F, 1.0F);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.minecraft != null && keyCode == this.minecraft.options.keyInventory.getKey().getValue()) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ==================== Rendering ====================

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        this.t = System.nanoTime() / 50000000L;

        // TC4 interpolates between the previous and current map position.
        int locX = Mth.floor(this.curMapX + (this.guiMapX - this.curMapX) * partialTick);
        int locY = Mth.floor(this.curMapY + (this.guiMapY - this.curMapY) * partialTick);
        locX = Mth.clamp(locX, this.mapMinX, Math.max(this.mapMinX, this.mapMaxX - 1));
        locY = Mth.clamp(locY, this.mapMinY, Math.max(this.mapMinY, this.mapMaxY - 1));

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Clip the map contents to the inner window (TC4 relies on the frame
        // being drawn over them, which leaves stray links outside the window).
        graphics.enableScissor(this.viewX, this.viewY, this.viewX + VIEW_WIDTH, this.viewY + VIEW_HEIGHT);
        this.renderMapBackground(graphics, locX, locY);
        this.renderConnections(graphics, locX, locY);
        this.renderNodes(graphics, locX, locY, mouseX, mouseY);
        graphics.disableScissor();

        this.renderTabs(graphics, mouseX, mouseY);
        this.renderBorder(graphics);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();

        this.renderPopup(graphics);
        if (this.currentHighlight != null) {
            this.renderResearchTooltip(graphics, mouseX, mouseY);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /**
     * TC4's {@code drawResearchBrowserBackground}: a single quad showing a
     * 224x196 texel window of the 512x512 category sheet. The window scrolls
     * with the map position so the parchment appears to move with the tree.
     */
    private void renderMapBackground(GuiGraphics graphics, int locX, int locY) {
        ResearchCategory cat = ResearchCategories.getResearchCategory(selectedCategory);
        if (cat == null || cat.background == null) {
            return;
        }
        int vx = (int) ((float) (locX - this.mapMinX)
                / (float) Math.abs(this.mapMinX - this.mapMaxX) * 288.0F);
        int vy = (int) ((float) (locY - this.mapMinY)
                / (float) Math.abs(this.mapMinY - this.mapMaxY) * 316.0F);
        // Keep the sampling window inside the sheet.
        vx = Mth.clamp(vx, 0, BG_SHEET - VIEW_WIDTH);
        vy = Mth.clamp(vy, 0, BG_SHEET - VIEW_HEIGHT);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(cat.background, this.viewX, this.viewY, VIEW_WIDTH, VIEW_HEIGHT,
                (float) vx, (float) vy, VIEW_WIDTH, VIEW_HEIGHT, BG_SHEET, BG_SHEET);

        // Optional foreground layer (TC6 feature, kept for addon compatibility).
        if (cat.background2 != null) {
            graphics.blit(cat.background2, this.viewX, this.viewY, VIEW_WIDTH, VIEW_HEIGHT,
                    (float) vx, (float) vy, VIEW_WIDTH, VIEW_HEIGHT, BG_SHEET, BG_SHEET);
        }
    }

    /**
     * TC4's parent/sibling link rendering ({@code genResearchBackground}).
     */
    private void renderConnections(GuiGraphics graphics, int locX, int locY) {
        for (ResearchEntry entry : this.research) {
            if (entry.getParents() != null) {
                String[] clean = entry.getParentsClean();
                for (int a = 0; a < entry.getParents().length; a++) {
                    String raw = entry.getParents()[a];
                    String parentKey = clean != null && a < clean.length ? clean[a] : raw;
                    if (raw == null || raw.startsWith("~")) {
                        // "~parent" is a hidden requirement: no line is drawn.
                        continue;
                    }
                    ResearchEntry parent = this.getEntry(parentKey);
                    if (parent == null || this.isVirtual(parent)) {
                        continue;
                    }
                    if (!selectedCategory.equals(parent.getCategory())) {
                        continue;
                    }
                    this.drawLink(graphics, entry, parent, locX, locY, 0.1F, 0.1F, 0.1F);
                }
            }

            if (entry.getSiblings() != null) {
                for (String siblingKey : entry.getSiblings()) {
                    if (siblingKey == null || siblingKey.startsWith("~")) {
                        continue;
                    }
                    ResearchEntry sibling = this.getEntry(siblingKey);
                    if (sibling == null || this.isVirtual(sibling)) {
                        continue;
                    }
                    if (!selectedCategory.equals(sibling.getCategory())) {
                        continue;
                    }
                    // TC4 skips the duplicate when the sibling already lists this
                    // entry as its parent.
                    if (sibling.getParents() != null
                            && java.util.Arrays.asList(sibling.getParents()).contains(entry.getKey())) {
                        continue;
                    }
                    this.drawLink(graphics, entry, sibling, locX, locY, 0.1F, 0.1F, 0.2F);
                }
            }
        }
    }

    /** Resolves the colour of a link and draws it. */
    private void drawLink(GuiGraphics graphics, ResearchEntry from, ResearchEntry to,
                          int locX, int locY, float darkR, float darkG, float darkB) {
        int x1 = this.viewX + from.getDisplayColumn() * 24 - locX + 11;
        int y1 = this.viewY + from.getDisplayRow() * 24 - locY + 11;
        int x2 = this.viewX + to.getDisplayColumn() * 24 - locX + 11;
        int y2 = this.viewY + to.getDisplayRow() * 24 - locY + 11;

        if (this.isResearchComplete(from.getKey())) {
            // Both ends known: plain dark line.
            this.drawLine(graphics, x1, y1, x2, y2, darkR, darkG, darkB, false);
        } else if (this.isShown(from)) {
            if (this.isResearchComplete(to.getKey())) {
                this.drawLine(graphics, x1, y1, x2, y2, 0.0F, 1.0F, 0.0F, true);
            } else if (this.isShown(to)) {
                this.drawLine(graphics, x1, y1, x2, y2, 0.0F, 0.0F, 1.0F, true);
            }
        }
    }

    /**
     * TC4's {@code drawLine}: a 3px poly-line that wiggles for undiscovered
     * research. Drawn as a triangle/quad strip so the thickness survives on
     * modern GL where line width is clamped to 1.
     */
    private void drawLine(GuiGraphics graphics, int x, int y, int x2, int y2,
                          float r, float g, float b, boolean wiggle) {
        float count = (float) (this.player != null ? this.player.tickCount : 0);
        double d3 = x - x2;
        double d4 = y - y2;
        float dist = Mth.sqrt((float) (d3 * d3 + d4 * d4));
        int inc = (int) (dist / 2.0F);
        if (inc < 1) {
            inc = 1;
        }
        float dx = (float) (d3 / inc);
        float dy = (float) (d4 / inc);
        if (Math.abs(d3) > Math.abs(d4)) {
            dx *= 2.0F;
        } else {
            dy *= 2.0F;
        }

        float[] points = new float[(inc + 1) * 2];
        float[] colors = new float[(inc + 1) * 4];
        // TC4 drawLine positions each vertex as "x - dx * a" where dx is
        // multiplied down after every step (NOT accumulated):
        //   var12.addVertex(x - dx*a + mx, ...); dx *= 1 - 1/(inc*1.5);
        for (int a = 0; a <= inc; a++) {
            float phase = (float) a / (float) inc;
            float r2 = r;
            float g2 = g;
            float b2 = b;
            float mx = 0.0F;
            float my = 0.0F;
            float alpha = 0.6F;
            if (wiggle) {
                mx = Mth.sin((count + (float) a) / 7.0F) * 5.0F * (1.0F - phase);
                my = Mth.sin((count + (float) a) / 5.0F) * 5.0F * (1.0F - phase);
                r2 = r * (1.0F - phase);
                g2 = g * (1.0F - phase);
                b2 = b * (1.0F - phase);
                alpha *= phase;
            }
            points[a * 2] = x - dx * (float) a + mx;
            points[a * 2 + 1] = y - dy * (float) a + my;
            colors[a * 4] = r2;
            colors[a * 4 + 1] = g2;
            colors[a * 4 + 2] = b2;
            colors[a * 4 + 3] = alpha;
            if (Math.abs(d3) > Math.abs(d4)) {
                dx *= 1.0F - 1.0F / ((float) inc * 1.5F);
            } else {
                dy *= 1.0F - 1.0F / ((float) inc * 1.5F);
            }
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        final float half = 1.5F;
        for (int a = 0; a < inc; a++) {
            float ax = points[a * 2];
            float ay = points[a * 2 + 1];
            float bx = points[(a + 1) * 2];
            float by = points[(a + 1) * 2 + 1];
            float vx = bx - ax;
            float vy = by - ay;
            float len = Mth.sqrt(vx * vx + vy * vy);
            if (len < 0.0001F) {
                continue;
            }
            float nx = -vy / len * half;
            float ny = vx / len * half;
            float r2 = colors[a * 4];
            float g2 = colors[a * 4 + 1];
            float b2 = colors[a * 4 + 2];
            float a2 = colors[a * 4 + 3];
            buffer.addVertex(matrix, ax + nx, ay + ny, 0.0F).setColor(r2, g2, b2, a2);
            buffer.addVertex(matrix, bx + nx, by + ny, 0.0F).setColor(r2, g2, b2, a2);
            buffer.addVertex(matrix, bx - nx, by - ny, 0.0F).setColor(r2, g2, b2, a2);
            buffer.addVertex(matrix, ax - nx, ay - ny, 0.0F).setColor(r2, g2, b2, a2);
        }
        BufferUploader.drawWithShader(buffer.build());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * TC4's node rendering: frame + optional spiky overlay + entry icon, dimmed
     * or pulsing depending on whether the research can be unlocked.
     */
    private void renderNodes(GuiGraphics graphics, int locX, int locY, int mouseX, int mouseY) {
        this.currentHighlight = null;

        // TC4 re-enables blending for the node art: the 26x26 frames, the spiky
        // overlay and the "new research" sparkle all have transparent pixels
        // (their RGB is black), so without blending they would be painted as
        // black boxes. drawLine() leaves blending disabled behind it.
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        for (ResearchEntry entry : this.research) {
            if (this.isVirtual(entry)) {
                continue;
            }
            int relX = entry.getDisplayColumn() * 24 - locX;
            int relY = entry.getDisplayRow() * 24 - locY;
            if (relX < -24 || relY < -24 || relX > VIEW_WIDTH || relY > VIEW_HEIGHT) {
                continue;
            }
            if (!this.isShown(entry)) {
                continue;
            }

            int frameX = this.viewX + relX - 2;
            int frameY = this.viewY + relY - 2;
            boolean complete = this.isResearchComplete(entry.getKey());
            boolean canUnlock = this.canUnlockResearch(entry);

            float brightness;
            if (complete) {
                brightness = 1.0F;
            } else if (canUnlock) {
                brightness = (float) Math.sin((double) (System.currentTimeMillis() % 600L) / 600.0 * Math.PI * 2.0) * 0.25F + 0.75F;
            } else {
                brightness = 0.3F;
            }

            RenderSystem.setShaderColor(brightness, brightness, brightness, 1.0F);

            // Frame (26x26, v=230 of gui_research.png).
            graphics.blit(FRAMES, frameX, frameY, this.getFrameU(entry), 230, 26, 26, TEX_SIZE, TEX_SIZE);
            // Spiky overlay for "important" research (TC4 isSpecial()).
            if (entry.hasMeta(ResearchEntry.EnumResearchMeta.SPIKY)) {
                graphics.blit(FRAMES, frameX, frameY, 26, 230, 26, 26, TEX_SIZE, TEX_SIZE);
            }

            // "New research" sparkle (TC4 highlightedItem), drawn between the
            // frame and the icon exactly like TC4 does.
            if (this.highlightedResearch.contains(entry.getKey())) {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                int px = (int) (this.t % 16L) * 16;
                graphics.blit(PARTICLES, frameX - 3, frameY - 3, px, 80, 16, 16, TEX_SIZE, TEX_SIZE);
            }

            // Locked research shows a very dark icon: TC4 draws texture icons
            // with a 0.2 colour modulator and item icons with renderWithColor
            // disabled. Item rendering ignores the shader colour in modern MC,
            // so the icon is darkened with an overlay instead.
            if (!canUnlock && !complete) {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                this.drawIcon(graphics, entry, frameX + 5, frameY + 5);
                graphics.fill(frameX + 5, frameY + 5, frameX + 21, frameY + 21, 0xCC000000);
            } else {
                RenderSystem.setShaderColor(brightness, brightness, brightness, 1.0F);
                this.drawIcon(graphics, entry, frameX + 5, frameY + 5);
            }

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            // Hover detection: TC4 tests a 22x22 box anchored at the node
            // origin (var42 = frameX + 2), i.e. frameX + 2 .. frameX + 24.
            if (mouseX >= this.viewX && mouseY >= this.viewY
                    && mouseX < this.viewX + VIEW_WIDTH && mouseY < this.viewY + VIEW_HEIGHT
                    && mouseX >= frameX + 2 && mouseX <= frameX + 24
                    && mouseY >= frameY + 2 && mouseY <= frameY + 24) {
                this.currentHighlight = entry;
            }
        }
    }

    /**
     * Frame selection, matching TC4's use of {@code Config.researchDifficulty}
     * (the port runs in "normal" difficulty where secondary research uses the
     * hexagonal frames).
     */
    private int getFrameU(ResearchEntry entry) {
        boolean secondary = entry.hasMeta(ResearchEntry.EnumResearchMeta.HEX)
                || entry.hasMeta(ResearchEntry.EnumResearchMeta.SECONDARY);
        if (entry.hasMeta(ResearchEntry.EnumResearchMeta.ROUND)) {
            return 54;
        }
        if (entry.hasMeta(ResearchEntry.EnumResearchMeta.HIDDEN)) {
            return secondary ? 230 : 86;
        }
        return secondary ? 110 : 0;
    }

    /**
     * Draws the entry icon. TC4 cycles through all icons of an entry and
     * supports both item stacks and texture resources (plus aspects).
     */
    private void drawIcon(GuiGraphics graphics, ResearchEntry entry, int x, int y) {
        Object[] icons = entry.getIcons();
        if (icons == null || icons.length == 0) {
            return;
        }
        int index = (int) (System.currentTimeMillis() / 1000L % icons.length);
        Object icon = icons[index];
        if (icon == null) {
            return;
        }
        if (icon instanceof ItemStack stack) {
            if (!stack.isEmpty()) {
                graphics.renderItem(stack, x, y);
            }
        } else if (icon instanceof ResourceLocation texture) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            graphics.blit(asTexture(texture), x, y, 0, 0, 16, 16, 16, 16);
        } else if (icon instanceof Aspect aspect) {
            AspectRenderer.drawAspect(graphics, x, y, aspect);
        }
    }

    /**
     * TC4 entry icons are full texture paths ({@code textures/.../foo.png}) and
     * that is what a direct texture bind needs. Addon data occasionally stores
     * the atlas style form ({@code foo}), so complete it here instead of
     * sampling a non-existent texture.
     */
    private static ResourceLocation asTexture(ResourceLocation loc) {
        String path = loc.getPath();
        boolean changed = false;
        if (!path.startsWith("textures/")) {
            path = "textures/" + path;
            changed = true;
        }
        if (!path.endsWith(".png")) {
            path = path + ".png";
            changed = true;
        }
        return changed ? ResourceLocation.fromNamespaceAndPath(loc.getNamespace(), path) : loc;
    }

    /**
     * TC4's category tabs: 24x24 art hanging off both sides of the window,
     * mirrored on the right, with a dark overlay over unselected tabs.
     */
    private void renderTabs(GuiGraphics graphics, int mouseX, int mouseY) {
        List<String> tabs = getVisibleTabs();
        int count = 0;
        boolean rightSide = false;
        String hoveredTab = null;

        // TC4 enables blending for the tab art (rounded tabs with transparent
        // corners, plus the dark "pushed in" overlay and the sparkle).
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        for (String key : tabs) {
            if (count == TABS_PER_SIDE) {
                count = 0;
                rightSide = true;
            }
            ResearchCategory cat = ResearchCategories.getResearchCategory(key);
            if (cat == null) {
                continue;
            }

            boolean selected = key.equals(selectedCategory);
            int s0 = rightSide ? PANE_WIDTH + 8 : 0;
            int s1 = selected ? 0 : 24;
            int s2 = rightSide ? (selected ? 14 : 6) : (selected ? 0 : 8);
            int tabX = rightSide ? this.paneX + s0 - 8 : this.paneX - TAB_SIZE + s0;
            int tabY = this.paneY + count * TAB_SIZE;

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            // Tab background.
            if (rightSide) {
                this.blitMirrored(graphics, tabX, tabY, 152 + s1, 232, TAB_SIZE, TAB_SIZE);
            } else {
                graphics.blit(FRAMES, tabX, tabY, 152 + s1, 232, TAB_SIZE, TAB_SIZE, TEX_SIZE, TEX_SIZE);
            }

            // Sparkle for categories containing new research.
            if (this.highlightedTabs.contains(key)) {
                int px = (int) (this.t % 16L) * 16;
                int hx = this.paneX - 27 + s2 + s0;
                int hy = this.paneY - 4 + count * TAB_SIZE;
                if (rightSide) {
                    this.blitMirrored(graphics, hx, hy, px, 80, 16, 16);
                } else {
                    graphics.blit(PARTICLES, hx, hy, px, 80, 16, 16, TEX_SIZE, TEX_SIZE);
                }
            }

            // Category icon.
            if (cat.icon != null) {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                graphics.blit(cat.icon, this.paneX - 19 + s2 + s0, this.paneY + 4 + count * TAB_SIZE,
                        0, 0, 16, 16, 16, 16);
            }

            // Dark overlay makes unselected tabs look pushed in.
            if (!selected) {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                if (rightSide) {
                    this.blitMirrored(graphics, tabX, tabY, 200, 232, TAB_SIZE, TAB_SIZE);
                } else {
                    graphics.blit(FRAMES, tabX, tabY, 200, 232, TAB_SIZE, TAB_SIZE, TEX_SIZE, TEX_SIZE);
                }
            }

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            if (mouseX >= tabX && mouseX < tabX + TAB_SIZE
                    && mouseY >= tabY && mouseY < tabY + TAB_SIZE) {
                hoveredTab = key;
            }
            count++;
        }

        // TC4 shows the category name next to the cursor while hovering a tab.
        if (hoveredTab != null) {
            graphics.drawString(this.font, ResearchCategories.getCategoryName(hoveredTab).getString(),
                    mouseX + 2, mouseY - 8, 0xFFFFFF, true);
        }
    }

    /** TC4's {@code drawTexturedModalRectReversed}. */
    private void blitMirrored(GuiGraphics graphics, int x, int y, int u, int v, int width, int height) {
        graphics.pose().pushPose();
        graphics.pose().translate(x + width, y, 0.0F);
        graphics.pose().scale(-1.0F, 1.0F, 1.0F);
        graphics.blit(FRAMES, 0, 0, u, v, width, height, TEX_SIZE, TEX_SIZE);
        graphics.pose().popPose();
    }

    /**
     * TC4's {@code drawResearchBrowserBorders}: the 3x3 nine-slice frame from
     * gui_research.png (corners 16x17, texture region 256x230). Drawn last so it
     * covers any content that overflows the viewport.
     */
    private void renderBorder(GuiGraphics graphics) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int[] xs = {0, BORDER_WIDTH, PANE_WIDTH - BORDER_WIDTH, PANE_WIDTH};
        int[] us = {0, BORDER_WIDTH, TEX_SIZE - BORDER_WIDTH, TEX_SIZE};
        int[] ys = {0, BORDER_HEIGHT, PANE_HEIGHT - BORDER_HEIGHT, PANE_HEIGHT};
        int[] vs = {0, BORDER_HEIGHT, 230 - BORDER_HEIGHT, 230};
        for (int iy = 0; iy < 3; iy++) {
            for (int ix = 0; ix < 3; ix++) {
                int x = this.paneX + xs[ix];
                int y = this.paneY + ys[iy];
                int w = xs[ix + 1] - xs[ix];
                int h = ys[iy + 1] - ys[iy];
                if (w <= 0 || h <= 0) {
                    continue;
                }
                graphics.blit(FRAMES, x, y, w, h, us[ix], vs[iy],
                        us[ix + 1] - us[ix], vs[iy + 1] - vs[iy], TEX_SIZE, TEX_SIZE);
            }
        }
    }

    /** TC4's popup message box (shown after starting a new research). */
    private void renderPopup(GuiGraphics graphics) {
        if (this.popuptime <= System.currentTimeMillis() || this.popupmessage.isEmpty()) {
            return;
        }
        List<FormattedCharSequence> lines = this.font.split(Component.literal(this.popupmessage), 150);
        int textHeight = lines.size() * this.font.lineHeight;
        int x = this.paneX + PANE_WIDTH / 2 - 78;
        int y = this.paneY + PANE_HEIGHT / 2 - textHeight / 2 - 3;
        graphics.fill(x, y, x + 156, y + textHeight + 6, 0x80000000);
        int lineY = y + 3;
        for (FormattedCharSequence line : lines) {
            graphics.drawString(this.font, line, x + 3, lineY, 0xFF9090FF, false);
            lineY += this.font.lineHeight;
        }
    }

    /**
     * TC4's research tooltip: name, short description and the requirements /
     * status of the hovered entry.
     */
    private void renderResearchTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        ResearchEntry entry = this.currentHighlight;
        if (entry == null) {
            return;
        }
        boolean complete = this.isResearchComplete(entry.getKey());
        boolean canUnlock = this.canUnlockResearch(entry);

        String name = entry.getLocalizedName().getString();
        int x = mouseX + 6;
        int y = mouseY - 4;

        if (!canUnlock && !complete) {
            String missing = Component.translatable("tc.researchmissing").getString();
            List<String> lines = new ArrayList<>();
            lines.add(missing);
            if (entry.getParents() != null) {
                String[] clean = entry.getParentsClean();
                for (int i = 0; i < entry.getParents().length; i++) {
                    String raw = entry.getParents()[i];
                    if (raw == null || raw.startsWith("!") || raw.startsWith("~")) {
                        continue;
                    }
                    String key = clean != null && i < clean.length ? clean[i] : raw;
                    if (this.isResearchComplete(key)) {
                        continue;
                    }
                    ResearchEntry parent = this.getEntry(key);
                    lines.add(" - " + (parent != null ? parent.getLocalizedName().getString() : key));
                }
            }
            int width = this.font.width(name);
            for (String line : lines) {
                width = Math.max(width, this.font.width(line));
            }
            int height = this.font.lineHeight * (lines.size() + 1);
            graphics.fill(x - 3, y - 3, x + width + 3, y + height + 6, 0x80000000);
            graphics.drawString(this.font, name, x, y, 0xFF808080, true);
            int lineY = y + this.font.lineHeight + 2;
            for (String line : lines) {
                graphics.drawString(this.font, line, x, lineY, 0xFF705050, false);
                lineY += this.font.lineHeight;
            }
            return;
        }

        // Name + description (TC4 draws the description at half scale). The
        // port's lang files do not cover every entry, so a missing text key
        // falls back to "no description" instead of printing the raw key.
        String textKey = "tc.research_text." + entry.getKey();
        String description = Language.getInstance().getOrDefault(textKey, "");
        boolean hasText = !description.isBlank();
        int nameWidth = this.font.width(name);
        int descWidth = hasText ? (int) (this.font.width(description) * 0.5F) : 0;
        int width = Math.max(nameWidth + 2, descWidth);
        int height = this.font.lineHeight + 2;
        if (hasText) {
            height += 4 + this.font.lineHeight / 2;
        }

        List<String> status = new ArrayList<>();
        if (!complete) {
            int stage = this.knowledge != null ? this.knowledge.getResearchStage(entry.getKey()) : -1;
            if (entry.getStages() != null && stage > 0) {
                status.add(Component.translatable("tc.research.stage").getString() + " " + stage
                        + "/" + entry.getStages().length);
            } else {
                status.add(Component.translatable("tc.research.begin").getString());
            }
        }
        if (this.knowledge != null) {
            if (this.knowledge.hasResearchFlag(entry.getKey(), IPlayerKnowledge.EnumResearchFlag.RESEARCH)) {
                status.add(Component.translatable("tc.research.newresearch").getString());
            }
            if (this.knowledge.hasResearchFlag(entry.getKey(), IPlayerKnowledge.EnumResearchFlag.PAGE)) {
                status.add(Component.translatable("tc.research.newpage").getString());
            }
        }
        if (!status.isEmpty()) {
            height += this.font.lineHeight * status.size();
        }

        graphics.fill(x - 3, y - 3, x + width + 3, y + height + 3, 0x80000000);
        graphics.drawString(this.font, name, x, y, 0xFFFFFF, true);
        if (hasText) {
            graphics.pose().pushPose();
            graphics.pose().translate(x, y + this.font.lineHeight + 2, 0.0F);
            graphics.pose().scale(0.5F, 0.5F, 1.0F);
            graphics.drawString(this.font, description, 0, 0, 0xFF9090FF, false);
            graphics.pose().popPose();
        }

        int lineY = y + this.font.lineHeight + (hasText ? 4 + this.font.lineHeight / 2 : 2);
        for (String line : status) {
            graphics.drawString(this.font, line, x, lineY, 0x87CEEB, false);
            lineY += this.font.lineHeight;
        }
    }
}
