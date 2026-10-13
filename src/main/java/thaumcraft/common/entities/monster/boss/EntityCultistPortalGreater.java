package thaumcraft.common.entities.monster.boss;
import net.minecraft.server.level.ServerLevel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.entities.monster.cult.EntityCultist;
import thaumcraft.common.entities.monster.cult.EntityCultistCleric;
import thaumcraft.common.entities.monster.cult.EntityCultistKnight;
import thaumcraft.init.ModEntities;
import thaumcraft.init.ModItems;

import java.util.List;

/**
 * EntityCultistPortalGreater - A larger, more powerful cultist portal.
 * This is a boss entity that spawns waves of cultists in stages,
 * eventually spawning a Cultist Leader. Has boss bar and complex spawn mechanics.
 * Spawns loot crates during setup phase, damages players that touch it.
 */
public class EntityCultistPortalGreater extends Monster {
    
    protected final ServerBossEvent bossEvent;
    
    private int stage = 0;
    private int stageCounter = 200;
    public int pulse = 0;
    
    public EntityCultistPortalGreater(EntityType<? extends EntityCultistPortalGreater> type, Level level) {
        super(type, level);
        this.xpReward = 30;
        this.setNoGravity(true);
        this.bossEvent = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_6);
        this.bossEvent.setDarkenScreen(true);
    }
    
    public EntityCultistPortalGreater(Level level) {
        this(ModEntities.CULTIST_PORTAL_GREATER.get(), level);
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 500.0)
                .add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ARMOR, 5.0);
    }
    
    @Override
    protected void registerGoals() {
        // No AI goals - stationary entity
    }
    
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("stage", stage);
    }
    
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        stage = tag.getInt("stage");
        if (hasCustomName()) {
            this.bossEvent.setName(getDisplayName());
        }
    }
    
    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(getDisplayName());
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
    
    // ==================== Properties ====================
    
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
        // Stationary entity
    }
    
    @Override
    protected void customServerAiStep() {
        // No AI
    }
    
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096.0;
    }
    
    @Override
    public float getLightLevelDependentMagicValue() {
        return 1.0f; // Always bright
    }
    
    // ==================== Update Logic ====================
    
    @Override
    public void tick() {
        super.tick();
        
        if (!level().isClientSide) {
            if (stageCounter > 0) {
                --stageCounter;
                
                // Stage 0 setup - spawn banners at tick 160
                if (stageCounter == 160 && stage == 0) {
                    level().broadcastEntityEvent(this, (byte) 16);
                    // TODO: Place banners around the portal when banner blocks implemented
                    // For now just play sound
                    playSound(SoundEvents.EVOKER_CAST_SPELL, 1.0f, 1.0f);
                }
                
                // Stage 0 setup - spawn loot crates between ticks 20-150
                if (stageCounter > 20 && stageCounter < 150 && stage == 0 && stageCounter % 13 == 0) {
                    int a = (int) getX() + random.nextInt(5) - random.nextInt(5);
                    int b = (int) getZ() + random.nextInt(5) - random.nextInt(5);
                    BlockPos bp = new BlockPos(a, (int) getY(), b);
                    
                    if (a != (int) getX() && b != (int) getZ() && level().isEmptyBlock(bp)) {
                        level().broadcastEntityEvent(this, (byte) 16);
                        // TODO: Place loot crates when implemented
                        // For now, drop a chest as placeholder
                        playSound(SoundEvents.EVOKER_CAST_SPELL, 1.0f, 1.0f);
                    }
                }
            } else if (level().getNearestPlayer(this, 48.0) != null) {
                // Active spawning phase
                level().broadcastEntityEvent(this, (byte) 16);
                
                switch (stage) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                        // Early stages - spawn minions quickly
                        stageCounter = 15 + random.nextInt(10 - stage) - stage;
                        spawnMinion();
                        break;
                    case 12:
                        // Stage 12 - spawn boss
                        stageCounter = 50 + getTiming() * 2 + random.nextInt(50);
                        spawnBoss();
                        break;
                    default:
                        // Later stages - spawn minions with timing based on existing cultists
                        int t = getTiming();
                        stageCounter = t + random.nextInt(5 + t / 3);
                        spawnMinion();
                        break;
                }
                ++stage;
            } else {
                // No player nearby - wait
                stageCounter = 30 + random.nextInt(30);
            }
            
            // Regenerate health before stage 12
            if (stage < 12) {
                heal(1.0f);
            }
            
            // Update boss bar
            this.bossEvent.setProgress(getHealth() / getMaxHealth());
        }
        
        if (pulse > 0) {
            --pulse;
        }
        
        // Client-side particles
        if (level().isClientSide) {
            // Portal particles
            if (random.nextInt(2) == 0) {
                double dx = (random.nextDouble() - 0.5) * getBbWidth();
                double dz = (random.nextDouble() - 0.5) * getBbWidth();
                level().addParticle(ParticleTypes.PORTAL,
                        getX() + dx, getY() + random.nextDouble() * getBbHeight(), getZ() + dz,
                        dx * 0.5, random.nextDouble() * 0.5, dz * 0.5);
            }
            // Flame particles (more ominous than lesser portal)
            if (random.nextInt(4) == 0) {
                level().addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                        getX() + (random.nextDouble() - 0.5) * 1.5,
                        getY() + random.nextDouble() * getBbHeight(),
                        getZ() + (random.nextDouble() - 0.5) * 1.5,
                        0, 0.02, 0);
            }
        }
    }
    
    /**
     * Gets spawn timing based on number of existing cultists nearby.
     */
    private int getTiming() {
        AABB searchBox = getBoundingBox().inflate(32.0);
        List<EntityCultist> cultists = level().getEntitiesOfClass(EntityCultist.class, searchBox);
        return cultists.size() * 20;
    }
    
    /**
     * Spawns a cultist minion (knight or cleric).
     */
    private void spawnMinion() {
        EntityCultist cultist;
        if (random.nextFloat() > 0.33f) {
            cultist = new EntityCultistKnight(level());
        } else {
            cultist = new EntityCultistCleric(level());
        }
        
        cultist.moveTo(
                getX() + random.nextFloat() - random.nextFloat(),
                getY() + 0.25,
                getZ() + random.nextFloat() - random.nextFloat(),
                random.nextFloat() * 360.0f, 0.0f);
        
        cultist.finalizeSpawn(
                (net.minecraft.world.level.ServerLevelAccessor) level(),
                level().getCurrentDifficultyAt(new BlockPos((int) cultist.getX(), (int) cultist.getY(), (int) cultist.getZ())),
                net.minecraft.world.entity.MobSpawnType.SPAWNER,
                null);
        
        cultist.setHomePos(blockPosition(), 32);
        level().addFreshEntity(cultist);
        cultist.spawnExplosionParticle();
        cultist.playSound(SoundEvents.EVOKER_CAST_SPELL, 1.0f, 1.0f);
        
        // After stage 12, portal takes damage when spawning
        if (stage > 12) {
            hurt(damageSources().magic(), 5 + random.nextInt(5));
        }
    }
    
    /**
     * Spawns the Cultist Leader boss.
     */
    private void spawnBoss() {
        EntityCultistLeader leader = new EntityCultistLeader(level());
        
        leader.moveTo(
                getX() + random.nextFloat() - random.nextFloat(),
                getY() + 0.25,
                getZ() + random.nextFloat() - random.nextFloat(),
                random.nextFloat() * 360.0f, 0.0f);
        
        leader.finalizeSpawn(
                (net.minecraft.world.level.ServerLevelAccessor) level(),
                level().getCurrentDifficultyAt(new BlockPos((int) leader.getX(), (int) leader.getY(), (int) leader.getZ())),
                net.minecraft.world.entity.MobSpawnType.SPAWNER,
                null);
        
        leader.setHomePos(blockPosition(), 32);
        level().addFreshEntity(leader);
        leader.spawnExplosionParticle();
        leader.playSound(SoundEvents.EVOKER_CAST_SPELL, 1.0f, 1.0f);
    }
    
    // ==================== Interaction ====================
    
    @Override
    public void playerTouch(Player player) {
        if (distanceToSqr(player) < 3.0) {
            if (player.hurt(damageSources().indirectMagic(this, this), 8.0f)) {
                playSound(SoundEvents.GENERIC_BURN, 1.0f, (random.nextFloat() - random.nextFloat()) * 0.1f + 1.0f);
            }
        }
    }
    
    // ==================== Sounds ====================
    
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
        return SoundEvents.AMBIENT_CAVE.value();
    }
    
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GENERIC_BURN;
    }
    
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GENERIC_EXPLODE.value();
    }
    
    // ==================== Loot ====================
    
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean wasRecentlyHit) {
        // Drop primordial pearl
        spawnAtLocation(new ItemStack(ModItems.PRIMORDIAL_PEARL.get()));
    }
    
    // ==================== Status Effects ====================
    
    @Override
    public void handleEntityEvent(byte id) {
        if (id == 16) {
            pulse = 10;
            // Spawn pulse particles
            for (int i = 0; i < 15; i++) {
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
        return false; // Immune to effects
    }
    
    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }
    
    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide) {
            level().explode(this, getX(), getY(), getZ(), 2.0f, Level.ExplosionInteraction.NONE);
        }
        super.die(source);
    }
    
    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
    
    @Override
    public boolean fireImmune() {
        return true;
    }
}




