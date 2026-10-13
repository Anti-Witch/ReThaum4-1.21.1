package thaumcraft.common.tiles.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.common.tiles.TileThaumcraft;
import thaumcraft.init.ModBlockEntities;

import java.util.List;

/**
 * TileSpa - Sanitizing Spa that cleanses warp and negative effects.
 * 
 * Features:
 * - Drains vis from aura to power cleansing
 * - Removes negative potion effects from entities inside
 * - Slowly reduces temporary warp on players
 * - Provides regeneration effect when powered
 * - Generates flux as a byproduct
 * 
 * Ported from 1.12.2
 */
public class TileSpa extends TileThaumcraft implements Container {

    public static final int VIS_COST = 1;
    public static final int CLEANSE_INTERVAL = 20; // Every second

    // Current power level (vis stored)
    public int charge = 0;
    public static final int MAX_CHARGE = 100;
    
    // Mix mode toggle
    public boolean mixMode = false;
    
    // Inventory for bath salts
    private ItemStack bathSalts = ItemStack.EMPTY;

    // Tick counter
    private int tickCount = 0;

    // Animation (client-side)
    public float bubblePhase = 0;

    public TileSpa(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public TileSpa(BlockPos pos, BlockState state) {
        this(ModBlockEntities.SPA.get(), pos, state);
    }

    // ==================== NBT ====================

    @Override
    protected void writeSyncNBT(CompoundTag tag) {
        super.writeSyncNBT(tag);
        tag.putInt("Charge", charge);
        tag.putBoolean("MixMode", mixMode);
        if (!bathSalts.isEmpty()) {
            tag.put("BathSalts", bathSalts.save(getRegistryProvider(), new CompoundTag()));
        }
    }

    @Override
    protected void readSyncNBT(CompoundTag tag) {
        super.readSyncNBT(tag);
        charge = tag.getInt("Charge");
        mixMode = tag.getBoolean("MixMode");
        if (tag.contains("BathSalts")) {
            bathSalts = ItemStack.parseOptional(getRegistryProvider(), tag.getCompound("BathSalts"));
        } else {
            bathSalts = ItemStack.EMPTY;
        }
    }

    // ==================== Tick ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileSpa tile) {
        tile.tickCount++;

        // Recharge from aura
        if (tile.tickCount % 10 == 0 && tile.charge < MAX_CHARGE) {
            float drained = AuraHelper.drainVis(level, pos, VIS_COST, false);
            if (drained > 0) {
                tile.charge += (int) drained;
                if (tile.charge > MAX_CHARGE) tile.charge = MAX_CHARGE;
                tile.markDirtyAndSync();
            }
        }

        // Cleanse entities
        if (tile.tickCount % CLEANSE_INTERVAL == 0 && tile.charge > 0) {
            tile.cleanseEntities();
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileSpa tile) {
        // Bubble animation
        if (tile.charge > 0) {
            tile.bubblePhase += 0.1f;
            if (tile.bubblePhase > Math.PI * 2) {
                tile.bubblePhase -= (float) (Math.PI * 2);
            }
        }
    }

    /**
     * Cleanse entities in the spa.
     */
    private void cleanseEntities() {
        if (level == null || charge <= 0) return;

        // Search for entities in a 3x2x3 area centered on the spa
        AABB searchBox = new AABB(
                worldPosition.getX() - 1,
                worldPosition.getY(),
                worldPosition.getZ() - 1,
                worldPosition.getX() + 2,
                worldPosition.getY() + 2,
                worldPosition.getZ() + 2
        );

        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, searchBox);

        for (LivingEntity entity : entities) {
            if (charge <= 0) break;

            boolean cleansed = false;

            // Remove negative effects
            if (removeNegativeEffect(entity)) {
                cleansed = true;
                charge -= 5;
                
                // Generate flux as byproduct
                AuraHelper.polluteAura(level, worldPosition, 0.25f, true);
            }

            // Cleanse warp from players
            if (entity instanceof Player player) {
                if (cleanseWarp(player)) {
                    cleansed = true;
                    charge -= 10;
                    
                    // More flux for warp cleansing
                    AuraHelper.polluteAura(level, worldPosition, 0.5f, true);
                }
            }

            // Give regeneration if we have charge
            if (charge >= 5 && !entity.hasEffect(MobEffects.REGENERATION)) {
                entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, true, true));
                charge -= 2;
                cleansed = true;
            }

            if (cleansed) {
                markDirtyAndSync();
            }
        }
    }

    /**
     * Remove one negative potion effect from an entity.
     * @return true if an effect was removed
     */
    private boolean removeNegativeEffect(LivingEntity entity) {
        // List of negative effects to cleanse
        Holder<MobEffect>[] negativeEffects = new Holder[] {
                MobEffects.POISON,
                MobEffects.WITHER,
                MobEffects.HUNGER,
                MobEffects.WEAKNESS,
                MobEffects.MOVEMENT_SLOWDOWN,
                MobEffects.DIG_SLOWDOWN,
                MobEffects.BLINDNESS,
                MobEffects.CONFUSION,
                MobEffects.UNLUCK,
                MobEffects.BAD_OMEN
        };

        for (Holder<MobEffect> effect : negativeEffects) {
            if (entity.hasEffect(effect)) {
                entity.removeEffect(effect);
                return true;
            }
        }

        return false;
    }

    /**
     * Cleanse temporary warp from a player.
     * @return true if warp was cleansed
     */
    private boolean cleanseWarp(Player player) {
        IPlayerWarp warp = ThaumcraftCapabilities.getWarp(player);
        if (warp != null && warp.get(IPlayerWarp.EnumWarpType.TEMPORARY) > 0) {
            warp.add(IPlayerWarp.EnumWarpType.TEMPORARY, -1);
            
            // Sync to client
            if (player instanceof ServerPlayer serverPlayer) {
                warp.sync(serverPlayer);
            }
            return true;
        }
        return false;
    }

    // ==================== Getters ====================

    public int getCharge() {
        return charge;
    }

    public float getChargePercent() {
        return (float) charge / MAX_CHARGE;
    }

    public boolean isPowered() {
        return charge > 0;
    }
    
    /**
     * Toggle mix mode for combining effects.
     */
    public void toggleMix() {
        mixMode = !mixMode;
        markDirtyAndSync();
    }
    
    // ==================== Container Implementation ====================
    
    @Override
    public int getContainerSize() {
        return 1;
    }
    
    @Override
    public boolean isEmpty() {
        return bathSalts.isEmpty();
    }
    
    @Override
    public ItemStack getItem(int slot) {
        return slot == 0 ? bathSalts : ItemStack.EMPTY;
    }
    
    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot != 0 || bathSalts.isEmpty()) return ItemStack.EMPTY;
        
        ItemStack result = bathSalts.split(amount);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }
    
    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot != 0) return ItemStack.EMPTY;
        
        ItemStack result = bathSalts;
        bathSalts = ItemStack.EMPTY;
        return result;
    }
    
    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == 0) {
            bathSalts = stack;
            setChanged();
        }
    }
    
    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }
    
    @Override
    public void clearContent() {
        bathSalts = ItemStack.EMPTY;
    }
}




