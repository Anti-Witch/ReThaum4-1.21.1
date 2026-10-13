package thaumcraft.common.entities.monster.boss;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.entities.IEldritchMob;
import thaumcraft.common.entities.projectile.EntityGolemOrb;
import thaumcraft.init.ModEntities;
import thaumcraft.init.ModSounds;

import javax.annotation.Nullable;

/**
 * EntityEldritchGolem - A large, powerful eldritch golem boss.
 * Features:
 * - Heavy melee attacks that knock targets upward
 * - When reduced to 0 health, becomes "headless" and more aggressive
 * - Headless form gains ranged attacks with golem orbs
 * - Destroys weak blocks when walking
 * - Fire immune
 */
public class EntityEldritchGolem extends EntityThaumcraftBoss implements IEldritchMob, RangedAttackMob {
    
    private static final EntityDataAccessor<Boolean> DATA_HEADLESS = 
            SynchedEntityData.defineId(EntityEldritchGolem.class, EntityDataSerializers.BOOLEAN);
    
    private int beamCharge = 0;
    private boolean chargingBeam = false;
    private int attackTimer = 0;
    
    public EntityEldritchGolem(EntityType<? extends EntityEldritchGolem> type, Level level) {
        super(type, level);
        this.xpReward = 50;
    }
    
    public EntityEldritchGolem(Level level) {
        this(ModEntities.ELDRITCH_GOLEM.get(), level);
    }
    
    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HEADLESS, false);
    }
    
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.1, false));
        this.goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 0.8));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return EntityThaumcraftBoss.createBossAttributes()
                .add(Attributes.MAX_HEALTH, 400.0)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ARMOR, 10.0);
    }
    
    // ==================== Headless State ====================
    
    public boolean isHeadless() {
        return this.entityData.get(DATA_HEADLESS);
    }
    
    public void setHeadless(boolean headless) {
        this.entityData.set(DATA_HEADLESS, headless);
    }
    
    /**
     * Called when becoming headless - adds ranged attack capability.
     */
    private void makeHeadless() {
        // Add ranged attack goal when headless
        this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 5, 5, 24.0f));
    }
        
    // ==================== NBT ====================
    
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("headless", isHeadless());
    }
    
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setHeadless(tag.getBoolean("headless"));
        if (isHeadless()) {
            makeHeadless();
        }
    }
    
    // ==================== Spawn ====================
    
    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            MobSpawnType spawnType, @Nullable SpawnGroupData spawnData) {
        
        spawnTimer = 100;
        return super.finalizeSpawn(level, difficulty, spawnType, spawnData);
    }
    
    // ==================== AI / Update ====================
    
    @Override
    public void aiStep() {
        super.aiStep();
        
        if (attackTimer > 0) {
            --attackTimer;
        }
        
        // Break soft blocks while walking
        Vec3 motion = getDeltaMovement();
        if (motion.x * motion.x + motion.z * motion.z > 2.5E-7 && random.nextInt(5) == 0) {
            BlockState bs = level().getBlockState(blockPosition());
            if (!bs.isAir()) {
                // Spawn block crack particles
                level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, bs),
                        getX() + (random.nextFloat() - 0.5) * getBbWidth(),
                        getBoundingBox().minY + 0.1,
                        getZ() + (random.nextFloat() - 0.5) * getBbWidth(),
                        4.0 * (random.nextFloat() - 0.5), 0.5, (random.nextFloat() - 0.5) * 4.0);
            }
        }
        
        // Destroy soft blocks underfoot
        if (!level().isClientSide) {
            BlockPos pos = blockPosition();
            BlockState bs = level().getBlockState(pos);
            float hardness = bs.getDestroySpeed(level(), pos);
            if (hardness >= 0.0f && hardness <= 0.15f) {
                level().destroyBlock(pos, true);
            }
        }
    }
    
    @Override
    public void tick() {
        if (spawnTimer == 150) {
            level().broadcastEntityEvent(this, (byte) 18);
        }
        
        if (spawnTimer > 0) {
            heal(2.0f);
        }
        
        super.tick();
        
        if (!level().isClientSide) {
            // Beam charging for headless mode
            if (isHeadless() && beamCharge <= 0) {
                chargingBeam = true;
            }
            
            if (isHeadless() && chargingBeam) {
                ++beamCharge;
                level().broadcastEntityEvent(this, (byte) 19);
                if (beamCharge == 150) {
                    chargingBeam = false;
                }
            }
        }
        
        // Client-side visual effects for headless mode
        if (level().isClientSide && isHeadless()) {
            setXRot(0.0f);
            // Spark particles from neck
            if (random.nextInt(20) == 0) {
                float yawRad = -yBodyRot * Mth.DEG_TO_RAD - Mth.PI;
                float lookX = Mth.sin(yawRad);
                float lookZ = Mth.cos(yawRad);
                float a = (random.nextFloat() - random.nextFloat()) / 3.0f;
                float b = (random.nextFloat() - random.nextFloat()) / 3.0f;
                level().addParticle(ParticleTypes.ELECTRIC_SPARK,
                        getX() + lookX + a, getY() + getEyeHeight() - 0.75f, getZ() + lookZ + b,
                        0, 0.1, 0);
            }
            // Smoke from neck
            float yawRad = -yBodyRot * Mth.DEG_TO_RAD - Mth.PI;
            float lookX = Mth.sin(yawRad) * 0.4f;
            float lookZ = Mth.cos(yawRad) * 0.4f;
            level().addParticle(ParticleTypes.SMOKE,
                    getX() + lookX, getY() + getEyeHeight() - 1.25f, getZ() + lookZ,
                    0, 0.001, 0);
        }
    }
    
    // ==================== Damage Handling ====================
    
    @Override
    public boolean hurt(DamageSource source, float amount) {
        // When about to die, become headless instead
        if (!level().isClientSide && amount > getHealth() && !isHeadless()) {
            setHeadless(true);
            spawnTimer = 100;
            
            // Explosion from head area
            double xx = Mth.cos(yBodyRot * Mth.DEG_TO_RAD) * 0.75f;
            double zz = Mth.sin(yBodyRot * Mth.DEG_TO_RAD) * 0.75f;
            level().explode(this, getX() + xx, getY() + getEyeHeight(), getZ() + zz, 2.0f, Level.ExplosionInteraction.NONE);
            
            makeHeadless();
            return false; // Don't actually take this damage
        }
        return super.hurt(source, amount);
    }
    
    // ==================== Attack ====================
    
    @Override
    public boolean doHurtTarget(Entity target) {
        if (attackTimer > 0) {
            return false;
        }
        attackTimer = 10;
        level().broadcastEntityEvent(this, (byte) 4);
        
        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.75f;
        boolean hit = target.hurt(damageSources().mobAttack(this), damage);
        
        if (hit) {
            // Knock target upward
            target.setDeltaMovement(target.getDeltaMovement().add(0, 0.2, 0));
            
            // Extra knockback when headless
            if (isHeadless() && target instanceof LivingEntity) {
                float knockX = -Mth.sin(getYRot() * Mth.DEG_TO_RAD) * 1.5f;
                float knockZ = Mth.cos(getYRot() * Mth.DEG_TO_RAD) * 1.5f;
                target.setDeltaMovement(target.getDeltaMovement().add(knockX, 0.1, knockZ));
            }
        }
        
        return hit;
    }
    
    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        if (hasLineOfSight(target) && !chargingBeam && beamCharge > 0) {
            beamCharge -= 15 + random.nextInt(5);
            getLookControl().setLookAt(target, 30.0f, 30.0f);
            
            Vec3 look = getLookAngle();
            EntityGolemOrb orb = new EntityGolemOrb(level(), this, target, false);
            orb.setPos(orb.getX() + look.x, orb.getY(), orb.getZ() + look.z);
            
            double dx = target.getX() + target.getDeltaMovement().x - getX();
            double dy = target.getY() - getY() - target.getBbHeight() / 2.0;
            double dz = target.getZ() + target.getDeltaMovement().z - getZ();
            
            orb.shoot(dx, dy, dz, 0.66f, 5.0f);
            
            playSound(ModSounds.EG_ATTACK.get(), 1.0f, 1.0f + random.nextFloat() * 0.1f);
            level().addFreshEntity(orb);
        }
    }
    
    // ==================== Status Events ====================
    
    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) {
            // Melee attack animation
            attackTimer = 10;
            playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0f, 1.0f);
        } else if (id == 18) {
            // Spawn animation
            spawnTimer = 150;
        } else if (id == 19) {
            // Charging beam visual effect
            // TODO: Lightning arc effect
        } else {
            super.handleEntityEvent(id);
        }
    }
    
    public int getAttackTimer() {
        return attackTimer;
    }
    
    // ==================== Sounds ====================
    
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }
    
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }
    
    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.IRON_GOLEM_STEP, 1.0f, 1.0f);
    }
    
    @Override
    public boolean fireImmune() {
        return true;
    }
}




