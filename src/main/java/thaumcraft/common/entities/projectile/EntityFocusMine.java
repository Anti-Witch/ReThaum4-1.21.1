package thaumcraft.common.entities.projectile;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.init.ModEntities;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Focus Mine Entity - A proximity mine that triggers when entities approach.
 * Can be configured to target enemies or friendlies.
 */
public class EntityFocusMine extends ThrowableProjectile {
    
    private static final EntityDataAccessor<Boolean> DATA_ARMED = 
            SynchedEntityData.defineId(EntityFocusMine.class, EntityDataSerializers.BOOLEAN);
    
    private FocusPackage focusPackage;
    private boolean targetFriendly;
    private int armingCountdown = 40; // Ticks before mine is active after landing
    
    public EntityFocusMine(EntityType<? extends EntityFocusMine> type, Level level) {
        super(type, level);
    }
    
    public EntityFocusMine(Level level, LivingEntity owner) {
        super(ModEntities.FOCUS_MINE.get(), owner, level);
    }
    
    /**
     * Create a focus mine with full parameters.
     */
    public EntityFocusMine(FocusPackage pack, Trajectory trajectory, boolean targetFriendly) {
        super(ModEntities.FOCUS_MINE.get(), pack.world);
        
        this.focusPackage = pack;
        this.targetFriendly = targetFriendly;
        
        // Find owner from package
        LivingEntity owner = findOwner(pack);
        if (owner != null) {
            setOwner(owner);
        }
        
        setPos(trajectory.source.x, trajectory.source.y, trajectory.source.z);
        
        // Mines don't move much - just a slight drop
        shoot(trajectory.direction.x, trajectory.direction.y, trajectory.direction.z, 0.0f, 0.0f);
    }
    
    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        builder.define(DATA_ARMED, false);
    }
    
    public boolean isArmed() {
        return this.entityData.get(DATA_ARMED);
    }
    
    public void setArmed(boolean armed) {
        this.entityData.set(DATA_ARMED, armed);
        if (armed) {
            armingCountdown = 0;
        }
    }
    
    @Override
    protected double getDefaultGravity() {
        return 0.01f;
    }
    
    @Override
    protected void onHit(HitResult result) {
        if (result != null && getOwner() != null) {
            // Mine lands and starts arming
            setArmed(true);
            
            // Stop movement
            setDeltaMovement(Vec3.ZERO);
        }
    }
    
    @Override
    protected void onHitEntity(EntityHitResult result) {
        // Mines don't trigger on entity collision during flight
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // Push out of blocks
        if (isInWall()) {
            Vec3 pos = position();
            Vec3 motion = getDeltaMovement();
            setDeltaMovement(motion.scale(0.25));
        }
        
        // Expire after 60 seconds
        if (tickCount > 1200) {
            discard();
            return;
        }
        
        // Expire if owner is gone (server only)
        if (!level().isClientSide && getOwner() == null) {
            discard();
            return;
        }
        
        if (isAlive() && isArmed()) {
            // Count down arming delay
            if (armingCountdown > 0) {
                armingCountdown--;
                return;
            }
            
            // Check for targets every 5 ticks
            if (tickCount % 5 == 0) {
                if (level().isClientSide) {
                    renderMineParticles();
                } else {
                    checkForTargets();
                }
            }
        }
    }
    
    /**
     * Render particles on the client when armed.
     */
    private void renderMineParticles() {
        // TODO: Implement particle rendering
        // Original called effect.renderParticleFX
    }
    
    /**
     * Check for valid targets in range and trigger if found.
     */
    private void checkForTargets() {
        if (focusPackage == null) return;
        
        double triggerRange = 1.0;
        AABB searchBox = getBoundingBox().inflate(triggerRange);
        
        List<Entity> entities = level().getEntities(this, searchBox, e -> {
            if (e.isRemoved()) return false;
            if (!(e instanceof LivingEntity living)) return false;
            if (e == getOwner()) return false;
            
            boolean isFriendly = isFriendlyTo(living);
            return targetFriendly ? isFriendly : !isFriendly;
        });
        
        int triggered = 0;
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living) {
                Vec3 entityPos = living.getBoundingBox().getCenter();
                Vec3 direction = entityPos.subtract(position()).normalize();

                // Execute the remaining focus package effects on the target
                Trajectory trajectory = new Trajectory(position(), direction);
                EntityHitResult hit = new EntityHitResult(living, entityPos);
                FocusEngine.runFocusPackage(focusPackage,
                    new Trajectory[] { trajectory }, new HitResult[] { hit });

                triggered++;
            }
        }
        
        // Mine explodes after triggering
        if (triggered > 0) {
            discard();
        }
    }
    
    /**
     * Check if an entity is friendly to the owner.
     */
    private boolean isFriendlyTo(LivingEntity target) {
        Entity owner = getOwner();
        if (owner == null) return false;
        
        // Same team = friendly
        if (owner.isAlliedTo(target)) return true;
        
        // Same type = potentially friendly
        if (owner.getType() == target.getType()) return true;
        
        // Check if target is owned by owner (pets, tamed animals)
        if (target instanceof net.minecraft.world.entity.OwnableEntity ownable) {
            if (owner.getUUID().equals(ownable.getOwnerUUID())) return true;
        }
        
        return false;
    }
    
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("armed", isArmed());
        tag.putBoolean("friendly", targetFriendly);
        if (focusPackage != null) {
            tag.put("pack", focusPackage.serialize());
        }
    }
    
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        targetFriendly = tag.getBoolean("friendly");
        setArmed(tag.getBoolean("armed"));
        if (isArmed()) {
            armingCountdown = 0;
        }
        if (tag.contains("pack")) {
            focusPackage = new FocusPackage();
            focusPackage.deserialize(tag.getCompound("pack"));
        }
    }
    
    /**
     * Find the owner LivingEntity from a FocusPackage.
     */
    @Nullable
    private LivingEntity findOwner(FocusPackage pack) {
        if (pack.getCasterUUID() == null || pack.world == null) return null;
        
        for (var player : pack.world.players()) {
            if (player.getUUID().equals(pack.getCasterUUID())) {
                return player;
            }
        }
        return null;
    }
    
    public FocusPackage getFocusPackage() {
        return focusPackage;
    }
    
    public void setFocusPackage(FocusPackage pack) {
        this.focusPackage = pack;
    }
    
    public boolean isTargetingFriendly() {
        return targetFriendly;
    }
}




