package thaumcraft.common.entities.projectile;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import thaumcraft.init.ModEntities;

/**
 * EntityAlumentum - Explosive throwable projectile.
 * When it hits something, it creates a small explosion.
 * Used with Alumentum item for combat/utility.
 */
public class EntityAlumentum extends ThrowableProjectile {
    
    public EntityAlumentum(EntityType<? extends EntityAlumentum> type, Level level) {
        super(type, level);
    }
    
    public EntityAlumentum(Level level, LivingEntity owner) {
        super(ModEntities.ALUMENTUM.get(), owner, level);
    }
    
    public EntityAlumentum(Level level, double x, double y, double z) {
        super(ModEntities.ALUMENTUM.get(), x, y, z, level);
    }
    
    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        // No additional synced data needed
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // Client-side particle effects
        if (level().isClientSide) {
            // TODO: Add particle effects via FXDispatcher when implemented
            // For now, spawn basic flame particles
            for (int i = 0; i < 3; i++) {
                double coeff = i / 3.0;
                double px = xOld + (getX() - xOld) * coeff;
                double py = yOld + (getY() - yOld) * coeff + getBbHeight() / 2.0f;
                double pz = zOld + (getZ() - zOld) * coeff;
                
                level().addParticle(
                    net.minecraft.core.particles.ParticleTypes.FLAME,
                    px, py, pz,
                    0.0125 * (random.nextFloat() - 0.5),
                    0.0125 * (random.nextFloat() - 0.5),
                    0.0125 * (random.nextFloat() - 0.5)
                );
            }
        }
    }
    
    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        
        if (!level().isClientSide) {
            // Create explosion at impact point
            // Explosion size 1.1f - small but noticeable
            level().explode(this, getX(), getY(), getZ(), 1.1f, Level.ExplosionInteraction.TNT);
            discard();
        }
    }
    
    @Override
    protected double getDefaultGravity() {
        return 0.03f; // Standard throwable gravity
    }
}




