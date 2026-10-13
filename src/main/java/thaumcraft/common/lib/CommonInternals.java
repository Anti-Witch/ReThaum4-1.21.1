package thaumcraft.common.lib;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectHelper;
import thaumcraft.api.aspects.AspectList;

import java.util.HashMap;
import java.util.Map;

/**
 * CommonInternals - Holds internal data for Thaumcraft systems.
 * 
 * Handles:
 * - Aspect registration for vanilla and modded items
 * - Smelting bonus data (bonus drops from infernal furnace)
 * - Crucible recipe catalyst data
 * 
 * Ported from 1.12.2
 */
public class CommonInternals {

    /**
     * Smelting bonus map - items that give bonus drops when smelted in infernal furnace.
     * Key: Input item ResourceLocation
     * Value: Array of possible bonus ItemStacks with weights
     */
    public static Map<String, ItemStack[]> smeltingBonus = new HashMap<>();

    /**
     * Initialize all aspect tags for vanilla items.
     * Called during mod initialization.
     */
    public static void initAspects() {
        // ==================== Basic Resources ====================
        
        // Primal elements
        registerAspects("minecraft:coal", new AspectList()
                .add(Aspect.FIRE, 10).add(Aspect.ENERGY, 10));
        registerAspects("minecraft:charcoal", new AspectList()
                .add(Aspect.FIRE, 10).add(Aspect.ENERGY, 10));
        registerAspects("minecraft:diamond", new AspectList()
                .add(Aspect.CRYSTAL, 15).add(Aspect.DESIRE, 15));
        registerAspects("minecraft:emerald", new AspectList()
                .add(Aspect.CRYSTAL, 10).add(Aspect.DESIRE, 10));
        registerAspects("minecraft:lapis_lazuli", new AspectList()
                .add(Aspect.SENSES, 5).add(Aspect.MIND, 5));
        registerAspects("minecraft:redstone", new AspectList()
                .add(Aspect.ENERGY, 5).add(Aspect.MECHANISM, 3));
        registerAspects("minecraft:glowstone_dust", new AspectList()
                .add(Aspect.LIGHT, 5).add(Aspect.ENERGY, 3));
        registerAspects("minecraft:quartz", new AspectList()
                .add(Aspect.CRYSTAL, 5).add(Aspect.ORDER, 3));

        // Metals
        registerAspects("minecraft:iron_ingot", new AspectList()
                .add(Aspect.METAL, 10));
        registerAspects("minecraft:gold_ingot", new AspectList()
                .add(Aspect.METAL, 10).add(Aspect.DESIRE, 5));
        registerAspects("minecraft:copper_ingot", new AspectList()
                .add(Aspect.METAL, 8).add(Aspect.EXCHANGE, 2));
        registerAspects("minecraft:netherite_ingot", new AspectList()
                .add(Aspect.METAL, 20).add(Aspect.FIRE, 10).add(Aspect.VOID, 10));

        // Raw ores
        registerAspects("minecraft:raw_iron", new AspectList()
                .add(Aspect.METAL, 5).add(Aspect.EARTH, 3));
        registerAspects("minecraft:raw_gold", new AspectList()
                .add(Aspect.METAL, 5).add(Aspect.DESIRE, 3));
        registerAspects("minecraft:raw_copper", new AspectList()
                .add(Aspect.METAL, 4).add(Aspect.EARTH, 2));

        // ==================== Building Blocks ====================
        
        registerAspects("minecraft:stone", new AspectList()
                .add(Aspect.EARTH, 5));
        registerAspects("minecraft:cobblestone", new AspectList()
                .add(Aspect.EARTH, 5).add(Aspect.ENTROPY, 1));
        registerAspects("minecraft:dirt", new AspectList()
                .add(Aspect.EARTH, 5));
        registerAspects("minecraft:sand", new AspectList()
                .add(Aspect.EARTH, 5).add(Aspect.ENTROPY, 3));
        registerAspects("minecraft:gravel", new AspectList()
                .add(Aspect.EARTH, 3).add(Aspect.ENTROPY, 3));
        registerAspects("minecraft:obsidian", new AspectList()
                .add(Aspect.EARTH, 5).add(Aspect.FIRE, 5).add(Aspect.DARKNESS, 5));
        registerAspects("minecraft:glass", new AspectList()
                .add(Aspect.CRYSTAL, 5));
        registerAspects("minecraft:clay_ball", new AspectList()
                .add(Aspect.EARTH, 3).add(Aspect.WATER, 3));
        registerAspects("minecraft:brick", new AspectList()
                .add(Aspect.EARTH, 3).add(Aspect.FIRE, 1));
        registerAspects("minecraft:nether_brick", new AspectList()
                .add(Aspect.EARTH, 3).add(Aspect.FIRE, 3));

        // ==================== Wood ====================
        
        registerAspects("minecraft:oak_log", new AspectList()
                .add(Aspect.PLANT, 20));
        registerAspects("minecraft:spruce_log", new AspectList()
                .add(Aspect.PLANT, 20));
        registerAspects("minecraft:birch_log", new AspectList()
                .add(Aspect.PLANT, 20));
        registerAspects("minecraft:jungle_log", new AspectList()
                .add(Aspect.PLANT, 20));
        registerAspects("minecraft:acacia_log", new AspectList()
                .add(Aspect.PLANT, 20));
        registerAspects("minecraft:dark_oak_log", new AspectList()
                .add(Aspect.PLANT, 20));
        registerAspects("minecraft:cherry_log", new AspectList()
                .add(Aspect.PLANT, 20));
        registerAspects("minecraft:mangrove_log", new AspectList()
                .add(Aspect.PLANT, 20));

        registerAspects("minecraft:oak_planks", new AspectList()
                .add(Aspect.PLANT, 3));
        registerAspects("minecraft:stick", new AspectList()
                .add(Aspect.PLANT, 1));

        // ==================== Plants ====================
        
        registerAspects("minecraft:wheat_seeds", new AspectList()
                .add(Aspect.PLANT, 3).add(Aspect.LIFE, 3));
        registerAspects("minecraft:wheat", new AspectList()
                .add(Aspect.PLANT, 5).add(Aspect.LIFE, 3));
        registerAspects("minecraft:apple", new AspectList()
                .add(Aspect.PLANT, 5).add(Aspect.LIFE, 5));
        registerAspects("minecraft:melon_slice", new AspectList()
                .add(Aspect.PLANT, 3).add(Aspect.LIFE, 3));
        registerAspects("minecraft:pumpkin", new AspectList()
                .add(Aspect.PLANT, 10).add(Aspect.LIFE, 5));
        registerAspects("minecraft:cactus", new AspectList()
                .add(Aspect.PLANT, 5).add(Aspect.AVERSION, 3));
        registerAspects("minecraft:sugar_cane", new AspectList()
                .add(Aspect.PLANT, 5).add(Aspect.WATER, 3));

        // ==================== Food ====================
        
        registerAspects("minecraft:bread", new AspectList()
                .add(Aspect.PLANT, 3).add(Aspect.LIFE, 5).add(Aspect.CRAFT, 3));
        registerAspects("minecraft:cooked_beef", new AspectList()
                .add(Aspect.BEAST, 5).add(Aspect.LIFE, 10).add(Aspect.FIRE, 3));
        registerAspects("minecraft:cooked_porkchop", new AspectList()
                .add(Aspect.BEAST, 5).add(Aspect.LIFE, 10).add(Aspect.FIRE, 3));
        registerAspects("minecraft:cooked_chicken", new AspectList()
                .add(Aspect.BEAST, 3).add(Aspect.LIFE, 8).add(Aspect.FIRE, 3).add(Aspect.FLIGHT, 3));
        registerAspects("minecraft:cooked_mutton", new AspectList()
                .add(Aspect.BEAST, 5).add(Aspect.LIFE, 8).add(Aspect.FIRE, 3));
        registerAspects("minecraft:golden_apple", new AspectList()
                .add(Aspect.PLANT, 5).add(Aspect.LIFE, 10).add(Aspect.DESIRE, 15).add(Aspect.MAGIC, 10));
        registerAspects("minecraft:enchanted_golden_apple", new AspectList()
                .add(Aspect.PLANT, 5).add(Aspect.LIFE, 20).add(Aspect.DESIRE, 30).add(Aspect.MAGIC, 30));

        // ==================== Mob Drops ====================
        
        registerAspects("minecraft:leather", new AspectList()
                .add(Aspect.BEAST, 5).add(Aspect.PROTECT, 3));
        registerAspects("minecraft:feather", new AspectList()
                .add(Aspect.BEAST, 3).add(Aspect.FLIGHT, 8).add(Aspect.AIR, 3));
        registerAspects("minecraft:bone", new AspectList()
                .add(Aspect.DEATH, 5).add(Aspect.UNDEAD, 5));
        registerAspects("minecraft:rotten_flesh", new AspectList()
                .add(Aspect.DEATH, 5).add(Aspect.UNDEAD, 5).add(Aspect.BEAST, 3));
        registerAspects("minecraft:spider_eye", new AspectList()
                .add(Aspect.BEAST, 5).add(Aspect.SENSES, 5).add(Aspect.DEATH, 3));
        registerAspects("minecraft:string", new AspectList()
                .add(Aspect.BEAST, 3).add(Aspect.TRAP, 3));
        registerAspects("minecraft:slime_ball", new AspectList()
                .add(Aspect.WATER, 5).add(Aspect.LIFE, 5).add(Aspect.ALCHEMY, 5));
        registerAspects("minecraft:ender_pearl", new AspectList()
                .add(Aspect.ELDRITCH, 10).add(Aspect.MOTION, 10).add(Aspect.EXCHANGE, 5));
        registerAspects("minecraft:blaze_rod", new AspectList()
                .add(Aspect.FIRE, 15).add(Aspect.ENERGY, 10));
        registerAspects("minecraft:ghast_tear", new AspectList()
                .add(Aspect.SOUL, 10).add(Aspect.SENSES, 10).add(Aspect.AVERSION, 5));
        registerAspects("minecraft:nether_star", new AspectList()
                .add(Aspect.ELDRITCH, 30).add(Aspect.ENERGY, 30).add(Aspect.AURA, 30).add(Aspect.FLIGHT, 30));
        registerAspects("minecraft:gunpowder", new AspectList()
                .add(Aspect.FIRE, 10).add(Aspect.ENTROPY, 10).add(Aspect.ALCHEMY, 5));
        registerAspects("minecraft:ink_sac", new AspectList()
                .add(Aspect.WATER, 3).add(Aspect.DARKNESS, 5).add(Aspect.SENSES, 3));
        registerAspects("minecraft:glow_ink_sac", new AspectList()
                .add(Aspect.WATER, 3).add(Aspect.LIGHT, 8).add(Aspect.SENSES, 5));

        // ==================== Tools & Weapons ====================
        
        registerAspects("minecraft:iron_sword", new AspectList()
                .add(Aspect.METAL, 15).add(Aspect.AVERSION, 10).add(Aspect.TOOL, 5));
        registerAspects("minecraft:iron_pickaxe", new AspectList()
                .add(Aspect.METAL, 22).add(Aspect.TOOL, 10));
        registerAspects("minecraft:iron_axe", new AspectList()
                .add(Aspect.METAL, 22).add(Aspect.TOOL, 10));
        registerAspects("minecraft:bow", new AspectList()
                .add(Aspect.PLANT, 5).add(Aspect.AVERSION, 10).add(Aspect.TOOL, 5));
        registerAspects("minecraft:arrow", new AspectList()
                .add(Aspect.AVERSION, 3).add(Aspect.FLIGHT, 3));
        registerAspects("minecraft:fishing_rod", new AspectList()
                .add(Aspect.TOOL, 5).add(Aspect.WATER, 5).add(Aspect.BEAST, 3));
        registerAspects("minecraft:shears", new AspectList()
                .add(Aspect.METAL, 10).add(Aspect.TOOL, 8));
        registerAspects("minecraft:flint_and_steel", new AspectList()
                .add(Aspect.METAL, 5).add(Aspect.FIRE, 10).add(Aspect.TOOL, 5));

        // ==================== Books & Knowledge ====================
        
        registerAspects("minecraft:book", new AspectList()
                .add(Aspect.MIND, 8).add(Aspect.PLANT, 5));
        registerAspects("minecraft:paper", new AspectList()
                .add(Aspect.MIND, 3).add(Aspect.PLANT, 3));
        registerAspects("minecraft:enchanted_book", new AspectList()
                .add(Aspect.MIND, 10).add(Aspect.MAGIC, 15).add(Aspect.PLANT, 5));
        registerAspects("minecraft:writable_book", new AspectList()
                .add(Aspect.MIND, 10).add(Aspect.PLANT, 5));
        registerAspects("minecraft:written_book", new AspectList()
                .add(Aspect.MIND, 15).add(Aspect.PLANT, 5));

        // ==================== Magic Items ====================
        
        registerAspects("minecraft:experience_bottle", new AspectList()
                .add(Aspect.MIND, 10).add(Aspect.ENERGY, 15).add(Aspect.MAGIC, 5));
        registerAspects("minecraft:potion", new AspectList()
                .add(Aspect.WATER, 5).add(Aspect.ALCHEMY, 5).add(Aspect.MAGIC, 5));
        registerAspects("minecraft:beacon", new AspectList()
                .add(Aspect.AURA, 30).add(Aspect.LIGHT, 20).add(Aspect.MAGIC, 30).add(Aspect.CRYSTAL, 20));
        registerAspects("minecraft:totem_of_undying", new AspectList()
                .add(Aspect.LIFE, 30).add(Aspect.SOUL, 20).add(Aspect.MAGIC, 20));
        registerAspects("minecraft:enchanting_table", new AspectList()
                .add(Aspect.MAGIC, 30).add(Aspect.MIND, 20).add(Aspect.CRYSTAL, 15).add(Aspect.DESIRE, 15));

        // ==================== Nether Items ====================
        
        registerAspects("minecraft:netherrack", new AspectList()
                .add(Aspect.EARTH, 3).add(Aspect.FIRE, 3));
        registerAspects("minecraft:soul_sand", new AspectList()
                .add(Aspect.EARTH, 3).add(Aspect.SOUL, 5).add(Aspect.TRAP, 3));
        registerAspects("minecraft:soul_soil", new AspectList()
                .add(Aspect.EARTH, 3).add(Aspect.SOUL, 8));
        registerAspects("minecraft:magma_block", new AspectList()
                .add(Aspect.EARTH, 3).add(Aspect.FIRE, 10));
        registerAspects("minecraft:nether_wart", new AspectList()
                .add(Aspect.PLANT, 5).add(Aspect.MAGIC, 5).add(Aspect.ALCHEMY, 5));

        // ==================== End Items ====================
        
        registerAspects("minecraft:end_stone", new AspectList()
                .add(Aspect.EARTH, 5).add(Aspect.ELDRITCH, 3));
        registerAspects("minecraft:chorus_fruit", new AspectList()
                .add(Aspect.PLANT, 5).add(Aspect.ELDRITCH, 5).add(Aspect.EXCHANGE, 5));
        registerAspects("minecraft:dragon_breath", new AspectList()
                .add(Aspect.ELDRITCH, 15).add(Aspect.AVERSION, 10).add(Aspect.MAGIC, 10));
        registerAspects("minecraft:dragon_egg", new AspectList()
                .add(Aspect.ELDRITCH, 50).add(Aspect.BEAST, 30).add(Aspect.LIFE, 30).add(Aspect.MAGIC, 30));
        registerAspects("minecraft:elytra", new AspectList()
                .add(Aspect.FLIGHT, 50).add(Aspect.AIR, 30).add(Aspect.MAGIC, 30).add(Aspect.ELDRITCH, 20));
        registerAspects("minecraft:shulker_shell", new AspectList()
                .add(Aspect.ELDRITCH, 10).add(Aspect.PROTECT, 10).add(Aspect.VOID, 10));

        // ==================== Misc ====================
        
        registerAspects("minecraft:egg", new AspectList()
                .add(Aspect.BEAST, 5).add(Aspect.LIFE, 8));
        registerAspects("minecraft:milk_bucket", new AspectList()
                .add(Aspect.BEAST, 5).add(Aspect.LIFE, 8).add(Aspect.METAL, 10));
        registerAspects("minecraft:bucket", new AspectList()
                .add(Aspect.METAL, 15).add(Aspect.VOID, 5));
        registerAspects("minecraft:clock", new AspectList()
                .add(Aspect.METAL, 15).add(Aspect.MECHANISM, 10).add(Aspect.DESIRE, 5));
        registerAspects("minecraft:compass", new AspectList()
                .add(Aspect.METAL, 15).add(Aspect.MECHANISM, 10).add(Aspect.SENSES, 5));
        registerAspects("minecraft:name_tag", new AspectList()
                .add(Aspect.BEAST, 5).add(Aspect.SOUL, 8).add(Aspect.MIND, 5));
        registerAspects("minecraft:saddle", new AspectList()
                .add(Aspect.BEAST, 10).add(Aspect.MOTION, 10).add(Aspect.TOOL, 5));
        registerAspects("minecraft:lead", new AspectList()
                .add(Aspect.BEAST, 5).add(Aspect.TRAP, 5));
        registerAspects("minecraft:music_disc_13", new AspectList()
                .add(Aspect.SENSES, 15).add(Aspect.AIR, 10).add(Aspect.DESIRE, 10));
    }

    /**
     * Helper method to register aspects for an item by string ID.
     */
    private static void registerAspects(String itemId, AspectList aspects) {
        AspectHelper.registerObjectTag(ResourceLocation.parse(itemId), aspects);
    }

    /**
     * Register smelting bonus for an item.
     * 
     * @param input ResourceLocation of the input item
     * @param bonuses Array of possible bonus items
     */
    public static void registerSmeltingBonus(ResourceLocation input, ItemStack... bonuses) {
        if (input != null && bonuses != null && bonuses.length > 0) {
            smeltingBonus.put(input.toString(), bonuses);
        }
    }

    /**
     * Get smelting bonus for an item.
     * 
     * @param input The input item
     * @return Array of bonus items, or null if none
     */
    public static ItemStack[] getSmeltingBonus(ItemStack input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        ResourceLocation itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(input.getItem());
        if (itemId == null) {
            return null;
        }
        return smeltingBonus.get(itemId.toString());
    }

    /**
     * Initialize smelting bonuses (bonus drops from infernal furnace).
     */
    public static void initSmeltingBonuses() {
        // Iron ore gives nuggets as bonus
        registerSmeltingBonus(ResourceLocation.parse("minecraft:raw_iron"),
                new ItemStack(Items.IRON_NUGGET, 1));
        registerSmeltingBonus(ResourceLocation.parse("minecraft:iron_ore"),
                new ItemStack(Items.IRON_NUGGET, 1));
        
        // Gold ore gives nuggets as bonus
        registerSmeltingBonus(ResourceLocation.parse("minecraft:raw_gold"),
                new ItemStack(Items.GOLD_NUGGET, 1));
        registerSmeltingBonus(ResourceLocation.parse("minecraft:gold_ore"),
                new ItemStack(Items.GOLD_NUGGET, 1));
        
        // Copper ore gives extra copper
        registerSmeltingBonus(ResourceLocation.parse("minecraft:raw_copper"),
                new ItemStack(Items.COPPER_INGOT, 1));
        
        // Wood gives charcoal and ash (when ash item is added)
        // TODO: Add ash when Thaumcraft items are implemented
    }
}




