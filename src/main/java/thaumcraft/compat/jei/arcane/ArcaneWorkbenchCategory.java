package thaumcraft.compat.jei.arcane;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.compat.jei.ThaumcraftJEIPlugin;
import thaumcraft.init.ModBlocks;

/**
 * JEI recipe category for Arcane Workbench recipes.
 * Displays:
 * - 3x3 crafting grid
 * - Vis cost
 * - Crystal requirements
 * - Research requirement
 * - Output item
 */
public class ArcaneWorkbenchCategory implements IRecipeCategory<IArcaneRecipe> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "arcane_workbench");
    
    // GUI texture - using vanilla crafting table as base for now
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("jei", "textures/jei/gui/gui_vanilla.png");

    private final IDrawable background;
    private final IDrawable icon;
    private final Component title;

    public ArcaneWorkbenchCategory(IGuiHelper guiHelper) {
        // Use a wider background to accommodate vis/crystal info
        this.background = guiHelper.createDrawable(TEXTURE, 0, 60, 116, 54);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModBlocks.ARCANE_WORKBENCH.get()));
        this.title = Component.translatable("gui.thaumcraft.arcane_workbench");
    }

    @Override
    public RecipeType<IArcaneRecipe> getRecipeType() {
        return ThaumcraftJEIPlugin.ARCANE_TYPE;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @SuppressWarnings("removal")
    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, IArcaneRecipe recipe, IFocusGroup focuses) {
        // Get recipe ingredients
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        
        // Add input slots (3x3 grid)
        int ingredientIndex = 0;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int x = col * 18 + 1;
                int y = row * 18 + 1;
                
                if (ingredientIndex < ingredients.size()) {
                    Ingredient ingredient = ingredients.get(ingredientIndex);
                    if (!ingredient.isEmpty()) {
                        builder.addSlot(RecipeIngredientRole.INPUT, x, y)
                                .addIngredients(ingredient);
                    }
                }
                ingredientIndex++;
            }
        }
        
        // Add output slot
        RegistryAccess registryAccess = Minecraft.getInstance().level != null 
                ? Minecraft.getInstance().level.registryAccess() 
                : RegistryAccess.EMPTY;
        ItemStack output = recipe.getResultItem(registryAccess);
        builder.addSlot(RecipeIngredientRole.OUTPUT, 95, 19)
                .addItemStack(output);
    }

    @Override
    public void draw(IArcaneRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        
        // Draw vis cost
        int visCost = recipe.getVis();
        if (visCost > 0) {
            String visText = visCost + " vis";
            guiGraphics.drawString(font, visText, 58, 0, 0x8B00FF, false);
        }
        
        // Draw crystal requirements
        AspectList crystals = recipe.getCrystals();
        if (crystals != null && crystals.size() > 0) {
            int yOffset = 45;
            StringBuilder crystalText = new StringBuilder();
            for (Aspect aspect : crystals.getAspects()) {
                if (crystalText.length() > 0) crystalText.append(" ");
                int amount = crystals.getAmount(aspect);
                crystalText.append(amount).append(aspect.getTag().substring(0, 1).toUpperCase());
            }
            guiGraphics.drawString(font, crystalText.toString(), 0, yOffset, 0x404040, false);
        }
        
        // Draw research requirement
        String research = recipe.getResearch();
        if (research != null && !research.isEmpty()) {
            // Truncate long research names
            String displayResearch = research.length() > 12 ? research.substring(0, 12) + "..." : research;
            guiGraphics.drawString(font, "Research: " + displayResearch, 0, -10, 0x808080, false);
        }
    }
}




