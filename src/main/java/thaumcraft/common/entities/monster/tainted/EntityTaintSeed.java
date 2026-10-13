package thaumcraft.common.entities.monster.tainted;
import net.minecraft.server.level.ServerLevel;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.common.blocks.world.taint.TaintHelper;
import thaumcraft.common.world.aura.AuraHandler;
import thaumcraft.init.ModEffects;
import thaumcraft.init.ModEntities;
import thaumcraft.init.ModItems;
import thaumcraft.init.ModSounds;

import java.util.List;

/**
 * EntityTaintSeed - A stationary taint-spreading entity.
 * Spreads taint fibers in the surrounding area and poisons nearby entities.
 * Cannot move but can attack with tentacles.
 */
public class EntityTaintSeed extends Monster implements ITaintedMob {
    
    public int boost = 0;
    private boolean firstRun = false;
    public float attackAnim = 0.0f;
    
    public EntityTaintSeed(EntityType<? extends EntityTaintSeed> type, Level level) {
        super(type, level);
        this.xpReward = 8;
    }
    
    public EntityTaintSeed(Level level) {
        this(ModEntities.TAINT_SEED.get(), level);
    }
    
    protected int getArea() {
        return 1;
    }
    
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, false));
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 75.0)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }
    
    @Override
    public boolean doHurtTarget(Entity target) {
        level().broadcastEntityEvent(this, (byte) 16);
        playSound(ModSounds.TENTACLE.get(), getSoundVolume(), getVoicePitch());
        return super.doHurtTarget(target);
    }
    
    @Override
    public void handleEntityEvent(byte id) {
        if (id == 16) {
            attackAnim = 0.5f;
        } else {
            super.handleEntityEvent(id);
        }
    }
    
    @Override
    public boolean canAttack(LivingEntity target) {
        // Can't attack other tainted mobs
        if (target instanceof ITaintedMob) {
            return false;
        }
        return super.canAttack(target);
    }
    
    @Override
    public boolean isAlliedTo(Entity entity) {
        if (entity instanceof ITaintedMob) {
            return true;
        }
        return super.isAlliedTo(entity);
    }
    
    @Override
    public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, net.minecraft.world.entity.MobSpawnType spawnType) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        
        // Check for other taint seeds nearby
        List<EntityTaintSeed> nearby = level.getEntitiesOfClass(EntityTaintSeed.class,
                new AABB(getX() - 32, getY() - 16, getZ() - 32,
                        getX() + 32, getY() + 16, getZ() + 32));
        
        return nearby.isEmpty() && !level.containsAnyLiquid(getBoundingBox());
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (!level().isClientSide) {
            // Register with taint system
            if (!firstRun || tickCount % 1200 == 0) {
                TaintHelper.removeTaintSeed(level(), blockPosition());
                TaintHelper.addTaintSeed(level(), blockPosition());
                firstRun = true;
            }
            
            if (isAlive()) {
                boolean tickFlag = tickCount % 20 == 0;
                
                if (boost > 0 || tickFlag) {
                    // Spread taint based on flux saturation or boost
                    float mod = (boost > 0) ? 1.0f : getFluxSaturation();
                    
                    if (boost > 0) {
                        --boost;
                    }
                    
                    if (mod <= 0.0f) {
                        // Starve without flux
                        hurt(damageSources().starve(), 0.5f);
                        // Add small amount of flux
                    } else {
                        // Spread taint fibers
                        spreadTaint();
                    }
                }
                
                if (tickFlag) {
                    // Attack target with tentacles
                    if (getTarget() != null && 
                            distanceToSqr(getTarget()) < getArea() * 256 &&
                            hasLineOfSight(getTarget())) {
                        spawnTentacles(getTarget());
                    }
                    
                    // Apply flux taint effect to nearby entities
                    List<LivingEntity> nearby = level().getEntitiesOfClass(LivingEntity.class,
                            getBoundingBox().inflate(getArea() * 4.0),
                            e -> !(e instanceof ITaintedMob));
                    
                    for (LivingEntity entity : nearby) {
                        // Apply flux taint effect
                        entity.addEffect(new MobEffectInstance(ModEffects.FLUX_TAINT, 100, getArea() - 1, false, true));
                    }
                }
            }
        } else {
            // Client-side animation
            if (attackAnim > 0.0f) {
                attackAnim *= 0.75f;
            }
            if (attackAnim < 0.001f) {
                attackAnim = 0.0f;
            }
            
            // TODO: Spawn taint particles
        }
    }
    
    private float getFluxSaturation() {
        return AuraHandler.getFluxSaturation(level(), blockPosition());
    }

    private void spreadTaint() {
        int area = getArea();
        BlockPos spreadPos = blockPosition().offset(
                Mth.randomBetweenInclusive(random, -area * 3, area * 3),
                Mth.randomBetweenInclusive(random, -area, area),
                Mth.randomBetweenInclusive(random, -area * 3, area * 3));

        TaintHelper.spreadFibres(level(), spreadPos);
    }
    
    protected void spawnTentacles(Entity target) {
        // Spawn small tentacles near the target
        EntityTaintacleSmall tentacle = new EntityTaintacleSmall(level());
        tentacle.moveTo(
                target.getX() + random.nextFloat() - random.nextFloat(),
                target.getY(),
                target.getZ() + random.nextFloat() - random.nextFloat(),
                0.0f, 0.0f);
        level().addFreshEntity(tentacle);
        
        playSound(ModSounds.TENTACLE.get(), getSoundVolume(), getVoicePitch());
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
    
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean wasRecentlyHit) {
        // TC4: taint seed drops tainted goo or a taint tendril
        if (random.nextBoolean()) {
            spawnAtLocation(new ItemStack(ModItems.TAINT_SLIME.get()));
        } else {
            spawnAtLocation(new ItemStack(ModItems.TAINT_TENDRIL.get()));
        }
    }
    
    @Override
    public void die(DamageSource source) {
        TaintHelper.removeTaintSeed(level(), blockPosition());
        super.die(source);
    }
    
    // Movement restrictions - taint seeds can't move
    
    @Override
    public boolean isPushable() {
        return false;
    }
    
    @Override
    public void push(double x, double y, double z) {
        // Can't be pushed
    }
    
    @Override
    public void move(MoverType type, Vec3 movement) {
        // Only allow downward movement (falling)
        if (movement.y > 0) {
            movement = new Vec3(0, 0, 0);
        } else {
            movement = new Vec3(0, movement.y, 0);
        }
        super.move(type, movement);
    }
    
    @Override
    protected int decreaseAirSupply(int air) {
        return air; // Can breathe underwater
    }
    
    
    
    @Override
    public boolean removeWhenFarAway(double distance) {
        return false; // Never despawn
    }
    
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("boost", boost);
    }
    
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        boost = tag.getInt("boost");
    }
}




