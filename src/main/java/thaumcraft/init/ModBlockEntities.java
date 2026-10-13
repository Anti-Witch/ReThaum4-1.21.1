package thaumcraft.init;

import net.minecraft.core.registries.Registries;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import thaumcraft.Thaumcraft;
import thaumcraft.common.tiles.crafting.*;
// import thaumcraft.common.tiles.crafting.TileFocalManipulator;
import thaumcraft.common.tiles.crafting.TileGolemBuilder;
import thaumcraft.common.tiles.crafting.TileThaumatorium;
import thaumcraft.common.tiles.devices.*;
import thaumcraft.common.tiles.devices.TileLevitator;
import thaumcraft.common.tiles.devices.TileRechargePedestal;
import thaumcraft.common.tiles.devices.TileFluxScrubber;
import thaumcraft.common.tiles.devices.TileInfernalFurnace;
import thaumcraft.common.tiles.devices.TileSpa;
import thaumcraft.common.tiles.devices.TileVisRelay;
import thaumcraft.common.tiles.devices.TileWaterJug;
import thaumcraft.common.tiles.essentia.*;
import thaumcraft.common.tiles.essentia.TileCentrifuge;
import thaumcraft.common.tiles.essentia.TileEssentiaReservoir;
import thaumcraft.common.tiles.essentia.TileJarBrain;
import thaumcraft.common.tiles.essentia.TileJarVoid;
import thaumcraft.common.tiles.TileJarNode;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.common.tiles.misc.TileVisNodeImpl;
import thaumcraft.common.tiles.crafting.*;
import thaumcraft.common.tiles.devices.*;
import thaumcraft.common.tiles.essentia.*;
import thaumcraft.common.tiles.misc.TileBanner;
import thaumcraft.common.tiles.misc.TileBarrierStone;
import thaumcraft.common.tiles.misc.TileHole;
import thaumcraft.common.tiles.misc.TileWarded;

/**
 * Registry for all Thaumcraft block entities (tile entities).
 * Uses DeferredRegister for 1.20.1 Forge.
 */
public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Thaumcraft.MODID);

    // ==================== Essentia ====================

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileJar>> JAR =
            BLOCK_ENTITIES.register("jar",
                    () -> BlockEntityType.Builder.of(TileJar::new,
                            ModBlocks.JAR_NORMAL.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileJarNode>> JAR_NODE =
            BLOCK_ENTITIES.register("jar_node",
                    () -> BlockEntityType.Builder.of(TileJarNode::new,
                            ModBlocks.JAR_NORMAL.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileJarVoid>> JAR_VOID =
            BLOCK_ENTITIES.register("jar_void",
                    () -> BlockEntityType.Builder.of(TileJarVoid::new,
                            ModBlocks.JAR_VOID.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileJarBrain>> JAR_BRAIN =
            BLOCK_ENTITIES.register("jar_brain",
                    () -> BlockEntityType.Builder.of(TileJarBrain::new,
                            ModBlocks.JAR_BRAIN.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileAlembic>> ALEMBIC =
            BLOCK_ENTITIES.register("alembic",
                    () -> BlockEntityType.Builder.of(TileAlembic::new,
                            ModBlocks.ALEMBIC.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileSmelter>> SMELTER =
            BLOCK_ENTITIES.register("smelter",
                    () -> BlockEntityType.Builder.of(TileSmelter::new,
                            ModBlocks.SMELTER.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileTube>> TUBE =
            BLOCK_ENTITIES.register("tube",
                    () -> BlockEntityType.Builder.of(TileTube::new,
                            ModBlocks.TUBE_NORMAL.get(),
                            ModBlocks.TUBE_RESTRICTED.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileTubeFilter>> TUBE_FILTER =
            BLOCK_ENTITIES.register("tube_filter",
                    () -> BlockEntityType.Builder.of(TileTubeFilter::new,
                            ModBlocks.TUBE_FILTER.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileTubeValve>> TUBE_VALVE =
            BLOCK_ENTITIES.register("tube_valve",
                    () -> BlockEntityType.Builder.of(TileTubeValve::new,
                            ModBlocks.TUBE_VALVE.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileTubeBuffer>> TUBE_BUFFER =
            BLOCK_ENTITIES.register("tube_buffer",
                    () -> BlockEntityType.Builder.of(TileTubeBuffer::new,
                            ModBlocks.TUBE_BUFFER.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileTubeOneway>> TUBE_ONEWAY =
            BLOCK_ENTITIES.register("tube_oneway",
                    () -> BlockEntityType.Builder.of(TileTubeOneway::new,
                            ModBlocks.TUBE_ONEWAY.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileTubeRestrict>> TUBE_RESTRICT =
            BLOCK_ENTITIES.register("tube_restrict",
                    () -> BlockEntityType.Builder.of(TileTubeRestrict::new,
                            ModBlocks.TUBE_RESTRICTED.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileCentrifuge>> CENTRIFUGE =
            BLOCK_ENTITIES.register("centrifuge",
                    () -> BlockEntityType.Builder.of(TileCentrifuge::new,
                            ModBlocks.CENTRIFUGE.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEssentiaReservoir>> ESSENTIA_RESERVOIR =
            BLOCK_ENTITIES.register("essentia_reservoir",
                    () -> BlockEntityType.Builder.of(TileEssentiaReservoir::new,
                            ModBlocks.ESSENTIA_RESERVOIR.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEssentiaInput>> ESSENTIA_INPUT =
            BLOCK_ENTITIES.register("essentia_input",
                    () -> BlockEntityType.Builder.of(TileEssentiaInput::new,
                            ModBlocks.ESSENTIA_INPUT.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEssentiaOutput>> ESSENTIA_OUTPUT =
            BLOCK_ENTITIES.register("essentia_output",
                    () -> BlockEntityType.Builder.of(TileEssentiaOutput::new,
                            ModBlocks.ESSENTIA_OUTPUT.get()
                    ).build(null));

    // ==================== Crafting ====================

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TilePedestal>> PEDESTAL =
            BLOCK_ENTITIES.register("pedestal",
                    () -> BlockEntityType.Builder.of(TilePedestal::new,
                            ModBlocks.PEDESTAL_ARCANE.get(),
                            ModBlocks.PEDESTAL_ANCIENT.get(),
                            ModBlocks.PEDESTAL_ELDRITCH.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileCrucible>> CRUCIBLE =
            BLOCK_ENTITIES.register("crucible",
                    () -> BlockEntityType.Builder.of(TileCrucible::new,
                            ModBlocks.CRUCIBLE.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileArcaneWorkbench>> ARCANE_WORKBENCH =
            BLOCK_ENTITIES.register("arcane_workbench",
                    () -> BlockEntityType.Builder.of(TileArcaneWorkbench::new,
                            ModBlocks.ARCANE_WORKBENCH.get()
                    ).build(null));


    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileResearchTable>> RESEARCH_TABLE =
            BLOCK_ENTITIES.register("research_table",
                    () -> BlockEntityType.Builder.of(TileResearchTable::new,
                            ModBlocks.RESEARCH_TABLE.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileDeconstructionTable>> DECONSTRUCTION_TABLE =
            BLOCK_ENTITIES.register("deconstruction_table",
                    () -> BlockEntityType.Builder.of(TileDeconstructionTable::new,
                            ModBlocks.DECONSTRUCTION_TABLE.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileInfusionMatrix>> INFUSION_MATRIX =
            BLOCK_ENTITIES.register("infusion_matrix",
                    () -> BlockEntityType.Builder.of(TileInfusionMatrix::new,
                            ModBlocks.INFUSION_MATRIX.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileFocalManipulator>> FOCAL_MANIPULATOR =
            BLOCK_ENTITIES.register("focal_manipulator",
                    () -> BlockEntityType.Builder.of(TileFocalManipulator::new,
                            ModBlocks.FOCAL_MANIPULATOR.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileThaumatorium>> THAUMATORIUM =
            BLOCK_ENTITIES.register("thaumatorium",
                    () -> BlockEntityType.Builder.of(TileThaumatorium::new,
                            ModBlocks.THAUMATORIUM.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileThaumatoriumTop>> THAUMATORIUM_TOP =
            BLOCK_ENTITIES.register("thaumatorium_top",
                    () -> BlockEntityType.Builder.of(TileThaumatoriumTop::new,
                            ModBlocks.THAUMATORIUM_TOP.get()
                    ).build(null));

    // ==================== Devices ====================

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileBellows>> BELLOWS =
            BLOCK_ENTITIES.register("bellows",
                    () -> BlockEntityType.Builder.of(TileBellows::new,
                            ModBlocks.BELLOWS.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileHungryChest>> HUNGRY_CHEST =
            BLOCK_ENTITIES.register("hungry_chest",
                    () -> BlockEntityType.Builder.of(TileHungryChest::new,
                            ModBlocks.HUNGRY_CHEST.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileLampArcane>> LAMP_ARCANE =
            BLOCK_ENTITIES.register("lamp_arcane",
                    () -> BlockEntityType.Builder.of(TileLampArcane::new,
                            ModBlocks.LAMP_ARCANE.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileLampGrowth>> LAMP_GROWTH =
            BLOCK_ENTITIES.register("lamp_growth",
                    () -> BlockEntityType.Builder.of(TileLampGrowth::new,
                            ModBlocks.LAMP_GROWTH.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileLampFertility>> LAMP_FERTILITY =
            BLOCK_ENTITIES.register("lamp_fertility",
                    () -> BlockEntityType.Builder.of(TileLampFertility::new,
                            ModBlocks.LAMP_FERTILITY.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileMirror>> MIRROR_ITEM =
            BLOCK_ENTITIES.register("mirror_item",
                    () -> BlockEntityType.Builder.of(TileMirror::new,
                            ModBlocks.MIRROR_ITEM.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileStabilizer>> STABILIZER =
            BLOCK_ENTITIES.register("stabilizer",
                    () -> BlockEntityType.Builder.of(TileStabilizer::new,
                            ModBlocks.STABILIZER.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileMirrorEssentia>> MIRROR_ESSENTIA =
            BLOCK_ENTITIES.register("mirror_essentia",
                    () -> BlockEntityType.Builder.of(TileMirrorEssentia::new,
                            ModBlocks.MIRROR_ESSENTIA.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileArcaneEar>> ARCANE_EAR =
            BLOCK_ENTITIES.register("arcane_ear",
                    () -> BlockEntityType.Builder.of(TileArcaneEar::new,
                            ModBlocks.ARCANE_EAR.get(),
                            ModBlocks.ARCANE_EAR_TOGGLE.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileRedstoneRelay>> REDSTONE_RELAY =
            BLOCK_ENTITIES.register("redstone_relay",
                    () -> BlockEntityType.Builder.of(TileRedstoneRelay::new,
                            ModBlocks.REDSTONE_RELAY.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileInfernalFurnace>> INFERNAL_FURNACE =
            BLOCK_ENTITIES.register("infernal_furnace",
                    () -> BlockEntityType.Builder.of(TileInfernalFurnace::new,
                            ModBlocks.INFERNAL_FURNACE.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileSpa>> SPA =
            BLOCK_ENTITIES.register("spa",
                    () -> BlockEntityType.Builder.of(TileSpa::new,
                            ModBlocks.SPA.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileFluxScrubber>> FLUX_SCRUBBER =
            BLOCK_ENTITIES.register("flux_scrubber",
                    () -> BlockEntityType.Builder.of(TileFluxScrubber::new,
                            ModBlocks.FLUX_SCRUBBER.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileVisRelay>> VIS_RELAY =
            BLOCK_ENTITIES.register("vis_relay",
                    () -> BlockEntityType.Builder.of(TileVisRelay::new,
                            ModBlocks.VIS_RELAY.get()
                    ).build(null));

    // ==================== Golem Crafting ====================

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileGolemBuilder>> GOLEM_BUILDER =
            BLOCK_ENTITIES.register("golem_builder",
                    () -> BlockEntityType.Builder.of(TileGolemBuilder::new,
                            ModBlocks.GOLEM_BUILDER.get()
                    ).build(null));

    // ==================== Misc ====================

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileHole>> HOLE =
            BLOCK_ENTITIES.register("hole",
                    () -> BlockEntityType.Builder.of(TileHole::new,
                            ModBlocks.HOLE.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileBarrierStone>> BARRIER_STONE =
            BLOCK_ENTITIES.register("barrier_stone",
                    () -> BlockEntityType.Builder.of(TileBarrierStone::new,
                            ModBlocks.PAVING_STONE_BARRIER.get()
                    ).build(null));

    // ==================== New Devices ====================

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileLevitator>> LEVITATOR =
            BLOCK_ENTITIES.register("levitator",
                    () -> BlockEntityType.Builder.of(TileLevitator::new,
                            ModBlocks.LEVITATOR.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileRechargePedestal>> RECHARGE_PEDESTAL =
            BLOCK_ENTITIES.register("recharge_pedestal",
                    () -> BlockEntityType.Builder.of(TileRechargePedestal::new,
                            ModBlocks.RECHARGE_PEDESTAL.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileWaterJug>> WATER_JUG =
            BLOCK_ENTITIES.register("water_jug",
                    () -> BlockEntityType.Builder.of(TileWaterJug::new,
                            ModBlocks.EVERFULL_URN.get()
                    ).build(null));

    // ==================== Banners ====================

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileBanner>> BANNER =
            BLOCK_ENTITIES.register("banner",
                    () -> BlockEntityType.Builder.of(TileBanner::new,
                            ModBlocks.BANNER_WHITE.get(),
                            ModBlocks.BANNER_ORANGE.get(),
                            ModBlocks.BANNER_MAGENTA.get(),
                            ModBlocks.BANNER_LIGHT_BLUE.get(),
                            ModBlocks.BANNER_YELLOW.get(),
                            ModBlocks.BANNER_LIME.get(),
                            ModBlocks.BANNER_PINK.get(),
                            ModBlocks.BANNER_GRAY.get(),
                            ModBlocks.BANNER_LIGHT_GRAY.get(),
                            ModBlocks.BANNER_CYAN.get(),
                            ModBlocks.BANNER_PURPLE.get(),
                            ModBlocks.BANNER_BLUE.get(),
                            ModBlocks.BANNER_BROWN.get(),
                            ModBlocks.BANNER_GREEN.get(),
                            ModBlocks.BANNER_RED.get(),
                            ModBlocks.BANNER_BLACK.get(),
                            ModBlocks.BANNER_CRIMSON_CULT.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileWarded>> WARDED =
            BLOCK_ENTITIES.register("warded",
                    () -> BlockEntityType.Builder.of(TileWarded::new,
                            ModBlocks.ARCANE_STONE.get(),
                            ModBlocks.ARCANE_STONE_BRICK.get(),
                            ModBlocks.ANCIENT_STONE.get(),
                            ModBlocks.ANCIENT_STONE_TILE.get(),
                            ModBlocks.ANCIENT_STONE_ROCK.get(),
                            ModBlocks.ANCIENT_STONE_GLYPHED.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileNode>> NODE =
            BLOCK_ENTITIES.register("node",
                    () -> BlockEntityType.Builder.of(TileNode::new,
                            ModBlocks.NODE.get(),
                            ModBlocks.CHARGED_OBSIDIAN_TOTEM.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileVisNodeImpl>> VIS_NODE =
            BLOCK_ENTITIES.register("vis_node",
                    () -> BlockEntityType.Builder.of(TileVisNodeImpl::new,
                            ModBlocks.VIS_NODE.get()
                    ).build(null));

    // ==================== Decoration ====================

    /**
     * Candles: one shared block entity type for all 16 dye colours. The colour
     * itself is read from the block instance by CandleRenderer.
     */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileCandle>> CANDLE =
            BLOCK_ENTITIES.register("candle",
                    () -> BlockEntityType.Builder.of(TileCandle::new,
                            ModBlocks.CANDLE_WHITE.get(),
                            ModBlocks.CANDLE_ORANGE.get(),
                            ModBlocks.CANDLE_MAGENTA.get(),
                            ModBlocks.CANDLE_LIGHT_BLUE.get(),
                            ModBlocks.CANDLE_YELLOW.get(),
                            ModBlocks.CANDLE_LIME.get(),
                            ModBlocks.CANDLE_PINK.get(),
                            ModBlocks.CANDLE_GRAY.get(),
                            ModBlocks.CANDLE_LIGHT_GRAY.get(),
                            ModBlocks.CANDLE_CYAN.get(),
                            ModBlocks.CANDLE_PURPLE.get(),
                            ModBlocks.CANDLE_BLUE.get(),
                            ModBlocks.CANDLE_BROWN.get(),
                            ModBlocks.CANDLE_GREEN.get(),
                            ModBlocks.CANDLE_RED.get(),
                            ModBlocks.CANDLE_BLACK.get()
                    ).build(null));
}






