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
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.crafting.IArcaneWorkbench;
import thaumcraft.api.wands.WandCap;
import thaumcraft.api.wands.WandRod;
import thaumcraft.common.items.wands.WandItem;
import thaumcraft.init.ModItems;
import thaumcraft.init.ModRecipeSerializers;
import thaumcraft.init.ModRecipeTypes;

/**
 * ArcaneWandRecipe - TC4's special arcane recipe for assembling a wand in the
 * Arcane Workbench: a cap in the top-right (2,0) and bottom-left (0,2) corner,
 * a rod in the centre (1,1); every other grid slot must be empty.
 * <p>
 * Ported from OpenTC4 common/lib/crafting/ArcaneWandRecipe (TC4):
 * - output is a wand carrying the rod/cap data components
 * - the vis cost is {@code capCraftCost * rodCraftCost} of every primal aspect,
 *   paid from the wand in the wand slot
 * - requires the CAP_x and ROD_x research
 * - the basic wood + iron wand is excluded (crafted with a normal recipe)
 */
public class ArcaneWandRecipe implements IArcaneRecipe {

    @Override
    public boolean matches(IArcaneWorkbench inv, Level level) {
        if (invSize(inv) < 9) {
            return false;
        }
        if (!emptyExcept(inv, 0, 2, 2, 0, 1, 1)) {
            return false;
        }
        return matchWandParts(inv.getItem(cell(0, 2)), inv.getItem(cell(2, 0)), inv.getItem(cell(1, 1)));
    }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        return false;
    }

    // ==================== Shared helpers (also used by the sceptre recipe) ====================

    protected static int cell(int x, int y) {
        return x + y * 3;
    }

    protected static int invSize(IArcaneWorkbench inv) {
        return inv.getContainerSize();
    }

    /**
     * True when every grid slot except the given (x,y) cells is empty.
     */
    protected static boolean emptyExcept(IArcaneWorkbench inv, int... cells) {
        outer:
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                int slot = cell(x, y);
                for (int i = 0; i + 1 < cells.length; i += 2) {
                    if (cells[i] == x && cells[i + 1] == y) {
                        continue outer;
                    }
                }
                if (!inv.getItem(slot).isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * TC4 checkMatch: both caps equal, cap and rod known.
     */
    protected boolean matchWandParts(ItemStack cap1, ItemStack cap2, ItemStack rod) {
        if (cap1.isEmpty() || cap2.isEmpty() || rod.isEmpty()) {
            return false;
        }
        if (!ItemStack.isSameItemSameComponents(cap1, cap2)) {
            return false;
        }
        String capTag = findCapTag(cap1);
        String rodTag = findRodTag(rod);
        if (capTag == null || rodTag == null) {
            return false;
        }
        // starter wand is a normal crafting recipe (TC4)
        return !("wood".equals(rodTag) && "iron".equals(capTag));
    }

    protected static String findCapTag(ItemStack stack) {
        for (WandCap cap : WandCap.caps.values()) {
            if (ItemStack.isSameItemSameComponents(stack, cap.getItem())) {
                return cap.getTag();
            }
        }
        return null;
    }

    protected static String findRodTag(ItemStack stack) {
        for (WandRod rod : WandRod.rods.values()) {
            if (ItemStack.isSameItemSameComponents(stack, rod.getItem())) {
                return rod.getTag();
            }
        }
        return null;
    }

    protected static boolean researchComplete(Player player, String capTag, String rodTag) {
        if (player == null) {
            return true;
        }
        return ThaumcraftCapabilities.isResearchComplete(player, "CAP_" + capTag)
                && ThaumcraftCapabilities.isResearchComplete(player, "ROD_" + rodTag);
    }

    /**
     * The wand pays {@code capCraftCost * rodCraftCost} of every primal aspect
     * (OpenTC4 ArcaneWandRecipe.getAspects), scaled by {@code factor} (1.0 for
     * wands, 1.5 for sceptres).
     */
    public static AspectList costForGrid(IArcaneWorkbench inv, ItemStack capStack, ItemStack rodStack, float factor) {
        AspectList al = new AspectList();
        int cc = -1;
        int cr = -1;
        for (WandCap cap : WandCap.caps.values()) {
            if (ItemStack.isSameItemSameComponents(capStack, cap.getItem())) {
                cc = cap.getCraftCost();
                break;
            }
        }
        for (WandRod rod : WandRod.rods.values()) {
            if (ItemStack.isSameItemSameComponents(rodStack, rod.getItem())) {
                cr = rod.getCraftCost();
                break;
            }
        }
        if (cc >= 0 && cr >= 0) {
            int cost = (int) ((float) (cc * cr) * factor);
            for (Aspect aspect : Aspect.getPrimalAspects()) {
                al.add(aspect, cost);
            }
        }
        return al;
    }

    // ==================== IArcaneRecipe ====================

    public ItemStack assemble(IArcaneWorkbench inv) {
        ItemStack cap1 = inv.getItem(cell(0, 2));
        ItemStack cap2 = inv.getItem(cell(2, 0));
        ItemStack rod = inv.getItem(cell(1, 1));
        if (!matchWandParts(cap1, cap2, rod)) {
            return ItemStack.EMPTY;
        }
        ItemStack out = new ItemStack(ModItems.WAND.get());
        WandItem.setRod(out, findRodTag(rod));
        WandItem.setCap(out, findCapTag(cap1));
        return out;
    }

    @Override
    public ItemStack assemble(IArcaneWorkbench inv, HolderLookup.Provider provider) {
        return assemble(inv);
    }

    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider provider) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        // The real result depends on the grid; JEI/recipe displays use a base wand
        return new ItemStack(ModItems.WAND.get());
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(IArcaneWorkbench inv) {
        return NonNullList.withSize(9, ItemStack.EMPTY);
    }

    /**
     * CAP_x / ROD_x research gate for the grid contents (TC4 checkMatch).
     */
    public boolean researchOk(IArcaneWorkbench inv, Player player) {
        return researchComplete(player,
                findCapTag(inv.getItem(cell(0, 2))),
                findRodTag(inv.getItem(cell(1, 1))));
    }

    /**
     * The vis cost depends on the grid; the result slot asks via
     * {@link #costForGrid}.
     */
    @Override
    public AspectList getCrystals() {
        return new AspectList();
    }

    public AspectList costForGrid(IArcaneWorkbench inv) {
        return costForGrid(inv, inv.getItem(cell(0, 2)), inv.getItem(cell(1, 1)), 1.0F);
    }

    @Override
    public int getVis() {
        return 0;
    }

    @Override
    public String getResearch() {
        return "";
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.create();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.ARCANE_WORKBENCH_SHAPED.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.ARCANE_WORKBENCH.get();
    }
}
