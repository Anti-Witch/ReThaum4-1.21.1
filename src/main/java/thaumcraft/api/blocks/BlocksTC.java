package thaumcraft.api.blocks;

import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;
import thaumcraft.init.ModBlocks;

/**
 * References to Thaumcraft blocks.
 * For 1.20.1, these point to the DeferredHolders in ModBlocks.
 * 
 * Note: Accessing .get() on DeferredHolders is only safe after the block registry event has fired.
 */
public class BlocksTC {

    // Stone Blocks
    public static final DeferredHolder<Block, Block> stoneArcane = ModBlocks.ARCANE_STONE;
    public static final DeferredHolder<Block, Block> stoneArcaneBrick = ModBlocks.ARCANE_STONE_BRICK;
    public static final DeferredHolder<Block, Block> stoneAncient = ModBlocks.ANCIENT_STONE;
    public static final DeferredHolder<Block, Block> stoneAncientTile = ModBlocks.ANCIENT_STONE_TILE;
    public static final DeferredHolder<Block, Block> stoneAncientRock = ModBlocks.ANCIENT_STONE_ROCK;
    public static final DeferredHolder<Block, Block> stoneAncientGlyphed = ModBlocks.ANCIENT_STONE_GLYPHED;
    public static final DeferredHolder<Block, Block> stoneAncientDoorway = ModBlocks.ANCIENT_STONE_DOORWAY;
    public static final DeferredHolder<Block, Block> stoneEldritchTile = ModBlocks.ELDRITCH_STONE_TILE;
    public static final DeferredHolder<Block, Block> stonePorous = ModBlocks.POROUS_STONE;

    // Stairs
    public static final DeferredHolder<Block, Block> stairsArcane = ModBlocks.ARCANE_STONE_STAIRS;
    public static final DeferredHolder<Block, Block> stairsArcaneBrick = ModBlocks.ARCANE_STONE_BRICK_STAIRS;
    public static final DeferredHolder<Block, Block> stairsAncient = ModBlocks.ANCIENT_STONE_STAIRS;
    
    // Slabs
    public static final DeferredHolder<Block, Block> slabArcaneStone = ModBlocks.ARCANE_STONE_SLAB;
    public static final DeferredHolder<Block, Block> slabArcaneBrick = ModBlocks.ARCANE_STONE_BRICK_SLAB;
    public static final DeferredHolder<Block, Block> slabAncient = ModBlocks.ANCIENT_STONE_SLAB;

    // Pillars
    public static final DeferredHolder<Block, Block> pillarArcane = ModBlocks.ARCANE_PILLAR;
    public static final DeferredHolder<Block, Block> pillarAncient = ModBlocks.ANCIENT_PILLAR;
    public static final DeferredHolder<Block, Block> pillarEldritch = ModBlocks.ELDRITCH_PILLAR;

    // Wood Blocks
    public static final DeferredHolder<Block, Block> logGreatwood = ModBlocks.GREATWOOD_LOG;
    public static final DeferredHolder<Block, Block> logSilverwood = ModBlocks.SILVERWOOD_LOG;
    public static final DeferredHolder<Block, Block> plankGreatwood = ModBlocks.GREATWOOD_PLANKS;
    public static final DeferredHolder<Block, Block> plankSilverwood = ModBlocks.SILVERWOOD_PLANKS;
    public static final DeferredHolder<Block, Block> stairsGreatwood = ModBlocks.GREATWOOD_STAIRS;
    public static final DeferredHolder<Block, Block> stairsSilverwood = ModBlocks.SILVERWOOD_STAIRS;
    public static final DeferredHolder<Block, Block> slabGreatwood = ModBlocks.GREATWOOD_SLAB;
    public static final DeferredHolder<Block, Block> slabSilverwood = ModBlocks.SILVERWOOD_SLAB;
    public static final DeferredHolder<Block, Block> leafGreatwood = ModBlocks.GREATWOOD_LEAVES;
    public static final DeferredHolder<Block, Block> leafSilverwood = ModBlocks.SILVERWOOD_LEAVES;
    public static final DeferredHolder<Block, Block> saplingGreatwood = ModBlocks.GREATWOOD_SAPLING;
    public static final DeferredHolder<Block, Block> saplingSilverwood = ModBlocks.SILVERWOOD_SAPLING;

    // Metal Blocks
    public static final DeferredHolder<Block, Block> metalBlockBrass = ModBlocks.BRASS_BLOCK;
    public static final DeferredHolder<Block, Block> metalBlockThaumium = ModBlocks.THAUMIUM_BLOCK;
    public static final DeferredHolder<Block, Block> metalBlockVoid = ModBlocks.VOID_METAL_BLOCK;
    public static final DeferredHolder<Block, Block> metalAlchemical = ModBlocks.ALCHEMICAL_BRASS_BLOCK;
    public static final DeferredHolder<Block, Block> metalAlchemicalAdvanced = ModBlocks.ALCHEMICAL_BRASS_ADVANCED_BLOCK;

    // Amber
    public static final DeferredHolder<Block, Block> amberBlock = ModBlocks.AMBER_BLOCK;
    public static final DeferredHolder<Block, Block> amberBrick = ModBlocks.AMBER_BRICK;

    // Matrix (TC6-only matrix_speed/matrix_cost removed - not in TC4.2.3.5)

    // Crafting
    public static final DeferredHolder<Block, Block> arcaneWorkbench = ModBlocks.ARCANE_WORKBENCH;
    public static final DeferredHolder<Block, Block> crucible = ModBlocks.CRUCIBLE;
    public static final DeferredHolder<Block, Block> researchTable = ModBlocks.RESEARCH_TABLE;
    public static final DeferredHolder<Block, Block> infusionMatrix = ModBlocks.INFUSION_MATRIX;
    public static final DeferredHolder<Block, Block> focalManipulator = ModBlocks.FOCAL_MANIPULATOR;
    public static final DeferredHolder<Block, Block> thaumatorium = ModBlocks.THAUMATORIUM;
    // Pattern Crafter (TC6-only, removed - not in TC4.2.3.5)
    public static final DeferredHolder<Block, Block> golemBuilder = ModBlocks.GOLEM_BUILDER;

    // Ores
    public static final DeferredHolder<Block, Block> oreAmber = ModBlocks.AMBER_ORE;
    public static final DeferredHolder<Block, Block> oreCinnabar = ModBlocks.CINNABAR_ORE;
    public static final DeferredHolder<Block, Block> oreQuartz = ModBlocks.QUARTZ_ORE;

    // Deepslate Ores
    public static final DeferredHolder<Block, Block> deepslateOreAmber = ModBlocks.DEEPSLATE_AMBER_ORE;
    public static final DeferredHolder<Block, Block> deepslateOreCinnabar = ModBlocks.DEEPSLATE_CINNABAR_ORE;
    public static final DeferredHolder<Block, Block> deepslateOreQuartz = ModBlocks.DEEPSLATE_QUARTZ_ORE;

    // Crystals
    public static final DeferredHolder<Block, Block> crystalAir = ModBlocks.CRYSTAL_AIR;
    public static final DeferredHolder<Block, Block> crystalFire = ModBlocks.CRYSTAL_FIRE;
    public static final DeferredHolder<Block, Block> crystalWater = ModBlocks.CRYSTAL_WATER;
    public static final DeferredHolder<Block, Block> crystalEarth = ModBlocks.CRYSTAL_EARTH;
    public static final DeferredHolder<Block, Block> crystalOrder = ModBlocks.CRYSTAL_ORDER;
    public static final DeferredHolder<Block, Block> crystalEntropy = ModBlocks.CRYSTAL_ENTROPY;

    // Plants
    public static final DeferredHolder<Block, Block> shimmerleaf = ModBlocks.SHIMMERLEAF;
    public static final DeferredHolder<Block, Block> cinderpearl = ModBlocks.CINDERPEARL;
    public static final DeferredHolder<Block, Block> vishroom = ModBlocks.VISHROOM;

    // Devices
    public static final DeferredHolder<Block, Block> pedestalArcane = ModBlocks.PEDESTAL_ARCANE;
    public static final DeferredHolder<Block, Block> pedestalAncient = ModBlocks.PEDESTAL_ANCIENT;
    public static final DeferredHolder<Block, Block> pedestalEldritch = ModBlocks.PEDESTAL_ELDRITCH;
    public static final DeferredHolder<Block, Block> tableWood = ModBlocks.TABLE_WOOD;
    public static final DeferredHolder<Block, Block> tableStone = ModBlocks.TABLE_STONE;
    public static final DeferredHolder<Block, Block> rechargePedestal = ModBlocks.RECHARGE_PEDESTAL;
    public static final DeferredHolder<Block, Block> bellows = ModBlocks.BELLOWS;
    public static final DeferredHolder<Block, Block> hungryChest = ModBlocks.HUNGRY_CHEST;
    public static final DeferredHolder<Block, Block> mirror = ModBlocks.MIRROR_ITEM;
    public static final DeferredHolder<Block, Block> mirrorEssentia = ModBlocks.MIRROR_ESSENTIA;
    public static final DeferredHolder<Block, Block> stabilizer = ModBlocks.STABILIZER;
    public static final DeferredHolder<Block, Block> arcaneEar = ModBlocks.ARCANE_EAR;
    public static final DeferredHolder<Block, Block> arcaneEarToggle = ModBlocks.ARCANE_EAR_TOGGLE;
    public static final DeferredHolder<Block, Block> redstoneRelay = ModBlocks.REDSTONE_RELAY;
    public static final DeferredHolder<Block, Block> levitator = ModBlocks.LEVITATOR;
    // TC6-only: dioptra / void_siphon / potion_sprayer / vis_generator / vis_battery / condenser removed
    public static final DeferredHolder<Block, Block> everfullUrn = ModBlocks.EVERFULL_URN;

    // Essentia
    public static final DeferredHolder<Block, Block> jarNormal = ModBlocks.JAR_NORMAL;
    public static final DeferredHolder<Block, Block> jarVoid = ModBlocks.JAR_VOID;
    public static final DeferredHolder<Block, Block> jarBrain = ModBlocks.JAR_BRAIN;
    public static final DeferredHolder<Block, Block> tube = ModBlocks.TUBE_NORMAL;
    public static final DeferredHolder<Block, Block> tubeValve = ModBlocks.TUBE_VALVE;
    public static final DeferredHolder<Block, Block> tubeRestrict = ModBlocks.TUBE_RESTRICTED;
    public static final DeferredHolder<Block, Block> tubeOneway = ModBlocks.TUBE_ONEWAY;
    public static final DeferredHolder<Block, Block> tubeFilter = ModBlocks.TUBE_FILTER;
    public static final DeferredHolder<Block, Block> tubeBuffer = ModBlocks.TUBE_BUFFER;
    public static final DeferredHolder<Block, Block> alembic = ModBlocks.ALEMBIC;
    public static final DeferredHolder<Block, Block> smelterBasic = ModBlocks.SMELTER;
    public static final DeferredHolder<Block, Block> centrifuge = ModBlocks.CENTRIFUGE;
    public static final DeferredHolder<Block, Block> infernalFurnace = ModBlocks.INFERNAL_FURNACE;
    public static final DeferredHolder<Block, Block> essentiaReservoir = ModBlocks.ESSENTIA_RESERVOIR;
    public static final DeferredHolder<Block, Block> spa = ModBlocks.SPA;
    public static final DeferredHolder<Block, Block> fluxScrubber = ModBlocks.FLUX_SCRUBBER;
    public static final DeferredHolder<Block, Block> visRelay = ModBlocks.VIS_RELAY;

    // Lamps
    public static final DeferredHolder<Block, Block> lampArcane = ModBlocks.LAMP_ARCANE;
    public static final DeferredHolder<Block, Block> lampGrowth = ModBlocks.LAMP_GROWTH;
    public static final DeferredHolder<Block, Block> lampFertility = ModBlocks.LAMP_FERTILITY;

    // Candles & Nitor
    public static final DeferredHolder<Block, Block> candleWhite = ModBlocks.CANDLE_WHITE;
    public static final DeferredHolder<Block, Block> nitorWhite = ModBlocks.NITOR_WHITE;
    // ... mapping for all colors would go here, omitting for brevity in this initial pass ...

    // Infusion Support
    public static final DeferredHolder<Block, Block> inlay = ModBlocks.INLAY;

    // Effects & Misc
    public static final DeferredHolder<Block, Block> effectSap = ModBlocks.EFFECT_SAP;
    public static final DeferredHolder<Block, Block> effectShock = ModBlocks.EFFECT_SHOCK;
    public static final DeferredHolder<Block, Block> effectGlimmer = ModBlocks.EFFECT_GLIMMER;
    public static final DeferredHolder<Block, Block> hole = ModBlocks.HOLE;
    public static final DeferredHolder<Block, Block> fluxGoo = ModBlocks.FLUX_GOO;
    public static final DeferredHolder<Block, Block> fluxGas = ModBlocks.FLUX_GAS;
    public static final DeferredHolder<Block, Block> taintFibre = ModBlocks.TAINT_FIBRE;

}





