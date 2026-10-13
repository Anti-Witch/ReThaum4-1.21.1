package thaumcraft.api.internal;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ThaumcraftApi.EntityTags;
import thaumcraft.api.ThaumcraftApi.SmeltBonus;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.IThaumcraftRecipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Internal variables and methods used by Thaumcraft and the API.
 * These should normally not be accessed directly by addon mods.
 * 
 * @author Azanor
 */
@SuppressWarnings("deprecation")
public class CommonInternals {
    
    /** Research JSON file locations registered by addons */
    public static Map<String, ResourceLocation> jsonLocs = new HashMap<>();
    
    /** Entity aspect tags for scanning */
    public static List<EntityTags> scanEntities = new ArrayList<>();
    
    /** Thaumcraft crafting recipe catalog (infusion, crucible, etc.) */
    public static Map<ResourceLocation, IThaumcraftRecipe> craftingRecipeCatalog = new HashMap<>();
    
    /** Fake recipes for thaumonomicon display only */
    public static Map<ResourceLocation, Object> craftingRecipeCatalogFake = new HashMap<>();
    
    /** Infernal furnace smelting bonuses */
    public static List<SmeltBonus> smeltingBonus = new ArrayList<>();
    
    /** Object -> Aspect mappings (item/block aspect tags) */
    public static ConcurrentHashMap<String, AspectList> objectTags = new ConcurrentHashMap<>();
    
    /** Warp amounts for crafted items */
    public static Map<Object, Integer> warpMap = new HashMap<>();
    
    /** Block -> Seed item mappings */
    public static Map<String, ItemStack> seedList = new HashMap<>();
    
    /**
     * Get a recipe from the crafting catalog.
     */
    public static IThaumcraftRecipe getCatalogRecipe(ResourceLocation key) {
        return craftingRecipeCatalog.get(key);
    }
    
    /**
     * Get a fake recipe from the catalog.
     */
    public static Object getCatalogRecipeFake(ResourceLocation key) {
        return craftingRecipeCatalogFake.get(key);
    }
    
    /**
     * Generate a unique ID for an itemstack based on its NBT serialization.
     * Obviously not truly unique, but unique enough for this purpose.
     */
    public static int generateUniqueItemstackId(ItemStack stack) {
        ItemStack sc = stack.copy();
        sc.setCount(1);
        // In 1.21.1, use item key + component patch for unique ID
        String id = sc.getItem().builtInRegistryHolder().key().location().toString();
        String components = sc.getComponentsPatch().toString();
        return (id + components).hashCode();
    }
    
    /**
     * Generate a unique ID for an itemstack, stripping all NBT data first.
     */
    public static int generateUniqueItemstackIdStripped(ItemStack stack) {
        ItemStack sc = stack.copy();
        sc.setCount(1);
        // In 1.21.1, just use the item registry key (no NBT/components)
        String id = sc.getItem().builtInRegistryHolder().key().location().toString();
        return id.hashCode();
    }
}




