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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import thaumcraft.init.ModBlocks;
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
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.entities.IEldritchMob;
import thaumcraft.common.entities.monster.EntityEldritchGuardian;
import thaumcraft.common.entities.monster.cult.EntityCultist;
import thaumcraft.common.entities.projectile.EntityEldritchOrb;
import thaumcraft.init.ModEntities;
import thaumcraft.init.ModSounds;

import javax.annotation.Nullable;

/**
 * EntityEldritchWarden - The most powerful eldritch boss.
 * Features:
 * - Dual boss bars (health + absorption shield)
 * - Ranged attacks with eldritch orbs
 * - Sonic blast attack that applies wither and weakness
 * - Field frenzy ability that spawns damaging blocks in expanding circles
 * - Teleport to home position during field frenzy
 * - Absorption shield regenerates over time
 * - Leaves "sap" effect blocks when walking
 */
public class EntityEldritchWarden extends EntityThaumcraftBoss implements RangedAttackMob, IEldritchMob {
    
    private static final EntityDataAccessor<Byte> DATA_TITLE = 
            SynchedEntityData.defineId(EntityEldritchWarden.class, EntityDataSerializers.BYTE);
    
    // Secondary boss bar for absorption shield
    protected final ServerBossEvent bossEventShield;
    
    private static final String[] TITLES = {
            "Aphoom-Zhah", "Basatan", "Chaugnar Faugn", "Mnomquah", "Nyogtha",
            "Oorn", "Shaikorth", "Rhan-Tegoth", "Rhogog", "Shudde M'ell",
            "Vulthoom", "Yag-Kosha", "Yibb-Tstll", "Zathog", "Zushakon"
    };
    
    private boolean fieldFrenzy = false;
    private int fieldFrenzyCounter = 0;
    private boolean lastBlast = false;
    
    // Client-side arm animation
    public float armLiftL = 0.0f;
    public float armLiftR = 0.0f;
    
    public EntityEldritchWarden(EntityType<? extends EntityEldritchWarden> type, Level level) {
        super(type, level);
        this.xpReward = 100;
        this.bossEventShield = new ServerBossEvent(Component.literal(""), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);
    }
    
    public EntityEldritchWarden(Level level) {
        this(ModEntities.ELDRITCH_WARDEN.get(), level);
    }
    
    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TITLE, (byte) 0);
    }
    
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 20, 40, 24.0f));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.1, false));
        this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.8));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, EntityCultist.class, true));
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return EntityThaumcraftBoss.createBossAttributes()
                .add(Attributes.MAX_HEALTH, 400.0)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.33)
                .add(Attributes.ARMOR, 8.0);
    }
    
    // ==================== Title System ====================
    
    @Override
    public void generateName() {
        // TODO: Add champion modifier name when implemented
        setCustomName(Component.literal(getTitle() + " the Eldritch Warden"));
    }
    
    public String getTitle() {
        return TITLES[this.entityData.get(DATA_TITLE) % TITLES.length];
    }
    
    public void setTitle(int title) {
        this.entityData.set(DATA_TITLE, (byte) (title % TITLES.length));
    }
    
    // ==================== Boss Bars ====================
    
    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEventShield.addPlayer(player);
    }
    
    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEventShield.removePlayer(player);
    }
    
    // ==================== NBT ====================
    
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("title", this.entityData.get(DATA_TITLE));
    }
    
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setTitle(tag.getByte("title"));
    }
    
    // ==================== Spawn ====================
    
    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            MobSpawnType spawnType, @Nullable SpawnGroupData spawnData) {

        spawnTimer = 150;
        setTitle(random.nextInt(TITLES.length));

        // Start with absorption shield equal to 66% of max health
        float shieldAmount = (float)(getAttributeValue(Attributes.MAX_HEALTH) * 0.66);
        setAbsorptionAmount(shieldAmount);

        return super.finalizeSpawn(level, difficulty, spawnType, spawnData);
    }
        
    // ==================== AI / Update ====================
    
    @Override
    protected void customServerAiStep() {
        if (fieldFrenzyCounter == 0) {
            super.customServerAiStep();
        }
        
        // Regenerate absorption shield
        int maxShield = (int)(getAttributeValue(Attributes.MAX_HEALTH) * 0.66);
        if (hurtTime <= 0 && tickCount % 25 == 0 && getAbsorptionAmount() < maxShield) {
            setAbsorptionAmount(getAbsorptionAmount() + 1.0f);
        }
        
        // Update shield boss bar
        this.bossEventShield.setProgress(getAbsorptionAmount() / maxShield);
    }
    
    @Override
    public void tick() {
        if (spawnTimer == 150) {
            level().broadcastEntityEvent(this, (byte) 18);
        }
        
        super.tick();
        
        if (level().isClientSide) {
            // Decay arm lift animations
            if (armLiftL > 0.0f) {
                armLiftL -= 0.05f;
            }
            if (armLiftR > 0.0f) {
                armLiftR -= 0.05f;
            }
            
            // Wisp particles
            float x = (float)(getX() + (random.nextFloat() - random.nextFloat()) * 0.2f);
            float z = (float)(getZ() + (random.nextFloat() - random.nextFloat()) * 0.2f);
            level().addParticle(ParticleTypes.WITCH,
                    x, getY() + 0.25 * getBbHeight(), z,
                    0, 0.02, 0);
            
            // Spawn animation smoke spiral
            if (spawnTimer > 0) {
                float height = Math.max(1.0f, getBbHeight() * ((150 - spawnTimer) / 150.0f));
                for (int a = 0; a < 3; ++a) {
                    level().addParticle(ParticleTypes.LARGE_SMOKE,
                            getX() + (random.nextFloat() - 0.5) * height,
                            getBoundingBox().minY + height / 2.0f,
                            getZ() + (random.nextFloat() - 0.5) * height,
                            0, 0.05, 0);
                }
            }
        }
    }
    
    @Override
    public void aiStep() {
        super.aiStep();
        
        // Place "sap" effect blocks when walking
        if (!level().isClientSide) {
            int i = Mth.floor(getX());
            int j = Mth.floor(getY());
            int k = Mth.floor(getZ());
            
            for (int l = 0; l < 4; ++l) {
                int bi = Mth.floor(getX() + (l % 2 * 2 - 1) * 0.25f);
                int bj = Mth.floor(getY());
                int bk = Mth.floor(getZ() + (l / 2 % 2 * 2 - 1) * 0.25f);
                BlockPos bp = new BlockPos(bi, bj, bk);
                
                if (level().isEmptyBlock(bp)) {
                    level().setBlock(bp, ModBlocks.EFFECT_SAP.get().defaultBlockState(), 3);
                    level().scheduleTick(bp, ModBlocks.EFFECT_SAP.get(), 20 + random.nextInt(20));
                }
            }
            
            // Field frenzy phase
            if (fieldFrenzyCounter > 0) {
                if (fieldFrenzyCounter == 150) {
                    teleportHome();
                }
                performFieldFrenzy();
            }
        }
    }
    
    /**
     * Performs the field frenzy attack - spawning rings of damaging blocks.
     */
    private void performFieldFrenzy() {
        if (fieldFrenzyCounter < 121 && fieldFrenzyCounter % 10 == 0) {
            level().broadcastEntityEvent(this, (byte) 17);
            
            double radius = (150 - fieldFrenzyCounter) / 8.0;
            int d = 1 + fieldFrenzyCounter / 8;
            int i = Mth.floor(getX());
            int j = Mth.floor(getY());
            int k = Mth.floor(getZ());
            
            for (int q = 0; q < 180 / d; ++q) {
                double radians = Math.toRadians(q * 2 * d);
                int deltaX = (int)(radius * Math.cos(radians));
                int deltaZ = (int)(radius * Math.sin(radians));
                BlockPos bp = new BlockPos(i + deltaX, j, k + deltaZ);
                
                if (level().isEmptyBlock(bp)) {
                    BlockState below = level().getBlockState(bp.below());
                    if (below.isSolidRender(level(), bp.below())) {
                        level().setBlock(bp, ModBlocks.EFFECT_SAP.get().defaultBlockState(), 3);
                        level().scheduleTick(bp, ModBlocks.EFFECT_SAP.get(), 20 + random.nextInt(40));
                    }
                }
            }
            
            playSound(SoundEvents.GENERIC_BURN, 1.0f, 0.9f + random.nextFloat() * 0.1f);
        }
        --fieldFrenzyCounter;
    }
    
    /**
     * Teleport back to home position.
     */
    private void teleportHome() {
        if (!hasHome()) return;
        
        BlockPos home = getRestrictCenter();
        double oldX = getX();
        double oldY = getY();
        double oldZ = getZ();
        
        // Find valid teleport position near home
        int tries = 20;
        int x = home.getX();
        int y = home.getY();
        int z = home.getZ();
        
        while (tries > 0) {
            BlockPos checkPos = new BlockPos(x, y, z);
            BlockState below = level().getBlockState(checkPos.below());
            BlockState at = level().getBlockState(checkPos);
            
            if (below.blocksMotion() && !at.blocksMotion()) {
                // Valid position
                teleportTo(x + 0.5, y + 0.1, z + 0.5);
                
                // Teleport particles
                for (int i = 0; i < 128; i++) {
                    double progress = i / 127.0;
                    double px = oldX + (getX() - oldX) * progress + (random.nextDouble() - 0.5) * getBbWidth() * 2.0;
                    double py = oldY + (getY() - oldY) * progress + random.nextDouble() * getBbHeight();
                    double pz = oldZ + (getZ() - oldZ) * progress + (random.nextDouble() - 0.5) * getBbWidth() * 2.0;
                    level().addParticle(ParticleTypes.PORTAL, px, py, pz, 
                            (random.nextFloat() - 0.5f) * 0.2f,
                            (random.nextFloat() - 0.5f) * 0.2f,
                            (random.nextFloat() - 0.5f) * 0.2f);
                }
                playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0f, 1.0f);
                return;
            }
            
            x = home.getX() + random.nextInt(8) - random.nextInt(8);
            z = home.getZ() + random.nextInt(8) - random.nextInt(8);
            --tries;
        }
    }
    
    // ==================== Damage Handling ====================
    
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return fieldFrenzyCounter > 0 || source == damageSources().drown() || source == damageSources().wither() 
                || super.isInvulnerableTo(source);
    }
    
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) {
            return false;
        }
        
        boolean wasHurt = super.hurt(source, amount);
        
        // Trigger field frenzy when absorption shield is depleted
        if (!level().isClientSide && wasHurt && !fieldFrenzy && getAbsorptionAmount() <= 0.0f) {
            fieldFrenzy = true;
            fieldFrenzyCounter = 150;
        }
        
        return wasHurt;
    }
    
    // ==================== Ranged Attack ====================
    
    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        if (random.nextFloat() > 0.2f) {
            // Fire eldritch orb
            EntityEldritchOrb orb = new EntityEldritchOrb(level(), this);
            
            lastBlast = !lastBlast;
            level().broadcastEntityEvent(this, (byte)(lastBlast ? 16 : 15));
            
            int rotOffset = lastBlast ? 90 : 180;
            double xx = Mth.cos((getYRot() + rotOffset) * Mth.DEG_TO_RAD) * 0.5f;
            double yy = 0.13;
            double zz = Mth.sin((getYRot() + rotOffset) * Mth.DEG_TO_RAD) * 0.5f;
            orb.setPos(orb.getX() - xx, orb.getY() - yy, orb.getZ() - zz);
            
            double dx = target.getX() + target.getDeltaMovement().x - getX();
            double dy = target.getY() - getY() - target.getBbHeight() / 2.0;
            double dz = target.getZ() + target.getDeltaMovement().z - getZ();
            
            orb.shoot(dx, dy, dz, 1.0f, 2.0f);
            
            playSound(ModSounds.EG_ATTACK.get(), 2.0f, 1.0f + random.nextFloat() * 0.1f);
            level().addFreshEntity(orb);
        } else if (hasLineOfSight(target)) {
            // Sonic blast attack
            // TODO: Send PacketFXSonic when implemented
            
            // Knockback
            float knockX = -Mth.sin(getYRot() * Mth.DEG_TO_RAD) * 1.5f;
            float knockZ = Mth.cos(getYRot() * Mth.DEG_TO_RAD) * 1.5f;
            target.setDeltaMovement(target.getDeltaMovement().add(knockX, 0.1, knockZ));
            
            // Apply debuffs
            try {
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 400, 0));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 400, 0));
            } catch (Exception ignored) {}
            
            // Add warp to player
            if (target instanceof Player player) {
                ThaumcraftApi.internalMethods.addWarpToPlayer(player, 1, IPlayerWarp.EnumWarpType.NORMAL);
            }
            
            playSound(ModSounds.EG_SCREECH.get(), 4.0f, 1.0f + random.nextFloat() * 0.1f);
        }
    }
    
    // ==================== Status Events ====================
    
    @Override
    public void handleEntityEvent(byte id) {
        if (id == 15) {
            armLiftL = 0.5f;
        } else if (id == 16) {
            armLiftR = 0.5f;
        } else if (id == 17) {
            armLiftL = 0.9f;
            armLiftR = 0.9f;
        } else if (id == 18) {
            spawnTimer = 150;
        } else {
            super.handleEntityEvent(id);
        }
    }
    
    // ==================== Team Logic ====================
    
    @Override
    public boolean canAttack(LivingEntity target) {
        if (target instanceof EntityEldritchGuardian) {
            return false;
        }
        return super.canAttack(target);
    }
    
    // ==================== Sounds ====================
    
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
}




