package thaumcraft.common.entities.monster;
import net.minecraft.server.level.ServerLevel;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import thaumcraft.init.ModEntities;

/**
 * EntityGiantBrainyZombie - A zombie that grows larger when damaged.
 * Gets stronger and bigger when hit, making it more dangerous.
 */
public class EntityGiantBrainyZombie extends EntityBrainyZombie {
    
    private static final EntityDataAccessor<Float> DATA_ANGER = 
            SynchedEntityData.defineId(EntityGiantBrainyZombie.class, EntityDataSerializers.FLOAT);
    
    public EntityGiantBrainyZombie(EntityType<? extends EntityGiantBrainyZombie> type, Level level) {
        super(type, level);
        this.xpReward = 15;
    }
    
    public EntityGiantBrainyZombie(Level level) {
        super(ModEntities.GIANT_BRAINY_ZOMBIE.get(), level);
        this.xpReward = 15;
    }
    
    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Add leap attack
        this.goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.4f));
    }
    
    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ANGER, 1.0f);
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0);
    }
    
    public float getAnger() {
        return this.entityData.get(DATA_ANGER);
    }
    
    public void setAnger(float anger) {
        this.entityData.set(DATA_ANGER, anger);
    }
    
    @Override
    public void aiStep() {
        super.aiStep();
        
        // Slowly calm down
        if (getAnger() > 1.0f) {
            setAnger(getAnger() - 0.002f);
        }
        
        // Update attack damage based on anger
        if (!level().isClientSide) {
            double baseDamage = 7.0 + (getAnger() - 1.0f) * 5.0;
            getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(baseDamage);
        }
    }
    
    /**
     * Gets the scale factor based on anger.
     */
    public float getScale() {
        return 1.0f + (getAnger() - 1.0f);
    }
    
    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return super.getDefaultDimensions(pose).scale(getScale());
    }
    
    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Get angrier when damaged
        setAnger(Math.min(2.0f, getAnger() + 0.1f));
        refreshDimensions(); // Update hitbox size
        return super.hurt(source, amount);
    }
    
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean wasRecentlyHit) {
        // Drop lots of rotten flesh
        for (int a = 0; a < 6; a++) {
            if (random.nextBoolean()) {
                spawnAtLocation(new ItemStack(Items.ROTTEN_FLESH, 2));
            }
        }
        for (int a = 0; a < 6; a++) {
            if (random.nextBoolean()) {
                spawnAtLocation(new ItemStack(Items.ROTTEN_FLESH, 2));
            }
        }
    }
}




