package thaumcraft.api.wands;

import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.aspects.AspectList;


/**
 * FocusUpgradeType - Represents an upgrade type for wand foci.
 * Ported from TC4 FocusUpgradeType.
 */
public class FocusUpgradeType {

    public final int id;
    public final ResourceLocation icon;
    public final String name;
    public final String text;
    public final thaumcraft.api.aspects.AspectList cost;

    public FocusUpgradeType(int id, ResourceLocation icon, String name, String text, thaumcraft.api.aspects.AspectList cost) {
        this.id = id;
        this.icon = icon;
        this.name = name;
        this.text = text;
        this.cost = cost;
    }

    public int getId() {
        return id;
    }

    public ResourceLocation getIcon() {
        return icon;
    }

    public String getName() {
        return name;
    }

    public String getText() {
        return text;
    }

    public thaumcraft.api.aspects.AspectList getCost() {
        return cost;
    }

    // ============================================================
    // PREDEFINED UPGRADE TYPES (TC4 standard)
    // ============================================================

    public static FocusUpgradeType frugal;
    public static FocusUpgradeType potency;
    public static FocusUpgradeType enlarge;
    public static FocusUpgradeType extend;
    public static FocusUpgradeType treasure;

    // Focus-specific upgrades
    public static FocusUpgradeType fireball;
    public static FocusUpgradeType firebeam;
    public static FocusUpgradeType alchemistsfire;
    public static FocusUpgradeType chainlightning;
    public static FocusUpgradeType earthshock;
    public static FocusUpgradeType dowsing;
    public static FocusUpgradeType silktouch;
    public static FocusUpgradeType scattershot;
    public static FocusUpgradeType iceboulder;
    public static FocusUpgradeType frostbeam;
    public static FocusUpgradeType alchemistsfrost;
    public static FocusUpgradeType architect;

    public static void init() {
        // Basic upgrades (available to most foci)
        frugal = new FocusUpgradeType(0, ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/foci/frugal.png"),
                "focus.upgrade.frugal.name", "focus.upgrade.frugal.text",
                new thaumcraft.api.aspects.AspectList().add(thaumcraft.api.aspects.Aspect.ORDER, 1));

        potency = new FocusUpgradeType(1, ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/foci/potency.png"),
                "focus.upgrade.potency.name", "focus.upgrade.potency.text",
                new thaumcraft.api.aspects.AspectList().add(thaumcraft.api.aspects.Aspect.ENERGY, 1));

        enlarge = new FocusUpgradeType(2, ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/foci/enlarge.png"),
                "focus.upgrade.enlarge.name", "focus.upgrade.enlarge.text",
                new thaumcraft.api.aspects.AspectList().add(thaumcraft.api.aspects.Aspect.ENERGY, 1).add(thaumcraft.api.aspects.Aspect.ORDER, 1));

        extend = new FocusUpgradeType(3, ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/foci/extend.png"),
                "focus.upgrade.extend.name", "focus.upgrade.extend.text",
                new thaumcraft.api.aspects.AspectList().add(thaumcraft.api.aspects.Aspect.AIR, 1).add(thaumcraft.api.aspects.Aspect.ORDER, 1));

        treasure = new FocusUpgradeType(4, ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/foci/treasure.png"),
                "focus.upgrade.treasure.name", "focus.upgrade.treasure.text",
                new thaumcraft.api.aspects.AspectList().add(thaumcraft.api.aspects.Aspect.ENTROPY, 1));

        // Fire focus upgrades
        fireball = new FocusUpgradeType(9, ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/foci/fireball.png"),
                "focus.upgrade.fireball.name", "focus.upgrade.fireball.text",
                new thaumcraft.api.aspects.AspectList().add(thaumcraft.api.aspects.Aspect.DARKNESS, 1));

        firebeam = new FocusUpgradeType(10, ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/foci/firebeam.png"),
                "focus.upgrade.firebeam.name", "focus.upgrade.firebeam.text",
                new thaumcraft.api.aspects.AspectList().add(thaumcraft.api.aspects.Aspect.ENERGY, 1).add(thaumcraft.api.aspects.Aspect.AIR, 1));

        alchemistsfire = new FocusUpgradeType(13, ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/foci/alchemistsfire.png"),
                "focus.upgrade.alchemistsfire.name", "focus.upgrade.alchemistsfire.text",
                new thaumcraft.api.aspects.AspectList().add(thaumcraft.api.aspects.Aspect.FIRE, 2).add(thaumcraft.api.aspects.Aspect.ENTROPY, 1));

        // Shock focus upgrades
        chainlightning = new FocusUpgradeType(18, ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/foci/chainlightning.png"),
                "focus.upgrade.chainlightning.name", "focus.upgrade.chainlightning.text",
                new thaumcraft.api.aspects.AspectList().add(thaumcraft.api.aspects.Aspect.ENERGY, 1));

        earthshock = new FocusUpgradeType(19, ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/foci/earthshock.png"),
                "focus.upgrade.earthshock.name", "focus.upgrade.earthshock.text",
                new thaumcraft.api.aspects.AspectList().add(thaumcraft.api.aspects.Aspect.EARTH, 1).add(thaumcraft.api.aspects.Aspect.ENERGY, 1));

        // Excavation focus upgrades
        dowsing = new FocusUpgradeType(16, ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/foci/dowsing.png"),
                "focus.upgrade.dowsing.name", "focus.upgrade.dowsing.text",
                new thaumcraft.api.aspects.AspectList().add(thaumcraft.api.aspects.Aspect.MAGIC, 1));

        silktouch = new FocusUpgradeType(17, ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/foci/silktouch.png"),
                "focus.upgrade.silktouch.name", "focus.upgrade.silktouch.text",
                new thaumcraft.api.aspects.AspectList().add(thaumcraft.api.aspects.Aspect.ORDER, 1).add(thaumcraft.api.aspects.Aspect.ENTROPY, 1));

        // Frost focus upgrades
        scattershot = new FocusUpgradeType(11, ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/foci/scattershot.png"),
                "focus.upgrade.scattershot.name", "focus.upgrade.scattershot.text",
                new thaumcraft.api.aspects.AspectList().add(thaumcraft.api.aspects.Aspect.AIR, 1).add(thaumcraft.api.aspects.Aspect.WATER, 1));

        iceboulder = new FocusUpgradeType(12, ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/foci/iceboulder.png"),
                "focus.upgrade.iceboulder.name", "focus.upgrade.iceboulder.text",
                new thaumcraft.api.aspects.AspectList().add(thaumcraft.api.aspects.Aspect.EARTH, 1).add(thaumcraft.api.aspects.Aspect.WATER, 1));

        frostbeam = new FocusUpgradeType(14, ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/foci/alchemistsfrost.png"),
                "focus.upgrade.frostbeam.name", "focus.upgrade.frostbeam.text",
                new thaumcraft.api.aspects.AspectList().add(thaumcraft.api.aspects.Aspect.AIR, 1).add(thaumcraft.api.aspects.Aspect.WATER, 1).add(thaumcraft.api.aspects.Aspect.ENTROPY, 1));

        alchemistsfrost = new FocusUpgradeType(15, ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/foci/alchemistsfrost.png"),
                "focus.upgrade.alchemistsfrost.name", "focus.upgrade.alchemistsfrost.text",
                new thaumcraft.api.aspects.AspectList().add(thaumcraft.api.aspects.Aspect.WATER, 1).add(thaumcraft.api.aspects.Aspect.ENTROPY, 1));

        architect = new FocusUpgradeType(20, ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/foci/architect.png"),
                "focus.upgrade.architect.name", "focus.upgrade.architect.text",
                new thaumcraft.api.aspects.AspectList().add(thaumcraft.api.aspects.Aspect.ORDER, 1).add(thaumcraft.api.aspects.Aspect.ENERGY, 1));
    }
}
