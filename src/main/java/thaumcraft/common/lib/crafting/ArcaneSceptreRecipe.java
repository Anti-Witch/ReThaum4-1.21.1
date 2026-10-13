package thaumcraft.common.lib.crafting;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.crafting.IArcaneWorkbench;
import thaumcraft.api.wands.WandRod;
import thaumcraft.common.items.wands.WandItem;
import thaumcraft.init.ModItems;
import thaumcraft.init.ModDataComponents;
import thaumcraft.init.ModRecipeSerializers;
import thaumcraft.init.ModRecipeTypes;

/**
 * ArcaneSceptreRecipe - TC4's special arcane recipe for the sceptre variant of
 * a wand: three equal caps at (1,0), (2,1) and (0,2), a rod at (1,1) and a
 * primal charm at (2,0); every other grid slot empty.
 * <p>
 * Ported from OpenTC4 common/lib/crafting/ArcaneSceptreRecipe (TC4):
 * - requires the SCEPTRE research
 * - vis cost is wand cost * 1.5 (of every primal aspect)
 * - the resulting wand is flagged as a sceptre (+50% capacity, cheaper costs)
 */
public class ArcaneSceptreRecipe extends ArcaneWandRecipe {

    @Override
    public boolean matches(IArcaneWorkbench inv, Level level) {
        if (invSize(inv) < 9) {
            return false;
        }
        if (!emptyExcept(inv, 1, 0, 2, 1, 0, 2, 1, 1, 2, 0)) {
            return false;
        }
        ItemStack cap1 = inv.getItem(cell(1, 0));
        ItemStack cap2 = inv.getItem(cell(2, 1));
        ItemStack cap3 = inv.getItem(cell(0, 2));
        ItemStack rod = inv.getItem(cell(1, 1));
        ItemStack charm = inv.getItem(cell(2, 0));
        if (!isPrimalCharm(charm)) {
            return false;
        }
        if (!ItemStack.isSameItemSameComponents(cap1, cap2)
                || !ItemStack.isSameItemSameComponents(cap1, cap3)) {
            return false;
        }
        return matchWandParts(cap1, cap2, rod);
    }

    /**
     * TC4 checks for itemResource meta 15 (primal charm); the port split TC4's
     * resource item into separate items.
     */
    private boolean isPrimalCharm(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.PRIMAL_CHARM.get());
    }

    @Override
    public ItemStack assemble(IArcaneWorkbench inv) {
        ItemStack cap1 = inv.getItem(cell(1, 0));
        ItemStack rod = inv.getItem(cell(1, 1));
        if (!matchWandParts(cap1, cap1, rod)) {
            return ItemStack.EMPTY;
        }
        ItemStack out = new ItemStack(ModItems.WAND.get());
        WandItem.setRod(out, findRodTag(rod));
        WandItem.setCap(out, findCapTag(cap1));
        out.set(ModDataComponents.WAND_SCEPTRE.get(), true);
        return out;
    }

    @Override
    public AspectList costForGrid(IArcaneWorkbench inv) {
        return costForGrid(inv, inv.getItem(cell(1, 0)), inv.getItem(cell(1, 1)), 1.5F);
    }

    @Override
    public boolean researchOk(IArcaneWorkbench inv, Player player) {
        // SCEPTRE is checked through getResearch() by the crafting manager
        return super.researchOk(inv, player);
    }

    @Override
    public String getResearch() {
        return "SCEPTRE";
    }
}
