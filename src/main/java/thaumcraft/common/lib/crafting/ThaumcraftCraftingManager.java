package thaumcraft.common.lib.crafting;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.crafting.IArcaneWorkbench;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.init.ModRecipeTypes;

import java.util.List;

/**
 * ThaumcraftCraftingManager - Handles recipe lookup for Thaumcraft crafting.
 * 
 * Provides methods to find matching recipes for:
 * - Arcane Workbench (arcane recipes)
 * - Crucible (alchemy recipes)
 * - Infusion Altar (infusion recipes)
 */
public class ThaumcraftCraftingManager {

    // Aspect cap for generated tags
    public static final int ASPECT_CAP = 500;

    // Cached references for recipe-driven aspect generation.
    // Populated by WorldEvents.captureRecipeData() when worlds load.
    private static RecipeManager cachedRecipeManager;
    private static RegistryAccess registryAccess;

    /**
     * TC4 craftingSpecial: special arcane recipes that are not data-driven
     * (wand and sceptre assembly).
     */
    private static final List<IArcaneRecipe> SPECIAL_ARCANE_RECIPES = List.of(
            new thaumcraft.common.lib.crafting.ArcaneSceptreRecipe(),
            new thaumcraft.common.lib.crafting.ArcaneWandRecipe()
    );

    /**
     * Called by WorldEvents to provide the recipe manager / registry access
     * for a freshly loaded world. Used by generateTags() to walk recipes
     * and synthesize aspect lists for items without manual aspect entries.
     */
    public static void captureRecipeData(Level level) {
        if (level instanceof ServerLevel server) {
            cachedRecipeManager = server.getRecipeManager();
            registryAccess = server.registryAccess();
        }
    }

    /**
     * Called by WorldEvents when the world unloads to release the cached refs.
     */
    public static void clearRecipeData() {
        cachedRecipeManager = null;
        registryAccess = null;
    }

    private static RecipeManager getRecipeManager() {
        if (cachedRecipeManager != null) return cachedRecipeManager;
        return null;
    }
    
    // ==================== Arcane Workbench Recipes ====================
    
    /**
     * Find a matching arcane recipe for the given crafting matrix.
     * 
     * @param matrix The crafting container (3x3 grid + 6 crystal slots)
     * @param player The player crafting
     * @return The matching arcane recipe, or null if none found
     */
    public static IArcaneRecipe findMatchingArcaneRecipe(CraftingContainer matrix, Player player) {
        if (player == null || matrix == null) {
            return null;
        }
        
        // The matrix must implement IArcaneWorkbench for arcane recipes
        if (!(matrix instanceof IArcaneWorkbench workbench)) {
            return null;
        }
        
        Level level = player.level();

        // TC4 craftingSpecial: wand and sceptre assembly (not data-driven recipes)
        for (IArcaneRecipe special : SPECIAL_ARCANE_RECIPES) {
            if (special.matches(workbench, level)) {
                if (special instanceof thaumcraft.common.lib.crafting.ArcaneWandRecipe wandRecipe
                        && !wandRecipe.researchOk(workbench, player)) {
                    continue;
                }
                String specialResearch = special.getResearch();
                if (specialResearch != null && !specialResearch.isEmpty()
                        && !ThaumcraftCapabilities.isResearchKnown(player, specialResearch)) {
                    continue;
                }
                return special;
            }
        }
        
        // Search through all arcane workbench recipes
        for (RecipeHolder<?> holder : level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.ARCANE_WORKBENCH.get())) {
            Recipe<?> recipe = holder.value();
            if (recipe instanceof IArcaneRecipe arcaneRecipe) {
                // Check if the recipe matches
                if (arcaneRecipe.matches(workbench, level)) {
                    // Check research requirement
                    String research = arcaneRecipe.getResearch();
                    if (research != null && !research.isEmpty()) {
                        if (!ThaumcraftCapabilities.isResearchKnown(player, research)) {
                            continue;
                        }
                    }
                    return arcaneRecipe;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Find the result of a matching arcane recipe.
     * 
     * @param matrix The crafting container
     * @param player The player crafting
     * @return The result ItemStack, or empty if no recipe matches
     */
    public static ItemStack findMatchingArcaneRecipeResult(CraftingContainer matrix, Player player) {
        IArcaneRecipe recipe = findMatchingArcaneRecipe(matrix, player);
        if (recipe != null && matrix instanceof IArcaneWorkbench workbench) {
            return recipe.assemble(workbench, player.level().registryAccess());
        }
        return ItemStack.EMPTY;
    }
    
    /**
     * Get the crystal requirements for a matching arcane recipe.
     * 
     * @param matrix The crafting container
     * @param player The player crafting
     * @return The AspectList of required crystals, or null if no recipe matches
     */
    public static AspectList findMatchingArcaneRecipeCrystals(CraftingContainer matrix, Player player) {
        IArcaneRecipe recipe = findMatchingArcaneRecipe(matrix, player);
        return recipe != null ? recipe.getCrystals() : null;
    }
    
    /**
     * Get the vis cost for a matching arcane recipe.
     * 
     * @param matrix The crafting container
     * @param player The player crafting
     * @return The vis cost, or 0 if no recipe matches
     */
    public static int findMatchingArcaneRecipeVis(CraftingContainer matrix, Player player) {
        IArcaneRecipe recipe = findMatchingArcaneRecipe(matrix, player);
        return recipe != null ? recipe.getVis() : 0;
    }

    /**
     * Get the per-aspect vis cost for a matching arcane recipe.
     * This is used by the Arcane Workbench GUI to display aspect indicators.
     * Returns the crystals AspectList which specifies per-aspect costs.
     *
     * @param matrix The crafting container
     * @param player The player crafting
     * @return The AspectList of per-aspect vis costs, or null if no recipe matches
     */
    public static AspectList findMatchingArcaneRecipeAspects(CraftingContainer matrix, Player player) {
        IArcaneRecipe recipe = findMatchingArcaneRecipe(matrix, player);
        return recipe != null ? recipe.getCrystals() : null;
    }
    
    // ==================== Crucible Recipes ====================
    
    /**
     * Find a matching crucible recipe for the given catalyst and aspects.
     * 
     * @param crucibleAspects The aspects currently in the crucible
     * @param catalyst The item being thrown in
     * @param player The player crafting
     * @param level The world
     * @return The matching crucible recipe, or null if none found
     */
    public static CrucibleRecipeType findMatchingCrucibleRecipe(AspectList crucibleAspects, 
                                                                  ItemStack catalyst, 
                                                                  Player player, 
                                                                  Level level) {
        if (catalyst.isEmpty() || level == null) {
            return null;
        }
        
        for (RecipeHolder<?> holder : level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.CRUCIBLE.get())) {
            Recipe<?> recipe = holder.value();
            if (recipe instanceof CrucibleRecipeType crucibleRecipe) {
                // Check if the recipe matches
                if (crucibleRecipe.matchesCrucible(crucibleAspects, catalyst)) {
                    // Check research requirement
                    String research = crucibleRecipe.getResearch();
                    if (research != null && !research.isEmpty() && player != null) {
                        if (!ThaumcraftCapabilities.isResearchKnown(player, research)) {
                            continue;
                        }
                    }
                    return crucibleRecipe;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Find all crucible recipes that use the given catalyst.
     * Useful for displaying possible recipes to the player.
     * 
     * @param catalyst The catalyst item
     * @param level The world
     * @return List of matching recipes
     */
    public static List<CrucibleRecipeType> findCrucibleRecipesForCatalyst(ItemStack catalyst, Level level) {
        return level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.CRUCIBLE.get())
                .stream()
                .map(RecipeHolder::value)
                .filter(recipe -> recipe instanceof CrucibleRecipeType)
                .map(recipe -> (CrucibleRecipeType) recipe)
                .filter(recipe -> recipe.catalystMatches(catalyst))
                .toList();
    }
    
    // ==================== Infusion Recipes ====================
    
    /**
     * Find a matching infusion recipe for the given altar state.
     * 
     * @param pedestalItems Items on the pedestals
     * @param centralItem The item on the runic matrix
     * @param player The player crafting
     * @param level The world
     * @return The matching infusion recipe, or null if none found
     */
    public static InfusionRecipeType findMatchingInfusionRecipe(List<ItemStack> pedestalItems,
                                                                  ItemStack centralItem,
                                                                  Player player,
                                                                  Level level) {
        if (centralItem.isEmpty() || level == null || player == null) {
            return null;
        }
        
        for (RecipeHolder<?> holder : level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.INFUSION.get())) {
            Recipe<?> recipe = holder.value();
            if (recipe instanceof InfusionRecipeType infusionRecipe) {
                // Research gating (must know the research to craft)
                if (infusionRecipe instanceof thaumcraft.api.crafting.IThaumcraftRecipe tr
                        && tr.getResearch() != null && !tr.getResearch().isEmpty()) {
                    if (!ThaumcraftCapabilities.isResearchKnown(player, tr.getResearch())) {
                        continue;
                    }
                }
                if (infusionRecipe.matchesInfusion(pedestalItems, centralItem, level, player)) {
                    return infusionRecipe;
                }
            }
        }
        
        return null;
    }
    
    // ==================== Object Tag Generation ====================
    
    /**
     * Get the aspects of an item.
     * This checks multiple sources in order:
     * 1. Exact match with NBT
     * 2. Match ignoring NBT
     * 3. Wildcard damage values
     * 4. Generated from recipes
     * 
     * @param stack The item stack
     * @return The AspectList for this item, never null
     */
    public static AspectList getObjectTags(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return new AspectList();
        }
        
        // First, try direct lookup via AspectHelper
        AspectList aspects = AspectHelper.getObjectAspects(stack);
        
        // If not found, check if it's an essentia container
        if (aspects == null && stack.getItem() instanceof IEssentiaContainerItem container) {
            if (!container.ignoreContainedAspects()) {
                aspects = container.getAspects(stack);
            }
        }
        
        // If still not found, generate from recipes
        if (aspects == null) {
            aspects = generateTags(stack);
        }
        
        // Add bonus aspects from enchantments, tools, armor, etc.
        aspects = getBonusTags(stack, aspects);
        
        // Cap aspects to prevent ridiculous values
        return capAspects(aspects, ASPECT_CAP);
    }
    
    /**
     * Register aspects for an item.
     * 
     * @param stack The item stack pattern
     * @param aspects The aspects to register
     */
    public static void registerObjectAspects(ItemStack stack, AspectList aspects) {
        if (stack != null && !stack.isEmpty() && aspects != null) {
            AspectHelper.registerObjectTag(stack, aspects);
        }
    }
    
    /**
     * Generate aspects for an item by analyzing its crafting recipes.
     * This is called for items that don't have manually registered aspects.
     * 
     * @param stack The item stack to generate tags for
     * @return The generated AspectList
     */
    public static AspectList generateTags(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return new AspectList();
        }

        // First check the AspectHelper's manual aspect list (already registered aspects win)
        AspectList manual = AspectHelper.getObjectAspects(stack);
        if (manual != null && manual.size() > 0) {
            return manual;
        }

        // Synthesize aspects from the first matching recipe that produces this item
        AspectList generated = generateTagsFromRecipes(stack);
        if (generated != null && generated.size() > 0) {
            return generated;
        }

        // Fallback: minimal aspect so items always have at least one tag
        AspectList fallback = new AspectList();
        fallback.add(Aspect.ENTROPY, 1);
        return fallback;
    }

    /**
     * Walks all recipes and returns an AspectList synthesised from the
     * first recipe whose result matches the given item. Ingredient aspects
     * are summed and divided by the output count. Returns null if no match.
     */
    private static AspectList generateTagsFromRecipes(ItemStack stack) {
        RecipeManager rm = getRecipeManager();
        if (rm == null) return null;

        for (RecipeHolder<?> holder : rm.getRecipes()) {
            Recipe<?> recipe = holder.value();
            ItemStack result = recipe.getResultItem(registryAccess);
            if (!ItemStack.isSameItem(result, stack)) continue;
            if (result.getCount() <= 0) continue;

            AspectList summed = new AspectList();
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient.isEmpty()) continue;
                for (ItemStack ingStack : ingredient.getItems()) {
                    if (ingStack == null || ingStack.isEmpty()) continue;
                    AspectList ingAspects = AspectHelper.getObjectAspects(ingStack);
                    if (ingAspects == null) continue;
                    for (Aspect a : ingAspects.getAspects()) {
                        if (a == null) continue;
                        summed.add(a, ingAspects.getAmount(a));
                    }
                }
            }

            int divisor = Math.max(1, result.getCount());
            AspectList result1 = new AspectList();
            for (Aspect a : summed.getAspects()) {
                if (a == null) continue;
                int amt = summed.getAmount(a) / divisor;
                if (amt > 0) result1.add(a, amt);
            }
            return result1;
        }
        return null;
    }
    
    /**
     * Add bonus aspects based on item properties (enchantments, tool type, etc.)
     */
    private static AspectList getBonusTags(ItemStack stack, AspectList sourceTags) {
        AspectList result = new AspectList();
        
        if (stack == null || stack.isEmpty()) {
            return result;
        }
        
        // Copy source tags if present
        if (sourceTags != null) {
            for (Aspect aspect : sourceTags.getAspects()) {
                if (aspect != null) {
                    result.add(aspect, sourceTags.getAmount(aspect));
                }
            }
        }
        
        // Add aspects based on item type
        net.minecraft.world.item.Item item = stack.getItem();
        
        // Armor
        if (item instanceof net.minecraft.world.item.ArmorItem armorItem) {
            int defense = armorItem.getDefense();
            if (defense > 0) {
                result.merge(Aspect.PROTECT, defense * 4);
            }
        }
        
        // Swords
        if (item instanceof net.minecraft.world.item.SwordItem swordItem) {
            float damage = swordItem.getDamage(stack);
            if (damage > 0) {
                result.merge(Aspect.AVERSION, (int)(damage * 4));
            }
        }
        
        // Bows
        if (item instanceof net.minecraft.world.item.BowItem) {
            result.merge(Aspect.AVERSION, 10);
            result.merge(Aspect.FLIGHT, 5);
        }
        
        // Tools
        if (item instanceof net.minecraft.world.item.TieredItem tieredItem) {
            net.minecraft.world.item.Tier tier = tieredItem.getTier();
            int tierLevel;
            if (tier == net.minecraft.world.item.Tiers.NETHERITE) tierLevel = 4;
            else if (tier == net.minecraft.world.item.Tiers.DIAMOND) tierLevel = 3;
            else if (tier == net.minecraft.world.item.Tiers.IRON) tierLevel = 2;
            else if (tier == net.minecraft.world.item.Tiers.STONE) tierLevel = 1;
            else tierLevel = 0;
            result.merge(Aspect.TOOL, (tierLevel + 1) * 4);
        }

        // Enchantments add aspects
        var enchantments = stack.getEnchantments();
        if (enchantments != null && !enchantments.isEmpty()) {
            // Each enchantment adds some magic aspect
            result.merge(Aspect.MAGIC, enchantments.size() * 3);
        }

        // Food
        var food = stack.get(net.minecraft.core.component.DataComponents.FOOD);
        if (food != null) {
            result.merge(Aspect.LIFE, food.nutrition());
            if (food.saturation() > 0.5f) {
                result.merge(Aspect.DESIRE, 2);
            }
        }
        
        return result;
    }
    
    /**
     * Cap aspect amounts to prevent extreme values.
     */
    private static AspectList capAspects(AspectList source, int maxAmount) {
        if (source == null) {
            return new AspectList();
        }
        
        AspectList result = new AspectList();
        for (Aspect aspect : source.getAspects()) {
            if (aspect != null) {
                int amount = Math.min(maxAmount, source.getAmount(aspect));
                result.add(aspect, amount);
            }
        }
        return result;
    }
}




