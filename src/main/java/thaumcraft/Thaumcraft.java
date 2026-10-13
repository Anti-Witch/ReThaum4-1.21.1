package thaumcraft;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import org.slf4j.Logger;
import thaumcraft.init.ModBlocks;
import thaumcraft.init.ModItems;
import thaumcraft.init.ModArmorMaterials;
import thaumcraft.init.ModCreativeTabs;
import thaumcraft.init.ModEntities;
import thaumcraft.init.ModEffects;
import thaumcraft.init.ModSounds;
import thaumcraft.init.ModBlockEntities;
import thaumcraft.init.ModMenuTypes;
import thaumcraft.init.ModRecipeTypes;
import thaumcraft.init.ModRecipeSerializers;
import thaumcraft.init.ModDataComponents;
import thaumcraft.init.ModFeatures;
import thaumcraft.init.ModStructures;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.items.casters.FocusInit;
import thaumcraft.common.golems.GolemProperties;
import thaumcraft.common.golems.seals.SealHandler;
import thaumcraft.common.lib.InternalMethodHandler;
import thaumcraft.common.config.ConfigResearch;
import thaumcraft.common.config.ConfigAspects;
import thaumcraft.common.config.ConfigMultiblocks;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.client.lib.events.KeyHandler;

/**
 * Thaumcraft - A mod about discovering the arcane and harnessing the power of magic.
 * Originally created by Azanor.
 * Ported to NeoForge 1.21.1 from the 1.20.1 Forge port and 1.12.2 decompiled source.
 */
@Mod(Thaumcraft.MODID)
public class Thaumcraft {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "thaumcraft";
    public static final String MODNAME = "Thaumcraft 4 Reforged";
    public static final String VERSION = "4.2.3.5";
    
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    
    // Singleton instance
    private static Thaumcraft instance;
    
    public Thaumcraft(IEventBus modEventBus) {
        instance = this;

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register Deferred Registers to the mod event bus
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.BLOCK_ITEMS.register(modEventBus);
        ModArmorMaterials.ARMOR_MATERIALS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModEffects.MOB_EFFECTS.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenuTypes.MENU_TYPES.register(modEventBus);
ModRecipeTypes.RECIPE_TYPES.register(modEventBus);
ModRecipeSerializers.RECIPE_SERIALIZERS.register(modEventBus);
ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModFeatures.FEATURES.register(modEventBus);
        ModStructures.STRUCTURE_TYPES.register(modEventBus);
        thaumcraft.api.capabilities.ThaumcraftCapabilities.ATTACHMENT_TYPES.register(modEventBus);

        // Register Curios slot types (no-op if Curios not installed)
        thaumcraft.common.lib.compat.CuriosCompat.registerSlotTypes(modEventBus);

        // Register ourselves for server and other game events we are interested in
        NeoForge.EVENT_BUS.register(this);

        // Register our mod's config specs so that NeoForge can create and load the config files.
        // ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ThaumcraftConfig.SPEC);
    }
    
    public static Thaumcraft getInstance() {
        return instance;
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("Thaumcraft common setup");
        
        // Initialize the internal API handler - this enables all ThaumcraftApi methods
        ThaumcraftApi.internalMethods = new InternalMethodHandler();
        LOGGER.info("Initialized ThaumcraftApi internal methods");
        
        // Initialize network system (registration is handled via @SubscribeEvent on PacketHandler)

        // Perform setup that must happen after all registry events
        event.enqueueWork(() -> {
            // Initialize focus system
            FocusInit.registerFoci();
            LOGGER.info("Registered {} focus elements", FocusInit.getAllFocusKeys().length);
            
            // Initialize TC4-style wand rods and caps
            thaumcraft.common.config.ConfigWands.init();
            LOGGER.info("Registered {} wand rods and {} wand caps",
                    thaumcraft.api.wands.WandRod.rods.size(),
                    thaumcraft.api.wands.WandCap.caps.size());
            
            // Initialize golem parts (materials, heads, arms, legs, addons)
            GolemProperties.registerDefaultParts();
            LOGGER.info("Registered golem parts");
            
            // Initialize golem seals
            SealHandler.registerDefaultSeals();
            
            // Initialize research system (categories, scannables, theorycraft)
            ConfigResearch.init();
            
            // Initialize aspect registry (item/block -> aspects mapping)
            ConfigAspects.init();
            
            // Initialize multiblock dust triggers (salis mundus recipes)
            ConfigMultiblocks.init();

            // Initialize wand right-click triggers (bookshelf -> Thaumonomicon,
            // warded jar -> jarring a node). Was never called before, so both
            // TC4 interactions did nothing.
            thaumcraft.api.wands.WandTriggerRegistry.init();
            
            // Initialize recipe system
            // ConfigRecipes.init();
            
            LOGGER.info("Thaumcraft setup complete!");
        });
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Parse research JSON files (must be done when server starts so data packs are loaded)
        ConfigResearch.postInit();
        
        // Register commands
        // CommandThaumcraft.register(event.getServer().getCommands().getDispatcher());
        LOGGER.info("Thaumcraft server starting");
    }

    // Entity attribute registration
    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void registerEntityAttributes(EntityAttributeCreationEvent event) {
            // Monster entities
            event.put(ModEntities.WISP.get(), 
                    thaumcraft.common.entities.monster.EntityWisp.createAttributes().build());
            event.put(ModEntities.FIRE_BAT.get(), 
                    thaumcraft.common.entities.monster.EntityFireBat.createAttributes().build());
            event.put(ModEntities.BRAINY_ZOMBIE.get(), 
                    thaumcraft.common.entities.monster.EntityBrainyZombie.createAttributes().build());
            event.put(ModEntities.MIND_SPIDER.get(), 
                    thaumcraft.common.entities.monster.EntityMindSpider.createAttributes().build());
            event.put(ModEntities.THAUMIC_SLIME.get(), 
                    thaumcraft.common.entities.monster.EntityThaumicSlime.createAttributes().build());
            event.put(ModEntities.GIANT_BRAINY_ZOMBIE.get(), 
                    thaumcraft.common.entities.monster.EntityGiantBrainyZombie.createAttributes().build());
            event.put(ModEntities.INHABITED_ZOMBIE.get(), 
                    thaumcraft.common.entities.monster.EntityInhabitedZombie.createAttributes().build());
            
            event.put(ModEntities.ELDRITCH_CRAB.get(), 
                    thaumcraft.common.entities.monster.EntityEldritchCrab.createAttributes().build());
            event.put(ModEntities.SPELL_BAT.get(), 
                    thaumcraft.common.entities.monster.EntitySpellBat.createAttributes().build());
            
            // Tainted entities
            event.put(ModEntities.TAINT_SWARM.get(), 
                    thaumcraft.common.entities.monster.tainted.EntityTaintSwarm.createAttributes().build());
            event.put(ModEntities.TAINTACLE.get(), 
                    thaumcraft.common.entities.monster.tainted.EntityTaintacle.createAttributes().build());
            event.put(ModEntities.TAINTACLE_SMALL.get(), 
                    thaumcraft.common.entities.monster.tainted.EntityTaintacleSmall.createAttributes().build());
            event.put(ModEntities.TAINT_SEED.get(), 
                    thaumcraft.common.entities.monster.tainted.EntityTaintSeed.createAttributes().build());
            event.put(ModEntities.TAINT_SEED_PRIME.get(), 
                    thaumcraft.common.entities.monster.tainted.EntityTaintSeedPrime.createAttributes().build());
            
            // Eldritch entities
            event.put(ModEntities.ELDRITCH_GUARDIAN.get(), 
                    thaumcraft.common.entities.monster.EntityEldritchGuardian.createAttributes().build());
            
            // Cult entities
            event.put(ModEntities.CULTIST.get(), 
                    thaumcraft.common.entities.monster.cult.EntityCultist.createAttributes().build());
            event.put(ModEntities.CULTIST_KNIGHT.get(), 
                    thaumcraft.common.entities.monster.cult.EntityCultistKnight.createAttributes().build());
            event.put(ModEntities.CULTIST_CLERIC.get(), 
                    thaumcraft.common.entities.monster.cult.EntityCultistCleric.createAttributes().build());
            event.put(ModEntities.CULTIST_PORTAL_LESSER.get(), 
                    thaumcraft.common.entities.monster.cult.EntityCultistPortalLesser.createAttributes().build());
            
            // Boss entities
            event.put(ModEntities.CULTIST_LEADER.get(), 
                    thaumcraft.common.entities.monster.boss.EntityCultistLeader.createAttributes().build());
            event.put(ModEntities.TAINTACLE_GIANT.get(), 
                    thaumcraft.common.entities.monster.boss.EntityTaintacleGiant.createAttributes().build());
            event.put(ModEntities.CULTIST_PORTAL_GREATER.get(), 
                    thaumcraft.common.entities.monster.boss.EntityCultistPortalGreater.createAttributes().build());
            event.put(ModEntities.ELDRITCH_GOLEM.get(), 
                    thaumcraft.common.entities.monster.boss.EntityEldritchGolem.createAttributes().build());
            event.put(ModEntities.ELDRITCH_WARDEN.get(), 
                    thaumcraft.common.entities.monster.boss.EntityEldritchWarden.createAttributes().build());
            
            // Pech
            event.put(ModEntities.PECH.get(), 
                    thaumcraft.common.entities.monster.EntityPech.createAttributes().build());
            
            // Construct entities
            event.put(ModEntities.ARCANE_BORE.get(), 
                    thaumcraft.common.entities.construct.EntityArcaneBore.createAttributes().build());
            
            // Golem
            event.put(ModEntities.THAUMCRAFT_GOLEM.get(), 
                    thaumcraft.common.golems.EntityThaumcraftGolem.createAttributes().build());
            
            LOGGER.info("Registered Thaumcraft entity attributes");
        }
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("Thaumcraft client setup");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
            
            // Parse research JSON on client too (needed for Thaumonomicon GUI)
            ConfigResearch.postInit();

            // Only the magical leaves (greatwood: biome foliage colour, silverwood: the fixed
            // TC4 tint 0x8899AA) and the ambient grass block get a tint - see
            // onRegisterBlockColors/onRegisterItemColors below. Saplings and the other
            // cross plants must stay untinted.

            // Register entity renderers and menu screens
            event.enqueueWork(() -> {
                // Entity renderers
                net.minecraft.client.renderer.entity.EntityRenderers.register(
                    ModEntities.THAUMCRAFT_GOLEM.get(),
                    thaumcraft.client.renderers.entity.GolemRenderer::new
                );
                net.minecraft.client.renderer.entity.EntityRenderers.register(
                    ModEntities.ARCANE_BORE.get(),
                    thaumcraft.client.renderers.entity.ArcaneBoreRenderer::new
                );
                net.minecraft.client.renderer.entity.EntityRenderers.register(
                    ModEntities.PECH.get(),
                    thaumcraft.client.renderers.entity.PechRenderer::new
                );
                net.minecraft.client.renderer.entity.EntityRenderers.register(
                    ModEntities.WISP.get(),
                    thaumcraft.client.renderers.entity.WispRenderer::new
                );
                net.minecraft.client.renderer.entity.EntityRenderers.register(
                    ModEntities.FLUX_RIFT.get(),
                    thaumcraft.client.renderers.entity.FluxRiftRenderer::new
                );
                net.minecraft.client.renderer.entity.EntityRenderers.register(
                    ModEntities.BRAINY_ZOMBIE.get(),
                    thaumcraft.client.renderers.entity.BrainyZombieRenderer::new
                );
                net.minecraft.client.renderer.entity.EntityRenderers.register(
                    ModEntities.GIANT_BRAINY_ZOMBIE.get(),
                    thaumcraft.client.renderers.entity.BrainyZombieRenderer::new
                );
                net.minecraft.client.renderer.entity.EntityRenderers.register(
                    ModEntities.THAUMIC_SLIME.get(),
                    thaumcraft.client.renderers.entity.ThaumicSlimeRenderer::new
                );
                net.minecraft.client.renderer.entity.EntityRenderers.register(
                    ModEntities.MIND_SPIDER.get(),
                    thaumcraft.client.renderers.entity.MindSpiderRenderer::new
                );
                net.minecraft.client.renderer.entity.EntityRenderers.register(
                    ModEntities.FIRE_BAT.get(),
                    thaumcraft.client.renderers.entity.FireBatRenderer::new
                );
                // Cultists
                net.minecraft.client.renderer.entity.EntityRenderers.register(
                    ModEntities.CULTIST.get(),
                    thaumcraft.client.renderers.entity.CultistRenderer::new
                );
                net.minecraft.client.renderer.entity.EntityRenderers.register(
                    ModEntities.CULTIST_KNIGHT.get(),
                    thaumcraft.client.renderers.entity.CultistRenderer::new
                );
                net.minecraft.client.renderer.entity.EntityRenderers.register(
                    ModEntities.CULTIST_CLERIC.get(),
                    thaumcraft.client.renderers.entity.CultistRenderer::new
                );
                // Tainted entities
                net.minecraft.client.renderer.entity.EntityRenderers.register(
                    ModEntities.TAINT_SWARM.get(),
                    thaumcraft.client.renderers.entity.TaintSwarmRenderer::new
                );
                net.minecraft.client.renderer.entity.EntityRenderers.register(
                    ModEntities.TAINTACLE.get(),
                    thaumcraft.client.renderers.entity.TaintacleRenderer::new
                );
                net.minecraft.client.renderer.entity.EntityRenderers.register(
                    ModEntities.TAINTACLE_SMALL.get(),
                    thaumcraft.client.renderers.entity.TaintacleRenderer::new
                );
                net.minecraft.client.renderer.entity.EntityRenderers.register(
                    ModEntities.TAINTACLE_GIANT.get(),
                    thaumcraft.client.renderers.entity.TaintacleRenderer::new
                );
                net.minecraft.client.renderer.entity.EntityRenderers.register(
                    ModEntities.TAINT_SEED.get(),
                    thaumcraft.client.renderers.entity.TaintSeedRenderer::new
                );
                net.minecraft.client.renderer.entity.EntityRenderers.register(
                    ModEntities.TAINT_SEED_PRIME.get(),
                    thaumcraft.client.renderers.entity.TaintSeedRenderer::new
                );
                // Eldritch entities
                net.minecraft.client.renderer.entity.EntityRenderers.register(
                    ModEntities.ELDRITCH_CRAB.get(),
                    thaumcraft.client.renderers.entity.EldritchCrabRenderer::new
                );
                net.minecraft.client.renderer.entity.EntityRenderers.register(
                    ModEntities.ELDRITCH_GUARDIAN.get(),
                    thaumcraft.client.renderers.entity.EldritchGuardianRenderer::new
                );
                net.minecraft.client.renderer.entity.EntityRenderers.register(
                    ModEntities.ELDRITCH_GOLEM.get(),
                    thaumcraft.client.renderers.entity.EldritchGolemRenderer::new
                );
                // SpellBat
                net.minecraft.client.renderer.entity.EntityRenderers.register(
                    ModEntities.SPELL_BAT.get(),
                    thaumcraft.client.renderers.entity.SpellBatRenderer::new
                );
                // InhabitedZombie
                net.minecraft.client.renderer.entity.EntityRenderers.register(
                    ModEntities.INHABITED_ZOMBIE.get(),
                    thaumcraft.client.renderers.entity.InhabitedZombieRenderer::new
                );
                // Boss entities
                net.minecraft.client.renderer.entity.EntityRenderers.register(
                    ModEntities.CULTIST_LEADER.get(),
                    thaumcraft.client.renderers.entity.CultistLeaderRenderer::new
                );
                // Cultist portals
                net.minecraft.client.renderer.entity.EntityRenderers.register(
                    ModEntities.CULTIST_PORTAL_LESSER.get(),
                    thaumcraft.client.renderers.entity.CultistPortalRenderer::new
                );
                net.minecraft.client.renderer.entity.EntityRenderers.register(
                    ModEntities.CULTIST_PORTAL_GREATER.get(),
                    thaumcraft.client.renderers.entity.CultistPortalGreaterRenderer::new
                );
                // Projectiles - using generic renderer
                net.minecraft.client.renderer.entity.EntityRenderers.register(
                    ModEntities.GOLEM_ORB.get(),
                    ctx -> thaumcraft.client.renderers.entity.ThaumcraftProjectileRenderer.Factory.orb(ctx, 0x8844FF)
                );
                net.minecraft.client.renderer.entity.EntityRenderers.register(
                    ModEntities.GOLEM_DART.get(),
                    ctx -> thaumcraft.client.renderers.entity.ThaumcraftProjectileRenderer.Factory.dart(ctx)
                );
                net.minecraft.client.renderer.entity.EntityRenderers.register(
                    ModEntities.ELDRITCH_ORB.get(),
                    thaumcraft.client.renderers.entity.EldritchOrbRenderer::new
                );
                net.minecraft.client.renderer.entity.EntityRenderers.register(
                    ModEntities.HOMING_SHARD.get(),
                    ctx -> thaumcraft.client.renderers.entity.ThaumcraftProjectileRenderer.Factory.magic(ctx, 0x66FFFF)
                );
                // Focus/projectile renderers
                net.minecraft.client.renderer.entity.EntityRenderers.register(
                    ModEntities.FOCUS_MINE.get(),
                    thaumcraft.client.renderers.entity.FocusMineRenderer::new
                );
                net.minecraft.client.renderer.entity.EntityRenderers.register(
                    ModEntities.FOCUS_CLOUD.get(),
                    thaumcraft.client.renderers.entity.FocusCloudRenderer::new
                );
                net.minecraft.client.renderer.entity.EntityRenderers.register(
                    ModEntities.RIFT_BLAST.get(),
                    thaumcraft.client.renderers.entity.RiftBlastRenderer::new
                );
                // Falling taint
                net.minecraft.client.renderer.entity.EntityRenderers.register(
                    ModEntities.FALLING_TAINT.get(),
                    thaumcraft.client.renderers.entity.FallingTaintRenderer::new
                );
                // Special/Following items - glowing magical item entities
                net.minecraft.client.renderer.entity.EntityRenderers.register(
                    ModEntities.SPECIAL_ITEM.get(),
                    thaumcraft.client.renderers.entity.SpecialItemRenderer::new
                );
                net.minecraft.client.renderer.entity.EntityRenderers.register(
                    ModEntities.FOLLOWING_ITEM.get(),
                    thaumcraft.client.renderers.entity.SpecialItemRenderer::new
                );
                // Invisible projectiles (effects come from particles)
                net.minecraft.client.renderer.entity.EntityRenderers.register(
                    ModEntities.FOCUS_PROJECTILE.get(),
                    thaumcraft.client.renderers.entity.NoProjectileRenderer::new
                );
                net.minecraft.client.renderer.entity.EntityRenderers.register(
                    ModEntities.ALUMENTUM.get(),
                    thaumcraft.client.renderers.entity.NoProjectileRenderer::new
                );
                // Bottle Taint - uses item rendering (thrown item texture)
                net.minecraft.client.renderer.entity.EntityRenderers.register(
                    ModEntities.BOTTLE_TAINT.get(),
                    thaumcraft.client.renderers.entity.BottleTaintRenderer::new
                );
                // Eldritch Warden - boss version of Eldritch Guardian
                net.minecraft.client.renderer.entity.EntityRenderers.register(
                    ModEntities.ELDRITCH_WARDEN.get(),
                    thaumcraft.client.renderers.entity.EldritchWardenRenderer::new
                );
                LOGGER.info("Registered Thaumcraft entity renderers");

                // Wand model predicates (TC4-style rod+cap model selection)
                thaumcraft.client.WandModelPredicates.register();
                LOGGER.info("Registered wand model predicates");

            });
            
            // Register block entity renderers
            // ProxyTESR.setupTESR();
            
            // Register key bindings
            // KeyHandler.registerKeyBindings();
            
            // Register color handlers
            // ColorHandler.registerColourHandlers();
        }

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(ModMenuTypes.GOLEM_BUILDER.get(), thaumcraft.client.gui.screens.GolemBuilderScreen::new);
            event.register(ModMenuTypes.ARCANE_WORKBENCH.get(), thaumcraft.client.gui.screens.ArcaneWorkbenchScreen::new);
            event.register(ModMenuTypes.THAUMATORIUM.get(), thaumcraft.client.gui.screens.ThaumatoriumScreen::new);
            event.register(ModMenuTypes.SMELTER.get(), thaumcraft.client.gui.screens.SmelterScreen::new);
            event.register(ModMenuTypes.RESEARCH_TABLE.get(), thaumcraft.client.gui.screens.ResearchTableScreen::new);
            event.register(ModMenuTypes.DECONSTRUCTION_TABLE.get(), thaumcraft.client.gui.screens.DeconstructionTableScreen::new);
            event.register(ModMenuTypes.FOCAL_MANIPULATOR.get(), thaumcraft.client.gui.screens.FocalManipulatorScreen::new);
            event.register(ModMenuTypes.FOCUS_POUCH.get(), thaumcraft.client.gui.screens.FocusPouchScreen::new);
            event.register(ModMenuTypes.HAND_MIRROR.get(), thaumcraft.client.gui.screens.HandMirrorScreen::new);
            event.register(ModMenuTypes.SPA.get(), thaumcraft.client.gui.screens.SpaScreen::new);
            event.register(ModMenuTypes.ARCANE_BORE.get(), thaumcraft.client.gui.screens.ArcaneBoreScreen::new);
            event.register(ModMenuTypes.PECH_TRADING.get(), thaumcraft.client.gui.screens.PechScreen::new);
            event.register(ModMenuTypes.HUNGRY_CHEST.get(), thaumcraft.client.gui.screens.HungryChestScreen::new);
            event.register(ModMenuTypes.LOGISTICS.get(), thaumcraft.client.gui.screens.LogisticsScreen::new);
            event.register(ModMenuTypes.SEAL.get(), thaumcraft.client.gui.screens.SealScreen::new);
        }

        @SubscribeEvent
        public static void onRegisterLayerDefinitions(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions event) {
            // Register entity model layers
            event.registerLayerDefinition(
                thaumcraft.client.models.entity.GolemModel.LAYER_LOCATION,
                thaumcraft.client.models.entity.GolemModel::createBodyLayer
            );
            event.registerLayerDefinition(
                thaumcraft.client.models.entity.ArcaneBoreModel.LAYER_LOCATION,
                thaumcraft.client.models.entity.ArcaneBoreModel::createBodyLayer
            );
            event.registerLayerDefinition(
                thaumcraft.client.models.entity.PechModel.LAYER_LOCATION,
                thaumcraft.client.models.entity.PechModel::createBodyLayer
            );
            event.registerLayerDefinition(
                thaumcraft.client.models.entity.TaintacleModel.LAYER_LOCATION,
                thaumcraft.client.models.entity.TaintacleModel::createBodyLayer
            );
            event.registerLayerDefinition(
                thaumcraft.client.models.entity.TaintSeedModel.LAYER_LOCATION,
                thaumcraft.client.models.entity.TaintSeedModel::createBodyLayer
            );
            event.registerLayerDefinition(
                thaumcraft.client.models.entity.EldritchGolemModel.LAYER_LOCATION,
                thaumcraft.client.models.entity.EldritchGolemModel::createBodyLayer
            );
            event.registerLayerDefinition(
                thaumcraft.client.models.entity.GrapplerModel.LAYER_LOCATION,
                thaumcraft.client.models.entity.GrapplerModel::createBodyLayer
            );
            
            // Register block entity model layers
            event.registerLayerDefinition(
                thaumcraft.client.models.block.CentrifugeModel.LAYER_LOCATION,
                thaumcraft.client.models.block.CentrifugeModel::createBodyLayer
            );
            event.registerLayerDefinition(
                thaumcraft.client.models.block.BellowsModel.LAYER_LOCATION,
                thaumcraft.client.models.block.BellowsModel::createBodyLayer
            );
            event.registerLayerDefinition(
                thaumcraft.client.models.block.TubeValveModel.LAYER_LOCATION,
                thaumcraft.client.models.block.TubeValveModel::createBodyLayer
            );
            event.registerLayerDefinition(
                thaumcraft.client.models.block.BrainModel.LAYER_LOCATION,
                thaumcraft.client.models.block.BrainModel::createBodyLayer
            );
            event.registerLayerDefinition(
                thaumcraft.client.models.block.BannerModel.LAYER_LOCATION,
                thaumcraft.client.models.block.BannerModel::createBodyLayer
            );
            LOGGER.info("Registered Thaumcraft model layers");
        }
        
        @SubscribeEvent
        public static void onRegisterRenderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
            // Register block entity renderers
            
            // Jar renderers
            event.registerBlockEntityRenderer(
                ModBlockEntities.JAR.get(),
                thaumcraft.client.renderers.tile.JarRenderer::new
            );
            event.registerBlockEntityRenderer(
                ModBlockEntities.JAR_VOID.get(),
                thaumcraft.client.renderers.tile.JarRenderer::new
            );
            
            // Pedestal renderers
            event.registerBlockEntityRenderer(
                ModBlockEntities.PEDESTAL.get(),
                thaumcraft.client.renderers.tile.PedestalRenderer::new
            );
            event.registerBlockEntityRenderer(
                ModBlockEntities.RECHARGE_PEDESTAL.get(),
                thaumcraft.client.renderers.tile.RechargePedestalRenderer::new
            );
            
            // Crafting device renderers
            event.registerBlockEntityRenderer(
                ModBlockEntities.CRUCIBLE.get(),
                thaumcraft.client.renderers.tile.CrucibleRenderer::new
            );
            event.registerBlockEntityRenderer(
                ModBlockEntities.INFUSION_MATRIX.get(),
                thaumcraft.client.renderers.tile.InfusionMatrixRenderer::new
            );
            event.registerBlockEntityRenderer(
                ModBlockEntities.RESEARCH_TABLE.get(),
                thaumcraft.client.renderers.tile.ResearchTableRenderer::new
            );
            event.registerBlockEntityRenderer(
                ModBlockEntities.DECONSTRUCTION_TABLE.get(),
                thaumcraft.client.renderers.tile.DeconstructionTableRenderer::new
            );
            event.registerBlockEntityRenderer(
                ModBlockEntities.NODE.get(),
                thaumcraft.client.renderers.tile.TileNodeRenderer::new
            );
            
            // Essentia device renderers
            event.registerBlockEntityRenderer(
                ModBlockEntities.CENTRIFUGE.get(),
                thaumcraft.client.renderers.tile.CentrifugeRenderer::new
            );
            event.registerBlockEntityRenderer(
                ModBlockEntities.ALEMBIC.get(),
                thaumcraft.client.renderers.tile.AlembicRenderer::new
            );
            
            // Other device renderers
            event.registerBlockEntityRenderer(
                ModBlockEntities.BELLOWS.get(),
                thaumcraft.client.renderers.tile.BellowsRenderer::new
            );
            event.registerBlockEntityRenderer(
                ModBlockEntities.MIRROR_ITEM.get(),
                thaumcraft.client.renderers.tile.MirrorRenderer::new
            );
            
            // Essentia tube renderers
            event.registerBlockEntityRenderer(
                ModBlockEntities.TUBE_VALVE.get(),
                thaumcraft.client.renderers.tile.TubeValveRenderer::new
            );
            event.registerBlockEntityRenderer(
                ModBlockEntities.TUBE_BUFFER.get(),
                thaumcraft.client.renderers.tile.TubeBufferRenderer::new
            );
            event.registerBlockEntityRenderer(
                ModBlockEntities.TUBE_ONEWAY.get(),
                thaumcraft.client.renderers.tile.TubeOnewayRenderer::new
            );
            
            // Brain in a Jar renderer
            event.registerBlockEntityRenderer(
                ModBlockEntities.JAR_BRAIN.get(),
                thaumcraft.client.renderers.tile.JarBrainRenderer::new
            );
            
            // Banner renderer
            event.registerBlockEntityRenderer(
                ModBlockEntities.BANNER.get(),
                thaumcraft.client.renderers.tile.BannerRenderer::new
            );
            
            // Focal Manipulator renderer
            event.registerBlockEntityRenderer(
                ModBlockEntities.FOCAL_MANIPULATOR.get(),
                thaumcraft.client.renderers.tile.FocalManipulatorRenderer::new
            );
            
            // Thaumatorium renderer
            event.registerBlockEntityRenderer(
                ModBlockEntities.THAUMATORIUM.get(),
                thaumcraft.client.renderers.tile.ThaumatoriumRenderer::new
            );
            
            // Pattern Crafter renderer (TC6-only) removed - not in TC4.2.3.5
            
            // Golem Builder renderer
            event.registerBlockEntityRenderer(
                ModBlockEntities.GOLEM_BUILDER.get(),
                thaumcraft.client.renderers.tile.GolemBuilderRenderer::new
            );
            
            // Hungry Chest renderer
            event.registerBlockEntityRenderer(
                ModBlockEntities.HUNGRY_CHEST.get(),
                thaumcraft.client.renderers.tile.HungryChestRenderer::new
            );
            
            // Node Stabilizer renderer (TC4 TileNodeStabilizerRenderer)
            event.registerBlockEntityRenderer(
                ModBlockEntities.STABILIZER.get(),
                thaumcraft.client.renderers.tile.StabilizerRenderer::new
            );
            
            // Portable Hole renderer
            event.registerBlockEntityRenderer(
                ModBlockEntities.HOLE.get(),
                thaumcraft.client.renderers.tile.HoleRenderer::new
            );
            
            // Dioptra / Void Siphon renderers (TC6-only) removed - not in TC4.2.3.5
            
            // Candle renderer (TC4 BlockCandleRenderer: wax base + 1-5 extra candles + wick)
            event.registerBlockEntityRenderer(
                ModBlockEntities.CANDLE.get(),
                thaumcraft.client.renderers.tile.CandleRenderer::new
            );
            
            // ==================== Entity renderers ====================
            // Bosses
            event.registerEntityRenderer(ModEntities.ELDRITCH_WARDEN.get(),
                    thaumcraft.client.renderers.entity.EldritchWardenRenderer::new);
            event.registerEntityRenderer(ModEntities.ELDRITCH_GOLEM.get(),
                    thaumcraft.client.renderers.entity.EldritchGolemRenderer::new);
            event.registerEntityRenderer(ModEntities.CULTIST_LEADER.get(),
                    thaumcraft.client.renderers.entity.CultistLeaderRenderer::new);
            event.registerEntityRenderer(ModEntities.TAINTACLE_GIANT.get(),
                    thaumcraft.client.renderers.entity.TaintacleRenderer::new);
            
            // Monsters
            event.registerEntityRenderer(ModEntities.BRAINY_ZOMBIE.get(),
                    thaumcraft.client.renderers.entity.BrainyZombieRenderer::new);
            event.registerEntityRenderer(ModEntities.GIANT_BRAINY_ZOMBIE.get(),
                    thaumcraft.client.renderers.entity.BrainyZombieRenderer::new);
            event.registerEntityRenderer(ModEntities.INHABITED_ZOMBIE.get(),
                    thaumcraft.client.renderers.entity.InhabitedZombieRenderer::new);
            event.registerEntityRenderer(ModEntities.WISP.get(),
                    thaumcraft.client.renderers.entity.WispRenderer::new);
            event.registerEntityRenderer(ModEntities.FIRE_BAT.get(),
                    thaumcraft.client.renderers.entity.FireBatRenderer::new);
            event.registerEntityRenderer(ModEntities.PECH.get(),
                    thaumcraft.client.renderers.entity.PechRenderer::new);
            event.registerEntityRenderer(ModEntities.MIND_SPIDER.get(),
                    thaumcraft.client.renderers.entity.MindSpiderRenderer::new);
            event.registerEntityRenderer(ModEntities.ELDRITCH_GUARDIAN.get(),
                    thaumcraft.client.renderers.entity.EldritchGuardianRenderer::new);
            event.registerEntityRenderer(ModEntities.CULTIST.get(),
                    thaumcraft.client.renderers.entity.CultistRenderer::new);
            event.registerEntityRenderer(ModEntities.CULTIST_KNIGHT.get(),
                    thaumcraft.client.renderers.entity.CultistRenderer::new);
            event.registerEntityRenderer(ModEntities.CULTIST_CLERIC.get(),
                    thaumcraft.client.renderers.entity.CultistRenderer::new);
            event.registerEntityRenderer(ModEntities.TAINTACLE.get(),
                    thaumcraft.client.renderers.entity.TaintacleRenderer::new);
            event.registerEntityRenderer(ModEntities.TAINTACLE_SMALL.get(),
                    thaumcraft.client.renderers.entity.TaintacleRenderer::new);
            event.registerEntityRenderer(ModEntities.TAINT_SWARM.get(),
                    thaumcraft.client.renderers.entity.TaintSwarmRenderer::new);
            event.registerEntityRenderer(ModEntities.TAINT_SEED.get(),
                    thaumcraft.client.renderers.entity.TaintSeedRenderer::new);
            event.registerEntityRenderer(ModEntities.TAINT_SEED_PRIME.get(),
                    thaumcraft.client.renderers.entity.TaintSeedRenderer::new);
            event.registerEntityRenderer(ModEntities.ELDRITCH_CRAB.get(),
                    thaumcraft.client.renderers.entity.EldritchCrabRenderer::new);
            event.registerEntityRenderer(ModEntities.THAUMIC_SLIME.get(),
                    thaumcraft.client.renderers.entity.ThaumicSlimeRenderer::new);
            
            // Constructs
            event.registerEntityRenderer(ModEntities.THAUMCRAFT_GOLEM.get(),
                    thaumcraft.client.renderers.entity.GolemRenderer::new);
            event.registerEntityRenderer(ModEntities.ARCANE_BORE.get(),
                    thaumcraft.client.renderers.entity.ArcaneBoreRenderer::new);
            
            // Projectiles
            event.registerEntityRenderer(ModEntities.FOCUS_CLOUD.get(),
                    thaumcraft.client.renderers.entity.FocusCloudRenderer::new);
            event.registerEntityRenderer(ModEntities.FOCUS_MINE.get(),
                    thaumcraft.client.renderers.entity.FocusMineRenderer::new);
            event.registerEntityRenderer(ModEntities.RIFT_BLAST.get(),
                    thaumcraft.client.renderers.entity.RiftBlastRenderer::new);
            event.registerEntityRenderer(ModEntities.BOTTLE_TAINT.get(),
                    thaumcraft.client.renderers.entity.BottleTaintRenderer::new);
            event.registerEntityRenderer(ModEntities.ELDRITCH_ORB.get(),
                    thaumcraft.client.renderers.entity.EldritchOrbRenderer::new);
            event.registerEntityRenderer(ModEntities.SPELL_BAT.get(),
                    thaumcraft.client.renderers.entity.SpellBatRenderer::new);
            event.registerEntityRenderer(ModEntities.GOLEM_DART.get(),
                    thaumcraft.client.renderers.entity.ThaumcraftProjectileRenderer.Factory::dart);
            event.registerEntityRenderer(ModEntities.GOLEM_ORB.get(),
                    ctx -> thaumcraft.client.renderers.entity.ThaumcraftProjectileRenderer.Factory.orb(ctx, 0xFFFFFF));
            // Invisible/particle-only projectiles
            event.registerEntityRenderer(ModEntities.FOCUS_PROJECTILE.get(),
                    thaumcraft.client.renderers.entity.NoProjectileRenderer::new);
            event.registerEntityRenderer(ModEntities.FIREBALL.get(),
                    thaumcraft.client.renderers.entity.NoProjectileRenderer::new);
            event.registerEntityRenderer(ModEntities.LIGHTNING_ORB.get(),
                    thaumcraft.client.renderers.entity.NoProjectileRenderer::new);
            event.registerEntityRenderer(ModEntities.ASPECT_ORB.get(),
                    thaumcraft.client.renderers.entity.AspectOrbRenderer::new);
            event.registerEntityRenderer(ModEntities.ALUMENTUM.get(),
                    thaumcraft.client.renderers.entity.NoProjectileRenderer::new);
            event.registerEntityRenderer(ModEntities.HOMING_SHARD.get(),
                    thaumcraft.client.renderers.entity.NoProjectileRenderer::new);
            event.registerEntityRenderer(ModEntities.FOLLOWING_ITEM.get(),
                    thaumcraft.client.renderers.entity.NoProjectileRenderer::new);
            
            // Special
            event.registerEntityRenderer(ModEntities.FLUX_RIFT.get(),
                    thaumcraft.client.renderers.entity.FluxRiftRenderer::new);
            event.registerEntityRenderer(ModEntities.CULTIST_PORTAL_LESSER.get(),
                    thaumcraft.client.renderers.entity.CultistPortalRenderer::new);
            event.registerEntityRenderer(ModEntities.CULTIST_PORTAL_GREATER.get(),
                    thaumcraft.client.renderers.entity.CultistPortalGreaterRenderer::new);
            event.registerEntityRenderer(ModEntities.SPECIAL_ITEM.get(),
                    thaumcraft.client.renderers.entity.SpecialItemRenderer::new);
            event.registerEntityRenderer(ModEntities.FALLING_TAINT.get(),
                    thaumcraft.client.renderers.entity.FallingTaintRenderer::new);
            
            LOGGER.info("Registered Thaumcraft block entity renderers");
        }
        
        @SubscribeEvent
        public static void onRegisterKeyMappings(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) {
            // Register Thaumcraft key bindings
            KeyHandler.registerKeyMappings(event);
        }
        
        @SubscribeEvent
        public static void onRegisterItemColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
            // Crystal essence item tint
            event.register(
                (stack, tintIndex) -> {
                    if (stack.getItem() instanceof thaumcraft.common.items.resources.ItemCrystalEssence crystal) {
                        thaumcraft.api.aspects.Aspect aspect = crystal.getAspect(stack);
                        if (aspect != null) return aspect.getColor();
                    }
                    return 0xFFFFFF;
                },
                ModItems.CRYSTAL_ESSENCE.get()
            );

            // Wisp essence - tint by the aspect it carries (TC4 getColorFromItemStack)
            event.register(
                (stack, tintIndex) -> {
                    if (stack.getItem() instanceof thaumcraft.common.items.resources.ItemWispEssence wisp) {
                        thaumcraft.api.aspects.AspectList aspects = wisp.getAspects(stack);
                        if (aspects != null && aspects.size() > 0) {
                            thaumcraft.api.aspects.Aspect aspect = aspects.getAspects()[0];
                            if (aspect != null) return aspect.getColor();
                        }
                    }
                    return 0xFFFFFF;
                },
                ModItems.WISP_ESSENCE.get()
            );

            // Nitor colour blocks - tint the icon by the dye colour (TC4 nitor
            // was one item with colour variants; the port has separate blocks)
            event.register(
                (stack, tint) -> {
                    if (stack.getItem() instanceof net.minecraft.world.item.BlockItem bi
                            && bi.getBlock() instanceof thaumcraft.common.blocks.misc.BlockNitor nitor) {
                        return nitor.getColor().getTextureDiffuseColor();
                    }
                    return 0xFFFFFF;
                },
                ModBlocks.NITOR_WHITE.get().asItem(),
                ModBlocks.NITOR_ORANGE.get().asItem(),
                ModBlocks.NITOR_MAGENTA.get().asItem(),
                ModBlocks.NITOR_LIGHT_BLUE.get().asItem(),
                ModBlocks.NITOR_YELLOW.get().asItem(),
                ModBlocks.NITOR_LIME.get().asItem(),
                ModBlocks.NITOR_PINK.get().asItem(),
                ModBlocks.NITOR_GRAY.get().asItem(),
                ModBlocks.NITOR_LIGHT_GRAY.get().asItem(),
                ModBlocks.NITOR_CYAN.get().asItem(),
                ModBlocks.NITOR_PURPLE.get().asItem(),
                ModBlocks.NITOR_BLUE.get().asItem(),
                ModBlocks.NITOR_BROWN.get().asItem(),
                ModBlocks.NITOR_GREEN.get().asItem(),
                ModBlocks.NITOR_RED.get().asItem(),
                ModBlocks.NITOR_BLACK.get().asItem()
            );

            // Research notes - tinted overlay (layer1) colored by the note's primary aspect
            event.register(
                (stack, tintIndex) -> {
                    if (tintIndex == 1 && stack.getItem() instanceof thaumcraft.common.items.ItemResearchNotes notes) {
                        return notes.getColor(stack);
                    }
                    return 0xFFFFFF;
                },
                ModItems.RESEARCH_NOTES.get()
            );

            // Jar labels - the overlay (layer1) is tinted by the aspect written on
            // the label (TC4 ItemResource pass 1 for damage 13)
            event.register(
                (stack, tintIndex) -> {
                    if (tintIndex == 1 && stack.getItem() instanceof thaumcraft.common.items.consumables.ItemLabel label) {
                        thaumcraft.api.aspects.Aspect aspect = label.getAspect(stack);
                        if (aspect != null) return aspect.getColor();
                    }
                    return 0xFFFFFF;
                },
                ModItems.LABEL_BLANK.get(),
                ModItems.LABEL_FILLED.get()
            );

            // Shards - TC4 colors per aspect (BlockCustomOreItem.colors[meta+1])
            final int[] shardColors = {0xFFFF7E, 0xFF3C01, 0x0090FF, 0x00A000, 0xEECCFF, 0x555577};
            event.register(
                (stack, tintIndex) -> {
                    if (stack.is(ModItems.SHARD_AIR.get())) return shardColors[0];
                    if (stack.is(ModItems.SHARD_FIRE.get())) return shardColors[1];
                    if (stack.is(ModItems.SHARD_WATER.get())) return shardColors[2];
                    if (stack.is(ModItems.SHARD_EARTH.get())) return shardColors[3];
                    if (stack.is(ModItems.SHARD_ORDER.get())) return shardColors[4];
                    if (stack.is(ModItems.SHARD_ENTROPY.get())) return shardColors[5];
                    return 0xFFFFFF;
                },
                ModItems.SHARD_AIR.get(),
                ModItems.SHARD_FIRE.get(),
                ModItems.SHARD_WATER.get(),
                ModItems.SHARD_EARTH.get(),
                ModItems.SHARD_ORDER.get(),
                ModItems.SHARD_ENTROPY.get()
            );
            // Candles - 16 dye colors (item form)
            event.register(
                (stack, tint) -> {
                    if (stack.getItem() instanceof net.minecraft.world.item.BlockItem bi && bi.getBlock() instanceof thaumcraft.common.blocks.basic.BlockCandle bc) {
                        return bc.getColor().getTextColor();
                    }
                    return 0xFFFFFF;
                },
                ModBlocks.CANDLE_WHITE.get().asItem(),
                ModBlocks.CANDLE_ORANGE.get().asItem(),
                ModBlocks.CANDLE_MAGENTA.get().asItem(),
                ModBlocks.CANDLE_LIGHT_BLUE.get().asItem(),
                ModBlocks.CANDLE_YELLOW.get().asItem(),
                ModBlocks.CANDLE_LIME.get().asItem(),
                ModBlocks.CANDLE_PINK.get().asItem(),
                ModBlocks.CANDLE_GRAY.get().asItem(),
                ModBlocks.CANDLE_LIGHT_GRAY.get().asItem(),
                ModBlocks.CANDLE_CYAN.get().asItem(),
                ModBlocks.CANDLE_PURPLE.get().asItem(),
                ModBlocks.CANDLE_BLUE.get().asItem(),
                ModBlocks.CANDLE_BROWN.get().asItem(),
                ModBlocks.CANDLE_GREEN.get().asItem(),
                ModBlocks.CANDLE_RED.get().asItem(),
                ModBlocks.CANDLE_BLACK.get().asItem()
            );
            // Crystals - tint by aspect color (6 aspects, item form)
            event.register(
                (stack, tint) -> {
                    if (stack.getItem() instanceof net.minecraft.world.item.BlockItem bi && bi.getBlock() instanceof thaumcraft.common.blocks.world.ore.BlockCrystalTC c) {
                        thaumcraft.api.aspects.Aspect a = c.getAspect();
                        return a != null ? a.getColor() : 0xFFFFFF;
                    }
                    return 0xFFFFFF;
                },
                ModBlocks.CRYSTAL_AIR.get().asItem(),
                ModBlocks.CRYSTAL_FIRE.get().asItem(),
                ModBlocks.CRYSTAL_WATER.get().asItem(),
                ModBlocks.CRYSTAL_EARTH.get().asItem(),
                ModBlocks.CRYSTAL_ORDER.get().asItem(),
                ModBlocks.CRYSTAL_ENTROPY.get().asItem()
            );
            // Robes (cloth + void robe): icon layer 0 is the greyscale cloth/dye part of the
            // texture and must be tinted with the robe colour (TC4 render pass 0); the second
            // icon layer is the pre-coloured trim/overlay and stays untinted.
            event.register(
                (stack, tint) -> {
                    if (tint != 0) return -1;
                    if (stack.getItem() instanceof thaumcraft.common.items.armor.ItemRobeArmor robe) {
                        return 0xFF000000 | robe.getColor(stack);
                    }
                    if (stack.getItem() instanceof thaumcraft.common.items.armor.ItemVoidRobeArmor robe) {
                        return 0xFF000000 | robe.getColor(stack);
                    }
                    return -1;
                },
                ModItems.CLOTH_CHEST.get(),
                ModItems.CLOTH_LEGS.get(),
                ModItems.CLOTH_BOOTS.get(),
                ModItems.VOID_ROBE_HELM.get(),
                ModItems.VOID_ROBE_CHEST.get(),
                ModItems.VOID_ROBE_LEGS.get()
            );
            // Block items that carry a block tint on their model (tintindex 0): the
            // greatwood/silverwood leaves and the ambient grass block. Without this the
            // grayscale leaf textures / grayscale grass-block top show up untinted (grey)
            // in the inventory and in the hand.
            event.register(
                (stack, tintIndex) -> stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem
                        ? event.getBlockColors().getColor(blockItem.getBlock().defaultBlockState(), null, null, tintIndex)
                        : -1,
                ModBlocks.GREATWOOD_LEAVES.get().asItem(),
                ModBlocks.SILVERWOOD_LEAVES.get().asItem(),
                ModBlocks.GRASS_AMBIENT.get().asItem()
            );
            LOGGER.info("Registered Thaumcraft item color handlers");
        }

        /**
         * Worn (void) robes use ArmorMaterial layers with dyeable=true; NeoForge tints
         * such layers with this extension's default dye colour, which in TC4 is the
         * robe colour stored in the item (0x6A3880 while undyed).
         */
        @SubscribeEvent
        public static void onRegisterClientExtensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
            final net.neoforged.neoforge.client.extensions.common.IClientItemExtensions robeColor =
                    new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
                @Override
                public int getDefaultDyeColor(net.minecraft.world.item.ItemStack stack) {
                    if (stack.getItem() instanceof thaumcraft.common.items.armor.ItemRobeArmor robe) {
                        return 0xFF000000 | robe.getColor(stack);
                    }
                    if (stack.getItem() instanceof thaumcraft.common.items.armor.ItemVoidRobeArmor robe) {
                        return 0xFF000000 | robe.getColor(stack);
                    }
                    return 0xFFFFFFFF;
                }
            };
            event.registerItem(robeColor,
                    ModItems.CLOTH_CHEST, ModItems.CLOTH_LEGS, ModItems.CLOTH_BOOTS,
                    ModItems.VOID_ROBE_HELM, ModItems.VOID_ROBE_CHEST, ModItems.VOID_ROBE_LEGS);
        }

        @SubscribeEvent
        public static void onRegisterBlockColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Block event) {
            // Magical leaves - only these (plus the ambient grass) are biome colourised.
            // TC4 BlockMagicalLeaves#colorMultiplier:
            //   greatwood  -> 3x3 averaged biome foliage colour (ColorizerFoliage of the biome)
            //   silverwood -> fixed tint 8952234 (0x8899AA), it never uses the biome colour
            // No tint at all is registered for the saplings / cinderpearl / shimmerleaf /
            // vishroom - in TC4 those are plain cross models with their own fixed colours.
            event.register(
                (state, level, pos, tint) -> {
                    if (state.is(ModBlocks.SILVERWOOD_LEAVES.get())) {
                        return 0x8899AA;
                    }
                    return level != null && pos != null
                            ? net.minecraft.client.renderer.BiomeColors.getAverageFoliageColor(level, pos)
                            : net.minecraft.world.level.FoliageColor.getDefaultColor();
                },
                ModBlocks.GREATWOOD_LEAVES.get(),
                ModBlocks.SILVERWOOD_LEAVES.get()
            );
            // Magical ambient grass (TC6 BlockGrassAmbient#colour handler:
            // BiomeColorHelper.getGrassColorAtPos / ColorizerGrass.getGrassColor(0.5, 1.0)).
            event.register(
                (state, level, pos, tint) -> level != null && pos != null
                        ? net.minecraft.client.renderer.BiomeColors.getAverageGrassColor(level, pos)
                        : net.minecraft.world.level.GrassColor.getDefaultColor(),
                ModBlocks.GRASS_AMBIENT.get()
            );
            // Candles - 16 dye colors via tint
            event.register(
                (state, level, pos, tint) -> {
                    if (state.getBlock() instanceof thaumcraft.common.blocks.basic.BlockCandle candle) {
                        return candle.getColor().getTextColor();
                    }
                    return 0xFFFFFF;
                },
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
            );
            // Crystals - tint by aspect color (6 aspects)
            event.register(
                (state, level, pos, tint) -> {
                    if (state.getBlock() instanceof thaumcraft.common.blocks.world.ore.BlockCrystalTC c) {
                        thaumcraft.api.aspects.Aspect a = c.getAspect();
                        return a != null ? a.getColor() : 0xFFFFFF;
                    }
                    return 0xFFFFFF;
                },
                ModBlocks.CRYSTAL_AIR.get(),
                ModBlocks.CRYSTAL_FIRE.get(),
                ModBlocks.CRYSTAL_WATER.get(),
                ModBlocks.CRYSTAL_EARTH.get(),
                ModBlocks.CRYSTAL_ORDER.get(),
                ModBlocks.CRYSTAL_ENTROPY.get()
            );
            LOGGER.info("Registered Thaumcraft block color handlers");
        }
    }
}




