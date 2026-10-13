package thaumcraft.api;

import java.util.Arrays;
import java.util.Map;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import thaumcraft.api.aspects.AspectHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.crafting.IThaumcraftRecipe;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.crafting.Part;
import thaumcraft.api.internal.CommonInternals;
import thaumcraft.api.internal.DummyInternalMethodHandler;
import thaumcraft.api.internal.IInternalMethodHandler;
import thaumcraft.api.internal.WeightedRandomLoot;

/**
 * Main API class for Thaumcraft.
 * Provides methods for addon mods to register recipes, aspects, warp, and more.
 * 
 * IMPORTANT: If you are adding your own aspects to items it is a good idea to do it 
 * AFTER Thaumcraft adds its aspects, otherwise odd things may happen.
 * 
 * @author Azanor
 * Ported to 1.20.1
 */
public class ThaumcraftApi {
    
    /**
     * Calling methods from this will only work properly once Thaumcraft is past the FMLCommonSetupEvent phase.
     * This is used to access the various methods described in IInternalMethodHandler.
     * @see IInternalMethodHandler
     */
    public static IInternalMethodHandler internalMethods = new DummyInternalMethodHandler();
    
    // ==================== RESEARCH ==================== 
    
    /**
     * <i><b>Important</b>: This must be called <b>before</b> the mod loading completes.</i>
     * Allows you to register the location of a json file in your assets folder that contains your research. 
     * For example: "thaumcraft:research/basics"
     * @param loc the resourcelocation of the json file
     */
    public static void registerResearchLocation(ResourceLocation loc) {
        if (!CommonInternals.jsonLocs.containsKey(loc.toString())) {
            CommonInternals.jsonLocs.put(loc.toString(), loc);
        }
    }
    
    // ==================== RECIPES ====================
    
    /**
     * Infernal furnace smelting bonus entry.
     */
    public static class SmeltBonus {
        public Object in;
        public ItemStack out;
        public float chance;
        
        public SmeltBonus(Object in, ItemStack out, float chance) {
            this.in = in;
            this.out = out;
            this.chance = chance;
        }
    }
    
    /**
     * This method is used to determine what bonus items are generated when the infernal furnace smelts items.
     * @param in The input of the smelting operation. Can either be an ItemStack or a tag string (e.g. "c:ores/gold")
     * @param out The bonus item that can be produced from the smelting operation.
     * @param chance the base chance of the item being produced as a bonus. Default value is .33f
     */
    public static void addSmeltingBonus(Object in, ItemStack out, float chance) {
        if (in instanceof ItemStack || in instanceof String) {
            CommonInternals.smeltingBonus.add(new SmeltBonus(in, out, chance));
        }
    }
    
    public static void addSmeltingBonus(Object in, ItemStack out) {
        if (in instanceof ItemStack || in instanceof String) {
            CommonInternals.smeltingBonus.add(new SmeltBonus(in, out, 0.33f));
        }
    }
    
    public static Map<ResourceLocation, IThaumcraftRecipe> getCraftingRecipes() {
        return CommonInternals.craftingRecipeCatalog;
    }
    
    public static Map<ResourceLocation, Object> getCraftingRecipesFake() {
        return CommonInternals.craftingRecipeCatalogFake;
    }
    
    /**
     * This adds recipes to the 'fake' recipe catalog. These recipes won't be craftable, but are useful for display 
     * in the thaumonomicon if they are dynamic recipes like infusion enchantment or runic infusion.
     * @param registry the recipe ID
     * @param recipe the recipe object
     */
    public static void addFakeCraftingRecipe(ResourceLocation registry, Object recipe) {
        getCraftingRecipesFake().put(registry, recipe);
    }
    
    /**
     * Use this method to add a multiblock blueprint recipe to the thaumcraft recipe catalog. 
     * This is used for display purposes in the thaumonomicon.
     * @param registry unique identifier for this recipe
     * @param recipe the blueprint recipe
     */
    public static void addMultiblockRecipeToCatalog(ResourceLocation registry, BluePrint recipe) {
        getCraftingRecipes().put(registry, recipe);
    }
    
    /**
     * Blueprint recipe for multiblock structures.
     */
    public static class BluePrint implements IThaumcraftRecipe {
        Part[][][] parts;
        String research;
        ItemStack displayStack;
        ItemStack[] ingredientList;
        private String group;
        
        public BluePrint(String research, Part[][][] parts, ItemStack... ingredientList) {
            this.parts = parts;
            this.research = research;
            this.ingredientList = ingredientList;
        }
        
        public BluePrint(String research, ItemStack display, Part[][][] parts, ItemStack... ingredientList) {
            this.parts = parts;
            this.research = research;
            this.displayStack = display;
            this.ingredientList = ingredientList;
        }
        
        public Part[][][] getParts() {
            return parts;
        }
        
        @Override
        public String getResearch() {
            return research;
        }
        
        /**
         * The items needed to craft this block - used for listing in the thaumonomicon 
         * and does not influence the actual recipe.
         */
        public ItemStack[] getIngredientList() {
            return ingredientList;
        }
        
        /**
         * This stack will be displayed instead of multipart object - used for recipe 
         * bookmark display in thaumonomicon only.
         */
        public ItemStack getDisplayStack() {
            return displayStack;
        }
        
        /**
         * Get the recipe group for thaumonomicon display.
         */
        public String getGroup() {
            return group;
        }
        
        public BluePrint setGroup(ResourceLocation loc) {
            group = loc.toString();
            return this;
        }
    }
    
    /**
     * Add an arcane crafting recipe to the catalog.
     * In 1.20.1, recipes should be registered via JSON or datapack, but this method 
     * allows programmatic registration for backwards compatibility.
     * 
     * @param registry unique identifier for this recipe
     * @param recipe the arcane recipe
     */
    public static void addArcaneCraftingRecipe(ResourceLocation registry, IArcaneRecipe recipe) {
        getCraftingRecipes().put(registry, recipe);
    }
    
    /**
     * Add an infusion crafting recipe to the catalog.
     * @param registry unique identifier for this recipe
     * @param recipe the infusion recipe
     */
    public static void addInfusionCraftingRecipe(ResourceLocation registry, InfusionRecipe recipe) {
        getCraftingRecipes().put(registry, recipe);
    }
    
    /**
     * Get an infusion recipe by its result.
     * @param res the recipe result
     * @return the recipe, or null if not found
     */
    public static InfusionRecipe getInfusionRecipe(ItemStack res) {
        for (Object r : getCraftingRecipes().values()) {
            if (r instanceof InfusionRecipe infusionRecipe) {
                if (infusionRecipe.getRecipeOutput() instanceof ItemStack output) {
                    if (ItemStack.isSameItem(output, res)) {
                        return infusionRecipe;
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Add a crucible recipe to the catalog.
     * @param registry unique identifier for this recipe
     * @param recipe the crucible recipe
     */
    public static void addCrucibleRecipe(ResourceLocation registry, CrucibleRecipe recipe) {
        getCraftingRecipes().put(registry, recipe);
    }
    
    /**
     * Get a crucible recipe by its result.
     * @param stack the recipe result
     * @return the recipe, or null if not found
     */
    public static CrucibleRecipe getCrucibleRecipe(ItemStack stack) {
        for (Object r : getCraftingRecipes().values()) {
            if (r instanceof CrucibleRecipe crucibleRecipe) {
                if (ItemStack.isSameItem(crucibleRecipe.getRecipeOutput(), stack)) {
                    return crucibleRecipe;
                }
            }
        }
        return null;
    }
    
    /**
     * Get a crucible recipe by its hash.
     * @param hash the unique recipe code
     * @return the recipe, or null if not found
     */
    public static CrucibleRecipe getCrucibleRecipeFromHash(int hash) {
        for (Object recipe : getCraftingRecipes().values()) {
            if (recipe instanceof CrucibleRecipe crucibleRecipe && crucibleRecipe.hash == hash) {
                return crucibleRecipe;
            }
        }
        return null;
    }
    
    // ==================== ASPECTS ====================
    
    /**
     * Checks to see if the passed item/block already has aspects associated with it.
     * @param item the item to check
     * @return true if aspects have been registered
     */
    public static boolean exists(ItemStack item) {
        if (item == null || item.isEmpty()) return false;
        return AspectHelper.getObjectAspects(item) != null;
    }
    
    /**
     * Used to assign aspects to the given item/block.
     * Example: ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.COBBLESTONE), 
     *          (new AspectList()).add(Aspect.ENTROPY, 1).add(Aspect.EARTH, 1));
     * 
     * @param item the item to register
     * @param aspects the aspects to associate
     */
    public static void registerObjectTag(ItemStack item, AspectList aspects) {
        if (item == null || item.isEmpty()) return;
        if (aspects == null) aspects = new AspectList();
        AspectHelper.registerObjectTag(item, aspects);
    }
    
    /**
     * Used to assign aspects to the given item tag. 
     * @param tagName the tag name (e.g. "c:ingots/iron")
     * @param aspects A AspectList of the associated aspects
     */
    public static void registerObjectTag(String tagName, AspectList aspects) {
        try {
            ResourceLocation tagLoc = ResourceLocation.parse(tagName);
            net.minecraft.tags.TagKey<Item> tagKey = net.minecraft.tags.ItemTags.create(tagLoc);
            net.minecraft.core.registries.BuiltInRegistries.ITEM.getTag(tagKey)
                    .ifPresent(tag -> tag.forEach(holder -> registerObjectTag(new ItemStack(holder.value()), aspects.copy())));
        } catch (Exception e) {}
    }

    /**
     * Used to assign aspects to the given item/block with automatic generation from recipes.
     * IMPORTANT - this should only be used if you are not happy with the default aspects 
     * the object would be assigned.
     * 
     * @param item the item to register
     * @param aspects additional aspects to add
     */
    public static void registerComplexObjectTag(ItemStack item, AspectList aspects) {
        if (item == null || item.isEmpty()) return;
        if (aspects == null) aspects = new AspectList();
        
        if (!exists(item)) {
            AspectList tmp = AspectHelper.getObjectAspects(item);
            if (tmp != null && tmp.size() > 0) {
                for (var tag : tmp.getAspects()) {
                    aspects.add(tag, tmp.getAmount(tag));
                }
            }
            registerObjectTag(item, aspects);
        } else {
            AspectList tmp = AspectHelper.getObjectAspects(item);
            if (tmp == null) tmp = new AspectList();
            for (var tag : aspects.getAspects()) {
                tmp.merge(tag, aspects.getAmount(tag));
            }
            registerObjectTag(item, tmp);
        }
    }

    public static void registerComplexObjectTag(String tagName, AspectList aspects) {
        try {
            ResourceLocation tagLoc = ResourceLocation.parse(tagName);
            net.minecraft.tags.TagKey<Item> tagKey = net.minecraft.tags.ItemTags.create(tagLoc);
            net.minecraft.core.registries.BuiltInRegistries.ITEM.getTag(tagKey)
                    .ifPresent(tag -> tag.forEach(holder -> registerComplexObjectTag(new ItemStack(holder.value()), aspects.copy())));
        } catch (Exception e) {}
    }
    
    /**
     * NBT tag filter for entity aspect registration.
     */
    public static class EntityTagsNBT {
        public String name;
        public Object value;
        
        public EntityTagsNBT(String name, Object value) {
            this.name = name;
            this.value = value;
        }
    }
    
    /**
     * Entity aspect registration entry.
     */
    public static class EntityTags {
        public String entityName;
        public EntityTagsNBT[] nbts;
        public AspectList aspects;
        
        public EntityTags(String entityName, AspectList aspects, EntityTagsNBT... nbts) {
            this.entityName = entityName;
            this.nbts = nbts;
            this.aspects = aspects;
        }
    }
    
    /**
     * This is used to add aspects to entities which you can then scan using a thaumometer.
     * Also used to calculate vis drops from mobs.
     * 
     * @param entityName the entity's registry name (e.g., "minecraft:zombie")
     * @param aspects the aspects to associate
     * @param nbt optional NBT filters to differentiate mob variants
     */
    public static void registerEntityTag(String entityName, AspectList aspects, EntityTagsNBT... nbt) {
        CommonInternals.scanEntities.add(new EntityTags(entityName, aspects, nbt));
        if (nbt == null || nbt.length == 0) {
            try {
                AspectHelper.registerEntityTag(ResourceLocation.parse(entityName), aspects);
            } catch (Exception e) {}
        }
    }
    
    // ==================== WARP ====================
    
    /**
     * This method is used to determine how much warp is gained if the item is crafted. 
     * The warp added is "sticky" warp.
     * @param craftresult The item crafted
     * @param amount how much warp is gained
     */
    public static void addWarpToItem(ItemStack craftresult, int amount) {
        if (craftresult == null || craftresult.isEmpty()) return;
        CommonInternals.warpMap.put(
            Arrays.asList(craftresult.getItem(), craftresult.getDamageValue()), 
            amount
        );
    }
    
    /**
     * Returns how much warp is gained from the item passed in.
     * @param in the item to check
     * @return how much warp it will give
     */
    public static int getWarp(ItemStack in) {
        if (in == null || in.isEmpty()) return 0;
        Integer warp = CommonInternals.warpMap.get(
            Arrays.asList(in.getItem(), in.getDamageValue())
        );
        return warp != null ? warp : 0;
    }
    
    // ==================== LOOT BAGS ====================
    
    /**
     * Used to add possible loot to treasure bags. As a reference, the weight of gold coins are 2000 
     * and a diamond is 50.
     * The weights are the same for all loot bag types - the only difference is how many items the bag contains.
     * 
     * @param item the item to add
     * @param weight the weight (higher = more common)
     * @param bagTypes array of which type of bag to add this loot to. Multiple types can be specified.
     *                 0 = common, 1 = uncommon, 2 = rare
     */
    public static void addLootBagItem(ItemStack item, int weight, int... bagTypes) {
        if (bagTypes == null || bagTypes.length == 0) {
            WeightedRandomLoot.lootBagCommon.add(new WeightedRandomLoot(item, weight));
        } else {
            for (int rarity : bagTypes) {
                switch (rarity) {
                    case 0 -> WeightedRandomLoot.lootBagCommon.add(new WeightedRandomLoot(item, weight));
                    case 1 -> WeightedRandomLoot.lootBagUncommon.add(new WeightedRandomLoot(item, weight));
                    case 2 -> WeightedRandomLoot.lootBagRare.add(new WeightedRandomLoot(item, weight));
                }
            }
        }
    }
    
    // ==================== CROPS ====================
    
    /**
     * This method is used to register an item that will act as a seed for the specified block.
     * If your seed items use IPlantable it might not be necessary to do this as we 
     * attempt to automatically detect such links.
     * 
     * @param block the crop block
     * @param seed the seed item
     */
    public static void registerSeed(Block block, ItemStack seed) {
        CommonInternals.seedList.put(block.getDescriptionId(), seed);
    }
    
    /**
     * Get the registered seed for a block.
     * @param block the crop block
     * @return the seed item, or null if not registered
     */
    public static ItemStack getSeed(Block block) {
        return CommonInternals.seedList.get(block.getDescriptionId());
    }
    
    // ==================== FMLInterModComms DOCUMENTATION ====================
    
    /**
     * To define mod crops you need to use FMLInterModComms (IMC) messages.
     * There are two 'types' of crops you can add: Standard crops and clickable crops.
     * 
     * Standard crops work like normal vanilla crops - they grow until a certain state
     * and you harvest them by destroying the block and collecting the drops.
     * 
     * Clickable crops are crops that you right click to gather their bounty instead of destroying them.
     * 
     * Example usage:
     * InterModComms.sendTo("thaumcraft", "harvestStandardCrop", () -> new ItemStack(myBlock));
     * InterModComms.sendTo("thaumcraft", "harvestClickableCrop", () -> new ItemStack(myBlock));
     * InterModComms.sendTo("thaumcraft", "harvestStackedCrop", () -> new ItemStack(myBlock));
     */
    
    /**
     * PORTABLE HOLE BLACKLIST
     * You can blacklist blocks that may not be portable holed through using IMC messages.
     * 
     * Simply add the mod and block name with a 'modid:blockname' designation.
     * Example: InterModComms.sendTo("thaumcraft", "portableHoleBlacklist", () -> "minecraft:obsidian");
     * 
     * You can also specify blockstates by adding ';' delimited 'name=value' pairs.
     * Example: "thaumcraft:log;variant=greatwood;variant=silverwood"
     */
    
    /**
     * NATIVE CLUSTERS
     * You can define certain ores that will have a chance to produce native clusters via IMC messages.
     * Format: "[ore item id],[cluster item id],[chance modifier float]"
     * 
     * The chance modifier is a multiplier applied to the default chance for that cluster 
     * to be produced (default 27.5% for a pickaxe of the core)
     */
    
    /**
     * LAMP OF GROWTH BLACKLIST
     * You can blacklist crops that should not be affected by the Lamp of Growth via IMC messages.
     * 
     * Example: InterModComms.sendTo("thaumcraft", "lampBlacklist", () -> new ItemStack(myCrop));
     */
    
    /**
     * DIMENSION BLACKLIST
     * You can blacklist a dimension to not spawn certain thaumcraft features via IMC messages.
     * Format: "[dimension_id]:[level]"
     * 
     * Level values:
     * [0] stop all TC spawning and generation
     * [1] allow ore and node generation (and node special features)
     * [2] allow mob spawning
     * [3] allow ore and node gen + mob spawning (and node special features)
     * 
     * Example: InterModComms.sendTo("thaumcraft", "dimensionBlacklist", () -> "minecraft:the_nether:1");
     */
    
    /**
     * BIOME BLACKLIST
     * You can blacklist a biome to not spawn certain thaumcraft features via IMC messages.
     * Format: "[biome_id]:[level]"
     * Uses the same level values as dimension blacklist.
     * 
     * Example: InterModComms.sendTo("thaumcraft", "biomeBlacklist", () -> "minecraft:ocean:2");
     */
    
    /**
     * CHAMPION MOB WHITELIST
     * You can whitelist an entity class so it can rarely spawn champion versions via IMC messages.
     * The entity must extend Monster.
     * Format: "[Entity Registry Name]:[level]"
     * 
     * The [level] value indicates how rare the champion version will be - the higher the number 
     * the more common. The number roughly equals the [n] in 100 chance of a mob being a champion version.
     * 
     * Example: InterModComms.sendTo("thaumcraft", "championWhiteList", () -> "minecraft:zombie:2");
     */
}





