package thaumcraft.common.entities.monster;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.entities.IEldritchMob;
import thaumcraft.common.entities.monster.cult.EntityCultist;
import thaumcraft.common.entities.projectile.EntityEldritchOrb;
import thaumcraft.init.ModEntities;
import thaumcraft.init.ModSounds;

import javax.annotation.Nullable;
import java.util.List;

/**
 * EntityEldritchGuardian - A powerful eldritch entity that guards the outer lands.
 * Shoots eldritch orbs at range and attacks with melee up close.
 * Resistant to magic damage.
 */
public class EntityEldritchGuardian extends Monster implements RangedAttackMob, IEldritchMob {
    
    // Animation state for arms
    public float armLiftL = 0.0f;
    public float armLiftR = 0.0f;
    private boolean lastBlast = false;
    
    // Home position
    private BlockPos homePos;
    private int homeDistance;
    
    public EntityEldritchGuardian(EntityType<? extends EntityEldritchGuardian> type, Level level) {
        super(type, level);
        this.xpReward = 20;
        ((GroundPathNavigation) getNavigation()).setCanOpenDoors(true);
    }
    
    public EntityEldritchGuardian(Level level) {
        this(ModEntities.ELDRITCH_GUARDIAN.get(), level);
    }
    
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 20, 40, 24.0f));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.8));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, EntityCultist.class, true));
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 50.0)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }
    
    @Override
    public int getArmorValue() {
        return 4;
    }
    
    @Override
    public boolean canPickUpLoot() {
        return false;
    }
    
    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Resistant to magic damage
        if (source.is(net.minecraft.tags.DamageTypeTags.WITCH_RESISTANT_TO)) {
            amount /= 2.0f;
        }
        return super.hurt(source, amount);
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (level().isClientSide) {
            // Decay arm lift animation
            if (armLiftL > 0.0f) {
                armLiftL -= 0.05f;
            }
            if (armLiftR > 0.0f) {
                armLiftR -= 0.05f;
            }
            
            // TODO: FXDispatcher.INSTANCE.wispFXEG particles
        }
    }
    
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            // Chance to set target on fire
            int difficulty = level().getDifficulty().getId();
            if (getMainHandItem().isEmpty() && isOnFire() && random.nextFloat() < difficulty * 0.3f) {
                target.setRemainingFireTicks(20 * (2 * difficulty));
            }
        }
        return hit;
    }
    
    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        if (random.nextFloat() > 0.15f) {
            // Fire eldritch orb
            EntityEldritchOrb orb = new EntityEldritchOrb(level(), this);
            lastBlast = !lastBlast;
            level().broadcastEntityEvent(this, (byte)(lastBlast ? 16 : 15));
            
            // Calculate offset for which arm fires
            int rotation = lastBlast ? 90 : 180;
            double offsetX = Math.cos(Math.toRadians((getYRot() + rotation) % 360.0f)) * 0.5f;
            double offsetZ = Math.sin(Math.toRadians((getYRot() + rotation) % 360.0f)) * 0.5f;
            
            orb.setPos(orb.getX() - offsetX, orb.getY(), orb.getZ() - offsetZ);
            
            // Aim at target with prediction
            Vec3 targetVel = target.getDeltaMovement().scale(10.0);
            Vec3 direction = target.position().add(targetVel).subtract(position()).normalize();
            
            orb.shoot(direction.x, direction.y, direction.z, 1.1f, 2.0f);
            playSound(ModSounds.EG_ATTACK.get(), 2.0f, 1.0f + random.nextFloat() * 0.1f);
            level().addFreshEntity(orb);
        } else if (hasLineOfSight(target)) {
            // Sonic scream attack - applies wither effect
            // TODO: Send PacketFXSonic for visual effect
            try {
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 400, 0));
            } catch (Exception ignored) {}
            
            // Add warp to players
            if (target instanceof Player player) {
                ThaumcraftApi.internalMethods.addWarpToPlayer(player, 1 + random.nextInt(3), IPlayerWarp.EnumWarpType.TEMPORARY);
            }
            
            playSound(ModSounds.EG_SCREECH.get(), 3.0f, 1.0f + random.nextFloat() * 0.1f);
        }
    }
    
    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case 15 -> armLiftL = 0.5f;
            case 16 -> armLiftR = 0.5f;
            case 17 -> {
                armLiftL = 0.9f;
                armLiftR = 0.9f;
            }
            default -> super.handleEntityEvent(id);
        }
    }
    
    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.EG_IDLE.get();
    }
    
    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.EG_DEATH.get();
    }
    
    @Override
    public int getAmbientSoundInterval() {
        return 500;
    }
    
    @Override
    protected float getSoundVolume() {
        return 1.5f;
    }
    
        
    // ==================== Home Position ====================
    
    public void setHomePos(BlockPos pos, int distance) {
        this.homePos = pos;
        this.homeDistance = distance;
        restrictTo(pos, distance);
    }
    
    public boolean hasHome() {
        return homePos != null && homeDistance > 0;
    }
    
    @Override
    public boolean removeWhenFarAway(double distance) {
        return !hasHome();
    }
    
    // ==================== Spawn Rules ====================
    
    @Override
    public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, MobSpawnType spawnType) {
        // Limit spawn density
        List<EntityEldritchGuardian> nearby = level.getEntitiesOfClass(EntityEldritchGuardian.class,
                new AABB(getX() - 32, getY() - 16, getZ() - 32,
                        getX() + 32, getY() + 16, getZ() + 32));
        return nearby.isEmpty() && super.checkSpawnRules(level, spawnType);
    }
    
    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            MobSpawnType spawnType, @Nullable SpawnGroupData spawnData) {
        
        spawnData = super.finalizeSpawn(level, difficulty, spawnType, spawnData);
        
        // TODO: In eldritch dimension, add absorption hearts
        // if (level.dimensionType() == ModDimensions.ELDRITCH) {
        //     int bonus = (int) getMaxHealth() / 2;
        //     setAbsorptionAmount(getAbsorptionAmount() + bonus);
        // }
        
        return spawnData;
    }
    
    // ==================== Team Logic ====================
    
    @Override
    public boolean isAlliedTo(Entity entity) {
        // Allied with other eldritch mobs
        if (entity instanceof IEldritchMob) {
            return true;
        }
        return super.isAlliedTo(entity);
    }
    
    // ==================== NBT ====================
    
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (homePos != null && homeDistance > 0) {
            tag.putInt("HomeD", homeDistance);
            tag.putInt("HomeX", homePos.getX());
            tag.putInt("HomeY", homePos.getY());
            tag.putInt("HomeZ", homePos.getZ());
        }
    }
    
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("HomeD")) {
            setHomePos(new BlockPos(
                    tag.getInt("HomeX"),
                    tag.getInt("HomeY"),
                    tag.getInt("HomeZ")),
                    tag.getInt("HomeD"));
        }
    }
}




