package thaumcraft.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.entity.player.Player;
import thaumcraft.Thaumcraft;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.crafting.IThaumcraftRecipe;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.common.lib.crafting.CrucibleRecipeType;
import thaumcraft.common.lib.crafting.InfusionRecipeType;
import thaumcraft.compat.jei.arcane.ArcaneWorkbenchCategory;
import thaumcraft.compat.jei.crucible.CrucibleCategory;
import thaumcraft.compat.jei.infusion.InfusionCategory;
import thaumcraft.init.ModBlocks;

import java.util.List;
import java.util.stream.Collectors;

/**
 * JEI Plugin for Thaumcraft.
 * Registers recipe categories for:
 * - Arcane Workbench (shaped and shapeless)
 * - Crucible
 * - Infusion Altar
 */
@JeiPlugin
public class ThaumcraftJEIPlugin implements IModPlugin {

    public static final ResourceLocation PLUGIN_ID = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "jei_plugin");

    // Recipe types for JEI
    public static final RecipeType<IArcaneRecipe> ARCANE_TYPE =
            RecipeType.create(Thaumcraft.MODID, "arcane_workbench", IArcaneRecipe.class);

    public static final RecipeType<CrucibleRecipeType> CRUCIBLE_TYPE =
            RecipeType.create(Thaumcraft.MODID, "crucible", CrucibleRecipeType.class);

    public static final RecipeType<InfusionRecipeType> INFUSION_TYPE =
            RecipeType.create(Thaumcraft.MODID, "infusion", InfusionRecipeType.class);

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IJeiHelpers jeiHelpers = registration.getJeiHelpers();
        IGuiHelper guiHelper = jeiHelpers.getGuiHelper();

        // Register Arcane Workbench category (handles both shaped and shapeless)
        registration.addRecipeCategories(new ArcaneWorkbenchCategory(guiHelper));

        // Register Crucible category
        registration.addRecipeCategories(new CrucibleCategory(guiHelper));

        // Register Infusion Altar category
        registration.addRecipeCategories(new InfusionCategory(guiHelper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        
        RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();
        Player player = Minecraft.getInstance().player;

        // Get all arcane recipes (both shaped and shapeless use same recipe type)
        List<IArcaneRecipe> arcaneRecipes = new java.util.ArrayList<>();
        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            if (holder.value() instanceof IArcaneRecipe r) arcaneRecipes.add(r);
        }

        registration.addRecipes(ARCANE_TYPE, hideUnowned(arcaneRecipes, player));

        // Get all crucible recipes
        List<CrucibleRecipeType> crucibleRecipes = new java.util.ArrayList<>();
        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            if (holder.value() instanceof CrucibleRecipeType r) crucibleRecipes.add(r);
        }

        registration.addRecipes(CRUCIBLE_TYPE, hideUnowned(crucibleRecipes, player));

        // Get all infusion recipes
        List<InfusionRecipeType> infusionRecipes = new java.util.ArrayList<>();
        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            if (holder.value() instanceof InfusionRecipeType r) infusionRecipes.add(r);
        }

        registration.addRecipes(INFUSION_TYPE, hideUnowned(infusionRecipes, player));

        Thaumcraft.LOGGER.info("JEI: Registered {} arcane, {} crucible, {} infusion recipes",
                arcaneRecipes.size(), crucibleRecipes.size(), infusionRecipes.size());
    }

    /**
     * Filter out recipes whose research prerequisite the player has not unlocked
     * yet. This hides Thaumcraft content from JEI until it is researched (craft-gating).
     */
    private static <T> List<T> hideUnowned(List<T> recipes, Player player) {
        if (player == null) {
            return recipes;
        }
        return recipes.stream()
                .filter(r -> {
                    if (r instanceof IThaumcraftRecipe tr) {
                        String research = tr.getResearch();
                        if (research != null && !research.isEmpty()) {
                            return ThaumcraftCapabilities.isResearchKnown(player, research);
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        // Arcane Workbench is the catalyst for arcane recipes
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ARCANE_WORKBENCH.get()), ARCANE_TYPE);

        // Crucible is the catalyst for crucible recipes
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CRUCIBLE.get()), CRUCIBLE_TYPE);

        // Infusion Matrix is the catalyst for infusion recipes
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.INFUSION_MATRIX.get()), INFUSION_TYPE);
    }
}




