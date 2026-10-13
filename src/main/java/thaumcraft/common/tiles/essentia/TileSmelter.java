package thaumcraft.common.tiles.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.menu.SmelterMenu;
import thaumcraft.common.tiles.TileThaumcraftInventory;
import thaumcraft.common.tiles.devices.TileBellows;
import thaumcraft.init.ModBlockEntities;

import javax.annotation.Nullable;

/**
 * Alchemical smelter tile entity - breaks down items into their component aspects.
 * Burns fuel to smelt items, then pushes aspects up to alembics.
 * 
 * Has 3 tiers: Basic (80% efficiency), Thaumium (90%), Void (95%)
 */
public class TileSmelter extends TileThaumcraftInventory implements Container, MenuProvider {

    // Slot indices
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_FUEL = 1;
    
    // Slots for hopper access
    private static final int[] SLOTS_BOTTOM = { SLOT_FUEL };
    private static final int[] SLOTS_TOP = {};
    private static final int[] SLOTS_SIDES = { SLOT_INPUT };

    // Aspect storage
    public AspectList aspects = new AspectList();
    public int vis = 0;
    private int maxVis = 256;
    
    // Smelting state
    public int smeltTime = 100;
    public int furnaceBurnTime = 0;
    public int currentItemBurnTime = 0;
    public int furnaceCookTime = 0;
    private boolean speedBoost = false;
    
    // Neighbor cache
    private int bellows = -1;
    private int count = 0;
    
    // Smelter type: 0 = basic, 1 = thaumium, 2 = void
    private int smelterType = 0;

    public TileSmelter(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 2);
    }

    public TileSmelter(BlockPos pos, BlockState state) {
        this(ModBlockEntities.SMELTER.get(), pos, state);
    }

    // ==================== NBT ====================

    @Override
    protected void writeSyncNBT(CompoundTag tag) {
        super.writeSyncNBT(tag);
        tag.putShort("BurnTime", (short) furnaceBurnTime);
    }

    @Override
    protected void readSyncNBT(CompoundTag tag) {
        super.readSyncNBT(tag);
        furnaceBurnTime = tag.getShort("BurnTime");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("SpeedBoost", speedBoost);
        tag.putShort("CookTime", (short) furnaceCookTime);
        aspects.writeToNBT(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        speedBoost = tag.getBoolean("SpeedBoost");
        furnaceCookTime = tag.getShort("CookTime");
        aspects.readFromNBT(tag);
        vis = aspects.visSize();
        
        // Recalculate current burn time
        ItemStack fuel = getItem(SLOT_FUEL);
        if (!fuel.isEmpty()) {
            currentItemBurnTime = fuel.getBurnTime(null);
        }
    }

    // ==================== Tick ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileSmelter tile) {
        boolean wasBurning = tile.furnaceBurnTime > 0;
        boolean changed = false;
        
        tile.count++;
        
        // Decrease burn time
        if (tile.furnaceBurnTime > 0) {
            tile.furnaceBurnTime--;
        }

        // Check bellows on first tick
        if (tile.bellows < 0) {
            tile.checkNeighbours();
        }

        int speed = tile.getSpeed();
        if (tile.speedBoost) {
            speed = (int) (speed * 0.8);
        }

        // Push aspects to alembics
        if (tile.count % speed == 0 && tile.aspects.size() > 0) {
            for (Aspect aspect : tile.aspects.getAspects()) {
                if (tile.aspects.getAmount(aspect) > 0) {
                    if (TileAlembic.processAlembics(level, pos, aspect)) {
                        tile.takeFromContainer(aspect, 1);
                        break;
                    }
                }
            }
            // TODO: Check for auxiliary vents and process through them too
        }

        // Try to start burning if we have fuel and can smelt
        if (tile.furnaceBurnTime == 0 && tile.canSmelt()) {
            ItemStack fuel = tile.getItem(SLOT_FUEL);
            int burnTime = fuel.getBurnTime(null);
            boolean speedFuel = fuel.is(thaumcraft.init.ModItems.ALUMENTUM.get());
            if (speedFuel) {
                burnTime = 200;
            }

            if (burnTime > 0) {
                tile.furnaceBurnTime = burnTime;
                tile.currentItemBurnTime = burnTime;
                changed = true;
                tile.speedBoost = speedFuel;
                
                // Consume fuel
                if (!fuel.isEmpty()) {
                    ItemStack containerItem = fuel.getCraftingRemainingItem();
                    fuel.shrink(1);
                    if (fuel.isEmpty()) {
                        tile.setItem(SLOT_FUEL, containerItem);
                    }
                }
                
                setFurnaceState(level, pos, state, true);
            } else {
                setFurnaceState(level, pos, state, false);
            }
        } else if (tile.furnaceBurnTime == 0) {
            setFurnaceState(level, pos, state, false);
        }

        // Process smelting
        if (isEnabled(state) && tile.furnaceBurnTime > 0 && tile.canSmelt()) {
            tile.furnaceCookTime++;
            if (tile.furnaceCookTime >= tile.smeltTime) {
                tile.furnaceCookTime = 0;
                tile.smeltItem();
                changed = true;
            }
        } else {
            tile.furnaceCookTime = 0;
        }

        // Sync if burn state changed
        if (wasBurning != (tile.furnaceBurnTime > 0)) {
            changed = true;
        }

        if (changed) {
            tile.setChanged();
        }
    }

    private boolean canSmelt() {
        ItemStack input = getItem(SLOT_INPUT);
        if (input.isEmpty()) {
            return false;
        }

        // TODO: Get aspects from item using ThaumcraftCraftingManager
        AspectList itemAspects = getItemAspects(input);
        if (itemAspects == null || itemAspects.size() == 0) {
            return false;
        }

        int itemVis = itemAspects.visSize();
        if (itemVis > maxVis - vis) {
            return false;
        }

        // Calculate smelt time based on item complexity and bellows
        smeltTime = (int) (itemVis * 2 * (1.0f - 0.125f * bellows));
        return true;
    }

    private void smeltItem() {
        if (!canSmelt()) return;

        ItemStack input = getItem(SLOT_INPUT);
        AspectList itemAspects = getItemAspects(input);
        
        int flux = 0;
        float efficiency = getEfficiency();
        
        // Add aspects to internal storage, with efficiency loss
        for (Aspect aspect : itemAspects.getAspects()) {
            int amount = itemAspects.getAmount(aspect);
            for (int i = 0; i < amount; i++) {
                // Flux has worse efficiency
                float effectiveEfficiency = (aspect == Aspect.FLUX) ? efficiency * 0.66f : efficiency;
                if (level.random.nextFloat() > effectiveEfficiency) {
                    flux++;
                } else {
                    aspects.add(aspect, 1);
                }
            }
        }

        // Generate flux pollution
        if (flux > 0) {
            // Vents above the smelter reduce the pollution emitted
            int fluxToRemove = flux;
            if (hasVentAbove()) {
                fluxToRemove = (int) Math.ceil(flux * 0.5f);
            }
            if (fluxToRemove > 0) {
                AuraHelper.polluteAura(level, worldPosition, fluxToRemove, true);
            }
        }

        vis = aspects.visSize();
        
        // Consume input
        input.shrink(1);
        if (input.isEmpty()) {
            setItem(SLOT_INPUT, ItemStack.EMPTY);
        }
    }

    /**
     * Get aspects from an item via the ThaumcraftCraftingManager lookup.
     */
    private AspectList getItemAspects(ItemStack stack) {
        return ThaumcraftCraftingManager.getObjectTags(stack);
    }

    public boolean takeFromContainer(Aspect tag, int amount) {
        if (aspects != null && aspects.getAmount(tag) >= amount) {
            aspects.remove(tag, amount);
            vis = aspects.visSize();
            setChanged();
            return true;
        }
        return false;
    }

    // ==================== State Helpers ====================

    private static void setFurnaceState(Level level, BlockPos pos, BlockState state, boolean lit) {
        if (state.hasProperty(BlockStateProperties.LIT)) {
            boolean currentlyLit = state.getValue(BlockStateProperties.LIT);
            if (currentlyLit != lit) {
                level.setBlock(pos, state.setValue(BlockStateProperties.LIT, lit), 3);
            }
        }
    }

    private static boolean isEnabled(BlockState state) {
        if (state.hasProperty(BlockStateProperties.ENABLED)) {
            return state.getValue(BlockStateProperties.ENABLED);
        }
        // If it's lit, it's enabled
        if (state.hasProperty(BlockStateProperties.LIT)) {
            return state.getValue(BlockStateProperties.LIT);
        }
        return true;
    }

    private Direction getFacing() {
        BlockState state = getBlockState();
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        return Direction.NORTH;
    }

    /**
     * Check whether a smelter vent block is connected above this smelter.
     * Used to reduce pollution emitted during smelting.
     */
    private boolean hasVentAbove() {
        if (level == null) return false;
        BlockState above = level.getBlockState(worldPosition.above());
        return above.is(thaumcraft.init.ModBlocks.SMELTER_VENT.get());
    }

    public void checkNeighbours() {
        Direction facing = getFacing();
        Direction[] checkDirs;
        
        switch (facing) {
            case NORTH -> checkDirs = new Direction[]{ Direction.SOUTH, Direction.EAST, Direction.WEST };
            case SOUTH -> checkDirs = new Direction[]{ Direction.NORTH, Direction.EAST, Direction.WEST };
            case EAST -> checkDirs = new Direction[]{ Direction.SOUTH, Direction.NORTH, Direction.WEST };
            case WEST -> checkDirs = new Direction[]{ Direction.SOUTH, Direction.EAST, Direction.NORTH };
            default -> checkDirs = Direction.Plane.HORIZONTAL.stream().toArray(Direction[]::new);
        }
        
        bellows = TileBellows.getBellows(level, worldPosition, checkDirs);
    }

    private float getEfficiency() {
        return switch (smelterType) {
            case 1 -> 0.9f;  // Thaumium
            case 2 -> 0.95f; // Void
            default -> 0.8f; // Basic
        };
    }

    /**
     * Speed up the smelter from an adjacent TileBellows.
     * Increments the cook timer by one tick while smelting is in progress.
     */
    public void speedUpFromBellows() {
        if (furnaceCookTime > 0 && furnaceCookTime < smeltTime) {
            furnaceCookTime++;
        }
    }

    private int getSpeed() {
        return switch (smelterType) {
            case 1 -> 10;  // Thaumium (faster)
            case 2 -> 15;  // Void (medium)
            default -> 20; // Basic (slowest)
        };
    }

    public void setSmelterType(int type) {
        this.smelterType = Math.max(0, Math.min(2, type));
    }

    // ==================== Container ====================

    @Override
    public int getContainerSize() {
        return 2;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    public static boolean isItemFuel(ItemStack stack) {
        return stack.getBurnTime(null) > 0;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_INPUT) {
            // Only accept items that have aspects
            AspectList aspects = getItemAspects(stack);
            return aspects != null && aspects.size() > 0;
        }
        return slot == SLOT_FUEL && isItemFuel(stack);
    }

    public int[] getSlotsForFace(Direction face) {
        return switch (face) {
            case DOWN -> SLOTS_BOTTOM;
            case UP -> SLOTS_TOP;
            default -> SLOTS_SIDES;
        };
    }

    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction face) {
        return face != Direction.UP && canPlaceItem(slot, stack);
    }

    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) {
        // Only allow extracting buckets from fuel slot
        return face != Direction.UP || slot != SLOT_FUEL || stack.getItem() == Items.BUCKET;
    }

    // ==================== GUI Helpers ====================

    public int getCookProgressScaled(int scale) {
        if (smeltTime <= 0) smeltTime = 1;
        return furnaceCookTime * scale / smeltTime;
    }

    public int getVisScaled(int scale) {
        return vis * scale / maxVis;
    }

    public int getBurnTimeRemainingScaled(int scale) {
        if (currentItemBurnTime == 0) currentItemBurnTime = 200;
        return furnaceBurnTime * scale / currentItemBurnTime;
    }

    public boolean isBurning() {
        return furnaceBurnTime > 0;
    }

    // ==================== MenuProvider ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.thaumcraft.smelter");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SmelterMenu(containerId, playerInventory, this);
    }
}




