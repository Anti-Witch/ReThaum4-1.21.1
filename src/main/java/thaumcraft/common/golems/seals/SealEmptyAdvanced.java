package thaumcraft.common.golems.seals;

import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.seals.ISealConfigToggles;

/**
 * SealEmptyAdvanced - Advanced version of the Empty seal.
 * Extends the basic empty seal with a 9-slot filter.
 * Requires SMART trait.
 */
public class SealEmptyAdvanced extends SealEmpty implements ISealConfigToggles {

    private ResourceLocation iconAdvanced;

    public SealEmptyAdvanced() {
        super();
        iconAdvanced = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "items/seals/seal_empty_advanced");
    }

    @Override
    public String getKey() {
        return "thaumcraft:empty_advanced";
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
