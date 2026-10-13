package thaumcraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.crafting.IThaumcraftRecipe;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.crafting.Part;
import thaumcraft.api.internal.CommonInternals;
import thaumcraft.client.lib.AspectRenderer;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * RecipeRenderer - Helper class for rendering recipes in the Thaumonomicon.
 * 
 * Supports:
 * - Vanilla crafting recipes (shaped and shapeless)
 * - Arcane crafting recipes
 * - Crucible recipes
 * - Infusion recipes
 * 
 * Ported from GuiResearchPage recipe rendering code.
 */
@OnlyIn(Dist.CLIENT)
public class RecipeRenderer {
    
    private static final ResourceLocation OVERLAY_TEXTURE = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_researchbook_overlay.png");
    private static final ResourceLocation BOOK_TEXTURE = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_researchbook.png");
    
    private static final int SLOT_SIZE = 18;
    private static final int ITEM_SIZE = 16;
    
    // Cycle counter for animated ingredients
    private static long lastCycleTime = 0;
    private static int cycleIndex = 0;
    
    /**
     * Render a recipe at the given position.
     * 
     * @param graphics the graphics context
     * @param recipeId the recipe resource location
     * @param x center X position
     * @param y center Y position
     * @param mouseX mouse X for tooltips
     * @param mouseY mouse Y for tooltips
     * @param font the font renderer
     * @return list of tooltip components if hovering over an item, null otherwise
     */
    public static List<net.minecraft.network.chat.Component> renderRecipe(
            GuiGraphics graphics, ResourceLocation recipeId, int x, int y, 
            int mouseX, int mouseY, Font font) {
        
        // Update cycle for animated ingredients
        updateCycle();
        
        // Try to find the recipe
        Object recipe = findRecipe(recipeId);
        
        if (recipe == null) {
            // Recipe not found - draw placeholder
            graphics.drawCenteredString(font, "Recipe not found:", x, y - 20, 0x804040);
            String idStr = recipeId.toString();
            if (idStr.length() > 30) {
                idStr = "..." + idStr.substring(idStr.length() - 27);
            }
            graphics.drawCenteredString(font, idStr, x, y - 8, 0x606060);
            return null;
        }
        
        // Render based on recipe type
        if (recipe instanceof CrucibleRecipe crucible) {
            return renderCrucibleRecipe(graphics, crucible, x, y, mouseX, mouseY, font);
        } else if (recipe instanceof InfusionRecipe infusion) {
            return renderInfusionRecipe(graphics, infusion, x, y, mouseX, mouseY, font);
        } else if (recipe instanceof IArcaneRecipe arcane) {
            return renderArcaneRecipe(graphics, arcane, x, y, mouseX, mouseY, font);
        } else if (recipe instanceof ThaumcraftApi.BluePrint blueprint) {
            return renderBluePrint(graphics, blueprint, x, y, mouseX, mouseY, font);
        } else if (recipe instanceof CraftingRecipe crafting) {
            return renderCraftingRecipe(graphics, crafting, x, y, mouseX, mouseY, font);
        } else {
            // Unknown recipe type
            graphics.drawCenteredString(font, "Unknown recipe type", x, y, 0x804040);
            return null;
        }
    }
    
    /** Type sub-folders used by the 1.21 data-driven recipe files. */
    private static final String[] RECIPE_FOLDERS = {
            "arcane_workbench", "crucible", "infusion", "crafting", "smelting", "multiblock"
    };
    
    /**
     * Find a recipe by its resource location.
     *
     * The research JSON files still carry the flat TC6 recipe ids
     * ("thaumcraft:nitor"), while a 1.21 datapack stores the very same recipe in a
     * type sub-folder ("thaumcraft:crucible/nitor"). This resolves both spellings
     * so the thaumonomicon can actually display the recipe pages.
     */
    private static Object findRecipe(ResourceLocation id) {
        // First check Thaumcraft's catalog
        IThaumcraftRecipe tcRecipe = CommonInternals.getCatalogRecipe(id);
        if (tcRecipe != null) {
            return tcRecipe;
        }
        
        // Catalog ids are not always spelled the same way as the research ids
        // (e.g. "thaumcraft:infusion_altar" vs "thaumcraft:infusionaltar").
        tcRecipe = findCatalogRecipeLoose(id);
        if (tcRecipe != null) {
            return tcRecipe;
        }
        
        // Check fake recipes
        Object fakeRecipe = CommonInternals.getCatalogRecipeFake(id);
        if (fakeRecipe != null) {
            return fakeRecipe;
        }
        
        // Try vanilla recipe manager
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            RecipeManager rm = mc.level.getRecipeManager();
            Optional<RecipeHolder<?>> vanillaRecipe = rm.byKey(id);
            if (vanillaRecipe.isPresent()) {
                return vanillaRecipe.get().value();
            }
            
            // Data recipes live in type sub-folders - retry with each known folder.
            String path = id.getPath();
            for (String folder : RECIPE_FOLDERS) {
                Optional<RecipeHolder<?>> holder = rm.byKey(
                        ResourceLocation.fromNamespaceAndPath(id.getNamespace(), folder + "/" + path));
                if (holder.isPresent()) {
                    return holder.get().value();
                }
            }
            
            // Last resort: match the file name anywhere in the recipe tree.
            for (RecipeHolder<?> holder : rm.getRecipes()) {
                if (holder.id().getNamespace().equals(id.getNamespace())
                        && holder.id().getPath().endsWith("/" + path)) {
                    return holder.value();
                }
            }
        }
        
        return null;
    }
    
    /** Catalog lookup that ignores underscores/dashes in the path. */
    private static IThaumcraftRecipe findCatalogRecipeLoose(ResourceLocation id) {
        String wanted = normalizeRecipeKey(id);
        for (Map.Entry<ResourceLocation, IThaumcraftRecipe> entry : CommonInternals.craftingRecipeCatalog.entrySet()) {
            ResourceLocation key = entry.getKey();
            if (key != null && key.getNamespace().equals(id.getNamespace())
                    && normalizeRecipeKey(key).equals(wanted)) {
                return entry.getValue();
            }
        }
        return null;
    }
    
    private static String normalizeRecipeKey(ResourceLocation id) {
        return id.getPath().replaceAll("[^a-z0-9]", "");
    }
    
    /**
     * Update the cycle index for animated ingredients.
     */
    private static void updateCycle() {
        long now = System.currentTimeMillis();
        if (now - lastCycleTime > 1000) {
            cycleIndex++;
            lastCycleTime = now;
        }
    }
    
    /**
     * Get the current item from an ingredient (cycles through options).
     */
    private static ItemStack cycleIngredient(Ingredient ingredient, int slotIndex) {
        ItemStack[] items = ingredient.getItems();
        if (items.length == 0) return ItemStack.EMPTY;
        return items[(cycleIndex + slotIndex) % items.length];
    }
    
    // ==================== Vanilla Crafting ====================
    
    private static List<net.minecraft.network.chat.Component> renderCraftingRecipe(
            GuiGraphics graphics, CraftingRecipe recipe, int x, int y, 
            int mouseX, int mouseY, Font font) {
        
        List<net.minecraft.network.chat.Component> tooltip = null;
        
        // Draw title
        String title = recipe instanceof ShapedRecipe ? "Crafting (Shaped)" : "Crafting (Shapeless)";
        graphics.drawCenteredString(font, title, x, y - 70, 0x505050);
        
        // Draw crafting grid background
        graphics.fill(x - 30, y - 50, x + 30, y + 10, 0x20000000);
        
        // Draw output
        ItemStack output = recipe.getResultItem(Minecraft.getInstance().level.registryAccess());
        renderItem(graphics, output, x - 8, y + 25);
        tooltip = checkItemTooltip(output, x - 8, y + 25, mouseX, mouseY, tooltip);
        
        // Draw arrow
        graphics.drawString(font, "в†’", x + 20, y - 18, 0x404040, false);
        
        // Draw ingredients
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        int width = recipe instanceof ShapedRecipe shaped ? shaped.getWidth() : 3;
        int height = recipe instanceof ShapedRecipe shaped ? shaped.getHeight() : (ingredients.size() + 2) / 3;
        
        for (int i = 0; i < ingredients.size(); i++) {
            Ingredient ing = ingredients.get(i);
            if (ing.isEmpty()) continue;
            
            int gridX, gridY;
            if (recipe instanceof ShapedRecipe) {
                gridX = i % width;
                gridY = i / width;
            } else {
                gridX = i % 3;
                gridY = i / 3;
            }
            
            int itemX = x - 28 + gridX * SLOT_SIZE;
            int itemY = y - 48 + gridY * SLOT_SIZE;
            
            ItemStack stack = cycleIngredient(ing, i);
            renderItem(graphics, stack, itemX, itemY);
            tooltip = checkItemTooltip(stack, itemX, itemY, mouseX, mouseY, tooltip);
        }
        
        return tooltip;
    }
    
    // ==================== Arcane Crafting ====================
    
    private static List<net.minecraft.network.chat.Component> renderArcaneRecipe(
            GuiGraphics graphics, IArcaneRecipe recipe, int x, int y, 
            int mouseX, int mouseY, Font font) {
        
        List<net.minecraft.network.chat.Component> tooltip = null;
        
        // Draw title
        graphics.drawCenteredString(font, "Arcane Crafting", x, y - 70, 0x505050);
        
        // Draw crafting grid background
        graphics.fill(x - 30, y - 50, x + 30, y + 10, 0x20404080);
        
        // Draw output
        ItemStack output = recipe.getResultItem(Minecraft.getInstance().level.registryAccess());
        renderItem(graphics, output, x - 8, y + 30);
        tooltip = checkItemTooltip(output, x - 8, y + 30, mouseX, mouseY, tooltip);
        
        // Draw vis cost
        int visCost = recipe.getVis();
        if (visCost > 0) {
            graphics.drawCenteredString(font, "Vis: " + visCost, x, y + 52, 0x8080FF);
        }
        
        // Draw crystal requirements
        AspectList crystals = recipe.getCrystals();
        if (crystals != null && crystals.size() > 0) {
            int totalWidth = crystals.size() * 20 - 4;
            int crystalX = x - totalWidth / 2;
            int crystalY = y + 58;
            
            for (Aspect aspect : crystals.getAspects()) {
                int amount = crystals.getAmount(aspect);
                // Draw small aspect icon with amount for crystals
                AspectRenderer.drawAspectSmall(graphics, crystalX, crystalY, aspect);
                graphics.drawString(font, "x" + amount, crystalX + 10, crystalY + 2, 0xFFFFFF, false);
                crystalX += 28;
            }
        }
        
        // Draw ingredients (3x3 grid)
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        for (int i = 0; i < ingredients.size() && i < 9; i++) {
            Ingredient ing = ingredients.get(i);
            if (ing.isEmpty()) continue;
            
            int gridX = i % 3;
            int gridY = i / 3;
            int itemX = x - 28 + gridX * SLOT_SIZE;
            int itemY = y - 48 + gridY * SLOT_SIZE;
            
            ItemStack stack = cycleIngredient(ing, i);
            renderItem(graphics, stack, itemX, itemY);
            tooltip = checkItemTooltip(stack, itemX, itemY, mouseX, mouseY, tooltip);
        }
        
        return tooltip;
    }
    
    // ==================== Crucible ====================
    
    private static List<net.minecraft.network.chat.Component> renderCrucibleRecipe(
            GuiGraphics graphics, CrucibleRecipe recipe, int x, int y, 
            int mouseX, int mouseY, Font font) {
        
        List<net.minecraft.network.chat.Component> tooltip = null;
        
        // Draw title
        graphics.drawCenteredString(font, "Crucible", x, y - 70, 0x505050);
        
        // Draw crucible shape (simplified)
        graphics.fill(x - 25, y - 30, x + 25, y + 30, 0x30804020);
        graphics.fill(x - 20, y - 25, x + 20, y + 25, 0x40402010);
        
        // Draw output above
        ItemStack output = recipe.getRecipeOutput();
        renderItem(graphics, output, x - 8, y - 55);
        tooltip = checkItemTooltip(output, x - 8, y - 55, mouseX, mouseY, tooltip);
        
        // Draw catalyst to the left
        ItemStack catalyst = cycleIngredient(recipe.getCatalyst(), 0);
        renderItem(graphics, catalyst, x - 50, y - 10);
        tooltip = checkItemTooltip(catalyst, x - 50, y - 10, mouseX, mouseY, tooltip);
        graphics.drawString(font, "в†’", x - 32, y - 6, 0x404040, false);
        
        // Draw aspects required
        AspectList aspects = recipe.getAspects();
        if (aspects != null && aspects.size() > 0) {
            int aspectY = y + 40;
            graphics.drawCenteredString(font, "Essentia:", x, aspectY, 0x606060);
            aspectY += 12;
            
            // Center the aspects
            int totalWidth = aspects.size() * 20 - 4;
            int aspectX = x - totalWidth / 2;
            
            for (Aspect aspect : aspects.getAspects()) {
                int amount = aspects.getAmount(aspect);
                List<net.minecraft.network.chat.Component> aspectTooltip = 
                        AspectRenderer.drawAspectWithTooltip(graphics, aspectX, aspectY, 
                                aspect, amount, mouseX, mouseY);
                if (aspectTooltip != null && tooltip == null) {
                    tooltip = aspectTooltip;
                }
                aspectX += 20;
            }
        }
        
        return tooltip;
    }
    
    // ==================== Infusion ====================
    
    private static List<net.minecraft.network.chat.Component> renderInfusionRecipe(
            GuiGraphics graphics, InfusionRecipe recipe, int x, int y, 
            int mouseX, int mouseY, Font font) {
        
        List<net.minecraft.network.chat.Component> tooltip = null;
        
        // Draw title
        graphics.drawCenteredString(font, "Infusion", x, y - 70, 0x505050);
        
        // Draw matrix shape (center circle)
        graphics.fill(x - 12, y - 12, x + 12, y + 12, 0x40800080);
        
        // Draw central item
        ItemStack central = cycleIngredient(recipe.getRecipeInput(), 0);
        renderItem(graphics, central, x - 8, y - 8);
        tooltip = checkItemTooltip(central, x - 8, y - 8, mouseX, mouseY, tooltip);
        
        // Draw output above
        Object outputObj = recipe.getRecipeOutput();
        if (outputObj instanceof ItemStack output) {
            renderItem(graphics, output, x - 8, y - 55);
            tooltip = checkItemTooltip(output, x - 8, y - 55, mouseX, mouseY, tooltip);
        }
        
        // Draw components in a circle
        NonNullList<Ingredient> components = recipe.getComponents();
        int numComponents = components.size();
        int radius = 35;
        
        for (int i = 0; i < numComponents; i++) {
            double angle = (2 * Math.PI * i / numComponents) - Math.PI / 2;
            int compX = x + (int)(Math.cos(angle) * radius) - 8;
            int compY = y + (int)(Math.sin(angle) * radius) - 8;
            
            ItemStack comp = cycleIngredient(components.get(i), i);
            renderItem(graphics, comp, compX, compY);
            tooltip = checkItemTooltip(comp, compX, compY, mouseX, mouseY, tooltip);
        }
        
        // Draw aspects required
        AspectList aspects = recipe.getAspects();
        if (aspects != null && aspects.size() > 0) {
            int aspectY = y + 50;
            graphics.drawCenteredString(font, "Essentia:", x, aspectY, 0x606060);
            aspectY += 12;
            
            // Limit to 5 aspects, center them
            int displayCount = Math.min(aspects.size(), 5);
            int totalWidth = displayCount * 20 - 4;
            int aspectX = x - totalWidth / 2;
            
            int count = 0;
            for (Aspect aspect : aspects.getAspects()) {
                if (count >= 5) {
                    graphics.drawString(font, "...", aspectX, aspectY + 4, 0x808080, false);
                    break;
                }
                int amount = aspects.getAmount(aspect);
                List<net.minecraft.network.chat.Component> aspectTooltip = 
                        AspectRenderer.drawAspectWithTooltip(graphics, aspectX, aspectY, 
                                aspect, amount, mouseX, mouseY);
                if (aspectTooltip != null && tooltip == null) {
                    tooltip = aspectTooltip;
                }
                aspectX += 20;
                count++;
            }
        }
        
        // Draw instability
        if (recipe.instability > 0) {
            String instText = "Instability: " + recipe.instability;
            int instColor = recipe.instability > 3 ? 0xFF6060 : (recipe.instability > 1 ? 0xFFFF60 : 0x60FF60);
            graphics.drawCenteredString(font, instText, x, y + 80, instColor);
        }
        
        return tooltip;
    }
    
    // ==================== Multiblock Blueprint ====================
    
    /**
     * Render a multiblock blueprint (infusion altar, infernal furnace, ...).
     *
     * TC4 shows a rotatable 3D preview here; this is a light 2D schematic: the
     * densest layer of the blueprint is drawn as a grid of blocks and the
     * material list is shown underneath, which is enough to build the structure.
     */
    private static List<net.minecraft.network.chat.Component> renderBluePrint(
            GuiGraphics graphics, ThaumcraftApi.BluePrint blueprint, int x, int y,
            int mouseX, int mouseY, Font font) {
        
        List<net.minecraft.network.chat.Component> tooltip = null;
        
        String title = blueprint.getResearch() != null ? blueprint.getResearch() : "Multiblock";
        graphics.drawCenteredString(font, title, x, y - 70, 0x505050);
        
        Part[][][] parts = blueprint.getParts();
        if (parts != null && parts.length > 0) {
            // Pick the layer with the most defined parts so the schematic is readable.
            Part[][] best = null;
            int bestCount = -1;
            for (Part[][] layer : parts) {
                int count = 0;
                if (layer != null) {
                    for (Part[] row : layer) {
                        if (row == null) continue;
                        for (Part part : row) {
                            if (part != null && !partStack(part).isEmpty()) count++;
                        }
                    }
                }
                if (count > bestCount) {
                    bestCount = count;
                    best = layer;
                }
            }
            
            if (best != null) {
                int rows = best.length;
                graphics.drawCenteredString(font, "Layer", x, y - 55, 0x606060);
                for (int r = 0; r < rows; r++) {
                    Part[] row = best[r];
                    if (row == null) continue;
                    int cols = row.length;
                    for (int c = 0; c < cols; c++) {
                        Part part = row[c];
                        if (part == null) continue;
                        ItemStack stack = partStack(part);
                        if (stack.isEmpty()) continue;
                        int sx = x - (cols * SLOT_SIZE) / 2 + c * SLOT_SIZE;
                        int sy = y - 46 + r * SLOT_SIZE;
                        renderItem(graphics, stack, sx, sy);
                        tooltip = checkItemTooltip(stack, sx, sy, mouseX, mouseY, tooltip);
                    }
                }
            }
        }
        
        ItemStack[] ingredients = blueprint.getIngredientList();
        if (ingredients != null && ingredients.length > 0) {
            int iy = y + 45;
            graphics.drawCenteredString(font, "Materials", x, iy - 12, 0x606060);
            int ix = x - (ingredients.length * SLOT_SIZE) / 2;
            for (ItemStack stack : ingredients) {
                if (stack != null && !stack.isEmpty()) {
                    renderItem(graphics, stack, ix, iy);
                    tooltip = checkItemTooltip(stack, ix, iy, mouseX, mouseY, tooltip);
                }
                ix += SLOT_SIZE;
            }
        }
        
        return tooltip;
    }
    
    /** Best display stack for a blueprint part (target block preferred, then source). */
    private static ItemStack partStack(Part part) {
        ItemStack stack = stackFromObject(part.getTarget());
        if (stack.isEmpty()) {
            stack = stackFromObject(part.getSource());
        }
        return stack;
    }
    
    private static ItemStack stackFromObject(Object o) {
        if (o instanceof ItemStack stack) {
            return stack;
        }
        if (o instanceof BlockState state) {
            return new ItemStack(state.getBlock());
        }
        if (o instanceof Block block) {
            return new ItemStack(block);
        }
        return ItemStack.EMPTY;
    }
    
    // ==================== Helpers ====================
    
    /**
     * Render an item stack at the given position.
     */
    private static void renderItem(GuiGraphics graphics, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return;
        graphics.renderItem(stack, x, y);
        graphics.renderItemDecorations(Minecraft.getInstance().font, stack, x, y);
    }
    
    /**
     * Check if mouse is hovering over an item and return tooltip if so.
     */
    private static List<net.minecraft.network.chat.Component> checkItemTooltip(
            ItemStack stack, int itemX, int itemY, int mouseX, int mouseY,
            List<net.minecraft.network.chat.Component> existingTooltip) {
        
        if (existingTooltip != null) return existingTooltip;
        if (stack.isEmpty()) return null;
        
        if (mouseX >= itemX && mouseX < itemX + ITEM_SIZE && 
            mouseY >= itemY && mouseY < itemY + ITEM_SIZE) {
            return stack.getTooltipLines(
                    net.minecraft.world.item.Item.TooltipContext.of(Minecraft.getInstance().level),
                    Minecraft.getInstance().player,
                    Minecraft.getInstance().options.advancedItemTooltips ?
                    net.minecraft.world.item.TooltipFlag.Default.ADVANCED :
                    net.minecraft.world.item.TooltipFlag.Default.NORMAL);
        }
        return null;
    }
}




