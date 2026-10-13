package thaumcraft.api.aspects;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/**
 * AspectHelper - Utility class for working with aspects.
 * Handles aspect lookups for items, blocks, and entities.
 * 
 * @author Azanor
 * Ported to 1.20.1
 */
@SuppressWarnings({"deprecation", "unchecked", "rawtypes"})
public class AspectHelper {
    
    /**
     * Registry of aspects for items/blocks
     * Key is the ResourceLocation string (e.g., "minecraft:stone")
     */
    private static Map<String, AspectList> objectTags = new HashMap<>();
    
    /**
     * Registry of aspects for entities
     * Key is the entity type ResourceLocation string
     */
    private static Map<String, AspectList> entityTags = new HashMap<>();
    
    /**
     * Get the aspects associated with an ItemStack
     * @param stack the item to query
     * @return the aspects for this item, or null if none
     */
    public static AspectList getObjectAspects(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) {
            return null;
        }
        
        // Check for specific item with NBT/damage
        // For now, just use the base item
        String key = itemId.toString();
        
        return objectTags.get(key);
    }
    
    /**
     * Register aspects for an item
     * @param stack the item to register
     * @param aspects the aspects to associate
     */
    public static void registerObjectTag(ItemStack stack, AspectList aspects) {
        if (stack == null || stack.isEmpty() || aspects == null) {
            return;
        }
        
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId != null) {
            objectTags.put(itemId.toString(), aspects);
        }
    }
    
    /**
     * Register aspects for an item by ResourceLocation
     * @param itemId the item's resource location
     * @param aspects the aspects to associate
     */
    public static void registerObjectTag(ResourceLocation itemId, AspectList aspects) {
        if (itemId != null && aspects != null) {
            objectTags.put(itemId.toString(), aspects);
        }
    }
    
    /**
     * Register aspects for an entity type
     * @param entityId the entity type's resource location
     * @param aspects the aspects to associate
     */
    public static void registerEntityTag(ResourceLocation entityId, AspectList aspects) {
        if (entityId != null && aspects != null) {
            entityTags.put(entityId.toString(), aspects);
        }
    }
    
    /**
     * Get the aspects associated with an entity type
     * @param entityId the entity type resource location
     * @return the aspects for this entity, or null if none
     */
    public static AspectList getEntityAspects(ResourceLocation entityId) {
        if (entityId == null) {
            return null;
        }
        return entityTags.get(entityId.toString());
    }
    
    /**
     * Get the aspects associated with an entity instance
     * @param entity the entity to query
     * @return the aspects for this entity, or null if none
     */
    public static AspectList getEntityAspects(Entity entity) {
        if (entity == null) {
            return null;
        }
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return getEntityAspects(entityId);
    }
    
    /**
     * Combine two primal aspects to get a compound aspect
     * @param aspect1 first aspect
     * @param aspect2 second aspect
     * @return the resulting compound aspect, or null if combination doesn't exist
     */
    public static Aspect getCombinationResult(Aspect aspect1, Aspect aspect2) {
        if (aspect1 == null || aspect2 == null) {
            return null;
        }
        
        // Check both orderings
        int hash1 = (aspect1.getTag() + aspect2.getTag()).hashCode();
        int hash2 = (aspect2.getTag() + aspect1.getTag()).hashCode();
        
        Aspect result = Aspect.mixList.get(hash1);
        if (result == null) {
            result = Aspect.mixList.get(hash2);
        }
        
        return result;
    }
    
    /**
     * Check if two aspects can be combined
     * @param aspect1 first aspect
     * @param aspect2 second aspect
     * @return true if these aspects can form a compound
     */
    public static boolean canCombine(Aspect aspect1, Aspect aspect2) {
        return getCombinationResult(aspect1, aspect2) != null;
    }
    
    /**
     * Clear all registered aspect tags
     * Used for reloading
     */
    public static void clearTags() {
        objectTags.clear();
        entityTags.clear();
    }
    
    /**
     * Get the number of registered object tags
     */
    public static int getObjectTagCount() {
        return objectTags.size();
    }
    
    /**
     * Get the number of registered entity tags
     */
    public static int getEntityTagCount() {
        return entityTags.size();
    }
}




