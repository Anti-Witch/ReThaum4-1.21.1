package thaumcraft.api.research;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.capabilities.IPlayerKnowledge.EnumKnowledgeType;

/**
 * Represents a single stage within a research entry.
 * Each stage can have requirements that must be met to progress to the next stage.
 */
public class ResearchStage {

    /** Localization key for the stage text */
    private String text;

    /** Recipes unlocked at this stage */
    private ResourceLocation[] recipes;

    /** Items that must be obtained to complete this stage */
    private Object[] obtain;

    /** Items that must be crafted to complete this stage */
    private Object[] craft;

    /** References to specific craft requirements */
    private int[] craftReference;

    /** Knowledge requirements for this stage */
    private Knowledge[] know;

    /** Research keys that must be completed for this stage */
    private String[] research;

    /** Icons to display for research requirements */
    private String[] researchIcon;

    /** Amount of warp gained when completing this stage */
    private int warp;

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

    public Object[] getObtain() {
        return obtain;
    }

    public void setObtain(Object[] obtain) {
        this.obtain = obtain;
    }

    public Object[] getCraft() {
        return craft;
    }

    public void setCraft(Object[] craft) {
        this.craft = craft;
    }

    public int[] getCraftReference() {
        return craftReference;
    }

    public void setCraftReference(int[] craftReference) {
        this.craftReference = craftReference;
    }

    public Knowledge[] getKnow() {
        return know;
    }

    public void setKnow(Knowledge[] know) {
        this.know = know;
    }

    public String[] getResearch() {
        return research;
    }

    public void setResearch(String[] research) {
        this.research = research;
    }

    public String[] getResearchIcon() {
        return researchIcon;
    }

    public void setResearchIcon(String[] researchIcon) {
        this.researchIcon = researchIcon;
    }

    public int getWarp() {
        return warp;
    }

    public void setWarp(int warp) {
        this.warp = warp;
    }

    /**
     * Represents a knowledge requirement or reward.
     */
    public static class Knowledge {
        public EnumKnowledgeType type;
        public ResearchCategory category;
        public int amount;

        public Knowledge(EnumKnowledgeType type, ResearchCategory category, int amount) {
            this.type = type;
            this.category = category;
            this.amount = amount;
        }

        /**
         * Parse a knowledge requirement from a string.
         * Format: "TYPE;amount" or "TYPE;category;amount"
         * @param text the string to parse
         * @return the parsed Knowledge, or null if invalid
         */
        public static Knowledge parse(String text) {
            String[] parts = text.split(";");
            
            if (parts.length == 2) {
                // Format: TYPE;amount
                try {
                    int amount = Integer.parseInt(parts[1]);
                    EnumKnowledgeType type = EnumKnowledgeType.valueOf(parts[0].toUpperCase());
                    if (type != null && !type.hasFields() && amount > 0) {
                        return new Knowledge(type, null, amount);
                    }
                } catch (Exception e) {
                    return null;
                }
            } else if (parts.length == 3) {
                // Format: TYPE;category;amount
                try {
                    int amount = Integer.parseInt(parts[2]);
                    EnumKnowledgeType type = EnumKnowledgeType.valueOf(parts[0].toUpperCase());
                    ResearchCategory category = ResearchCategories.getResearchCategory(parts[1].toUpperCase());
                    if (type != null && category != null && amount > 0) {
                        return new Knowledge(type, category, amount);
                    }
                } catch (Exception e) {
                    return null;
                }
            }
            
            return null;
        }
    }
}




