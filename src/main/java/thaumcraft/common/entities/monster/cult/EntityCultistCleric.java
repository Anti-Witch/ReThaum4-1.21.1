package thaumcraft.common.entities.monster.cult;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import thaumcraft.common.entities.monster.EntityEldritchGuardian;
import thaumcraft.common.entities.projectile.EntityGolemOrb;
import thaumcraft.init.ModEntities;
import thaumcraft.init.ModSounds;

/**
 * EntityCultistCleric - A ranged spellcaster of the Crimson Cult.
 * Wears crimson robes and attacks with magical projectiles.
 * Can act as a ritualist in cult rituals.
 */
public class EntityCultistCleric extends EntityCultist implements RangedAttackMob, IEntityWithComplexSpawn {
    
    private static final EntityDataAccessor<Boolean> DATA_RITUALIST = 
            SynchedEntityData.defineId(EntityCultistCleric.class, EntityDataSerializers.BOOLEAN);
    
    public int rage = 0;
    
    public EntityCultistCleric(EntityType<? extends EntityCultistCleric> type, Level level) {
        super(type, level);
    }
    
    public EntityCultistCleric(Level level) {
        this(ModEntities.CULTIST_CLERIC.get(), level);
    }
    
    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_RITUALIST, false);
    }
    
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // TODO: Add AIAltarFocus when implemented
        this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 20, 40, 24.0f));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(4, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 0.8));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, EntityCultist.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, EntityEldritchGuardian.class, true));
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.MAX_HEALTH, 24.0);
    }
    
    public boolean isRitualist() {
        return this.entityData.get(DATA_RITUALIST);
    }
    
    public void setRitualist(boolean ritualist) {
        this.entityData.set(DATA_RITUALIST, ritualist);
    }
    
    @Override
    protected void setLoot(DifficultyInstance difficulty) {
        // TODO: Use actual crimson robe armor when implemented
        // setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.CRIMSON_ROBE_HELM.get()));
        // setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.CRIMSON_ROBE_CHEST.get()));
        // setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.CRIMSON_ROBE_LEGS.get()));
        
        // Use leather armor dyed red as placeholder
        ItemStack helmet = new ItemStack(Items.LEATHER_HELMET);
        ItemStack chest = new ItemStack(Items.LEATHER_CHESTPLATE);
        ItemStack legs = new ItemStack(Items.LEATHER_LEGGINGS);
        
        setItemSlot(EquipmentSlot.HEAD, helmet);
        setItemSlot(EquipmentSlot.CHEST, chest);
        setItemSlot(EquipmentSlot.LEGS, legs);
        
        // Chance for boots on hard difficulty
        float bootChance = (level().getDifficulty() == Difficulty.HARD) ? 0.3f : 0.1f;
        if (random.nextFloat() < bootChance) {
            setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
        }
    }
    
    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        double dx = target.getX() - getX();
        double dy = target.getBoundingBox().minY + target.getBbHeight() / 2.0f - (getY() + getBbHeight() / 2.0f);
        double dz = target.getZ() - getZ();
        
        swing(InteractionHand.MAIN_HAND);
        
        float roll = random.nextFloat();
        if (roll > 0.66f) {
            // Fire homing orb
            EntityGolemOrb orb = new EntityGolemOrb(level(), this, target, true);
            Vec3 targetVel = new Vec3(target.getDeltaMovement().x * 10.0,
                    target.getDeltaMovement().y * 10.0,
                    target.getDeltaMovement().z * 10.0);
            Vec3 direction = target.position().add(targetVel).subtract(position()).normalize();
            orb.setPos(orb.getX() + direction.x, orb.getY() + direction.y, orb.getZ() + direction.z);
            orb.shoot(direction.x, direction.y, direction.z, 0.66f, 3.0f);
            playSound(ModSounds.EG_ATTACK.get(), 1.0f, 1.0f + random.nextFloat() * 0.1f);
            level().addFreshEntity(orb);
        } else {
            // Fire small fireballs
            float scatter = Mth.sqrt((float) distanceTo(target)) * 0.5f;
            level().levelEvent(null, 1009, blockPosition(), 0);
            
            for (int i = 0; i < 3; ++i) {
                SmallFireball fireball = new SmallFireball(level(), this,
                        new Vec3(dx + random.nextGaussian() * scatter,
                        dy,
                        dz + random.nextGaussian() * scatter));
                fireball.setPos(getX(), getY() + getBbHeight() / 2.0f + 0.5, getZ());
                level().addFreshEntity(fireball);
            }
        }
    }
    
    @Override
    public boolean removeWhenFarAway(double distance) {
        return !isRitualist();
    }
    
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) {
            return false;
        }
        // Attacking a ritualist interrupts the ritual
        setRitualist(false);
        return super.hurt(source, amount);
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // Look at home position when ritualist
        if (level().isClientSide && isRitualist() && hasHome()) {
            BlockPos home = getHomePos();
            double dx = home.getX() + 0.5 - getX();
            double dy = home.getY() + 1.5 - (getY() + getEyeHeight());
            double dz = home.getZ() + 0.5 - getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            
            float targetYaw = (float)(Math.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0f;
            float targetPitch = (float)(-(Math.atan2(dy, dist) * Mth.RAD_TO_DEG));
            
            setXRot(rotlerp(getXRot(), targetPitch, 10.0f));
            yHeadRot = rotlerp(yHeadRot, targetYaw, (float) getHeadRotSpeed());
        }
        
        // Stop ritualist mode if too angry
        if (!level().isClientSide && isRitualist() && rage >= 5) {
            setRitualist(false);
        }
    }
    
    private float rotlerp(float current, float target, float max) {
        float diff = Mth.wrapDegrees(target - current);
        if (diff > max) diff = max;
        if (diff < -max) diff = -max;
        return current + diff;
    }
    
    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.CHANT.get();
    }
    
    @Override
    public int getAmbientSoundInterval() {
        return 500;
    }
    
    @Override
    public void handleEntityEvent(byte id) {
        if (id == 19) {
            // Angry particles
            for (int i = 0; i < 3; ++i) {
                double dx = random.nextGaussian() * 0.02;
                double dy = random.nextGaussian() * 0.02;
                double dz = random.nextGaussian() * 0.02;
                level().addParticle(ParticleTypes.ANGRY_VILLAGER,
                        getX() + random.nextFloat() * getBbWidth() * 2.0f - getBbWidth(),
                        getY() + 0.5 + random.nextFloat() * getBbHeight(),
                        getZ() + random.nextFloat() * getBbWidth() * 2.0f - getBbWidth(),
                        dx, dy, dz);
            }
        } else {
            super.handleEntityEvent(id);
        }
    }
    
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("ritualist", isRitualist());
    }
    
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setRitualist(tag.getBoolean("ritualist"));
    }
    
    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        BlockPos home = getHomePos();
        if (home != null) {
            buffer.writeInt(home.getX());
            buffer.writeInt(home.getY());
            buffer.writeInt(home.getZ());
        } else {
            buffer.writeInt(0);
            buffer.writeInt(0);
            buffer.writeInt(0);
        }
    }
    
    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buffer) {
        BlockPos home = new BlockPos(buffer.readInt(), buffer.readInt(), buffer.readInt());
        if (home.getX() != 0 || home.getY() != 0 || home.getZ() != 0) {
            setHomePos(home, 8);
        }
    }
}




