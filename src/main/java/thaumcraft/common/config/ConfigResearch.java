package thaumcraft.common.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.entities.IEldritchMob;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.api.research.ResearchEntry;
import thaumcraft.api.research.ScanBlock;
import thaumcraft.api.research.ScanEntity;
import thaumcraft.api.research.ScanItem;
import thaumcraft.api.research.ScanningManager;
import thaumcraft.common.entities.EntityFluxRift;
import thaumcraft.common.entities.construct.EntityOwnedConstruct;
import thaumcraft.common.entities.monster.*;
import thaumcraft.common.entities.monster.boss.EntityThaumcraftBoss;
import thaumcraft.common.entities.monster.cult.EntityCultist;
import thaumcraft.common.entities.monster.tainted.*;
import thaumcraft.common.lib.research.ResearchManager;
import thaumcraft.common.lib.research.ScanGeneric;
// TC6-only: import thaumcraft.common.lib.research.ScanSky;
import thaumcraft.init.ModBlocks;
import thaumcraft.init.ModItems;

/**
 * ConfigResearch - Initializes research categories and scannables.
 * Ported from 1.12.2 to 1.20.1
 */
public class ConfigResearch {
    
    public static final String[] TC_CATEGORIES = {
        "BASICS", "THAUMATURGY", "ALCHEMY", "ARTIFICE", "GOLEMANCY", "ELDRITCH"
    };
    /**
     * Initialize all research-related systems.
     * Called during mod common setup.
     */
    public static void init() {
        Thaumcraft.LOGGER.info("Initializing Thaumcraft research system...");
        
        initCategories();
        initScannables();
        initWarp();
        
        // Register research JSON locations. TC4 research data lives in
        // data/thaumcraft/research/<category>.json (201 TC4 entries).
        for (String cat : TC_CATEGORIES) {
            ThaumcraftApi.registerResearchLocation(ResourceLocation.fromNamespaceAndPath("thaumcraft", "research/" + cat.toLowerCase()));
        }
        
        Thaumcraft.LOGGER.info("Research categories and JSON locations registered");
    }
    
    /**
     * Post-initialization - parse all research JSON files.
     * Called after all mods have registered their research locations.
     */
    public static void postInit() {
        Thaumcraft.LOGGER.info("Parsing research JSON files...");
        ResearchManager.parseAllResearch();
        Thaumcraft.LOGGER.info("Research system initialized with {} categories", 
                ResearchCategories.researchCategories.size());
    }
    
    /**
     * Initialize all research categories.
     * <p>
     * TC4 has exactly six categories - BASICS, THAUMATURGY, ALCHEMY, ARTIFICE,
     * GOLEMANCY and ELDRITCH - and every tab is available from the start (the
     * eldritch tab only appears once ELDRITCHMINOR has been learned, which the
     * browser handles itself). The aspect formulas are kept from the port's
     * knowledge system; TC4 itself did not use per-category formulas.
     */
    private static void initCategories() {
        // BASICS - always visible, no prerequisite research
        ResearchCategories.registerCategory("BASICS", null,
                new AspectList()
                        .add(Aspect.PLANT, 5).add(Aspect.ORDER, 5).add(Aspect.ENTROPY, 5)
                        .add(Aspect.AIR, 5).add(Aspect.FIRE, 5).add(Aspect.EARTH, 3).add(Aspect.WATER, 5),
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/item/thaumonomiconcheat.png"),
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/gui/gui_researchback.png"),
                null);
        
        // THAUMATURGY - always visible
        ResearchCategories.registerCategory("THAUMATURGY", null,
                new AspectList()
                        .add(Aspect.AURA, 20).add(Aspect.MAGIC, 20).add(Aspect.FLUX, 15)
                        .add(Aspect.CRYSTAL, 5).add(Aspect.COLD, 5).add(Aspect.AIR, 5),
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/r_thaumaturgy.png"),
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/gui/gui_researchback.png"),
                null);
        
        // ALCHEMY - always visible
        ResearchCategories.registerCategory("ALCHEMY", null,
                new AspectList()
                        .add(Aspect.ALCHEMY, 30).add(Aspect.FLUX, 10).add(Aspect.MAGIC, 10)
                        .add(Aspect.LIFE, 5).add(Aspect.AVERSION, 5).add(Aspect.DESIRE, 5).add(Aspect.WATER, 5),
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/r_crucible.png"),
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/gui/gui_researchback.png"),
                null);
        
        // ARTIFICE - always visible
        ResearchCategories.registerCategory("ARTIFICE", null,
                new AspectList()
                        .add(Aspect.MECHANISM, 10).add(Aspect.CRAFT, 10).add(Aspect.METAL, 10)
                        .add(Aspect.TOOL, 10).add(Aspect.ENERGY, 10).add(Aspect.LIGHT, 5)
                        .add(Aspect.FLIGHT, 5).add(Aspect.TRAP, 5).add(Aspect.FIRE, 5),
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/r_artifice.png"),
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/gui/gui_researchback.png"),
                null);
        
        // GOLEMANCY - always visible
        ResearchCategories.registerCategory("GOLEMANCY", null,
                new AspectList()
                        .add(Aspect.MAN, 20).add(Aspect.MOTION, 10).add(Aspect.MIND, 10)
                        .add(Aspect.MECHANISM, 10).add(Aspect.EXCHANGE, 5).add(Aspect.SENSES, 5)
                        .add(Aspect.BEAST, 5).add(Aspect.ORDER, 5),
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/r_golemancy.png"),
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/gui/gui_researchback.png"),
                null);
        
        // ELDRITCH - always registered; the browser only shows the tab after ELDRITCHMINOR
        ResearchCategories.registerCategory("ELDRITCH", null,
                new AspectList()
                        .add(Aspect.ELDRITCH, 20).add(Aspect.DARKNESS, 10).add(Aspect.MAGIC, 5)
                        .add(Aspect.MIND, 5).add(Aspect.VOID, 5).add(Aspect.DEATH, 5)
                        .add(Aspect.UNDEAD, 5).add(Aspect.ENTROPY, 5),
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/r_eldritch.png"),
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/gui/gui_researchbackeldritch.png"),
                null);
        
        Thaumcraft.LOGGER.info("Registered {} research categories", TC_CATEGORIES.length);
    }
    
    /**
     * Initialize all scannable objects for the Thaumometer.
     */
    private static void initScannables() {
        // Generic scanner for basic items/blocks
        ScanningManager.addScannableThing(new ScanGeneric());
        
        // TODO: Port ScanEnchantment and ScanPotion when enchantment/effect registries are stable
        // For now, skip dynamic enchantment/potion scanning
        
        // Thaumcraft entities
        ScanningManager.addScannableThing(new ScanEntity("!Wisp", EntityWisp.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!ThaumSlime", EntityThaumicSlime.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!Firebat", EntityFireBat.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!Pech", EntityPech.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!BrainyZombie", EntityBrainyZombie.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!EldritchCrab", EntityEldritchCrab.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!EldritchCrab", EntityInhabitedZombie.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!CrimsonCultist", EntityCultist.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!EldritchGuardian", EntityEldritchGuardian.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!Taintacle", EntityTaintacle.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!TaintSeed", EntityTaintSeed.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!TaintSwarm", EntityTaintSwarm.class, true));
        ScanningManager.addScannableThing(new ScanEntity("f_toomuchflux", EntityFluxRift.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!FluxRift", EntityFluxRift.class, true));
        
        // Vanilla entity types for research triggers
        ScanningManager.addScannableThing(new ScanEntity("f_golem", EntityOwnedConstruct.class, true));
        ScanningManager.addScannableThing(new ScanEntity("f_SPIDER", Spider.class, true));
        ScanningManager.addScannableThing(new ScanEntity("f_BAT", Bat.class, true));
        ScanningManager.addScannableThing(new ScanEntity("f_BAT", EntityFireBat.class, true));
        ScanningManager.addScannableThing(new ScanEntity("f_FLY", Bat.class, true));
        ScanningManager.addScannableThing(new ScanEntity("f_FLY", Parrot.class, true));
        ScanningManager.addScannableThing(new ScanEntity("f_FLY", EntityFireBat.class, true));
        ScanningManager.addScannableThing(new ScanEntity("f_FLY", EntityTaintSwarm.class, true));
        ScanningManager.addScannableThing(new ScanEntity("f_FLY", EntityWisp.class, true));
        ScanningManager.addScannableThing(new ScanEntity("f_FLY", Ghast.class, true));
        ScanningManager.addScannableThing(new ScanEntity("f_FLY", Blaze.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!ORMOB", IEldritchMob.class, true));
        ScanningManager.addScannableThing(new ScanEntity("!ORBOSS", EntityThaumcraftBoss.class, true));
        ScanningManager.addScannableThing(new ScanEntity("f_TELEPORT", EnderMan.class, true));
        ScanningManager.addScannableThing(new ScanEntity("f_BRAIN", EntityBrainyZombie.class, true));
        
        // Thaumcraft blocks - use block registry objects
        ScanningManager.addScannableThing(new ScanBlock("!ORBLOCK1", 
                ModBlocks.ANCIENT_STONE.get(), ModBlocks.ANCIENT_STONE_TILE.get()));
        ScanningManager.addScannableThing(new ScanBlock("!ORBLOCK2", 
                ModBlocks.ELDRITCH_STONE_TILE.get()));
        ScanningManager.addScannableThing(new ScanBlock("!ORBLOCK3", 
                ModBlocks.ANCIENT_STONE_GLYPHED.get()));
        ScanningManager.addScannableThing(new ScanBlock("ORE", 
                ModBlocks.AMBER_ORE.get(), ModBlocks.CINNABAR_ORE.get(),
                ModBlocks.CRYSTAL_AIR.get(), ModBlocks.CRYSTAL_FIRE.get(),
                ModBlocks.CRYSTAL_WATER.get(), ModBlocks.CRYSTAL_EARTH.get(),
                ModBlocks.CRYSTAL_ORDER.get(), ModBlocks.CRYSTAL_ENTROPY.get()));
        ScanningManager.addScannableThing(new ScanBlock("!OREAMBER", ModBlocks.AMBER_ORE.get()));
        ScanningManager.addScannableThing(new ScanBlock("!ORECINNABAR", ModBlocks.CINNABAR_ORE.get()));
        ScanningManager.addScannableThing(new ScanBlock("!ORECRYSTAL",
                ModBlocks.CRYSTAL_AIR.get(), ModBlocks.CRYSTAL_FIRE.get(),
                ModBlocks.CRYSTAL_WATER.get(), ModBlocks.CRYSTAL_EARTH.get(),
                ModBlocks.CRYSTAL_ORDER.get(), ModBlocks.CRYSTAL_ENTROPY.get()));
        
        // Plants
        ScanningManager.addScannableThing(new ScanBlock("PLANTS",
                ModBlocks.GREATWOOD_LOG.get(), ModBlocks.SILVERWOOD_LOG.get(),
                ModBlocks.GREATWOOD_SAPLING.get(), ModBlocks.SILVERWOOD_SAPLING.get(),
                ModBlocks.CINDERPEARL.get(), ModBlocks.SHIMMERLEAF.get(), ModBlocks.VISHROOM.get()));
        ScanningManager.addScannableThing(new ScanBlock("!PLANTWOOD", ModBlocks.GREATWOOD_LOG.get()));
        ScanningManager.addScannableThing(new ScanBlock("!PLANTWOOD", ModBlocks.SILVERWOOD_LOG.get()));
        ScanningManager.addScannableThing(new ScanBlock("!PLANTWOOD", ModBlocks.GREATWOOD_SAPLING.get()));
        ScanningManager.addScannableThing(new ScanBlock("!PLANTWOOD", ModBlocks.SILVERWOOD_SAPLING.get()));
        ScanningManager.addScannableThing(new ScanBlock("!PLANTCINDERPEARL", ModBlocks.CINDERPEARL.get()));
        ScanningManager.addScannableThing(new ScanBlock("!PLANTSHIMMERLEAF", ModBlocks.SHIMMERLEAF.get()));
        ScanningManager.addScannableThing(new ScanBlock("!PLANTVISHROOM", ModBlocks.VISHROOM.get()));
        
        // Special items
        ScanningManager.addScannableThing(new ScanItem("PRIMPEARL", new ItemStack(ModItems.PRIMORDIAL_PEARL.get())));
        ScanningManager.addScannableThing(new ScanItem("!DRAGONBREATH", new ItemStack(Items.DRAGON_BREATH)));
        ScanningManager.addScannableThing(new ScanItem("!TOTEMUNDYING", new ItemStack(Items.TOTEM_OF_UNDYING)));
        ScanningManager.addScannableThing(new ScanItem("f_TELEPORT", new ItemStack(Items.ENDER_PEARL)));
        // TC6-removed: ScanningManager.addScannableThing(new ScanItem("f_BRAIN", new ItemStack(ModItems.BRAIN_NORMAL.get())));
        ScanningManager.addScannableThing(new ScanItem("f_arrow", new ItemStack(Items.ARROW)));
        ScanningManager.addScannableThing(new ScanItem("f_VOIDSEED", new ItemStack(ModItems.VOID_SEED.get())));
        ScanningManager.addScannableThing(new ScanItem("f_MATCLAY", new ItemStack(Items.CLAY_BALL)));
        
        // Portals
        ScanningManager.addScannableThing(new ScanBlock("f_TELEPORT", 
                Blocks.NETHER_PORTAL, Blocks.END_PORTAL, Blocks.END_PORTAL_FRAME));
        ScanningManager.addScannableThing(new ScanBlock("f_DISPENSER", Blocks.DISPENSER));
        ScanningManager.addScannableThing(new ScanItem("f_DISPENSER", new ItemStack(Blocks.DISPENSER)));
        
        // Sky scanning
        // TC6-only: ScanningManager.addScannableThing(new ScanSky());
        
        Thaumcraft.LOGGER.info("Registered {} scannable objects", ScanningManager.getScannableCount());
    }
    
    /**
     * Initialize warp values for items.
     */
    private static void initWarp() {
        // Brain in a Jar gives warp
        ThaumcraftApi.addWarpToItem(new ItemStack(ModBlocks.JAR_BRAIN.get()), 1);
    }
    
    /**
     * Check periodic research triggers for a player.
     * Called from PlayerEvents tick handler.
     */
    public static void checkPeriodicResearch(Player player) {
        if (player.level().isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        
        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
        if (knowledge == null) return;
        
        // Check for dimension-based research
        ResourceLocation dimKey = player.level().dimension().location();
        
        // Nether discovery
        if (!knowledge.isResearchKnown("m_hellandback") && dimKey.getPath().contains("nether")) {
            knowledge.addResearch("m_hellandback");
            knowledge.sync(serverPlayer);
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("\u00A75" + 
                            net.minecraft.network.chat.Component.translatable("got.hellandback").getString()), 
                    true);
        }
        
        // End discovery
        if (!knowledge.isResearchKnown("m_endoftheworld") && dimKey.getPath().contains("end")) {
            knowledge.addResearch("m_endoftheworld");
            knowledge.sync(serverPlayer);
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("\u00A75" + 
                            net.minecraft.network.chat.Component.translatable("got.endoftheworld").getString()), 
                    true);
        }
        
        // Height-based discoveries (TC4 EventHandlerEntity: no category gate)
        // Deep underground
        if (player.getY() < 10 && !knowledge.isResearchKnown("m_deepdown")) {
            knowledge.addResearch("m_deepdown");
            knowledge.sync(serverPlayer);
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("\u00A75" +
                            net.minecraft.network.chat.Component.translatable("got.deepdown").getString()),
                    true);
        }

        // High up
        int worldHeight = player.level().getMaxBuildHeight();
        if (player.getY() > worldHeight * 0.4 && !knowledge.isResearchKnown("m_uphigh")) {
            knowledge.addResearch("m_uphigh");
            knowledge.sync(serverPlayer);
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("\u00A75" +
                            net.minecraft.network.chat.Component.translatable("got.uphigh").getString()),
                    true);
        }
        
        // TC4 discovery triggers (hidden research becomes known when an aspect
        // is discovered, a trigger item is carried or an entity has been
        // scanned - TC4 ResearchItem.setAspectTriggers/setItemTriggers/
        // setEntityTriggers).
        checkDiscoveryTriggers(serverPlayer, knowledge);

        // TODO: Add stat-based discoveries (walking, running, jumping, swimming) when stat tracking is implemented
    }

    /**
     * Make hidden research known once one of its TC4 discovery triggers fires.
     * This mirrors the "@key" marker TC4 sets when a trigger item is picked
     * up, an aspect is discovered or a trigger entity is scanned/killed.
     */
    private static void checkDiscoveryTriggers(ServerPlayer player, IPlayerKnowledge knowledge) {
        for (ResearchCategory category : ResearchCategories.researchCategories.values()) {
            for (ResearchEntry entry : category.research.values()) {
                if (!entry.hasTriggers() || knowledge.isResearchKnown(entry.getKey())) {
                    continue;
                }
                if (!ResearchManager.doesPlayerHaveRequisites(player, entry.getKey())) {
                    continue;
                }
                if (isTriggered(player, knowledge, entry)) {
                    knowledge.addResearch(entry.getKey());
                    knowledge.sync(player);
                }
            }
        }
    }

    private static boolean isTriggered(ServerPlayer player, IPlayerKnowledge knowledge, ResearchEntry entry) {
        if (entry.getAspectTriggers() != null) {
            for (Aspect aspect : entry.getAspectTriggers()) {
                if (aspect != null && knowledge.hasDiscoveredAspect(aspect)) {
                    return true;
                }
            }
        }
        if (entry.getItemTriggers() != null) {
            for (ItemStack trigger : entry.getItemTriggers()) {
                if (!trigger.isEmpty() && player.getInventory().contains(trigger)) {
                    return true;
                }
            }
        }
        if (entry.getEntityTriggers() != null) {
            for (String entity : entry.getEntityTriggers()) {
                if (entity != null && isEntityTriggered(knowledge, entity)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Entity triggers use the TC4 entity class names; the port records scans
     * under "!" keys, with a couple of aliases for entities whose scan key
     * differs from the class name.
     */
    private static boolean isEntityTriggered(IPlayerKnowledge knowledge, String entity) {
        if (knowledge.isResearchKnown("!" + entity)) {
            return true;
        }
        switch (entity) {
            case "Enderman":
                return knowledge.isResearchKnown("f_TELEPORT");
            case "GiantBrainyZombie":
                return knowledge.isResearchKnown("!BrainyZombie");
            default:
                return false;
        }
    }
}




