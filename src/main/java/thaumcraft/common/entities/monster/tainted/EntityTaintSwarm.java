package thaumcraft.common.entities.monster.tainted;
import net.minecraft.server.level.ServerLevel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import thaumcraft.init.ModEntities;
import thaumcraft.init.ModItems;
import thaumcraft.init.ModSounds;

/**
 * EntityTaintSwarm - A flying swarm of tainted insects.
 * Floats around and attacks players, applying weakness on hit.
 * Can be summoned by other entities (in which case it takes damage when no target).
 */
public class EntityTaintSwarm extends Monster {
    
    private static final EntityDataAccessor<Boolean> DATA_SUMMONED = 
            SynchedEntityData.defineId(EntityTaintSwarm.class, EntityDataSerializers.BOOLEAN);
    
    private BlockPos currentFlightTarget;
    public int damBonus = 0;
    private int attackTime = 0;
    
    public EntityTaintSwarm(EntityType<? extends EntityTaintSwarm> type, Level level) {
        super(type, level);
    }
    
    public EntityTaintSwarm(Level level) {
        super(ModEntities.TAINT_SWARM.get(), level);
    }
    
    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SUMMONED, false);
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FLYING_SPEED, 0.5)
                .add(Attributes.MOVEMENT_SPEED, 0.25);
    }
    
    public boolean isSummoned() {
        return this.entityData.get(DATA_SUMMONED);
    }
    
    public void setSummoned(boolean summoned) {
        this.entityData.set(DATA_SUMMONED, summoned);
    }
    
    /**
     * Check if entity is a tainted mob.
     */
    public boolean isTaintedMob(Entity entity) {
        return entity instanceof EntityTaintSwarm ||
               entity instanceof EntityTaintacle;
    }
    
    @Override
    public boolean canAttack(LivingEntity target) {
        if (isTaintedMob(target)) {
            return false;
        }
        return super.canAttack(target);
    }
    
    @Override
    public boolean isAlliedTo(Entity other) {
        if (isTaintedMob(other)) {
            return true;
        }
        return super.isAlliedTo(other);
    }
    
    @Override
    public float getLightLevelDependentMagicValue() {
        return 1.0f; // Always fully bright
    }
    
    @Override
    protected float getSoundVolume() {
        return 0.1f;
    }
    
    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.SWARM.get();
    }
    
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.SWARM_ATTACK.get();
    }
    
    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SWARM_ATTACK.get();
    }
    
    @Override
    public boolean isPushable() {
        return false;
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // Reduce vertical momentum
        Vec3 motion = getDeltaMovement();
        setDeltaMovement(motion.x, motion.y * 0.6, motion.z);
        
        // Client-side swarm particles
        if (level().isClientSide) {
            for (int i = 0; i < 3; i++) {
                double offsetX = (random.nextDouble() - 0.5) * getBbWidth() * 2;
                double offsetY = random.nextDouble() * getBbHeight();
                double offsetZ = (random.nextDouble() - 0.5) * getBbWidth() * 2;
                
                level().addParticle(ParticleTypes.SMOKE,
                        getX() + offsetX, getY() + offsetY, getZ() + offsetZ,
                        (random.nextDouble() - 0.5) * 0.05,
                        random.nextDouble() * 0.02,
                        (random.nextDouble() - 0.5) * 0.05);
            }
        }
    }
    
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        
        if (attackTime > 0) {
            attackTime--;
        }
        
        LivingEntity target = getTarget();
        
        if (target == null) {
            // Summoned swarms take damage when they have no target
            if (isSummoned()) {
                hurt(damageSources().generic(), 5.0f);
            }
            
            // Wander around
            if (currentFlightTarget != null && 
                    (!level().isEmptyBlock(currentFlightTarget) || 
                     currentFlightTarget.getY() < 1 ||
                     currentFlightTarget.getY() > level().getHeightmapPos(
                             net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                             currentFlightTarget).above(2).getY())) {
                currentFlightTarget = null;
            }
            
            if (currentFlightTarget == null || random.nextInt(30) == 0 || 
                    currentFlightTarget.distSqr(blockPosition()) < 4.0) {
                currentFlightTarget = new BlockPos(
                        (int)getX() + random.nextInt(7) - random.nextInt(7),
                        (int)getY() + random.nextInt(6) - 2,
                        (int)getZ() + random.nextInt(7) - random.nextInt(7));
            }
            
            flyToward(currentFlightTarget.getX() + 0.5, currentFlightTarget.getY() + 0.1, 
                    currentFlightTarget.getZ() + 0.5, 0.015, 0.1);
            
            // Look for targets
            if (!isSummoned()) {
                Player nearestPlayer = level().getNearestPlayer(this, 8.0);
                if (nearestPlayer != null && !nearestPlayer.getAbilities().invulnerable) {
                    setTarget(nearestPlayer);
                }
            }
        } else {
            // Chase target
            flyToward(target.getX(), target.getEyeY(), target.getZ(), 0.025, 0.1);
            
            // Attack if close enough
            if (target.isAlive() && hasLineOfSight(target)) {
                float dist = distanceTo(target);
                if (attackTime <= 0 && dist < 3.0f && 
                        target.getBoundingBox().maxY > getBoundingBox().minY &&
                        target.getBoundingBox().minY < getBoundingBox().maxY) {
                    
                    if (isSummoned()) {
                        target.invulnerableTime = 0; // Reset invulnerability for summoned swarms
                    }
                    
                    attackTime = 15 + random.nextInt(10);
                    
                    // Preserve target's momentum (swarm doesn't knock back)
                    Vec3 targetMotion = target.getDeltaMovement();
                    
                    if (doHurtTarget(target)) {
                        // Apply weakness (target is already LivingEntity from getTarget())
                        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
                    }
                    
                    // Restore momentum
                    target.setDeltaMovement(targetMotion);
                    target.hurtMarked = false;
                    
                    playSound(ModSounds.SWARM_ATTACK.get(), 0.3f, 0.9f + random.nextFloat() * 0.2f);
                }
            } else if (!target.isAlive()) {
                setTarget(null);
            }
            
            // Clear target if in creative
            if (target instanceof Player player && player.getAbilities().invulnerable) {
                setTarget(null);
            }
        }
    }
    
    private void flyToward(double x, double y, double z, double horizAccel, double vertAccel) {
        double dx = x - getX();
        double dy = y - getY();
        double dz = z - getZ();
        
        Vec3 motion = getDeltaMovement();
        setDeltaMovement(
                motion.x + (Math.signum(dx) * 0.5 - motion.x) * horizAccel,
                motion.y + (Math.signum(dy) * 0.7 - motion.y) * vertAccel,
                motion.z + (Math.signum(dz) * 0.5 - motion.z) * horizAccel);
        
        float yaw = (float)(Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0f;
        float yawDiff = Mth.wrapDegrees(yaw - getYRot());
        zza = 0.1f;
        setYRot(getYRot() + yawDiff);
    }
    
    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false; // Flying mob
    }
    
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean wasRecentlyHit) {
        super.dropCustomDeathLoot(level, source, wasRecentlyHit);
        
        // TC4: 50% chance to drop tainted goo (itemResource 11)
        if (random.nextBoolean()) {
            spawnAtLocation(new ItemStack(ModItems.TAINT_SLIME.get()));
        }
    }
    
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Summoned", isSummoned());
        tag.putByte("DamBonus", (byte)damBonus);
    }
    
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setSummoned(tag.getBoolean("Summoned"));
        damBonus = tag.getByte("DamBonus");
    }
}




