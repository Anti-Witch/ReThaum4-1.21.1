package thaumcraft.common.entities.monster;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.init.ModEntities;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * EntitySpellBat - A magical bat summoned by focus spells.
 * Can be friendly (healing allies) or hostile (attacking enemies).
 * Executes focus effects on targets it attacks.
 */
public class EntitySpellBat extends Monster implements IEntityWithComplexSpawn {
    
    private static final EntityDataAccessor<Boolean> DATA_FRIENDLY = 
            SynchedEntityData.defineId(EntitySpellBat.class, EntityDataSerializers.BOOLEAN);
    
    private BlockPos currentFlightTarget;
    public LivingEntity owner;
    private UUID ownerUUID;
    private FocusPackage focusPackage;
    /** Cached focus effects (client side) for the particle trail. */
    private FocusEffect[] effects;
    private int attackTime;
    public int damBonus = 0;
    public int color = 0xFFFFFF;
    
    public EntitySpellBat(EntityType<? extends EntitySpellBat> type, Level level) {
        super(type, level);
    }
    
    public EntitySpellBat(Level level) {
        this(ModEntities.SPELL_BAT.get(), level);
    }
    
    public EntitySpellBat(Level level, FocusPackage pack, boolean friendly) {
        this(ModEntities.SPELL_BAT.get(), level);
        this.focusPackage = pack;
        setOwner(pack.getCaster());
        setFriendly(friendly);
    }
    
    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FRIENDLY, false);
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 5.0)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3);
    }
    
    public boolean isFriendly() {
        return this.entityData.get(DATA_FRIENDLY);
    }
    
    public void setFriendly(boolean friendly) {
        this.entityData.set(DATA_FRIENDLY, friendly);
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
    
    public FocusPackage getFocusPackage() {
        return focusPackage;
    }
    
    @Override
    public float getLightLevelDependentMagicValue() {
        return 1.0f; // Always bright
    }
    
    @Override
    protected float getSoundVolume() {
        return 0.1f;
    }
    
    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.95f;
    }
    
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BAT_AMBIENT;
    }
    
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BAT_HURT;
    }
    
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BAT_DEATH;
    }
    
    @Override
    public boolean isPushable() {
        return false;
    }
    

    
    @Override
    public boolean isAlliedTo(Entity other) {
        LivingEntity owner = getOwner();
        if (other == owner) {
            return true;
        }
        if (owner != null) {
            return owner.isAlliedTo(other) || other.isAlliedTo(owner);
        }
        return super.isAlliedTo(other);
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // Dampen vertical movement (floaty flight)
        setDeltaMovement(getDeltaMovement().multiply(1.0, 0.6, 1.0));
        
        // Despawn after timeout or if owner is gone
        if (!level().isClientSide && (tickCount > 600 || getOwner() == null)) {
            discard();
            return;
        }
        
        // Client-side particle effects (TC4 EntitySpellBat.onUpdate)
        if (level().isClientSide && isAlive() && focusPackage != null) {
            if (effects == null) {
                effects = focusPackage.getFocusEffects();
                if (effects.length > 0) {
                    int r = 0, g = 0, b = 0;
                    for (FocusEffect ef : effects) {
                        java.awt.Color c = new java.awt.Color(FocusEngine.getElementColor(ef.getKey()));
                        r += c.getRed();
                        g += c.getGreen();
                        b += c.getBlue();
                    }
                    color = new java.awt.Color(r / effects.length, g / effects.length, b / effects.length).getRGB();
                }
            }
            if (effects.length > 0) {
                FocusEffect eff = effects[random.nextInt(effects.length)];
                eff.renderParticleFX(level(),
                        getX() + random.nextGaussian() * 0.125,
                        getY() + getBbHeight() / 2.0f + random.nextGaussian() * 0.125,
                        getZ() + random.nextGaussian() * 0.125,
                        0.0, 0.0, 0.0);
            }
        }
    }
    
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        
        if (attackTime > 0) {
            --attackTime;
        }
        
        BlockPos currentPos = blockPosition();
        
        // Flight AI
        if (getTarget() == null) {
            // Wander around
            if (currentFlightTarget != null && 
                    (!level().isEmptyBlock(currentFlightTarget) || currentFlightTarget.getY() < 1)) {
                currentFlightTarget = null;
            }
            
            if (currentFlightTarget == null || random.nextInt(30) == 0 || 
                    currentPos.distSqr(currentFlightTarget) < 4.0) {
                currentFlightTarget = new BlockPos(
                        (int) getX() + random.nextInt(7) - random.nextInt(7),
                        (int) getY() + random.nextInt(6) - 2,
                        (int) getZ() + random.nextInt(7) - random.nextInt(7));
            }
            
            moveTowardsTarget(currentFlightTarget.getX() + 0.5, 
                    currentFlightTarget.getY() + 0.1,
                    currentFlightTarget.getZ() + 0.5);
        } else {
            // Move toward attack target
            moveTowardsTarget(getTarget().getX(),
                    getTarget().getY() + getTarget().getEyeHeight() * 0.66,
                    getTarget().getZ());
        }
        
        // Find target if we don't have one
        if (getTarget() == null) {
            setTarget(findTargetToAttack());
        } else if (getTarget().isAlive()) {
            float dist = distanceTo(getTarget());
            if (isAlive() && hasLineOfSight(getTarget())) {
                attackEntity(getTarget(), dist);
            }
        } else {
            setTarget(null);
        }
        
        // Don't attack creative players
        if (!isFriendly() && getTarget() instanceof Player player && player.getAbilities().invulnerable) {
            setTarget(null);
        }
    }
    
    private void moveTowardsTarget(double x, double y, double z) {
        double dx = x - getX();
        double dy = y - getY();
        double dz = z - getZ();
        
        Vec3 delta = getDeltaMovement();
        setDeltaMovement(
                delta.x + (Math.signum(dx) * 0.5 - delta.x) * 0.1,
                delta.y + (Math.signum(dy) * 0.7 - delta.y) * 0.1,
                delta.z + (Math.signum(dz) * 0.5 - delta.z) * 0.1
        );
        
        float yaw = (float)(Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0f;
        float yawDiff = Mth.wrapDegrees(yaw - getYRot());
        zza = 0.5f;
        setYRot(getYRot() + yawDiff);
    }
    
    protected void attackEntity(Entity target, float distance) {
        if (attackTime <= 0 && distance < Math.max(2.5f, target.getBbWidth() * 1.1f) &&
                target.getBoundingBox().maxY > getBoundingBox().minY &&
                target.getBoundingBox().minY < getBoundingBox().maxY) {
            
            attackTime = 40;
            
            if (!level().isClientSide) {
                // Execute focus package on target
                // TODO: Integrate with FocusEngine when fully implemented
                // RayTraceResult ray = new RayTraceResult(target);
                // Trajectory tra = new Trajectory(position(), ...);
                // FocusEngine.runFocusPackage(focusPackage.copy(getOwner()), ...);
                
                // Self-damage for attacking
                setHealth(getHealth() - 1.0f);
            }
            
            playSound(SoundEvents.BAT_HURT, 0.5f, 0.9f + random.nextFloat() * 0.2f);
        }
    }
    
    @Nullable
    protected LivingEntity findTargetToAttack() {
        double range = 12.0;
        List<LivingEntity> entities = level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(range),
                e -> e != this && e.isAlive() && !e.isSpectator());
        
        double closestDist = Double.MAX_VALUE;
        LivingEntity closest = null;
        
        for (LivingEntity e : entities) {
            // Skip based on friendly/hostile mode
            if (isFriendly()) {
                // Only target friendlies (to heal/buff)
                if (!isAlliedTo(e) && e != getOwner()) {
                    continue;
                }
            } else {
                // Skip friendlies when hostile
                if (isAlliedTo(e) || e == getOwner()) {
                    continue;
                }
            }
            
            double dist = distanceToSqr(e);
            if (dist < closestDist) {
                closestDist = dist;
                closest = e;
            }
        }
        
        return closest;
    }
    
    @Override
    protected void pushEntities() {
        if (!isFriendly()) {
            super.pushEntities();
        }
    }
    
    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }
    
    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false; // Flying - no fall damage
    }
    
    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
        // Flying - no fall damage
    }
    
    @Override
    public boolean isIgnoringBlockTriggers() {
        return true; // Don't trigger pressure plates
    }
    
    @Override
    protected boolean isAffectedByFluids() {
        return false;
    }
    
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerUUID != null) {
            tag.putUUID("OwnerUUID", ownerUUID);
        }
        tag.putBoolean("friendly", isFriendly());
        if (focusPackage != null) {
            tag.put("pack", focusPackage.serialize());
        }
    }
    
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("OwnerUUID")) {
            ownerUUID = tag.getUUID("OwnerUUID");
        }
        setFriendly(tag.getBoolean("friendly"));
        if (tag.contains("pack")) {
            focusPackage = new FocusPackage();
            try {
                focusPackage.deserialize(tag.getCompound("pack"));
            } catch (Exception ignored) {}
        }
    }
    
    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        if (focusPackage != null) {
            CompoundTag tag = focusPackage.serialize();
            buffer.writeNbt(tag);
        } else {
            buffer.writeNbt(new CompoundTag());
        }
    }
    
    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buffer) {
        try {
            CompoundTag tag = buffer.readNbt();
            if (tag != null && !tag.isEmpty()) {
                focusPackage = new FocusPackage();
                focusPackage.deserialize(tag);
            }
        } catch (Exception ignored) {}
    }
}




