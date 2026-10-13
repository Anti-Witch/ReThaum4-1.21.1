package thaumcraft.common.tiles.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.common.blocks.world.taint.BlockFluxGas;
import thaumcraft.common.blocks.world.taint.BlockFluxGoo;
import thaumcraft.common.tiles.TileThaumcraft;
import thaumcraft.init.ModBlockEntities;
import thaumcraft.init.ModBlocks;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.lib.crafting.CrucibleRecipeType;


/**
 * Crucible tile entity - melts items into their aspects and performs crucible crafting.
 * Requires heat from below (lava, fire, nitor, magma block).
 * Can accept water via fluid handlers.
 */
public class TileCrucible extends TileThaumcraft implements IAspectContainer {

    public static final int MAX_ASPECTS = 500;
    public static final int TANK_CAPACITY = 1000;

    public short heat = 0;
    public AspectList aspects = new AspectList();
    
    private final FluidTank tank = new FluidTank(TANK_CAPACITY) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == Fluids.WATER;
        }
    };
    private int bellows = -1;
    private long tickCounter = 0;

    public TileCrucible(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRUCIBLE.get(), pos, state);
    }

    // ==================== NBT ====================

    @Override
    protected void writeSyncNBT(CompoundTag tag) {
        super.writeSyncNBT(tag);
        tag.putShort("Heat", heat);
        tank.writeToNBT(getRegistryProvider(), tag);
        aspects.writeToNBT(tag);
    }

    @Override
    protected void readSyncNBT(CompoundTag tag) {
        super.readSyncNBT(tag);
        heat = tag.getShort("Heat");
        tank.readFromNBT(getRegistryProvider(), tag);
        aspects.readFromNBT(tag);
    }

    // ==================== Tick ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileCrucible tile) {
        tile.tickCounter++;
        int prevHeat = tile.heat;

        // Check for heat source below
        if (tile.tank.getFluidAmount() > 0) {
            BlockState below = level.getBlockState(pos.below());
            if (tile.isHeatSource(below)) {
                if (tile.heat < 200) {
                    tile.heat++;
                    if (prevHeat < 151 && tile.heat >= 151) {
                        tile.markDirtyAndSync();
                    }
                }
            } else if (tile.heat > 0) {
                tile.heat--;
                if (tile.heat == 149) {
                    tile.markDirtyAndSync();
                }
            }
        } else if (tile.heat > 0) {
            tile.heat--;
        }

        // Overflow: TC4 dumps one random aspect every 5 ticks while over capacity
        // (tagAmount() > 100 && counter % 5 == 0 -> takeRandomFromSource() + spill())
        if (tile.aspects.visSize() > MAX_ASPECTS && tile.tickCounter % 5 == 0) {
            tile.spillRandom();
        }

        // Boiling: TC4 processes one aspect every 100 ticks while heat > 150.
        // Compound aspects decompose into a component, primal aspects spill out.
        if (tile.tickCounter > 100 && tile.heat > 150) {
            tile.tickCounter = 0;
            tile.boilAspect();
            tile.markDirtyAndSync();
        }
    }

    private boolean isHeatSource(BlockState state) {
        // Check for lava, fire, magma block, or nitor (all 16 color variants)
        return state.is(Blocks.LAVA) ||
               state.is(Blocks.FIRE) ||
               state.is(Blocks.SOUL_FIRE) ||
               state.is(Blocks.MAGMA_BLOCK) ||
               state.getBlock() instanceof thaumcraft.common.blocks.misc.BlockNitor;
    }

    // ==================== Smelting ====================

    /**
     * Attempt to smelt an item dropped into the crucible.
     */
    public void attemptSmelt(ItemEntity itemEntity) {
        if (level == null || level.isClientSide) return;
        if (heat < 151 || tank.getFluidAmount() <= 0) return;

        ItemStack stack = itemEntity.getItem();
        // In 1.20.1, thrower info is stored differently
        String thrower = itemEntity.getOwner() != null ? 
                itemEntity.getOwner().getName().getString() : "";

        ItemStack result = attemptSmelt(stack, thrower);
        if (result == null || result.isEmpty()) {
            itemEntity.discard();
        } else {
            itemEntity.setItem(result);
        }
    }

    /**
     * Attempt to smelt an item stack.
     * Returns remaining items, or null if fully consumed.
     */
    public ItemStack attemptSmelt(ItemStack item, String username) {
        if (level == null) return item;
        
        boolean itemChanged = false;
        int remaining = item.getCount();

        // Check for crucible recipe
        Player player = level.getPlayerByUUID(java.util.UUID.nameUUIDFromBytes(username.getBytes())); // Approximate player retrieval
        // Better to pass Player entity if possible, but username is what we have from ItemEntity
        // For now, let's just pass null for player if we can't easily get it, or try to get it properly
        if (player == null && !username.isEmpty()) {
             // Try to find player by name if UUID fails or simple name
             player = level.players().stream().filter(p -> p.getName().getString().equals(username)).findFirst().orElse(null);
        }

        CrucibleRecipeType recipe = ThaumcraftCraftingManager.findMatchingCrucibleRecipe(aspects, item, player, level);
        
        if (recipe != null) {
            // Found a recipe!
            AspectList required = recipe.getAspects();
            if (aspects.contains(required)) {
                aspects.remove(required);
                
                // Crafted successfully
                ItemStack result = recipe.assemble(null, level.registryAccess());
                
                // Spawn result in world
                double x = worldPosition.getX() + 0.5;
                double y = worldPosition.getY() + 0.5;
                double z = worldPosition.getZ() + 0.5;
                ItemEntity entity = new ItemEntity(level, x, y + 1.0, z, result.copy());
                entity.setDeltaMovement(0, 0.25, 0);
                level.addFreshEntity(entity);
                
                // Play sound
                level.playSound(null, worldPosition, SoundEvents.EXPERIENCE_ORB_PICKUP,
                        SoundSource.BLOCKS, 0.5f, 1.0f);
                
                // Effects
                if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    thaumcraft.common.lib.network.PacketHandler.sendToAllTrackingChunk(
                            new thaumcraft.common.lib.network.fx.PacketFXBlockBamf(worldPosition, 0x9933FF,
                                    true, true, net.minecraft.core.Direction.UP),
                            serverLevel, worldPosition);
                }
                
                remaining--;
                itemChanged = true;
                markDirtyAndSync();
                
                if (remaining <= 0) {
                    return null;
                }
                ItemStack ret = item.copy();
                ret.setCount(remaining);
                return ret;
            }
        }

        // For now, just dissolve items into aspects
        AspectList itemAspects = getItemAspects(item);
        if (itemAspects != null && itemAspects.size() > 0) {
            for (Aspect aspect : itemAspects.getAspects()) {
                aspects.add(aspect, itemAspects.getAmount(aspect));
            }
            remaining--;
            itemChanged = true;
            tickCounter = -150; // Reset spill timer

            level.playSound(null, worldPosition, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP,
                    SoundSource.BLOCKS, 0.2f, 1.0f + level.random.nextFloat() * 0.4f);
        }

        if (itemChanged) {
            markDirtyAndSync();
        }

        if (remaining <= 0) {
            return null;
        }
        
        ItemStack result = item.copy();
        result.setCount(remaining);
        return result;
    }

    /**
     * Get aspects from an item using the crafting manager.
     */
    private AspectList getItemAspects(ItemStack item) {
        return ThaumcraftCraftingManager.getObjectTags(item);
    }

    /**
     * Overflow handling: take one random aspect out of the crucible and spill it.
     *
     * TC4: {@code takeRandomFromSource(); spill();}
     * The block placement is 1:1 with TC4's spill(); the aura pollution is a
     * port-specific addition (TC4 had no aura system) kept from the previous
     * implementation.
     */
    public void spillRandom() {
        if (level == null || level.isClientSide || aspects.size() <= 0) return;

        Aspect[] aspectArray = aspects.getAspects();
        Aspect toSpill = aspectArray[level.random.nextInt(aspectArray.length)];
        aspects.remove(toSpill, 1);

        polluteForAspect(toSpill);
        spill();
        markDirtyAndSync();
    }

    /**
     * 1:1 port of TC4 {@code TileCrucible.spill()} (OpenTC4 lines 174-204):
     * 25% chance to place flux gas/goo above the crucible, top up an existing
     * flux block above (LEVEL/meta +1, capped at 7) or place a fresh level 0
     * block in a random 3x3x3 offset.
     */
    public void spill() {
        if (level == null || level.isClientSide) return;
        if (level.random.nextInt(4) != 0) return;

        BlockPos above = worldPosition.above();
        if (level.getBlockState(above).isAir()) {
            if (level.random.nextBoolean()) {
                level.setBlock(above, BlockFluxGas.withLevel(ModBlocks.FLUX_GAS.get(), 0), 3);
            } else {
                level.setBlock(above, BlockFluxGoo.withLevel(ModBlocks.FLUX_GOO.get(), 0), 3);
            }
            return;
        }

        BlockState aboveState = level.getBlockState(above);
        if (aboveState.is(ModBlocks.FLUX_GOO.get())) {
            int gooLevel = aboveState.getValue(BlockFluxGoo.LEVEL);
            if (gooLevel < 7) {
                level.setBlock(above, aboveState.setValue(BlockFluxGoo.LEVEL, gooLevel + 1), 3);
            }
            return;
        }
        if (aboveState.is(ModBlocks.FLUX_GAS.get())) {
            int gasLevel = aboveState.getValue(BlockFluxGas.LEVEL);
            if (gasLevel < 7) {
                level.setBlock(above, aboveState.setValue(BlockFluxGas.LEVEL, gasLevel + 1), 3);
            }
            return;
        }

        int x = -1 + level.random.nextInt(3);
        int y = -1 + level.random.nextInt(3);
        int z = -1 + level.random.nextInt(3);
        BlockPos target = worldPosition.offset(x, y, z);
        if (level.getBlockState(target).isAir()) {
            if (level.random.nextBoolean()) {
                level.setBlock(target, BlockFluxGas.withLevel(ModBlocks.FLUX_GAS.get(), 0), 3);
            } else {
                level.setBlock(target, BlockFluxGoo.withLevel(ModBlocks.FLUX_GOO.get(), 0), 3);
            }
        }
    }

    /**
     * TC4 boiling behaviour (counter > 100 && heat > 150): drain 2 water, remove
     * one random aspect; compound aspects decompose into one of their components,
     * primal aspects are spilled out as flux.
     */
    private void boilAspect() {
        if (level == null || level.isClientSide || aspects.size() <= 0) return;

        Aspect[] aspectArray = aspects.getAspects();
        Aspect aspect = aspectArray[level.random.nextInt(aspectArray.length)];
        if (aspect.isPrimal()) {
            // TC4 re-rolls once in an attempt to pick a non-primal aspect
            aspect = aspectArray[level.random.nextInt(aspectArray.length)];
        }

        tank.drain(2, IFluidHandler.FluidAction.EXECUTE);
        aspects.remove(aspect, 1);

        if (!aspect.isPrimal()) {
            Aspect[] components = aspect.getComponents();
            aspects.add(components[level.random.nextBoolean() ? 0 : 1], 1);
        } else {
            polluteForAspect(aspect);
            spill();
        }
    }

    /**
     * Port-specific aura pollution for aspects leaving the crucible as flux.
     */
    private void polluteForAspect(Aspect aspect) {
        float pollution = (aspect == Aspect.FLUX) ? 1.0f : 0.25f;
        AuraHelper.polluteAura(level, worldPosition, pollution, true);
    }

    /**
     * Dump all contents of the crucible as flux.
     */
    public void spillAll() {
        if (level == null) return;
        
        int total = aspects.visSize();
        if (tank.getFluidAmount() > 0 || total > 0) {
            tank.drain(TANK_CAPACITY, IFluidHandler.FluidAction.EXECUTE);
            AuraHelper.polluteAura(level, worldPosition, total * 0.25f, true);

            int flux = aspects.getAmount(Aspect.FLUX);
            if (flux > 0) {
                AuraHelper.polluteAura(level, worldPosition, flux * 0.75f, false);
            }

            aspects = new AspectList();
            markDirtyAndSync();
        }
    }

    // ==================== IAspectContainer ====================

    @Override
    public AspectList getAspects() {
        return aspects;
    }

    @Override
    public void setAspects(AspectList aspects) {
        // Crucible doesn't allow direct setting
    }

    @Override
    public int addToContainer(Aspect tag, int amount) {
        return 0; // Cannot add directly
    }

    @Override
    public boolean takeFromContainer(Aspect tag, int amount) {
        return false; // Cannot take directly
    }

    @Override
    public boolean takeFromContainer(AspectList list) {
        return false;
    }

    @Override
    public boolean doesContainerContainAmount(Aspect tag, int amount) {
        return aspects.getAmount(tag) >= amount;
    }

    @Override
    public boolean doesContainerContain(AspectList list) {
        for (Aspect a : list.getAspects()) {
            if (aspects.getAmount(a) >= list.getAmount(a)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int containerContains(Aspect tag) {
        return aspects.getAmount(tag);
    }

    @Override
    public boolean doesContainerAccept(Aspect tag) {
        return true;
    }

    // ==================== Fluid Handling ====================

    public FluidTank getTank() {
        return tank;
    }

    public float getFluidHeight() {
        float base = 0.3f + 0.5f * (tank.getFluidAmount() / (float) TANK_CAPACITY);
        float extra = aspects.visSize() / (float) MAX_ASPECTS * (1.0f - base);
        float total = base + extra;
        return Math.min(total, 0.9999f);
    }

    // ==================== Rendering ====================

    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                worldPosition.getX() + 1, worldPosition.getY() + 1, worldPosition.getZ() + 1);
    }

    public boolean isHeated() {
        return heat >= 151;
    }
}




