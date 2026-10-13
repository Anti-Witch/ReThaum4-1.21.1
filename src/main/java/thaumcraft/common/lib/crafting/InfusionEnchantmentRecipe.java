package thaumcraft.common.lib.crafting;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.RecipeMatcher;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.crafting.IThaumcraftRecipe;
import thaumcraft.init.ModRecipeSerializers;
import thaumcraft.init.ModRecipeTypes;

import java.util.List;
import java.util.Map;

/**
 * Infusion enchantment recipe - TC4 {@code InfusionEnchantmentRecipe}.
 *
 * Unlike a normal infusion recipe this does not create a new item: it infuses a
 * <b>vanilla enchantment</b> into the item sitting on the central pedestal.
 * The enchantment is applied when the infusion completes and the whole process
 * additionally consumes experience levels from players standing near the altar
 * (see {@code TileInfusionMatrix}).
 *
 * The recipe is data driven ({@code data/<namespace>/recipe/infusion_enchantment/*.json}):
 * <pre>
 * {
 *   "type": "thaumcraft:infusion_enchantment",
 *   "enchantment": "minecraft:sharpness",
 *   "research": "INFUSIONENCHANTMENT",
 *   "instability": 2,
 *   "aspects": { "praecantatio": 4, "telum": 8 },
 *   "components": [ { "item": "minecraft:iron_sword" }, { "item": "thaumcraft:salis_mundus" } ]
 * }
 * </pre>
 */
public class InfusionEnchantmentRecipe implements Recipe<RecipeInput>, IThaumcraftRecipe {

    /** The enchantment that is infused onto the central item. */
    private final Holder<Enchantment> enchantment;

    /** Base essentia cost (before the per-level modifier is applied). */
    private final AspectList aspects;

    /** Research key required to use this recipe. */
    private final String research;

    /** Items that have to be placed on the pedestals. */
    private final NonNullList<Ingredient> components;

    /** Base instability of the recipe. */
    private final int instability;

    /** Experience cost of a single level, TC4: max(1, minEnchantability(1) / 3). */
    private final int recipeXP;

    public InfusionEnchantmentRecipe(Holder<Enchantment> enchantment, String research, int instability,
                                     AspectList aspects, NonNullList<Ingredient> components) {
        this.enchantment = enchantment;
        this.research = research == null ? "" : research;
        this.instability = instability;
        this.aspects = aspects == null ? new AspectList() : aspects;
        this.components = components == null ? NonNullList.create() : components;
        this.recipeXP = Math.max(1, enchantment.value().getMinCost(1) / 3);
    }

    // ==================== Recipe ====================

    @Override
    public boolean matches(RecipeInput container, Level level) {
        // Infusion matching is handled by matchesInfusion()
        return false;
    }

    /**
     * Checks if this recipe matches the current infusion altar state.
     *
     * @param pedestalItems the items on the pedestals
     * @param centralItem the item on the central pedestal
     * @param level the world
     * @param player the player performing the infusion
     * @return true if the recipe can be crafted
     */
    public boolean matchesInfusion(List<ItemStack> pedestalItems, ItemStack centralItem,
                                   Level level, Player player) {
        if (centralItem == null || centralItem.isEmpty() || player == null) {
            return false;
        }

        if (!research.isEmpty() && !ThaumcraftCapabilities.isResearchKnown(player, research)) {
            return false;
        }

        Enchantment ench = enchantment.value();

        // The item must be a valid target for this enchantment
        if (!ench.canEnchant(centralItem)) {
            return false;
        }

        // Enchantment already at max level?
        ItemEnchantments existing = centralItem.getEnchantments();
        if (existing.getLevel(enchantment) >= ench.getMaxLevel()) {
            return false;
        }

        // Must be compatible with every enchantment already on the item
        for (Holder<Enchantment> other : existing.keySet()) {
            if (!Enchantment.areCompatible(enchantment, other)) {
                return false;
            }
        }

        return RecipeMatcher.findMatches(pedestalItems, components) != null;
    }

    @Override
    public ItemStack assemble(RecipeInput container, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.INFUSION_ENCHANTMENT.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.INFUSION.get();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return components;
    }

    @Override
    public String getGroup() {
        return "infusion_enchantment";
    }

    // ==================== TC4 logic ====================

    /**
     * TC4: instability of the recipe grows with the amount of enchantments
     * already present on the target item.
     */
    public int calcInstability(ItemStack recipeInput) {
        int i = 0;
        for (Map.Entry<Holder<Enchantment>, Integer> entry : recipeInput.getEnchantments().entrySet()) {
            i += entry.getValue();
        }
        return (i / 2) + instability;
    }

    /**
     * TC4: each already applied level of this enchantment makes the next one
     * cost more experience.
     */
    public int calcXP(ItemStack recipeInput) {
        return recipeXP * (1 + recipeInput.getEnchantments().getLevel(enchantment));
    }

    /**
     * TC4: essentia cost modifier - the current level of this enchantment plus
     * a tenth of every other enchantment level on the item.
     */
    public float getEssentiaMod(ItemStack recipeInput) {
        ItemEnchantments existing = recipeInput.getEnchantments();
        float mod = existing.getLevel(enchantment);
        for (Map.Entry<Holder<Enchantment>, Integer> entry : existing.entrySet()) {
            if (!entry.getKey().equals(enchantment)) {
                mod += entry.getValue() * 0.1f;
            }
        }
        return mod;
    }

    /**
     * Builds the finished item (central item with one more level of the
     * enchantment). Used for previews; the altar applies the enchantment itself
     * when the crafting finishes.
     */
    public ItemStack getRecipeOutput(Player player, ItemStack input, List<ItemStack> components) {
        if (input == null || input.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack out = input.copy();
        EnchantmentHelper.updateEnchantments(out, mutable ->
                mutable.set(enchantment, Math.min(enchantment.value().getMaxLevel(),
                        mutable.getLevel(enchantment) + 1)));
        return out;
    }

    // ==================== Getters ====================

    public Holder<Enchantment> getEnchantment() {
        return enchantment;
    }

    /**
     * Registry key of the infused enchantment (used for serialization).
     */
    public ResourceKey<Enchantment> getEnchantmentKey() {
        return enchantment.unwrapKey().orElseThrow(() ->
                new IllegalStateException("Infusion enchantment recipe holds a direct (unregistered) enchantment"));
    }

    public AspectList getAspects() {
        return aspects;
    }

    @Override
    public String getResearch() {
        return research;
    }

    public NonNullList<Ingredient> getComponents() {
        return components;
    }

    public int getInstability() {
        return instability;
    }

    public int getRecipeXP() {
        return recipeXP;
    }
}
