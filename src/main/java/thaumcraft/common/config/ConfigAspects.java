package thaumcraft.common.config;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.AspectRegistryEvent;
import thaumcraft.init.ModBlocks;
import thaumcraft.init.ModItems;

public class ConfigAspects {

    public static void init() {
        registerItemAspects();
        registerEntityAspects();
        registerThaumcraftItems();
        
        AspectRegistryEvent are = new AspectRegistryEvent();
        NeoForge.EVENT_BUS.post(are);
    }

    
    private static void registerEntityAspects() {
        ThaumcraftApi.registerEntityTag("minecraft:zombie", new AspectList().add(Aspect.UNDEAD, 20).add(Aspect.MAN, 10).add(Aspect.EARTH, 5));
        ThaumcraftApi.registerEntityTag("minecraft:husk", new AspectList().add(Aspect.UNDEAD, 20).add(Aspect.MAN, 10).add(Aspect.FIRE, 5));
        ThaumcraftApi.registerEntityTag("minecraft:giant", new AspectList().add(Aspect.UNDEAD, 25).add(Aspect.MAN, 15).add(Aspect.EARTH, 10));
        ThaumcraftApi.registerEntityTag("minecraft:skeleton", new AspectList().add(Aspect.UNDEAD, 20).add(Aspect.MAN, 5).add(Aspect.EARTH, 5));
        ThaumcraftApi.registerEntityTag("minecraft:wither_skeleton", new AspectList().add(Aspect.UNDEAD, 25).add(Aspect.MAN, 5).add(Aspect.ENTROPY, 10));
        ThaumcraftApi.registerEntityTag("minecraft:creeper", new AspectList().add(Aspect.PLANT, 15).add(Aspect.FIRE, 15));
        // ThaumcraftApi.registerEntityTag("minecraft:creeper", new AspectList().add(Aspect.PLANT, 15).add(Aspect.FIRE, 15).add(Aspect.ENERGY, 15), new ThaumcraftApi.EntityTagsNBT("powered", 1));
        ThaumcraftApi.registerEntityTag("minecraft:horse", new AspectList().add(Aspect.BEAST, 15).add(Aspect.EARTH, 5).add(Aspect.AIR, 5));
        ThaumcraftApi.registerEntityTag("minecraft:donkey", new AspectList().add(Aspect.BEAST, 15).add(Aspect.EARTH, 5).add(Aspect.AIR, 5));
        ThaumcraftApi.registerEntityTag("minecraft:mule", new AspectList().add(Aspect.BEAST, 15).add(Aspect.EARTH, 5).add(Aspect.AIR, 5));
        ThaumcraftApi.registerEntityTag("minecraft:skeleton_horse", new AspectList().add(Aspect.BEAST, 5).add(Aspect.UNDEAD, 10).add(Aspect.EARTH, 5).add(Aspect.AIR, 5));
        ThaumcraftApi.registerEntityTag("minecraft:zombie_horse", new AspectList().add(Aspect.BEAST, 10).add(Aspect.UNDEAD, 5).add(Aspect.EARTH, 5).add(Aspect.AIR, 5));
        ThaumcraftApi.registerEntityTag("minecraft:pig", new AspectList().add(Aspect.BEAST, 10).add(Aspect.EARTH, 10).add(Aspect.DESIRE, 5));
        ThaumcraftApi.registerEntityTag("minecraft:experience_orb", new AspectList().add(Aspect.MIND, 10));
        ThaumcraftApi.registerEntityTag("minecraft:sheep", new AspectList().add(Aspect.BEAST, 10).add(Aspect.EARTH, 10));
        ThaumcraftApi.registerEntityTag("minecraft:cow", new AspectList().add(Aspect.BEAST, 15).add(Aspect.EARTH, 15));
        ThaumcraftApi.registerEntityTag("minecraft:mooshroom", new AspectList().add(Aspect.BEAST, 15).add(Aspect.PLANT, 15).add(Aspect.EARTH, 15));
        ThaumcraftApi.registerEntityTag("minecraft:snow_golem", new AspectList().add(Aspect.COLD, 10).add(Aspect.MAN, 5).add(Aspect.MECHANISM, 5).add(Aspect.MAGIC, 5));
        ThaumcraftApi.registerEntityTag("minecraft:ocelot", new AspectList().add(Aspect.BEAST, 10).add(Aspect.ENTROPY, 10));
        ThaumcraftApi.registerEntityTag("minecraft:chicken", new AspectList().add(Aspect.BEAST, 5).add(Aspect.FLIGHT, 5).add(Aspect.AIR, 5));
        ThaumcraftApi.registerEntityTag("minecraft:squid", new AspectList().add(Aspect.BEAST, 5).add(Aspect.WATER, 10));
        ThaumcraftApi.registerEntityTag("minecraft:wolf", new AspectList().add(Aspect.BEAST, 15).add(Aspect.EARTH, 10).add(Aspect.AVERSION, 5));
        ThaumcraftApi.registerEntityTag("minecraft:bat", new AspectList().add(Aspect.BEAST, 5).add(Aspect.FLIGHT, 5).add(Aspect.DARKNESS, 5));
        ThaumcraftApi.registerEntityTag("minecraft:spider", new AspectList().add(Aspect.BEAST, 10).add(Aspect.ENTROPY, 10).add(Aspect.TRAP, 10));
        ThaumcraftApi.registerEntityTag("minecraft:slime", new AspectList().add(Aspect.LIFE, 10).add(Aspect.WATER, 10).add(Aspect.ALCHEMY, 5));
        ThaumcraftApi.registerEntityTag("minecraft:ghast", new AspectList().add(Aspect.UNDEAD, 15).add(Aspect.FIRE, 15));
        ThaumcraftApi.registerEntityTag("minecraft:zombified_piglin", new AspectList().add(Aspect.UNDEAD, 15).add(Aspect.FIRE, 15).add(Aspect.BEAST, 10));
        ThaumcraftApi.registerEntityTag("minecraft:enderman", new AspectList().add(Aspect.ELDRITCH, 10).add(Aspect.MOTION, 15).add(Aspect.DESIRE, 5));
        ThaumcraftApi.registerEntityTag("minecraft:cave_spider", new AspectList().add(Aspect.BEAST, 5).add(Aspect.DEATH, 10).add(Aspect.TRAP, 10));
        ThaumcraftApi.registerEntityTag("minecraft:silverfish", new AspectList().add(Aspect.BEAST, 5).add(Aspect.EARTH, 10));
        ThaumcraftApi.registerEntityTag("minecraft:blaze", new AspectList().add(Aspect.ELDRITCH, 5).add(Aspect.FIRE, 15).add(Aspect.FLIGHT, 5));
        ThaumcraftApi.registerEntityTag("minecraft:magma_cube", new AspectList().add(Aspect.WATER, 5).add(Aspect.FIRE, 10).add(Aspect.ALCHEMY, 5));
        ThaumcraftApi.registerEntityTag("minecraft:ender_dragon", new AspectList().add(Aspect.ELDRITCH, 50).add(Aspect.BEAST, 30).add(Aspect.ENTROPY, 50).add(Aspect.FLIGHT, 10));
        ThaumcraftApi.registerEntityTag("minecraft:wither", new AspectList().add(Aspect.UNDEAD, 50).add(Aspect.ENTROPY, 25).add(Aspect.FIRE, 25));
        ThaumcraftApi.registerEntityTag("minecraft:witch", new AspectList().add(Aspect.MAN, 15).add(Aspect.MAGIC, 5).add(Aspect.ALCHEMY, 10));
        ThaumcraftApi.registerEntityTag("minecraft:villager", new AspectList().add(Aspect.MAN, 15));
        ThaumcraftApi.registerEntityTag("minecraft:iron_golem", new AspectList().add(Aspect.METAL, 15).add(Aspect.MAN, 5).add(Aspect.MECHANISM, 5).add(Aspect.MAGIC, 5));
        ThaumcraftApi.registerEntityTag("minecraft:end_crystal", new AspectList().add(Aspect.ELDRITCH, 15).add(Aspect.AURA, 15).add(Aspect.LIFE, 15));
        // ThaumcraftApi.registerEntityTag("minecraft:item_frame", new AspectList().add(Aspect.SENSES, 5).add(Aspect.CRAFT, 5));
        // ThaumcraftApi.registerEntityTag("minecraft:painting", new AspectList().add(Aspect.SENSES, 10).add(Aspect.CRAFT, 5));
        ThaumcraftApi.registerEntityTag("minecraft:guardian", new AspectList().add(Aspect.BEAST, 10).add(Aspect.ELDRITCH, 10).add(Aspect.WATER, 10));
        ThaumcraftApi.registerEntityTag("minecraft:elder_guardian", new AspectList().add(Aspect.BEAST, 10).add(Aspect.ELDRITCH, 15).add(Aspect.WATER, 15));
        ThaumcraftApi.registerEntityTag("minecraft:rabbit", new AspectList().add(Aspect.BEAST, 5).add(Aspect.EARTH, 5).add(Aspect.MOTION, 5));
        ThaumcraftApi.registerEntityTag("minecraft:endermite", new AspectList().add(Aspect.BEAST, 5).add(Aspect.ELDRITCH, 5).add(Aspect.MOTION, 5));
        ThaumcraftApi.registerEntityTag("minecraft:polar_bear", new AspectList().add(Aspect.BEAST, 15).add(Aspect.COLD, 10));
        ThaumcraftApi.registerEntityTag("minecraft:shulker", new AspectList().add(Aspect.ELDRITCH, 10).add(Aspect.TRAP, 5).add(Aspect.FLIGHT, 5).add(Aspect.PROTECT, 5));
        ThaumcraftApi.registerEntityTag("minecraft:evoker", new AspectList().add(Aspect.ELDRITCH, 5).add(Aspect.MAGIC, 5).add(Aspect.MAN, 10));
        ThaumcraftApi.registerEntityTag("minecraft:vindicator", new AspectList().add(Aspect.AVERSION, 5).add(Aspect.MAGIC, 5).add(Aspect.MAN, 10));
        ThaumcraftApi.registerEntityTag("minecraft:illusioner", new AspectList().add(Aspect.SENSES, 5).add(Aspect.MAGIC, 5).add(Aspect.MAN, 10));
        ThaumcraftApi.registerEntityTag("minecraft:llama", new AspectList().add(Aspect.BEAST, 15).add(Aspect.WATER, 5));
        ThaumcraftApi.registerEntityTag("minecraft:parrot", new AspectList().add(Aspect.BEAST, 5).add(Aspect.FLIGHT, 5).add(Aspect.SENSES, 5));
        ThaumcraftApi.registerEntityTag("minecraft:stray", new AspectList().add(Aspect.UNDEAD, 20).add(Aspect.MAN, 5).add(Aspect.TRAP, 5));
        ThaumcraftApi.registerEntityTag("minecraft:vex", new AspectList().add(Aspect.ELDRITCH, 5).add(Aspect.FLIGHT, 5).add(Aspect.MAGIC, 5).add(Aspect.MAN, 5));
        
        // Thaumcraft entities
        ThaumcraftApi.registerEntityTag("thaumcraft:flux_rift", new AspectList().add(Aspect.FLUX, 20).add(Aspect.ELDRITCH, 20).add(Aspect.AURA, 20));
        ThaumcraftApi.registerEntityTag("thaumcraft:firebat", new AspectList().add(Aspect.BEAST, 5).add(Aspect.FLIGHT, 5).add(Aspect.FIRE, 10));
        ThaumcraftApi.registerEntityTag("thaumcraft:pech", new AspectList().add(Aspect.MAN, 10).add(Aspect.AURA, 5).add(Aspect.EXCHANGE, 10));
        ThaumcraftApi.registerEntityTag("thaumcraft:thaumic_slime", new AspectList().add(Aspect.LIFE, 5).add(Aspect.WATER, 5).add(Aspect.FLUX, 5).add(Aspect.ALCHEMY, 5));
        ThaumcraftApi.registerEntityTag("thaumcraft:brainy_zombie", new AspectList().add(Aspect.UNDEAD, 20).add(Aspect.MAN, 10).add(Aspect.MIND, 5).add(Aspect.AVERSION, 5));
        ThaumcraftApi.registerEntityTag("thaumcraft:giant_brainy_zombie", new AspectList().add(Aspect.UNDEAD, 25).add(Aspect.MAN, 15).add(Aspect.MIND, 5).add(Aspect.AVERSION, 10));
        ThaumcraftApi.registerEntityTag("thaumcraft:taintacle", new AspectList().add(Aspect.FLUX, 15).add(Aspect.BEAST, 10));
        ThaumcraftApi.registerEntityTag("thaumcraft:taint_seed", new AspectList().add(Aspect.FLUX, 20).add(Aspect.AURA, 10).add(Aspect.PLANT, 5));
        ThaumcraftApi.registerEntityTag("thaumcraft:taint_seed_prime", new AspectList().add(Aspect.FLUX, 25).add(Aspect.AURA, 15).add(Aspect.PLANT, 5));
        ThaumcraftApi.registerEntityTag("thaumcraft:taintacle_small", new AspectList().add(Aspect.FLUX, 5).add(Aspect.BEAST, 5));
        ThaumcraftApi.registerEntityTag("thaumcraft:taint_swarm", new AspectList().add(Aspect.FLUX, 15).add(Aspect.AIR, 5));
        ThaumcraftApi.registerEntityTag("thaumcraft:mind_spider", new AspectList().add(Aspect.FLUX, 5).add(Aspect.FIRE, 5));
        ThaumcraftApi.registerEntityTag("thaumcraft:eldritch_guardian", new AspectList().add(Aspect.ELDRITCH, 20).add(Aspect.DEATH, 20).add(Aspect.UNDEAD, 20));
        ThaumcraftApi.registerEntityTag("thaumcraft:cultist_knight", new AspectList().add(Aspect.ELDRITCH, 5).add(Aspect.MAN, 15).add(Aspect.AVERSION, 5));
        ThaumcraftApi.registerEntityTag("thaumcraft:cultist_cleric", new AspectList().add(Aspect.ELDRITCH, 5).add(Aspect.MAN, 15).add(Aspect.AVERSION, 5));
        ThaumcraftApi.registerEntityTag("thaumcraft:eldritch_crab", new AspectList().add(Aspect.ELDRITCH, 10).add(Aspect.BEAST, 10).add(Aspect.TRAP, 10));
        ThaumcraftApi.registerEntityTag("thaumcraft:inhabited_zombie", new AspectList().add(Aspect.ELDRITCH, 10).add(Aspect.UNDEAD, 10).add(Aspect.MAN, 5));
        ThaumcraftApi.registerEntityTag("thaumcraft:eldritch_warden", new AspectList().add(Aspect.ELDRITCH, 40).add(Aspect.DEATH, 40).add(Aspect.UNDEAD, 40));
        ThaumcraftApi.registerEntityTag("thaumcraft:eldritch_golem", new AspectList().add(Aspect.ELDRITCH, 40).add(Aspect.ENERGY, 40).add(Aspect.MECHANISM, 40));
        ThaumcraftApi.registerEntityTag("thaumcraft:cultist_leader", new AspectList().add(Aspect.ELDRITCH, 40).add(Aspect.AVERSION, 40).add(Aspect.MAN, 40));
        ThaumcraftApi.registerEntityTag("thaumcraft:taintacle_giant", new AspectList().add(Aspect.ELDRITCH, 40).add(Aspect.BEAST, 40).add(Aspect.FLUX, 40));
        ThaumcraftApi.registerEntityTag("thaumcraft:golem", new AspectList().add(Aspect.MECHANISM, 10).add(Aspect.MAN, 10).add(Aspect.MOTION, 10));
        ThaumcraftApi.registerEntityTag("thaumcraft:wisp", new AspectList().add(Aspect.AURA, 10).add(Aspect.FLIGHT, 5));
    }
    
    private static void registerItemAspects() {
        // Basic Ores - Using forge tags for compatibility
        ThaumcraftApi.registerObjectTag("c:ores/lapis", new AspectList().add(Aspect.EARTH, 5).add(Aspect.SENSES, 15));
        ThaumcraftApi.registerObjectTag("c:ores/diamond", new AspectList().add(Aspect.EARTH, 5).add(Aspect.DESIRE, 15).add(Aspect.CRYSTAL, 15));
        ThaumcraftApi.registerObjectTag("c:gems/diamond", new AspectList().add(Aspect.CRYSTAL, 15).add(Aspect.DESIRE, 15));
        ThaumcraftApi.registerObjectTag("c:ores/redstone", new AspectList().add(Aspect.EARTH, 5).add(Aspect.ENERGY, 15));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.REDSTONE_ORE), new AspectList().add(Aspect.EARTH, 5).add(Aspect.ENERGY, 15));
        ThaumcraftApi.registerObjectTag("c:ores/emerald", new AspectList().add(Aspect.EARTH, 5).add(Aspect.DESIRE, 10).add(Aspect.CRYSTAL, 15));
        ThaumcraftApi.registerObjectTag("c:gems/emerald", new AspectList().add(Aspect.CRYSTAL, 15).add(Aspect.DESIRE, 10));
        ThaumcraftApi.registerObjectTag("c:ores/quartz", new AspectList().add(Aspect.EARTH, 5).add(Aspect.CRYSTAL, 10));
        ThaumcraftApi.registerObjectTag("c:gems/quartz", new AspectList().add(Aspect.CRYSTAL, 5));
        ThaumcraftApi.registerObjectTag("c:ores/iron", new AspectList().add(Aspect.EARTH, 5).add(Aspect.METAL, 15));
        ThaumcraftApi.registerObjectTag("c:dusts/iron", new AspectList().add(Aspect.METAL, 15).add(Aspect.ENTROPY, 1));
        ThaumcraftApi.registerObjectTag("c:ingots/iron", new AspectList().add(Aspect.METAL, 15));
        ThaumcraftApi.registerObjectTag("c:ores/gold", new AspectList().add(Aspect.EARTH, 5).add(Aspect.METAL, 10).add(Aspect.DESIRE, 10));
        ThaumcraftApi.registerObjectTag("c:dusts/gold", new AspectList().add(Aspect.METAL, 10).add(Aspect.DESIRE, 10).add(Aspect.ENTROPY, 1));
        ThaumcraftApi.registerObjectTag("c:ingots/gold", new AspectList().add(Aspect.METAL, 10).add(Aspect.DESIRE, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.COAL_ORE), new AspectList().add(Aspect.EARTH, 5).add(Aspect.ENERGY, 15).add(Aspect.FIRE, 15));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.COAL), new AspectList().add(Aspect.ENERGY, 10).add(Aspect.FIRE, 10));
        ThaumcraftApi.registerObjectTag("c:dusts/redstone", new AspectList().add(Aspect.ENERGY, 10));
        ThaumcraftApi.registerObjectTag("c:dusts/glowstone", new AspectList().add(Aspect.SENSES, 5).add(Aspect.LIGHT, 10));
        ThaumcraftApi.registerObjectTag("c:ingots/copper", new AspectList().add(Aspect.METAL, 10).add(Aspect.EXCHANGE, 5));
        ThaumcraftApi.registerObjectTag("c:dusts/copper", new AspectList().add(Aspect.METAL, 10).add(Aspect.ENTROPY, 1).add(Aspect.EXCHANGE, 5));
        ThaumcraftApi.registerObjectTag("c:ores/copper", new AspectList().add(Aspect.METAL, 10).add(Aspect.EARTH, 5).add(Aspect.EXCHANGE, 5));
        
        // Basic Blocks
        ThaumcraftApi.registerObjectTag("c:stones", new AspectList().add(Aspect.EARTH, 5));
        ThaumcraftApi.registerObjectTag("c:cobblestones", new AspectList().add(Aspect.EARTH, 5).add(Aspect.ENTROPY, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.BEDROCK), new AspectList().add(Aspect.VOID, 25).add(Aspect.ENTROPY, 25).add(Aspect.EARTH, 25).add(Aspect.DARKNESS, 25));
        ThaumcraftApi.registerObjectTag("minecraft:dirt", new AspectList().add(Aspect.EARTH, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.PODZOL), new AspectList().add(Aspect.EARTH, 5).add(Aspect.PLANT, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.FARMLAND), new AspectList().add(Aspect.EARTH, 5).add(Aspect.WATER, 2).add(Aspect.ORDER, 2));
        ThaumcraftApi.registerObjectTag("c:sands", new AspectList().add(Aspect.EARTH, 5).add(Aspect.ENTROPY, 5));
        ThaumcraftApi.registerObjectTag("minecraft:grass", new AspectList().add(Aspect.EARTH, 5).add(Aspect.PLANT, 2)); // Not sure if this tag exists
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.GRASS_BLOCK), new AspectList().add(Aspect.EARTH, 5).add(Aspect.PLANT, 2));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.DIRT_PATH), new AspectList().add(Aspect.EARTH, 5).add(Aspect.PLANT, 2).add(Aspect.ORDER, 2));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.END_STONE), new AspectList().add(Aspect.EARTH, 5).add(Aspect.DARKNESS, 5));
        ThaumcraftApi.registerObjectTag("c:gravels", new AspectList().add(Aspect.EARTH, 5).add(Aspect.ENTROPY, 2));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.MYCELIUM), new AspectList().add(Aspect.EARTH, 5).add(Aspect.PLANT, 1).add(Aspect.FLUX, 1));
        
        // Clay
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.CLAY_BALL), new AspectList().add(Aspect.WATER, 5).add(Aspect.EARTH, 5));
        
        // Nether
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.NETHERRACK), new AspectList().add(Aspect.EARTH, 5).add(Aspect.FIRE, 2));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.SOUL_SAND), new AspectList().add(Aspect.EARTH, 3).add(Aspect.TRAP, 1).add(Aspect.SOUL, 3));
        
        // Obsidian
        ThaumcraftApi.registerObjectTag("c:obsidians", new AspectList().add(Aspect.EARTH, 5).add(Aspect.FIRE, 5).add(Aspect.DARKNESS, 5));
        
        // Wood
        ThaumcraftApi.registerObjectTag("minecraft:logs", new AspectList().add(Aspect.PLANT, 20));
        ThaumcraftApi.registerObjectTag("minecraft:saplings", new AspectList().add(Aspect.PLANT, 15).add(Aspect.LIFE, 5));
        ThaumcraftApi.registerObjectTag("minecraft:leaves", new AspectList().add(Aspect.PLANT, 5));
        
        // Other
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.ICE), new AspectList().add(Aspect.COLD, 20));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.PACKED_ICE), new AspectList().add(Aspect.COLD, 15).add(Aspect.ORDER, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.SNOWBALL), new AspectList().add(Aspect.COLD, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.SLIME_BALL), new AspectList().add(Aspect.WATER, 5).add(Aspect.LIFE, 5).add(Aspect.ALCHEMY, 1));
        ThaumcraftApi.registerObjectTag("c:leathers", new AspectList().add(Aspect.BEAST, 5).add(Aspect.PROTECT, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.ROTTEN_FLESH), new AspectList().add(Aspect.MAN, 5).add(Aspect.LIFE, 5).add(Aspect.ENTROPY, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.FEATHER), new AspectList().add(Aspect.FLIGHT, 5).add(Aspect.AIR, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.BONE), new AspectList().add(Aspect.DEATH, 5).add(Aspect.LIFE, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.EGG), new AspectList().add(Aspect.LIFE, 5).add(Aspect.BEAST, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.GUNPOWDER), new AspectList().add(Aspect.FIRE, 10).add(Aspect.ENTROPY, 10).add(Aspect.ALCHEMY, 5));
        
        // Glass
        ThaumcraftApi.registerObjectTag("c:glass_blocks", new AspectList().add(Aspect.CRYSTAL, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.GLASS), new AspectList().add(Aspect.CRYSTAL, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.GLASS_PANE), new AspectList().add(Aspect.CRYSTAL, 1));
        
        // Mossy stone
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.MOSSY_COBBLESTONE), new AspectList().add(Aspect.EARTH, 5).add(Aspect.PLANT, 3).add(Aspect.ENTROPY, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.MOSSY_STONE_BRICKS), new AspectList().add(Aspect.EARTH, 5).add(Aspect.PLANT, 1).add(Aspect.ORDER, 1));
        
        // Plants and Flowers
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.GRASS_BLOCK), new AspectList().add(Aspect.EARTH, 5).add(Aspect.PLANT, 2));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.TALL_GRASS), new AspectList().add(Aspect.PLANT, 5).add(Aspect.AIR, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.FERN), new AspectList().add(Aspect.PLANT, 5).add(Aspect.AIR, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.LARGE_FERN), new AspectList().add(Aspect.PLANT, 5).add(Aspect.AIR, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.LILY_PAD), new AspectList().add(Aspect.PLANT, 5).add(Aspect.WATER, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.DEAD_BUSH), new AspectList().add(Aspect.PLANT, 5).add(Aspect.ENTROPY, 1));
        ThaumcraftApi.registerObjectTag("c:vines", new AspectList().add(Aspect.PLANT, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.VINE), new AspectList().add(Aspect.PLANT, 5));
        
        // Seeds and crops
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.WHEAT_SEEDS), new AspectList().add(Aspect.PLANT, 5).add(Aspect.LIFE, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.MELON_SEEDS), new AspectList().add(Aspect.PLANT, 5).add(Aspect.LIFE, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.PUMPKIN_SEEDS), new AspectList().add(Aspect.PLANT, 5).add(Aspect.LIFE, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.BEETROOT_SEEDS), new AspectList().add(Aspect.PLANT, 5).add(Aspect.LIFE, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.NETHER_WART), new AspectList().add(Aspect.PLANT, 1).add(Aspect.FLUX, 2).add(Aspect.ALCHEMY, 3));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.WHEAT), new AspectList().add(Aspect.PLANT, 5).add(Aspect.LIFE, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.APPLE), new AspectList().add(Aspect.PLANT, 5).add(Aspect.LIFE, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.CARROT), new AspectList().add(Aspect.PLANT, 5).add(Aspect.LIFE, 5).add(Aspect.SENSES, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.POTATO), new AspectList().add(Aspect.PLANT, 5).add(Aspect.LIFE, 5).add(Aspect.EARTH, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.BEETROOT), new AspectList().add(Aspect.PLANT, 5).add(Aspect.LIFE, 5).add(Aspect.DESIRE, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.BAKED_POTATO), new AspectList().add(Aspect.PLANT, 5).add(Aspect.LIFE, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.POISONOUS_POTATO), new AspectList().add(Aspect.PLANT, 5).add(Aspect.DEATH, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.PUMPKIN), new AspectList().add(Aspect.PLANT, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.MELON_SLICE), new AspectList().add(Aspect.PLANT, 1));
        
        // Flowers
        ThaumcraftApi.registerObjectTag("minecraft:small_flowers", new AspectList().add(Aspect.PLANT, 5).add(Aspect.LIFE, 1).add(Aspect.SENSES, 5));
        ThaumcraftApi.registerObjectTag("minecraft:tall_flowers", new AspectList().add(Aspect.PLANT, 5).add(Aspect.LIFE, 1).add(Aspect.SENSES, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.POPPY), new AspectList().add(Aspect.PLANT, 5).add(Aspect.LIFE, 1).add(Aspect.SENSES, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.DANDELION), new AspectList().add(Aspect.PLANT, 5).add(Aspect.LIFE, 1).add(Aspect.SENSES, 5));
        
        // Mushrooms
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.BROWN_MUSHROOM), new AspectList().add(Aspect.PLANT, 5).add(Aspect.DARKNESS, 2).add(Aspect.EARTH, 2));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.RED_MUSHROOM), new AspectList().add(Aspect.PLANT, 5).add(Aspect.DARKNESS, 2).add(Aspect.FIRE, 2));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.BROWN_MUSHROOM_BLOCK), new AspectList().add(Aspect.PLANT, 5).add(Aspect.DARKNESS, 2).add(Aspect.EARTH, 2));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.RED_MUSHROOM_BLOCK), new AspectList().add(Aspect.PLANT, 5).add(Aspect.DARKNESS, 2).add(Aspect.FIRE, 2));
        
        // Cactus and sugar cane
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.CACTUS), new AspectList().add(Aspect.PLANT, 5).add(Aspect.WATER, 5).add(Aspect.AVERSION, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.SUGAR_CANE), new AspectList().add(Aspect.PLANT, 5).add(Aspect.WATER, 3).add(Aspect.AIR, 2));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.SUGAR), new AspectList().add(Aspect.DESIRE, 1).add(Aspect.ENERGY, 1).add(Aspect.WATER, 1));
        
        // Wool
        ThaumcraftApi.registerObjectTag("minecraft:wool", new AspectList().add(Aspect.BEAST, 15).add(Aspect.CRAFT, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.WHITE_WOOL), new AspectList().add(Aspect.BEAST, 15).add(Aspect.CRAFT, 5));
        
        // Food items
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.BREAD), new AspectList().add(Aspect.PLANT, 5).add(Aspect.LIFE, 5).add(Aspect.CRAFT, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.COOKIE), new AspectList().add(Aspect.PLANT, 5).add(Aspect.LIFE, 5).add(Aspect.DESIRE, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.CAKE), new AspectList().add(Aspect.PLANT, 5).add(Aspect.LIFE, 10).add(Aspect.DESIRE, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.PUMPKIN_PIE), new AspectList().add(Aspect.PLANT, 5).add(Aspect.LIFE, 5).add(Aspect.DESIRE, 2));
        
        // Meat
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.COD), new AspectList().add(Aspect.BEAST, 5).add(Aspect.LIFE, 5).add(Aspect.WATER, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.SALMON), new AspectList().add(Aspect.BEAST, 5).add(Aspect.LIFE, 5).add(Aspect.WATER, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.TROPICAL_FISH), new AspectList().add(Aspect.BEAST, 5).add(Aspect.LIFE, 5).add(Aspect.WATER, 5).add(Aspect.SENSES, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.PUFFERFISH), new AspectList().add(Aspect.BEAST, 5).add(Aspect.DEATH, 5).add(Aspect.WATER, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.COOKED_COD), new AspectList().add(Aspect.CRAFT, 1).add(Aspect.BEAST, 5).add(Aspect.LIFE, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.COOKED_SALMON), new AspectList().add(Aspect.CRAFT, 1).add(Aspect.BEAST, 5).add(Aspect.LIFE, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.CHICKEN), new AspectList().add(Aspect.BEAST, 5).add(Aspect.LIFE, 5).add(Aspect.AIR, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.COOKED_CHICKEN), new AspectList().add(Aspect.CRAFT, 1).add(Aspect.BEAST, 5).add(Aspect.LIFE, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.PORKCHOP), new AspectList().add(Aspect.BEAST, 5).add(Aspect.LIFE, 5).add(Aspect.EARTH, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.COOKED_PORKCHOP), new AspectList().add(Aspect.CRAFT, 1).add(Aspect.BEAST, 5).add(Aspect.LIFE, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.BEEF), new AspectList().add(Aspect.BEAST, 5).add(Aspect.LIFE, 5).add(Aspect.EARTH, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.COOKED_BEEF), new AspectList().add(Aspect.CRAFT, 1).add(Aspect.BEAST, 5).add(Aspect.LIFE, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.MUTTON), new AspectList().add(Aspect.BEAST, 5).add(Aspect.LIFE, 5).add(Aspect.EARTH, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.COOKED_MUTTON), new AspectList().add(Aspect.CRAFT, 1).add(Aspect.BEAST, 5).add(Aspect.LIFE, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.RABBIT), new AspectList().add(Aspect.BEAST, 5).add(Aspect.LIFE, 5).add(Aspect.EARTH, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.COOKED_RABBIT), new AspectList().add(Aspect.CRAFT, 1).add(Aspect.BEAST, 5).add(Aspect.LIFE, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.RABBIT_HIDE), new AspectList().add(Aspect.BEAST, 5).add(Aspect.PROTECT, 2));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.RABBIT_FOOT), new AspectList().add(Aspect.BEAST, 5).add(Aspect.PROTECT, 5).add(Aspect.MOTION, 10).add(Aspect.ALCHEMY, 5));
        
        // Monster drops
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.STRING), new AspectList().add(Aspect.BEAST, 5).add(Aspect.CRAFT, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.SPIDER_EYE), new AspectList().add(Aspect.SENSES, 5).add(Aspect.BEAST, 5).add(Aspect.DEATH, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.FERMENTED_SPIDER_EYE), new AspectList().add(Aspect.SENSES, 5).add(Aspect.ALCHEMY, 5).add(Aspect.DEATH, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.BLAZE_ROD), new AspectList().add(Aspect.FIRE, 15).add(Aspect.ENERGY, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.BLAZE_POWDER), new AspectList().add(Aspect.FIRE, 10).add(Aspect.ALCHEMY, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.MAGMA_CREAM), new AspectList().add(Aspect.FIRE, 5).add(Aspect.ALCHEMY, 5).add(Aspect.WATER, 5));
        ThaumcraftApi.registerObjectTag("c:ender_pearls", new AspectList().add(Aspect.ELDRITCH, 10).add(Aspect.MOTION, 15));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.ENDER_PEARL), new AspectList().add(Aspect.ELDRITCH, 10).add(Aspect.MOTION, 15));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.ENDER_EYE), new AspectList().add(Aspect.ELDRITCH, 10).add(Aspect.MOTION, 15).add(Aspect.SENSES, 10).add(Aspect.MAGIC, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.GHAST_TEAR), new AspectList().add(Aspect.UNDEAD, 5).add(Aspect.SOUL, 10).add(Aspect.ALCHEMY, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.PHANTOM_MEMBRANE), new AspectList().add(Aspect.FLIGHT, 10).add(Aspect.UNDEAD, 5).add(Aspect.ALCHEMY, 5));
        
        // Heads/skulls
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.SKELETON_SKULL), new AspectList().add(Aspect.DEATH, 10).add(Aspect.SOUL, 10).add(Aspect.UNDEAD, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.WITHER_SKELETON_SKULL), new AspectList().add(Aspect.DEATH, 10).add(Aspect.SOUL, 10).add(Aspect.UNDEAD, 10).add(Aspect.ENTROPY, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.ZOMBIE_HEAD), new AspectList().add(Aspect.DEATH, 10).add(Aspect.SOUL, 10).add(Aspect.MAN, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.PLAYER_HEAD), new AspectList().add(Aspect.DEATH, 10).add(Aspect.SOUL, 10).add(Aspect.MAN, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.CREEPER_HEAD), new AspectList().add(Aspect.DEATH, 10).add(Aspect.SOUL, 10).add(Aspect.ENTROPY, 5).add(Aspect.FIRE, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.DRAGON_HEAD), new AspectList().add(Aspect.DEATH, 10).add(Aspect.SOUL, 10).add(Aspect.FIRE, 10).add(Aspect.DARKNESS, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.DRAGON_BREATH), new AspectList().add(Aspect.DARKNESS, 10).add(Aspect.ENTROPY, 10).add(Aspect.FIRE, 10).add(Aspect.ALCHEMY, 10));
        
        // Nether star and special items
        ThaumcraftApi.registerObjectTag("c:nether_stars", new AspectList().add(Aspect.ELDRITCH, 10).add(Aspect.MAGIC, 20).add(Aspect.ORDER, 20).add(Aspect.AURA, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.NETHER_STAR), new AspectList().add(Aspect.ELDRITCH, 10).add(Aspect.MAGIC, 20).add(Aspect.ORDER, 20).add(Aspect.AURA, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.TOTEM_OF_UNDYING), new AspectList().add(Aspect.ORDER, 10).add(Aspect.ENTROPY, 10).add(Aspect.LIFE, 25).add(Aspect.UNDEAD, 10));
        
        // End items
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.CHORUS_FLOWER), new AspectList().add(Aspect.ELDRITCH, 5).add(Aspect.SENSES, 5).add(Aspect.PLANT, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.CHORUS_PLANT), new AspectList().add(Aspect.ELDRITCH, 5).add(Aspect.PLANT, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.CHORUS_FRUIT), new AspectList().add(Aspect.ELDRITCH, 5).add(Aspect.SENSES, 5).add(Aspect.PLANT, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.POPPED_CHORUS_FRUIT), new AspectList().add(Aspect.ELDRITCH, 5).add(Aspect.SENSES, 5).add(Aspect.PLANT, 4).add(Aspect.FIRE, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.SHULKER_SHELL), new AspectList().add(Aspect.PROTECT, 10).add(Aspect.ELDRITCH, 5).add(Aspect.BEAST, 5).add(Aspect.VOID, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.ELYTRA), new AspectList().add(Aspect.FLIGHT, 20).add(Aspect.MOTION, 15));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.END_ROD), new AspectList().add(Aspect.FIRE, 1).add(Aspect.LIGHT, 5));
        
        // Dragon egg and portals
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.DRAGON_EGG), new AspectList().add(Aspect.ELDRITCH, 15).add(Aspect.BEAST, 15).add(Aspect.DARKNESS, 15).add(Aspect.MOTION, 15).add(Aspect.MAGIC, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.END_PORTAL_FRAME), new AspectList().add(Aspect.ELDRITCH, 10).add(Aspect.ENERGY, 10).add(Aspect.MOTION, 10).add(Aspect.MAGIC, 5));
        
        // Glowstone and prismarine
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.GLOWSTONE), new AspectList().add(Aspect.SENSES, 5).add(Aspect.LIGHT, 20));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.PRISMARINE_SHARD), new AspectList().add(Aspect.WATER, 5).add(Aspect.EARTH, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.PRISMARINE_CRYSTALS), new AspectList().add(Aspect.WATER, 5).add(Aspect.CRYSTAL, 5).add(Aspect.LIGHT, 5));
        
        // Misc mob drops
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.INK_SAC), new AspectList().add(Aspect.WATER, 2).add(Aspect.BEAST, 2).add(Aspect.DARKNESS, 2));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.GLOW_INK_SAC), new AspectList().add(Aspect.WATER, 2).add(Aspect.BEAST, 2).add(Aspect.LIGHT, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.EXPERIENCE_BOTTLE), new AspectList().add(Aspect.MIND, 20));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.NAME_TAG), new AspectList().add(Aspect.MIND, 10).add(Aspect.BEAST, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.SADDLE), new AspectList().add(Aspect.BEAST, 10).add(Aspect.MOTION, 10).add(Aspect.ORDER, 5));
        
        // Horse armor
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.IRON_HORSE_ARMOR), new AspectList().add(Aspect.METAL, 15).add(Aspect.PROTECT, 10).add(Aspect.BEAST, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.GOLDEN_HORSE_ARMOR), new AspectList().add(Aspect.METAL, 10).add(Aspect.PROTECT, 15).add(Aspect.BEAST, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.DIAMOND_HORSE_ARMOR), new AspectList().add(Aspect.CRYSTAL, 15).add(Aspect.PROTECT, 20).add(Aspect.BEAST, 5));
        
        // Chainmail armor
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.CHAINMAIL_HELMET), new AspectList().add(Aspect.METAL, 42).add(Aspect.PROTECT, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.CHAINMAIL_CHESTPLATE), new AspectList().add(Aspect.METAL, 67).add(Aspect.PROTECT, 15));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.CHAINMAIL_LEGGINGS), new AspectList().add(Aspect.METAL, 58).add(Aspect.PROTECT, 12));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.CHAINMAIL_BOOTS), new AspectList().add(Aspect.METAL, 33).add(Aspect.PROTECT, 8));
        
        // Tools and mechanism
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.FLINT), new AspectList().add(Aspect.EARTH, 5).add(Aspect.TOOL, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.FLINT_AND_STEEL), new AspectList().add(Aspect.FIRE, 10).add(Aspect.TOOL, 5).add(Aspect.METAL, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.COMPASS), new AspectList().add(Aspect.METAL, 5).add(Aspect.SENSES, 10).add(Aspect.MECHANISM, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.CLOCK), new AspectList().add(Aspect.METAL, 5).add(Aspect.SENSES, 5).add(Aspect.MECHANISM, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.FISHING_ROD), new AspectList().add(Aspect.WATER, 10).add(Aspect.TOOL, 5).add(Aspect.BEAST, 3));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.LEAD), new AspectList().add(Aspect.BEAST, 5).add(Aspect.CRAFT, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.SHIELD), new AspectList().add(Aspect.PROTECT, 20).add(Aspect.PLANT, 5).add(Aspect.METAL, 3));
        
        // Containers
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.CHEST), new AspectList().add(Aspect.VOID, 15).add(Aspect.PLANT, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.TRAPPED_CHEST), new AspectList().add(Aspect.VOID, 15).add(Aspect.TRAP, 10).add(Aspect.PLANT, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.ENDER_CHEST), new AspectList().add(Aspect.VOID, 20).add(Aspect.EXCHANGE, 10).add(Aspect.MOTION, 10).add(Aspect.ELDRITCH, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.BUCKET), new AspectList().add(Aspect.VOID, 5).add(Aspect.METAL, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.WATER_BUCKET), new AspectList().add(Aspect.VOID, 5).add(Aspect.METAL, 5).add(Aspect.WATER, 20));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.LAVA_BUCKET), new AspectList().add(Aspect.VOID, 5).add(Aspect.METAL, 5).add(Aspect.FIRE, 15).add(Aspect.EARTH, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.MILK_BUCKET), new AspectList().add(Aspect.VOID, 5).add(Aspect.METAL, 5).add(Aspect.LIFE, 10).add(Aspect.BEAST, 5).add(Aspect.WATER, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.GLASS_BOTTLE), new AspectList().add(Aspect.VOID, 5).add(Aspect.CRYSTAL, 3));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.BOWL), new AspectList().add(Aspect.VOID, 5).add(Aspect.PLANT, 2));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.FLOWER_POT), new AspectList().add(Aspect.VOID, 5).add(Aspect.PLANT, 5).add(Aspect.EARTH, 3));
        
        // Books and paper
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.PAPER), new AspectList().add(Aspect.MIND, 2).add(Aspect.PLANT, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.BOOK), new AspectList().add(Aspect.MIND, 8).add(Aspect.BEAST, 5).add(Aspect.PLANT, 3));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.ENCHANTED_BOOK), new AspectList().add(Aspect.MIND, 8).add(Aspect.MAGIC, 15).add(Aspect.AURA, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.BOOKSHELF), new AspectList().add(Aspect.MIND, 20).add(Aspect.PLANT, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.WRITABLE_BOOK), new AspectList().add(Aspect.MIND, 8).add(Aspect.BEAST, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.WRITTEN_BOOK), new AspectList().add(Aspect.MIND, 15).add(Aspect.BEAST, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.MAP), new AspectList().add(Aspect.MIND, 5).add(Aspect.SENSES, 10));
        
        // Redstone mechanisms
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.LEVER), new AspectList().add(Aspect.MECHANISM, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.STONE_BUTTON), new AspectList().add(Aspect.MECHANISM, 5).add(Aspect.EARTH, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.PISTON), new AspectList().add(Aspect.MECHANISM, 10).add(Aspect.MOTION, 10).add(Aspect.METAL, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.STICKY_PISTON), new AspectList().add(Aspect.MECHANISM, 10).add(Aspect.MOTION, 10).add(Aspect.METAL, 5).add(Aspect.LIFE, 3));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.REPEATER), new AspectList().add(Aspect.MECHANISM, 15).add(Aspect.ENERGY, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.COMPARATOR), new AspectList().add(Aspect.MECHANISM, 15).add(Aspect.ORDER, 5).add(Aspect.SENSES, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.HOPPER), new AspectList().add(Aspect.MECHANISM, 5).add(Aspect.EXCHANGE, 10).add(Aspect.VOID, 5).add(Aspect.METAL, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.DROPPER), new AspectList().add(Aspect.MECHANISM, 5).add(Aspect.EXCHANGE, 10).add(Aspect.VOID, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.DISPENSER), new AspectList().add(Aspect.MECHANISM, 5).add(Aspect.EXCHANGE, 10).add(Aspect.VOID, 5).add(Aspect.AVERSION, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.OBSERVER), new AspectList().add(Aspect.MECHANISM, 5).add(Aspect.SENSES, 15));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.DAYLIGHT_DETECTOR), new AspectList().add(Aspect.SENSES, 10).add(Aspect.LIGHT, 10).add(Aspect.MECHANISM, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.TRIPWIRE_HOOK), new AspectList().add(Aspect.SENSES, 5).add(Aspect.MECHANISM, 5).add(Aspect.TRAP, 5));
        
        // Rails
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.RAIL), new AspectList().add(Aspect.MOTION, 10).add(Aspect.METAL, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.POWERED_RAIL), new AspectList().add(Aspect.MOTION, 10).add(Aspect.METAL, 5).add(Aspect.ENERGY, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.DETECTOR_RAIL), new AspectList().add(Aspect.MOTION, 10).add(Aspect.METAL, 5).add(Aspect.SENSES, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.ACTIVATOR_RAIL), new AspectList().add(Aspect.MOTION, 10).add(Aspect.METAL, 5).add(Aspect.MECHANISM, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.MINECART), new AspectList().add(Aspect.MOTION, 15).add(Aspect.METAL, 10));
        
        // Crafting stations
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.CRAFTING_TABLE), new AspectList().add(Aspect.CRAFT, 20).add(Aspect.PLANT, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.FURNACE), new AspectList().add(Aspect.FIRE, 10).add(Aspect.CRAFT, 5).add(Aspect.EARTH, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.BLAST_FURNACE), new AspectList().add(Aspect.FIRE, 15).add(Aspect.CRAFT, 5).add(Aspect.METAL, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.SMOKER), new AspectList().add(Aspect.FIRE, 15).add(Aspect.CRAFT, 5).add(Aspect.PLANT, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.BREWING_STAND), new AspectList().add(Aspect.CRAFT, 15).add(Aspect.ALCHEMY, 25).add(Aspect.FIRE, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.CAULDRON), new AspectList().add(Aspect.VOID, 10).add(Aspect.ALCHEMY, 15).add(Aspect.METAL, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.ENCHANTING_TABLE), new AspectList().add(Aspect.MAGIC, 25).add(Aspect.CRAFT, 15).add(Aspect.MIND, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.ANVIL), new AspectList().add(Aspect.METAL, 40).add(Aspect.CRAFT, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.GRINDSTONE), new AspectList().add(Aspect.CRAFT, 10).add(Aspect.TOOL, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.SMITHING_TABLE), new AspectList().add(Aspect.CRAFT, 15).add(Aspect.TOOL, 10).add(Aspect.METAL, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.LOOM), new AspectList().add(Aspect.CRAFT, 10).add(Aspect.BEAST, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.STONECUTTER), new AspectList().add(Aspect.CRAFT, 10).add(Aspect.EARTH, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.CARTOGRAPHY_TABLE), new AspectList().add(Aspect.CRAFT, 10).add(Aspect.SENSES, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.FLETCHING_TABLE), new AspectList().add(Aspect.CRAFT, 10).add(Aspect.AVERSION, 5));
        
        // Music
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.JUKEBOX), new AspectList().add(Aspect.SENSES, 20).add(Aspect.MECHANISM, 10).add(Aspect.AIR, 15));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.NOTE_BLOCK), new AspectList().add(Aspect.SENSES, 20).add(Aspect.MECHANISM, 10).add(Aspect.AIR, 15));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.MUSIC_DISC_13), new AspectList().add(Aspect.SENSES, 15).add(Aspect.AIR, 5).add(Aspect.DESIRE, 15));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.MUSIC_DISC_CAT), new AspectList().add(Aspect.SENSES, 15).add(Aspect.AIR, 5).add(Aspect.BEAST, 5).add(Aspect.DESIRE, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.MUSIC_DISC_BLOCKS), new AspectList().add(Aspect.SENSES, 15).add(Aspect.AIR, 5).add(Aspect.TOOL, 5).add(Aspect.DESIRE, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.MUSIC_DISC_CHIRP), new AspectList().add(Aspect.SENSES, 15).add(Aspect.AIR, 5).add(Aspect.EARTH, 5).add(Aspect.DESIRE, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.MUSIC_DISC_FAR), new AspectList().add(Aspect.SENSES, 15).add(Aspect.AIR, 5).add(Aspect.ELDRITCH, 5).add(Aspect.DESIRE, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.MUSIC_DISC_MALL), new AspectList().add(Aspect.SENSES, 15).add(Aspect.AIR, 5).add(Aspect.MAN, 5).add(Aspect.DESIRE, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.MUSIC_DISC_MELLOHI), new AspectList().add(Aspect.SENSES, 15).add(Aspect.AIR, 5).add(Aspect.CRAFT, 5).add(Aspect.DESIRE, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.MUSIC_DISC_STAL), new AspectList().add(Aspect.SENSES, 15).add(Aspect.AIR, 5).add(Aspect.DARKNESS, 5).add(Aspect.DESIRE, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.MUSIC_DISC_STRAD), new AspectList().add(Aspect.SENSES, 15).add(Aspect.AIR, 5).add(Aspect.ENERGY, 5).add(Aspect.DESIRE, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.MUSIC_DISC_WARD), new AspectList().add(Aspect.SENSES, 15).add(Aspect.AIR, 5).add(Aspect.LIFE, 5).add(Aspect.DESIRE, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.MUSIC_DISC_11), new AspectList().add(Aspect.SENSES, 15).add(Aspect.AIR, 5).add(Aspect.DESIRE, 15).add(Aspect.ENTROPY, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.MUSIC_DISC_WAIT), new AspectList().add(Aspect.SENSES, 15).add(Aspect.AIR, 5).add(Aspect.TRAP, 5).add(Aspect.DESIRE, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.MUSIC_DISC_PIGSTEP), new AspectList().add(Aspect.SENSES, 15).add(Aspect.AIR, 5).add(Aspect.FIRE, 5).add(Aspect.DESIRE, 10));
        
        // Arrows
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.ARROW), new AspectList().add(Aspect.AVERSION, 5).add(Aspect.FLIGHT, 3));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.SPECTRAL_ARROW), new AspectList().add(Aspect.AVERSION, 5).add(Aspect.FLIGHT, 3).add(Aspect.SENSES, 10).add(Aspect.MAGIC, 5));
        
        // Golden foods
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.GOLDEN_APPLE), new AspectList().add(Aspect.PLANT, 5).add(Aspect.LIFE, 10).add(Aspect.DESIRE, 15).add(Aspect.MAGIC, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE), new AspectList().add(Aspect.PLANT, 5).add(Aspect.LIFE, 15).add(Aspect.PROTECT, 15).add(Aspect.MAGIC, 10).add(Aspect.DESIRE, 20));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.GOLDEN_CARROT), new AspectList().add(Aspect.PLANT, 5).add(Aspect.SENSES, 10).add(Aspect.ALCHEMY, 5).add(Aspect.DESIRE, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.GLISTERING_MELON_SLICE), new AspectList().add(Aspect.PLANT, 1).add(Aspect.ALCHEMY, 5).add(Aspect.DESIRE, 10));
        
        // Sponge
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.SPONGE), new AspectList().add(Aspect.EARTH, 5).add(Aspect.TRAP, 5).add(Aspect.VOID, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.WET_SPONGE), new AspectList().add(Aspect.EARTH, 5).add(Aspect.TRAP, 5).add(Aspect.WATER, 5));
        
        // Web
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.COBWEB), new AspectList().add(Aspect.TRAP, 5).add(Aspect.BEAST, 1));
        
        // Torch
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.TORCH), new AspectList().add(Aspect.LIGHT, 5).add(Aspect.FIRE, 3));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.SOUL_TORCH), new AspectList().add(Aspect.LIGHT, 5).add(Aspect.SOUL, 3));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.LANTERN), new AspectList().add(Aspect.LIGHT, 10).add(Aspect.METAL, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.SOUL_LANTERN), new AspectList().add(Aspect.LIGHT, 10).add(Aspect.METAL, 5).add(Aspect.SOUL, 3));
        
        // Beacon
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.BEACON), new AspectList().add(Aspect.AURA, 10).add(Aspect.MAGIC, 10).add(Aspect.EXCHANGE, 10).add(Aspect.LIGHT, 15));
        
        // Dyes (basic)
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.BLACK_DYE), new AspectList().add(Aspect.SENSES, 5).add(Aspect.DARKNESS, 2));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.WHITE_DYE), new AspectList().add(Aspect.SENSES, 5).add(Aspect.LIGHT, 2));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.RED_DYE), new AspectList().add(Aspect.SENSES, 5).add(Aspect.FIRE, 2));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.BLUE_DYE), new AspectList().add(Aspect.SENSES, 5).add(Aspect.WATER, 2));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.YELLOW_DYE), new AspectList().add(Aspect.SENSES, 5).add(Aspect.LIGHT, 2));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.GREEN_DYE), new AspectList().add(Aspect.SENSES, 5).add(Aspect.PLANT, 2));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.BROWN_DYE), new AspectList().add(Aspect.SENSES, 5).add(Aspect.EARTH, 2));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.CYAN_DYE), new AspectList().add(Aspect.SENSES, 5).add(Aspect.WATER, 2));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.PURPLE_DYE), new AspectList().add(Aspect.SENSES, 5).add(Aspect.MAGIC, 2));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.ORANGE_DYE), new AspectList().add(Aspect.SENSES, 5).add(Aspect.FIRE, 1).add(Aspect.LIGHT, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.PINK_DYE), new AspectList().add(Aspect.SENSES, 5).add(Aspect.LIFE, 2));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.LIME_DYE), new AspectList().add(Aspect.SENSES, 5).add(Aspect.PLANT, 1).add(Aspect.LIGHT, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.MAGENTA_DYE), new AspectList().add(Aspect.SENSES, 5).add(Aspect.MAGIC, 1).add(Aspect.SENSES, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.LIGHT_BLUE_DYE), new AspectList().add(Aspect.SENSES, 5).add(Aspect.AIR, 2));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.GRAY_DYE), new AspectList().add(Aspect.SENSES, 5).add(Aspect.ENTROPY, 2));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.LIGHT_GRAY_DYE), new AspectList().add(Aspect.SENSES, 5).add(Aspect.ORDER, 2));
        
        // Netherite
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.NETHERITE_SCRAP), new AspectList().add(Aspect.METAL, 15).add(Aspect.FIRE, 10).add(Aspect.VOID, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.NETHERITE_INGOT), new AspectList().add(Aspect.METAL, 25).add(Aspect.FIRE, 15).add(Aspect.VOID, 10).add(Aspect.DESIRE, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.NETHERITE_BLOCK), new AspectList().add(Aspect.METAL, 200).add(Aspect.FIRE, 120).add(Aspect.VOID, 80).add(Aspect.DESIRE, 80));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.ANCIENT_DEBRIS), new AspectList().add(Aspect.METAL, 15).add(Aspect.FIRE, 5).add(Aspect.EARTH, 5).add(Aspect.VOID, 5));
        
        // Amethyst (1.17+)
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.AMETHYST_SHARD), new AspectList().add(Aspect.CRYSTAL, 10).add(Aspect.AURA, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.AMETHYST_BLOCK), new AspectList().add(Aspect.CRYSTAL, 25).add(Aspect.AURA, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.BUDDING_AMETHYST), new AspectList().add(Aspect.CRYSTAL, 20).add(Aspect.AURA, 10).add(Aspect.LIFE, 5));
        
        // Honey (1.15+)
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.HONEYCOMB), new AspectList().add(Aspect.LIFE, 5).add(Aspect.BEAST, 5).add(Aspect.ORDER, 3));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.HONEY_BOTTLE), new AspectList().add(Aspect.LIFE, 10).add(Aspect.BEAST, 5).add(Aspect.DESIRE, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.HONEYCOMB_BLOCK), new AspectList().add(Aspect.LIFE, 15).add(Aspect.BEAST, 15).add(Aspect.ORDER, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.HONEY_BLOCK), new AspectList().add(Aspect.LIFE, 20).add(Aspect.BEAST, 10).add(Aspect.TRAP, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.BEE_NEST), new AspectList().add(Aspect.LIFE, 10).add(Aspect.BEAST, 15).add(Aspect.PLANT, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.BEEHIVE), new AspectList().add(Aspect.LIFE, 10).add(Aspect.BEAST, 15).add(Aspect.CRAFT, 5));
        
        // Copper (1.17+)
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.RAW_COPPER), new AspectList().add(Aspect.METAL, 10).add(Aspect.EARTH, 5).add(Aspect.EXCHANGE, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.COPPER_BLOCK), new AspectList().add(Aspect.METAL, 80).add(Aspect.EXCHANGE, 40));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.RAW_COPPER_BLOCK), new AspectList().add(Aspect.METAL, 80).add(Aspect.EARTH, 40).add(Aspect.EXCHANGE, 40));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.SPYGLASS), new AspectList().add(Aspect.SENSES, 20).add(Aspect.METAL, 5).add(Aspect.CRYSTAL, 5));
        
        // Sculk (1.19+)
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.SCULK), new AspectList().add(Aspect.SOUL, 10).add(Aspect.DARKNESS, 10).add(Aspect.ENTROPY, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.SCULK_SENSOR), new AspectList().add(Aspect.SOUL, 10).add(Aspect.SENSES, 20).add(Aspect.MECHANISM, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.SCULK_CATALYST), new AspectList().add(Aspect.SOUL, 20).add(Aspect.LIFE, 10).add(Aspect.DEATH, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.SCULK_SHRIEKER), new AspectList().add(Aspect.SOUL, 20).add(Aspect.SENSES, 15).add(Aspect.DARKNESS, 15));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.ECHO_SHARD), new AspectList().add(Aspect.SOUL, 15).add(Aspect.SENSES, 15).add(Aspect.DARKNESS, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.RECOVERY_COMPASS), new AspectList().add(Aspect.SOUL, 20).add(Aspect.SENSES, 15).add(Aspect.DEATH, 10));
        
        // New 1.20 entities
        ThaumcraftApi.registerEntityTag("minecraft:camel", new AspectList().add(Aspect.BEAST, 15).add(Aspect.EARTH, 10));
        ThaumcraftApi.registerEntityTag("minecraft:sniffer", new AspectList().add(Aspect.BEAST, 15).add(Aspect.SENSES, 10).add(Aspect.EARTH, 5));
        ThaumcraftApi.registerEntityTag("minecraft:warden", new AspectList().add(Aspect.DARKNESS, 50).add(Aspect.SOUL, 50).add(Aspect.DEATH, 30));
        ThaumcraftApi.registerEntityTag("minecraft:allay", new AspectList().add(Aspect.FLIGHT, 10).add(Aspect.MAGIC, 10).add(Aspect.EXCHANGE, 10));
        ThaumcraftApi.registerEntityTag("minecraft:frog", new AspectList().add(Aspect.BEAST, 5).add(Aspect.WATER, 5).add(Aspect.MOTION, 5));
        ThaumcraftApi.registerEntityTag("minecraft:tadpole", new AspectList().add(Aspect.BEAST, 2).add(Aspect.WATER, 5).add(Aspect.LIFE, 3));
        ThaumcraftApi.registerEntityTag("minecraft:goat", new AspectList().add(Aspect.BEAST, 15).add(Aspect.EARTH, 5).add(Aspect.MOTION, 5));
        ThaumcraftApi.registerEntityTag("minecraft:axolotl", new AspectList().add(Aspect.BEAST, 10).add(Aspect.WATER, 10).add(Aspect.LIFE, 5));
        ThaumcraftApi.registerEntityTag("minecraft:glow_squid", new AspectList().add(Aspect.BEAST, 5).add(Aspect.WATER, 10).add(Aspect.LIGHT, 10));
        ThaumcraftApi.registerEntityTag("minecraft:piglin", new AspectList().add(Aspect.BEAST, 10).add(Aspect.FIRE, 10).add(Aspect.DESIRE, 10));
        ThaumcraftApi.registerEntityTag("minecraft:piglin_brute", new AspectList().add(Aspect.BEAST, 15).add(Aspect.FIRE, 10).add(Aspect.AVERSION, 10));
        ThaumcraftApi.registerEntityTag("minecraft:hoglin", new AspectList().add(Aspect.BEAST, 20).add(Aspect.FIRE, 10).add(Aspect.AVERSION, 5));
        ThaumcraftApi.registerEntityTag("minecraft:zoglin", new AspectList().add(Aspect.BEAST, 20).add(Aspect.UNDEAD, 15).add(Aspect.AVERSION, 10));
        ThaumcraftApi.registerEntityTag("minecraft:strider", new AspectList().add(Aspect.BEAST, 10).add(Aspect.FIRE, 15).add(Aspect.MOTION, 5));
        ThaumcraftApi.registerEntityTag("minecraft:drowned", new AspectList().add(Aspect.UNDEAD, 20).add(Aspect.MAN, 10).add(Aspect.WATER, 10));
        ThaumcraftApi.registerEntityTag("minecraft:phantom", new AspectList().add(Aspect.UNDEAD, 15).add(Aspect.FLIGHT, 15).add(Aspect.DARKNESS, 10));
        ThaumcraftApi.registerEntityTag("minecraft:turtle", new AspectList().add(Aspect.BEAST, 10).add(Aspect.WATER, 10).add(Aspect.PROTECT, 5));
        ThaumcraftApi.registerEntityTag("minecraft:dolphin", new AspectList().add(Aspect.BEAST, 15).add(Aspect.WATER, 15).add(Aspect.MOTION, 10));
        ThaumcraftApi.registerEntityTag("minecraft:panda", new AspectList().add(Aspect.BEAST, 15).add(Aspect.PLANT, 10));
        ThaumcraftApi.registerEntityTag("minecraft:fox", new AspectList().add(Aspect.BEAST, 10).add(Aspect.EARTH, 5).add(Aspect.DESIRE, 5));
        ThaumcraftApi.registerEntityTag("minecraft:bee", new AspectList().add(Aspect.BEAST, 5).add(Aspect.FLIGHT, 5).add(Aspect.LIFE, 5));
        ThaumcraftApi.registerEntityTag("minecraft:cat", new AspectList().add(Aspect.BEAST, 10).add(Aspect.ENTROPY, 10));
        ThaumcraftApi.registerEntityTag("minecraft:ravager", new AspectList().add(Aspect.BEAST, 30).add(Aspect.AVERSION, 20).add(Aspect.MAN, 5));
        ThaumcraftApi.registerEntityTag("minecraft:pillager", new AspectList().add(Aspect.AVERSION, 10).add(Aspect.MAN, 15));
        ThaumcraftApi.registerEntityTag("minecraft:wandering_trader", new AspectList().add(Aspect.MAN, 15).add(Aspect.EXCHANGE, 10));
        ThaumcraftApi.registerEntityTag("minecraft:trader_llama", new AspectList().add(Aspect.BEAST, 15).add(Aspect.EXCHANGE, 5));
    }

    /**
     * Register aspects for Thaumcraft blocks and items
     */
    private static void registerThaumcraftItems() {
        // Crystal clusters (blocks)
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.CRYSTAL_AIR.get()), new AspectList().add(Aspect.AIR, 15).add(Aspect.CRYSTAL, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.CRYSTAL_FIRE.get()), new AspectList().add(Aspect.FIRE, 15).add(Aspect.CRYSTAL, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.CRYSTAL_WATER.get()), new AspectList().add(Aspect.WATER, 15).add(Aspect.CRYSTAL, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.CRYSTAL_EARTH.get()), new AspectList().add(Aspect.EARTH, 15).add(Aspect.CRYSTAL, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.CRYSTAL_ORDER.get()), new AspectList().add(Aspect.ORDER, 15).add(Aspect.CRYSTAL, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.CRYSTAL_ENTROPY.get()), new AspectList().add(Aspect.ENTROPY, 15).add(Aspect.CRYSTAL, 10));
        
        // Ores
        ThaumcraftApi.registerObjectTag("thaumcraft:ores/cinnabar", new AspectList().add(Aspect.EARTH, 5).add(Aspect.METAL, 10).add(Aspect.ALCHEMY, 5).add(Aspect.DEATH, 5));
        ThaumcraftApi.registerObjectTag("thaumcraft:ores/amber", new AspectList().add(Aspect.EARTH, 5).add(Aspect.TRAP, 10).add(Aspect.CRYSTAL, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.CINNABAR_ORE.get()), new AspectList().add(Aspect.EARTH, 5).add(Aspect.METAL, 10).add(Aspect.ALCHEMY, 5).add(Aspect.DEATH, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.AMBER_ORE.get()), new AspectList().add(Aspect.EARTH, 5).add(Aspect.TRAP, 10).add(Aspect.CRYSTAL, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.QUARTZ_ORE.get()), new AspectList().add(Aspect.EARTH, 5).add(Aspect.CRYSTAL, 10));
        
        // Resources
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.QUICKSILVER.get()), new AspectList().add(Aspect.METAL, 10).add(Aspect.DEATH, 5).add(Aspect.ALCHEMY, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.AMBER.get()), new AspectList().add(Aspect.TRAP, 10).add(Aspect.CRYSTAL, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.THAUMIUM_INGOT.get()), new AspectList().add(Aspect.METAL, 10).add(Aspect.MAGIC, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.VOID_METAL_INGOT.get()), new AspectList().add(Aspect.METAL, 10).add(Aspect.VOID, 10).add(Aspect.DARKNESS, 5));
        // TC6-removed: ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.BRASS_INGOT.get()), new AspectList().add(Aspect.METAL, 10).add(Aspect.TOOL, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.SALIS_MUNDUS.get()), new AspectList().add(Aspect.MAGIC, 5).add(Aspect.ENERGY, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.TALLOW.get()), new AspectList().add(Aspect.LIFE, 5).add(Aspect.BEAST, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.ALUMENTUM.get()), new AspectList().add(Aspect.FIRE, 15).add(Aspect.ENERGY, 15).add(Aspect.ALCHEMY, 5));
        
        // Plates
        // TC6-removed: ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.PLATE_BRASS.get()), new AspectList().add(Aspect.METAL, 5).add(Aspect.TOOL, 2));
        // TC6-removed: ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.PLATE_IRON.get()), new AspectList().add(Aspect.METAL, 5));
        // TC6-removed: ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.PLATE_THAUMIUM.get()), new AspectList().add(Aspect.METAL, 5).add(Aspect.MAGIC, 5));
        // TC6-removed: ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.PLATE_VOID.get()), new AspectList().add(Aspect.METAL, 5).add(Aspect.VOID, 5));
        
        // Nuggets
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.THAUMIUM_NUGGET.get()), new AspectList().add(Aspect.METAL, 1).add(Aspect.MAGIC, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.VOID_METAL_NUGGET.get()), new AspectList().add(Aspect.METAL, 1).add(Aspect.VOID, 1));
        // TC6-removed: ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.BRASS_NUGGET.get()), new AspectList().add(Aspect.METAL, 1));
        
        // Magic items
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.THAUMONOMICON.get()), new AspectList().add(Aspect.MIND, 20).add(Aspect.MAGIC, 10).add(Aspect.PLANT, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.THAUMOMETER.get()), new AspectList().add(Aspect.SENSES, 10).add(Aspect.AURA, 10).add(Aspect.MAGIC, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.SCRIBING_TOOLS.get()), new AspectList().add(Aspect.MIND, 5).add(Aspect.CRAFT, 5));
        
        // Phials
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.PHIAL_EMPTY.get()), new AspectList().add(Aspect.VOID, 3).add(Aspect.CRYSTAL, 3));
        // Filled phials have variable aspects based on contents
        
        // Loot bags
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.LOOT_BAG_COMMON.get()), new AspectList().add(Aspect.DESIRE, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.LOOT_BAG_UNCOMMON.get()), new AspectList().add(Aspect.DESIRE, 20));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.LOOT_BAG_RARE.get()), new AspectList().add(Aspect.DESIRE, 30));
        
        // Curios
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.PRIMORDIAL_PEARL.get()), new AspectList()
                .add(Aspect.AIR, 10).add(Aspect.FIRE, 10).add(Aspect.WATER, 10)
                .add(Aspect.EARTH, 10).add(Aspect.ORDER, 10).add(Aspect.ENTROPY, 10));
        // TC6-only: Celestial Notes removed
        
        // Food
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.TRIPLE_MEAT_TREAT.get()), new AspectList().add(Aspect.LIFE, 10).add(Aspect.DESIRE, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.ZOMBIE_BRAIN.get()), new AspectList().add(Aspect.LIFE, 5).add(Aspect.MIND, 20).add(Aspect.UNDEAD, 10));
        
        // Taint blocks
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.TAINT_FIBRE.get()), new AspectList().add(Aspect.PLANT, 5).add(Aspect.FLUX, 10));
        
        // Trees
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.GREATWOOD_LOG.get()), new AspectList().add(Aspect.PLANT, 20).add(Aspect.LIFE, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.SILVERWOOD_LOG.get()), new AspectList().add(Aspect.PLANT, 20).add(Aspect.AURA, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.GREATWOOD_LEAVES.get()), new AspectList().add(Aspect.PLANT, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.SILVERWOOD_LEAVES.get()), new AspectList().add(Aspect.PLANT, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.GREATWOOD_SAPLING.get()), new AspectList().add(Aspect.PLANT, 15).add(Aspect.LIFE, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.SILVERWOOD_SAPLING.get()), new AspectList().add(Aspect.PLANT, 15).add(Aspect.AURA, 5));
        
        // Plants
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.SHIMMERLEAF.get()), new AspectList().add(Aspect.PLANT, 5).add(Aspect.AURA, 10).add(Aspect.ENERGY, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.CINDERPEARL.get()), new AspectList().add(Aspect.PLANT, 5).add(Aspect.AURA, 5).add(Aspect.FIRE, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.VISHROOM.get()), new AspectList().add(Aspect.PLANT, 2).add(Aspect.DEATH, 1).add(Aspect.MAGIC, 1).add(Aspect.ENTROPY, 1));
        
        // Ancient/Eldritch stones
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.ANCIENT_STONE.get()), new AspectList().add(Aspect.EARTH, 5).add(Aspect.ELDRITCH, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.ANCIENT_STONE_TILE.get()), new AspectList().add(Aspect.EARTH, 5).add(Aspect.ELDRITCH, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.ANCIENT_STONE_ROCK.get()), new AspectList().add(Aspect.EARTH, 5).add(Aspect.ELDRITCH, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.ELDRITCH_STONE_TILE.get()), new AspectList().add(Aspect.EARTH, 5).add(Aspect.ELDRITCH, 5));
        
        // Arcane stone
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.ARCANE_STONE.get()), new AspectList().add(Aspect.EARTH, 5).add(Aspect.MAGIC, 3));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.ARCANE_STONE_BRICK.get()), new AspectList().add(Aspect.EARTH, 5).add(Aspect.MAGIC, 3).add(Aspect.ORDER, 1));
        
        // Metal blocks
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.THAUMIUM_BLOCK.get()), new AspectList().add(Aspect.METAL, 80).add(Aspect.MAGIC, 80));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.VOID_METAL_BLOCK.get()), new AspectList().add(Aspect.METAL, 80).add(Aspect.VOID, 80).add(Aspect.DARKNESS, 40));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.BRASS_BLOCK.get()), new AspectList().add(Aspect.METAL, 80).add(Aspect.TOOL, 40));
        
        // Porous stone
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.POROUS_STONE.get()), new AspectList().add(Aspect.EARTH, 5).add(Aspect.VOID, 5));
        
        // Crafting blocks
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.ARCANE_WORKBENCH.get()), new AspectList().add(Aspect.CRAFT, 20).add(Aspect.MAGIC, 5).add(Aspect.AURA, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.CRUCIBLE.get()), new AspectList().add(Aspect.VOID, 10).add(Aspect.CRAFT, 20).add(Aspect.ALCHEMY, 20));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.RESEARCH_TABLE.get()), new AspectList().add(Aspect.MIND, 10).add(Aspect.CRAFT, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.INFUSION_MATRIX.get()), new AspectList().add(Aspect.MAGIC, 20).add(Aspect.CRAFT, 15).add(Aspect.ELDRITCH, 10));
        
        // Pedestals
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.PEDESTAL_ARCANE.get()), new AspectList().add(Aspect.MAGIC, 3).add(Aspect.AIR, 3).add(Aspect.EARTH, 3));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.PEDESTAL_ANCIENT.get()), new AspectList().add(Aspect.MAGIC, 3).add(Aspect.ELDRITCH, 3).add(Aspect.EARTH, 3));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.PEDESTAL_ELDRITCH.get()), new AspectList().add(Aspect.MAGIC, 3).add(Aspect.ELDRITCH, 5).add(Aspect.VOID, 3));
        
        // Goggles
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.GOGGLES.get()), new AspectList().add(Aspect.SENSES, 10).add(Aspect.AURA, 10).add(Aspect.PROTECT, 5));
        
        // Armor - Thaumium
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.THAUMIUM_HELM.get()), new AspectList().add(Aspect.METAL, 20).add(Aspect.MAGIC, 20).add(Aspect.PROTECT, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.THAUMIUM_CHEST.get()), new AspectList().add(Aspect.METAL, 35).add(Aspect.MAGIC, 35).add(Aspect.PROTECT, 15));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.THAUMIUM_LEGS.get()), new AspectList().add(Aspect.METAL, 30).add(Aspect.MAGIC, 30).add(Aspect.PROTECT, 12));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.THAUMIUM_BOOTS.get()), new AspectList().add(Aspect.METAL, 15).add(Aspect.MAGIC, 15).add(Aspect.PROTECT, 8));
        
        // Armor - Void
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.VOID_HELM.get()), new AspectList().add(Aspect.METAL, 20).add(Aspect.VOID, 20).add(Aspect.PROTECT, 15));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.VOID_CHEST.get()), new AspectList().add(Aspect.METAL, 35).add(Aspect.VOID, 35).add(Aspect.PROTECT, 20));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.VOID_LEGS.get()), new AspectList().add(Aspect.METAL, 30).add(Aspect.VOID, 30).add(Aspect.PROTECT, 18));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.VOID_BOOTS.get()), new AspectList().add(Aspect.METAL, 15).add(Aspect.VOID, 15).add(Aspect.PROTECT, 12));
        
        // Tools - Thaumium
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.THAUMIUM_SWORD.get()), new AspectList().add(Aspect.METAL, 10).add(Aspect.MAGIC, 10).add(Aspect.AVERSION, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.THAUMIUM_PICK.get()), new AspectList().add(Aspect.METAL, 15).add(Aspect.MAGIC, 15).add(Aspect.TOOL, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.THAUMIUM_AXE.get()), new AspectList().add(Aspect.METAL, 15).add(Aspect.MAGIC, 15).add(Aspect.TOOL, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.THAUMIUM_SHOVEL.get()), new AspectList().add(Aspect.METAL, 5).add(Aspect.MAGIC, 5).add(Aspect.TOOL, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.THAUMIUM_HOE.get()), new AspectList().add(Aspect.METAL, 10).add(Aspect.MAGIC, 10).add(Aspect.TOOL, 5));
        
        // Tools - Void
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.VOID_SWORD.get()), new AspectList().add(Aspect.METAL, 10).add(Aspect.VOID, 10).add(Aspect.AVERSION, 15));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.VOID_PICK.get()), new AspectList().add(Aspect.METAL, 15).add(Aspect.VOID, 15).add(Aspect.TOOL, 15));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.VOID_AXE.get()), new AspectList().add(Aspect.METAL, 15).add(Aspect.VOID, 15).add(Aspect.TOOL, 15));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.VOID_SHOVEL.get()), new AspectList().add(Aspect.METAL, 5).add(Aspect.VOID, 5).add(Aspect.TOOL, 15));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.VOID_HOE.get()), new AspectList().add(Aspect.METAL, 10).add(Aspect.VOID, 10).add(Aspect.TOOL, 10));
        
        // Crimson gear
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.CRIMSON_BLADE.get()), new AspectList().add(Aspect.METAL, 15).add(Aspect.ELDRITCH, 10).add(Aspect.DEATH, 10).add(Aspect.AVERSION, 15));
        
        // Caster and Focus (DISABLED - replaced by Wand/WandFocus system, Етап В)
//        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.CASTER_BASIC.get()), new AspectList().add(Aspect.MAGIC, 20).add(Aspect.AURA, 10).add(Aspect.TOOL, 5));
// TC6-removed: //        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.FOCUS_BLANK.get()), new AspectList().add(Aspect.MAGIC, 10).add(Aspect.AURA, 5));
// TC6-removed: //        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.FOCUS_ADVANCED.get()), new AspectList().add(Aspect.MAGIC, 20).add(Aspect.AURA, 10));
        
        // Baubles
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.AMULET_VIS_CRAFTED.get()), new AspectList().add(Aspect.AURA, 20).add(Aspect.METAL, 5).add(Aspect.MAGIC, 5));
        // TC6-removed: ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.CURIOSITY_BAND.get()), new AspectList().add(Aspect.MIND, 20).add(Aspect.METAL, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.CLOUD_RING.get()), new AspectList().add(Aspect.FLIGHT, 15).add(Aspect.AIR, 15).add(Aspect.METAL, 5));
        
        // Jars
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.JAR_NORMAL.get()), new AspectList().add(Aspect.VOID, 10).add(Aspect.CRYSTAL, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.JAR_VOID.get()), new AspectList().add(Aspect.VOID, 20).add(Aspect.CRYSTAL, 5).add(Aspect.DARKNESS, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.JAR_BRAIN.get()), new AspectList().add(Aspect.VOID, 10).add(Aspect.CRYSTAL, 5).add(Aspect.MIND, 20));
        
        // Essentia system
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.ALEMBIC.get()), new AspectList().add(Aspect.METAL, 15).add(Aspect.ALCHEMY, 10).add(Aspect.VOID, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.CENTRIFUGE.get()), new AspectList().add(Aspect.MECHANISM, 10).add(Aspect.ALCHEMY, 10).add(Aspect.MOTION, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.TUBE_NORMAL.get()), new AspectList().add(Aspect.METAL, 5).add(Aspect.VOID, 3));
        
        // Smelters
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.SMELTER.get()), new AspectList().add(Aspect.FIRE, 15).add(Aspect.METAL, 10).add(Aspect.ALCHEMY, 5));
        
        // Devices
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.HUNGRY_CHEST.get()), new AspectList().add(Aspect.VOID, 20).add(Aspect.DESIRE, 10).add(Aspect.PLANT, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.ARCANE_EAR.get()), new AspectList().add(Aspect.SENSES, 20).add(Aspect.MECHANISM, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.BELLOWS.get()), new AspectList().add(Aspect.AIR, 10).add(Aspect.MECHANISM, 5).add(Aspect.BEAST, 5));
        
        // Shards
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.SHARD_AIR.get()), new AspectList().add(Aspect.AIR, 10).add(Aspect.CRYSTAL, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.SHARD_FIRE.get()), new AspectList().add(Aspect.FIRE, 10).add(Aspect.CRYSTAL, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.SHARD_WATER.get()), new AspectList().add(Aspect.WATER, 10).add(Aspect.CRYSTAL, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.SHARD_EARTH.get()), new AspectList().add(Aspect.EARTH, 10).add(Aspect.CRYSTAL, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.SHARD_ORDER.get()), new AspectList().add(Aspect.ORDER, 10).add(Aspect.CRYSTAL, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.SHARD_ENTROPY.get()), new AspectList().add(Aspect.ENTROPY, 10).add(Aspect.CRYSTAL, 5));
        // TC6-removed: ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.BALANCED_SHARD.get()), new AspectList().add(Aspect.ORDER, 5).add(Aspect.ENTROPY, 5).add(Aspect.CRYSTAL, 5));
        
        // Clusters
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.CLUSTER_IRON.get()), new AspectList().add(Aspect.ORDER, 5).add(Aspect.METAL, 15).add(Aspect.EARTH, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.CLUSTER_GOLD.get()), new AspectList().add(Aspect.ORDER, 5).add(Aspect.METAL, 15).add(Aspect.EARTH, 5).add(Aspect.DESIRE, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.CLUSTER_COPPER.get()), new AspectList().add(Aspect.ORDER, 5).add(Aspect.METAL, 15).add(Aspect.EARTH, 5).add(Aspect.EXCHANGE, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.CLUSTER_CINNABAR.get()), new AspectList().add(Aspect.ORDER, 5).add(Aspect.METAL, 15).add(Aspect.EARTH, 5).add(Aspect.ALCHEMY, 5).add(Aspect.DEATH, 5));
        
        // Misc resources
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.ENCHANTED_FABRIC.get()), new AspectList().add(Aspect.CRAFT, 5).add(Aspect.MAGIC, 5).add(Aspect.BEAST, 3));
        // TC6-removed: ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.PRIMAL_CHARM.get()), new AspectList().add(Aspect.MAGIC, 15).add(Aspect.AURA, 10));
        // TC6-removed: ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.MIRRORED_GLASS.get()), new AspectList().add(Aspect.CRYSTAL, 5).add(Aspect.MOTION, 5).add(Aspect.VOID, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.VOID_SEED.get()), new AspectList().add(Aspect.VOID, 10).add(Aspect.ELDRITCH, 5).add(Aspect.LIFE, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.FILTER.get()), new AspectList().add(Aspect.ORDER, 5).add(Aspect.VOID, 3));
        // TC6-removed: ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.MORPHIC_RESONATOR.get()), new AspectList().add(Aspect.AURA, 5).add(Aspect.EXCHANGE, 5).add(Aspect.MECHANISM, 5));
        // TC6-removed: ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.VIS_RESONATOR.get()), new AspectList().add(Aspect.AURA, 10).add(Aspect.CRYSTAL, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.JAR_BRACE.get()), new AspectList().add(Aspect.METAL, 5).add(Aspect.ORDER, 3));
        // TC6-removed: ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.MECHANISM_SIMPLE.get()), new AspectList().add(Aspect.MECHANISM, 10).add(Aspect.METAL, 5));
        // TC6-removed: ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.MECHANISM_COMPLEX.get()), new AspectList().add(Aspect.MECHANISM, 20).add(Aspect.METAL, 10));
        
        // Brains
        // TC6-removed: ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.BRAIN_NORMAL.get()), new AspectList().add(Aspect.LIFE, 5).add(Aspect.MIND, 20).add(Aspect.UNDEAD, 10));
        // TC6-removed: ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.BRAIN_CLOCKWORK.get()), new AspectList().add(Aspect.MECHANISM, 15).add(Aspect.MIND, 20).add(Aspect.ORDER, 5));
        // TC6-removed: ThaumcraftApi.registerObjectTag(new ItemStack(ModItems.BRAIN_CURIOUS.get()), new AspectList().add(Aspect.LIFE, 5).add(Aspect.MIND, 25).add(Aspect.SENSES, 10));
        
        // Loot blocks (crates and urns)
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.LOOT_CRATE_COMMON.get()), new AspectList().add(Aspect.DESIRE, 10).add(Aspect.PLANT, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.LOOT_CRATE_UNCOMMON.get()), new AspectList().add(Aspect.DESIRE, 20).add(Aspect.PLANT, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.LOOT_CRATE_RARE.get()), new AspectList().add(Aspect.DESIRE, 30).add(Aspect.PLANT, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.LOOT_URN_COMMON.get()), new AspectList().add(Aspect.DESIRE, 10).add(Aspect.EARTH, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.LOOT_URN_UNCOMMON.get()), new AspectList().add(Aspect.DESIRE, 20).add(Aspect.EARTH, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.LOOT_URN_RARE.get()), new AspectList().add(Aspect.DESIRE, 30).add(Aspect.EARTH, 5));
        
        // Liquid blocks (no item form, but useful for scanning)
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.LIQUID_DEATH.get()), new AspectList().add(Aspect.DEATH, 20).add(Aspect.WATER, 10).add(Aspect.ALCHEMY, 10));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.PURIFYING_FLUID.get()), new AspectList().add(Aspect.LIFE, 20).add(Aspect.WATER, 10).add(Aspect.MAGIC, 10).add(Aspect.ORDER, 5));
        
        // Banners
        AspectList bannerAspects = new AspectList().add(Aspect.CRAFT, 5).add(Aspect.SENSES, 5).add(Aspect.BEAST, 3);
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.BANNER_WHITE.get()), bannerAspects.copy().add(Aspect.LIGHT, 2));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.BANNER_ORANGE.get()), bannerAspects.copy().add(Aspect.FIRE, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.BANNER_MAGENTA.get()), bannerAspects.copy().add(Aspect.MAGIC, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.BANNER_LIGHT_BLUE.get()), bannerAspects.copy().add(Aspect.AIR, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.BANNER_YELLOW.get()), bannerAspects.copy().add(Aspect.LIGHT, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.BANNER_LIME.get()), bannerAspects.copy().add(Aspect.PLANT, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.BANNER_PINK.get()), bannerAspects.copy().add(Aspect.LIFE, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.BANNER_GRAY.get()), bannerAspects.copy().add(Aspect.ENTROPY, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.BANNER_LIGHT_GRAY.get()), bannerAspects.copy().add(Aspect.ORDER, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.BANNER_CYAN.get()), bannerAspects.copy().add(Aspect.WATER, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.BANNER_PURPLE.get()), bannerAspects.copy().add(Aspect.MAGIC, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.BANNER_BLUE.get()), bannerAspects.copy().add(Aspect.WATER, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.BANNER_BROWN.get()), bannerAspects.copy().add(Aspect.EARTH, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.BANNER_GREEN.get()), bannerAspects.copy().add(Aspect.PLANT, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.BANNER_RED.get()), bannerAspects.copy().add(Aspect.FIRE, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.BANNER_BLACK.get()), bannerAspects.copy().add(Aspect.DARKNESS, 2));
        ThaumcraftApi.registerObjectTag(new ItemStack(ModBlocks.BANNER_CRIMSON_CULT.get()), new AspectList().add(Aspect.CRAFT, 5).add(Aspect.SENSES, 5).add(Aspect.ELDRITCH, 10).add(Aspect.DARKNESS, 5));
    }
}




