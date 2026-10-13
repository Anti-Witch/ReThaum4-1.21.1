package thaumcraft.client.fx.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * FXGenericP2E - Point-to-Entity generic particle.
 * Particle that homes toward a target entity, useful for absorption/collection effects.
 * 
 * Extends FXGeneric with entity tracking behavior.
 * 
 * Ported from 1.12.2
 */
@OnlyIn(Dist.CLIENT)
public class FXGenericP2E extends FXGeneric {
    
    private Entity target;
    
    /**
     * Create a particle that moves toward a target entity.
     * 
     * @param level The client level
     * @param x Starting X position
     * @param y Starting Y position
     * @param z Starting Z position
     * @param target The entity to move toward
     */
    public FXGenericP2E(ClientLevel level, double x, double y, double z, Entity target) {
        super(level, x, y, z, 0, 0, 0);
        
        this.setSize(0.1f, 0.1f);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        
        this.target = target;
        
        // Calculate lifetime based on distance
        double dx = target.getX() - x;
        double dy = target.getY() - y;
        double dz = target.getZ() - z;
        int base = (int)(Mth.sqrt((float)(dx * dx + dy * dy + dz * dz)) * 5.0f);
        if (base < 1) base = 1;
        this.lifetime = base;
        
        // Small random initial velocity
        float f3 = 0.01f;
        this.xd = (float)random.nextGaussian() * f3;
        this.yd = (float)random.nextGaussian() * f3;
        this.zd = (float)random.nextGaussian() * f3;
        
        this.gravity = 0.2f;
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (target == null || !target.isAlive()) {
            this.remove();
            return;
        }
        
        double dx = target.getX() - x;
        double dy = target.getY() + target.getBbHeight() / 2.0 - y;
        double dz = target.getZ() - z;
        
        double d13 = 0.3;
        double dist = Mth.sqrt((float)(dx * dx + dy * dy + dz * dz));
        
        // Speed up when close, shrink particle
        if (dist < 4.0) {
            quadSize *= 0.9f;
            d13 = 0.6;
        }
        
        // Remove when very close
        if (dist < 0.25) {
            this.remove();
            return;
        }
        
        // Normalize direction
        dx /= dist;
        dy /= dist;
        dz /= dist;
        
        // Accelerate toward target
        xd += dx * d13;
        yd += dy * d13;
        zd += dz * d13;
        
        // Clamp max speed
        xd = Mth.clamp((float)xd, -0.35f, 0.35f);
        yd = Mth.clamp((float)yd, -0.35f, 0.35f);
        zd = Mth.clamp((float)zd, -0.35f, 0.35f);
    }
}




