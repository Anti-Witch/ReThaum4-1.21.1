package thaumcraft.init;

import net.minecraft.core.registries.Registries;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import net.minecraft.world.item.DyeColor;
import thaumcraft.common.blocks.BlockTC;
import thaumcraft.common.blocks.basic.*;
import thaumcraft.common.blocks.basic.BlockBannerTC;
import thaumcraft.common.blocks.basic.BlockPavingStone;
import thaumcraft.common.blocks.basic.BlockTable;
import thaumcraft.common.blocks.crafting.BlockArcaneWorkbench;
import thaumcraft.common.blocks.crafting.BlockArcaneWorkbenchCharger;
import thaumcraft.common.blocks.crafting.BlockCrucible;
import thaumcraft.common.blocks.crafting.BlockDeconstructionTable;
import thaumcraft.common.blocks.crafting.BlockFocalManipulator;
import thaumcraft.common.blocks.crafting.BlockGolemBuilder;
import thaumcraft.common.blocks.crafting.BlockInfusionMatrix;
import thaumcraft.common.blocks.crafting.BlockResearchTable;
import thaumcraft.common.blocks.crafting.BlockThaumatorium;
import thaumcraft.common.blocks.crafting.BlockThaumatoriumTop;
import thaumcraft.common.blocks.devices.*;
import thaumcraft.common.blocks.essentia.BlockAlembic;
import thaumcraft.common.blocks.essentia.BlockCentrifuge;
import thaumcraft.common.blocks.essentia.BlockEssentiaReservoir;
import thaumcraft.common.blocks.essentia.BlockJar;
import thaumcraft.common.blocks.essentia.BlockSmelter;
import thaumcraft.common.blocks.essentia.BlockTube;
import thaumcraft.common.blocks.essentia.BlockEssentiaTransport;
import thaumcraft.common.blocks.misc.BlockBarrier;
import thaumcraft.common.blocks.misc.BlockEffect;
import thaumcraft.common.blocks.misc.BlockFluidDeath;
import thaumcraft.common.blocks.misc.BlockFluidPure;
import thaumcraft.common.blocks.misc.BlockHole;
import thaumcraft.common.blocks.misc.BlockNitor;
import thaumcraft.common.blocks.misc.BlockNode;
import thaumcraft.common.blocks.misc.BlockVisNode;
import thaumcraft.common.blocks.world.ore.BlockCrystalTC;
import thaumcraft.common.blocks.world.ore.BlockOreTC;
import thaumcraft.common.blocks.world.plants.BlockCinderpearl;
import thaumcraft.common.blocks.world.plants.BlockLeavesTC;
import thaumcraft.common.blocks.world.plants.BlockSaplingTC;
import thaumcraft.common.blocks.world.plants.BlockShimmerleaf;
import thaumcraft.common.blocks.world.plants.BlockVishroom;
import thaumcraft.common.blocks.world.taint.BlockFluxGas;
import thaumcraft.common.blocks.world.taint.BlockFluxGoo;
import thaumcraft.common.blocks.world.taint.BlockTaint;
import thaumcraft.common.blocks.world.taint.BlockTaintFeature;
import thaumcraft.common.blocks.world.taint.BlockTaintFibre;
import thaumcraft.common.blocks.world.taint.BlockTaintLog;
import thaumcraft.common.blocks.world.taint.BlockTaintWeed;
import thaumcraft.common.blocks.world.BlockGrassAmbient;
import thaumcraft.common.blocks.world.BlockLoot;

import java.util.function.Supplier;

/**
 * Registry for all Thaumcraft blocks.
 * Uses DeferredRegister for 1.20.1 Forge.
 */
public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, Thaumcraft.MODID);
    public static final DeferredRegister<Item> BLOCK_ITEMS = DeferredRegister.create(Registries.ITEM, Thaumcraft.MODID);

    // ==================== Stone Blocks ====================

    public static final DeferredHolder<Block, Block> ARCANE_STONE = registerBlock("arcane_stone",
            () -> BlockStoneTC.create(true));

    public static final DeferredHolder<Block, Block> ARCANE_STONE_BRICK = registerBlock("arcane_stone_brick",
            () -> BlockStoneTC.create(true));

    public static final DeferredHolder<Block, Block> ANCIENT_STONE = registerBlock("ancient_stone",
            () -> BlockStoneTC.create(true));

    public static final DeferredHolder<Block, Block> ANCIENT_STONE_TILE = registerBlock("ancient_stone_tile",
            () -> BlockStoneTC.create(false));

    public static final DeferredHolder<Block, Block> ANCIENT_STONE_ROCK = registerBlock("ancient_rock",
            () -> BlockStoneTC.createUnbreakable());

    public static final DeferredHolder<Block, Block> ANCIENT_STONE_GLYPHED = registerBlock("ancient_stone_glyphed",
            () -> BlockStoneTC.create(false));

    public static final DeferredHolder<Block, Block> ANCIENT_STONE_DOORWAY = registerBlock("ancient_pedestal_doorway",
            () -> BlockStoneTC.createUnbreakable());

    public static final DeferredHolder<Block, Block> ELDRITCH_STONE_TILE = registerBlock("eldritch_stone_tile",
            () -> BlockStoneTC.createReinforced());

    public static final DeferredHolder<Block, Block> POROUS_STONE = registerBlock("porous_stone",
            () -> new BlockStoneTC(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(1.5f, 10.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops(), true));

    // ==================== Obsidian Totem (TC4) ====================

    public static final DeferredHolder<Block, Block> OBSIDIAN_TILE = registerBlock("obsidian_tile",
            BlockObsidianTile::new);

    public static final DeferredHolder<Block, Block> OBSIDIAN_TOTEM = registerBlock("obsidian_totem",
            BlockObsidianTotem::new);

    public static final DeferredHolder<Block, Block> CHARGED_OBSIDIAN_TOTEM = registerBlock("charged_obsidian_totem",
            BlockChargedObsidianTotem::new);

    // ==================== Stairs ====================

    public static final DeferredHolder<Block, Block> ARCANE_STONE_STAIRS = registerBlock("arcane_stone_stairs",
            () -> new StairBlock(ARCANE_STONE.get().defaultBlockState(),
                    BlockBehaviour.Properties.ofFullCopy(ARCANE_STONE.get())));

    public static final DeferredHolder<Block, Block> ARCANE_STONE_BRICK_STAIRS = registerBlock("arcane_stone_brick_stairs",
            () -> new StairBlock(ARCANE_STONE_BRICK.get().defaultBlockState(),
                    BlockBehaviour.Properties.ofFullCopy(ARCANE_STONE_BRICK.get())));

    public static final DeferredHolder<Block, Block> ANCIENT_STONE_STAIRS = registerBlock("ancient_stone_stairs",
            () -> new StairBlock(ANCIENT_STONE.get().defaultBlockState(),
                    BlockBehaviour.Properties.ofFullCopy(ANCIENT_STONE.get())));

    // ==================== Slabs ====================

    public static final DeferredHolder<Block, Block> ARCANE_STONE_SLAB = registerBlock("arcane_stone_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(2.0f, 10.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final DeferredHolder<Block, Block> ARCANE_STONE_BRICK_SLAB = registerBlock("arcane_stone_brick_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(2.0f, 10.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final DeferredHolder<Block, Block> ANCIENT_STONE_SLAB = registerBlock("ancient_stone_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(2.0f, 10.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    // ==================== Pillars ====================

    public static final DeferredHolder<Block, Block> ARCANE_PILLAR = registerBlock("arcane_pillar",
            () -> BlockPillarTC.createArcane(ARCANE_STONE.get()));

    public static final DeferredHolder<Block, Block> ANCIENT_PILLAR = registerBlock("ancient_pillar",
            () -> BlockPillarTC.createAncient(ANCIENT_STONE.get()));

    public static final DeferredHolder<Block, Block> ELDRITCH_PILLAR = registerBlock("eldritch_pillar",
            () -> BlockPillarTC.createEldritch(ELDRITCH_STONE_TILE.get()));

    // ==================== Wood Blocks ====================

    public static final DeferredHolder<Block, Block> GREATWOOD_LOG = registerBlock("greatwood_log",
            BlockLogTC::createGreatwood);

    public static final DeferredHolder<Block, Block> SILVERWOOD_LOG = registerBlock("silverwood_log",
            BlockLogTC::createSilverwood);

    public static final DeferredHolder<Block, Block> GREATWOOD_PLANKS = registerBlock("greatwood_planks",
            BlockPlanksTC::createGreatwood);

    public static final DeferredHolder<Block, Block> SILVERWOOD_PLANKS = registerBlock("silverwood_planks",
            BlockPlanksTC::createSilverwood);

    public static final DeferredHolder<Block, Block> GREATWOOD_STAIRS = registerBlock("greatwood_stairs",
            () -> new StairBlock(GREATWOOD_PLANKS.get().defaultBlockState(),
                    BlockBehaviour.Properties.ofFullCopy(GREATWOOD_PLANKS.get())));

    public static final DeferredHolder<Block, Block> SILVERWOOD_STAIRS = registerBlock("silverwood_stairs",
            () -> new StairBlock(SILVERWOOD_PLANKS.get().defaultBlockState(),
                    BlockBehaviour.Properties.ofFullCopy(SILVERWOOD_PLANKS.get())));

    public static final DeferredHolder<Block, Block> GREATWOOD_SLAB = registerBlock("greatwood_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(2.0f, 3.0f)
                    .sound(SoundType.WOOD)));

    public static final DeferredHolder<Block, Block> SILVERWOOD_SLAB = registerBlock("silverwood_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.QUARTZ)
                    .strength(2.0f, 3.0f)
                    .sound(SoundType.WOOD)));

    // ==================== Metal Blocks ====================

    public static final DeferredHolder<Block, Block> BRASS_BLOCK = registerBlock("brass_block",
            () -> BlockMetalTC.create(MapColor.GOLD));

    public static final DeferredHolder<Block, Block> THAUMIUM_BLOCK = registerBlock("thaumium_block",
            () -> BlockMetalTC.create(MapColor.COLOR_PURPLE));

    public static final DeferredHolder<Block, Block> VOID_METAL_BLOCK = registerBlock("void_metal_block",
            () -> BlockMetalTC.create(MapColor.COLOR_BLACK));

    public static final DeferredHolder<Block, Block> ALCHEMICAL_BRASS_BLOCK = registerBlock("alchemical_brass_block",
            () -> BlockMetalTC.create(MapColor.GOLD));

    public static final DeferredHolder<Block, Block> ALCHEMICAL_BRASS_ADVANCED_BLOCK = registerBlock("alchemical_brass_advanced_block",
            () -> BlockMetalTC.create(MapColor.GOLD));

    // ==================== Amber Blocks ====================

    public static final DeferredHolder<Block, Block> AMBER_BLOCK = registerBlock("amber_block",
            () -> new BlockTC(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(3.0f)
                    .sound(SoundType.GLASS)
                    .noOcclusion()));

    public static final DeferredHolder<Block, Block> AMBER_BRICK = registerBlock("amber_brick",
            () -> new BlockTC(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(3.0f)
                    .sound(SoundType.GLASS)
                    .noOcclusion()));

    // ==================== Crafting Blocks ====================

    public static final DeferredHolder<Block, Block> ARCANE_WORKBENCH = registerBlock("arcane_workbench",
            BlockArcaneWorkbench::new);

    public static final DeferredHolder<Block, Block> ARCANE_WORKBENCH_CHARGER = registerBlock("arcane_workbench_charger",
            BlockArcaneWorkbenchCharger::create);


    public static final DeferredHolder<Block, Block> CRUCIBLE = registerBlock("crucible",
            BlockCrucible::new);

    public static final DeferredHolder<Block, Block> RESEARCH_TABLE = registerBlock("research_table",
            BlockResearchTable::new);

    public static final DeferredHolder<Block, Block> DECONSTRUCTION_TABLE = registerBlock("deconstruction_table",
            BlockDeconstructionTable::new);

    public static final DeferredHolder<Block, Block> INFUSION_MATRIX = registerBlock("infusion_matrix",
            BlockInfusionMatrix::new);

    // ==================== Ore Blocks ====================

    public static final DeferredHolder<Block, Block> AMBER_ORE = registerBlock("amber_ore",
            BlockOreTC::createAmberOre);

    public static final DeferredHolder<Block, Block> CINNABAR_ORE = registerBlock("cinnabar_ore",
            BlockOreTC::createCinnabarOre);

    public static final DeferredHolder<Block, Block> QUARTZ_ORE = registerBlock("quartz_ore",
            BlockOreTC::createQuartzOre);

    // ==================== Deepslate Ore Blocks ====================

    public static final DeferredHolder<Block, Block> DEEPSLATE_AMBER_ORE = registerBlock("deepslate_amber_ore",
            BlockOreTC::createDeepslateAmberOre);

    public static final DeferredHolder<Block, Block> DEEPSLATE_CINNABAR_ORE = registerBlock("deepslate_cinnabar_ore",
            BlockOreTC::createDeepslateCinnabarOre);

    public static final DeferredHolder<Block, Block> DEEPSLATE_QUARTZ_ORE = registerBlock("deepslate_quartz_ore",
            BlockOreTC::createDeepslateQuartzOre);

    // ==================== Crystal Blocks ====================

    public static final DeferredHolder<Block, Block> CRYSTAL_AIR = registerBlock("crystal_air",
            () -> new BlockCrystalTC(Aspect.AIR));

    public static final DeferredHolder<Block, Block> CRYSTAL_FIRE = registerBlock("crystal_fire",
            () -> new BlockCrystalTC(Aspect.FIRE));

    public static final DeferredHolder<Block, Block> CRYSTAL_WATER = registerBlock("crystal_water",
            () -> new BlockCrystalTC(Aspect.WATER));

    public static final DeferredHolder<Block, Block> CRYSTAL_EARTH = registerBlock("crystal_earth",
            () -> new BlockCrystalTC(Aspect.EARTH));

    public static final DeferredHolder<Block, Block> CRYSTAL_ORDER = registerBlock("crystal_order",
            () -> new BlockCrystalTC(Aspect.ORDER));

    public static final DeferredHolder<Block, Block> CRYSTAL_ENTROPY = registerBlock("crystal_entropy",
            () -> new BlockCrystalTC(Aspect.ENTROPY));

    // ==================== Plant Blocks ====================

    public static final DeferredHolder<Block, Block> GRASS_AMBIENT = registerBlock("grass_ambient",
            BlockGrassAmbient::new);

    public static final DeferredHolder<Block, Block> SHIMMERLEAF = registerBlock("shimmerleaf",
            BlockShimmerleaf::new);

    public static final DeferredHolder<Block, Block> CINDERPEARL = registerBlock("cinderpearl",
            BlockCinderpearl::new);

    public static final DeferredHolder<Block, Block> VISHROOM = registerBlock("vishroom",
            BlockVishroom::new);

    // ==================== Tree Blocks ====================

    public static final DeferredHolder<Block, Block> GREATWOOD_LEAVES = registerBlock("greatwood_leaves",
            BlockLeavesTC::createGreatwood);

    public static final DeferredHolder<Block, Block> SILVERWOOD_LEAVES = registerBlock("silverwood_leaves",
            BlockLeavesTC::createSilverwood);

    public static final DeferredHolder<Block, Block> GREATWOOD_SAPLING = registerBlock("greatwood_sapling",
            BlockSaplingTC::createGreatwood);

    public static final DeferredHolder<Block, Block> SILVERWOOD_SAPLING = registerBlock("silverwood_sapling",
            BlockSaplingTC::createSilverwood);

    // ==================== Device Blocks ====================

    public static final DeferredHolder<Block, Block> PEDESTAL_ARCANE = registerBlock("pedestal_arcane",
            BlockPedestal::createArcane);

    public static final DeferredHolder<Block, Block> PEDESTAL_ANCIENT = registerBlock("pedestal_ancient",
            BlockPedestal::createAncient);

    public static final DeferredHolder<Block, Block> PEDESTAL_ELDRITCH = registerBlock("pedestal_eldritch",
            BlockPedestal::createEldritch);

    public static final DeferredHolder<Block, Block> TABLE_WOOD = registerBlock("table_wood",
            thaumcraft.common.blocks.basic.BlockTable::createWooden);

    public static final DeferredHolder<Block, Block> TABLE_STONE = registerBlock("table_stone",
            thaumcraft.common.blocks.basic.BlockTable::createStone);

    // ==================== Candle Blocks (16 colors) ====================

    public static final DeferredHolder<Block, Block> CANDLE_WHITE = registerBlock("candle_white",
            () -> BlockCandle.create(DyeColor.WHITE));
    public static final DeferredHolder<Block, Block> CANDLE_ORANGE = registerBlock("candle_orange",
            () -> BlockCandle.create(DyeColor.ORANGE));
    public static final DeferredHolder<Block, Block> CANDLE_MAGENTA = registerBlock("candle_magenta",
            () -> BlockCandle.create(DyeColor.MAGENTA));
    public static final DeferredHolder<Block, Block> CANDLE_LIGHT_BLUE = registerBlock("candle_light_blue",
            () -> BlockCandle.create(DyeColor.LIGHT_BLUE));
    public static final DeferredHolder<Block, Block> CANDLE_YELLOW = registerBlock("candle_yellow",
            () -> BlockCandle.create(DyeColor.YELLOW));
    public static final DeferredHolder<Block, Block> CANDLE_LIME = registerBlock("candle_lime",
            () -> BlockCandle.create(DyeColor.LIME));
    public static final DeferredHolder<Block, Block> CANDLE_PINK = registerBlock("candle_pink",
            () -> BlockCandle.create(DyeColor.PINK));
    public static final DeferredHolder<Block, Block> CANDLE_GRAY = registerBlock("candle_gray",
            () -> BlockCandle.create(DyeColor.GRAY));
    public static final DeferredHolder<Block, Block> CANDLE_LIGHT_GRAY = registerBlock("candle_light_gray",
            () -> BlockCandle.create(DyeColor.LIGHT_GRAY));
    public static final DeferredHolder<Block, Block> CANDLE_CYAN = registerBlock("candle_cyan",
            () -> BlockCandle.create(DyeColor.CYAN));
    public static final DeferredHolder<Block, Block> CANDLE_PURPLE = registerBlock("candle_purple",
            () -> BlockCandle.create(DyeColor.PURPLE));
    public static final DeferredHolder<Block, Block> CANDLE_BLUE = registerBlock("candle_blue",
            () -> BlockCandle.create(DyeColor.BLUE));
    public static final DeferredHolder<Block, Block> CANDLE_BROWN = registerBlock("candle_brown",
            () -> BlockCandle.create(DyeColor.BROWN));
    public static final DeferredHolder<Block, Block> CANDLE_GREEN = registerBlock("candle_green",
            () -> BlockCandle.create(DyeColor.GREEN));
    public static final DeferredHolder<Block, Block> CANDLE_RED = registerBlock("candle_red",
            () -> BlockCandle.create(DyeColor.RED));
    public static final DeferredHolder<Block, Block> CANDLE_BLACK = registerBlock("candle_black",
            () -> BlockCandle.create(DyeColor.BLACK));

    // ==================== Nitor Blocks (16 colors) ====================

    public static final DeferredHolder<Block, Block> NITOR_WHITE = registerBlock("nitor_white",
            () -> BlockNitor.create(DyeColor.WHITE));
    public static final DeferredHolder<Block, Block> NITOR_ORANGE = registerBlock("nitor_orange",
            () -> BlockNitor.create(DyeColor.ORANGE));
    public static final DeferredHolder<Block, Block> NITOR_MAGENTA = registerBlock("nitor_magenta",
            () -> BlockNitor.create(DyeColor.MAGENTA));
    public static final DeferredHolder<Block, Block> NITOR_LIGHT_BLUE = registerBlock("nitor_light_blue",
            () -> BlockNitor.create(DyeColor.LIGHT_BLUE));
    public static final DeferredHolder<Block, Block> NITOR_YELLOW = registerBlock("nitor_yellow",
            () -> BlockNitor.create(DyeColor.YELLOW));
    public static final DeferredHolder<Block, Block> NITOR_LIME = registerBlock("nitor_lime",
            () -> BlockNitor.create(DyeColor.LIME));
    public static final DeferredHolder<Block, Block> NITOR_PINK = registerBlock("nitor_pink",
            () -> BlockNitor.create(DyeColor.PINK));
    public static final DeferredHolder<Block, Block> NITOR_GRAY = registerBlock("nitor_gray",
            () -> BlockNitor.create(DyeColor.GRAY));
    public static final DeferredHolder<Block, Block> NITOR_LIGHT_GRAY = registerBlock("nitor_light_gray",
            () -> BlockNitor.create(DyeColor.LIGHT_GRAY));
    public static final DeferredHolder<Block, Block> NITOR_CYAN = registerBlock("nitor_cyan",
            () -> BlockNitor.create(DyeColor.CYAN));
    public static final DeferredHolder<Block, Block> NITOR_PURPLE = registerBlock("nitor_purple",
            () -> BlockNitor.create(DyeColor.PURPLE));
    public static final DeferredHolder<Block, Block> NITOR_BLUE = registerBlock("nitor_blue",
            () -> BlockNitor.create(DyeColor.BLUE));
    public static final DeferredHolder<Block, Block> NITOR_BROWN = registerBlock("nitor_brown",
            () -> BlockNitor.create(DyeColor.BROWN));
    public static final DeferredHolder<Block, Block> NITOR_GREEN = registerBlock("nitor_green",
            () -> BlockNitor.create(DyeColor.GREEN));
    public static final DeferredHolder<Block, Block> NITOR_RED = registerBlock("nitor_red",
            () -> BlockNitor.create(DyeColor.RED));
    public static final DeferredHolder<Block, Block> NITOR_BLACK = registerBlock("nitor_black",
            () -> BlockNitor.create(DyeColor.BLACK));

    // ==================== Essentia Jars ====================

    public static final DeferredHolder<Block, Block> JAR_NORMAL = registerBlock("jar_normal",
            BlockJar::createNormal);

    public static final DeferredHolder<Block, Block> JAR_VOID = registerBlock("jar_void",
            BlockJar::createVoid);

    public static final DeferredHolder<Block, Block> JAR_BRAIN = registerBlock("jar_brain",
            BlockJar::createBrain);

    /**
     * Node in a Jar (TC4 blockJar metadata 2). Registered without a BlockItem:
     * the matching item is ItemJarNode (ModItems.JAR_NODE), which carries the
     * captured node in its NBT.
     */
    public static final DeferredHolder<Block, Block> JAR_NODE = registerBlockNoItem("jar_node",
            BlockJar::createNodeJar);

    // ==================== Essentia Tubes ====================

    public static final DeferredHolder<Block, Block> TUBE_NORMAL = registerBlock("tube_normal",
            BlockTube::createNormal);

    public static final DeferredHolder<Block, Block> TUBE_RESTRICTED = registerBlock("tube_restricted",
            BlockTube::createRestricted);

    public static final DeferredHolder<Block, Block> TUBE_FILTER = registerBlock("tube_filter",
            BlockTube::createFilter);

    public static final DeferredHolder<Block, Block> TUBE_VALVE = registerBlock("tube_valve",
            BlockTube::createValve);

    public static final DeferredHolder<Block, Block> TUBE_BUFFER = registerBlock("tube_buffer",
            BlockTube::createBuffer);

    public static final DeferredHolder<Block, Block> TUBE_ONEWAY = registerBlock("tube_oneway",
            BlockTube::createOneway);

    // ==================== Advanced Crafting ====================

    public static final DeferredHolder<Block, Block> FOCAL_MANIPULATOR = registerBlock("focal_manipulator",
            BlockFocalManipulator::new);

    public static final DeferredHolder<Block, Block> THAUMATORIUM = registerBlock("thaumatorium",
            BlockThaumatorium::new);

    public static final DeferredHolder<Block, Block> THAUMATORIUM_TOP = registerBlockNoItem("thaumatorium_top",
            BlockThaumatoriumTop::new);

    // ==================== More Device Blocks ====================

    public static final DeferredHolder<Block, Block> BELLOWS = registerBlock("bellows",
            BlockBellows::new);

    public static final DeferredHolder<Block, Block> LAMP_ARCANE = registerBlock("lamp_arcane",
            BlockLamp::createArcane);

    public static final DeferredHolder<Block, Block> LAMP_GROWTH = registerBlock("lamp_growth",
            BlockLamp::createGrowth);

    public static final DeferredHolder<Block, Block> LAMP_FERTILITY = registerBlock("lamp_fertility",
            BlockLamp::createFertility);

    public static final DeferredHolder<Block, Block> HUNGRY_CHEST = registerBlock("hungry_chest",
            BlockHungryChest::new);

    public static final DeferredHolder<Block, Block> MIRROR_ITEM = registerBlock("mirror_item",
            BlockMirror::createItem);

    public static final DeferredHolder<Block, Block> MIRROR_ESSENTIA = registerBlock("mirror_essentia",
            BlockMirror::createPlayer);

    public static final DeferredHolder<Block, Block> STABILIZER = registerBlock("stabilizer",
            BlockStabilizer::new);

    // TC6-only: vis_generator / condenser / condenser_lattice(_dirty) removed (not in TC4.2.3.5)

    public static final DeferredHolder<Block, Block> ARCANE_EAR = registerBlock("arcane_ear",
            BlockArcaneEar::createPulse);

    public static final DeferredHolder<Block, Block> ARCANE_EAR_TOGGLE = registerBlock("arcane_ear_toggle",
            BlockArcaneEar::createToggle);

    public static final DeferredHolder<Block, Block> REDSTONE_RELAY = registerBlock("redstone_relay",
            BlockRedstoneRelay::new);

    // ==================== Essentia Processing ====================

    public static final DeferredHolder<Block, Block> ALEMBIC = registerBlock("alembic",
            BlockAlembic::new);

    public static final DeferredHolder<Block, Block> SMELTER = registerBlock("smelter",
            BlockSmelter::new);

    // Smelter upgrade variants (use same block class for now)
    public static final DeferredHolder<Block, Block> SMELTER_AUX = registerBlock("smelter_aux",
            BlockSmelter::new);

    public static final DeferredHolder<Block, Block> SMELTER_THAUMIUM = registerBlock("smelter_thaumium",
            BlockSmelter::new);

    public static final DeferredHolder<Block, Block> SMELTER_VOID = registerBlock("smelter_void",
            BlockSmelter::new);

    public static final DeferredHolder<Block, Block> SMELTER_VENT = registerBlock("smelter_vent",
            BlockSmelter::new);

    public static final DeferredHolder<Block, Block> CENTRIFUGE = registerBlock("centrifuge",
            BlockCentrifuge::new);

    public static final DeferredHolder<Block, Block> INFERNAL_FURNACE = registerBlock("infernal_furnace",
            BlockInfernalFurnace::new);

    public static final DeferredHolder<Block, Block> ESSENTIA_RESERVOIR = registerBlock("essentia_reservoir",
            BlockEssentiaReservoir::new);

    public static final DeferredHolder<Block, Block> ESSENTIA_INPUT = registerBlock("essentia_input",
            () -> new BlockEssentiaTransport(BlockEssentiaTransport.TransportType.INPUT));

    public static final DeferredHolder<Block, Block> ESSENTIA_OUTPUT = registerBlock("essentia_output",
            () -> new BlockEssentiaTransport(BlockEssentiaTransport.TransportType.OUTPUT));

    public static final DeferredHolder<Block, Block> SPA = registerBlock("spa",
            BlockSpa::new);

    public static final DeferredHolder<Block, Block> FLUX_SCRUBBER = registerBlock("flux_scrubber",
            BlockFluxScrubber::new);

    public static final DeferredHolder<Block, Block> VIS_RELAY = registerBlock("vis_relay",
            BlockVisRelay::new);

    // ==================== Golem Crafting ====================

    public static final DeferredHolder<Block, Block> GOLEM_BUILDER = registerBlock("golem_builder",
            BlockGolemBuilder::new);

    // ==================== Effect Blocks ====================

    public static final DeferredHolder<Block, Block> EFFECT_SAP = registerBlockNoItem("effect_sap",
            BlockEffect::createSap);

    public static final DeferredHolder<Block, Block> EFFECT_SHOCK = registerBlockNoItem("effect_shock",
            BlockEffect::createShock);

    public static final DeferredHolder<Block, Block> EFFECT_GLIMMER = registerBlockNoItem("effect_glimmer",
            BlockEffect::createGlimmer);

    // ==================== Taint Blocks ====================

    public static final DeferredHolder<Block, Block> FLESH_BLOCK = registerBlock("flesh_block",
            () -> new BlockTC(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(1.0f)
                    .sound(SoundType.SLIME_BLOCK)));

    public static final DeferredHolder<Block, Block> FLUX_GOO = registerBlockNoItem("flux_goo",
            BlockFluxGoo::new);

    public static final DeferredHolder<Block, Block> FLUX_GAS = registerBlockNoItem("flux_gas",
            BlockFluxGas::new);

    public static final DeferredHolder<Block, Block> TAINT_FIBRE = registerBlock("taint_fibre",
            BlockTaintFibre::new);

    public static final DeferredHolder<Block, Block> TAINT_SOIL = registerBlock("taint_soil",
            () -> new BlockTaint(BlockTaint.TaintType.SOIL));

    public static final DeferredHolder<Block, Block> TAINT_ROCK = registerBlock("taint_rock",
            () -> new BlockTaint(BlockTaint.TaintType.ROCK));

    public static final DeferredHolder<Block, Block> TAINT_CRUST = registerBlock("taint_crust",
            () -> new BlockTaint(BlockTaint.TaintType.CRUST));

    public static final DeferredHolder<Block, Block> TAINT_GEYSER = registerBlockNoItem("taint_geyser",
            () -> new BlockTaint(BlockTaint.TaintType.GEYSER));

    public static final DeferredHolder<Block, Block> TAINT_LOG = registerBlock("taint_log",
            BlockTaintLog::new);

    public static final DeferredHolder<Block, Block> TAINT_FEATURE = registerBlockNoItem("taint_feature",
            BlockTaintFeature::new);

    public static final DeferredHolder<Block, Block> TAINT_WEED = registerBlock("taint_weed",
            BlockTaintWeed::new);

    // ==================== Liquid Blocks ====================

    public static final DeferredHolder<Block, Block> LIQUID_DEATH = registerBlockNoItem("liquid_death",
            BlockFluidDeath::new);

    public static final DeferredHolder<Block, Block> PURIFYING_FLUID = registerBlockNoItem("purifying_fluid",
            BlockFluidPure::new);

    // ==================== Additional Devices ====================

    public static final DeferredHolder<Block, Block> LEVITATOR = registerBlock("levitator",
            BlockLevitator::new);

    public static final DeferredHolder<Block, Block> RECHARGE_PEDESTAL = registerBlock("recharge_pedestal",
            BlockRechargePedestal::new);

    // TC6-only: void_siphon / potion_sprayer / vis_battery removed (not in TC4.2.3.5)

    public static final DeferredHolder<Block, Block> BRAIN_BOX = registerBlock("brain_box",
            BlockBrainBox::create);

    public static final DeferredHolder<Block, Block> INLAY = registerBlock("inlay",
            BlockInlay::create);

    // ==================== Loot Blocks ====================

    public static final DeferredHolder<Block, Block> LOOT_CRATE_COMMON = registerBlock("loot_crate_common",
            BlockLoot::createCrateCommon);

    public static final DeferredHolder<Block, Block> LOOT_CRATE_UNCOMMON = registerBlock("loot_crate_uncommon",
            BlockLoot::createCrateUncommon);

    public static final DeferredHolder<Block, Block> LOOT_CRATE_RARE = registerBlock("loot_crate_rare",
            BlockLoot::createCrateRare);

    public static final DeferredHolder<Block, Block> LOOT_URN_COMMON = registerBlock("loot_urn_common",
            BlockLoot::createUrnCommon);

    public static final DeferredHolder<Block, Block> LOOT_URN_UNCOMMON = registerBlock("loot_urn_uncommon",
            BlockLoot::createUrnUncommon);

    public static final DeferredHolder<Block, Block> LOOT_URN_RARE = registerBlock("loot_urn_rare",
            BlockLoot::createUrnRare);

    // ==================== Misc Blocks ====================

    public static final DeferredHolder<Block, Block> NODE = registerBlockNoItem("node",
            BlockNode::new);

    public static final DeferredHolder<Block, Block> VIS_NODE = registerBlockNoItem("vis_node",
            BlockVisNode::new);

    public static final DeferredHolder<Block, Block> HOLE = registerBlockNoItem("hole",
            BlockHole::new);

    public static final DeferredHolder<Block, Block> EVERFULL_URN = registerBlock("everfull_urn",
            BlockWaterJug::new);

    // ==================== Paving Stones ====================

    public static final DeferredHolder<Block, Block> PAVING_STONE = registerBlock("paving_stone",
            BlockPavingStone::createNormal);

    public static final DeferredHolder<Block, Block> PAVING_STONE_TRAVEL = registerBlock("paving_stone_travel",
            BlockPavingStone::createTravel);

    public static final DeferredHolder<Block, Block> PAVING_STONE_BARRIER = registerBlock("paving_stone_barrier",
            BlockPavingStone::createBarrier);

    public static final DeferredHolder<Block, Block> BARRIER = registerBlockNoItem("barrier",
            BlockBarrier::new);

    // ==================== Banners ====================

    public static final DeferredHolder<Block, Block> BANNER_WHITE = registerBlock("banner_white",
            BlockBannerTC::createWhite);
    public static final DeferredHolder<Block, Block> BANNER_ORANGE = registerBlock("banner_orange",
            BlockBannerTC::createOrange);
    public static final DeferredHolder<Block, Block> BANNER_MAGENTA = registerBlock("banner_magenta",
            BlockBannerTC::createMagenta);
    public static final DeferredHolder<Block, Block> BANNER_LIGHT_BLUE = registerBlock("banner_light_blue",
            BlockBannerTC::createLightBlue);
    public static final DeferredHolder<Block, Block> BANNER_YELLOW = registerBlock("banner_yellow",
            BlockBannerTC::createYellow);
    public static final DeferredHolder<Block, Block> BANNER_LIME = registerBlock("banner_lime",
            BlockBannerTC::createLime);
    public static final DeferredHolder<Block, Block> BANNER_PINK = registerBlock("banner_pink",
            BlockBannerTC::createPink);
    public static final DeferredHolder<Block, Block> BANNER_GRAY = registerBlock("banner_gray",
            BlockBannerTC::createGray);
    public static final DeferredHolder<Block, Block> BANNER_LIGHT_GRAY = registerBlock("banner_light_gray",
            BlockBannerTC::createLightGray);
    public static final DeferredHolder<Block, Block> BANNER_CYAN = registerBlock("banner_cyan",
            BlockBannerTC::createCyan);
    public static final DeferredHolder<Block, Block> BANNER_PURPLE = registerBlock("banner_purple",
            BlockBannerTC::createPurple);
    public static final DeferredHolder<Block, Block> BANNER_BLUE = registerBlock("banner_blue",
            BlockBannerTC::createBlue);
    public static final DeferredHolder<Block, Block> BANNER_BROWN = registerBlock("banner_brown",
            BlockBannerTC::createBrown);
    public static final DeferredHolder<Block, Block> BANNER_GREEN = registerBlock("banner_green",
            BlockBannerTC::createGreen);
    public static final DeferredHolder<Block, Block> BANNER_RED = registerBlock("banner_red",
            BlockBannerTC::createRed);
    public static final DeferredHolder<Block, Block> BANNER_BLACK = registerBlock("banner_black",
            BlockBannerTC::createBlack);
    public static final DeferredHolder<Block, Block> BANNER_CRIMSON_CULT = registerBlock("banner_crimson_cult",
            BlockBannerTC::createCrimsonCult);

    // ==================== Helper Methods ====================

    /**
     * Register a block and its corresponding item.
     */
    private static <T extends Block> DeferredHolder<Block, T> registerBlock(String name, Supplier<T> block) {
        DeferredHolder<Block, T> blockObj = BLOCKS.register(name, block);
        registerBlockItem(name, blockObj);
        return blockObj;
    }

    /**
     * Register a block without a corresponding item.
     * Used for effect blocks, holes, and other non-obtainable blocks.
     */
    private static <T extends Block> DeferredHolder<Block, T> registerBlockNoItem(String name, Supplier<T> block) {
        return BLOCKS.register(name, block);
    }

    /**
     * Register a BlockItem for a block.
     */
    private static <T extends Block> void registerBlockItem(String name, DeferredHolder<Block, T> block) {
        BLOCK_ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }
}







