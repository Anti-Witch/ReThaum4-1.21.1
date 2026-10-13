package thaumcraft.init;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import thaumcraft.Thaumcraft;
import thaumcraft.common.world.structures.BarrowStructure;

/**
 * ModStructures - Registration for all Thaumcraft world generation structures.
 * 
 * In 1.20.1, structures use a multi-part registration system:
 * 
 * 1. StructureType (registered here) - Defines the codec for serialization
 * 2. Structure (defined in datapack JSON) - Configures the specific structure instance
 * 3. StructureSet (defined in datapack JSON) - Controls placement (spacing, separation, biomes)
 * 4. Template Pool (optional, for jigsaw structures) - Defines the structure pieces
 * 
 * Structure JSON files go in:
 * - data/thaumcraft/worldgen/structure/
 * - data/thaumcraft/worldgen/structure_set/
 * - data/thaumcraft/worldgen/template_pool/
 */
public class ModStructures {
    
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = 
            DeferredRegister.create(Registries.STRUCTURE_TYPE, Thaumcraft.MODID);
    
    // ==================== Structure Types ====================
    
    /**
     * Barrow structure type - Ancient burial mound with loot and spawners.
     * 
     * Barrows appear as grass-covered mounds in plains and forest biomes.
     * They contain:
     * - Stone/cobblestone interior
     * - Central chamber with chest (dungeon loot)
     * - Side chambers with skeleton/zombie spawners
     * - Thaumcraft loot crates and urns
     * - Iron bar entrance
     */
    public static final DeferredHolder<StructureType<?>,  StructureType<BarrowStructure>> BARROW = 
            STRUCTURE_TYPES.register("barrow", () -> explicitCodec(BarrowStructure.CODEC));

    /**
     * Ancient stone circle - jigsaw structure (start_pool + size), same codec
     * as the barrow. Registered 2026-08-11: the JSON (type thaumcraft:ancient_stone_circle)
     * existed without a matching StructureType, which would fail datapack load.
     */
    public static final DeferredHolder<StructureType<?>, StructureType<BarrowStructure>> ANCIENT_STONE_CIRCLE =
            STRUCTURE_TYPES.register("ancient_stone_circle", () -> explicitCodec(BarrowStructure.CODEC));

    /**
     * Eldritch obelisk - jigsaw structure, same codec as the barrow.
     */
    public static final DeferredHolder<StructureType<?>, StructureType<BarrowStructure>> ELDRITCH_OBELISK =
            STRUCTURE_TYPES.register("eldritch_obelisk", () -> explicitCodec(BarrowStructure.CODEC));

    /**
     * Ruined tower - jigsaw structure, same codec as the barrow.
     */
    public static final DeferredHolder<StructureType<?>, StructureType<BarrowStructure>> RUINED_TOWER =
            STRUCTURE_TYPES.register("ruined_tower", () -> explicitCodec(BarrowStructure.CODEC));

    // ==================== Helper Methods ====================
    
    /**
     * Helper to create a StructureType from a codec.
     * This explicit method helps with generic type inference.
     */
    private static <S extends Structure> StructureType<S> explicitCodec(MapCodec<S> codec) {
        return () -> codec;
    }
    
    // ==================== Future Structures ====================
    // 
    // TODO: Add more Thaumcraft structures:
    // - Eldritch Obelisk - Spawns in magical biomes, has special properties
    // - Cultist Portal - Crimson cult ritual sites
    // - Ancient Stone Circle - Generates in plains, magical significance
    // - Ruined Tower - Abandoned wizard tower with research notes
    // - Tainted Land Formation - For tainted biomes (when implemented)
}






