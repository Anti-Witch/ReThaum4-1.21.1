package thaumcraft.common.entities.monster.boss;
import net.minecraft.server.level.ServerLevel;

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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.entities.IEldritchMob;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.common.entities.monster.tainted.EntityTaintacle;
import thaumcraft.init.ModEntities;
import thaumcraft.init.ModItems;

import javax.annotation.Nullable;
import java.util.List;

/**
 * EntityTaintacleGiant - A giant boss taintacle.
 * Much larger and stronger than regular taintacles.
 * Has boss bar, damage cap with enrage, and drops primordial pearl.
 * Implements both ITaintedMob and IEldritchMob interfaces.
 */
public class EntityTaintacleGiant extends EntityTaintacle implements ITaintedMob, IEldritchMob {
    
    private static final EntityDataAccessor<Integer> DATA_ANGER = 
            SynchedEntityData.defineId(EntityTaintacleGiant.class, EntityDataSerializers.INT);
    
    protected final ServerBossEvent bossEvent;
    
    public EntityTaintacleGiant(EntityType<? extends EntityTaintacleGiant> type, Level level) {
        super(type, level);
        this.xpReward = 20;
        this.bossEvent = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
        this.bossEvent.setDarkenScreen(true);
    }
    
    public EntityTaintacleGiant(Level level) {
        this(ModEntities.TAINTACLE_GIANT.get(), level);
    }
    
    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ANGER, 0);
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return EntityTaintacle.createAttributes()
                .add(Attributes.MAX_HEALTH, 175.0)
                .add(Attributes.ATTACK_DAMAGE, 9.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9);
    }
    
    // ==================== Anger/Enrage System ====================
    
    public int getAnger() {
        return this.entityData.get(DATA_ANGER);
    }
    
    public void setAnger(int anger) {
        this.entityData.set(DATA_ANGER, anger);
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
    public void tick() {
        super.tick();
        
        // Decay anger
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
    
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossEvent.setProgress(getHealth() / getMaxHealth());
    }
    
    // ==================== Damage Handling ====================
    
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide) {
            // Damage cap with enrage mechanic
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
    
    @Override
    public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, MobSpawnType spawnType) {
        return false; // Cannot spawn naturally
    }
    
    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            MobSpawnType spawnType, @Nullable SpawnGroupData spawnData) {
        
        // TODO: EntityUtils.makeChampion when implemented
        this.bossEvent.setName(getDisplayName());
        return spawnData;
    }
    
    // ==================== Immunities ====================
    
    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
    
    
    
    @Override
    protected int decreaseAirSupply(int air) {
        return air; // Doesn't drown
    }
    
    @Override
    public boolean isNoAi() {
        return false;
    }
    
    // ==================== Loot ====================
    
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean wasRecentlyHit) {
        // Only drop primordial pearl if no other giant taintacles nearby
        AABB searchBox = getBoundingBox().inflate(48.0);
        List<EntityTaintacleGiant> others = level().getEntitiesOfClass(EntityTaintacleGiant.class, searchBox,
                e -> e != this && e.isAlive());
        
        if (others.isEmpty()) {
            spawnAtLocation(new ItemStack(ModItems.PRIMORDIAL_PEARL.get()));
        }
    }
    
    // ==================== NBT ====================
    
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Anger", getAnger());
    }
    
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Anger")) {
            setAnger(tag.getInt("Anger"));
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




