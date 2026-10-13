package thaumcraft.common.entities.projectile;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Focus Cloud Entity - An area effect cloud that applies focus effects to entities within.
 * Periodically hits entities and blocks within its radius.
 */
public class EntityFocusCloud extends Entity {
    
    private static final EntityDataAccessor<Float> DATA_RADIUS = 
            SynchedEntityData.defineId(EntityFocusCloud.class, EntityDataSerializers.FLOAT);
    
    /** Cooldown map to prevent hitting the same target too often */
    private static final Map<Long, Long> COOLDOWN_MAP = new HashMap<>();
    
    private FocusPackage focusPackage;
    private LivingEntity owner;
    private UUID ownerUUID;
    private int duration; // Duration in seconds
    
    public EntityFocusCloud(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }
    
    /**
     * Create a focus cloud with full parameters.
     */
    public EntityFocusCloud(FocusPackage pack, Trajectory trajectory, float radius, int duration) {
        super(ModEntities.FOCUS_CLOUD.get(), pack.world);
        
        this.focusPackage = pack;
        this.duration = duration;
        this.noPhysics = true;
        
        setPos(trajectory.source.x, trajectory.source.y, trajectory.source.z);
        setRadius(radius);
        
        // Find owner from package
        if (pack.getCasterUUID() != null && pack.world != null) {
            for (var player : pack.world.players()) {
                if (player.getUUID().equals(pack.getCasterUUID())) {
                    setOwner(player);
                    break;
                }
            }
        }
    }
    
    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        builder.define(DATA_RADIUS, 0.5f);
    }
    
    public void setRadius(float radius) {
        if (!level().isClientSide) {
            this.entityData.set(DATA_RADIUS, radius);
        }
        // Update bounding box
        double x = getX();
        double y = getY();
        double z = getZ();
        setBoundingBox(new AABB(x - radius, y - 0.25, z - radius, x + radius, y + 0.25, z + radius));
    }
    
    public float getRadius() {
        return this.entityData.get(DATA_RADIUS);
    }
    
    public void setDuration(int duration) {
        this.duration = duration;
    }
    
    public int getDuration() {
        return duration;
    }
    
    public void setOwner(@Nullable LivingEntity owner) {
        this.owner = owner;
        this.ownerUUID = owner != null ? owner.getUUID() : null;
    }
    
    @Nullable
    public LivingEntity getOwner() {
        if (owner == null && ownerUUID != null && level() instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(ownerUUID);
            if (entity instanceof LivingEntity living) {
                owner = living;
            }
        }
        return owner;
    }
    
    @Override
    public void tick() {
        super.tick();
        
        float radius = getRadius();
        int durationTicks = duration * 20;
        
        // Expire after duration
        if (!level().isClientSide && (tickCount > durationTicks || getOwner() == null)) {
            discard();
            return;
        }
        
        if (isAlive()) {
            if (level().isClientSide) {
                // Client-side: render particles
                renderCloudParticles(radius);
            } else {
                // Server-side: apply effects every 5 ticks
                if (tickCount % 5 == 0) {
                    applyEffects(radius);
                }
            }
        }
    }
    
    /**
     * Render particles on the client.
     */
    private void renderCloudParticles(float radius) {
        // TODO: Implement particle rendering
        // Original used FXDispatcher.drawFocusCloudParticle
        // and called effect.renderParticleFX
    }
    
    /**
     * Apply focus effects to entities and blocks within radius.
     */
    private void applyEffects(float radius) {
        if (focusPackage == null) return;
        
        long currentTime = System.currentTimeMillis();
        
        // Find entities in range
        AABB searchBox = getBoundingBox().inflate(radius);
        List<Entity> entities = level().getEntities(this, searchBox, e -> {
            if (e.isRemoved()) return false;
            if (e instanceof EntityFocusCloud) return false;
            if (!(e instanceof LivingEntity)) return false;
            
            // Check if within actual radius (sphere, not box)
            double distSq = e.distanceToSqr(this);
            return distSq <= radius * radius;
        });
        
        for (Entity entity : entities) {
            // Check cooldown
            long entityKey = entity.getId();
            if (COOLDOWN_MAP.containsKey(entityKey) && COOLDOWN_MAP.get(entityKey) > currentTime) {
                continue;
            }
            
            // Apply cooldown (2 seconds)
            COOLDOWN_MAP.put(entityKey, currentTime + 2000L);
            
            // Create trajectory and target for this entity
            Vec3 entityPos = entity.getBoundingBox().getCenter();
            Vec3 direction = entityPos.subtract(position()).normalize();

            // Execute the remaining focus package effects on the target
            Trajectory trajectory = new Trajectory(position(), direction);
            EntityHitResult hit = new EntityHitResult(entity, entityPos);
            FocusEngine.runFocusPackage(focusPackage,
                new Trajectory[] { trajectory }, new HitResult[] { hit });
        }
        
        // Also randomly hit blocks in radius
        for (int i = 0; i < (int) radius; i++) {
            Vec3 randomDir = new Vec3(
                    random.nextGaussian(),
                    random.nextGaussian(),
                    random.nextGaussian()
            ).normalize();
            
            var blockHit = level().clip(new net.minecraft.world.level.ClipContext(
                    position(),
                    position().add(randomDir.scale(radius)),
                    net.minecraft.world.level.ClipContext.Block.OUTLINE,
                    net.minecraft.world.level.ClipContext.Fluid.NONE,
                    this));
            
            if (blockHit != null && blockHit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                long blockKey = blockHit.getBlockPos().asLong();
                
                if (!COOLDOWN_MAP.containsKey(blockKey) || COOLDOWN_MAP.get(blockKey) <= currentTime) {
                    COOLDOWN_MAP.put(blockKey, currentTime + 2000L);

                    // Execute the remaining focus package for the block
                    Trajectory trajectory = new Trajectory(position(), randomDir);
                    FocusEngine.runFocusPackage(focusPackage,
                        new Trajectory[] { trajectory }, new HitResult[] { blockHit });
                }
            }
        }
        
        // Clean up old cooldown entries occasionally
        if (tickCount % 100 == 0) {
            COOLDOWN_MAP.entrySet().removeIf(entry -> entry.getValue() <= currentTime);
        }
    }
    
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", tickCount);
        tag.putInt("Duration", duration);
        tag.putFloat("Radius", getRadius());
        if (ownerUUID != null) {
            tag.putUUID("OwnerUUID", ownerUUID);
        }
        if (focusPackage != null) {
            tag.put("pack", focusPackage.serialize());
        }
    }
    
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        tickCount = tag.getInt("Age");
        duration = tag.getInt("Duration");
        setRadius(tag.getFloat("Radius"));
        if (tag.hasUUID("OwnerUUID")) {
            ownerUUID = tag.getUUID("OwnerUUID");
        }
        if (tag.contains("pack")) {
            focusPackage = new FocusPackage();
            focusPackage.deserialize(tag.getCompound("pack"));
        }
    }
    
    public FocusPackage getFocusPackage() {
        return focusPackage;
    }
    
    public void setFocusPackage(FocusPackage pack) {
        this.focusPackage = pack;
    }
}




