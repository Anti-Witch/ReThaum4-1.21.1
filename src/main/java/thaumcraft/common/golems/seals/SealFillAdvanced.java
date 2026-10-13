package thaumcraft.common.golems.seals;

import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.seals.ISealConfigToggles;

/**
 * SealFillAdvanced - Advanced version of the Fill seal.
 * Extends the basic fill seal with a 9-slot filter.
 * Requires SMART trait.
 */
public class SealFillAdvanced extends SealFill implements ISealConfigToggles {

    private ResourceLocation iconAdvanced;

    public SealFillAdvanced() {
        super();
        iconAdvanced = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "items/seals/seal_fill_advanced");
    }

    @Override
    public String getKey() {
        return "thaumcraft:fill_advanced";
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
        return new int[] { 1, 0, 4 };
    }

    @Override
    public EnumGolemTrait[] getRequiredTags() {
        return new EnumGolemTrait[] { EnumGolemTrait.SMART };
    }
}
