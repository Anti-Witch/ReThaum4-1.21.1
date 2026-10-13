package thaumcraft.client.gui.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.research.*;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.client.gui.RecipeRenderer;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.playerdata.PacketSyncProgressToServer;
import thaumcraft.common.lib.research.ResearchManager;

import java.util.ArrayList;
import java.util.List;

/**
 * ResearchPageScreen - Displays detailed information about a research entry.
 * 
 * Shows research stages, recipes, text descriptions, requirements, and progress.
 * This is a simplified but functional implementation covering the core features.
 * 
 * Ported from 1.12.2 GuiResearchPage to 1.20.1
 */
@OnlyIn(Dist.CLIENT)
public class ResearchPageScreen extends Screen {
    
    // Textures
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_researchbook.png");
    private static final ResourceLocation OVERLAY = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_researchbook_overlay.png");
    
    // Pane dimensions
    private static final int PANE_WIDTH = 256;
    private static final int PANE_HEIGHT = 181;
    private static final int PAGE_WIDTH = 140;
    private static final int PAGE_HEIGHT = 210;
    
    // Research data
    private final ResearchEntry research;
    private final ResourceLocation highlightRecipe;
    private final double returnX;
    private final double returnY;
    
    // Player data
    private Player player;
    private IPlayerKnowledge playerKnowledge;
    
    // Page tracking
    private int currentStage = 0;
    private int page = 0;
    private int maxPages = 1;
    private boolean isComplete = false;
    private boolean hasAllRequisites = false;
    
    // Parsed page content
    private ArrayList<Page> pages = new ArrayList<>();
    
    /** TC4 inline image markup: {@code <IMG>path:u:v:w:h:scale</IMG>}. */
    private static final java.util.regex.Pattern IMG_PATTERN =
            java.util.regex.Pattern.compile("<IMG>([^<]+)</IMG>");
    
    // Requirement tracking
    private boolean[] hasItem;
    private boolean[] hasCraft;
    private boolean[] hasResearch;
    private boolean[] hasKnow;
    
    public ResearchPageScreen(ResearchEntry research, ResourceLocation highlightRecipe, double returnX, double returnY) {
        super(Component.translatable(research.getName()));
        this.research = research;
        this.highlightRecipe = highlightRecipe;
        this.returnX = returnX;
        this.returnY = returnY;
    }
    
    @Override
    protected void init() {
        super.init();
        
        player = minecraft.player;
        playerKnowledge = ThaumcraftCapabilities.getKnowledge(player);
        
        parsePages();
    }
    
    /**
     * Parse research content into displayable pages.
     */
    private void parsePages() {
        pages.clear();
        
        if (playerKnowledge == null) {
            return;
        }
        
        // Determine current stage
        currentStage = playerKnowledge.getResearchStage(research.getKey());
        if (currentStage < 1) currentStage = 1;
        
        isComplete = playerKnowledge.isResearchComplete(research.getKey());
        hasAllRequisites = ResearchManager.doesPlayerHaveRequisites(player, research.getKey());
        
        // Get stages up to current (or all if complete)
        ResearchStage[] stages = research.getStages();
        if (stages == null || stages.length == 0) {
            // No stages - just show title page
            Page titlePage = new Page();
            titlePage.contents.add(Component.translatable("tc.research.nostages").getString());
            pages.add(titlePage);
            maxPages = 1;
            return;
        }
        
        // TC4 shows every page of a research as soon as it is known; the port's
        // staged reveal is only kept for entries the player has not started yet.
        boolean allVisible = isComplete || playerKnowledge.isResearchKnown(research.getKey());
        int maxStage = allVisible ? stages.length : Math.min(currentStage, stages.length);
        
        // Build pages for each visible stage
        for (int s = 0; s < maxStage; s++) {
            ResearchStage stage = stages[s];
            
            // Create page for stage text (TC4 markup aware)
            if (stage.getText() != null) {
                String text = Component.translatable(stage.getText()).getString();
                addTextPages(text);
            } else {
                pages.add(new Page());
            }
            
            // Add recipe pages if any
            if (stage.getRecipes() != null && stage.getRecipes().length > 0) {
                for (ResourceLocation recipe : stage.getRecipes()) {
                    Page recipePage = new Page();
                    recipePage.isRecipePage = true;
                    recipePage.recipeId = recipe;
                    pages.add(recipePage);
                }
            }
        }
        
        // Add addenda if research is complete
        if (isComplete && research.getAddenda() != null) {
            for (ResearchAddendum addendum : research.getAddenda()) {
                // Check if addendum requirements are met
                boolean canShow = true;
                if (addendum.getResearch() != null) {
                    for (String req : addendum.getResearch()) {
                        if (!ThaumcraftCapabilities.isResearchComplete(player, req)) {
                            canShow = false;
                            break;
                        }
                    }
                }
                
                if (canShow) {
                    Page addendumPage = new Page();
                    addendumPage.isAddendum = true;
                    
                    if (addendum.getText() != null) {
                        String text = Component.translatable(addendum.getText()).getString();
                        List<String> lines = wrapText(text, PAGE_WIDTH - 10);
                        addendumPage.contents.addAll(lines);
                    }
                    
                    pages.add(addendumPage);
                    
                    // Add addendum recipes
                    if (addendum.getRecipes() != null) {
                        for (ResourceLocation recipe : addendum.getRecipes()) {
                            Page recipePage = new Page();
                            recipePage.isRecipePage = true;
                            recipePage.isAddendum = true;
                            recipePage.recipeId = recipe;
                            pages.add(recipePage);
                        }
                    }
                }
            }
        }
        
        // Ensure we have at least one page
        if (pages.isEmpty()) {
            Page emptyPage = new Page();
            emptyPage.contents.add(Component.translatable("tc.research.empty").getString());
            pages.add(emptyPage);
        }
        
        maxPages = pages.size();
        
        // Update requirement tracking
        if (!isComplete && currentStage > 0 && currentStage <= stages.length) {
            ResearchStage currentStageData = stages[currentStage - 1];
            updateRequirementTracking(currentStageData);
        }
    }
    
    /**
     * Update tracking arrays for current stage requirements.
     */
    private void updateRequirementTracking(ResearchStage stage) {
        // Track item requirements
        if (stage.getObtain() != null) {
            hasItem = new boolean[stage.getObtain().length];
            for (int i = 0; i < stage.getObtain().length; i++) {
                Object o = stage.getObtain()[i];
                if (o instanceof ItemStack stack) {
                    hasItem[i] = playerHasItem(stack);
                }
            }
        }
        
        // Track craft requirements
        if (stage.getCraft() != null) {
            hasCraft = new boolean[stage.getCraft().length];
            int[] refs = stage.getCraftReference();
            for (int i = 0; i < stage.getCraft().length; i++) {
                String refKey = "[#]" + refs[i];
                hasCraft[i] = playerKnowledge.isResearchKnown(refKey);
            }
        }
        
        // Track research requirements
        if (stage.getResearch() != null) {
            hasResearch = new boolean[stage.getResearch().length];
            for (int i = 0; i < stage.getResearch().length; i++) {
                hasResearch[i] = ThaumcraftCapabilities.isResearchComplete(player, stage.getResearch()[i]);
            }
        }
        
        // Track knowledge requirements
        if (stage.getKnow() != null) {
            hasKnow = new boolean[stage.getKnow().length];
            for (int i = 0; i < stage.getKnow().length; i++) {
                ResearchStage.Knowledge k = stage.getKnow()[i];
                String catKey = k.category != null ? k.category.key : null;
                int playerKnow = playerKnowledge.getKnowledge(k.type, catKey);
                hasKnow[i] = playerKnow >= k.amount;
            }
        }
    }
    
    /**
     * Check if player has an item in their inventory.
     */
    private boolean playerHasItem(ItemStack required) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (ItemStack.isSameItemSameComponents(stack, required)) {
                count += stack.getCount();
                if (count >= required.getCount()) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Convert a TC4 research page text into one or more displayable pages.
     * <p>
     * TC4 page markup:
     * <ul>
     *   <li>{@code <BR>} - line break</li>
     *   <li>{@code <LINE>} - line break (already handled by {@link #wrapText})</li>
     *   <li>{@code <PAGE>} - start a new book page</li>
     *   <li>{@code <IMG>path:u:v:w:h:scale</IMG>} - inline image</li>
     * </ul>
     */
    private void addTextPages(String rawText) {
        String[] segments = rawText.split("<PAGE>");
        for (String segment : segments) {
            Page textPage = new Page();

            // Pull inline images out of the text
            java.util.regex.Matcher matcher = IMG_PATTERN.matcher(segment);
            StringBuilder withoutImages = new StringBuilder();
            int last = 0;
            while (matcher.find()) {
                withoutImages.append(segment, last, matcher.start());
                last = matcher.end();
                String[] parts = matcher.group(1).split(":");
                if (parts.length >= 6) {
                    try {
                        ResourceLocation texture = resolveImageTexture(parts[0] + ":" + parts[1]);
                        if (texture == null) {
                            continue;   // texture not shipped by the port - skip the image
                        }
                        ImageRef ref = new ImageRef();
                        ref.texture = texture;
                        ref.u = Integer.parseInt(parts[2]);
                        ref.v = Integer.parseInt(parts[3]);
                        ref.w = Integer.parseInt(parts[4]);
                        ref.h = Integer.parseInt(parts[5]);
                        ref.scale = parts.length >= 7 ? Float.parseFloat(parts[6]) : 1.0F;
                        textPage.images.add(ref);
                    } catch (NumberFormatException ignored) {
                        // Malformed image tag - skip it
                    }
                }
            }
            withoutImages.append(segment, last, segment.length());

            // TC4 uses <BR> for line breaks; wrapText handles <LINE> and \n
            String text = withoutImages.toString().replace("<BR>", "\n");
            List<String> lines = wrapText(text, PAGE_WIDTH - 10);
            textPage.contents.addAll(lines);

            pages.add(textPage);
        }
    }

    /**
     * Resolve an inline image path, tolerating TC4's legacy
     * {@code textures/items/} folder (the port uses {@code textures/item/}).
     * Returns null when neither variant is shipped.
     */
    private ResourceLocation resolveImageTexture(String raw) {
        ResourceLocation loc = ResourceLocation.parse(raw);
        ResourceLocation direct = resolveExisting(loc);
        if (direct != null) {
            return direct;
        }
        String path = loc.getPath();
        if (path.startsWith("textures/items/")) {
            ResourceLocation renamed = ResourceLocation.fromNamespaceAndPath(loc.getNamespace(),
                    "textures/item/" + path.substring("textures/items/".length()));
            return resolveExisting(renamed);
        }
        return null;
    }

    private ResourceLocation resolveExisting(ResourceLocation loc) {
        try {
            return Minecraft.getInstance().getResourceManager().getResource(loc).isPresent() ? loc : null;
        } catch (Exception e) {
            // Resource manager not ready - keep the path and let the blit fail soft
            return loc;
        }
    }

    /**
     * Wrap text to fit within a given width.
     */
    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        
        // Split by explicit line breaks
        String[] paragraphs = text.split("<LINE>|\\n");
        
        for (String paragraph : paragraphs) {
            if (paragraph.trim().isEmpty()) {
                lines.add("");
                continue;
            }
            
            // Word wrap each paragraph
            String[] words = paragraph.split(" ");
            StringBuilder currentLine = new StringBuilder();
            
            for (String word : words) {
                String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
                int testWidth = font.width(testLine);
                
                if (testWidth > maxWidth && currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    if (currentLine.length() > 0) {
                        currentLine.append(" ");
                    }
                    currentLine.append(word);
                }
            }
            
            if (currentLine.length() > 0) {
                lines.add(currentLine.toString());
            }
        }
        
        return lines;
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        
        int sw = (width - PANE_WIDTH) / 2;
        int sh = (height - PANE_HEIGHT) / 2;
        
        // Draw book background
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        
        // Scale and draw background
        graphics.pose().pushPose();
        float scale = 1.3f;
        float offsetX = (width - PANE_WIDTH * scale) / 2.0f;
        float offsetY = (height - PANE_HEIGHT * scale) / 2.0f;
        graphics.pose().translate(offsetX, offsetY, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.blit(TEXTURE, 0, 0, 0, 0, PANE_WIDTH, PANE_HEIGHT);
        graphics.pose().popPose();
        
        // Draw title on first page
        if (page == 0) {
            String title = research.getLocalizedName().getString();
            int titleWidth = font.width(title);
            int titleX = sw + 70 - titleWidth / 2;
            graphics.drawString(font, title, titleX, sh + 8, 0x202020, false);
            
            // Draw separator line
            graphics.blit(TEXTURE, sw + 4, sh + 3, 24, 184, 96, 4);
            graphics.blit(TEXTURE, sw + 4, sh + 20, 24, 184, 96, 4);
        }
        
        // Draw page content
        int contentY = sh + (page == 0 ? 38 : 15);
        List<net.minecraft.network.chat.Component> tooltip = null;
        
        // Draw left page (even page number)
        if (page < pages.size()) {
            tooltip = drawPageContent(graphics, pages.get(page), sw + 12, contentY, mouseX, mouseY, false);
        }
        
        // Draw right page (odd page number)
        if (page + 1 < pages.size()) {
            List<net.minecraft.network.chat.Component> rightTooltip = drawPageContent(graphics, pages.get(page + 1), sw + 152, contentY, mouseX, mouseY, true);
            if (tooltip == null) tooltip = rightTooltip;
        }
        
        // Draw navigation arrows
        float bob = (float) Math.sin(System.currentTimeMillis() / 300.0) * 0.2f + 0.1f;
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 0.8f + bob);
        
        if (page > 0) {
            graphics.blit(TEXTURE, sw - 16, sh + 190, 0, 184, 12, 8);
        }
        if (page < maxPages - 2) {
            graphics.blit(TEXTURE, sw + 262, sh + 190, 12, 184, 12, 8);
        }
        
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        
        // Draw requirements if not complete (on first page)
        if (!isComplete && page == 0 && research.getStages() != null && currentStage > 0 && currentStage <= research.getStages().length) {
            drawRequirements(graphics, sw, sh, mouseX, mouseY, research.getStages()[currentStage - 1]);
        }
        
        // Draw complete button if all requirements are met
        if (!isComplete && hasAllRequisites && allCurrentRequirementsMet()) {
            drawCompleteButton(graphics, sw, sh, mouseX, mouseY);
        }
        
        // Draw page numbers
        String pageNum = (page / 2 + 1) + " / " + ((maxPages + 1) / 2);
        graphics.drawCenteredString(font, pageNum, sw + PANE_WIDTH / 2, sh + PANE_HEIGHT + 5, 0x808080);
        
        super.render(graphics, mouseX, mouseY, partialTick);
        
        // Draw tooltip last (on top of everything)
        if (tooltip != null && !tooltip.isEmpty()) {
            graphics.renderTooltip(font, tooltip, java.util.Optional.empty(), mouseX, mouseY);
        }
    }
    
    /**
     * Draw the content of a single page.
     * Returns tooltip to display if hovering over an item.
     */
    private List<net.minecraft.network.chat.Component> drawPageContent(GuiGraphics graphics, Page page, int x, int y, int mouseX, int mouseY, boolean rightSide) {
        if (page.isRecipePage) {
            return drawRecipe(graphics, x, y, page.recipeId, mouseX, mouseY);
        }
        
        // Draw text content
        int lineY = y;
        for (Object content : page.contents) {
            if (content instanceof String text) {
                graphics.drawString(font, text, x, lineY, 0x202020, false);
                lineY += font.lineHeight + 2;
            }
        }
        
        // Draw TC4 inline images below the text
        for (ImageRef image : page.images) {
            int iw = Math.max(1, (int) (image.w * image.scale));
            int ih = Math.max(1, (int) (image.h * image.scale));
            if (iw > PAGE_WIDTH) {
                ih = ih * PAGE_WIDTH / iw;
                iw = PAGE_WIDTH;
            }
            if (lineY + ih > y + PAGE_HEIGHT - 20) {
                break;
            }
            graphics.blit(image.texture, x, lineY, iw, ih,
                    (float) image.u, (float) image.v, image.w, image.h, 256, 256);
            lineY += ih + 2;
        }
        
        // Mark addendum pages
        if (page.isAddendum) {
            graphics.drawString(font, "В§o[Addendum]", x, y - 12, 0x606060, false);
        }
        
        return null;
    }
    
    /**
     * Draw recipe using the RecipeRenderer.
     */
    private List<net.minecraft.network.chat.Component> drawRecipe(GuiGraphics graphics, int x, int y, ResourceLocation recipeId, int mouseX, int mouseY) {
        if (recipeId == null) {
            graphics.drawString(font, "No recipe", x + 40, y + 40, 0x808080, false);
            return null;
        }
        
        // Use RecipeRenderer to draw the recipe
        return RecipeRenderer.renderRecipe(graphics, recipeId, x + 60, y + 60, mouseX, mouseY, font);
    }
    
    /**
     * Draw current stage requirements at the bottom of the page.
     */
    private void drawRequirements(GuiGraphics graphics, int sw, int sh, int mx, int my, ResearchStage stage) {
        int y = sh + PANE_HEIGHT - 30;
        int x = sw + 12;
        
        // Draw requirement sections
        if (stage.getResearch() != null && stage.getResearch().length > 0) {
            graphics.drawString(font, "В§7Required Research:", x, y, 0xFFFFFF, false);
            y += 10;
            for (int i = 0; i < stage.getResearch().length; i++) {
                String reqKey = stage.getResearch()[i];
                ResearchEntry reqEntry = ResearchCategories.getResearch(reqKey);
                String reqName = reqEntry != null ? reqEntry.getLocalizedName().getString() : reqKey;
                String checkmark = (hasResearch != null && hasResearch[i]) ? "В§aвњ“ " : "В§cвњ— ";
                graphics.drawString(font, checkmark + reqName, x + 5, y, 0xFFFFFF, false);
                y += 10;
            }
        }
        
        if (stage.getObtain() != null && stage.getObtain().length > 0) {
            graphics.drawString(font, "В§7Items to Obtain:", x, y, 0xFFFFFF, false);
            y += 10;
            for (int i = 0; i < stage.getObtain().length; i++) {
                Object o = stage.getObtain()[i];
                String itemName = o instanceof ItemStack ? ((ItemStack) o).getHoverName().getString() : "Item";
                String checkmark = (hasItem != null && hasItem[i]) ? "В§aвњ“ " : "В§cвњ— ";
                graphics.drawString(font, checkmark + itemName, x + 5, y, 0xFFFFFF, false);
                y += 10;
            }
        }
        
        if (stage.getKnow() != null && stage.getKnow().length > 0) {
            graphics.drawString(font, "В§7Knowledge Required:", x, y, 0xFFFFFF, false);
            y += 10;
            for (int i = 0; i < stage.getKnow().length; i++) {
                ResearchStage.Knowledge k = stage.getKnow()[i];
                String knowName = k.type.name() + (k.category != null ? " (" + k.category.key + ")" : "") + ": " + k.amount;
                String checkmark = (hasKnow != null && hasKnow[i]) ? "В§aвњ“ " : "В§cвњ— ";
                graphics.drawString(font, checkmark + knowName, x + 5, y, 0xFFFFFF, false);
                y += 10;
            }
        }
    }
    
    /**
     * Check if all requirements for current stage are met.
     */
    private boolean allCurrentRequirementsMet() {
        if (hasItem != null) {
            for (boolean b : hasItem) if (!b) return false;
        }
        if (hasCraft != null) {
            for (boolean b : hasCraft) if (!b) return false;
        }
        if (hasResearch != null) {
            for (boolean b : hasResearch) if (!b) return false;
        }
        if (hasKnow != null) {
            for (boolean b : hasKnow) if (!b) return false;
        }
        return true;
    }
    
    /**
     * Draw the complete/progress button.
     */
    private void drawCompleteButton(GuiGraphics graphics, int sw, int sh, int mx, int my) {
        int buttonX = sw + PANE_WIDTH / 2 - 40;
        int buttonY = sh + PANE_HEIGHT - 15;
        int buttonW = 80;
        int buttonH = 12;
        
        boolean hover = mx >= buttonX && mx < buttonX + buttonW && my >= buttonY && my < buttonY + buttonH;
        
        int color = hover ? 0x4060A060 : 0x40408040;
        graphics.fill(buttonX, buttonY, buttonX + buttonW, buttonY + buttonH, color);
        
        String text = "Complete Stage";
        graphics.drawCenteredString(font, text, buttonX + buttonW / 2, buttonY + 2, hover ? 0xFFFFFF : 0xC0C0C0);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int sw = (width - PANE_WIDTH) / 2;
        int sh = (height - PANE_HEIGHT) / 2;
        
        // Check navigation arrows
        if (page > 0 && mouseX >= sw - 16 && mouseX < sw - 4 && mouseY >= sh + 190 && mouseY < sh + 198) {
            page -= 2;
            if (page < 0) page = 0;
            return true;
        }
        
        if (page < maxPages - 2 && mouseX >= sw + 262 && mouseX < sw + 274 && mouseY >= sh + 190 && mouseY < sh + 198) {
            page += 2;
            return true;
        }
        
        // Check complete button
        if (!isComplete && hasAllRequisites && allCurrentRequirementsMet()) {
            int buttonX = sw + PANE_WIDTH / 2 - 40;
            int buttonY = sh + PANE_HEIGHT - 15;
            int buttonW = 80;
            int buttonH = 12;
            
            if (mouseX >= buttonX && mouseX < buttonX + buttonW && mouseY >= buttonY && mouseY < buttonY + buttonH) {
                // Send progress packet to server
                PacketHandler.sendToServer(new PacketSyncProgressToServer(research.getKey(), false, true, false));
                
                // Re-parse pages after progress
                parsePages();
                return true;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY < 0 && page < maxPages - 2) {
            page += 2;
            return true;
        } else if (scrollY > 0 && page > 0) {
            page -= 2;
            if (page < 0) page = 0;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
    
    @Override
    public void onClose() {
        // Return to research browser at saved position
        minecraft.setScreen(new ResearchBrowserScreen(returnX, returnY));
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    // ==================== Inner Classes ====================
    
    /**
     * Represents a single page of content.
     */
    private static class Page {
        ArrayList<Object> contents = new ArrayList<>();
        ArrayList<ImageRef> images = new ArrayList<>();
        boolean isRecipePage = false;
        boolean isAddendum = false;
        ResourceLocation recipeId = null;
    }
    
    /**
     * An inline TC4 {@code <IMG>} reference.
     */
    private static class ImageRef {
        ResourceLocation texture;
        int u;
        int v;
        int w;
        int h;
        float scale = 1.0F;
    }
}




