package thaumcraft.compat.jei.infusion;

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
import thaumcraft.common.lib.crafting.InfusionRecipeType;
import thaumcraft.compat.jei.ThaumcraftJEIPlugin;
import thaumcraft.init.ModBlocks;

/**
 * JEI recipe category for Infusion Altar recipes.
 * Displays:
 * - Central item (on the matrix)
 * - Component items (on pedestals) arranged in a circle
 * - Required aspects with amounts
 * - Instability level
 * - Research requirement
 * - Output item
 */
public class InfusionCategory implements IRecipeCategory<InfusionRecipeType> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "infusion");

    private final IDrawable background;
    private final IDrawable icon;
    private final Component title;

    public InfusionCategory(IGuiHelper guiHelper) {
        // Create a larger background for infusion (needs space for pedestal items)
        this.background = guiHelper.createBlankDrawable(170, 100);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModBlocks.INFUSION_MATRIX.get()));
        this.title = Component.translatable("gui.thaumcraft.infusion");
    }

    @Override
    public RecipeType<InfusionRecipeType> getRecipeType() {
        return ThaumcraftJEIPlugin.INFUSION_TYPE;
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
    public void setRecipe(IRecipeLayoutBuilder builder, InfusionRecipeType recipe, IFocusGroup focuses) {
        // Center coordinates for the central item
        int centerX = 75;
        int centerY = 40;
        
        // Add central item input (center)
        Ingredient centralItem = recipe.getCentralItem();
        if (centralItem != null && !centralItem.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, centerX, centerY)
                    .addIngredients(centralItem);
        }
        
        // Add component items in a circle around the center
        NonNullList<Ingredient> components = recipe.getComponents();
        int numComponents = components.size();
        int radius = 30;
        
        for (int i = 0; i < numComponents; i++) {
            // Calculate position in a circle
            double angle = (2 * Math.PI * i / numComponents) - (Math.PI / 2); // Start from top
            int x = centerX + (int)(radius * Math.cos(angle));
            int y = centerY + (int)(radius * Math.sin(angle));
            
            Ingredient component = components.get(i);
            if (!component.isEmpty()) {
                builder.addSlot(RecipeIngredientRole.INPUT, x, y)
                        .addIngredients(component);
            }
        }
        
        // Add output slot (right side)
        RegistryAccess registryAccess = Minecraft.getInstance().level != null 
                ? Minecraft.getInstance().level.registryAccess() 
                : RegistryAccess.EMPTY;
        ItemStack output = recipe.getResultItem(registryAccess);
        builder.addSlot(RecipeIngredientRole.OUTPUT, 140, centerY)
                .addItemStack(output);
    }

    @Override
    public void draw(InfusionRecipeType recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        
        // Draw arrow
        guiGraphics.drawString(font, "в†’", 115, 43, 0x404040, false);
        
        // Draw research requirement at top
        String research = recipe.getResearch();
        if (research != null && !research.isEmpty()) {
            String displayResearch = research.length() > 20 ? research.substring(0, 17) + "..." : research;
            guiGraphics.drawString(font, "Research: " + displayResearch, 5, 2, 0x808080, false);
        }
        
        // Draw instability level
        int instability = recipe.getInstability();
        String instabilityText = "Instability: " + getInstabilityText(instability);
        int instabilityColor = getInstabilityColor(instability);
        guiGraphics.drawString(font, instabilityText, 5, 85, instabilityColor, false);
        
        // Draw aspect requirements
        AspectList aspects = recipe.getAspects();
        if (aspects != null && aspects.size() > 0) {
            StringBuilder aspectText = new StringBuilder();
            boolean first = true;
            for (Aspect aspect : aspects.getAspects()) {
                if (!first) aspectText.append(" ");
                first = false;
                int amount = aspects.getAmount(aspect);
                // Use first 3 letters of aspect name
                String shortName = aspect.getName().length() > 3 
                        ? aspect.getName().substring(0, 3) 
                        : aspect.getName();
                aspectText.append(amount).append(shortName);
            }
            
            String text = aspectText.toString();
            if (text.length() > 35) {
                text = text.substring(0, 32) + "...";
            }
            guiGraphics.drawString(font, text, 5, 75, 0x8B00FF, false);
        }
    }
    
    private String getInstabilityText(int instability) {
        if (instability <= 1) return "Negligible";
        if (instability <= 2) return "Minor";
        if (instability <= 3) return "Moderate";
        if (instability <= 5) return "High";
        if (instability <= 7) return "Very High";
        return "Dangerous";
    }
    
    private int getInstabilityColor(int instability) {
        if (instability <= 1) return 0x00FF00; // Green
        if (instability <= 2) return 0x7FFF00; // Yellow-Green
        if (instability <= 3) return 0xFFFF00; // Yellow
        if (instability <= 5) return 0xFF7F00; // Orange
        if (instability <= 7) return 0xFF3F00; // Red-Orange
        return 0xFF0000; // Red
    }
}




