package thaumcraft.api.research;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Represents additional content that can be unlocked after completing research.
 * Addenda provide bonus recipes or information.
 */
public class ResearchAddendum {

    /** Localization key for the addendum text */
    private String text;

    /** Recipes unlocked by this addendum */
    private ResourceLocation[] recipes;

    /** Research keys that must be completed for this addendum */
    private String[] research;

    // Getters and Setters

    public String getText() {
        return text;
    }

    public Component getTextLocalized() {
        return Component.translatable(getText());
    }

    public void setText(String text) {
        this.text = text;
    }

    public ResourceLocation[] getRecipes() {
        return recipes;
    }

    public void setRecipes(ResourceLocation[] recipes) {
        this.recipes = recipes;
    }

    public String[] getResearch() {
        return research;
    }

    public void setResearch(String[] research) {
        this.research = research;
    }
}




