package thaumcraft.init;

import net.minecraft.core.registries.Registries;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import thaumcraft.Thaumcraft;
import thaumcraft.common.items.armor.ItemGoggles;
import thaumcraft.common.items.armor.ItemRobeArmor;
import thaumcraft.common.items.armor.ItemThaumiumArmor;
import thaumcraft.common.items.armor.ItemVoidArmor;
import thaumcraft.common.items.armor.ItemBootsTraveller;
import thaumcraft.common.items.armor.ItemFortressArmor;
import thaumcraft.common.items.armor.ItemVoidRobeArmor;
import thaumcraft.common.items.armor.ItemCultistRobeArmor;
import thaumcraft.common.items.armor.ItemCultistPlateArmor;
import thaumcraft.common.items.armor.ItemCultistBoots;
import thaumcraft.common.items.armor.ItemCultistLeaderArmor;
import thaumcraft.common.items.consumables.ItemPhial;
import thaumcraft.common.items.curios.ItemThaumonomicon;
import thaumcraft.common.items.curios.ItemFolioOmniscience;
import thaumcraft.common.items.resources.ItemCrystalEssence;
import thaumcraft.common.items.resources.ItemMaterial;
import thaumcraft.common.items.ItemResearchNotes;
import thaumcraft.common.items.resources.ItemMagicDust;
import thaumcraft.common.items.tools.ItemScribingTools;
import thaumcraft.common.items.tools.ItemThaumometer;
import thaumcraft.common.items.tools.ItemThaumiumSword;
import thaumcraft.common.items.tools.ItemThaumiumPickaxe;
import thaumcraft.common.items.tools.ItemThaumiumAxe;
import thaumcraft.common.items.tools.ItemThaumiumShovel;
import thaumcraft.common.items.tools.ItemThaumiumHoe;
import thaumcraft.common.items.tools.ItemVoidSword;
import thaumcraft.common.items.tools.ItemVoidPickaxe;
import thaumcraft.common.items.tools.ItemVoidAxe;
import thaumcraft.common.items.tools.ItemVoidShovel;
import thaumcraft.common.items.tools.ItemVoidHoe;
import thaumcraft.common.items.baubles.ItemAmuletVis;
import thaumcraft.common.items.baubles.ItemCloudRing;
import thaumcraft.common.items.baubles.ItemCuriosityBand;
import thaumcraft.common.items.tools.ItemElementalPickaxe;
import thaumcraft.common.items.tools.ItemElementalAxe;
import thaumcraft.common.items.tools.ItemElementalSword;
import thaumcraft.common.items.tools.ItemElementalShovel;
import thaumcraft.common.items.tools.ItemPrimalCrusher;
import thaumcraft.common.items.tools.ItemCrimsonBlade;
import thaumcraft.common.items.tools.ItemElementalHoe;
import thaumcraft.common.items.tools.ItemResonator;
import thaumcraft.common.items.tools.ItemSanityChecker;
import thaumcraft.common.items.tools.ItemHandMirror;
import thaumcraft.common.items.consumables.ItemBathSalts;
import thaumcraft.common.items.consumables.ItemSanitySoap;
import thaumcraft.common.items.consumables.ItemBottleTaint;
import thaumcraft.common.items.consumables.ItemLabel;
// TC6-only: import thaumcraft.common.items.curios.ItemCelestialNotes;
import thaumcraft.common.items.curios.ItemLootBag;
import thaumcraft.common.items.curios.ItemPechWand;
import thaumcraft.common.items.curios.ItemPrimordialPearl;

import thaumcraft.common.items.wands.foci.ItemFocusFire;
import thaumcraft.common.items.wands.foci.ItemFocusShock;
import thaumcraft.common.items.wands.foci.ItemFocusExcavation;
import thaumcraft.common.items.wands.foci.ItemFocusEqualTrade;
import thaumcraft.common.items.wands.foci.ItemFocusPortableHole;
import thaumcraft.common.items.wands.foci.ItemFocusFrost;
import thaumcraft.common.items.wands.foci.ItemFocusWarding;

import thaumcraft.common.items.wands.ItemFocusUpgrade;

import thaumcraft.common.golems.ItemGolemBell;
import thaumcraft.common.golems.ItemGolemPlacer;
import thaumcraft.common.items.misc.ItemCreativeFluxSponge;

import thaumcraft.common.items.baubles.ItemAmuletVis;
import thaumcraft.common.items.baubles.ItemGirdleHover;
import thaumcraft.common.items.baubles.ItemRingApprentice;
import thaumcraft.common.items.baubles.ItemRingFancy;
import thaumcraft.common.items.baubles.ItemBeltFancy;
import thaumcraft.common.items.baubles.ItemAmuletFancy;


import thaumcraft.common.golems.ItemGolemCore;
// import thaumcraft.common.golems.ItemGolemModule; // TC6-only, removed
import thaumcraft.common.items.essentia.ItemJarNode;

/**
 * Registry for all Thaumcraft items.
 * Uses DeferredRegister for 1.20.1 Forge.
 */
public class ModItems {

    public static final DeferredRegister<Item> ITEMS = 
            DeferredRegister.create(Registries.ITEM, Thaumcraft.MODID);

    // ==================== Tools ====================

    public static final DeferredHolder<Item, Item> THAUMONOMICON = ITEMS.register("thaumonomicon",
            ItemThaumonomicon::new);

    public static final DeferredHolder<Item, Item> CRIMSON_RITES = ITEMS.register("crimson_rites",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)));

    public static final DeferredHolder<Item, Item> THAUMOMETER = ITEMS.register("thaumometer",
            ItemThaumometer::new);

    public static final DeferredHolder<Item, Item> SCRIBING_TOOLS = ITEMS.register("scribing_tools",
            ItemScribingTools::new);

    // ==================== Phials ====================

    public static final DeferredHolder<Item, Item> PHIAL_EMPTY = ITEMS.register("phial_empty",
            ItemPhial::createEmpty);

    public static final DeferredHolder<Item, Item> PHIAL_FILLED = ITEMS.register("phial_filled",
            ItemPhial::createFilled);

    // ==================== Basic Materials ====================

    public static final DeferredHolder<Item, Item> QUICKSILVER = ITEMS.register("quicksilver",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> ALUMENTUM = ITEMS.register("alumentum",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> NITOR = ITEMS.register("nitor",
            thaumcraft.common.items.ItemNitor::new);

    public static final DeferredHolder<Item, Item> SALIS_MUNDUS = ITEMS.register("salis_mundus",
            ItemMagicDust::new);

    // TC6-only: balanced_shard — removed (not in TC4)
    // public static final DeferredHolder<Item, Item> BALANCED_SHARD = ...

    // ==================== Metal Ingots & Nuggets ====================

    public static final DeferredHolder<Item, Item> THAUMIUM_INGOT = ITEMS.register("thaumium_ingot",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> THAUMIUM_NUGGET = ITEMS.register("thaumium_nugget",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> VOID_METAL_INGOT = ITEMS.register("void_metal_ingot",
            () -> new ItemMaterial(new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredHolder<Item, Item> VOID_METAL_NUGGET = ITEMS.register("void_metal_nugget",
            () -> new ItemMaterial(new Item.Properties().rarity(Rarity.RARE)));

    // TC6-only: brass_ingot, brass_nugget, quartz_nugget — removed
    // quicksilver_nugget stays (TC4 had it in itemResource)
    // public static final DeferredHolder<Item, Item> BRASS_INGOT = ...
    // public static final DeferredHolder<Item, Item> BRASS_NUGGET = ...
    public static final DeferredHolder<Item, Item> QUICKSILVER_NUGGET = ITEMS.register("quicksilver_nugget",
            ItemMaterial::basic);
    // public static final DeferredHolder<Item, Item> QUARTZ_NUGGET = ...

    // ==================== Primal Shards ====================

    public static final DeferredHolder<Item, Item> SHARD_AIR = ITEMS.register("shard_air",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> SHARD_FIRE = ITEMS.register("shard_fire",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> SHARD_WATER = ITEMS.register("shard_water",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> SHARD_EARTH = ITEMS.register("shard_earth",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> SHARD_ORDER = ITEMS.register("shard_order",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> SHARD_ENTROPY = ITEMS.register("shard_entropy",
            ItemMaterial::basic);

    /** TC4 shard meta 6 - Balanced Shard (used by wand rod infusions and the primal charm). */
    public static final DeferredHolder<Item, Item> SHARD_BALANCED = ITEMS.register("shard_balanced",
            ItemMaterial::basic);

    // ==================== Crafting Components ====================

    public static final DeferredHolder<Item, Item> AMBER = ITEMS.register("amber",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> AMBER_BEAD = ITEMS.register("amber_bead",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> ENCHANTED_FABRIC = ITEMS.register("enchanted_fabric",
            ItemMaterial::uncommon);

    // Mirrored Glass / resonators - present in TC4 (mirrored_glass, morphic_resonator, vis_resonator models exist in TC4 assets)
    public static final DeferredHolder<Item, Item> MIRRORED_GLASS = ITEMS.register("mirrored_glass",
            ItemMaterial::uncommon);

    public static final DeferredHolder<Item, Item> FILTER = ITEMS.register("filter",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> MORPHIC_RESONATOR = ITEMS.register("morphic_resonator",
            ItemMaterial::uncommon);

    public static final DeferredHolder<Item, Item> VIS_RESONATOR = ITEMS.register("vis_resonator",
            ItemMaterial::uncommon);

    public static final DeferredHolder<Item, Item> TALLOW = ITEMS.register("tallow",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> JAR_BRACE = ITEMS.register("jar_brace",
            ItemMaterial::basic);

    // Mechanisms - present in TC4 (mechanism_simple/complex models exist in TC4 assets)
    public static final DeferredHolder<Item, Item> MECHANISM_SIMPLE = ITEMS.register("mechanism_simple",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> MECHANISM_COMPLEX = ITEMS.register("mechanism_complex",
            ItemMaterial::uncommon);

    // Plates - required by smelter/condenser/mechanism recipes (tc plates tag + 29 recipes)
    public static final DeferredHolder<Item, Item> PLATE_IRON = ITEMS.register("plate_iron",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> PLATE_BRASS = ITEMS.register("plate_brass",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> PLATE_THAUMIUM = ITEMS.register("plate_thaumium",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> PLATE_VOID = ITEMS.register("plate_void",
            ItemMaterial::rare);

    // Brass ingot/nugget + quartz nugget - TC4 materials (nuggetBrass, ingotBrass, nuggetQuartz in TC4 ConfigRecipes)
    public static final DeferredHolder<Item, Item> BRASS_INGOT = ITEMS.register("brass_ingot",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> BRASS_NUGGET = ITEMS.register("brass_nugget",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> QUARTZ_NUGGET = ITEMS.register("quartz_nugget",
            ItemMaterial::basic);

    // TC6-only: flux_crystal — removed (TC4 taint creatures drop taint_slime /
    // taint_tendril; see ModItems.TAINT_SLIME / TAINT_TENDRIL below)
    // public static final DeferredHolder<Item, Item> FLUX_CRYSTAL = ITEMS.register("flux_crystal",
    //         ItemMaterial::uncommon);

    // Rare earth nugget - TC4 (rareearth, arcane bore ingredient)
    public static final DeferredHolder<Item, Item> NUGGET_RAREEARTH = ITEMS.register("nugget_rareearth",
            ItemMaterial::uncommon);

    // ==================== Clusters (Raw Ores) ====================

    public static final DeferredHolder<Item, Item> CLUSTER_IRON = ITEMS.register("cluster_iron",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> CLUSTER_GOLD = ITEMS.register("cluster_gold",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> CLUSTER_COPPER = ITEMS.register("cluster_copper",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> CLUSTER_CINNABAR = ITEMS.register("cluster_cinnabar",
            ItemMaterial::basic);

    // TC6-only: brain items — removed (TC4 used golem_core with metadata)
    // 3 items: brain_normal, brain_clockwork, brain_curious

    // ==================== Research Notes ====================

    public static final DeferredHolder<Item, Item> RESEARCH_NOTES = ITEMS.register("research_notes",
            ItemResearchNotes::new);

    // ==================== Curiosities ====================

    // TC6-only: curiosity — removed (TC4 had curiosity_band, not this)
    // public static final DeferredHolder<Item, Item> CURIOSITY = ...

    public static final DeferredHolder<Item, Item> PRIMORDIAL_PEARL = ITEMS.register("primordial_pearl",
            ItemPrimordialPearl::new);

    // TC4 ItemResource damage 11/12 (dropped by every taint creature). These
    // were wrongly removed as "TC6-only" - they exist in OpenTC4 and their
    // textures were already present.
    public static final DeferredHolder<Item, Item> TAINT_SLIME = ITEMS.register("taint_slime",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> TAINT_TENDRIL = ITEMS.register("taint_tendril",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> ZOMBIE_BRAIN = ITEMS.register("zombie_brain",
            ItemMaterial::basic);

    // TC6-only: flux_crystal — removed
    // public static final DeferredHolder<Item, Item> FLUX_CRYSTAL = ...

    public static final DeferredHolder<Item, Item> VOID_SEED = ITEMS.register("void_seed",
            () -> new ItemMaterial(new Item.Properties().rarity(Rarity.RARE)));

    // ==================== Food ====================

    // Note: Foods need FoodProperties - simplified for now
    public static final DeferredHolder<Item, Item> TRIPLE_MEAT_TREAT = ITEMS.register("triple_meat_treat",
            () -> new Item(new Item.Properties()
                    .food(new net.minecraft.world.food.FoodProperties.Builder()
                            .nutrition(8)
                            .saturationModifier(1.0f)
                            .build())));

    public static final DeferredHolder<Item, Item> CHUNKS_BEEF = ITEMS.register("chunks_beef",
            () -> new Item(new Item.Properties()
                    .food(new net.minecraft.world.food.FoodProperties.Builder()
                            .nutrition(4)
                            .saturationModifier(0.5f)
                            .build())));

    public static final DeferredHolder<Item, Item> CHUNKS_CHICKEN = ITEMS.register("chunks_chicken",
            () -> new Item(new Item.Properties()
                    .food(new net.minecraft.world.food.FoodProperties.Builder()
                            .nutrition(4)
                            .saturationModifier(0.5f)
                            .build())));

    public static final DeferredHolder<Item, Item> CHUNKS_PORK = ITEMS.register("chunks_pork",
            () -> new Item(new Item.Properties()
                    .food(new net.minecraft.world.food.FoodProperties.Builder()
                            .nutrition(4)
                            .saturationModifier(0.5f)
                            .build())));

    public static final DeferredHolder<Item, Item> CHUNKS_FISH = ITEMS.register("chunks_fish",
            () -> new Item(new Item.Properties()
                    .food(new net.minecraft.world.food.FoodProperties.Builder()
                            .nutrition(4)
                            .saturationModifier(0.4f)
                            .build())));

    // ==================== Armor - Goggles ====================

    public static final DeferredHolder<Item, Item> GOGGLES = ITEMS.register("goggles",
            ItemGoggles::new);

    // Thaumaturge's robes - present in TC4 (cloth_chest/legs/boots models exist in TC4 assets)
    public static final DeferredHolder<Item, Item> CLOTH_CHEST = ITEMS.register("cloth_chest",
            ItemRobeArmor::createChest);

    public static final DeferredHolder<Item, Item> CLOTH_LEGS = ITEMS.register("cloth_legs",
            ItemRobeArmor::createLegs);

    public static final DeferredHolder<Item, Item> CLOTH_BOOTS = ITEMS.register("cloth_boots",
            ItemRobeArmor::createBoots);

    // ==================== Tools - Thaumium ====================

    public static final DeferredHolder<Item, Item> THAUMIUM_SWORD = ITEMS.register("thaumium_sword",
            ItemThaumiumSword::new);

    public static final DeferredHolder<Item, Item> THAUMIUM_PICK = ITEMS.register("thaumium_pick",
            ItemThaumiumPickaxe::new);

    public static final DeferredHolder<Item, Item> THAUMIUM_AXE = ITEMS.register("thaumium_axe",
            ItemThaumiumAxe::new);

    public static final DeferredHolder<Item, Item> THAUMIUM_SHOVEL = ITEMS.register("thaumium_shovel",
            ItemThaumiumShovel::new);

    public static final DeferredHolder<Item, Item> THAUMIUM_HOE = ITEMS.register("thaumium_hoe",
            ItemThaumiumHoe::new);

    // ==================== Tools - Void Metal ====================

    public static final DeferredHolder<Item, Item> VOID_SWORD = ITEMS.register("void_sword",
            ItemVoidSword::new);

    public static final DeferredHolder<Item, Item> VOID_PICK = ITEMS.register("void_pick",
            ItemVoidPickaxe::new);

    public static final DeferredHolder<Item, Item> VOID_AXE = ITEMS.register("void_axe",
            ItemVoidAxe::new);

    public static final DeferredHolder<Item, Item> VOID_SHOVEL = ITEMS.register("void_shovel",
            ItemVoidShovel::new);

    public static final DeferredHolder<Item, Item> VOID_HOE = ITEMS.register("void_hoe",
            ItemVoidHoe::new);

    // ==================== Armor - Thaumium ====================

    public static final DeferredHolder<Item, Item> THAUMIUM_HELM = ITEMS.register("thaumium_helm",
            ItemThaumiumArmor::createHelmet);

    public static final DeferredHolder<Item, Item> THAUMIUM_CHEST = ITEMS.register("thaumium_chest",
            ItemThaumiumArmor::createChestplate);

    public static final DeferredHolder<Item, Item> THAUMIUM_LEGS = ITEMS.register("thaumium_legs",
            ItemThaumiumArmor::createLeggings);

    public static final DeferredHolder<Item, Item> THAUMIUM_BOOTS = ITEMS.register("thaumium_boots",
            ItemThaumiumArmor::createBoots);

    // ==================== Armor - Void Metal ====================

    public static final DeferredHolder<Item, Item> VOID_HELM = ITEMS.register("void_helm",
            ItemVoidArmor::createHelmet);

    public static final DeferredHolder<Item, Item> VOID_CHEST = ITEMS.register("void_chest",
            ItemVoidArmor::createChestplate);

    public static final DeferredHolder<Item, Item> VOID_LEGS = ITEMS.register("void_legs",
            ItemVoidArmor::createLeggings);

    public static final DeferredHolder<Item, Item> VOID_BOOTS = ITEMS.register("void_boots",
            ItemVoidArmor::createBoots);

    // ==================== Special Armor ====================

    public static final DeferredHolder<Item, Item> TRAVELLER_BOOTS = ITEMS.register("traveller_boots",
            ItemBootsTraveller::new);

    // ==================== Fortress Armor ====================

    public static final DeferredHolder<Item, Item> FORTRESS_HELM = ITEMS.register("fortress_helm",
            ItemFortressArmor::createHelmet);

    public static final DeferredHolder<Item, Item> FORTRESS_CHEST = ITEMS.register("fortress_chest",
            ItemFortressArmor::createChestplate);

    public static final DeferredHolder<Item, Item> FORTRESS_LEGS = ITEMS.register("fortress_legs",
            ItemFortressArmor::createLeggings);

    // ==================== Void Robe Armor ====================

    public static final DeferredHolder<Item, Item> VOID_ROBE_HELM = ITEMS.register("void_robe_helm",
            ItemVoidRobeArmor::createHelmet);

    public static final DeferredHolder<Item, Item> VOID_ROBE_CHEST = ITEMS.register("void_robe_chest",
            ItemVoidRobeArmor::createChestplate);

    public static final DeferredHolder<Item, Item> VOID_ROBE_LEGS = ITEMS.register("void_robe_legs",
            ItemVoidRobeArmor::createLeggings);

    // ==================== Crimson Cult Robe Armor ====================

    public static final DeferredHolder<Item, Item> CRIMSON_ROBE_HELM = ITEMS.register("crimson_robe_helm",
            ItemCultistRobeArmor::createHelmet);

    public static final DeferredHolder<Item, Item> CRIMSON_ROBE_CHEST = ITEMS.register("crimson_robe_chest",
            ItemCultistRobeArmor::createChestplate);

    public static final DeferredHolder<Item, Item> CRIMSON_ROBE_LEGS = ITEMS.register("crimson_robe_legs",
            ItemCultistRobeArmor::createLeggings);

    // ==================== Crimson Cult Plate Armor ====================

    public static final DeferredHolder<Item, Item> CRIMSON_PLATE_HELM = ITEMS.register("crimson_plate_helm",
            ItemCultistPlateArmor::createHelmet);

    public static final DeferredHolder<Item, Item> CRIMSON_PLATE_CHEST = ITEMS.register("crimson_plate_chest",
            ItemCultistPlateArmor::createChestplate);

    public static final DeferredHolder<Item, Item> CRIMSON_PLATE_LEGS = ITEMS.register("crimson_plate_legs",
            ItemCultistPlateArmor::createLeggings);

    // ==================== Crimson Cult Boots (shared) ====================

    public static final DeferredHolder<Item, Item> CRIMSON_BOOTS = ITEMS.register("crimson_boots",
            ItemCultistBoots::new);

    // ==================== Crimson Praetor Armor (Cultist Leader) ====================

    public static final DeferredHolder<Item, Item> CRIMSON_PRAETOR_HELM = ITEMS.register("crimson_praetor_helm",
            ItemCultistLeaderArmor::createHelmet);

    public static final DeferredHolder<Item, Item> CRIMSON_PRAETOR_CHEST = ITEMS.register("crimson_praetor_chest",
            ItemCultistLeaderArmor::createChestplate);

    public static final DeferredHolder<Item, Item> CRIMSON_PRAETOR_LEGS = ITEMS.register("crimson_praetor_legs",
            ItemCultistLeaderArmor::createLeggings);

    // ==================== Baubles / Curios ====================

    public static final DeferredHolder<Item, Item> AMULET_VIS_FOUND = ITEMS.register("amulet_vis_found",
            ItemAmuletVis::createFound);

    public static final DeferredHolder<Item, Item> AMULET_VIS_CRAFTED = ITEMS.register("amulet_vis_crafted",
            ItemAmuletVis::createCrafted);

    public static final DeferredHolder<Item, Item> CLOUD_RING = ITEMS.register("cloud_ring",
            ItemCloudRing::new);

    public static final DeferredHolder<Item, Item> CURIOSITY_BAND = ITEMS.register("curiosity_band",
            ItemCuriosityBand::new);

    // TC6-only: charm_undying / verdant_charm* / voidseer_charm - removed
    // (TC4.2.3.5 has no charms; its baubles are amulet/ring/girdle runic + vis amulet)

    // ==================== Elemental Tools ====================

    public static final DeferredHolder<Item, Item> ELEMENTAL_PICK = ITEMS.register("elemental_pick",
            ItemElementalPickaxe::new);

    public static final DeferredHolder<Item, Item> ELEMENTAL_AXE = ITEMS.register("elemental_axe",
            ItemElementalAxe::new);

    public static final DeferredHolder<Item, Item> ELEMENTAL_SWORD = ITEMS.register("elemental_sword",
            ItemElementalSword::new);

    public static final DeferredHolder<Item, Item> ELEMENTAL_SHOVEL = ITEMS.register("elemental_shovel",
            ItemElementalShovel::new);

    // ==================== Special Tools ====================

    public static final DeferredHolder<Item, Item> PRIMAL_CRUSHER = ITEMS.register("primal_crusher",
            ItemPrimalCrusher::new);

    public static final DeferredHolder<Item, Item> CRIMSON_BLADE = ITEMS.register("crimson_blade",
            ItemCrimsonBlade::new);

    public static final DeferredHolder<Item, Item> ELEMENTAL_HOE = ITEMS.register("elemental_hoe",
            ItemElementalHoe::new);

    // ==================== Utility Tools ====================

    public static final DeferredHolder<Item, Item> RESONATOR = ITEMS.register("resonator",
            ItemResonator::new);

    public static final DeferredHolder<Item, Item> SANITY_CHECKER = ITEMS.register("sanity_checker",
            ItemSanityChecker::new);

    public static final DeferredHolder<Item, Item> HAND_MIRROR = ITEMS.register("hand_mirror",
            ItemHandMirror::new);

    // ==================== Consumables ====================

    public static final DeferredHolder<Item, Item> BATH_SALTS = ITEMS.register("bath_salts",
            ItemBathSalts::new);

    public static final DeferredHolder<Item, Item> SANITY_SOAP = ITEMS.register("sanity_soap",
            ItemSanitySoap::new);

    public static final DeferredHolder<Item, Item> BOTTLE_TAINT = ITEMS.register("bottle_taint",
            ItemBottleTaint::new);

    public static final DeferredHolder<Item, Item> LABEL_BLANK = ITEMS.register("label_blank",
            ItemLabel::createBlank);

    public static final DeferredHolder<Item, Item> LABEL_FILLED = ITEMS.register("label_filled",
            ItemLabel::createFilled);

    // ==================== Loot Bags ====================

    public static final DeferredHolder<Item, Item> LOOT_BAG_COMMON = ITEMS.register("loot_bag_common",
            ItemLootBag::createCommon);

    public static final DeferredHolder<Item, Item> LOOT_BAG_UNCOMMON = ITEMS.register("loot_bag_uncommon",
            ItemLootBag::createUncommon);

    public static final DeferredHolder<Item, Item> LOOT_BAG_RARE = ITEMS.register("loot_bag_rare",
            ItemLootBag::createRare);

    // ==================== Curios ====================

    public static final DeferredHolder<Item, Item> PECH_WAND = ITEMS.register("pech_wand",
            ItemPechWand::new);

    // TC6-only: Celestial Notes (removed — not in TC4)
    // public static final DeferredHolder<Item, Item> CELESTIAL_NOTES_SUN = ITEMS.register("celestial_notes_sun", ItemCelestialNotes::createSun);
    // public static final DeferredHolder<Item, Item> CELESTIAL_NOTES_STARS_1 = ITEMS.register("celestial_notes_stars_1", ItemCelestialNotes::createStars1);
    // public static final DeferredHolder<Item, Item> CELESTIAL_NOTES_STARS_2 = ITEMS.register("celestial_notes_stars_2", ItemCelestialNotes::createStars2);
    // public static final DeferredHolder<Item, Item> CELESTIAL_NOTES_STARS_3 = ITEMS.register("celestial_notes_stars_3", ItemCelestialNotes::createStars3);
    // public static final DeferredHolder<Item, Item> CELESTIAL_NOTES_STARS_4 = ITEMS.register("celestial_notes_stars_4", ItemCelestialNotes::createStars4);
    // public static final DeferredHolder<Item, Item> CELESTIAL_NOTES_MOON_1 = ITEMS.register("celestial_notes_moon_1", ItemCelestialNotes::createMoon1);
    // public static final DeferredHolder<Item, Item> CELESTIAL_NOTES_MOON_2 = ITEMS.register("celestial_notes_moon_2", ItemCelestialNotes::createMoon2);
    // public static final DeferredHolder<Item, Item> CELESTIAL_NOTES_MOON_3 = ITEMS.register("celestial_notes_moon_3", ItemCelestialNotes::createMoon3);
    // public static final DeferredHolder<Item, Item> CELESTIAL_NOTES_MOON_4 = ITEMS.register("celestial_notes_moon_4", ItemCelestialNotes::createMoon4);
    // public static final DeferredHolder<Item, Item> CELESTIAL_NOTES_MOON_5 = ITEMS.register("celestial_notes_moon_5", ItemCelestialNotes::createMoon5);
    // public static final DeferredHolder<Item, Item> CELESTIAL_NOTES_MOON_6 = ITEMS.register("celestial_notes_moon_6", ItemCelestialNotes::createMoon6);
    // public static final DeferredHolder<Item, Item> CELESTIAL_NOTES_MOON_7 = ITEMS.register("celestial_notes_moon_7", ItemCelestialNotes::createMoon7);
    // public static final DeferredHolder<Item, Item> CELESTIAL_NOTES_MOON_8 = ITEMS.register("celestial_notes_moon_8", ItemCelestialNotes::createMoon8);

    // Focus Pouch (focus storage) - restored 2026-08-09 (class + menu + screen + CasterManager all active)
    public static final DeferredHolder<Item, Item> FOCUS_POUCH = ITEMS.register("focus_pouch",
            thaumcraft.common.items.casters.ItemFocusPouch::new);

    // Blank/advanced focus - TC4 focus items (focus1/focus2 registry names, focus_1/focus_2 textures)
    public static final DeferredHolder<Item, Item> FOCUS_BLANK = ITEMS.register("focus_blank",
            thaumcraft.common.items.casters.ItemFocus::createBlank);

    public static final DeferredHolder<Item, Item> FOCUS_ADVANCED = ITEMS.register("focus_advanced",
            thaumcraft.common.items.casters.ItemFocus::createAdvanced);

    // TC6-only: focus_blank, focus_advanced — removed (not in TC4)
    // public static final DeferredHolder<Item, Item> FOCUS_BLANK = ...
    // public static final DeferredHolder<Item, Item> FOCUS_ADVANCED = ...

    // ==================== Wand System (TC4) ====================

    // Wand Rods
    public static final DeferredHolder<Item, Item> WAND_ROD_GREATWOOD = ITEMS.register("wand_rod_greatwood",
            () -> new thaumcraft.common.items.wands.WandRodItem(new Item.Properties()));

    public static final DeferredHolder<Item, Item> WAND_ROD_SILVERWOOD = ITEMS.register("wand_rod_silverwood",
            () -> new thaumcraft.common.items.wands.WandRodItem(new Item.Properties()));

    public static final DeferredHolder<Item, Item> WAND_ROD_OBSIDIAN = ITEMS.register("wand_rod_obsidian",
            () -> new thaumcraft.common.items.wands.WandRodItem(new Item.Properties()));

    public static final DeferredHolder<Item, Item> WAND_ROD_ICE = ITEMS.register("wand_rod_ice",
            () -> new thaumcraft.common.items.wands.WandRodItem(new Item.Properties()));

    public static final DeferredHolder<Item, Item> WAND_ROD_QUARTZ = ITEMS.register("wand_rod_quartz",
            () -> new thaumcraft.common.items.wands.WandRodItem(new Item.Properties()));

    public static final DeferredHolder<Item, Item> WAND_ROD_REED = ITEMS.register("wand_rod_reed",
            () -> new thaumcraft.common.items.wands.WandRodItem(new Item.Properties()));

    public static final DeferredHolder<Item, Item> WAND_ROD_BLAZE = ITEMS.register("wand_rod_blaze",
            () -> new thaumcraft.common.items.wands.WandRodItem(new Item.Properties()));

    public static final DeferredHolder<Item, Item> WAND_ROD_BONE = ITEMS.register("wand_rod_bone",
            () -> new thaumcraft.common.items.wands.WandRodItem(new Item.Properties()));

    // Staff rods (longer range)
    public static final DeferredHolder<Item, Item> WAND_ROD_GREATWOOD_STAFF = ITEMS.register("wand_rod_greatwood_staff",
            () -> new thaumcraft.common.items.wands.WandRodItem(new Item.Properties()));

    public static final DeferredHolder<Item, Item> WAND_ROD_SILVERWOOD_STAFF = ITEMS.register("wand_rod_silverwood_staff",
            () -> new thaumcraft.common.items.wands.WandRodItem(new Item.Properties()));

    public static final DeferredHolder<Item, Item> WAND_ROD_OBSIDIAN_STAFF = ITEMS.register("wand_rod_obsidian_staff",
            () -> new thaumcraft.common.items.wands.WandRodItem(new Item.Properties()));

    // Remaining TC4 staff rods (OpenTC4 ConfigItems STAFF_ROD_*)
    public static final DeferredHolder<Item, Item> WAND_ROD_ICE_STAFF = ITEMS.register("wand_rod_ice_staff",
            () -> new thaumcraft.common.items.wands.WandRodItem(new Item.Properties()));

    public static final DeferredHolder<Item, Item> WAND_ROD_QUARTZ_STAFF = ITEMS.register("wand_rod_quartz_staff",
            () -> new thaumcraft.common.items.wands.WandRodItem(new Item.Properties()));

    public static final DeferredHolder<Item, Item> WAND_ROD_REED_STAFF = ITEMS.register("wand_rod_reed_staff",
            () -> new thaumcraft.common.items.wands.WandRodItem(new Item.Properties()));

    public static final DeferredHolder<Item, Item> WAND_ROD_BLAZE_STAFF = ITEMS.register("wand_rod_blaze_staff",
            () -> new thaumcraft.common.items.wands.WandRodItem(new Item.Properties()));

    public static final DeferredHolder<Item, Item> WAND_ROD_BONE_STAFF = ITEMS.register("wand_rod_bone_staff",
            () -> new thaumcraft.common.items.wands.WandRodItem(new Item.Properties()));

    public static final DeferredHolder<Item, Item> WAND_ROD_PRIMAL_STAFF = ITEMS.register("wand_rod_primal_staff",
            () -> new thaumcraft.common.items.wands.WandRodItem(new Item.Properties()));

    // Wand Caps
    public static final DeferredHolder<Item, Item> WAND_CAP_IRON = ITEMS.register("wand_cap_iron",
            () -> new thaumcraft.common.items.wands.WandCapItem(new Item.Properties()));

    public static final DeferredHolder<Item, Item> WAND_CAP_GOLD = ITEMS.register("wand_cap_gold",
            () -> new thaumcraft.common.items.wands.WandCapItem(new Item.Properties()));

    public static final DeferredHolder<Item, Item> WAND_CAP_THAUMIUM = ITEMS.register("wand_cap_thaumium",
            () -> new thaumcraft.common.items.wands.WandCapItem(new Item.Properties()));

    public static final DeferredHolder<Item, Item> WAND_CAP_COPPER = ITEMS.register("wand_cap_copper",
            () -> new thaumcraft.common.items.wands.WandCapItem(new Item.Properties()));

    public static final DeferredHolder<Item, Item> WAND_CAP_SILVER = ITEMS.register("wand_cap_silver",
            () -> new thaumcraft.common.items.wands.WandCapItem(new Item.Properties()));

    public static final DeferredHolder<Item, Item> WAND_CAP_SILVER_INERT = ITEMS.register("wand_cap_silver_inert",
            () -> new thaumcraft.common.items.wands.WandCapItem(new Item.Properties()));

    public static final DeferredHolder<Item, Item> WAND_CAP_THAUMIUM_INERT = ITEMS.register("wand_cap_thaumium_inert",
            () -> new thaumcraft.common.items.wands.WandCapItem(new Item.Properties()));

    public static final DeferredHolder<Item, Item> WAND_CAP_VOID = ITEMS.register("wand_cap_void",
            () -> new thaumcraft.common.items.wands.WandCapItem(new Item.Properties()));

    public static final DeferredHolder<Item, Item> WAND_CAP_VOID_INERT = ITEMS.register("wand_cap_void_inert",
            () -> new thaumcraft.common.items.wands.WandCapItem(new Item.Properties()));

    // Main Wand Item (rod + cap combination)
    public static final DeferredHolder<Item, Item> WAND = ITEMS.register("wand",
            () -> new thaumcraft.common.items.wands.WandItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    // ==================== Focus Items ====================

    public static final DeferredHolder<Item, Item> FOCUS_FIRE = ITEMS.register("focus_fire",
            ItemFocusFire::new);

    public static final DeferredHolder<Item, Item> FOCUS_SHOCK = ITEMS.register("focus_shock",
            ItemFocusShock::new);

    public static final DeferredHolder<Item, Item> FOCUS_EXCAVATION = ITEMS.register("focus_excavation",
            ItemFocusExcavation::new);

    public static final DeferredHolder<Item, Item> FOCUS_EQUAL_TRADE = ITEMS.register("focus_equal_trade",
            ItemFocusEqualTrade::new);

    public static final DeferredHolder<Item, Item> FOCUS_PORTABLE_HOLE = ITEMS.register("focus_portable_hole",
            ItemFocusPortableHole::new);

    public static final DeferredHolder<Item, Item> FOCUS_FROST = ITEMS.register("focus_frost",
            ItemFocusFrost::new);

    public static final DeferredHolder<Item, Item> FOCUS_WARDING = ITEMS.register("focus_warding",
            ItemFocusWarding::new);

    // TC6-only foci - removed (TC4 has no primal "focus_air/water/earth/
    // order/entropy" and no focus_treasure; OpenTC4 foci list confirmed)
    // public static final DeferredHolder<Item, Item> FOCUS_TREASURE = ...
    // public static final DeferredHolder<Item, Item> FOCUS_AIR = ...
    // public static final DeferredHolder<Item, Item> FOCUS_WATER = ...
    // public static final DeferredHolder<Item, Item> FOCUS_EARTH = ...
    // public static final DeferredHolder<Item, Item> FOCUS_ORDER = ...
    // public static final DeferredHolder<Item, Item> FOCUS_ENTROPY = ...

    // TC6-only: focus upgrade items — removed (not in TC4)
    // 17 items: frugal, potency, enlarge, extend, treasure, fireball, firebeam,
    // alchemists_fire, chainlightning, earthshock, dowsing, silktouch, scattershot,
    // iceboulder, alchemists_frost, architect, frostbeam

    // public static final DeferredHolder<Item, Item> FOCUS_UPGRADE_HIDING = ITEMS.register("focus_upgrade_hiding",
    //         () -> new ItemFocusUpgrade(thaumcraft.api.wands.FocusUpgradeType.hiding));

    // ==================== Golem Items ====================

    public static final DeferredHolder<Item, Item> GOLEM_PLACER = ITEMS.register("golem_placer",
            ItemGolemPlacer::new);

    public static final DeferredHolder<Item, Item> GOLEM_BELL = ITEMS.register("golem_bell",
            ItemGolemBell::new);

    // ==================== Golem Seals (TC5/TC6-only, removed) ====================
    // TC4.2.3.5 has no control seals: golems use Golem Animation Cores (see GolemCoreType).
    // The seal entity/AI system (SealHandler, golem tasks) is still present and needs a
    // dedicated golemancy rework; the seal *items* are no longer registered.

    // ==================== Crystal Essence ====================

    public static final DeferredHolder<Item, Item> CRYSTAL_ESSENCE = ITEMS.register("crystal_essence",
            () -> new ItemCrystalEssence());

    // ==================== Golem Cores (TC4 ItemGolemCore) ====================
    // TC4 used ONE item with 12 metadata variants + the blank base (100).
    // Here the variant lives in the "thaumcraft:golem_core_type" Data Component;
    // see ItemGolemCore.createAllStacks() / GolemCoreType for the variants.
    public static final DeferredHolder<Item, Item> GOLEM_CORE = ITEMS.register("golem_core",
            () -> new ItemGolemCore());

    // TC6-only: golem modules — removed (not in TC4, TC4 used itemGolemDecoration with metadata)
    // 16 items: vision, aggression, harvest, lumber, butcher, guard, use, provide,
    // stock, empty, fill, breaker, pickup, place, fluid, redstone

    // TC6-only: golem upgrades speed/strength/durability/stealth/flight — removed
    // (TC4 golem upgrades are aspect-based: UPGRADEAIR/EARTH/FIRE/WATER/ORDER/ENTROPY)

    // ==================== Creative-Only Items ====================

    public static final DeferredHolder<Item, Item> FOLIO_OMNISCIENCE = ITEMS.register("folio_omniscience",
            ItemFolioOmniscience::new);

    // TC6-only: creative_flux_sponge — removed (not present in TC4)
    // public static final DeferredHolder<Item, Item> CREATIVE_FLUX_SPONGE = ITEMS.register("creative_flux_sponge",
    //         () -> new ItemCreativeFluxSponge(new Item.Properties()));

    // ==================== Essentia Containers ====================

    public static final DeferredHolder<Item, Item> JAR_NODE = ITEMS.register("jar_node",
            () -> new ItemJarNode(ModBlocks.JAR_NODE.get(), new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    // ==================== Essentia Containers (MISSING CLASSES - TODO) ====================
    // public static final DeferredHolder<Item, Item> JAR_VOID = ITEMS.register("jar_void",
    //         () -> new thaumcraft.common.items.essentia.ItemWardedJar(thaumcraft.common.blocks.essentia.BlockJar.JarType.VOID));
    //
    // public static final DeferredHolder<Item, Item> JAR_BRAIN = ITEMS.register("jar_brain",
    //         () -> new thaumcraft.common.items.essentia.ItemWardedJar(thaumcraft.common.blocks.essentia.BlockJar.JarType.BRAIN));
    //
    // public static final DeferredHolder<Item, Item> TUBE_NORMAL = ITEMS.register("tube_normal",
    //         () -> new thaumcraft.common.items.essentia.ItemEssentiaTube(thaumcraft.common.blocks.essentia.BlockTube.TubeType.NORMAL));
    //
    // public static final DeferredHolder<Item, Item> TUBE_RESTRICTED = ITEMS.register("tube_restricted",
    //         () -> new thaumcraft.common.items.essentia.ItemEssentiaTube(thaumcraft.common.blocks.essentia.BlockTube.TubeType.RESTRICTED));
    //
    // public static final DeferredHolder<Item, Item> TUBE_VALVE = ITEMS.register("tube_valve",
    //         () -> new thaumcraft.common.items.essentia.ItemEssentiaTube(thaumcraft.common.blocks.essentia.BlockTube.TubeType.VALVE));
    //
    // public static final DeferredHolder<Item, Item> TUBE_FILTER = ITEMS.register("tube_filter",
    //         () -> new thaumcraft.common.items.essentia.ItemEssentiaTube(thaumcraft.common.blocks.essentia.BlockTube.TubeType.FILTER));
    //
    // public static final DeferredHolder<Item, Item> TUBE_BUFFER = ITEMS.register("tube_buffer",
    //         () -> new thaumcraft.common.items.essentia.ItemEssentiaTube(thaumcraft.common.blocks.essentia.BlockTube.TubeType.BUFFER));
    //
    // public static final DeferredHolder<Item, Item> TUBE_ONEWAY = ITEMS.register("tube_oneway",
    //         () -> new thaumcraft.common.items.essentia.ItemEssentiaTube(thaumcraft.common.blocks.essentia.BlockTube.TubeType.ONEWAY));

    // public static final DeferredHolder<Item, Item> TUBE_DIRECTIONAL = ITEMS.register("tube_directional",
    //         () -> new thaumcraft.common.items.essentia.ItemEssentiaTube(thaumcraft.common.blocks.essentia.BlockTube.TubeType.DIRECTIONAL));

    // ==================== Essentia Containers (MISSING CLASSES - TODO) ====================
    // public static final DeferredHolder<Item, Item> ALEMBIC = ITEMS.register("alembic",
    //         () -> new thaumcraft.common.items.essentia.ItemAlembic());
    //
    // public static final DeferredHolder<Item, Item> SMELTER_BASIC = ITEMS.register("smelter_basic",
    //         () -> new thaumcraft.common.items.essentia.ItemEssentiaSmelter(thaumcraft.common.blocks.essentia.BlockSmelter.SmelterType.BASIC));
    //
    // public static final DeferredHolder<Item, Item> SMELTER_THAUMIUM = ITEMS.register("smelter_thaumium",
    //         () -> new thaumcraft.common.items.essentia.ItemEssentiaSmelter(thaumcraft.common.blocks.essentia.BlockSmelter.SmelterType.THAUMIUM));
    //
    // public static final DeferredHolder<Item, Item> SMELTER_VOID = ITEMS.register("smelter_void",
    //         () -> new thaumcraft.common.items.essentia.ItemEssentiaSmelter(thaumcraft.common.blocks.essentia.BlockSmelter.SmelterType.VOID));
    //
    // public static final DeferredHolder<Item, Item> CENTRIFUGE = ITEMS.register("centrifuge",
    //         () -> new thaumcraft.common.items.essentia.ItemCentrifuge());
    // public static final DeferredHolder<Item, Item> ESSENTIA_RESERVOIR = ITEMS.register("essentia_reservoir",
    //         () -> new thaumcraft.common.items.essentia.ItemEssentiaReservoir());
    //
    // public static final DeferredHolder<Item, Item> ESSENTIA_INPUT = ITEMS.register("essentia_input",
    //         () -> new thaumcraft.common.items.essentia.ItemEssentiaTransfuser(thaumcraft.common.blocks.essentia.BlockEssentiaTransport.TransportType.INPUT));
    //
    // public static final DeferredHolder<Item, Item> ESSENTIA_OUTPUT = ITEMS.register("essentia_output",
    //         () -> new thaumcraft.common.items.essentia.ItemEssentiaTransfuser(thaumcraft.common.blocks.essentia.BlockEssentiaTransport.TransportType.OUTPUT));

    // ==================== Additional Baubles (MISSING CLASSES - TODO) ====================

    // public static final DeferredHolder<Item, Item> AMULET_FANCY = ITEMS.register("amulet_fancy",
    //         thaumcraft.common.items.baubles.ItemAmuletFancy::new);
    //
    // public static final DeferredHolder<Item, Item> RING_FANCY = ITEMS.register("ring_fancy",
    //         thaumcraft.common.items.baubles.ItemRingFancy::new);
    //
    // public static final DeferredHolder<Item, Item> BELT_FANCY = ITEMS.register("belt_fancy",
    //         thaumcraft.common.items.baubles.ItemBeltFancy::new);
    //
    // public static final DeferredHolder<Item, Item> RING_APPRENTICE = ITEMS.register("ring_apprentice",
    //         thaumcraft.common.items.baubles.ItemRingApprentice::new);

    // ==================== Golem Items (MISSING CLASSES - TODO) ====================
    // public static final DeferredHolder<Item, Item> GOLEM_PLACER = ITEMS.register("golem_placer",
    //         ItemGolemPlacer::new);
    //
    // public static final DeferredHolder<Item, Item> GOLEM_BELL = ITEMS.register("golem_bell",
    //         ItemGolemBell::new);
    //
    // ... (other golem items commented out)

    // ==================== Golem Seals (TC5/TC6-only, removed) ====================

    // ==================== Crystal Essence (MISSING CLASS - TODO) ====================
    // public static final DeferredHolder<Item, Item> CRYSTAL_ESSENCE = ITEMS.register("crystal_essence",
    //         () -> new ItemCrystalEssence());

    // ==================== Turret Placers (TC6-only, removed) ====================

    // ==================== Focus Pouch (registered above) ====================

    // TC6-only: rare_earths, alchemical_brass_*, advanced_brass_*, quicksilver_drop — removed
    // public static final DeferredHolder<Item, Item> RARE_EARTHS = ...
    // public static final DeferredHolder<Item, Item> ALCHEMICAL_BRASS_INGOT = ...
    // public static final DeferredHolder<Item, Item> ALCHEMICAL_BRASS_NUGGET = ...
    // public static final DeferredHolder<Item, Item> ADVANCED_BRASS_INGOT = ...
    // public static final DeferredHolder<Item, Item> ADVANCED_BRASS_NUGGET = ...
    // public static final DeferredHolder<Item, Item> QUICKSILVER_DROP = ...

    public static final DeferredHolder<Item, Item> SILVER_INGOT = ITEMS.register("silver_ingot",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> SILVER_NUGGET = ITEMS.register("silver_nugget",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> TIN_INGOT = ITEMS.register("tin_ingot",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> TIN_NUGGET = ITEMS.register("tin_nugget",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> LEAD_INGOT = ITEMS.register("lead_ingot",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> LEAD_NUGGET = ITEMS.register("lead_nugget",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> COPPER_INGOT = ITEMS.register("copper_ingot",
            ItemMaterial::basic);

    public static final DeferredHolder<Item, Item> COPPER_NUGGET = ITEMS.register("copper_nugget",
            ItemMaterial::basic);

    // ==================== Aura Node (Creative/GM1) ====================

    public static final DeferredHolder<Item, Item> NODE_ITEM = ITEMS.register("node",
            () -> new thaumcraft.common.items.misc.ItemBlockNode(ModBlocks.NODE.get()));

    // ==================== Restored TC4 Items ====================
    // These items existed in TC4 but were missing from the port.

    // Primal Charm - TC4 itemResource meta 15, used for staff rods and the sceptre
    public static final DeferredHolder<Item, Item> PRIMAL_CHARM = ITEMS.register("primal_charm",
            ItemMaterial::basic);

    // Mana Bean - consumable food that gives random aspects (TC4)
    public static final DeferredHolder<Item, Item> MANA_BEAN = ITEMS.register("mana_bean",
            () -> new Item(new Item.Properties()
                    .food(new net.minecraft.world.food.FoodProperties.Builder()
                            .nutrition(1)
                            .saturationModifier(0.3f)
                            .build())));

    // Buckets for Thaumcraft fluids (TC4) — already registered in ModFluids
    // public static final DeferredHolder<Item, Item> BUCKET_DEATH = ... (see ModFluids.LIQUID_DEATH_BUCKET)
    // public static final DeferredHolder<Item, Item> BUCKET_PURE = ... (see ModFluids.PURIFYING_FLUID_BUCKET)

    // Compass Stone - Thaumcraft compass (TC4)
    public static final DeferredHolder<Item, Item> COMPASS_STONE = ITEMS.register("compass_stone",
            ItemMaterial::basic);

    // Arcane Door Key (TC4)
    public static final DeferredHolder<Item, Item> ARCANE_DOOR_KEY = ITEMS.register("arcane_door_key",
            ItemMaterial::uncommon);

    // Inkwell - converts two wooden tables into a Research Table (TC4);
    // doubles as the table's ink supply (IScribeTools, 100 uses)
    public static final DeferredHolder<Item, Item> INKWELL = ITEMS.register("inkwell",
            thaumcraft.common.items.tools.ItemInkwell::new);

    // Wisp Essence - dropped by wisps and destroyed aura nodes (TC4). Carries
    // a single aspect in its data (see ItemWispEssence).
    public static final DeferredHolder<Item, Item> WISP_ESSENCE = ITEMS.register("wisp_essence",
            thaumcraft.common.items.resources.ItemWispEssence::new);

    // Trunk Spawner - for traveling trunk golem (TC4)
    public static final DeferredHolder<Item, Item> TRUNK_SPAWNER = ITEMS.register("trunk_spawner",
            ItemMaterial::uncommon);

    // Primal Arrow (TC4)
    public static final DeferredHolder<Item, Item> PRIMAL_ARROW = ITEMS.register("primal_arrow",
            () -> new Item(new Item.Properties().stacksTo(64)));

    // Baubles - Hover Harness (TC4: chestplate that enables Potentia-powered flight)
    public static final DeferredHolder<Item, Item> HOVER_HARNESS = ITEMS.register("hover_harness",
            thaumcraft.common.items.armor.ItemHoverHarness::new);

    // Thaumostatic Girdle (TC4: belt bauble, reduces fall damage and boosts the harness)
    public static final DeferredHolder<Item, Item> GIRDLE_HOVER = ITEMS.register("girdle_hover",
            thaumcraft.common.items.baubles.ItemGirdleHover::new);

    // Runic Baubles (TC4: protection items)
    public static final DeferredHolder<Item, Item> AMULET_RUNIC = ITEMS.register("amulet_runic",
            ItemMaterial::uncommon);

    public static final DeferredHolder<Item, Item> RING_RUNIC = ITEMS.register("ring_runic",
            ItemMaterial::uncommon);

    public static final DeferredHolder<Item, Item> GIRDLE_RUNIC = ITEMS.register("girdle_runic",
            ItemMaterial::uncommon);

    // Bone Bow (TC4)
    public static final DeferredHolder<Item, Item> BOW_BONE = ITEMS.register("bow_bone",
            ItemMaterial::uncommon);

    // Item Essence / Spawn Egg (TC4) — simplified for now
    public static final DeferredHolder<Item, Item> SPAWNER_EGG = ITEMS.register("spawner_egg",
            () -> new Item(new Item.Properties()));

}






