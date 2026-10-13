package thaumcraft.common.golems.seals;

import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.seals.ISealConfigToggles;

/**
 * SealPickupAdvanced - Advanced version of the Pickup seal.
 * 
 * Extends the basic pickup seal with:
 * - 9-slot filter (vs 1-slot)
 * - Requires SMART trait
 * 
 * The advanced filter allows more precise control over which
 * items golems will pick up.
 * 
 * Ported from 1.12.2.
 */
public class SealPickupAdvanced extends SealPickup implements ISealConfigToggles {
    
    private ResourceLocation iconAdvanced;
    
    public SealPickupAdvanced() {
        super();
        iconAdvanced = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "items/seals/seal_pickup_advanced");
    }
    
    @Override
    public String getKey() {
        return "thaumcraft:pickup_advanced";
    }
    
    @Override
    public int getFilterSize() {
        return 9;
    }
    
    @Override
    public ResourceLocation getSealIcon() {
        return iconAdvanced;
    }
    
    @Override
    public int[] getGuiCategories() {
        return new int[] { 2, 1, 3, 0, 4 };
    }
    
    @Override
    public EnumGolemTrait[] getRequiredTags() {
        return new EnumGolemTrait[] { EnumGolemTrait.SMART };
    }
    
    @Override
    public SealToggle[] getToggles() {
        return props;
    }
    
    @Override
    public void setToggle(int index, boolean value) {
        if (index >= 0 && index < props.length) {
            props[index].setValue(value);
        }
    }
}




