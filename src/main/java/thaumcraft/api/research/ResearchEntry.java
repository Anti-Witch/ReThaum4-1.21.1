package thaumcraft.api.research;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchStage.Knowledge;

import java.util.Arrays;

/**
 * Represents a single research entry in the Thaumonomicon.
 * Research entries contain stages, requirements, and rewards.
 */
public class ResearchEntry {

    /** A short string used as a key for this research. Must be unique */
    private String key;

    /** A short string used as a reference to the research category to which this must be added */
    private String category;

    /** A text name of the research entry. Can be a localizable string key */
    private String name;

    /** Links to any research that needs to be completed before this research can be discovered or learned */
    private String[] parents;

    /**
     * Research that must be completed before this entry can be unlocked, but
     * that is not drawn as a visible link on the research tree (TC4
     * {@code ResearchItem.parentsHidden}).
     */
    private String[] parentsHidden;

    /** Any research linked to this that will be unlocked automatically when this research is complete */
    private String[] siblings;

    /** The horizontal position of the research icon */
    private int displayColumn;

    /** The vertical position of the research icon */
    private int displayRow;

    /** The icon(s) to be used for this research (can be ItemStack, ResourceLocation, or Aspect) */
    private Object[] icons;

    /** Special meta-data tags that indicate how this research must be handled */
    private EnumResearchMeta[] meta;

    /** Items the player will receive on completion of this research */
    private ItemStack[] rewardItem;

    /** Knowledge the player will receive on completion of this research */
    private Knowledge[] rewardKnow;

    /** The various stages present in this research entry */
    private ResearchStage[] stages;

    /** The various addenda present in this research entry */
    private ResearchAddendum[] addenda;

    /**
     * The TC4 hex-grid puzzle data for this research.
     * Required aspects (the ones that must be connected on the note canvas)
     * and complexity (how large the research grid is - 1, 2 or 3).
     * These mirror TC4's ResearchItem.tags / ResearchItem.complexity and are
     * used by the TC4-style research table hex-grid minigame.
     */
    private AspectList requiredAspects;
    private int complexity = 1;

    /**
     * TC4 discovery triggers ({@code setAspectTriggers}): discovering any of
     * these aspects makes a hidden research entry known.
     */
    private Aspect[] aspectTriggers;

    /**
     * TC4 discovery triggers ({@code setItemTriggers}): carrying any of these
     * items makes a hidden research entry known.
     */
    private ItemStack[] itemTriggers;

    /**
     * TC4 discovery triggers ({@code setEntityTriggers}): scanning/killing one
     * of these entities makes a hidden research entry known.
     */
    private String[] entityTriggers;

    /**
     * Meta tags that modify research behavior
     */
    public enum EnumResearchMeta {
        /** Round icon frame */
        ROUND,
        /** Spiky icon frame (visual frame style from TC4) */
        SPIKY,
        /** Reverse direction of parent connections */
        REVERSE,
        /** Hidden until requirements are met */
        HIDDEN,
        /** Automatically unlocked when parents are complete */
        AUTOUNLOCK,
        /** Hexagonal icon frame */
        HEX,
        /**
         * TC4 {@code isConcealed()}: the node is not drawn until every parent
         * (including hidden parents) is complete.
         */
        CONCEALED,
        /**
         * TC4 {@code isSecondary()}: drawn with the hexagonal frame at normal
         * research difficulty.
         */
        SECONDARY,
        /**
         * TC4 {@code isStub()}: no research note is required - the entry is
         * unlocked directly (these entries carry no aspects).
         */
        STUB,
        /**
         * TC4 {@code isLost()}: only discoverable by scanning, never by
         * knowledge fragments. Behaves like {@link #HIDDEN} in the browser.
         */
        LOST
    }

    // Getters and Setters

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    /**
     * @return the localized name of this research entry
     */
    public Component getLocalizedName() {
        return Component.translatable(getName());
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getParents() {
        return parents;
    }

    /**
     * @return parents with ALL prefixes and postfixes stripped away
     */
    public String[] getParentsClean() {
        if (parents == null) return null;
        
        String[] out = getParentsStripped();
        for (int i = 0; i < out.length; i++) {
            if (out[i].contains("@")) {
                out[i] = out[i].substring(0, out[i].indexOf("@"));
            }
        }
        return out;
    }

    /**
     * @return parents with prefixes stripped away
     */
    public String[] getParentsStripped() {
        if (parents == null) return null;
        
        String[] out = new String[parents.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = parents[i];
            if (out[i].startsWith("~")) {
                out[i] = out[i].substring(1);
            }
        }
        return out;
    }

    public void setParents(String[] parents) {
        this.parents = parents;
    }

    public String[] getParentsHidden() {
        return parentsHidden;
    }

    public void setParentsHidden(String[] parentsHidden) {
        this.parentsHidden = parentsHidden;
    }

    /**
     * @return hidden parents with ALL prefixes and postfixes stripped away
     */
    public String[] getParentsHiddenClean() {
        if (parentsHidden == null) return null;

        String[] out = new String[parentsHidden.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = parentsHidden[i];
            if (out[i].startsWith("~")) {
                out[i] = out[i].substring(1);
            }
            if (out[i].contains("@")) {
                out[i] = out[i].substring(0, out[i].indexOf("@"));
            }
        }
        return out;
    }

    public String[] getSiblings() {
        return siblings;
    }

    public void setSiblings(String[] siblings) {
        this.siblings = siblings;
    }

    public int getDisplayColumn() {
        return displayColumn;
    }

    public void setDisplayColumn(int displayColumn) {
        this.displayColumn = displayColumn;
    }

    public int getDisplayRow() {
        return displayRow;
    }

    public void setDisplayRow(int displayRow) {
        this.displayRow = displayRow;
    }

    public Object[] getIcons() {
        return icons;
    }

    public void setIcons(Object[] icons) {
        this.icons = icons;
    }

    public EnumResearchMeta[] getMeta() {
        return meta;
    }

    public boolean hasMeta(EnumResearchMeta me) {
        return meta != null && Arrays.asList(meta).contains(me);
    }

    public void setMeta(EnumResearchMeta[] meta) {
        this.meta = meta;
    }

    public ResearchStage[] getStages() {
        return stages;
    }

    public void setStages(ResearchStage[] stages) {
        this.stages = stages;
    }

    public ItemStack[] getRewardItem() {
        return rewardItem;
    }

    public void setRewardItem(ItemStack[] rewardItem) {
        this.rewardItem = rewardItem;
    }

    public Knowledge[] getRewardKnow() {
        return rewardKnow;
    }

    public void setRewardKnow(Knowledge[] rewardKnow) {
        this.rewardKnow = rewardKnow;
    }

    public ResearchAddendum[] getAddenda() {
        return addenda;
    }

    public AspectList getRequiredAspects() {
        return requiredAspects;
    }

    public void setRequiredAspects(AspectList requiredAspects) {
        this.requiredAspects = requiredAspects;
    }

    public Aspect[] getAspectTriggers() {
        return aspectTriggers;
    }

    public void setAspectTriggers(Aspect[] aspectTriggers) {
        this.aspectTriggers = aspectTriggers;
    }

    public ItemStack[] getItemTriggers() {
        return itemTriggers;
    }

    public void setItemTriggers(ItemStack[] itemTriggers) {
        this.itemTriggers = itemTriggers;
    }

    public String[] getEntityTriggers() {
        return entityTriggers;
    }

    public void setEntityTriggers(String[] entityTriggers) {
        this.entityTriggers = entityTriggers;
    }

    /**
     * @return true when this entry carries any TC4 discovery trigger
     */
    public boolean hasTriggers() {
        return (aspectTriggers != null && aspectTriggers.length > 0)
                || (itemTriggers != null && itemTriggers.length > 0)
                || (entityTriggers != null && entityTriggers.length > 0);
    }

    public int getComplexity() {
        return complexity;
    }

    public void setComplexity(int complexity) {
        this.complexity = complexity;
    }

    public void setAddenda(ResearchAddendum[] addenda) {
        this.addenda = addenda;
    }
}




