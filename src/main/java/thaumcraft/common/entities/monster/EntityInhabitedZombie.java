package thaumcraft.common.entities.monster;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import thaumcraft.init.ModEntities;

import javax.annotation.Nullable;
import java.util.List;

/**
 * EntityInhabitedZombie - A zombie inhabited by an eldritch crab.
 * When killed, spawns an EntityEldritchCrab.
 * Implements IEldritchMob behavior.
 */
public class EntityInhabitedZombie extends Zombie {
    
    public EntityInhabitedZombie(EntityType<? extends EntityInhabitedZombie> type, Level level) {
        super(type, level);
    }
    
    public EntityInhabitedZombie(Level level) {
        super(ModEntities.INHABITED_ZOMBIE.get(), level);
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0);
    }
    
    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            MobSpawnType spawnType, @Nullable SpawnGroupData spawnData) {
        
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnData);
        
        // Equip with crimson armor based on difficulty
        float armorChance = (level.getDifficulty() == Difficulty.HARD) ? 0.9f : 0.6f;
        
        // TODO: Use actual crimson armor when implemented
        // setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.CRIMSON_PLATE_HELM.get()));
        // if (random.nextFloat() <= armorChance) {
        //     setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.CRIMSON_PLATE_CHEST.get()));
        // }
        // if (random.nextFloat() <= armorChance) {
        //     setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.CRIMSON_PLATE_LEGS.get()));
        // }
        
        return result;
    }
    
    // Note: In 1.20.1, zombies don't have killedEntity that spawns reinforcements
    // The reinforcement logic is handled differently - via SPAWN_REINFORCEMENTS_CHANCE attribute
    // which we already set to 0.0 in createAttributes()
    
    @Override
    protected boolean shouldDropLoot() {
        return false; // No normal loot, crab spawns instead
    }
    
    @Override
    protected SoundEvent getAmbientSound() {
        // TODO: Return SoundsTC.crabtalk when implemented
        return SoundEvents.ZOMBIE_AMBIENT;
    }
    
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GENERIC_HURT;
    }
    
    @Override
    protected void tickDeath() {
        // Custom death - spawn crab immediately and remove
        if (!level().isClientSide) {
            // TODO: Spawn EntityEldritchCrab when implemented
            // EntityEldritchCrab crab = new EntityEldritchCrab(level());
            // crab.moveTo(getX(), getY() + getEyeHeight(), getZ(), getYRot(), getXRot());
            // crab.setHelm(true);
            // level().addFreshEntity(crab);
            
            // Drop XP
            if ((lastHurtByPlayerTime > 0 || isAlwaysExperienceDropper()) && 
                    level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBLOOT)) {
                int xp = getExperienceReward((net.minecraft.server.level.ServerLevel) level(), this.lastHurtByPlayer);
                net.minecraft.world.entity.ExperienceOrb.award((net.minecraft.server.level.ServerLevel)level(), position(), xp);
            }
        }
        
        // Explosion particles
        for (int k = 0; k < 20; k++) {
            double dx = random.nextGaussian() * 0.02;
            double dy = random.nextGaussian() * 0.02;
            double dz = random.nextGaussian() * 0.02;
            level().addParticle(ParticleTypes.POOF,
                    getX() + random.nextFloat() * getBbWidth() * 2.0f - getBbWidth(),
                    getY() + random.nextFloat() * getBbHeight(),
                    getZ() + random.nextFloat() * getBbWidth() * 2.0f - getBbWidth(),
                    dx, dy, dz);
        }
        
        discard();
    }
    
    @Override
    public void die(DamageSource source) {
        // Don't call super - we handle death in tickDeath
    }
    
    @Override
    public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, MobSpawnType spawnType) {
        // Limit spawn density
        List<EntityInhabitedZombie> nearby = level.getEntitiesOfClass(EntityInhabitedZombie.class,
                new AABB(getX() - 32, getY() - 16, getZ() - 32, 
                        getX() + 32, getY() + 16, getZ() + 32));
        return nearby.isEmpty() && super.checkSpawnRules(level, spawnType);
    }
}




