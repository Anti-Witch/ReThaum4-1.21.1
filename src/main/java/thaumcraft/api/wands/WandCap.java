package thaumcraft.api.wands;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * WandCap - represents a wand cap type (iron, gold, thaumium, void, etc.)
 * Stores cap properties: discount modifiers, texture, item ingredient.
 */
public class WandCap {

    private String tag;
    private int craftCost;
    float baseCostModifier;
    List<Aspect> specialCostModifierAspects;
    float specialCostModifier;
    ResourceLocation texture;
    ItemStack item;

    public static final LinkedHashMap<String, WandCap> caps = new LinkedHashMap<>();

    public WandCap(String tag, float discount, ItemStack item, int craftCost) {
        this.setTag(tag);
        this.baseCostModifier = discount;
        this.specialCostModifierAspects = null;
        this.texture = ResourceLocation.parse("thaumcraft:textures/models/wand_cap_" + getTag() + ".png");
        this.item = item;
        this.setCraftCost(craftCost);
        caps.put(tag, this);
    }

    public WandCap(String tag, float discount, List<Aspect> specialAspects, float discountSpecial, ItemStack item, int craftCost) {
        this.setTag(tag);
        this.baseCostModifier = discount;
        this.specialCostModifierAspects = specialAspects;
        this.specialCostModifier = discountSpecial;
        this.texture = ResourceLocation.parse("thaumcraft:textures/models/wand_cap_" + getTag() + ".png");
        this.item = item;
        this.setCraftCost(craftCost);
        caps.put(tag, this);
    }

    public float getBaseCostModifier() {
        return baseCostModifier;
    }

    public List<Aspect> getSpecialCostModifierAspects() {
        return specialCostModifierAspects;
    }

    public float getSpecialCostModifier() {
        return specialCostModifier;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    public void setTexture(ResourceLocation texture) {
        this.texture = texture;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public ItemStack getItem() {
        return item;
    }

    public void setItem(ItemStack item) {
        this.item = item;
    }

    public int getCraftCost() {
        return craftCost;
    }

    public void setCraftCost(int craftCost) {
        this.craftCost = craftCost;
    }

    /**
     * The research a player needs to have finished to be able to craft a wand with this cap.
     */
    public String getResearch() {
        return "CAP_" + getTag();
    }
}