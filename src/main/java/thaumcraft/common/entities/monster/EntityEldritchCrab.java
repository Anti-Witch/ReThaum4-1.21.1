package thaumcraft.common.entities.monster;
import net.minecraft.server.level.ServerLevel;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.entities.IEldritchMob;
import thaumcraft.init.ModEntities;
import thaumcraft.init.ModSounds;

import javax.annotation.Nullable;

/**
 * EntityEldritchCrab - A spider-like eldritch creature.
 * Can ride on the heads of other entities and attack them.
 * Sometimes wears a helm for extra armor.
 */
public class EntityEldritchCrab extends Monster implements IEldritchMob {
    
    private static final EntityDataAccessor<Boolean> DATA_HELM = 
            SynchedEntityData.defineId(EntityEldritchCrab.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_RIDING = 
            SynchedEntityData.defineId(EntityEldritchCrab.class, EntityDataSerializers.INT);
    
    private int attackTime = 0;
    
    public EntityEldritchCrab(EntityType<? extends EntityEldritchCrab> type, Level level) {
        super(type, level);
        this.xpReward = 6;
        ((GroundPathNavigation) getNavigation()).setCanOpenDoors(true);
    }
    
    public EntityEldritchCrab(Level level) {
        this(ModEntities.ELDRITCH_CRAB.get(), level);
    }
    
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.63f));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        // TODO: Target EntityCultist when implemented
    }
    
    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HELM, false);
        builder.define(DATA_RIDING, -1);
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3);
    }
    
    public boolean hasHelm() {
        return this.entityData.get(DATA_HELM);
    }
    
    public void setHelm(boolean helm) {
        this.entityData.set(DATA_HELM, helm);
        // Update movement speed based on helm
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(helm ? 0.275 : 0.3);
    }
    
    public int getRidingTarget() {
        return this.entityData.get(DATA_RIDING);
    }
    
    public void setRidingTarget(int entityId) {
        this.entityData.set(DATA_RIDING, entityId);
    }
    
    @Override
    public int getArmorValue() {
        return hasHelm() ? 5 : 0;
    }
    
    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            MobSpawnType spawnType, @Nullable SpawnGroupData spawnData) {
        
        spawnData = super.finalizeSpawn(level, difficulty, spawnType, spawnData);
        
        // Chance to have helm
        if (level.getDifficulty() == Difficulty.HARD) {
            setHelm(true);
        } else {
            setHelm(random.nextFloat() < 0.33f);
        }
        
        // Chance to have a random potion effect (like spiders)
        if (level.getDifficulty() == Difficulty.HARD && random.nextFloat() < 0.1f * difficulty.getSpecialMultiplier()) {
            int effectType = random.nextInt(3);
            MobEffectInstance effect = switch (effectType) {
                case 0 -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Integer.MAX_VALUE, 1);
                case 1 -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, 0);
                default -> new MobEffectInstance(MobEffects.REGENERATION, Integer.MAX_VALUE, 0);
            };
            addEffect(effect);
        }
        
        return spawnData;
    }
    
    @Override
    public void tick() {
        super.tick();
        --attackTime;
        
        // Prevent fall damage shortly after spawning
        if (tickCount < 20) {
            fallDistance = 0.0f;
        }
        
        if (!level().isClientSide) {
            // Try to ride on target's head
            if (getVehicle() == null && getTarget() != null && !getTarget().isVehicle() && 
                    !onGround() && !hasHelm() && getTarget().isAlive() &&
                    getY() - getTarget().getY() >= getTarget().getBbHeight() / 2.0f &&
                    distanceToSqr(getTarget()) < 4.0) {
                startRiding(getTarget());
                setRidingTarget(getTarget().getId());
            }
            
            // Attack while riding
            if (getVehicle() != null && isAlive() && attackTime <= 0) {
                attackTime = 10 + random.nextInt(10);
                doHurtTarget(getVehicle());
                
                // Chance to dismount
                if (random.nextFloat() < 0.2) {
                    stopRiding();
                    setRidingTarget(-1);
                }
            }
            
            // Sync riding state
            if (getVehicle() == null && getRidingTarget() != -1) {
                setRidingTarget(-1);
            }
        } else {
            // Client-side riding sync
            if (getVehicle() == null && getRidingTarget() != -1) {
                Entity e = level().getEntity(getRidingTarget());
                if (e != null) {
                    startRiding(e);
                }
            } else if (getVehicle() != null && getRidingTarget() == -1) {
                stopRiding();
            }
        }
    }
    
    @Override
    public boolean doHurtTarget(Entity target) {
        if (super.doHurtTarget(target)) {
            playSound(ModSounds.CRAB_CLAW.get(), 1.0f, 0.9f + random.nextFloat() * 0.2f);
            return true;
        }
        return false;
    }
    
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        
        // Helm breaks at half health
        if (hasHelm() && getHealth() / getMaxHealth() <= 0.5f) {
            setHelm(false);
            // TODO: renderBrokenItemStack for crimson plate
        }
        
        return hurt;
    }
    
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean wasRecentlyHit) {
        super.dropCustomDeathLoot(level, source, wasRecentlyHit);
        
        // Chance to drop ender pearl
        if (wasRecentlyHit && (random.nextInt(3) == 0 || random.nextInt(1 + 0) > 0)) {
            spawnAtLocation(new ItemStack(Items.ENDER_PEARL));
        }
    }
    
    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }
    
    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.CRAB_TALK.get();
    }
    
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.CRAB_CLAW.get();
    }
    
    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.CRAB_DEATH.get();
    }
    
    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.SPIDER_STEP, 0.15f, 1.0f);
    }
    
    
    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        // Immune to poison
        if (effect.getEffect() == MobEffects.POISON) {
            return false;
        }
        return super.canBeAffected(effect);
    }
    
    @Override
    public boolean isAlliedTo(Entity entity) {
        // Allied with other crabs
        if (entity instanceof EntityEldritchCrab) {
            return true;
        }
        return super.isAlliedTo(entity);
    }
    
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("helm", hasHelm());
    }
    
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setHelm(tag.getBoolean("helm"));
    }
}




