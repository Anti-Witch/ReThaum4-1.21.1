package thaumcraft.client.fx.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * FXGenericP2P - Point-to-point particle that travels toward a target location.
 * Extends FXGeneric with homing behavior toward the target coordinates.
 * Used for essentia streams, vis transfer, and magical particle trails.
 */
@OnlyIn(Dist.CLIENT)
public class FXGenericP2P extends FXGeneric {

    protected double targetX;
    protected double targetY;
    protected double targetZ;

    public FXGenericP2P(ClientLevel level, double x, double y, double z,
                        double targetX, double targetY, double targetZ) {
        super(level, x, y, z, 0, 0, 0);

        this.setSize(0.1f, 0.1f);

        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;

        // Calculate distance and set lifetime based on it
        double dx = targetX - x;
        double dy = targetY - y;
        double dz = targetZ - z;
        int base = (int) (Math.sqrt(dx * dx + dy * dy + dz * dz) * 3.0f);
        if (base < 1) base = 1;

        this.lifetime = base / 2 + this.random.nextInt(base);

        // Small random initial velocity
        float f3 = 0.01f;
        this.xd = this.random.nextGaussian() * f3;
        this.yd = this.random.nextGaussian() * f3;
        this.zd = this.random.nextGaussian() * f3;

        this.gravity = 0.2f;
    }

    @Override
    public void tick() {
        // Call parent tick for basic animation
        super.tick();

        // Calculate direction to target
        double dx = this.targetX - this.x;
        double dy = this.targetY - this.y;
        double dz = this.targetZ - this.z;

        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        // Acceleration toward target
        double accel = 0.3;

        // Shrink and accelerate when close
        if (distance < 4.0) {
            this.quadSize *= 0.9f;
            accel = 0.6;
        }

        // Normalize direction
        if (distance > 0) {
            dx /= distance;
            dy /= distance;
            dz /= distance;
        }

        // Apply acceleration toward target
        this.xd += dx * accel;
        this.yd += dy * accel;
        this.zd += dz * accel;

        // Clamp velocity
        this.xd = Mth.clamp(this.xd, -0.35, 0.35);
        this.yd = Mth.clamp(this.yd, -0.35, 0.35);
        this.zd = Mth.clamp(this.zd, -0.35, 0.35);
    }

    // ==================== Configuration Methods ====================

    public void setTarget(double x, double y, double z) {
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
    }
}




