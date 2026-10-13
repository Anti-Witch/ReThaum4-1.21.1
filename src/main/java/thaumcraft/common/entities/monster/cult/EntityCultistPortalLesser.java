package thaumcraft.common.entities.monster.cult;
import net.minecraft.server.level.ServerLevel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.init.ModEntities;
import thaumcraft.init.ModSounds;

import java.util.List;

/**
 * EntityCultistPortalLesser - A stationary portal that spawns cultists when players are nearby.
 * The portal activates when a player comes within 32 blocks and periodically spawns
 * cultist knights or clerics. The portal takes damage each time it spawns a cultist.
 * It damages players that get too close and creates an explosion when destroyed.
 */
public class EntityCultistPortalLesser extends Monster {
    
    private static final EntityDataAccessor<Boolean> DATA_ACTIVE = 
            SynchedEntityData.defineId(EntityCultistPortalLesser.class, EntityDataSerializers.BOOLEAN);
    
    private int stageCounter = 100;
    public int activeCounter = 0;
    public int pulse = 0;
    
    public EntityCultistPortalLesser(EntityType<? extends EntityCultistPortalLesser> type, Level level) {
        super(type, level);
        this.xpReward = 10;
        this.setNoGravity(true);
    }
    
    public EntityCultistPortalLesser(Level level) {
        this(ModEntities.CULTIST_PORTAL_LESSER.get(), level);
    }
    
    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ACTIVE, false);
    }
    
    public boolean isActive() {
        return this.entityData.get(DATA_ACTIVE);
    }
    
    public void setActive(boolean active) {
        this.entityData.set(DATA_ACTIVE, active);
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ARMOR, 4.0);
    }
    
    @Override
    protected void registerGoals() {
        // No AI goals - this is a stationary entity
    }
    
    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
    
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("active", isActive());
    }
    
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setActive(tag.getBoolean("active"));
    }
    
    @Override
    public boolean isPickable() {
        return true;
    }
    
    @Override
    public boolean isPushable() {
        return false;
    }
    
    @Override
    public void move(MoverType type, Vec3 movement) {
        // Stationary entity - don't move
    }
    
    @Override
    protected void customServerAiStep() {
        // Override to prevent default AI behavior
    }
    
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096.0;
    }
    
    @Override
    public float getLightLevelDependentMagicValue() {
        return 1.0f; // Always fully bright
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (isActive()) {
            ++activeCounter;
        }
        
        if (!level().isClientSide) {
            if (!isActive()) {
                // Check for nearby player every 10 ticks
                if (tickCount % 10 == 0) {
                    Player player = level().getNearestPlayer(this, 32.0);
                    if (player != null) {
                        setActive(true);
                        playSound(ModSounds.CRAFT_START.get(), 1.0f, 1.0f);
                    }
                }
            } else if (stageCounter-- <= 0) {
                // Try to spawn cultists if player is in range and visible
                Player player = level().getNearestPlayer(this, 32.0);
                if (player != null && hasLineOfSight(player)) {
                    // Determine max cultists based on difficulty
                    int maxCount;
                    if (level().getDifficulty() == Difficulty.HARD) {
                        maxCount = 6;
                    } else if (level().getDifficulty() == Difficulty.NORMAL) {
                        maxCount = 4;
                    } else {
                        maxCount = 2;
                    }
                    
                    // Count existing cultists nearby
                    try {
                        AABB searchBox = getBoundingBox().inflate(32.0, 32.0, 32.0);
                        List<EntityCultist> existingCultists = level().getEntitiesOfClass(EntityCultist.class, searchBox);
                        if (existingCultists != null) {
                            maxCount -= existingCultists.size();
                        }
                    } catch (Exception ignored) {}
                    
                    if (maxCount > 0) {
                        // Trigger pulse effect on client
                        level().broadcastEntityEvent(this, (byte) 16);
                        spawnMinion();
                    }
                }
                stageCounter = 50 + random.nextInt(50);
            }
        }
        
        // Decay pulse effect
        if (pulse > 0) {
            --pulse;
        }
        
        // Client-side visual effects
        if (level().isClientSide && isActive()) {
            // Portal particles
            if (random.nextInt(3) == 0) {
                double dx = (random.nextDouble() - 0.5) * getBbWidth();
                double dz = (random.nextDouble() - 0.5) * getBbWidth();
                level().addParticle(ParticleTypes.PORTAL,
                        getX() + dx, getY() + random.nextDouble() * getBbHeight(), getZ() + dz,
                        dx * 0.5, random.nextDouble() * 0.5, dz * 0.5);
            }
            // Enchant particles
            if (random.nextInt(5) == 0) {
                level().addParticle(ParticleTypes.ENCHANT,
                        getX() + (random.nextDouble() - 0.5) * 2.0,
                        getY() + random.nextDouble() * getBbHeight(),
                        getZ() + (random.nextDouble() - 0.5) * 2.0,
                        0, 0.5, 0);
            }
        }
    }
    
    /**
     * Spawns a cultist minion at the portal's location.
     */
    private void spawnMinion() {
        EntityCultist cultist;
        if (random.nextFloat() > 0.33f) {
            cultist = new EntityCultistKnight(level());
        } else {
            cultist = new EntityCultistCleric(level());
        }
        
        // Position near the portal
        cultist.moveTo(
                getX() + random.nextFloat() - random.nextFloat(),
                getY() + 0.25,
                getZ() + random.nextFloat() - random.nextFloat(),
                random.nextFloat() * 360.0f, 0.0f);
        
        // Initialize the cultist
        cultist.finalizeSpawn(
                (net.minecraft.world.level.ServerLevelAccessor) level(),
                level().getCurrentDifficultyAt(new BlockPos((int) cultist.getX(), (int) cultist.getY(), (int) cultist.getZ())),
                net.minecraft.world.entity.MobSpawnType.SPAWNER,
                null);
        
        level().addFreshEntity(cultist);
        cultist.spawnExplosionParticle();
        
        cultist.playSound(ModSounds.WAND_FAIL.get(), 1.0f, 1.0f);
        
        // Portal takes damage when spawning
        hurt(damageSources().magic(), 5 + random.nextInt(5));
    }
    
    @Override
    public void playerTouch(Player player) {
        // Damage players that get too close
        if (distanceToSqr(player) < 3.0) {
            if (player.hurt(damageSources().indirectMagic(this, this), 4.0f)) {
                playSound(ModSounds.ZAP.get(), 1.0f, (random.nextFloat() - random.nextFloat()) * 0.1f + 1.0f);
            }
        }
    }
    
    @Override
    protected float getSoundVolume() {
        return 0.75f;
    }
    
    @Override
    public int getAmbientSoundInterval() {
        return 540;
    }
    
    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.MONOLITH.get();
    }
    
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ZAP.get();
    }
    
    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SHOCK.get();
    }
    
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean wasRecentlyHit) {
        // No drops
    }
    
    @Override
    public void handleEntityEvent(byte id) {
        if (id == 16) {
            pulse = 10;
            // Spawn pulse particles
            for (int i = 0; i < 10; i++) {
                double dx = (random.nextDouble() - 0.5) * 2.0;
                double dy = random.nextDouble() * getBbHeight();
                double dz = (random.nextDouble() - 0.5) * 2.0;
                level().addParticle(ParticleTypes.WITCH,
                        getX() + dx, getY() + dy, getZ() + dz,
                        dx * 0.2, 0.2, dz * 0.2);
            }
        } else {
            super.handleEntityEvent(id);
        }
    }
    
    @Override
    public boolean addEffect(MobEffectInstance effect, Entity source) {
        // Immune to potion effects
        return false;
    }
    
    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        // Immune to fall damage
        return false;
    }
    
    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide) {
            // Create explosion on death
            level().explode(this, getX(), getY(), getZ(), 1.5f, Level.ExplosionInteraction.NONE);
        }
        super.die(source);
    }
    

    
    @Override
    public boolean fireImmune() {
        return true;
    }
}




