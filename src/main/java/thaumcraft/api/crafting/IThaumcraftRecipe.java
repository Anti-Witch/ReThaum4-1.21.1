package thaumcraft.api.crafting;

/**
 * Base interface for all Thaumcraft recipe types.
 * Provides common methods for research requirements.
 */
public interface IThaumcraftRecipe {

    /**
     * Gets the research key required to unlock this recipe.
     * @return the research key, or null if no research is required
     */
    String getResearch();
}




