package thaumcraft.common.golems.seals;

import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.seals.ISealConfigToggles;

/**
 * SealGuardAdvanced - Advanced version of the Guard seal.
 * Extends the basic guard seal with a larger protection area
 * and requires the SMART trait.
 */
public class SealGuardAdvanced extends SealGuard implements ISealConfigToggles {

    private ResourceLocation iconAdvanced;

    public SealGuardAdvanced() {
        super();
        iconAdvanced = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "items/seals/seal_guard_advanced");
    }

    @Override
    public String getKey() {
        return "thaumcraft:guard_advanced";
    }

    @Override
    public ResourceLocation getSealIcon() {
        return iconAdvanced;
    }

    @Override
    public int[] getGuiCategories() {
        return new int[] { 3, 0, 4 };
    }

    @Override
    public EnumGolemTrait[] getRequiredTags() {
        return new EnumGolemTrait[] { EnumGolemTrait.SMART };
    }
}
