package thaumcraft.common.menu.slot;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.RecipeCraftingHolder;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.common.CommonHooks;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.crafting.IArcaneWorkbench;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.tiles.crafting.TileArcaneWorkbench;

/**
 * ArcaneWorkbenchResultSlot - The output slot for arcane workbench crafting.
 * 
 * Handles:
 * - Consuming crafting ingredients
 * - Consuming vis from the wand in the wand slot
 * - Firing crafting events
 */
public class ArcaneWorkbenchResultSlot extends Slot {
    
    private final CraftingContainer craftMatrix;
    private final Player player;
    private int amountCrafted;
    
    public ArcaneWorkbenchResultSlot(TileArcaneWorkbench tile, Player player, 
            CraftingContainer craftMatrix, Container resultContainer, int slot, int x, int y) {
        super(resultContainer, slot, x, y);
        this.player = player;
        this.craftMatrix = craftMatrix;
    }
    
    @Override
    public boolean mayPlace(ItemStack stack) {
        // Cannot place items in the result slot
        return false;
    }
    
    @Override
    public ItemStack remove(int amount) {
        if (hasItem()) {
            amountCrafted += Math.min(amount, getItem().getCount());
        }
        return super.remove(amount);
    }
    
    @Override
    protected void onQuickCraft(ItemStack stack, int amount) {
        amountCrafted += amount;
        checkTakeAchievements(stack);
    }
    
    @Override
    protected void onSwapCraft(int numItems) {
        amountCrafted += numItems;
    }
    
    @Override
    protected void checkTakeAchievements(ItemStack stack) {
        if (amountCrafted > 0) {
            stack.onCraftedBy(player.level(), player, amountCrafted);
            net.neoforged.neoforge.event.EventHooks.firePlayerCraftingEvent(player, stack, craftMatrix);
        }
        
        if (this.container instanceof RecipeCraftingHolder holder) {
            holder.awardUsedRecipes(player, craftMatrix.getItems());
        }

        amountCrafted = 0;
    }
    
    @Override
    public void onTake(Player thePlayer, ItemStack stack) {
        checkTakeAchievements(stack);
        
        // Find the matching recipe
        IArcaneRecipe arcaneRecipe = ThaumcraftCraftingManager.findMatchingArcaneRecipe(craftMatrix, thePlayer);
        
        CommonHooks.setCraftingPlayer(thePlayer);
        
        NonNullList<ItemStack> remainingItems;
        if (arcaneRecipe != null && craftMatrix instanceof IArcaneWorkbench workbench) {
            // Get remaining items from arcane recipe
            remainingItems = arcaneRecipe.getRemainingItems(workbench);
            
            // Consume vis from the wand in the wand slot (slot 9)
            ItemStack wandStack = craftMatrix.getItem(TileArcaneWorkbench.SLOT_WAND);
            if (wandStack.getItem() instanceof thaumcraft.common.items.wands.WandItem wand) {
                // TC4 special recipes (wand/sceptre) derive their cost from the
                // grid; data-driven recipes use their crystals-aspect cost list.
                AspectList cost = (arcaneRecipe instanceof thaumcraft.common.lib.crafting.ArcaneWandRecipe wandRecipe)
                        ? wandRecipe.costForGrid(workbench)
                        : arcaneRecipe.getCrystals();
                if (cost != null && cost.size() > 0) {
                    wand.consumeAllVisCrafting(wandStack, thePlayer, cost, false);
                }
            }
        } else if (arcaneRecipe != null) {
            // Fallback - shouldn't happen but safety check
            remainingItems = NonNullList.withSize(craftMatrix.getContainerSize(), ItemStack.EMPTY);
        } else {
            // Check for vanilla recipe
            var level = thePlayer.level();
            java.util.List<ItemStack> inputItems = new java.util.ArrayList<>();
            for (int i = 0; i < 9; i++) inputItems.add(craftMatrix.getItem(i));
            CraftingInput craftInput = CraftingInput.of(3, 3, inputItems);
            var recipeOpt = level.getRecipeManager()
                    .getRecipeFor(RecipeType.CRAFTING, craftInput, level);

            if (recipeOpt.isPresent()) {
                CraftingRecipe vanillaRecipe = recipeOpt.get().value();
                remainingItems = vanillaRecipe.getRemainingItems(craftInput);
            } else {
                remainingItems = NonNullList.withSize(craftMatrix.getContainerSize(), ItemStack.EMPTY);
            }
        }
        
        CommonHooks.setCraftingPlayer(null);
        
        // Consume ingredients and handle remaining items (buckets, etc.)
        for (int i = 0; i < Math.min(9, remainingItems.size()); i++) {
            ItemStack slotStack = craftMatrix.getItem(i);
            ItemStack remaining = remainingItems.get(i);
            
            if (!slotStack.isEmpty()) {
                craftMatrix.removeItem(i, 1);
                slotStack = craftMatrix.getItem(i);
            }
            
            if (!remaining.isEmpty()) {
                if (slotStack.isEmpty()) {
                    craftMatrix.setItem(i, remaining);
                } else if (ItemStack.isSameItemSameComponents(slotStack, remaining)) {
                    remaining.grow(slotStack.getCount());
                    craftMatrix.setItem(i, remaining);
                } else if (!player.getInventory().add(remaining)) {
                    player.drop(remaining, false);
                }
            }
        }
    }
}




