package thaumcraft.common.entities.projectile;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.init.ModEntities;

/**
 * EntityGolemOrb - Homing magic orb projectile fired by golems.
 * 
 * Features:
 * - Homes in on target
 * - Damage based on golem's attack attribute
 * - Can be deflected by hitting it
 * - Red variant does more damage and lasts longer
 */
public class EntityGolemOrb extends ThrowableProjectile {
    
    private static final EntityDataAccessor<Boolean> DATA_RED = 
            SynchedEntityData.defineId(EntityGolemOrb.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_TARGET_ID = 
            SynchedEntityData.defineId(EntityGolemOrb.class, EntityDataSerializers.INT);
    
    private LivingEntity target = null;
    
    public EntityGolemOrb(EntityType<? extends EntityGolemOrb> type, Level level) {
        super(type, level);
    }
    
    public EntityGolemOrb(Level level, LivingEntity owner, LivingEntity target, boolean red) {
        super(ModEntities.GOLEM_ORB.get(), owner, level);
        this.target = target;
        entityData.set(DATA_RED, red);
        entityData.set(DATA_TARGET_ID, target.getId());
    }
    
    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        builder.define(DATA_RED, false);
        builder.define(DATA_TARGET_ID, -1);
    }
    
    public boolean isRed() {
        return entityData.get(DATA_RED);
    }
    
    @Override
    protected double getDefaultGravity() {
        return 0.0f; // No gravity
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // Resolve target from synced ID if needed
        if (target == null) {
            int targetId = entityData.get(DATA_TARGET_ID);
            if (targetId >= 0) {
                Entity e = level().getEntity(targetId);
                if (e instanceof LivingEntity le) {
                    target = le;
                }
            }
        }
        
        // Die after timeout (red lasts longer)
        int maxLife = isRed() ? 240 : 160;
        if (tickCount > maxLife) {
            discard();
            return;
        }
        
        // Home toward target
        if (target != null && target.isAlive()) {
            double distSq = distanceToSqr(target);
            double dx = target.getX() - getX();
            double dy = target.getY() + target.getBbHeight() * 0.6 - getY();
            double dz = target.getZ() - getZ();
            
            double accel = 0.2;
            dx = dx / distSq * accel;
            dy = dy / distSq * accel;
            dz = dz / distSq * accel;
            
            Vec3 motion = getDeltaMovement();
            setDeltaMovement(
                Mth.clamp(motion.x + dx, -0.25, 0.25),
                Mth.clamp(motion.y + dy, -0.25, 0.25),
                Mth.clamp(motion.z + dz, -0.25, 0.25)
            );
        }
        
        // Client particles
        if (level().isClientSide) {
            int color = isRed() ? 0xFF0000 : 0x00FFFF;
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;
            
            level().addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, r, g, b),
                getX(), getY(), getZ(), 0, 0, 0);
        }
    }
    
    @Override
    protected void onHit(HitResult result) {
        if (result.getType() == HitResult.Type.ENTITY) {
            onHitEntity((EntityHitResult) result);
        }
        
        // Play sound and burst particles
        playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f + (random.nextFloat() - random.nextFloat()) * 0.2f);
        
        if (level().isClientSide) {
            for (int i = 0; i < 8; i++) {
                level().addParticle(ParticleTypes.WITCH,
                    getX(), getY(), getZ(),
                    (random.nextFloat() - 0.5) * 0.5,
                    (random.nextFloat() - 0.5) * 0.5,
                    (random.nextFloat() - 0.5) * 0.5);
            }
        }
        
        discard();
    }
    
    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!level().isClientSide) {
            Entity owner = getOwner();
            if (owner instanceof LivingEntity livingOwner) {
                // Damage based on owner's attack damage
                double attackDamage = livingOwner.getAttributeValue(Attributes.ATTACK_DAMAGE);
                float damage = (float)(attackDamage * (isRed() ? 1.0 : 0.6));
                
                DamageSource source = level().damageSources().indirectMagic(this, owner);
                result.getEntity().hurt(source, damage);
            }
        }
    }
    
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) {
            return false;
        }
        
        // Can be deflected by hitting it
        Entity attacker = source.getEntity();
        if (attacker != null) {
            Vec3 look = attacker.getLookAngle();
            setDeltaMovement(look.scale(0.9));
            playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f + (random.nextFloat() - random.nextFloat()) * 0.2f);
            return true;
        }
        return false;
    }
    
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Red", isRed());
        if (target != null) {
            tag.putInt("TargetId", target.getId());
        }
    }
    
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_RED, tag.getBoolean("Red"));
        if (tag.contains("TargetId")) {
            entityData.set(DATA_TARGET_ID, tag.getInt("TargetId"));
        }
    }
}




