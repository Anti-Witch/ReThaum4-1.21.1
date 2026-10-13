package thaumcraft.common.entities.monster.boss;
import net.minecraft.server.level.ServerLevel;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
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
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.entities.monster.cult.EntityCultist;
import thaumcraft.common.entities.projectile.EntityGolemOrb;
import thaumcraft.init.ModEntities;
import thaumcraft.init.ModItems;
import thaumcraft.init.ModSounds;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * EntityCultistLeader - The boss of the Crimson Cult.
 * A powerful cultist that leads other cult members in battle.
 * Has both ranged (orb projectile) and melee attacks.
 * Provides regeneration to nearby cultists.
 */
public class EntityCultistLeader extends EntityThaumcraftBoss implements RangedAttackMob {
    
    private static final EntityDataAccessor<Byte> DATA_TITLE = 
            SynchedEntityData.defineId(EntityCultistLeader.class, EntityDataSerializers.BYTE);
    
    private static final String[] TITLES = {
            "Alberic", "Anselm", "Bastian", "Beturian", "Chabier",
            "Chorache", "Chuse", "Dodorol", "Ebardo", "Ferrando",
            "Fertus", "Guillen", "Larpe", "Obano", "Zelipe"
    };
    
    public EntityCultistLeader(EntityType<? extends EntityCultistLeader> type, Level level) {
        super(type, level);
        this.xpReward = 40;
    }
    
    public EntityCultistLeader(Level level) {
        this(ModEntities.CULTIST_LEADER.get(), level);
    }
    
    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TITLE, (byte) 0);
    }
    
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 30, 40, 24.0f));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.1, false));
        this.goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 0.8));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, EntityCultist.class, EntityCultistLeader.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return EntityThaumcraftBoss.createBossAttributes()
                .add(Attributes.MAX_HEALTH, 150.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ARMOR, 8.0);
    }
    
    @Override
    public void generateName() {
        // TODO: Add champion modifier name when implemented
        setCustomName(net.minecraft.network.chat.Component.literal(getTitle() + " the Crimson Praetor"));
    }
    
    public String getTitle() {
        return TITLES[this.entityData.get(DATA_TITLE) % TITLES.length];
    }
    
    public void setTitle(int title) {
        this.entityData.set(DATA_TITLE, (byte) (title % TITLES.length));
    }
    
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
    
    /**
     * Set up equipment based on difficulty.
     * Equips the Crimson Praetor armor set and Crimson Blade.
     */
    protected void setLoot(DifficultyInstance difficulty) {
        // Equip Crimson Praetor armor
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.CRIMSON_PRAETOR_HELM.get()));
        setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.CRIMSON_PRAETOR_CHEST.get()));
        setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.CRIMSON_PRAETOR_LEGS.get()));
        setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.CRIMSON_BOOTS.get()));
        
        // Equip Crimson Blade as weapon
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.CRIMSON_BLADE.get()));
        
        // No drops from equipment (armor is rare boss loot handled separately)
        setDropChance(EquipmentSlot.HEAD, 0.0f);
        setDropChance(EquipmentSlot.CHEST, 0.0f);
        setDropChance(EquipmentSlot.LEGS, 0.0f);
        setDropChance(EquipmentSlot.FEET, 0.0f);
        setDropChance(EquipmentSlot.MAINHAND, 0.0f);
    }
    
    /**
     * Add enchantments based on difficulty.
     */
    protected void setEnchantmentBasedOnDifficulty(DifficultyInstance difficulty) {
        float diff = difficulty.getSpecialMultiplier();
        ItemStack weapon = getMainHandItem();
        if (!weapon.isEmpty() && random.nextFloat() < 0.5f * diff) {
            EnchantmentHelper.enchantItem(random, weapon, (int)(7.0f + diff * random.nextInt(22)), level().registryAccess(), Optional.empty());
        }
    }
    
    // ==================== Team Logic ====================
    
    @Override
    public boolean isAlliedTo(Entity entity) {
        if (entity instanceof EntityCultist || entity instanceof EntityCultistLeader) {
            return true;
        }
        return super.isAlliedTo(entity);
    }
    
    @Override
    public boolean canAttack(LivingEntity target) {
        if (target instanceof EntityCultist || target instanceof EntityCultistLeader) {
            return false;
        }
        return super.canAttack(target);
    }
    
    // ==================== Spawn ====================
    
    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            MobSpawnType spawnType, @Nullable SpawnGroupData spawnData) {
        
        setLoot(difficulty);
        setEnchantmentBasedOnDifficulty(difficulty);
        setTitle(random.nextInt(TITLES.length));
        return super.finalizeSpawn(level, difficulty, spawnType, spawnData);
    }
    
    // ==================== AI Updates ====================
    
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        
        // Provide regeneration to nearby cultists
        if (tickCount % 20 == 0) {
            AABB searchBox = getBoundingBox().inflate(8.0);
            List<EntityCultist> nearbyCultists = level().getEntitiesOfClass(EntityCultist.class, searchBox);
            
            for (EntityCultist cultist : nearbyCultists) {
                try {
                    if (!cultist.hasEffect(MobEffects.REGENERATION)) {
                        cultist.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 1));
                    }
                } catch (Exception ignored) {}
            }
        }
    }
    
    // ==================== Ranged Attack ====================
    
    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        if (hasLineOfSight(target)) {
            swing(InteractionHand.MAIN_HAND);
            getLookControl().setLookAt(target, 30.0f, 30.0f);
            
            // Create and fire the orb projectile (red = true for boss)
            EntityGolemOrb orb = new EntityGolemOrb(level(), this, target, true);
            
            // Calculate aim direction
            double dx = target.getX() - getX();
            double dy = target.getY(0.5) - getY(0.5);
            double dz = target.getZ() - getZ();
            
            // Offset projectile position slightly forward
            Vec3 forward = new Vec3(dx, 0, dz).normalize().scale(0.5);
            orb.setPos(orb.getX() + forward.x, orb.getY(), orb.getZ() + forward.z);
            
            // Shoot with some vertical adjustment
            orb.shoot(dx, dy + 2.0, dz, 0.66f, 3.0f);
            
            playSound(ModSounds.EG_ATTACK.get(), 1.0f, 1.0f + random.nextFloat() * 0.1f);
            level().addFreshEntity(orb);
        }
    }
    
    // ==================== Spawn Particles ====================
    
    public void spawnExplosionParticle() {
        if (level().isClientSide) {
            for (int i = 0; i < 20; ++i) {
                double dx = random.nextGaussian() * 0.05;
                double dy = random.nextGaussian() * 0.05;
                double dz = random.nextGaussian() * 0.05;
                level().addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,
                        getX() + random.nextFloat() * getBbWidth() * 2.0f - getBbWidth() + dx * 2.0,
                        getY() + random.nextFloat() * getBbHeight() + dy * 2.0,
                        getZ() + random.nextFloat() * getBbWidth() * 2.0f - getBbWidth() + dz * 2.0,
                        dx, dy, dz);
            }
        } else {
            level().broadcastEntityEvent(this, (byte) 20);
        }
    }
    
    @Override
    public void handleEntityEvent(byte id) {
        if (id == 20) {
            spawnExplosionParticle();
        } else {
            super.handleEntityEvent(id);
        }
    }
    
    // ==================== Loot ====================
    
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean wasRecentlyHit) {
        super.dropCustomDeathLoot(level, source, wasRecentlyHit);
        // Drop tier 2 (uncommon) loot bag
        spawnAtLocation(new ItemStack(ModItems.LOOT_BAG_UNCOMMON.get()));
    }
}




