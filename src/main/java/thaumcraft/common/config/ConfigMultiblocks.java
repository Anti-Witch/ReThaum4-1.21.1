package thaumcraft.common.config;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.crafting.IDustTrigger;
import thaumcraft.api.crafting.Part;
import thaumcraft.common.lib.crafting.DustTriggerMultiblock;
import thaumcraft.common.lib.crafting.DustTriggerSimple;
import thaumcraft.init.ModBlocks;
import thaumcraft.init.ModItems;

/**
 * Registers all multiblock and simple dust triggers.
 * Dust triggers are activated when salis mundus is used on specific blocks.
 * 
 * Ported from 1.12.2 ConfigRecipes.initializeCompoundRecipes()
 */
public class ConfigMultiblocks {
    
    /**
     * Register all dust triggers (called during commonSetup).
     */
    public static void init() {
        // Clear any existing triggers (for reload support)
        IDustTrigger.clearTriggers();
        
        // Register simple triggers
        registerSimpleTriggers();
        
        // Register multiblock triggers
        registerInfernalFurnace();
        registerInfusionAltars();
        registerThaumatorium();
        registerGolemPress();
        
        Thaumcraft.LOGGER.info("Registered {} dust triggers", IDustTrigger.getTriggers().size());
    }
    
    /**
     * Register simple single-block transformations.
     */
    private static void registerSimpleTriggers() {
        // Bookshelf -> Thaumonomicon (after getting first dream)
        IDustTrigger.registerDustTrigger(new DustTriggerSimple(
                "!gotdream", 
                Blocks.BOOKSHELF, 
                new ItemStack(ModItems.THAUMONOMICON.get())
        ));
        
        // Crafting Table -> Arcane Workbench (TC4: available once the player
        // knows the basics)
        IDustTrigger.registerDustTrigger(new DustTriggerSimple(
                "BASICTHAUMATURGY", 
                Blocks.CRAFTING_TABLE, 
                new ItemStack(ModBlocks.ARCANE_WORKBENCH.get())
        ));
        
        // Cauldron -> Crucible (TC4: CRUCIBLE is an auto-unlock root research)
        IDustTrigger.registerDustTrigger(new DustTriggerSimple(
                "CRUCIBLE", 
                Blocks.CAULDRON, 
                new ItemStack(ModBlocks.CRUCIBLE.get())
        ));
    }
    
    /**
     * Register the Infernal Furnace multiblock.
     * 3x3x3 structure with nether bricks, obsidian, iron bars, and lava.
     * 
     * Blueprint layout (Y layers from top to bottom):
     * Layer 0 (top):     NB OB NB
     *                    OB OB OB
     *                    NB OB NB
     * 
     * Layer 1 (middle):  NB OB NB
     *                    OB LA OB  (LA = lava -> infernal furnace)
     *                    NB IB NB  (IB = iron bars -> air)
     * 
     * Layer 2 (bottom):  NB OB NB
     *                    OB -- OB  (-- = nothing)
     *                    NB OB NB
     */
    private static void registerInfernalFurnace() {
        // Part definitions
        Part NB = new Part(Blocks.NETHER_BRICKS, new ItemStack(Blocks.NETHER_BRICKS));
        Part OB = new Part(Blocks.OBSIDIAN, new ItemStack(Blocks.OBSIDIAN));
        Part IB = new Part(Blocks.IRON_BARS, ItemStack.EMPTY); // Transform to air
        Part LA = new Part(Blocks.LAVA, ModBlocks.INFERNAL_FURNACE.get(), true);
        
        // Blueprint [Y][X][Z] - Y=0 is top layer
        Part[][][] blueprint = {
            // Layer 0 (top)
            {
                { NB, OB, NB },
                { OB, null, OB },
                { NB, OB, NB }
            },
            // Layer 1 (middle) - contains lava core and iron bars opening
            {
                { NB, OB, NB },
                { OB, LA, OB },
                { NB, IB, NB }
            },
            // Layer 2 (bottom)
            {
                { NB, OB, NB },
                { OB, OB, OB },
                { NB, OB, NB }
            }
        };
        
        IDustTrigger.registerDustTrigger(new DustTriggerMultiblock("INFERNALFURNACE", blueprint));
        
        // Add to catalog for thaumonomicon display
        ThaumcraftApi.addMultiblockRecipeToCatalog(
                ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "infernalfurnace"),
                new ThaumcraftApi.BluePrint("INFERNALFURNACE", blueprint,
                        new ItemStack(Blocks.NETHER_BRICKS, 12),
                        new ItemStack(Blocks.OBSIDIAN, 12),
                        new ItemStack(Blocks.IRON_BARS),
                        new ItemStack(Items.LAVA_BUCKET)
                )
        );
    }
    
    /**
     * Register the three variants of Infusion Altar.
     * All use the same 3x3x3 layout with different materials.
     * 
     * Blueprint layout (Y layers from top to bottom):
     * Layer 0 (top):     --  --  --
     *                    --  IM  --  (IM = infusion matrix)
     *                    --  --  --
     * 
     * Layer 1 (middle):  ST  --  ST
     *                    --  --  --
     *                    ST  --  ST  (ST = stone -> air)
     * 
     * Layer 2 (bottom):  ST  --  ST  (ST = stone -> pillar)
     *                    --  PD  --  (PD = pedestal)
     *                    ST  --  ST
     */
    private static void registerInfusionAltars() {
        // ===== Normal Infusion Altar =====
        Part IM = new Part(ModBlocks.INFUSION_MATRIX.get(), null);
        Part SNT = new Part(ModBlocks.ARCANE_STONE.get(), ItemStack.EMPTY); // Stone -> air
        Part SNB1 = new Part(ModBlocks.ARCANE_STONE.get(), new ItemStack(ModBlocks.ARCANE_PILLAR.get()));
        Part SNB2 = new Part(ModBlocks.ARCANE_STONE.get(), new ItemStack(ModBlocks.ARCANE_PILLAR.get()));
        Part SNB3 = new Part(ModBlocks.ARCANE_STONE.get(), new ItemStack(ModBlocks.ARCANE_PILLAR.get()));
        Part SNB4 = new Part(ModBlocks.ARCANE_STONE.get(), new ItemStack(ModBlocks.ARCANE_PILLAR.get()));
        Part PN = new Part(ModBlocks.PEDESTAL_ARCANE.get(), null);
        
        Part[][][] normalBlueprint = {
            // Layer 0 (top) - infusion matrix
            {
                { null, null, null },
                { null, IM, null },
                { null, null, null }
            },
            // Layer 1 (middle) - corner stones become air
            {
                { SNT, null, SNT },
                { null, null, null },
                { SNT, null, SNT }
            },
            // Layer 2 (bottom) - corner stones become pillars, center pedestal stays
            {
                { SNB1, null, SNB2 },
                { null, PN, null },
                { SNB3, null, SNB4 }
            }
        };
        
        IDustTrigger.registerDustTrigger(new DustTriggerMultiblock("INFUSION", normalBlueprint));
        ThaumcraftApi.addMultiblockRecipeToCatalog(
                ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "infusionaltar"),
                new ThaumcraftApi.BluePrint("INFUSION", normalBlueprint,
                        new ItemStack(ModBlocks.ARCANE_STONE.get(), 8),
                        new ItemStack(ModBlocks.PEDESTAL_ARCANE.get()),
                        new ItemStack(ModBlocks.INFUSION_MATRIX.get())
                )
        );
        
        // ===== Ancient Infusion Altar =====
        Part SAT = new Part(ModBlocks.ANCIENT_STONE.get(), ItemStack.EMPTY);
        Part SAB1 = new Part(ModBlocks.ANCIENT_STONE.get(), new ItemStack(ModBlocks.ANCIENT_PILLAR.get()));
        Part SAB2 = new Part(ModBlocks.ANCIENT_STONE.get(), new ItemStack(ModBlocks.ANCIENT_PILLAR.get()));
        Part SAB3 = new Part(ModBlocks.ANCIENT_STONE.get(), new ItemStack(ModBlocks.ANCIENT_PILLAR.get()));
        Part SAB4 = new Part(ModBlocks.ANCIENT_STONE.get(), new ItemStack(ModBlocks.ANCIENT_PILLAR.get()));
        Part PA = new Part(ModBlocks.PEDESTAL_ANCIENT.get(), null);
        
        Part[][][] ancientBlueprint = {
            { { null, null, null }, { null, IM, null }, { null, null, null } },
            { { SAT, null, SAT }, { null, null, null }, { SAT, null, SAT } },
            { { SAB1, null, SAB2 }, { null, PA, null }, { SAB3, null, SAB4 } }
        };
        
        IDustTrigger.registerDustTrigger(new DustTriggerMultiblock("INFUSION", ancientBlueprint));
        ThaumcraftApi.addMultiblockRecipeToCatalog(
                ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "infusionaltarancient"),
                new ThaumcraftApi.BluePrint("INFUSION", ancientBlueprint,
                        new ItemStack(ModBlocks.ANCIENT_STONE.get(), 8),
                        new ItemStack(ModBlocks.PEDESTAL_ANCIENT.get()),
                        new ItemStack(ModBlocks.INFUSION_MATRIX.get())
                )
        );
        
        // ===== Eldritch Infusion Altar =====
        Part SET = new Part(ModBlocks.ELDRITCH_STONE_TILE.get(), ItemStack.EMPTY);
        Part SEB1 = new Part(ModBlocks.ELDRITCH_STONE_TILE.get(), new ItemStack(ModBlocks.ELDRITCH_PILLAR.get()));
        Part SEB2 = new Part(ModBlocks.ELDRITCH_STONE_TILE.get(), new ItemStack(ModBlocks.ELDRITCH_PILLAR.get()));
        Part SEB3 = new Part(ModBlocks.ELDRITCH_STONE_TILE.get(), new ItemStack(ModBlocks.ELDRITCH_PILLAR.get()));
        Part SEB4 = new Part(ModBlocks.ELDRITCH_STONE_TILE.get(), new ItemStack(ModBlocks.ELDRITCH_PILLAR.get()));
        Part PE = new Part(ModBlocks.PEDESTAL_ELDRITCH.get(), null);
        
        Part[][][] eldritchBlueprint = {
            { { null, null, null }, { null, IM, null }, { null, null, null } },
            { { SET, null, SET }, { null, null, null }, { SET, null, SET } },
            { { SEB1, null, SEB2 }, { null, PE, null }, { SEB3, null, SEB4 } }
        };
        
        IDustTrigger.registerDustTrigger(new DustTriggerMultiblock("INFUSION", eldritchBlueprint));
        ThaumcraftApi.addMultiblockRecipeToCatalog(
                ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "infusionaltareldritch"),
                new ThaumcraftApi.BluePrint("INFUSION", eldritchBlueprint,
                        new ItemStack(ModBlocks.ELDRITCH_STONE_TILE.get(), 8),
                        new ItemStack(ModBlocks.PEDESTAL_ELDRITCH.get()),
                        new ItemStack(ModBlocks.INFUSION_MATRIX.get())
                )
        );
    }
    
    /**
     * Register the Thaumatorium multiblock.
     * Simple 1x3x1 vertical stack.
     * 
     * Blueprint:
     * Layer 0 (top):    Alchemical Brass Block -> Thaumatorium Top
     * Layer 1 (middle): Alchemical Brass Block -> Thaumatorium
     * Layer 2 (bottom): Crucible (stays)
     */
    private static void registerThaumatorium() {
        Part TH1 = new Part(ModBlocks.ALCHEMICAL_BRASS_BLOCK.get(), ModBlocks.THAUMATORIUM_TOP.get()).setApplyPlayerFacing(true);
        Part TH2 = new Part(ModBlocks.ALCHEMICAL_BRASS_BLOCK.get(), ModBlocks.THAUMATORIUM.get()).setApplyPlayerFacing(true);
        Part TH3 = new Part(ModBlocks.CRUCIBLE.get(), null);
        
        Part[][][] blueprint = {
            { { TH1 } },
            { { TH2 } },
            { { TH3 } }
        };
        
        IDustTrigger.registerDustTrigger(new DustTriggerMultiblock("THAUMATORIUM", blueprint));
        ThaumcraftApi.addMultiblockRecipeToCatalog(
                ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "thaumatorium"),
                new ThaumcraftApi.BluePrint("THAUMATORIUM", blueprint,
                        new ItemStack(ModBlocks.ALCHEMICAL_BRASS_BLOCK.get(), 2),
                        new ItemStack(ModBlocks.CRUCIBLE.get())
                )
        );
    }
    
    /**
     * Register the Golem Press multiblock.
     * 2x2x2 structure for creating golem parts.
     * 
     * Blueprint layout (Y layers from top to bottom):
     * Layer 0 (top):    --  --
     *                   IB  --  (IB = iron bars -> placeholder)
     * 
     * Layer 1 (bottom): CA  AN  (CA = cauldron, AN = anvil)
     *                   PI  TB  (PI = piston facing up -> golem builder, TB = stone table)
     */
    private static void registerGolemPress() {
        Part GP1 = new Part(Blocks.IRON_BARS, new ItemStack(Blocks.IRON_BARS));
        Part GP2 = new Part(Blocks.CAULDRON, new ItemStack(Blocks.CAULDRON));
        Part GP3 = new Part(Blocks.PISTON.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.UP), 
                           ModBlocks.GOLEM_BUILDER.get());
        Part GP4 = new Part(Blocks.ANVIL, new ItemStack(Blocks.ANVIL));
        Part GP5 = new Part(ModBlocks.TABLE_STONE.get(), new ItemStack(ModBlocks.TABLE_STONE.get()));
        
        Part[][][] blueprint = {
            // Layer 0 (top) - just the iron bars
            {
                { null, null },
                { GP1, null }
            },
            // Layer 1 (bottom) - cauldron, anvil, piston->golem builder, stone table
            {
                { GP2, GP4 },
                { GP3, GP5 }
            }
        };
        
        IDustTrigger.registerDustTrigger(new DustTriggerMultiblock("GOLEMBELL", blueprint));
        ThaumcraftApi.addMultiblockRecipeToCatalog(
                ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "golempress"),
                new ThaumcraftApi.BluePrint("GOLEMBELL", 
                        new ItemStack(ModBlocks.GOLEM_BUILDER.get()), // Display item
                        blueprint,
                        new ItemStack(Blocks.IRON_BARS),
                        new ItemStack(Items.CAULDRON),
                        new ItemStack(Blocks.PISTON),
                        new ItemStack(Blocks.ANVIL),
                        new ItemStack(ModBlocks.TABLE_STONE.get())
                )
        );
    }
}




