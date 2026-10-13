package thaumcraft.common.tiles.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.common.tiles.TileThaumcraftInventory;
import thaumcraft.init.ModBlockEntities;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * TileInfernalFurnace - A magical furnace that smelts items using aura.
 * 
 * Features:
 * - Smelts items without fuel
 * - Bonus outputs based on bellows attached
 * - Uses aura for speed boost
 * - Ejects results automatically
 * 
 * Ported from 1.12.2
 */
public class TileInfernalFurnace extends TileThaumcraftInventory {
    
    public int furnaceCookTime = 0;
    public int furnaceMaxCookTime = 0;
    public int speedyTime = 0;
    public int facingX = -5;
    public int facingZ = -5;
    
    public TileInfernalFurnace(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INFERNAL_FURNACE.get(), pos, state, 32);
    }
    
    // ==================== NBT ====================
    
    @Override
    protected void writeSyncNBT(CompoundTag tag) {
        super.writeSyncNBT(tag);
        tag.putShort("CookTime", (short) furnaceCookTime);
        tag.putShort("SpeedyTime", (short) speedyTime);
    }
    
    @Override
    protected void readSyncNBT(CompoundTag tag) {
        super.readSyncNBT(tag);
        furnaceCookTime = tag.getShort("CookTime");
        speedyTime = tag.getShort("SpeedyTime");
    }
    
    // ==================== Tick ====================
    
    public static void serverTick(Level level, BlockPos pos, BlockState state, TileInfernalFurnace tile) {
        if (tile.facingX == -5) {
            tile.setFacing(state);
        }
        
        boolean wasCooked = false;
        
        if (tile.furnaceCookTime > 0) {
            tile.furnaceCookTime--;
            wasCooked = true;
        }
        
        if (tile.furnaceMaxCookTime <= 0) {
            tile.furnaceMaxCookTime = tile.calcCookTime();
        }
        
        if (tile.furnaceCookTime > tile.furnaceMaxCookTime) {
            tile.furnaceCookTime = tile.furnaceMaxCookTime;
        }
        
        // Item finished cooking
        if (tile.furnaceCookTime <= 0 && wasCooked) {
            for (int slot = 0; slot < tile.getContainerSize(); slot++) {
                ItemStack inputStack = tile.getItem(slot);
                if (!inputStack.isEmpty()) {
                    ItemStack result = tile.getSmeltingResult(inputStack);
                    if (!result.isEmpty()) {
                        if (tile.speedyTime > 0) {
                            tile.speedyTime--;
                        }
                        tile.ejectItem(result.copy(), inputStack);
                        level.blockEvent(pos, state.getBlock(), 3, 0);
                        
                        // Chance to pollute aura
                        if (level.random.nextInt(20) == 0) {
                            Direction facing = tile.getFacing(state);
                            if (facing != null) {
                                AuraHelper.polluteAura(level, pos.relative(facing.getOpposite()), 1.0f, true);
                            }
                        }
                        
                        tile.removeItem(slot, 1);
                        break;
                    }
                    // Invalid item - discard it
                    tile.setItem(slot, ItemStack.EMPTY);
                }
            }
        }
        
        // Recharge speed boost from aura
        if (tile.speedyTime <= 0) {
            tile.speedyTime = (int) AuraHelper.drainVis(level, pos, 20.0f, false);
        }
        
        // Start cooking new item
        if (tile.furnaceCookTime == 0 && !wasCooked) {
            for (int slot = 0; slot < tile.getContainerSize(); slot++) {
                if (tile.canSmelt(tile.getItem(slot))) {
                    tile.furnaceMaxCookTime = tile.calcCookTime();
                    tile.furnaceCookTime = tile.furnaceMaxCookTime;
                    break;
                }
            }
        }
    }
    
    // ==================== Smelting ====================
    
    private ItemStack getSmeltingResult(ItemStack input) {
        if (input.isEmpty() || level == null) return ItemStack.EMPTY;
        
        RecipeManager recipeManager = level.getRecipeManager();
        Optional<RecipeHolder<SmeltingRecipe>> recipe = recipeManager.getRecipeFor(
                RecipeType.SMELTING,
                new SingleRecipeInput(input),
                level
        );

        return recipe.map(r -> r.value().getResultItem(level.registryAccess()).copy()).orElse(ItemStack.EMPTY);
    }
    
    private boolean canSmelt(ItemStack stack) {
        return !stack.isEmpty() && !getSmeltingResult(stack).isEmpty();
    }
    
    private float getSmeltingExperience(ItemStack result) {
        if (level == null) return 0;
        
        // Try to get experience from recipe
        RecipeManager recipeManager = level.getRecipeManager();
        for (RecipeHolder<SmeltingRecipe> holder : recipeManager.getAllRecipesFor(RecipeType.SMELTING)) {
            SmeltingRecipe recipe = holder.value();
            if (ItemStack.isSameItem(recipe.getResultItem(level.registryAccess()), result)) {
                return recipe.getExperience();
            }
        }
        return 0;
    }
    
    // ==================== Bellows ====================
    
    private int getBellows() {
        if (level == null) return 0;
        
        int bellows = 0;
        for (Direction dir : Direction.values()) {
            if (dir != Direction.UP) {
                BlockPos bellowsPos = worldPosition.relative(dir, 2);
                BlockEntity tile = level.getBlockEntity(bellowsPos);
                
                // Check for bellows pointing at furnace and not powered
                if (tile instanceof TileBellows bellowsTile) {
                    BlockState bellowsState = level.getBlockState(bellowsPos);
                    Direction bellowsFacing = bellowsTile.getFacing(bellowsState);
                    if (bellowsFacing == dir.getOpposite() && !level.hasNeighborSignal(bellowsPos)) {
                        bellows++;
                    }
                }
            }
        }
        return Math.min(4, bellows);
    }
    
    private int calcCookTime() {
        int b = getBellows();
        if (b > 0) {
            b *= 20 - (b - 1);
        }
        return Math.max(10, ((speedyTime > 0) ? 80 : 140) - b);
    }
    
    // ==================== Item Handling ====================
    
    /**
     * Add items from a hopper or similar.
     * @return Remaining items that couldn't be added
     */
    public ItemStack addItemsToInventory(ItemStack items) {
        if (canSmelt(items)) {
            // Find empty slot
            for (int i = 0; i < getContainerSize(); i++) {
                if (getItem(i).isEmpty()) {
                    setItem(i, items.split(1));
                    if (items.isEmpty()) return ItemStack.EMPTY;
                }
            }
            return items;
        } else {
            destroyItem();
            return ItemStack.EMPTY;
        }
    }
    
    private void destroyItem() {
        if (level == null) return;
        level.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.3f, 
                2.6f + (level.random.nextFloat() - level.random.nextFloat()) * 0.8f);
    }
    
    public void ejectItem(ItemStack items, ItemStack inputStack) {
        if (items.isEmpty() || level == null) return;
        
        List<ItemStack> outputs = new ArrayList<>();
        outputs.add(items.copy());
        
        int bellows = getBellows() + 1;
        
        // Add bonus items based on bellows
        for (int i = 0; i < bellows; i++) {
            ItemStack[] bonus = getSmeltingBonus(inputStack);
            if (bonus != null) {
                for (ItemStack b : bonus) {
                    if (!b.isEmpty()) {
                        outputs.add(b);
                    }
                }
            }
        }
        
        // Eject all items
        Direction facing = getFacing(level.getBlockState(worldPosition));
        if (facing == null) facing = Direction.NORTH;
        Direction ejectDir = facing.getOpposite();
        
        for (ItemStack output : outputs) {
            if (!output.isEmpty()) {
                double x = worldPosition.getX() + 0.5 + ejectDir.getStepX() * 0.7;
                double y = worldPosition.getY() + 0.5;
                double z = worldPosition.getZ() + 0.5 + ejectDir.getStepZ() * 0.7;
                
                net.minecraft.world.entity.item.ItemEntity itemEntity = 
                        new net.minecraft.world.entity.item.ItemEntity(level, x, y, z, output);
                itemEntity.setDeltaMovement(
                        ejectDir.getStepX() * 0.1,
                        0.1,
                        ejectDir.getStepZ() * 0.1
                );
                level.addFreshEntity(itemEntity);
            }
        }
        
        // Spawn XP
        spawnExperience(items);
    }
    
    private void spawnExperience(ItemStack result) {
        if (level == null) return;
        
        float xpf = getSmeltingExperience(result);
        int count = result.getCount();
        
        if (xpf == 0) {
            count = 0;
        } else if (xpf < 1.0f) {
            int xpAmount = Mth.floor(count * xpf);
            if (xpAmount < Mth.ceil(count * xpf) && level.random.nextFloat() < count * xpf - xpAmount) {
                xpAmount++;
            }
            count = xpAmount;
        }
        
        Direction facing = getFacing(level.getBlockState(worldPosition));
        if (facing == null) facing = Direction.NORTH;
        
        float lx = worldPosition.getX() + 0.5f + facing.getOpposite().getStepX() * 1.2f;
        float lz = worldPosition.getZ() + 0.5f + facing.getOpposite().getStepZ() * 1.2f;
        
        while (count > 0) {
            int xpValue = ExperienceOrb.getExperienceValue(count);
            count -= xpValue;
            
            ExperienceOrb xp = new ExperienceOrb(level, lx, worldPosition.getY() + 0.4, lz, xpValue);
            float mx = facing.getOpposite().getStepX() == 0 ? 
                    (level.random.nextFloat() - level.random.nextFloat()) * 0.025f : 
                    facing.getOpposite().getStepX() * 0.13f;
            float mz = facing.getOpposite().getStepZ() == 0 ? 
                    (level.random.nextFloat() - level.random.nextFloat()) * 0.025f : 
                    facing.getOpposite().getStepZ() * 0.13f;
            xp.setDeltaMovement(mx, 0, mz);
            level.addFreshEntity(xp);
        }
    }
    
    private ItemStack[] getSmeltingBonus(ItemStack input) {
        // Bellows provide bonus output: an extra copy of the smelted result.
        // In TC4, every adjacent bellows adds one bonus output per smelt operation.
        ItemStack smelted = getSmeltingResult(input);
        if (smelted.isEmpty()) {
            return new ItemStack[0];
        }
        return new ItemStack[]{ smelted.copy() };
    }
    
    // ==================== Facing ====================
    
    private void setFacing(BlockState state) {
        Direction facing = getFacing(state);
        if (facing != null) {
            Direction opposite = facing.getOpposite();
            facingX = opposite.getStepX();
            facingZ = opposite.getStepZ();
        } else {
            facingX = 0;
            facingZ = -1;
        }
    }
    
    private Direction getFacing(BlockState state) {
        // Get facing from block state
        if (state.hasProperty(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING)) {
            return state.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
        }
        return Direction.NORTH;
    }
    
    // ==================== Block Events ====================
    
    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == 3) {
            if (level != null && level.isClientSide) {
                // TODO: Add particle effects when FX system is ported
                // FXDispatcher.INSTANCE.furnaceLavaFx(...)
                level.playLocalSound(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                        SoundEvents.LAVA_POP, SoundSource.BLOCKS,
                        0.1f + level.random.nextFloat() * 0.1f,
                        0.9f + level.random.nextFloat() * 0.15f, false);
            }
            return true;
        }
        return super.triggerEvent(id, param);
    }
    
    // ==================== Rendering ====================
    
    public AABB getRenderBoundingBox() {
        return new AABB(
                worldPosition.getX() - 1.3, worldPosition.getY() - 1.3, worldPosition.getZ() - 1.3,
                worldPosition.getX() + 2.3, worldPosition.getY() + 2.3, worldPosition.getZ() + 2.3
        );
    }
}




