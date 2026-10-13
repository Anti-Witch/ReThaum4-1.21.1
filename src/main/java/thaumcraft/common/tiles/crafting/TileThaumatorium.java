package thaumcraft.common.tiles.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectSource;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.lib.crafting.CrucibleRecipeType;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.menu.ThaumatoriumMenu;
import thaumcraft.common.tiles.TileThaumcraftInventory;
import thaumcraft.common.tiles.devices.TileBellows;
import thaumcraft.init.ModBlockEntities;

import javax.annotation.Nullable;
import java.util.List;

/**
 * TileThaumatorium - An automated alchemical crafting machine.
 * 
 * Uses essentia from connected sources to craft alchemy recipes.
 * Features:
 * - Pulls essentia from connected tubes/jars
 * - Stores multiple aspect types internally
 * - Crafts alchemy recipes automatically when catalyst is inserted
 * - Can be sped up with bellows
 * 
 * Slots:
 * - 0: Input/catalyst slot
 * - 1: Output slot
 * 
 * Ported from 1.12.2
 */
public class TileThaumatorium extends TileThaumcraftInventory implements IAspectSource, IEssentiaTransport, MenuProvider {

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int MAX_ESSENTIA = 64;

    // Stored essentia for crafting
    private AspectList storedAspects = new AspectList();

    // Current crafting state
    public boolean crafting = false;
    public int craftingProgress = 0;
    public int craftingTime = 100;

    // Recipe being crafted
    @Nullable
    private AspectList recipeAspects = null;
    @Nullable
    private ItemStack recipeResult = null;

    // Animation (client-side)
    public float rotation = 0;
    public float rotationLast = 0;

    // Tick counter
    private int tickCount = 0;

    public TileThaumatorium(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 2);
    }

    public TileThaumatorium(BlockPos pos, BlockState state) {
        this(ModBlockEntities.THAUMATORIUM.get(), pos, state);
    }

    // ==================== NBT ====================

    @Override
    protected void writeSyncNBT(CompoundTag tag) {
        super.writeSyncNBT(tag);
        storedAspects.writeToNBT(tag, "StoredAspects");
        tag.putBoolean("Crafting", crafting);
        tag.putInt("CraftingProgress", craftingProgress);
        tag.putInt("CraftingTime", craftingTime);

        if (recipeAspects != null) {
            recipeAspects.writeToNBT(tag, "RecipeAspects");
        }
        if (recipeResult != null && !recipeResult.isEmpty()) {
            CompoundTag resultTag = new CompoundTag();
            recipeResult.save(getRegistryProvider(), resultTag);
            tag.put("RecipeResult", resultTag);
        }
    }

    @Override
    protected void readSyncNBT(CompoundTag tag) {
        super.readSyncNBT(tag);
        storedAspects = new AspectList();
        storedAspects.readFromNBT(tag, "StoredAspects");
        crafting = tag.getBoolean("Crafting");
        craftingProgress = tag.getInt("CraftingProgress");
        craftingTime = tag.getInt("CraftingTime");

        if (tag.contains("RecipeAspects")) {
            recipeAspects = new AspectList();
            recipeAspects.readFromNBT(tag, "RecipeAspects");
        } else {
            recipeAspects = null;
        }

        if (tag.contains("RecipeResult")) {
            recipeResult = ItemStack.parseOptional(getRegistryProvider(), tag.getCompound("RecipeResult"));
        } else {
            recipeResult = null;
        }
    }

    // ==================== Tick ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileThaumatorium tile) {
        tile.tickCount++;

        // Pull essentia from connected sources periodically
        if (tile.tickCount % 10 == 0) {
            tile.pullEssentia();
        }

        // Process crafting
        if (tile.crafting && tile.recipeAspects != null && tile.recipeResult != null) {
            int bellowsBonus = tile.getBellowsBonus();
            tile.craftingProgress += 1 + bellowsBonus;

            if (tile.craftingProgress >= tile.craftingTime) {
                tile.finishCrafting();
            }
        } else if (!tile.crafting) {
            // Check if we can start crafting
            tile.checkAndStartCrafting();
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileThaumatorium tile) {
        tile.rotationLast = tile.rotation;

        if (tile.crafting) {
            tile.rotation += 5.0f + tile.getBellowsBonus() * 2.0f;
        } else if (tile.rotation % 90 != 0) {
            // Slowly align to 90 degree intervals when idle
            tile.rotation += 1.0f;
        }

        if (tile.rotation >= 360.0f) {
            tile.rotation -= 360.0f;
            tile.rotationLast -= 360.0f;
        }
    }

    /**
     * Try to pull essentia from connected sources.
     */
    private void pullEssentia() {
        if (level == null) return;

        for (Direction dir : Direction.values()) {
            if (dir == Direction.UP) continue; // Don't pull from above

            // Check if we need more of any aspect
            for (Aspect aspect : Aspect.aspects.values()) {
                if (storedAspects.getAmount(aspect) >= MAX_ESSENTIA) continue;

                BlockEntity te = level.getBlockEntity(worldPosition.relative(dir));
                if (te instanceof IEssentiaTransport transport) {
                    if (!transport.canOutputTo(dir.getOpposite())) continue;

                    Aspect available = transport.getEssentiaType(dir.getOpposite());
                    if (available != null && transport.getSuctionAmount(dir.getOpposite()) < getSuctionAmount(dir)) {
                        int taken = transport.takeEssentia(available, 1, dir.getOpposite());
                        if (taken > 0) {
                            storedAspects.add(available, taken);
                            markDirtyAndSync();
                            return; // Only pull one per tick
                        }
                    }
                } else if (te instanceof IAspectSource source) {
                    AspectList available = source.getAspects();
                    for (Aspect a : available.getAspects()) {
                        if (storedAspects.getAmount(a) < MAX_ESSENTIA && available.getAmount(a) > 0) {
                            if (source.takeFromContainer(a, 1)) {
                                storedAspects.add(a, 1);
                                markDirtyAndSync();
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Check if we can start crafting with the current input item.
     */
    private void checkAndStartCrafting() {
        if (level == null) return;

        ItemStack input = getItem(INPUT_SLOT);
        ItemStack output = getItem(OUTPUT_SLOT);

        if (input.isEmpty()) return;
        if (!output.isEmpty() && output.getCount() >= output.getMaxStackSize()) return;

        // Look up actual crucible recipes that use this item as catalyst
        List<CrucibleRecipeType> recipes = ThaumcraftCraftingManager.findCrucibleRecipesForCatalyst(input, level);
        for (CrucibleRecipeType recipe : recipes) {
            AspectList required = recipe.getAspects();
            if (required == null || required.size() == 0) continue;

            ItemStack result = recipe.getResultItem();
            if (result == null || result.isEmpty()) continue;

            // Check output can accept result
            if (!output.isEmpty() && !(ItemStack.isSameItem(output, result)
                    && output.getCount() + result.getCount() <= output.getMaxStackSize())) {
                continue;
            }

            // Check if we have enough essentia
            if (storedAspects.contains(required)) {
                // Start crafting
                recipeAspects = required.copy();
                recipeResult = result.copy();
                crafting = true;
                craftingProgress = 0;
                craftingTime = 100 - getBellowsBonus() * 10;

                // Consume input
                input.shrink(1);

                // Consume essentia
                for (Aspect a : recipeAspects.getAspects()) {
                    storedAspects.remove(a, recipeAspects.getAmount(a));
                }

                markDirtyAndSync();

                // Play start sound
                level.playSound(null, worldPosition, SoundEvents.ENCHANTMENT_TABLE_USE,
                        SoundSource.BLOCKS, 0.5f, 1.0f);
                return;
            }
        }
    }

    /**
     * Finish the current crafting operation.
     */
    private void finishCrafting() {
        if (level == null || recipeResult == null) return;

        ItemStack output = getItem(OUTPUT_SLOT);
        if (output.isEmpty()) {
            setItem(OUTPUT_SLOT, recipeResult.copy());
        } else if (ItemStack.isSameItem(output, recipeResult)) {
            output.grow(recipeResult.getCount());
        }

        // Reset crafting state
        crafting = false;
        craftingProgress = 0;
        recipeAspects = null;
        recipeResult = null;

        markDirtyAndSync();

        // Play completion sound
        level.playSound(null, worldPosition, SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.BLOCKS, 0.5f, 1.0f + level.random.nextFloat() * 0.2f);
    }

    /**
     * Get speed bonus from adjacent bellows.
     */
    private int getBellowsBonus() {
        if (level == null) return 0;
        return TileBellows.getBellows(level, worldPosition, 
                new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST});
    }

    // ==================== IAspectSource ====================

    @Override
    public AspectList getAspects() {
        return storedAspects.copy();
    }

    @Override
    public void setAspects(AspectList aspects) {
        this.storedAspects = aspects.copy();
        markDirtyAndSync();
    }

    @Override
    public int addToContainer(Aspect tag, int amt) {
        if (amt == 0) return amt;

        int current = storedAspects.getAmount(tag);
        int canAdd = Math.min(amt, MAX_ESSENTIA - current);

        if (canAdd > 0) {
            storedAspects.add(tag, canAdd);
            markDirtyAndSync();
        }

        return amt - canAdd;
    }

    @Override
    public boolean takeFromContainer(Aspect tag, int amt) {
        if (storedAspects.getAmount(tag) >= amt) {
            storedAspects.remove(tag, amt);
            markDirtyAndSync();
            return true;
        }
        return false;
    }

    @Override
    public boolean takeFromContainer(AspectList list) {
        if (storedAspects.contains(list)) {
            for (Aspect a : list.getAspects()) {
                storedAspects.remove(a, list.getAmount(a));
            }
            markDirtyAndSync();
            return true;
        }
        return false;
    }

    @Override
    public boolean doesContainerContainAmount(Aspect tag, int amt) {
        return storedAspects.getAmount(tag) >= amt;
    }

    @Override
    public boolean doesContainerContain(AspectList list) {
        return storedAspects.contains(list);
    }

    @Override
    public int containerContains(Aspect tag) {
        return storedAspects.getAmount(tag);
    }

    @Override
    public boolean doesContainerAccept(Aspect tag) {
        return storedAspects.getAmount(tag) < MAX_ESSENTIA;
    }

    // ==================== IEssentiaTransport ====================

    @Override
    public boolean isConnectable(Direction face) {
        return face != Direction.UP;
    }

    @Override
    public boolean canInputFrom(Direction face) {
        return face != Direction.UP;
    }

    @Override
    public boolean canOutputTo(Direction face) {
        return false; // Thaumatorium doesn't output essentia
    }

    @Override
    public void setSuction(Aspect aspect, int amount) {
        // Fixed suction
    }

    @Override
    public Aspect getSuctionType(Direction face) {
        return null; // Will accept any aspect
    }

    @Override
    public int getSuctionAmount(Direction face) {
        return face != Direction.UP ? 64 : 0;
    }

    @Override
    public Aspect getEssentiaType(Direction face) {
        return null;
    }

    @Override
    public int getEssentiaAmount(Direction face) {
        return 0;
    }

    @Override
    public int takeEssentia(Aspect aspect, int amount, Direction face) {
        return 0; // Can't take from thaumatorium
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, Direction face) {
        if (canInputFrom(face)) {
            return amount - addToContainer(aspect, amount);
        }
        return 0;
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    @Override
    public boolean isBlocked() {
        return false;
    }

    // ==================== Getters ====================

    public AspectList getStoredAspects() {
        return storedAspects;
    }

    public float getCraftingProgress() {
        return craftingTime > 0 ? (float) craftingProgress / craftingTime : 0;
    }

    public boolean isCrafting() {
        return crafting;
    }

    @Nullable
    public ItemStack getRecipeResult() {
        return recipeResult;
    }

    // ==================== MenuProvider ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.thaumcraft.thaumatorium");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ThaumatoriumMenu(containerId, playerInventory, this);
    }
}




