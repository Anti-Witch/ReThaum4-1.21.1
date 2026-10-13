package thaumcraft.compat.jei.crucible;

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
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.lib.crafting.CrucibleRecipeType;
import thaumcraft.compat.jei.ThaumcraftJEIPlugin;
import thaumcraft.init.ModBlocks;

/**
 * JEI recipe category for Crucible alchemy recipes.
 * Displays:
 * - Catalyst item (thrown into crucible)
 * - Required aspects with amounts
 * - Research requirement
 * - Output item
 */
public class CrucibleCategory implements IRecipeCategory<CrucibleRecipeType> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "crucible");
    
    // Simple background texture
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("jei", "textures/jei/gui/gui_vanilla.png");

    private final IDrawable background;
    private final IDrawable icon;
    private final Component title;

    public CrucibleCategory(IGuiHelper guiHelper) {
        // Create a simple background
        this.background = guiHelper.createBlankDrawable(150, 80);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModBlocks.CRUCIBLE.get()));
        this.title = Component.translatable("gui.thaumcraft.crucible");
    }

    @Override
    public RecipeType<CrucibleRecipeType> getRecipeType() {
        return ThaumcraftJEIPlugin.CRUCIBLE_TYPE;
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
    public void setRecipe(IRecipeLayoutBuilder builder, CrucibleRecipeType recipe, IFocusGroup focuses) {
        // Add catalyst input slot (left side)
        builder.addSlot(RecipeIngredientRole.INPUT, 10, 30)
                .addIngredients(recipe.getCatalyst());
        
        // Add output slot (right side)
        RegistryAccess registryAccess = Minecraft.getInstance().level != null 
                ? Minecraft.getInstance().level.registryAccess() 
                : RegistryAccess.EMPTY;
        ItemStack output = recipe.getResultItem(registryAccess);
        builder.addSlot(RecipeIngredientRole.OUTPUT, 120, 30)
                .addItemStack(output);
    }

    @Override
    public void draw(CrucibleRecipeType recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        
        // Draw arrow
        guiGraphics.drawString(font, "в†’", 65, 33, 0x404040, false);
        
        // Draw aspect requirements
        AspectList aspects = recipe.getAspects();
        if (aspects != null && aspects.size() > 0) {
            int xOffset = 35;
            int yOffset = 55;
            StringBuilder aspectText = new StringBuilder("Aspects: ");
            boolean first = true;
            for (Aspect aspect : aspects.getAspects()) {
                if (!first) aspectText.append(", ");
                first = false;
                int amount = aspects.getAmount(aspect);
                aspectText.append(amount).append(" ").append(aspect.getName());
            }
            
            // Wrap text if too long
            String text = aspectText.toString();
            if (text.length() > 40) {
                text = text.substring(0, 37) + "...";
            }
            guiGraphics.drawString(font, text, 5, yOffset, 0x404040, false);
        }
        
        // Draw research requirement at top
        String research = recipe.getResearch();
        if (research != null && !research.isEmpty()) {
            guiGraphics.drawString(font, "Research: " + research, 5, 5, 0x808080, false);
        }
    }
}




