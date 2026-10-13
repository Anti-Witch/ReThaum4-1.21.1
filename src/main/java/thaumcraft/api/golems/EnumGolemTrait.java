package thaumcraft.api.golems;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Enumeration of all golem traits.
 * Traits define special behaviors and attributes for golems.
 * Some traits have opposites that cancel each other out.
 */
public enum EnumGolemTrait {
    SMART(ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/golem/tag_smart.png")),
    DEFT(ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/golem/tag_deft.png")),
    CLUMSY(ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/golem/tag_clumsy.png")),
    FIGHTER(ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/golem/tag_fighter.png")),
    WHEELED(ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/golem/tag_wheeled.png")),
    FLYER(ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/golem/tag_flyer.png")),
    CLIMBER(ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/golem/tag_climber.png")),
    HEAVY(ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/golem/tag_heavy.png")),
    LIGHT(ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/golem/tag_light.png")),
    FRAGILE(ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/golem/tag_fragile.png")),
    REPAIR(ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/golem/tag_repair.png")),
    SCOUT(ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/golem/tag_scout.png")),
    ARMORED(ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/golem/tag_armored.png")),
    BRUTAL(ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/golem/tag_brutal.png")),
    FIREPROOF(ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/golem/tag_fireproof.png")),
    BREAKER(ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/golem/tag_breaker.png")),
    HAULER(ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/golem/tag_hauler.png")),
    RANGED(ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/golem/tag_ranged.png")),
    BLASTPROOF(ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/golem/tag_blastproof.png"));

    static {
        // Set up opposing traits that cancel each other out
        CLUMSY.opposite = DEFT;
        DEFT.opposite = CLUMSY;

        HEAVY.opposite = LIGHT;
        LIGHT.opposite = HEAVY;

        FRAGILE.opposite = ARMORED;
        ARMORED.opposite = FRAGILE;
    }

    public final ResourceLocation icon;
    public EnumGolemTrait opposite;

    EnumGolemTrait(ResourceLocation icon) {
        this.icon = icon;
    }

    public Component getLocalizedName() {
        return Component.translatable("golem.trait." + name().toLowerCase());
    }

    public Component getLocalizedDescription() {
        return Component.translatable("golem.trait.text." + name().toLowerCase());
    }
}




