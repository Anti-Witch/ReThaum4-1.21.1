package thaumcraft.common.golems;

import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;

/**
 * TC4 golem animation cores ("the colored pills").
 *
 * In TC4 these were metadata values of the single item {@code ItemGolemCore}
 * (thaumcraft.common.entities.golems.ItemGolemCore): 0-11 were the functional
 * cores, 100 the blank core used as the crafting base. The icons live in
 * {@code textures/item/golem_core_<name>.png} and are the original TC4 art.
 *
 * Behaviour flags ({@link #hasGUI}, {@link #canSort}, {@link #hasInventory})
 * are copied verbatim from TC4's {@code ItemGolemCore} static helpers so that
 * the future golem AI/seal code can rely on them.
 *
 * BODYGUARD and PATROL only exist as textures in the TC4 assets (no code,
 * no lang entries, no recipes in TC4 1.7.10); they are kept as extra
 * creative-only variants for completeness.
 */
public enum GolemCoreType {
    FILL(0, "fill", "COREFILL", true, true, true),
    EMPTY(1, "empty", "COREEMPTY", true, true, true),
    GATHER(2, "gather", "COREGATHER", true, true, true),
    HARVEST(3, "harvest", "COREHARVEST", false, false, false),
    GUARD(4, "guard", "COREGUARD", true, false, false),
    /** TC4 name: "Decanting". */
    LIQUID(5, "liquid", "CORELIQUID", true, false, true),
    /** TC4 name: "Alchemy". */
    ESSENTIA(6, "essentia", "COREALCHEMY", false, false, false),
    /** TC4 name: "Chop". */
    LUMBER(7, "lumber", "CORELUMBER", false, false, false),
    USE(8, "use", "COREUSE", true, true, true),
    BUTCHER(9, "butcher", "COREBUTCHER", false, false, false),
    SORTING(10, "sorting", "CORESORTING", true, true, false),
    /** TC4 name: "Fishing". */
    FISH(11, "fish", "COREFISHING", false, false, false),
    /** Texture-only leftover in TC4, no behaviour defined there. */
    BODYGUARD(12, "bodyguard", "", false, false, false),
    /** Texture-only leftover in TC4, no behaviour defined there. */
    PATROL(13, "patrol", "", false, false, false),
    /** TC4 damage value 100 - the crafting base every core is made from. */
    BLANK(100, "blank", "COREGATHER", false, false, false);

    private final int id;
    private final String name;
    private final String research;
    private final boolean gui;
    private final boolean sortable;
    private final boolean inventory;

    GolemCoreType(int id, String name, String research, boolean gui, boolean sortable, boolean inventory) {
        this.id = id;
        this.name = name;
        this.research = research;
        this.gui = gui;
        this.sortable = sortable;
        this.inventory = inventory;
    }

    /** TC4 metadata/damage value. */
    public int getId() {
        return id;
    }

    /** Icon/lang suffix, e.g. {@code fill} for {@code golem_core_fill.png}. */
    public String getName() {
        return name;
    }

    /** TC4 research key that unlocks this core ("" if none). */
    public String getResearch() {
        return research;
    }

    /** TC4 {@code ItemGolemCore.hasGUI(int)}. */
    public boolean hasGUI() {
        return gui;
    }

    /** TC4 {@code ItemGolemCore.canSort(int)}. */
    public boolean canSort() {
        return sortable;
    }

    /** TC4 {@code ItemGolemCore.hasInventory(int)}. */
    public boolean hasInventory() {
        return inventory;
    }

    public boolean isBlank() {
        return this == BLANK;
    }

    public String getTranslationKey() {
        return "item.thaumcraft.golem_core." + name;
    }

    public ResourceLocation getTexture() {
        return ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "item/golem_core_" + name);
    }

    public static GolemCoreType byId(int id) {
        for (GolemCoreType type : values()) {
            if (type.id == id) return type;
        }
        return BLANK;
    }

    /** All variants that are shown in the creative tab (everything but BLANK first). */
    public static GolemCoreType[] functionalCores() {
        return new GolemCoreType[]{
                FILL, EMPTY, GATHER, HARVEST, GUARD, LIQUID, ESSENTIA,
                LUMBER, USE, BUTCHER, SORTING, FISH, BODYGUARD, PATROL
        };
    }

    // TC4 ItemGolemCore static helpers, kept for the future golem AI.

    public static boolean hasGUI(int core) {
        return byId(core).hasGUI();
    }

    public static boolean canSort(int core) {
        return byId(core).canSort();
    }

    public static boolean hasInventory(int core) {
        return byId(core).hasInventory();
    }
}
