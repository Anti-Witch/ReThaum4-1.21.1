package thaumcraft.common.entities.monster;
import net.minecraft.server.level.ServerLevel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.init.ModEntities;
import thaumcraft.init.ModSounds;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;

import java.util.ArrayList;
import java.util.List;

/**
 * EntityWisp - A flying magical creature associated with aspects.
 * Spawns in dark areas and attacks players with magical zaps.
 * Drops crystals of its aspect type on death.
 */
public class EntityWisp extends FlyingMob implements Enemy {
    
    private static final EntityDataAccessor<String> DATA_TYPE = 
            SynchedEntityData.defineId(EntityWisp.class, EntityDataSerializers.STRING);
    
    private BlockPos currentFlightTarget;
    private int aggroCooldown = 0;
    public int prevAttackCounter = 0;
    public int attackCounter = 0;
    
    public EntityWisp(EntityType<? extends EntityWisp> type, Level level) {
        super(type, level);
        this.xpReward = 5;
    }
    
    public EntityWisp(Level level) {
        super(ModEntities.WISP.get(), level);
        this.xpReward = 5;
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 22.0)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FLYING_SPEED, 0.5)
                .add(Attributes.MOVEMENT_SPEED, 0.25);
    }
    
    /**
     * Static spawn rule check for use with SpawnPlacementRegisterEvent.
     * Wisps spawn in dark areas and not in peaceful mode.
     */
    public static boolean checkWispSpawnRules(EntityType<? extends EntityWisp> type, ServerLevelAccessor level, 
            MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        // Check difficulty
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        
        // Check spawn density - max 8 wisps in area
        int count = level.getEntitiesOfClass(EntityWisp.class, 
                new AABB(pos).inflate(16.0)).size();
        if (count >= 8) {
            return false;
        }
        
        // Check light level - similar to monsters
        int skyLight = level.getBrightness(LightLayer.SKY, pos);
        if (skyLight > random.nextInt(32)) {
            return false;
        }
        
        int blockLight = level.getMaxLocalRawBrightness(pos);
        return blockLight <= random.nextInt(8);
    }
    
    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TYPE, "");
    }
    
    public String getWispType() {
        return this.entityData.get(DATA_TYPE);
    }
    
    public void setWispType(String type) {
        this.entityData.set(DATA_TYPE, type != null ? type : "");
    }
    
    /**
     * Gets the aspect associated with this wisp, or null if not set.
     */
    public Aspect getAspect() {
        String type = getWispType();
        if (type == null || type.isEmpty()) return null;
        return Aspect.getAspect(type);
    }
    
    /**
     * Gets the color for rendering based on aspect type.
     */
    public int getColor() {
        Aspect aspect = getAspect();
        return aspect != null ? aspect.getColor() : 0xFFFFFF;
    }
    
    @Override
    protected boolean shouldDespawnInPeaceful() {
        return true;
    }
    
    @Override
    public int getMaxAirSupply() {
        return Integer.MAX_VALUE; // Can't drown
    }
    
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof LivingEntity living) {
            setTarget(living);
            aggroCooldown = 200;
        }
        return super.hurt(source, amount);
    }
    
    @Override
    public void die(DamageSource source) {
        super.die(source);
        
        // Particle burst on death (client-side)
        if (level().isClientSide) {
            for (int i = 0; i < 20; i++) {
                level().addParticle(ParticleTypes.WITCH,
                        getX() + (random.nextDouble() - 0.5) * 0.9,
                        getY() + 0.45 + (random.nextDouble() - 0.5) * 0.9,
                        getZ() + (random.nextDouble() - 0.5) * 0.9,
                        0, 0, 0);
            }
        }
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // Initial spawn burst
        if (level().isClientSide && tickCount <= 1) {
            for (int i = 0; i < 30; i++) {
                level().addParticle(ParticleTypes.WITCH,
                        getX() + (random.nextDouble() - 0.5),
                        getY() + (random.nextDouble() - 0.5),
                        getZ() + (random.nextDouble() - 0.5),
                        0, 0, 0);
            }
        }
        
        // Ambient particles
        if (level().isClientSide && random.nextBoolean()) {
            Aspect aspect = getAspect();
            if (aspect != null) {
                // Spawn colored particles based on aspect
                level().addParticle(ParticleTypes.WITCH,
                        getX() + (random.nextFloat() - random.nextFloat()) * 0.7,
                        getY() + (random.nextFloat() - random.nextFloat()) * 0.7,
                        getZ() + (random.nextFloat() - random.nextFloat()) * 0.7,
                        0, 0, 0);
            }
        }
        
        // Reduce vertical movement
        Vec3 motion = getDeltaMovement();
        setDeltaMovement(motion.x, motion.y * 0.6, motion.z);
    }
    
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        
        // Initialize aspect type if not set
        if (getWispType().isEmpty()) {
            ArrayList<Aspect> aspects;
            if (random.nextInt(10) != 0) {
                aspects = Aspect.getPrimalAspects();
            } else {
                aspects = Aspect.getCompoundAspects();
            }
            if (aspects != null && !aspects.isEmpty()) {
                setWispType(aspects.get(random.nextInt(aspects.size())).getTag());
            }
        }
        
        // Despawn in peaceful
        if (level().getDifficulty() == Difficulty.PEACEFUL) {
            discard();
            return;
        }
        
        prevAttackCounter = attackCounter;
        double attackRange = 16.0;
        
        LivingEntity target = getTarget();
        
        if (target == null || !hasLineOfSight(target)) {
            // Wander behavior
            if (currentFlightTarget != null && 
                    (!level().isEmptyBlock(currentFlightTarget) || 
                     currentFlightTarget.getY() < 1 || 
                     currentFlightTarget.getY() > level().getHeightmapPos(
                             net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, 
                             currentFlightTarget).above(8).getY())) {
                currentFlightTarget = null;
            }
            
            if (currentFlightTarget == null || random.nextInt(30) == 0 || 
                    currentFlightTarget.distSqr(blockPosition()) < 4.0) {
                currentFlightTarget = new BlockPos(
                        (int)getX() + random.nextInt(7) - random.nextInt(7),
                        (int)getY() + random.nextInt(6) - 2,
                        (int)getZ() + random.nextInt(7) - random.nextInt(7));
            }
            
            flyToward(currentFlightTarget.getX() + 0.5, currentFlightTarget.getY() + 0.1, currentFlightTarget.getZ() + 0.5, 0.15f);
        } else if (distanceToSqr(target) > attackRange * attackRange / 2.0 && hasLineOfSight(target)) {
            // Chase target
            flyToward(target.getX(), target.getEyeY() * 0.66 + target.getY() * 0.34, target.getZ(), 0.5f);
        }
        
        // Clear target if in creative
        if (target instanceof Player player && player.getAbilities().invulnerable) {
            setTarget(null);
        }
        
        // Clear dead targets
        if (target != null && !target.isAlive()) {
            setTarget(null);
        }
        
        // Aggro cooldown
        aggroCooldown--;
        
        // Randomly acquire targets
        if (random.nextInt(1000) == 0 && (target == null || aggroCooldown <= 0)) {
            Player nearestPlayer = level().getNearestPlayer(this, 16.0);
            if (nearestPlayer != null) {
                setTarget(nearestPlayer);
                aggroCooldown = 50;
            }
        }
        
        // Attack logic
        target = getTarget();
        if (isAlive() && target != null && distanceToSqr(target) < attackRange * attackRange) {
            // Face target
            double dx = target.getX() - getX();
            double dz = target.getZ() - getZ();
            float targetYaw = -(float)Math.atan2(dx, dz) * Mth.RAD_TO_DEG;
            setYRot(targetYaw);
            yBodyRot = targetYaw;
            
            if (hasLineOfSight(target)) {
                attackCounter++;
                
                if (attackCounter == 20) {
                    // Zap attack!
                    playSound(ModSounds.ZAP.get(), 1.0f, 1.1f);
                    
                    // TODO: Send zap visual packet when networking is implemented
                    
                    // Damage calculation - moving targets are harder to hit
                    float damage = (float)getAttributeValue(Attributes.ATTACK_DAMAGE);
                    Vec3 targetMotion = target.getDeltaMovement();
                    
                    if (Math.abs(targetMotion.x) > 0.1 || Math.abs(targetMotion.y) > 0.1 || Math.abs(targetMotion.z) > 0.1) {
                        // Moving target - 40% hit chance
                        if (random.nextFloat() < 0.4f) {
                            target.hurt(damageSources().mobAttack(this), damage);
                        }
                    } else {
                        // Stationary target - 66% hit chance, bonus damage
                        if (random.nextFloat() < 0.66f) {
                            target.hurt(damageSources().mobAttack(this), damage + 1.0f);
                        }
                    }
                    
                    // Reset attack counter with some randomness
                    attackCounter = -20 + random.nextInt(20);
                }
            } else if (attackCounter > 0) {
                attackCounter--;
            }
        }
    }
    
    private void flyToward(double x, double y, double z, float speed) {
        double dx = x - getX();
        double dy = y - getY();
        double dz = z - getZ();
        
        Vec3 motion = getDeltaMovement();
        setDeltaMovement(
                motion.x + (Math.signum(dx) * 0.5 - motion.x) * 0.1,
                motion.y + (Math.signum(dy) * 0.7 - motion.y) * 0.1,
                motion.z + (Math.signum(dz) * 0.5 - motion.z) * 0.1);
        
        float yaw = (float)(Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0f;
        float yawDiff = Mth.wrapDegrees(yaw - getYRot());
        zza = speed;
        setYRot(getYRot() + yawDiff);
    }
    
    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.WISP_LIVE.get();
    }
    
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ZAP.get();
    }
    
    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.WISP_DEAD.get();
    }
    
    @Override
    protected float getSoundVolume() {
        return 0.25f;
    }
    
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean wasRecentlyHit) {
        super.dropCustomDeathLoot(level, source, wasRecentlyHit);
        
        // Drop crystal of wisp's aspect type
        Aspect aspect = getAspect();
        if (aspect != null) {
            this.spawnAtLocation(ThaumcraftApiHelper.makeCrystal(aspect));
        }
    }
    
    @Override
    public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, MobSpawnType spawnType) {
        // Check spawn density
        int count = 0;
        try {
            List<EntityWisp> nearby = level.getEntitiesOfClass(EntityWisp.class, 
                    getBoundingBox().inflate(16.0));
            count = nearby.size();
        } catch (Exception ignored) {}
        
        if (count >= 8) return false;
        if (level.getDifficulty() == Difficulty.PEACEFUL) return false;
        
        return isValidLightLevel(level) && super.checkSpawnRules(level, spawnType);
    }
    
    private boolean isValidLightLevel(net.minecraft.world.level.LevelAccessor level) {
        BlockPos pos = new BlockPos((int)getX(), (int)getBoundingBox().minY, (int)getZ());
        
        // Check sky light
        if (level.getBrightness(LightLayer.SKY, pos) > random.nextInt(32)) {
            return false;
        }
        
        // Check overall light level
        int lightLevel = level.getMaxLocalRawBrightness(pos);
        return lightLevel <= random.nextInt(8);
    }
    
    @Override
    public int getMaxSpawnClusterSize() {
        return 2;
    }
    
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("WispType", getWispType());
    }
    
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setWispType(tag.getString("WispType"));
    }
}




