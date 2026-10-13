package thaumcraft.api.research;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a category/tab in the Thaumonomicon research GUI.
 * Each category contains multiple research entries.
 */
public class ResearchCategory {

    /** Is the smallest column used on the GUI. */
    public int minDisplayColumn;

    /** Is the smallest row used on the GUI. */
    public int minDisplayRow;

    /** Is the biggest column used on the GUI. */
    public int maxDisplayColumn;

    /** Is the biggest row used on the GUI. */
    public int maxDisplayRow;

    /** Icon displayed on the category tab */
    public ResourceLocation icon;

    /** Background texture for the category page */
    public ResourceLocation background;

    /** Optional foreground texture (between background and icons) */
    public ResourceLocation background2;

    /** Research key that must be completed for this category to be visible (null = always visible) */
    public String researchKey;

    /** Unique key for this category */
    public String key;

    /** Aspect formula used to calculate knowledge gain in this category */
    public AspectList formula;

    /** All research entries in this category */
    public Map<String, ResearchEntry> research = new HashMap<>();

    public ResearchCategory(String key, String researchKey, AspectList formula, 
                           ResourceLocation icon, ResourceLocation background) {
        this.key = key;
        this.researchKey = researchKey;
        this.icon = icon;
        this.background = background;
        this.background2 = null;
        this.formula = formula;
    }

    public ResearchCategory(String key, String researchKey, AspectList formula,
                           ResourceLocation icon, ResourceLocation background, 
                           ResourceLocation background2) {
        this.key = key;
        this.researchKey = researchKey;
        this.icon = icon;
        this.background = background;
        this.background2 = background2;
        this.formula = formula;
    }

    /**
     * For a given list of aspects this method will calculate the amount of raw knowledge
     * you will be able to gain for the knowledge field.
     * @param aspects the aspects to calculate from
     * @return the knowledge amount
     */
    public int applyFormula(AspectList aspects) {
        return applyFormula(aspects, 1);
    }

    /**
     * This version of the method accepts a multiplier for the total.
     * @param aspects the aspects to calculate from
     * @param mod multiplier to total
     * @return the knowledge amount
     */
    public int applyFormula(AspectList aspects, double mod) {
        if (formula == null) return 0;
        
        double total = 0;
        for (Aspect aspect : formula.getAspects()) {
            total += (mod * mod) * aspects.getAmount(aspect) * (formula.getAmount(aspect) / 10.0);
        }
        
        if (total > 0) {
            total = Math.sqrt(total);
        }
        
        return Mth.ceil(total);
    }
}




