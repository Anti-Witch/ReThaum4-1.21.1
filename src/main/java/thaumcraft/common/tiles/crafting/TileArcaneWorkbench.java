package thaumcraft.common.tiles.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.common.menu.ArcaneWorkbenchMenu;
import thaumcraft.common.tiles.TileThaumcraft;
import thaumcraft.init.ModBlockEntities;
import thaumcraft.init.ModBlocks;

import javax.annotation.Nullable;

/**
 * Arcane workbench tile entity - handles vis-based crafting.
 * Stores the crafting grid contents and tracks available vis from the aura.
 */
public class TileArcaneWorkbench extends TileThaumcraft implements MenuProvider {

    // Crafting grid: 9 slots for 3x3 grid + 1 wand slot
    public static final int GRID_SIZE = 9;
    public static final int SLOT_WAND = 9;
    public static final int TOTAL_SLOTS = GRID_SIZE + 1;

    private NonNullList<ItemStack> craftMatrix = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);
    
    // Cached aura vis for crafting calculations
    public int auraVisServer = 0;
    public int auraVisClient = 0;

    public TileArcaneWorkbench(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public TileArcaneWorkbench(BlockPos pos, BlockState state) {
        this(ModBlockEntities.ARCANE_WORKBENCH.get(), pos, state);
    }

    // ==================== NBT ====================

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, craftMatrix, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        craftMatrix = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, craftMatrix, registries);
    }

    @Override
    protected void writeSyncNBT(CompoundTag tag) {
        // Don't sync crafting contents to all clients
    }

    @Override
    protected void readSyncNBT(CompoundTag tag) {
        // Don't sync crafting contents from server
    }

    // ==================== Crafting Matrix ====================

    public ItemStack getStackInSlot(int slot) {
        if (slot >= 0 && slot < TOTAL_SLOTS) {
            return craftMatrix.get(slot);
        }
        return ItemStack.EMPTY;
    }

    public void setStackInSlot(int slot, ItemStack stack) {
        if (slot >= 0 && slot < TOTAL_SLOTS) {
            craftMatrix.set(slot, stack);
            setChanged();
        }
    }

    public ItemStack removeStackFromSlot(int slot) {
        if (slot >= 0 && slot < TOTAL_SLOTS) {
            ItemStack stack = craftMatrix.get(slot);
            craftMatrix.set(slot, ItemStack.EMPTY);
            setChanged();
            return stack;
        }
        return ItemStack.EMPTY;
    }

    public void clearCraftMatrix() {
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            craftMatrix.set(i, ItemStack.EMPTY);
        }
        setChanged();
    }

    // ==================== Aura Interaction ====================

    /**
     * Update the cached aura vis amount.
     * Called periodically or when crafting.
     */
    public void updateAura() {
        if (level == null || level.isClientSide) return;

        int totalVis = 0;
        
        // Check if there's a workbench charger block above
        BlockState above = level.getBlockState(worldPosition.above());
        boolean hasCharger = above.is(ModBlocks.ARCANE_WORKBENCH_CHARGER.get());
        if (hasCharger) {
            int chunkX = worldPosition.getX() >> 4;
            int chunkZ = worldPosition.getZ() >> 4;
            
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    // Get vis from each chunk
                    BlockPos chunkCenter = new BlockPos((chunkX + dx) << 4, worldPosition.getY(), (chunkZ + dz) << 4);
                    totalVis += (int) AuraHelper.getVis(level, chunkCenter);
                }
            }
        } else {
            // Just get vis from current chunk
            totalVis = (int) AuraHelper.getVis(level, worldPosition);
        }

        auraVisServer = totalVis;
    }

    /**
     * Spend vis from the aura for crafting.
     * 
     * @param visCost The amount of vis to spend
     */
    public void spendAura(int visCost) {
        if (level == null || level.isClientSide || visCost <= 0) return;

        BlockState above = level.getBlockState(worldPosition.above());
        boolean hasCharger = above.is(ModBlocks.ARCANE_WORKBENCH_CHARGER.get());

        if (hasCharger) {
            // Distribute vis drain across 3x3 chunk area
            int remaining = visCost;
            int perChunk = Math.max(1, visCost / 9);
            int attempts = 0;

            while (remaining > 0 && attempts < 1000) {
                for (int dx = -1; dx <= 1 && remaining > 0; dx++) {
                    for (int dz = -1; dz <= 1 && remaining > 0; dz++) {
                        int toDrain = Math.min(perChunk, remaining);
                        BlockPos drainPos = worldPosition.offset(dx * 16, 0, dz * 16);
                        int drained = (int) AuraHelper.drainVis(level, drainPos, toDrain, false);
                        remaining -= drained;
                    }
                }
                attempts++;
            }
        } else {
            // Drain from current chunk
            AuraHelper.drainVis(level, worldPosition, visCost, false);
        }
    }

    // ==================== Drop Items ====================

    /**
     * Drop all items in the crafting matrix.
     */
    public void dropContents() {
        if (level != null && !level.isClientSide) {
            for (int i = 0; i < TOTAL_SLOTS; i++) {
                ItemStack stack = craftMatrix.get(i);
                if (!stack.isEmpty()) {
                    net.minecraft.world.Containers.dropItemStack(level,
                            worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
                }
            }
            clearCraftMatrix();
        }
    }

    // ==================== Container Access ====================

    /**
     * Check if player can interact with this workbench.
     */
    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }

    // ==================== MenuProvider ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.thaumcraft.arcane_workbench");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ArcaneWorkbenchMenu(containerId, playerInventory, this);
    }
}




