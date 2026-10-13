package thaumcraft.api.aspects;

import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ThaumcraftApi;

public class AspectEventProxy {
    
    /**
     * Used to assign aspects to the given item/block.
     * @param item the item passed.
     * @param aspects A AspectList of the associated aspects
     */
    public void registerObjectTag(ItemStack item, AspectList aspects) {
        ThaumcraftApi.registerObjectTag(item, aspects);
    }   
    
    /**
     * Used to assign aspects to the given item tag. 
     * @param tagName the tag name (e.g. "c:ingots/iron")
     * @param aspects A AspectList of the associated aspects
     */
    public void registerObjectTag(String tagName, AspectList aspects) {
        ThaumcraftApi.registerObjectTag(tagName, aspects);
    }
    
    /**
     * Used to assign aspects to the given item/block. 
     * Attempts to automatically generate aspect tags by checking registered recipes.
     * IMPORTANT - this should only be used if you are not happy with the default aspects the object would be assigned.
     * @param item the item to register
     * @param aspects A AspectList of the associated aspects
     */
    public void registerComplexObjectTag(ItemStack item, AspectList aspects) {
        ThaumcraftApi.registerComplexObjectTag(item, aspects);
    }
    
    /**
     * Used to assign aspects to the given item tag. 
     * Attempts to automatically generate aspect tags by checking registered recipes.
     * IMPORTANT - this should only be used if you are not happy with the default aspects the object would be assigned.
     * @param tagName the tag name
     * @param aspects A AspectList of the associated aspects
     */
    public void registerComplexObjectTag(String tagName, AspectList aspects) {
        ThaumcraftApi.registerComplexObjectTag(tagName, aspects);
    }

}




