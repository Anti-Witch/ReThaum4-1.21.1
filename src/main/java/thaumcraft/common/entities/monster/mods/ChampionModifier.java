package thaumcraft.common.entities.monster.mods;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import thaumcraft.Thaumcraft;

import java.util.UUID;

/**
 * ChampionModifier - Defines different types of champion mob modifiers.
 * 
 * Champion mobs are elite versions of regular monsters that spawn rarely in the world.
 * Each modifier grants unique abilities, visual effects, and combat enhancements.
 * 
 * Modifier Types:
 * -1: Universal (applies to all damage types)
 *  0: Defensive (reduces incoming damage)
 *  1: Offensive (enhances outgoing damage)
 *  2: Physical (affects physical combat)
 * 
 * Ported to 1.20.1
 */
public class ChampionModifier {
    
    private final String name;
    public final int id;
    public final int type;
    public final IChampionModifierEffect effect;
    public final AttributeModifier attributeMod;
    
    // Static registry of all champion modifiers
    public static final ChampionModifier[] MODS;
    
    // Attribute modifiers for tracking
    public static final AttributeModifier ATTRIBUTE_MOD_NONE;
    public static final AttributeModifier ATTRIBUTE_MINUS_ONE;
    
    public ChampionModifier(int id, String name, int type, IChampionModifierEffect effect, UUID uuid) {
        this.name = name;
        this.id = id;
        this.type = type;
        this.effect = effect;
        this.attributeMod = new AttributeModifier(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, name), id + 2, AttributeModifier.Operation.ADD_VALUE);
    }
    
    /**
     * Get the localized name for this modifier.
     */
    public String getModNameLocalized() {
        return Component.translatable("champion.mod." + name).getString();
    }
    
    /**
     * Get the internal name of this modifier.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Get a modifier by its ID.
     */
    public static ChampionModifier getById(int id) {
        if (id >= 0 && id < MODS.length) {
            return MODS[id];
        }
        return null;
    }
    
    /**
     * Get a random modifier for champion spawning.
     */
    public static ChampionModifier getRandomModifier(java.util.Random random) {
        return MODS[random.nextInt(MODS.length)];
    }
    
    static {
        // Initialize all champion modifiers
        MODS = new ChampionModifier[] {
            new ChampionModifier(0, "bold", -1, new ChampionModBold(), 
                    UUID.fromString("40289aa1-907f-4ac6-ad79-e6681efe2cbc")),
            new ChampionModifier(1, "spine", 2, new ChampionModSpined(), 
                    UUID.fromString("365eead5-3f15-42a8-9e68-36100faef945")),
            new ChampionModifier(2, "armor", 2, new ChampionModArmored(), 
                    UUID.fromString("4e23758d-348e-42a8-8de6-08ae0a59033c")),
            new ChampionModifier(3, "mighty", -1, new ChampionModMighty(), 
                    UUID.fromString("6d2ffe79-f034-4a06-b288-e1916c21e385")),
            new ChampionModifier(4, "grim", 1, new ChampionModGrim(), 
                    UUID.fromString("0f23321e-f921-4246-90b8-21ef202de224")),
            new ChampionModifier(5, "warded", 0, new ChampionModWarded(), 
                    UUID.fromString("b622c4d8-abc6-4db7-b3ee-5cf71b8e5286")),
            new ChampionModifier(6, "warp", 1, new ChampionModWarp(), 
                    UUID.fromString("107da049-af7a-4409-989a-6de23c8fe036")),
            new ChampionModifier(7, "undying", 0, new ChampionModUndying(), 
                    UUID.fromString("cb9484d3-6255-4893-a4f2-3ecc375692ee")),
            new ChampionModifier(8, "fiery", 1, new ChampionModFire(), 
                    UUID.fromString("6b567fdf-9245-48f5-8314-f93fe5db1427")),
            new ChampionModifier(9, "sickly", 1, new ChampionModSickly(), 
                    UUID.fromString("b5718868-9ab0-424c-af1f-8b35e836b46e")),
            new ChampionModifier(10, "venomous", 1, new ChampionModPoison(), 
                    UUID.fromString("ab9a132e-c619-4c0a-a103-10cbbcfba1a2")),
            new ChampionModifier(11, "vampiric", 1, new ChampionModVampire(), 
                    UUID.fromString("3412251e-af81-4c3c-93ba-2e1c33b049ea")),
            new ChampionModifier(12, "infested", 2, new ChampionModInfested(), 
                    UUID.fromString("9c577fbe-ddbc-4ea2-a661-770ea775f43b")),
            new ChampionModifier(13, "tainted", 0, new ChampionModTainted(), 
                    UUID.fromString("a3bb2595-8221-4140-bc73-538abcd1bbd2"))
        };
        
        ATTRIBUTE_MOD_NONE = new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "champion_normal"),
                1.0, AttributeModifier.Operation.ADD_VALUE);
        ATTRIBUTE_MINUS_ONE = new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "champion_minus1"),
                -1.0, AttributeModifier.Operation.ADD_VALUE);
    }
}




