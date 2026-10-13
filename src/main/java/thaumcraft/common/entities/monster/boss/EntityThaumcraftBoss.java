package thaumcraft.common.entities.monster.boss;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import thaumcraft.api.entities.IEldritchMob;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * EntityThaumcraftBoss - Base class for Thaumcraft boss entities.
 * Provides boss bar, damage cap, enrage mechanic, and aggro tracking.
 */
public abstract class EntityThaumcraftBoss extends Monster {
    
    private static final EntityDataAccessor<Integer> DATA_ANGER = 
            SynchedEntityData.defineId(EntityThaumcraftBoss.class, EntityDataSerializers.INT);
    
    protected final ServerBossEvent bossEvent;
    private final Map<Integer, Integer> aggro = new HashMap<>();
    protected int spawnTimer = 0;
    
    // Home position
    private BlockPos homePos;
    private int homeDistance;
    
    public EntityThaumcraftBoss(EntityType<? extends EntityThaumcraftBoss> type, Level level) {
        super(type, level);
        this.xpReward = 50;
        this.bossEvent = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
        this.bossEvent.setDarkenScreen(true);
    }
    
    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ANGER, 0);
    }
    
    public static AttributeSupplier.Builder createBossAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.95)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }
    
    // ==================== Anger/Enrage System ====================
    
    public int getAnger() {
        return this.entityData.get(DATA_ANGER);
    }
    
    public void setAnger(int anger) {
        this.entityData.set(DATA_ANGER, anger);
    }
    
    public int getSpawnTimer() {
        return spawnTimer;
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
    
    // ==================== Boss Bar ====================
    
    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }
    
    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }
    
    // ==================== Update Logic ====================
    
    @Override
    protected void customServerAiStep() {
        if (spawnTimer == 0) {
            super.customServerAiStep();
        }
        
        if (getTarget() != null && !getTarget().isAlive()) {
            setTarget(null);
        }
        
        this.bossEvent.setProgress(getHealth() / getMaxHealth());
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (spawnTimer > 0) {
            --spawnTimer;
        }
        
        if (getAnger() > 0) {
            setAnger(getAnger() - 1);
        }
        
        // Angry particles on client
        if (level().isClientSide && random.nextInt(15) == 0 && getAnger() > 0) {
            double dx = random.nextGaussian() * 0.02;
            double dy = random.nextGaussian() * 0.02;
            double dz = random.nextGaussian() * 0.02;
            level().addParticle(ParticleTypes.ANGRY_VILLAGER,
                    getX() + random.nextFloat() * getBbWidth() - getBbWidth() / 2.0,
                    getBoundingBox().minY + getBbHeight() + random.nextFloat() * 0.5,
                    getZ() + random.nextFloat() * getBbWidth() - getBbWidth() / 2.0,
                    dx, dy, dz);
        }
        
        // Slow regeneration
        if (!level().isClientSide && tickCount % 30 == 0) {
            heal(1.0f);
        }
    }
    
    // ==================== Damage Handling ====================
    
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return super.isInvulnerableTo(source) || spawnTimer > 0;
    }
    
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide) {
            // Track aggro
            if (source.getEntity() instanceof LivingEntity living) {
                int targetId = living.getId();
                int currentAggro = aggro.getOrDefault(targetId, 0);
                aggro.put(targetId, currentAggro + (int) amount);
            }
            
            // Damage cap with enrage
            if (amount > 35.0f) {
                if (getAnger() == 0) {
                    // Enrage!
                    try {
                        addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, (int)(amount / 15.0f)));
                        addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, (int)(amount / 10.0f)));
                        addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 200, (int)(amount / 40.0f)));
                    } catch (Exception ignored) {}
                    setAnger(200);
                    
                    // Notify attacker
                    if (source.getEntity() instanceof Player player) {
                        player.displayClientMessage(
                                Component.translatable("tc.boss.enrage", getDisplayName()), true);
                    }
                }
                amount = 35.0f;
            }
        }
        return super.hurt(source, amount);
    }
    
    // ==================== Spawn ====================
    
    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            MobSpawnType spawnType, @Nullable SpawnGroupData spawnData) {
        
        setHomePos(blockPosition(), 24);
        generateName();
        this.bossEvent.setName(getDisplayName());
        return spawnData;
    }
    
    /**
     * Override to generate a custom boss name.
     */
    public void generateName() {
        // Subclasses can override
    }
    
    // ==================== Immunities ====================
    
    
    
    @Override
    protected int decreaseAirSupply(int air) {
        return air;
    }
    
    @Override
    public void makeStuckInBlock(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.phys.Vec3 motion) {
        // Immune to webs
    }
    
    @Override
    public boolean canPickUpLoot() {
        return false;
    }
    
    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
    
    @Override
    public boolean isPushable() {
        return !isInvulnerableTo(damageSources().starve());
    }
    
    // ==================== Team Logic ====================
    
    @Override
    public boolean isAlliedTo(Entity entity) {
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
        
        if (hasCustomName()) {
            this.bossEvent.setName(getDisplayName());
        }
    }
    
    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(getDisplayName());
    }
}




