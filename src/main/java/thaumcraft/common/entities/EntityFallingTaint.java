package thaumcraft.common.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import thaumcraft.init.ModEntities;

/**
 * EntityFallingTaint - A falling taint block entity.
 * Similar to FallingBlockEntity but for taint blocks.
 */
public class EntityFallingTaint extends Entity implements IEntityWithComplexSpawn {
    
    public BlockState fallTile;
    private BlockPos oldPos;
    public int fallTime;
    private int fallHurtMax = 40;
    private float fallHurtAmount = 2.0f;
    
    public EntityFallingTaint(EntityType<?> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }
    
    public EntityFallingTaint(Level level) {
        this(ModEntities.FALLING_TAINT.get(), level);
    }
    
    public EntityFallingTaint(Level level, double x, double y, double z, BlockState state, BlockPos originalPos) {
        this(ModEntities.FALLING_TAINT.get(), level);
        this.fallTile = state;
        this.oldPos = originalPos;
        this.blocksBuilding = true;
        setPos(x, y, z);
        setDeltaMovement(Vec3.ZERO);
        xo = x;
        yo = y;
        zo = z;
    }
    
    public BlockState getBlockState() {
        return fallTile;
    }
    
    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        // No synched data needed
    }
    
    @Override
    public boolean isPickable() {
        return !isRemoved();
    }
    
    @Override
    protected boolean repositionEntityAfterLoad() {
        return false;
    }
    
    @Override
    public void tick() {
        if (fallTile == null || fallTile.isAir()) {
            discard();
            return;
        }
        
        xo = getX();
        yo = getY();
        zo = getZ();
        
        ++fallTime;
        
        // Apply gravity
        setDeltaMovement(getDeltaMovement().add(0.0, -0.04, 0.0));
        move(MoverType.SELF, getDeltaMovement());
        
        // Apply drag
        setDeltaMovement(getDeltaMovement().scale(0.98));
        
        BlockPos currentPos = blockPosition();
        
        if (!level().isClientSide) {
            // First tick - remove original block
            if (fallTime == 1) {
                if (oldPos != null && !level().getBlockState(oldPos).equals(fallTile)) {
                    discard();
                    return;
                }
                if (oldPos != null) {
                    level().removeBlock(oldPos, false);
                }
            }
            
            // Check if landed
            if (onGround() || isTaintGooBelow(currentPos)) {
                // Bounce slightly
                setDeltaMovement(getDeltaMovement().multiply(0.7, -0.5, 0.7));
                
                // Place block if possible
                if (canPlace(currentPos)) {
                    // TODO: Play SoundsTC.gore
                    discard();
                    level().setBlock(currentPos, fallTile, 3);
                } else {
                    discard();
                }
            } else if ((fallTime > 100 && (currentPos.getY() < level().getMinBuildHeight() || 
                    currentPos.getY() > level().getMaxBuildHeight())) || fallTime > 600) {
                discard();
            }
        } else {
            // Client-side particles on landing
            if (onGround() || fallTime == 1) {
                // TODO: FXDispatcher.INSTANCE.taintLandFX
            }
        }
    }
    
    private boolean isTaintGooBelow(BlockPos pos) {
        // TODO: Check for BlocksTC.fluxGoo
        return false;
    }
    
    private boolean canPlace(BlockPos pos) {
        BlockState currentState = level().getBlockState(pos);
        // Can place in air or replaceable blocks
        if (currentState.isAir() || currentState.canBeReplaced()) {
            return true;
        }
        // TODO: Check for taint fiber or flux goo
        return false;
    }
    
    @Override
    public boolean causeFallDamage(float distance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
        return false;
    }
    
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (fallTile != null) {
            tag.put("BlockState", NbtUtils.writeBlockState(fallTile));
        }
        tag.putInt("Time", fallTime);
        tag.putFloat("FallHurtAmount", fallHurtAmount);
        tag.putInt("FallHurtMax", fallHurtMax);
        if (oldPos != null) {
            tag.putLong("Old", oldPos.asLong());
        }
    }
    
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("BlockState")) {
            fallTile = NbtUtils.readBlockState(level().holderLookup(net.minecraft.core.registries.Registries.BLOCK), tag.getCompound("BlockState"));
        } else {
            fallTile = Blocks.SAND.defaultBlockState();
        }
        fallTime = tag.getInt("Time");
        if (tag.contains("FallHurtAmount")) {
            fallHurtAmount = tag.getFloat("FallHurtAmount");
            fallHurtMax = tag.getInt("FallHurtMax");
        }
        if (tag.contains("Old")) {
            oldPos = BlockPos.of(tag.getLong("Old"));
        }
    }
    
    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        if (fallTile != null) {
            buffer.writeInt(Block.getId(fallTile));
        } else {
            buffer.writeInt(0);
        }
    }
    
    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buffer) {
        int stateId = buffer.readInt();
        if (stateId > 0) {
            fallTile = Block.stateById(stateId);
        }
    }
    
    @Override
    public SoundSource getSoundSource() {
        return SoundSource.BLOCKS;
    }
    
    @Override
    public boolean displayFireAnimation() {
        return false;
    }
}




