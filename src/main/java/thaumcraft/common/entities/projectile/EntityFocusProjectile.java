package thaumcraft.common.entities.projectile;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.client.fx.FocusFX;
import thaumcraft.init.ModEntities;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

/**
 * Focus Projectile Entity - A projectile that delivers focus effects on impact.
 * Supports bouncing, homing (seeking), and various other behaviors.
 */
public class EntityFocusProjectile extends ThrowableProjectile {
    
    /** No special behavior */
    public static final int SPECIAL_NONE = 0;
    /** Bounces off surfaces */
    public static final int SPECIAL_BOUNCY = 1;
    /** Seeks hostile entities */
    public static final int SPECIAL_SEEKING_HOSTILE = 2;
    /** Seeks friendly entities */
    public static final int SPECIAL_SEEKING_FRIENDLY = 3;
    
    private static final EntityDataAccessor<Integer> DATA_SPECIAL = 
            SynchedEntityData.defineId(EntityFocusProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_OWNER_ID = 
            SynchedEntityData.defineId(EntityFocusProjectile.class, EntityDataSerializers.INT);
    /**
     * Focus effect keys carried by this projectile ("thaumcraft.FIRE%..."). The
     * full focus package is not synced to tracking clients, so the effect keys
     * needed for the particle trail travel in synced entity data (TC4 sent the
     * package in the custom spawn packet).
     */
    private static final EntityDataAccessor<String> DATA_PARTS = 
            SynchedEntityData.defineId(EntityFocusProjectile.class, EntityDataSerializers.STRING);
    
    private FocusPackage focusPackage;
    private Entity seekTarget;
    /** TC4 EntityFocusProjectile.firstParticle - one full effect burst per projectile. */
    private boolean firstParticle = true;
    
    public EntityFocusProjectile(EntityType<? extends EntityFocusProjectile> type, Level level) {
        super(type, level);
    }
    
    public EntityFocusProjectile(Level level, LivingEntity owner) {
        super(ModEntities.FOCUS_PROJECTILE.get(), owner, level);
    }
    
    /**
     * Create a focus projectile with full parameters.
     */
    public EntityFocusProjectile(FocusPackage pack, float speed, Trajectory trajectory, int special) {
        super(ModEntities.FOCUS_PROJECTILE.get(), pack.world);
        
        this.focusPackage = pack;
        setPartsFromPackage(pack);
        
        // Find owner from package
        LivingEntity owner = findOwner(pack);
        if (owner != null) {
            setOwner(owner);
            setOwnerId(owner.getId());
        }
        
        // Position slightly in front of the caster
        double offsetScale = owner != null ? owner.getBbWidth() * 2.1 : 0.5;
        Vec3 startPos = trajectory.source.add(trajectory.direction.scale(offsetScale));
        setPos(startPos.x, startPos.y, startPos.z);
        
        // Set velocity
        Vec3 dir = trajectory.direction.normalize();
        shoot(dir.x, dir.y, dir.z, speed, 0.0f);
        
        setSpecial(special);
    }
    
    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        builder.define(DATA_SPECIAL, 0);
        builder.define(DATA_OWNER_ID, 0);
        builder.define(DATA_PARTS, "");
    }

    /**
     * Store the focus effect keys for the client-side particle trail.
     */
    public void setFocusParts(String[] parts) {
        if (parts == null || parts.length == 0) {
            this.entityData.set(DATA_PARTS, "");
            return;
        }
        this.entityData.set(DATA_PARTS, String.join("%", parts));
    }

    /**
     * Focus effect keys carried by this projectile (empty on the server side
     * until a package is assigned).
     */
    public String[] getFocusParts() {
        String parts = this.entityData.get(DATA_PARTS);
        return parts.isEmpty() ? new String[0] : parts.split("%");
    }

    private void setPartsFromPackage(FocusPackage pack) {
        if (pack == null) {
            setFocusParts(new String[0]);
            return;
        }
        FocusEffect[] effects = pack.getFocusEffects();
        String[] keys = new String[effects.length];
        for (int i = 0; i < effects.length; i++) {
            keys[i] = effects[i].getKey();
        }
        setFocusParts(keys);
    }
    
    public void setSpecial(int special) {
        this.entityData.set(DATA_SPECIAL, special);
    }
    
    public int getSpecial() {
        return this.entityData.get(DATA_SPECIAL);
    }
    
    public void setOwnerId(int id) {
        this.entityData.set(DATA_OWNER_ID, id);
    }
    
    public int getOwnerId() {
        return this.entityData.get(DATA_OWNER_ID);
    }
    
    @Override
    protected double getDefaultGravity() {
        // Seeking projectiles have less gravity
        return getSpecial() > 1 ? 0.005f : 0.01f;
    }
    
    @Override
    protected void onHit(HitResult result) {
        if (result == null) return;
        
        int special = getSpecial();
        
        // Handle bouncy projectiles
        if (special == SPECIAL_BOUNCY && result.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) result;
            BlockState state = level().getBlockState(blockHit.getBlockPos());
            AABB collision = state.getCollisionShape(level(), blockHit.getBlockPos()).bounds();
            
            if (!collision.equals(AABB.ofSize(Vec3.ZERO, 0, 0, 0))) {
                // Bounce off the surface
                Vec3 motion = getDeltaMovement();
                
                switch (blockHit.getDirection().getAxis()) {
                    case X -> setDeltaMovement(motion.x * -0.9, motion.y * 0.9, motion.z * 0.9);
                    case Y -> setDeltaMovement(motion.x * 0.9, motion.y * -0.9, motion.z * 0.9);
                    case Z -> setDeltaMovement(motion.x * 0.9, motion.y * 0.9, motion.z * -0.9);
                }
                
                // Move back slightly
                Vec3 newMotion = getDeltaMovement();
                double len = newMotion.length();
                if (len > 0) {
                    setPos(getX() - newMotion.x / len * 0.05,
                           getY() - newMotion.y / len * 0.05,
                           getZ() - newMotion.z / len * 0.05);
                }
                
                playSound(SoundEvents.LEASH_KNOT_PLACE, 0.25f, 1.0f);
                
                // Die if too slow
                if (newMotion.length() < 0.2) {
                    if (!level().isClientSide) {
                        discard();
                    }
                }
                return;
            }
        }
        
        // Normal impact - execute focus effects
        if (!level().isClientSide) {
            executeFocusPackage(result);
            discard();
        }
    }
    
    @Override
    protected void onHitEntity(EntityHitResult result) {
        // Handled by onHit
    }
    
    @Override
    protected void onHitBlock(BlockHitResult result) {
        // Handled by onHit
    }
    
    /**
     * Execute the focus package at the hit location.
     */
    private void executeFocusPackage(HitResult hit) {
        if (focusPackage == null) return;

        Vec3 prevPos = new Vec3(xOld, yOld, zOld);
        Vec3 motion = getDeltaMovement().normalize();

        // Execute focus package effects at the impact point
        FocusEngine.runFocusPackage(focusPackage,
            new Trajectory[] { new Trajectory(prevPos, motion) },
            new HitResult[] { hit });
    }
    
    @Override
    public void tick() {
        super.tick();
        
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
        
        // Client-side particle trail (TC4 EntityFocusProjectile.renderParticle)
        if (level().isClientSide) {
            renderParticle(1.0f);
        }
        
        // Handle seeking behavior
        int special = getSpecial();
        if (special == SPECIAL_SEEKING_HOSTILE || special == SPECIAL_SEEKING_FRIENDLY) {
            updateSeeking(special == SPECIAL_SEEKING_FRIENDLY);
        }
    }
    
    /**
     * TC4 EntityFocusProjectile.renderParticle: spawn the coloured mote trail
     * behind the projectile and, on the very first tick, one full burst of the
     * focus effect's own particles so the projectile matches the cast focus.
     */
    public void renderParticle(float coeff) {
        String[] parts = getFocusParts();
        if (parts.length == 0) return;
        
        double px = xOld + (getX() - xOld) * coeff;
        double py = yOld + (getY() - yOld) * coeff + getBbHeight() / 2.0f;
        double pz = zOld + (getZ() - zOld) * coeff;
        
        FocusFX.projectileTrail(level(), parts, px, py, pz,
                random.nextGaussian() * 0.01,
                random.nextGaussian() * 0.01,
                random.nextGaussian() * 0.01,
                firstParticle);
        firstParticle = false;
    }
    
    /**
     * Update seeking behavior - find and track targets.
     */
    private void updateSeeking(boolean seekFriendly) {
        // Only search for new targets every 5 ticks
        if (seekTarget == null && tickCount % 5 == 0) {
            seekTarget = findSeekTarget(seekFriendly);
        }
        
        // Adjust trajectory towards target
        if (seekTarget != null) {
            if (seekTarget.isRemoved() || !canSee(seekTarget)) {
                seekTarget = null;
            } else {
                Vec3 targetPos = seekTarget.getBoundingBox().getCenter();
                Vec3 toTarget = targetPos.subtract(position()).normalize();
                
                Vec3 motion = getDeltaMovement();
                double speed = motion.length();
                Vec3 newDir = motion.normalize().add(toTarget.scale(0.275)).normalize();
                
                setDeltaMovement(newDir.scale(speed));
            }
        }
    }
    
    /**
     * Find a target for seeking projectiles.
     */
    @Nullable
    private Entity findSeekTarget(boolean seekFriendly) {
        double range = 16.0;
        AABB searchBox = getBoundingBox().inflate(range);
        
        Predicate<Entity> filter = e -> {
            if (!(e instanceof LivingEntity living)) return false;
            if (e.isRemoved()) return false;
            if (e == getOwner()) return false;
            if (!canSee(e)) return false;
            
            boolean isFriendly = isFriendlyTo(living);
            return seekFriendly ? isFriendly : !isFriendly;
        };
        
        List<Entity> entities = level().getEntities(this, searchBox, filter);
        
        // Return closest valid target
        Entity closest = null;
        double closestDist = range * range;
        
        for (Entity e : entities) {
            double dist = distanceToSqr(e);
            if (dist < closestDist) {
                closestDist = dist;
                closest = e;
            }
        }
        
        return closest;
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
    
    /**
     * Check if we can see an entity.
     */
    private boolean canSee(Entity target) {
        Vec3 start = getEyePosition();
        Vec3 end = target.getEyePosition();
        return level().clip(new net.minecraft.world.level.ClipContext(
                start, end,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                this)).getType() == HitResult.Type.MISS;
    }
    
    @Override
    public Vec3 getLookAngle() {
        return getDeltaMovement().normalize();
    }
    
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("special", getSpecial());
        if (focusPackage != null) {
            tag.put("pack", focusPackage.serialize());
        }
    }
    
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setSpecial(tag.getInt("special"));
        if (tag.contains("pack")) {
            focusPackage = new FocusPackage();
            focusPackage.deserialize(tag.getCompound("pack"));
        }
        if (getOwner() != null) {
            setOwnerId(getOwner().getId());
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
        setPartsFromPackage(pack);
    }
}




