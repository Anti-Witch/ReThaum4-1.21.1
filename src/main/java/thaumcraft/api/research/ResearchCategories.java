package thaumcraft.api.research;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.aspects.AspectList;

import java.util.Collection;
import java.util.LinkedHashMap;

/**
 * Registry for all research categories and entries.
 * Use this class to register new categories and look up research.
 */
public class ResearchCategories {

    /** All registered research categories, keyed by their unique key */
    public static LinkedHashMap<String, ResearchCategory> researchCategories = new LinkedHashMap<>();

    /**
     * Gets a research category by key.
     * @param key the category key
     * @return the category, or null if not found
     */
    public static ResearchCategory getResearchCategory(String key) {
        return researchCategories.get(key);
    }

    /**
     * Gets the localized name of a research category.
     * @param key the category key
     * @return the localized name component
     */
    public static Component getCategoryName(String key) {
        return Component.translatable("tc.research_category." + key);
    }

    /**
     * Gets a research entry by key, searching all categories.
     * @param key the research key
     * @return the research entry, or null if not found
     */
    public static ResearchEntry getResearch(String key) {
        for (ResearchCategory category : researchCategories.values()) {
            for (ResearchEntry entry : category.research.values()) {
                if (entry.getKey().equals(key)) {
                    return entry;
                }
            }
        }
        return null;
    }

    /**
     * Registers a new research category.
     * This should only be done at the PostInit stage.
     *
     * @param key the unique key for this category
     * @param researchKey the research that must be completed for this category to be visible (null = always visible)
     * @param formula aspects required to gain knowledge in this category
     * @param icon the icon for the category tab
     * @param background the background texture for the category page
     * @return the registered category, or null if a category with that key already exists
     */
    public static ResearchCategory registerCategory(String key, String researchKey, AspectList formula,
                                                    ResourceLocation icon, ResourceLocation background) {
        if (getResearchCategory(key) == null) {
            ResearchCategory category = new ResearchCategory(key, researchKey, formula, icon, background);
            researchCategories.put(key, category);
            return category;
        }
        return null;
    }

    /**
     * Registers a new research category with a foreground layer.
     * This should only be done at the PostInit stage.
     *
     * @param key the unique key for this category
     * @param researchKey the research that must be completed for this category to be visible (null = always visible)
     * @param formula aspects required to gain knowledge in this category
     * @param icon the icon for the category tab
     * @param background the background texture for the category page
     * @param background2 the foreground texture (between background and icons)
     * @return the registered category, or null if a category with that key already exists
     */
    public static ResearchCategory registerCategory(String key, String researchKey, AspectList formula,
                                                    ResourceLocation icon, ResourceLocation background,
                                                    ResourceLocation background2) {
        if (getResearchCategory(key) == null) {
            ResearchCategory category = new ResearchCategory(key, researchKey, formula, icon, background, background2);
            researchCategories.put(key, category);
            return category;
        }
        return null;
    }

    /**
     * Gets all registered category keys.
     * @return collection of category keys
     */
    public static Collection<String> getCategoryKeys() {
        return researchCategories.keySet();
    }

    /**
     * Gets all registered categories.
     * @return collection of categories
     */
    public static Collection<ResearchCategory> getCategories() {
        return researchCategories.values();
    }
}




