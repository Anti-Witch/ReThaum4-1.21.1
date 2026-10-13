package thaumcraft.common.entities.projectile;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import thaumcraft.init.ModEntities;

import java.util.List;

/**
 * EntityEldritchOrb - AOE magic orb fired by Eldritch creatures.
 * 
 * Features:
 * - No gravity, flies straight
 * - AOE damage on impact (2 block radius)
 * - Applies weakness to living targets (not undead)
 * - Damage based on owner's attack attribute
 */
public class EntityEldritchOrb extends ThrowableProjectile {
    
    public EntityEldritchOrb(EntityType<? extends EntityEldritchOrb> type, Level level) {
        super(type, level);
    }
    
    public EntityEldritchOrb(Level level, LivingEntity owner) {
        super(ModEntities.ELDRITCH_ORB.get(), owner, level);
        // Shoot in direction owner is looking
        shootFromRotation(owner, owner.getXRot(), owner.getYRot(), -5.0f, 0.75f, 0.0f);
    }
    
    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        // No additional synced data
    }
    
    @Override
    protected double getDefaultGravity() {
        return 0.0f; // No gravity
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // Die after 5 seconds
        if (tickCount > 100) {
            discard();
            return;
        }
        
        // Client particles - dark purple/void themed
        if (level().isClientSide) {
            for (int i = 0; i < 2; i++) {
                level().addParticle(ParticleTypes.PORTAL,
                    getX() + (random.nextDouble() - 0.5) * 0.5,
                    getY() + (random.nextDouble() - 0.5) * 0.5,
                    getZ() + (random.nextDouble() - 0.5) * 0.5,
                    (random.nextDouble() - 0.5) * 0.2,
                    (random.nextDouble() - 0.5) * 0.2,
                    (random.nextDouble() - 0.5) * 0.2);
            }
        }
    }
    
    @Override
    protected void onHit(HitResult result) {
        if (!level().isClientSide) {
            Entity owner = getOwner();
            
            // Find all entities in 2 block radius (excluding owner)
            AABB aoe = getBoundingBox().inflate(2.0);
            List<Entity> entities = level().getEntities(owner, aoe);
            
            for (Entity entity : entities) {
                if (entity instanceof LivingEntity living) {
                    // Skip undead
                    if (living.isInvertedHealAndHarm()) continue;
                    
                    // Calculate damage from owner's attack attribute
                    float damage = 4.0f; // Default damage
                    if (owner instanceof LivingEntity livingOwner) {
                        damage = (float)(livingOwner.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.666);
                    }
                    
                    DamageSource source = level().damageSources().indirectMagic(this, owner);
                    living.hurt(source, damage);
                    
                    // Apply weakness
                    try {
                        living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 0));
                    } catch (Exception ignored) {}
                }
            }
            
            // Sound effect
            playSound(SoundEvents.LAVA_EXTINGUISH, 0.5f, 2.6f + (random.nextFloat() - random.nextFloat()) * 0.8f);
            discard();
        }
        
        // Client-side impact particles
        if (level().isClientSide) {
            for (int i = 0; i < 20; i++) {
                level().addParticle(ParticleTypes.PORTAL,
                    getX(), getY(), getZ(),
                    (random.nextDouble() - 0.5) * 0.5,
                    (random.nextDouble() - 0.5) * 0.5,
                    (random.nextDouble() - 0.5) * 0.5);
            }
        }
    }
}




