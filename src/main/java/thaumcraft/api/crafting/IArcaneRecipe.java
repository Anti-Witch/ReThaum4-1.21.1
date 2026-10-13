package thaumcraft.api.crafting;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.AspectList;

/**
 * Interface for arcane workbench recipes.
 * These recipes consume vis from the aura and require specific crystals.
 */
public interface IArcaneRecipe extends Recipe<RecipeInput>, IThaumcraftRecipe {

    /**
     * Assemble the recipe result from a standard recipe input.
     */
    @Override
    ItemStack assemble(RecipeInput input, HolderLookup.Provider provider);

    /**
     * Check if the recipe matches the given input.
     */
    @Override
    boolean matches(RecipeInput input, Level level);

    /**
     * Get the recipe result (used for display).
     */
    @Override
    ItemStack getResultItem(HolderLookup.Provider provider);

    @Override
    RecipeSerializer<?> getSerializer();

    @Override
    RecipeType<?> getType();

    /**
     * Assemble the recipe result from the arcane workbench inventory.
     */
    ItemStack assemble(IArcaneWorkbench inv, HolderLookup.Provider provider);

    /**
     * Check if the recipe matches the given arcane workbench inventory.
     */
    boolean matches(IArcaneWorkbench inv, Level level);

    /**
     * Get the remaining items after crafting.
     */
    NonNullList<ItemStack> getRemainingItems(IArcaneWorkbench inv);

    /**
     * Check if this is a special recipe (no recipe book entry).
     */
    default boolean isSpecial() { return true; }

    /**
     * Gets the ingredients used by this recipe (for display in JEI).
     * @return the list of ingredients
     */
    NonNullList<Ingredient> getIngredients();

    /**
     * Gets the vis cost for this recipe.
     * @return the amount of vis required
     */
    int getVis();

    /**
     * Gets the research key required to unlock this recipe.
     * @return the research key
     */
    @Override
    String getResearch();

    /**
     * Gets the crystals required for this recipe.
     * The AspectList contains the primal aspects and their required amounts.
     * @return the crystal requirements
     */
    AspectList getCrystals();
}
