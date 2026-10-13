package thaumcraft.common.world.biomes;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * BiomeHandler - Manages Thaumcraft biome registration and aura modifiers.
 * 
 * In 1.20.1, biomes are defined via JSON data files in data/thaumcraft/worldgen/biome/
 * This class provides:
 * - Biome ResourceKey references for use in code
 * - Aura level modifiers based on biome tags
 * - Aspect associations for biomes
 * - Greatwood spawn chance per biome type
 * 
 * Ported from Thaumcraft 1.12.2 with 1.20.1 biome tag system.
 */
public class BiomeHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BiomeHandler.class);

    // ==================== Thaumcraft Biome Keys ====================
    
    /** Eerie biome - spooky magical biome with dark atmosphere */
    public static final ResourceKey<Biome> EERIE = ResourceKey.create(
        Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "eerie"));
    
    /** Magical Forest biome - peaceful magical biome with greatwood/silverwood trees */
    public static final ResourceKey<Biome> MAGICAL_FOREST = ResourceKey.create(
        Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "magical_forest"));
    
    /** Eldritch biome - otherworldly biome found in the Outer Lands */
    public static final ResourceKey<Biome> ELDRITCH = ResourceKey.create(
        Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "eldritch"));

    // ==================== Biome Tags ====================
    
    /** Tag for all Thaumcraft biomes */
    public static final TagKey<Biome> IS_THAUMCRAFT = TagKey.create(
        Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "is_thaumcraft"));
    
    /** Tag for magical biomes (high aura) */
    public static final TagKey<Biome> IS_MAGICAL = TagKey.create(
        Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "is_magical"));
    
    /** Tag for tainted biomes */
    public static final TagKey<Biome> IS_TAINTED = TagKey.create(
        Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "is_tainted"));

    // ==================== Biome Info Storage ====================
    
    /**
     * BiomeInfo holds aura modifier, associated aspect, and tree spawn data.
     */
    public record BiomeInfo(float auraModifier, Aspect aspect, boolean supportsGreatwood, float greatwoodChance) {}
    
    /** Map of biome tag to aura/aspect info */
    private static final Map<TagKey<Biome>, BiomeInfo> BIOME_TAG_INFO = new HashMap<>();
    
    /** Map of specific biome to aura/aspect info (overrides tag-based lookup) */
    private static final Map<ResourceKey<Biome>, BiomeInfo> BIOME_SPECIFIC_INFO = new HashMap<>();

    // ==================== Initialization ====================
    
    /**
     * Register biome info for vanilla biome tags.
     * Called during mod initialization.
     */
    public static void registerBiomeInfo() {
        LOGGER.info("Registering Thaumcraft biome info");
        
        // Water biomes - low aura, water aspect
        registerTagInfo(BiomeTags.IS_OCEAN, 0.33f, Aspect.WATER, false, 0.0f);
        registerTagInfo(BiomeTags.IS_RIVER, 0.4f, Aspect.WATER, false, 0.0f);
        registerTagInfo(BiomeTags.IS_BEACH, 0.3f, Aspect.EARTH, false, 0.0f);
        
        // Hot biomes - low aura, fire aspect
        registerTagInfo(BiomeTags.IS_NETHER, 0.125f, Aspect.FIRE, false, 0.0f);
        registerTagInfo(BiomeTags.IS_BADLANDS, 0.33f, Aspect.FIRE, false, 0.0f);
        
        // Cold biomes - low/medium aura, order aspect
        registerTagInfo(BiomeTags.IS_TAIGA, 0.33f, Aspect.EARTH, true, 0.2f);
        
        // Forest biomes - medium/high aura, earth aspect, greatwood support
        registerTagInfo(BiomeTags.IS_FOREST, 0.5f, Aspect.EARTH, true, 1.0f);
        registerTagInfo(BiomeTags.IS_JUNGLE, 0.6f, Aspect.EARTH, false, 0.0f);
        
        // Mountain biomes - medium aura, air aspect
        registerTagInfo(BiomeTags.IS_MOUNTAIN, 0.3f, Aspect.AIR, false, 0.0f);
        registerTagInfo(BiomeTags.IS_HILL, 0.33f, Aspect.AIR, false, 0.0f);
        
        // Savanna biomes - low aura, air aspect
        registerTagInfo(BiomeTags.IS_SAVANNA, 0.25f, Aspect.AIR, true, 0.2f);
        
        // End biomes - very low aura, air aspect
        registerTagInfo(BiomeTags.IS_END, 0.125f, Aspect.AIR, false, 0.0f);

        // Register Thaumcraft biome specific info
        registerBiomeInfo(MAGICAL_FOREST, 1.5f, Aspect.ORDER, true, 1.0f);
        registerBiomeInfo(EERIE, 0.75f, Aspect.ENTROPY, false, 0.0f);
        registerBiomeInfo(ELDRITCH, 0.25f, Aspect.ENTROPY, false, 0.0f);
        
        // Plains and generic biomes - medium aura
        registerBiomeInfo(Biomes.PLAINS, 0.3f, Aspect.AIR, true, 0.2f);
        registerBiomeInfo(Biomes.SUNFLOWER_PLAINS, 0.35f, Aspect.AIR, true, 0.2f);
        registerBiomeInfo(Biomes.MEADOW, 0.4f, Aspect.AIR, true, 0.3f);
        
        // Swamp biomes - medium aura, entropy aspect
        registerBiomeInfo(Biomes.SWAMP, 0.5f, Aspect.ENTROPY, true, 0.2f);
        registerBiomeInfo(Biomes.MANGROVE_SWAMP, 0.5f, Aspect.ENTROPY, false, 0.0f);
        
        // Mushroom biome - high aura
        registerBiomeInfo(Biomes.MUSHROOM_FIELDS, 0.75f, Aspect.ORDER, false, 0.0f);
        
        // Lush caves - high aura
        registerBiomeInfo(Biomes.LUSH_CAVES, 0.5f, Aspect.WATER, false, 0.0f);
    }

    /**
     * Register biome info for a specific biome tag.
     */
    public static void registerTagInfo(TagKey<Biome> tag, float auraModifier, Aspect aspect, 
            boolean supportsGreatwood, float greatwoodChance) {
        BIOME_TAG_INFO.put(tag, new BiomeInfo(auraModifier, aspect, supportsGreatwood, greatwoodChance));
    }

    /**
     * Register biome info for a specific biome.
     */
    public static void registerBiomeInfo(ResourceKey<Biome> biome, float auraModifier, Aspect aspect,
            boolean supportsGreatwood, float greatwoodChance) {
        BIOME_SPECIFIC_INFO.put(biome, new BiomeInfo(auraModifier, aspect, supportsGreatwood, greatwoodChance));
    }

    // ==================== Biome Queries ====================

    /**
     * Get the aura modifier for a biome.
     * Higher values mean more vis generation.
     * 
     * @param biome The biome holder
     * @return Aura modifier (0.0 - 2.0, default 0.5)
     */
    public static float getAuraModifier(Holder<Biome> biome) {
        // Check specific biome first
        Optional<ResourceKey<Biome>> key = biome.unwrapKey();
        if (key.isPresent()) {
            BiomeInfo specificInfo = BIOME_SPECIFIC_INFO.get(key.get());
            if (specificInfo != null) {
                return specificInfo.auraModifier();
            }
        }
        
        // Fall back to tag-based lookup
        float total = 0.0f;
        int count = 0;
        
        for (Map.Entry<TagKey<Biome>, BiomeInfo> entry : BIOME_TAG_INFO.entrySet()) {
            if (biome.is(entry.getKey())) {
                total += entry.getValue().auraModifier();
                count++;
            }
        }
        
        return count > 0 ? total / count : 0.5f;
    }

    /**
     * Get a random aspect associated with the biome.
     * 
     * @param biome The biome holder
     * @return An aspect, or null if none found
     */
    public static Aspect getBiomeAspect(Holder<Biome> biome) {
        // Check specific biome first
        Optional<ResourceKey<Biome>> key = biome.unwrapKey();
        if (key.isPresent()) {
            BiomeInfo specificInfo = BIOME_SPECIFIC_INFO.get(key.get());
            if (specificInfo != null) {
                return specificInfo.aspect();
            }
        }
        
        // Fall back to tag-based lookup (return first match)
        for (Map.Entry<TagKey<Biome>, BiomeInfo> entry : BIOME_TAG_INFO.entrySet()) {
            if (biome.is(entry.getKey())) {
                return entry.getValue().aspect();
            }
        }
        
        return null;
    }

    /**
     * Check if a biome supports greatwood tree generation.
     * 
     * @param biome The biome holder
     * @return Greatwood spawn chance (0.0 = none, 1.0 = always when conditions met)
     */
    public static float getGreatwoodChance(Holder<Biome> biome) {
        // Check specific biome first
        Optional<ResourceKey<Biome>> key = biome.unwrapKey();
        if (key.isPresent()) {
            BiomeInfo specificInfo = BIOME_SPECIFIC_INFO.get(key.get());
            if (specificInfo != null && specificInfo.supportsGreatwood()) {
                return specificInfo.greatwoodChance();
            }
        }
        
        // Fall back to tag-based lookup
        for (Map.Entry<TagKey<Biome>, BiomeInfo> entry : BIOME_TAG_INFO.entrySet()) {
            if (biome.is(entry.getKey()) && entry.getValue().supportsGreatwood()) {
                return entry.getValue().greatwoodChance();
            }
        }
        
        return 0.0f;
    }

    /**
     * Check if a biome is a Thaumcraft magical biome.
     */
    public static boolean isMagicalBiome(Holder<Biome> biome) {
        return biome.is(IS_MAGICAL) || biome.is(IS_THAUMCRAFT);
    }

    /**
     * Check if a biome is tainted.
     */
    public static boolean isTaintedBiome(Holder<Biome> biome) {
        return biome.is(IS_TAINTED);
    }
}




