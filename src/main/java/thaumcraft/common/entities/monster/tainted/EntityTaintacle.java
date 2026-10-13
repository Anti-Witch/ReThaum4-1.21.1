package thaumcraft.common.entities.monster.tainted;
import net.minecraft.server.level.ServerLevel;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.init.ModEntities;
import thaumcraft.init.ModItems;
import thaumcraft.init.ModSounds;

/**
 * EntityTaintacle - A stationary tainted tentacle that attacks nearby players.
 * Cannot move horizontally, only faces toward targets.
 * Spawns smaller tentacles near players at range.
 * Takes damage when not on tainted ground.
 */
public class EntityTaintacle extends Monster {
    
    public float flailIntensity = 1.0f;
    
    /** Length factor for the tentacle - affects model scaling and animation */
    protected int length = 10;
    
    public EntityTaintacle(EntityType<? extends EntityTaintacle> type, Level level) {
        super(type, level);
        this.xpReward = 8;
    }
    
    public EntityTaintacle(Level level) {
        super(ModEntities.TAINTACLE.get(), level);
        this.xpReward = 8;
    }
    
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 6.0f));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 50.0)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0); // Cannot move
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
    public void move(MoverType type, Vec3 movement) {
        // Only allow downward movement (falling), no horizontal movement
        double clampedX = 0;
        double clampedY = movement.y > 0 ? 0 : movement.y;
        double clampedZ = 0;
        super.move(type, new Vec3(clampedX, clampedY, clampedZ));
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (!level().isClientSide && tickCount % 20 == 0) {
            // TODO: Check if on taint material when implemented
            // For now, tentacles don't take damage from ground
            boolean onTaint = true; // Placeholder
            
            if (!onTaint) {
                hurt(damageSources().starve(), 1.0f);
            }
            
            // Spawn small tentacles near distant targets (only for large taintacles)
            if (!(this instanceof EntityTaintacleSmall) && tickCount % 40 == 0) {
                LivingEntity target = getTarget();
                if (target != null) {
                    double distSq = distanceToSqr(target);
                    if (distSq > 16.0 && distSq < 256.0 && hasLineOfSight(target)) {
                        spawnSmallTentacle(target);
                    }
                }
            }
        }
        
        // Client-side effects
        if (level().isClientSide) {
            // Decay flail intensity
            if (flailIntensity > 1.0f) {
                flailIntensity -= 0.01f;
            }
            
            // Rising particles when spawning
            if (tickCount < getBbHeight() * 10.0f && onGround()) {
                for (int i = 0; i < 3; i++) {
                    level().addParticle(ParticleTypes.LARGE_SMOKE,
                            getX() + (random.nextDouble() - 0.5) * getBbWidth(),
                            getY() + random.nextDouble() * 0.5,
                            getZ() + (random.nextDouble() - 0.5) * getBbWidth(),
                            0, 0.05, 0);
                }
            }
        }
    }
    
    /**
     * Spawns a small tentacle near the target.
     */
    protected void spawnSmallTentacle(Entity target) {
        // TODO: Check for taint biome/material when implemented
        
        EntityTaintacleSmall smallTentacle = new EntityTaintacleSmall(level());
        smallTentacle.moveTo(
                target.getX() + random.nextFloat() - random.nextFloat(),
                target.getY(),
                target.getZ() + random.nextFloat() - random.nextFloat(),
                0.0f, 0.0f);
        level().addFreshEntity(smallTentacle);
        
        playSound(ModSounds.TENTACLE.get(), getSoundVolume(), getVoicePitch());
    }
    
    /**
     * Faces toward the given entity.
     */
    public void faceEntity(Entity target, float maxYawChange) {
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        float targetYaw = (float)(Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0f;
        setYRot(rotlerp(getYRot(), targetYaw, maxYawChange));
    }
    
    private float rotlerp(float current, float target, float maxChange) {
        float diff = Mth.wrapDegrees(target - current);
        if (diff > maxChange) diff = maxChange;
        if (diff < -maxChange) diff = -maxChange;
        return current + diff;
    }
    
    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }
    
    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.TENTACLE.get();
    }
    
    @Override
    public float getVoicePitch() {
        return 1.3f - getBbHeight() / 10.0f;
    }
    
    @Override
    protected float getSoundVolume() {
        return getBbHeight() / 8.0f;
    }
    
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.TENTACLE.get();
    }
    
    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.TENTACLE.get();
    }
    
    /**
     * Gets the length factor of this tentacle.
     * Used by the renderer for scaling.
     */
    public int getLength() {
        return this.length;
    }
    
    @Override
    public void handleEntityEvent(byte id) {
        if (id == 16) {
            flailIntensity = 3.0f;
        } else {
            super.handleEntityEvent(id);
        }
    }
    
    @Override
    public boolean doHurtTarget(Entity target) {
        // Trigger flail animation
        level().broadcastEntityEvent(this, (byte)16);
        playSound(SoundEvents.SLIME_ATTACK, getSoundVolume(), getVoicePitch());
        return super.doHurtTarget(target);
    }
    
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean wasRecentlyHit) {
        super.dropCustomDeathLoot(level, source, wasRecentlyHit);
        
        // TC4: taint creatures drop tainted goo (itemResource 11) or a taint
        // tendril (12), never a "flux crystal" (that item is TC6).
        if (random.nextBoolean()) {
            spawnAtLocation(new net.minecraft.world.item.ItemStack(ModItems.TAINT_SLIME.get()));
        } else {
            spawnAtLocation(new net.minecraft.world.item.ItemStack(ModItems.TAINT_TENDRIL.get()));
        }
    }
}




