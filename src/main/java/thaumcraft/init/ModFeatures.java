package thaumcraft.init;

import net.minecraft.core.registries.Registries;

import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import thaumcraft.Thaumcraft;
import thaumcraft.common.world.features.BigMagicTreeFeature;
import thaumcraft.common.world.features.CrystalClusterFeature;
import thaumcraft.common.world.features.GreatwoodTreeFeature;
import thaumcraft.common.world.features.NodeFeature;
import thaumcraft.common.world.features.ObsidianTotemFeature;
import thaumcraft.common.world.features.SilverwoodTreeFeature;
import thaumcraft.common.world.features.TaintWorldFeature;
import thaumcraft.common.world.features.ThaumcraftPlantFeature;
import thaumcraft.common.world.structures.AncientStoneCircleFeature;
import thaumcraft.common.world.structures.BarrowFeature;
import thaumcraft.common.world.structures.EldritchObeliskFeature;
import thaumcraft.common.world.structures.RuinedTowerFeature;

/**
 * Registry for all Thaumcraft world generation features.
 * 
 * In 1.20.1, world generation uses the Feature system:
 * - Feature: The actual generation logic
 * - ConfiguredFeature: A feature with its configuration (datapack JSON or code)
 * - PlacedFeature: A configured feature with placement rules (where it spawns)
 * 
 * Features are registered here, while ConfiguredFeatures and PlacedFeatures
 * are defined in datapacks under:
 * - data/thaumcraft/worldgen/configured_feature/
 * - data/thaumcraft/worldgen/placed_feature/
 */
public class ModFeatures {
    
    public static final DeferredRegister<Feature<?>> FEATURES = 
            DeferredRegister.create(Registries.FEATURE, Thaumcraft.MODID);
    
    // ==================== Node Features ====================

    /**
     * Aura Node - Natural aura node generation.
     * Generates nodes in caves and underground areas throughout the world.
     * Supports all 6 node types: NORMAL, UNSTABLE, PURE, HUNGRY, DARK, TAINTED
     * With modifiers: BRIGHT, PALE, FADING
     */
    public static final DeferredHolder<Feature<?>, NodeFeature> AURA_NODE =
            FEATURES.register("aura_node",
                    () -> new NodeFeature(NodeFeature.NodeFeatureConfiguration.CODEC));

    /**
     * Silverwood Node - Pure node generated inside silverwood trees.
     * Always PURE type, smaller vis capacity.
     */
    public static final DeferredHolder<Feature<?>, NodeFeature> SILVERWOOD_NODE =
            FEATURES.register("silverwood_node",
                    () -> new NodeFeature(NodeFeature.NodeFeatureConfiguration.CODEC));

    /**
     * Sinister Node - Dark node generated in totem circles and eldritch structures.
     * Always DARK type, spawns in eerie biomes.
     */
    public static final DeferredHolder<Feature<?>, NodeFeature> SINISTER_NODE =
            FEATURES.register("sinister_node",
                    () -> new NodeFeature(NodeFeature.NodeFeatureConfiguration.CODEC));

    /**
     * Obsidian Totem - TC4 world-gen structure:
     * Obsidian Tile base + 2-5 totem segments, one hosting a Charged
     * Obsidian Totem with an artificial DARK (sinister) node inside.
     */
    public static final DeferredHolder<Feature<?>, ObsidianTotemFeature> OBSIDIAN_TOTEM =
            FEATURES.register("obsidian_totem",
                    () -> new ObsidianTotemFeature(NoneFeatureConfiguration.CODEC));

    // ==================== Tree Features ====================
    
    /**
     * Greatwood tree - large magical tree with 2x2 trunk.
     * Spawns in forests, plains, and similar biomes.
     * Has a rare spider nest variant.
     */
    public static final DeferredHolder<Feature<?>,  GreatwoodTreeFeature> GREATWOOD_TREE = 
            FEATURES.register("greatwood_tree", 
                    () -> new GreatwoodTreeFeature(NoneFeatureConfiguration.CODEC));
    
    /**
     * Silverwood tree - magical pale tree with unique trunk shape.
     * Rarer than greatwood, spawns in magical biomes and forests.
     * Spawns shimmerleaf flowers around it.
     */
    public static final DeferredHolder<Feature<?>,  SilverwoodTreeFeature> SILVERWOOD_TREE = 
            FEATURES.register("silverwood_tree", 
                    () -> new SilverwoodTreeFeature(NoneFeatureConfiguration.CODEC));
    
    /**
     * Big Magic Tree - Large, majestic magical tree with sprawling branches.
     * This is the "fancy" tree variant for magical forest biomes.
     * Taller than regular greatwood/silverwood with more complex branch structure.
     */
    public static final DeferredHolder<Feature<?>,  BigMagicTreeFeature> BIG_MAGIC_TREE = 
            FEATURES.register("big_magic_tree", 
                    () -> new BigMagicTreeFeature(NoneFeatureConfiguration.CODEC));
    
    /**
     * Big Silverwood Tree - Large silverwood variant for magical biomes.
     * Uses silverwood logs and leaves instead of greatwood.
     */
    public static final DeferredHolder<Feature<?>,  BigMagicTreeFeature> BIG_SILVERWOOD_TREE = 
            FEATURES.register("big_silverwood_tree", 
                    () -> new BigMagicTreeFeature(NoneFeatureConfiguration.CODEC, 
                            BigMagicTreeFeature.TreeType.SILVERWOOD));
    
    // ==================== Plant Features ====================
    
    /**
     * Cinderpearl plant cluster - desert fire plants.
     * Spawns in desert biomes on sand.
     */
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> CINDERPEARL_PATCH = 
            FEATURES.register("cinderpearl_patch", 
                    () -> new ThaumcraftPlantFeature(NoneFeatureConfiguration.CODEC, 
                            ThaumcraftPlantFeature.PlantType.CINDERPEARL));
    
    /**
     * Shimmerleaf plant cluster - glowing magical flowers.
     * Primarily spawns around silverwood trees, but can appear in magical biomes.
     */
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> SHIMMERLEAF_PATCH = 
            FEATURES.register("shimmerleaf_patch", 
                    () -> new ThaumcraftPlantFeature(NoneFeatureConfiguration.CODEC, 
                            ThaumcraftPlantFeature.PlantType.SHIMMERLEAF));
    
    /**
     * Vishroom mushroom cluster - magical cave mushrooms.
     * Spawns underground in caves.
     */
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> VISHROOM_PATCH = 
            FEATURES.register("vishroom_patch", 
                    () -> new ThaumcraftPlantFeature(NoneFeatureConfiguration.CODEC, 
                            ThaumcraftPlantFeature.PlantType.VISHROOM));

    /**
     * Taint weed cluster - fibrous taint plants growing on tainted ground.
     * Spawns in tainted biomes.
     */
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> TAINT_WEED = 
            FEATURES.register("taint_weed", 
                    () -> new ThaumcraftPlantFeature(NoneFeatureConfiguration.CODEC, 
                            ThaumcraftPlantFeature.PlantType.TAINT_WEED));
    
    // ==================== Crystal Features ====================
    
    /**
     * Air crystal cluster - spawns on cave walls.
     * More common at high altitudes.
     */
    public static final DeferredHolder<Feature<?>,  CrystalClusterFeature> CRYSTAL_CLUSTER_AIR = 
            FEATURES.register("crystal_cluster_air", 
                    () -> new CrystalClusterFeature(NoneFeatureConfiguration.CODEC, 
                            CrystalClusterFeature.CrystalType.AIR));
    
    /**
     * Fire crystal cluster - spawns on cave walls.
     * More common near lava and in warm biomes.
     */
    public static final DeferredHolder<Feature<?>,  CrystalClusterFeature> CRYSTAL_CLUSTER_FIRE = 
            FEATURES.register("crystal_cluster_fire", 
                    () -> new CrystalClusterFeature(NoneFeatureConfiguration.CODEC, 
                            CrystalClusterFeature.CrystalType.FIRE));
    
    /**
     * Water crystal cluster - spawns on cave walls.
     * More common near water and in ocean caves.
     */
    public static final DeferredHolder<Feature<?>,  CrystalClusterFeature> CRYSTAL_CLUSTER_WATER = 
            FEATURES.register("crystal_cluster_water", 
                    () -> new CrystalClusterFeature(NoneFeatureConfiguration.CODEC, 
                            CrystalClusterFeature.CrystalType.WATER));
    
    /**
     * Earth crystal cluster - spawns on cave walls.
     * More common at low altitudes (deep caves).
     */
    public static final DeferredHolder<Feature<?>,  CrystalClusterFeature> CRYSTAL_CLUSTER_EARTH = 
            FEATURES.register("crystal_cluster_earth", 
                    () -> new CrystalClusterFeature(NoneFeatureConfiguration.CODEC, 
                            CrystalClusterFeature.CrystalType.EARTH));
    
    /**
     * Order crystal cluster - spawns on cave walls.
     * Rarest primal crystal type.
     */
    public static final DeferredHolder<Feature<?>,  CrystalClusterFeature> CRYSTAL_CLUSTER_ORDER = 
            FEATURES.register("crystal_cluster_order", 
                    () -> new CrystalClusterFeature(NoneFeatureConfiguration.CODEC, 
                            CrystalClusterFeature.CrystalType.ORDER));
    
    /**
     * Entropy crystal cluster - spawns on cave walls.
     * Rarest primal crystal type.
     */
    public static final DeferredHolder<Feature<?>,  CrystalClusterFeature> CRYSTAL_CLUSTER_ENTROPY = 
            FEATURES.register("crystal_cluster_entropy", 
                    () -> new CrystalClusterFeature(NoneFeatureConfiguration.CODEC, 
                            CrystalClusterFeature.CrystalType.ENTROPY));
    
    
    
    // ==================== Taint Features ====================
    
    /**
     * Taint crust - replaces surface tops with taint crust in the tainted biome.
     */
    public static final DeferredHolder<Feature<?>,  TaintWorldFeature> TAINT_CRUST = 
            FEATURES.register("taint_crust", 
                    () -> new TaintWorldFeature(NoneFeatureConfiguration.CODEC, 
                            TaintWorldFeature.TaintFeatureType.CRUST));
    
    /**
     * Taint geyser - taint geyser pillars rising from tainted soil.
     */
    public static final DeferredHolder<Feature<?>,  TaintWorldFeature> TAINT_GEYSER = 
            FEATURES.register("taint_geyser", 
                    () -> new TaintWorldFeature(NoneFeatureConfiguration.CODEC, 
                            TaintWorldFeature.TaintFeatureType.GEYSER));
    
    /**
     * Taint feature - taint tendrils attached to tainted surfaces.
     */
    public static final DeferredHolder<Feature<?>,  TaintWorldFeature> TAINT_FEATURE = 
            FEATURES.register("taint_feature", 
                    () -> new TaintWorldFeature(NoneFeatureConfiguration.CODEC, 
                            TaintWorldFeature.TaintFeatureType.TENDRILS));
    
    // ==================== Ore Features ====================
    // Note: Ore generation in 1.20.1 typically uses vanilla OreFeature
    // with custom OreConfiguration. For Thaumcraft ores (cinnabar, amber, etc.),
    // we can use datapack JSON configurations.
    
    // ==================== Structure Features ====================
    
    /**
     * Barrow mound - Ancient burial mound with loot and spawners.
     * Underground stone chamber with grass-covered mound entrance.
     * Contains chest, Thaumcraft loot crates/urns, and monster spawners.
     */
    public static final DeferredHolder<Feature<?>,  BarrowFeature> BARROW = 
            FEATURES.register("barrow", 
                    () -> new BarrowFeature(NoneFeatureConfiguration.CODEC));
    
    /**
     * Ancient Stone Circle - Mysterious stone monuments.
     * Can generate as:
     * - Small circle (4-6 standing stones)
     * - Large circle (8-12 stones with central altar)
     * - Single obelisk with glyphed stones
     */
    public static final DeferredHolder<Feature<?>,  AncientStoneCircleFeature> ANCIENT_STONE_CIRCLE = 
            FEATURES.register("ancient_stone_circle", 
                    () -> new AncientStoneCircleFeature(NoneFeatureConfiguration.CODEC));
    
    /**
     * Eldritch Obelisk - Tall dark stone monuments.
     * Features:
     * - Central eldritch stone pillar (10-15 blocks tall)
     * - Obsidian-lined base platform
     * - Ancient stone decorations
     * - Scattered debris around the perimeter
     * Hints at eldritch knowledge and may spawn eldritch mobs.
     */
    public static final DeferredHolder<Feature<?>,  EldritchObeliskFeature> ELDRITCH_OBELISK = 
            FEATURES.register("eldritch_obelisk", 
                    () -> new EldritchObeliskFeature(NoneFeatureConfiguration.CODEC));
    
    /**
     * Ruined Tower - Abandoned wizard towers.
     * Features:
     * - Circular stone tower (radius 3-4, height 8-14)
     * - Partial collapse on one side
     * - Multiple floors with wooden planks
     * - Bookshelves and loot crates/urns
     * - Vegetation growing through the ruins
     * Contains research materials and Thaumcraft loot.
     */
    public static final DeferredHolder<Feature<?>,  RuinedTowerFeature> RUINED_TOWER = 
            FEATURES.register("ruined_tower", 
                    () -> new RuinedTowerFeature(NoneFeatureConfiguration.CODEC));
}







