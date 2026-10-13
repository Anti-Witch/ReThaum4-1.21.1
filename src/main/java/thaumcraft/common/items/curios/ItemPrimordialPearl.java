package thaumcraft.common.items.curios;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import thaumcraft.common.items.ItemTCBase;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Primordial Pearl - A rare artifact that serves as a crafting catalyst.
 *
 * The pearl has 8 durability levels and degrades when used in crafting:
 * - Damage 0-2: Pearl (full appearance)
 * - Damage 3-5: Nodule (partially used)
 * - Damage 6-7: Mote (nearly depleted)
 *
 * When used in recipes, it returns a damaged version of itself until fully consumed.
 */
public class ItemPrimordialPearl extends ItemTCBase {

    public static final int MAX_DAMAGE = 8;

    public ItemPrimordialPearl() {
        super(new Properties()
                .stacksTo(1)
                .rarity(Rarity.EPIC)
                .durability(MAX_DAMAGE));
    }

    /**
     * Get the visual variant based on damage level.
     * Used for model predicates to show different textures.
     * @return 0 = pearl, 1 = nodule, 2 = mote
     */
    public static int getVariant(ItemStack stack) {
        int damage = stack.getDamageValue();
        if (damage < 3) {
            return 0; // Pearl
        } else if (damage < 6) {
            return 1; // Nodule
        } else {
            return 2; // Mote
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        String suffix = switch (getVariant(stack)) {
            case 0 -> "pearl";
            case 1 -> "nodule";
            case 2 -> "mote";
            default -> "pearl";
        };
        return Component.translatable(this.getDescriptionId() + "." + suffix);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        int uses = MAX_DAMAGE - stack.getDamageValue();
        tooltip.add(Component.translatable("item.thaumcraft.primordial_pearl.uses", uses));
    }

    /**
     * Returns the item left in the crafting grid after using this item.
     * The pearl returns a damaged version of itself until fully consumed.
     */
    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        if (!hasCraftingRemainingItem(stack)) {
            return ItemStack.EMPTY;
        }
        ItemStack result = stack.copy();
        result.setDamageValue(stack.getDamageValue() + 1);
        return result;
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        // Returns true if there's still uses left after this use
        return stack.getDamageValue() < MAX_DAMAGE - 1;
    }

    @Override
    public int getBurnTime(ItemStack itemStack, @Nullable RecipeType<?> recipeType) {
        return 0; // Not a fuel
    }

    @Override
    public boolean isRepairable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return false;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return false;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }

    @Override
    public int getEnchantmentValue() {
        return 0;
    }

    public Rarity getRarity(ItemStack stack) {
        // Color based on remaining uses
        return switch (getVariant(stack)) {
            case 0 -> Rarity.EPIC;
            case 1 -> Rarity.RARE;
            case 2 -> Rarity.UNCOMMON;
            default -> Rarity.EPIC;
        };
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        // Only show enchantment glow for fresh pearls
        return stack.getDamageValue() < 3;
    }
}
