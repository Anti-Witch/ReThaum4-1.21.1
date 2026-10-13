package thaumcraft.common.lib.enchantment;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import thaumcraft.common.lib.crafting.InfusionEnchantmentRecipe;
import thaumcraft.init.ModRecipeTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * Registry/helper for TC4 style infusion enchantments.
 *
 * In TC4 an "infusion enchantment" is simply a vanilla enchantment that is
 * applied to a tool, weapon or piece of armor through infusion crafting
 * ({@link InfusionEnchantmentRecipe}) instead of the enchanting table.
 *
 * The recipes themselves are data driven and live in the recipe manager, this
 * class is the single place that knows how to find them and how to read/write
 * the enchantment levels on an {@link ItemStack}.
 */
public final class InfusionEnchantment {

    /** TC4 research key that unlocks the infusion enchantment recipes. */
    public static final String RESEARCH = "INFUSIONENCHANTMENT";

    private InfusionEnchantment() {
    }

    // ==================== Recipe registry ====================

    /**
     * All infusion enchantment recipes currently loaded from the data pack.
     */
    public static List<InfusionEnchantmentRecipe> getRecipes(Level level) {
        if (level == null) {
            return List.of();
        }
        List<InfusionEnchantmentRecipe> recipes = new ArrayList<>();
        for (RecipeHolder<?> holder : level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.INFUSION.get())) {
            if (holder.value() instanceof InfusionEnchantmentRecipe recipe) {
                recipes.add(recipe);
            }
        }
        return recipes;
    }

    /**
     * Find the infusion enchantment recipe for the given enchantment, if any.
     */
    public static InfusionEnchantmentRecipe findRecipe(Level level, Holder<Enchantment> enchantment) {
        if (enchantment == null) {
            return null;
        }
        for (InfusionEnchantmentRecipe recipe : getRecipes(level)) {
            if (recipe.getEnchantment().equals(enchantment)) {
                return recipe;
            }
        }
        return null;
    }

    /**
     * Resolve a registered enchantment by its registry key.
     */
    public static Holder<Enchantment> resolve(HolderLookup.Provider registries, ResourceKey<Enchantment> key) {
        return registries.lookupOrThrow(Registries.ENCHANTMENT).get(key).orElse(null);
    }

    // ==================== Item stack helpers ====================

    /**
     * Level of an enchantment on the given stack (0 when it is not present).
     */
    public static int getLevel(ItemStack stack, Holder<Enchantment> enchantment) {
        if (stack == null || stack.isEmpty() || enchantment == null) {
            return 0;
        }
        return EnchantmentHelper.getEnchantmentsForCrafting(stack).getLevel(enchantment);
    }

    public static boolean has(ItemStack stack, Holder<Enchantment> enchantment) {
        return getLevel(stack, enchantment) > 0;
    }

    public static boolean isAtMaxLevel(ItemStack stack, Holder<Enchantment> enchantment) {
        return enchantment != null && getLevel(stack, enchantment) >= enchantment.value().getMaxLevel();
    }

    /**
     * Add one level of the enchantment to the stack (capped at the maximum
     * level). This is what the infusion altar calls when a recipe completes.
     */
    public static void addLevel(ItemStack stack, Holder<Enchantment> enchantment) {
        if (stack == null || stack.isEmpty() || enchantment == null) {
            return;
        }
        EnchantmentHelper.updateEnchantments(stack, mutable ->
                mutable.set(enchantment, Math.min(enchantment.value().getMaxLevel(),
                        mutable.getLevel(enchantment) + 1)));
    }
}
