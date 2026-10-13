package thaumcraft.api.crafting;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.RecipeMatcher;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;

import java.util.List;

/**
 * Represents an infusion altar recipe.
 * Infusion recipes require a central item on the runic matrix,
 * component items on pedestals, and essentia from nearby jars.
 */
public class InfusionRecipe implements IThaumcraftRecipe {

    /** The aspects (essentia) required for this recipe */
    public AspectList aspects;

    /** The research key required to unlock this recipe */
    public String research;

    /** The component items required on pedestals */
    protected NonNullList<Ingredient> components = NonNullList.create();

    /** The central item on the matrix (Ingredient.EMPTY means any item is valid) */
    public Ingredient sourceInput;

    /** The recipe output (can be ItemStack or other types for special recipes) */
    public Object recipeOutput;

    /** The instability level of this recipe (affects chance of bad effects) */
    public int instability;

    private String group = "";

    /**
     * Creates a new infusion recipe.
     *
     * @param research the research key required
     * @param outputResult the output (usually ItemStack)
     * @param instability the instability level (higher = more dangerous)
     * @param aspects the essentia required
     * @param centralItem the item required on the matrix
     * @param recipe the component items required on pedestals
     */
    public InfusionRecipe(String research, Object outputResult, int instability, 
                          AspectList aspects, Object centralItem, Object... recipe) {
        this.research = research;
        this.recipeOutput = outputResult;
        this.aspects = aspects;
        this.instability = instability;
        
        this.sourceInput = ThaumcraftApiHelper.getIngredient(centralItem);
        if (sourceInput == null) {
            throw new RuntimeException("Invalid infusion central item: " + centralItem);
        }

        for (Object ingredient : recipe) {
            Ingredient ing = ThaumcraftApiHelper.getIngredient(ingredient);
            if (ing != null) {
                components.add(ing);
            } else {
                StringBuilder error = new StringBuilder("Invalid infusion recipe: ");
                for (Object tmp : recipe) {
                    error.append(tmp).append(", ");
                }
                error.append(outputResult);
                throw new RuntimeException(error.toString());
            }
        }
    }

    /**
     * Checks if this recipe matches the current infusion setup.
     *
     * @param pedestalItems the items on the pedestals
     * @param centralItem the item on the runic matrix
     * @param level the world
     * @param player the player crafting
     * @return true if the recipe can be crafted
     */
    public boolean matches(List<ItemStack> pedestalItems, ItemStack centralItem, 
                          Level level, Player player) {
        if (getRecipeInput() == null) return false;
        
        if (!ThaumcraftCapabilities.isResearchKnown(player, research)) {
            return false;
        }

        boolean centralMatches = getRecipeInput() == Ingredient.EMPTY || 
                                 getRecipeInput().test(centralItem);
        boolean componentsMatch = RecipeMatcher.findMatches(pedestalItems, getComponents()) != null;
        
        return centralMatches && componentsMatch;
    }

    @Override
    public String getResearch() {
        return research;
    }

    public Ingredient getRecipeInput() {
        return sourceInput;
    }

    public NonNullList<Ingredient> getComponents() {
        return components;
    }

    public Object getRecipeOutput() {
        return recipeOutput;
    }

    public AspectList getAspects() {
        return aspects;
    }

    /**
     * Gets the recipe output, potentially modified based on input.
     * Override this for recipes that modify the output based on input properties.
     */
    public Object getRecipeOutput(Player player, ItemStack input, List<ItemStack> components) {
        return recipeOutput;
    }

    /**
     * Gets the aspects required, potentially modified based on input.
     * Override this for recipes with variable essentia costs.
     */
    public AspectList getAspects(Player player, ItemStack input, List<ItemStack> components) {
        return aspects;
    }

    /**
     * Gets the instability level, potentially modified based on input.
     * Override this for recipes with variable instability.
     */
    public int getInstability(Player player, ItemStack input, List<ItemStack> components) {
        return instability;
    }

    public String getGroup() {
        return group;
    }

    public InfusionRecipe setGroup(ResourceLocation location) {
        this.group = location.toString();
        return this;
    }

    public InfusionRecipe setGroup(String group) {
        this.group = group;
        return this;
    }
}




