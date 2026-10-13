package thaumcraft.api.wands;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;

/**
 * WandRod - represents a wand rod type (greatwood, silverwood, obsidian, etc.)
 * Stores rod properties: capacity, craft cost, texture, item ingredient.
 */
public class WandRod {

    private String tag;
    private int craftCost;
    int capacity;
    protected ResourceLocation texture;
    ItemStack item;
    IWandRodOnUpdate onUpdate;
    boolean glow;

    public static final java.util.LinkedHashMap<String, WandRod> rods = new java.util.LinkedHashMap<>();

    public WandRod(String tag, int capacity, ItemStack item, int craftCost, ResourceLocation texture) {
        this.setTag(tag);
        this.capacity = capacity;
        this.texture = texture;
        this.item = item;
        this.setCraftCost(craftCost);
        rods.put(tag, this);
    }

    public WandRod(String tag, int capacity, ItemStack item, int craftCost, IWandRodOnUpdate onUpdate, ResourceLocation texture) {
        this.setTag(tag);
        this.capacity = capacity;
        this.texture = texture;
        this.item = item;
        this.setCraftCost(craftCost);
        rods.put(tag, this);
        this.onUpdate = onUpdate;
    }

    public WandRod(String tag, int capacity, ItemStack item, int craftCost) {
        this.setTag(tag);
        this.capacity = capacity;
        this.texture = ResourceLocation.parse("thaumcraft:textures/models/wand_rod_" + getTag() + ".png");
        this.item = item;
        this.setCraftCost(craftCost);
        rods.put(tag, this);
    }

    public WandRod(String tag, int capacity, ItemStack item, int craftCost, IWandRodOnUpdate onUpdate) {
        this.setTag(tag);
        this.capacity = capacity;
        this.texture = ResourceLocation.parse("thaumcraft:textures/models/wand_rod_" + getTag() + ".png");
        this.item = item;
        this.setCraftCost(craftCost);
        rods.put(tag, this);
        this.onUpdate = onUpdate;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    public void setTexture(ResourceLocation texture) {
        this.texture = texture;
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

    public IWandRodOnUpdate getOnUpdate() {
        return onUpdate;
    }

    public void setOnUpdate(IWandRodOnUpdate onUpdate) {
        this.onUpdate = onUpdate;
    }

    public boolean isGlowing() {
        return glow;
    }

    public void setGlowing(boolean hasGlow) {
        this.glow = hasGlow;
    }

    /**
     * The research a player needs to have finished to be able to craft a wand with this rod.
     */
    public String getResearch() {
        return "ROD_" + getTag(); // TC4 uses the exact-case tag (e.g. ROD_greatwood)
    }
}