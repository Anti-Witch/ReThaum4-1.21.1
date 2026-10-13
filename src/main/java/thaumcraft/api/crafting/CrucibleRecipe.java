package thaumcraft.api.crafting;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * Represents a crucible recipe.
 * Crucible recipes require throwing a catalyst item into a crucible
 * containing the required aspects (essentia).
 */
public class CrucibleRecipe implements IThaumcraftRecipe {

    private final ItemStack recipeOutput;
    private Ingredient catalyst;
    private AspectList aspects;
    private final String research;
    private String group = "";
    public int hash;

    /**
     * Creates a new crucible recipe.
     *
     * @param researchKey the research required to unlock this recipe
     * @param result the output item
     * @param catalyst the catalyst item (can be ItemStack, Ingredient, or tag string)
     * @param aspects the aspects required in the crucible
     */
    public CrucibleRecipe(String researchKey, ItemStack result, Object catalyst, AspectList aspects) {
        this.recipeOutput = result;
        this.aspects = aspects;
        this.research = researchKey;
        this.catalyst = ThaumcraftApiHelper.getIngredient(catalyst);

        if (this.catalyst == null) {
            throw new RuntimeException("Invalid crucible recipe catalyst: " + catalyst);
        }

        generateHash();
    }

    private void generateHash() {
        StringBuilder hc = new StringBuilder(research);
        hc.append(recipeOutput.toString());
        
        // In 1.21.1 NBT replaced with DataComponents - use component patch for hashing
        if (!recipeOutput.getComponentsPatch().isEmpty()) {
            hc.append(recipeOutput.getComponentsPatch().toString());
        }
        
        for (ItemStack is : catalyst.getItems()) {
            hc.append(is.toString());
            if (!is.getComponentsPatch().isEmpty()) {
                hc.append(is.getComponentsPatch().toString());
            }
        }
        
        hash = hc.toString().hashCode();
    }

    /**
     * Checks if this recipe matches the given catalyst and aspects.
     *
     * @param crucibleAspects the aspects currently in the crucible
     * @param catalystStack the item being thrown in
     * @return true if the recipe can be crafted
     */
    public boolean matches(AspectList crucibleAspects, ItemStack catalystStack) {
        if (!catalyst.test(catalystStack)) return false;
        if (crucibleAspects == null) return false;
        
        for (Aspect aspect : aspects.getAspects()) {
            if (crucibleAspects.getAmount(aspect) < aspects.getAmount(aspect)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the given item can act as a catalyst for this recipe.
     *
     * @param catalystStack the item to check
     * @return true if it matches the catalyst requirement
     */
    public boolean catalystMatches(ItemStack catalystStack) {
        return catalyst.test(catalystStack);
    }

    /**
     * Removes the required aspects from the given aspect list.
     *
     * @param crucibleAspects the aspects to remove from
     * @return a new AspectList with the required aspects removed
     */
    public AspectList removeMatching(AspectList crucibleAspects) {
        AspectList result = new AspectList();
        result.aspects.putAll(crucibleAspects.aspects);
        
        for (Aspect aspect : aspects.getAspects()) {
            result.remove(aspect, aspects.getAmount(aspect));
        }
        
        return result;
    }

    public ItemStack getRecipeOutput() {
        return recipeOutput.copy();
    }

    @Override
    public String getResearch() {
        return research;
    }

    public Ingredient getCatalyst() {
        return catalyst;
    }

    public void setCatalyst(Ingredient catalyst) {
        this.catalyst = catalyst;
    }

    public AspectList getAspects() {
        return aspects;
    }

    public void setAspects(AspectList aspects) {
        this.aspects = aspects;
    }

    public String getGroup() {
        return group;
    }

    public CrucibleRecipe setGroup(ResourceLocation location) {
        this.group = location.toString();
        return this;
    }

    public CrucibleRecipe setGroup(String group) {
        this.group = group;
        return this;
    }
}




