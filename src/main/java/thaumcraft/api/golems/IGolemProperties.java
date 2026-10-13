package thaumcraft.api.golems;

import net.minecraft.world.item.ItemStack;
import thaumcraft.api.golems.parts.GolemAddon;
import thaumcraft.api.golems.parts.GolemArm;
import thaumcraft.api.golems.parts.GolemHead;
import thaumcraft.api.golems.parts.GolemLeg;
import thaumcraft.api.golems.parts.GolemMaterial;

import java.util.Set;

/**
 * Interface for golem property storage.
 * Properties are packed into a long value for efficient storage and network sync.
 * Contains material, head, arms, legs, addon, and rank.
 */
public interface IGolemProperties {

    /**
     * @return Set of all traits this golem has from its parts
     */
    Set<EnumGolemTrait> getTraits();

    /**
     * Check if this golem has a specific trait
     * @param trait The trait to check for
     * @return true if the golem has the trait
     */
    boolean hasTrait(EnumGolemTrait trait);

    /**
     * @return All properties packed into a long value
     */
    long toLong();

    /**
     * @return Array of ItemStacks needed to craft this golem configuration
     */
    ItemStack[] generateComponents();

    // ==================== Material ====================

    void setMaterial(GolemMaterial mat);
    GolemMaterial getMaterial();

    // ==================== Head ====================

    void setHead(GolemHead head);
    GolemHead getHead();

    // ==================== Arms ====================

    void setArms(GolemArm arms);
    GolemArm getArms();

    // ==================== Legs ====================

    void setLegs(GolemLeg legs);
    GolemLeg getLegs();

    // ==================== Addon ====================

    void setAddon(GolemAddon addon);
    GolemAddon getAddon();

    // ==================== Rank ====================

    void setRank(int rank);
    int getRank();
}




