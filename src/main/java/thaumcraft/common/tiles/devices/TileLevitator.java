package thaumcraft.common.tiles.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.tiles.TileThaumcraft;
import thaumcraft.init.ModBlockEntities;

import java.util.List;

/**
 * TileLevitator - Pushes entities in a direction using vis from the aura.
 * 
 * Features:
 * - Configurable range (4, 8, 16, 32 blocks)
 * - Pushes items and pushable entities
 * - Softens fall damage
 * - Consumes vis from aura based on range
 */
public class TileLevitator extends TileThaumcraft {
    
    private static final int[] RANGES = {4, 8, 16, 32};
    
    private int rangeIndex = 1; // Default to 8 blocks
    private int rangeActual = 0;
    private int counter = 0;
    private int vis = 0;
    
    public TileLevitator(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    
    public TileLevitator(BlockPos pos, BlockState state) {
        this(ModBlockEntities.LEVITATOR.get(), pos, state);
    }
    
    // ==================== Tick ====================
    
    public static void serverTick(Level level, BlockPos pos, BlockState state, TileLevitator tile) {
        tile.tick();
    }
    
    public static void clientTick(Level level, BlockPos pos, BlockState state, TileLevitator tile) {
        tile.tick();
    }
    
    private void tick() {
        Direction facing = getFacing();
        int maxRange = RANGES[rangeIndex];
        
        // Clamp actual range
        if (rangeActual > maxRange) {
            rangeActual = 0;
        }
        
        // Calculate actual range (check for obstructions)
        int p = counter % maxRange;
        BlockPos checkPos = worldPosition.relative(facing, 1 + p);
        if (level.getBlockState(checkPos).canOcclude()) {
            if (1 + p < rangeActual) {
                rangeActual = 1 + p;
            }
            counter = -1;
        } else if (1 + p > rangeActual) {
            rangeActual = 1 + p;
        }
        counter++;
        
        // Drain vis from aura on server
        if (!level.isClientSide && vis < 10) {
            vis += (int)(AuraHelper.drainVis(level, worldPosition, 1.0f, false) * 1200.0f);
            setChanged();
            syncTile(false);
        }
        
        // Only operate if enabled, has range, and has vis
        if (rangeActual <= 0 || vis <= 0 || !isEnabled()) {
            return;
        }
        
        // Calculate entity selection box
        AABB box = createSelectionBox(facing);
        List<Entity> targets = level.getEntitiesOfClass(Entity.class, box, 
                e -> e instanceof ItemEntity || e.isPushable() || e instanceof AbstractHorse);
        
        boolean lifted = false;
        
        for (Entity entity : targets) {
            lifted = true;
            
            // Draw effects
            if (level.isClientSide) {
                drawFXAt(entity);
                drawFX(facing, 0.6);
            }
            
            // Apply motion
            if (entity.isShiftKeyDown() && facing == Direction.UP) {
                // Sneaking while going up - slow fall
                if (entity.getDeltaMovement().y < 0) {
                    entity.setDeltaMovement(
                            entity.getDeltaMovement().x,
                            entity.getDeltaMovement().y * 0.9,
                            entity.getDeltaMovement().z
                    );
                }
            } else {
                // Normal push
                double mx = entity.getDeltaMovement().x + 0.1 * facing.getStepX();
                double my = entity.getDeltaMovement().y + 0.1 * facing.getStepY();
                double mz = entity.getDeltaMovement().z + 0.1 * facing.getStepZ();
                
                // Counter gravity for horizontal movement
                if (facing.getAxis() != Direction.Axis.Y && !entity.onGround()) {
                    if (entity.getDeltaMovement().y < 0) {
                        my = entity.getDeltaMovement().y * 0.9;
                    }
                    my += 0.08;
                }
                
                // Clamp velocity
                mx = clamp(mx, -0.35, 0.35);
                my = clamp(my, -0.35, 0.35);
                mz = clamp(mz, -0.35, 0.35);
                
                entity.setDeltaMovement(mx, my, mz);
            }
            
            // Reset fall distance
            entity.fallDistance = 0;
            
            // Consume vis
            vis -= getCost();
            if (vis <= 0) {
                break;
            }
        }
        
        // Ambient particles
        if (level.isClientSide) {
            drawFX(facing, 0.1);
        }
        
        if (lifted && !level.isClientSide && counter % 20 == 0) {
            setChanged();
        }
    }
    
    private AABB createSelectionBox(Direction facing) {
        int ox = facing.getStepX();
        int oy = facing.getStepY();
        int oz = facing.getStepZ();
        
        return new AABB(
                worldPosition.getX() - (ox < 0 ? rangeActual : 0),
                worldPosition.getY() - (oy < 0 ? rangeActual : 0),
                worldPosition.getZ() - (oz < 0 ? rangeActual : 0),
                worldPosition.getX() + 1 + (ox > 0 ? rangeActual : 0),
                worldPosition.getY() + 1 + (oy > 0 ? rangeActual : 0),
                worldPosition.getZ() + 1 + (oz > 0 ? rangeActual : 0)
        );
    }
    
    private void drawFX(Direction facing, double chance) {
        if (level.isClientSide && level.random.nextFloat() < chance) {
            float x = worldPosition.getX() + 0.25f + level.random.nextFloat() * 0.5f;
            float y = worldPosition.getY() + 0.25f + level.random.nextFloat() * 0.5f;
            float z = worldPosition.getZ() + 0.25f + level.random.nextFloat() * 0.5f;
            FXDispatcher.INSTANCE.drawLevitatorParticles(x, y, z,
                    facing.getStepX() / 50.0,
                    facing.getStepY() / 50.0,
                    facing.getStepZ() / 50.0);
        }
    }
    
    private void drawFXAt(Entity entity) {
        if (level.isClientSide && level.random.nextFloat() < 0.1f) {
            float x = (float)(entity.getX() + (level.random.nextFloat() - level.random.nextFloat()) * entity.getBbWidth());
            float y = (float)(entity.getY() + level.random.nextFloat() * entity.getBbHeight());
            float z = (float)(entity.getZ() + (level.random.nextFloat() - level.random.nextFloat()) * entity.getBbWidth());
            FXDispatcher.INSTANCE.drawLevitatorParticles(x, y, z,
                    (level.random.nextFloat() - level.random.nextFloat()) * 0.01,
                    (level.random.nextFloat() - level.random.nextFloat()) * 0.01,
                    (level.random.nextFloat() - level.random.nextFloat()) * 0.01);
        }
    }
    
    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
    
    // ==================== Configuration ====================
    
    /**
     * Get the vis cost per entity per tick.
     */
    public int getCost() {
        return RANGES[rangeIndex] * 2;
    }
    
    /**
     * Get the current max range.
     */
    public int getRange() {
        return RANGES[rangeIndex];
    }
    
    /**
     * Cycle to the next range setting.
     */
    public void increaseRange(Player player) {
        rangeActual = 0;
        if (!level.isClientSide) {
            rangeIndex++;
            if (rangeIndex >= RANGES.length) {
                rangeIndex = 0;
            }
            setChanged();
            syncTile(false);
            player.displayClientMessage(
                    Component.translatable("tc.levitator", RANGES[rangeIndex], getCost()),
                    true);
        }
    }
    
    /**
     * Get the facing direction from block state.
     */
    private Direction getFacing() {
        BlockState state = getBlockState();
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.getValue(BlockStateProperties.FACING);
        }
        return Direction.UP;
    }
    
    /**
     * Check if the levitator is enabled (not powered by redstone).
     */
    private boolean isEnabled() {
        // Check for redstone power - levitator is disabled when powered
        return !level.hasNeighborSignal(worldPosition);
    }
    
    // ==================== NBT ====================
    
    @Override
    protected void writeSyncNBT(CompoundTag tag) {
        super.writeSyncNBT(tag);
        tag.putByte("range", (byte)rangeIndex);
        tag.putInt("vis", vis);
    }
    
    @Override
    protected void readSyncNBT(CompoundTag tag) {
        super.readSyncNBT(tag);
        rangeIndex = tag.getByte("range");
        if (rangeIndex < 0 || rangeIndex >= RANGES.length) {
            rangeIndex = 1;
        }
        vis = tag.getInt("vis");
    }
}




