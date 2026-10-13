package thaumcraft.client.fx.particles;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * FXVisSparkle - Sparkle particle that travels toward a target location.
 * Used for vis effects and magical sparkles moving toward nodes/targets.
 */
@OnlyIn(Dist.CLIENT)
public class FXVisSparkle extends TextureSheetParticle {

    protected double targetX;
    protected double targetY;
    protected double targetZ;
    protected float sizeMod;

    // Sprite tracking
    protected int spriteIndexX = 0;
    protected int spriteIndexY = 0;
    protected static final int GRID_SIZE = 64;

    public FXVisSparkle(ClientLevel level, double x, double y, double z, 
                        double targetX, double targetY, double targetZ) {
        super(level, x, y, z, 0, 0, 0);

        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;

        // Gray starting color
        this.rCol = 0.6f;
        this.gCol = 0.6f;
        this.bCol = 0.6f;

        this.quadSize = 0.0f;
        this.lifetime = 1000;

        // Random initial velocity
        float f3 = 0.01f;
        this.xd = (float) this.random.nextGaussian() * f3;
        this.yd = (float) this.random.nextGaussian() * f3;
        this.zd = (float) this.random.nextGaussian() * f3;

        this.sizeMod = 45 + this.random.nextInt(15);

        // Green tinted color
        this.rCol = 0.2f;
        this.gCol = 0.6f + this.random.nextFloat() * 0.3f;
        this.bCol = 0.2f;

        this.gravity = 0.2f;
    }

    /**
     * Set sprite index from a linear index in the 64x64 grid
     */
    public void setParticleTextureIndex(int index) {
        if (index < 0) index = 0;
        this.spriteIndexX = index % GRID_SIZE;
        this.spriteIndexY = index / GRID_SIZE;
    }

    @Override
    protected float getU0() {
        return (float) spriteIndexX / (float) GRID_SIZE;
    }

    @Override
    protected float getU1() {
        return ((float) spriteIndexX + 1.0f) / (float) GRID_SIZE;
    }

    @Override
    protected float getV0() {
        return (float) spriteIndexY / (float) GRID_SIZE;
    }

    @Override
    protected float getV1() {
        return ((float) spriteIndexY + 1.0f) / (float) GRID_SIZE;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        // Move
        this.move(this.xd, this.yd, this.zd);

        // Apply friction
        this.xd *= 0.985;
        this.yd *= 0.985;
        this.zd *= 0.985;

        // Calculate direction to target
        double dx = targetX - this.x;
        double dy = targetY - this.y;
        double dz = targetZ - this.z;

        double accel = 0.1;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        // Shrink when close to target
        if (distance < 2.0) {
            this.quadSize *= 0.95f;
        }

        // Die when very close
        if (distance < 0.2) {
            this.lifetime = this.age;
        }

        // Grow during first few ticks
        if (this.age < 10) {
            this.quadSize = this.age / this.sizeMod;
        }

        // Normalize direction and apply acceleration
        if (distance > 0) {
            dx /= distance;
            dy /= distance;
            dz /= distance;

            this.xd += dx * accel;
            this.yd += dy * accel;
            this.zd += dz * accel;

            // Clamp velocity
            this.xd = Mth.clamp(this.xd, -0.1, 0.1);
            this.yd = Mth.clamp(this.yd, -0.1, 0.1);
            this.zd = Mth.clamp(this.zd, -0.1, 0.1);
        }

        if (this.age++ >= this.lifetime) {
            this.remove();
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        // Bobbing effect
        float bob = Mth.sin(this.age / 3.0f) * 0.3f + 6.0f;

        Vec3 cameraPos = camera.getPosition();
        float x = (float) (Mth.lerp(partialTicks, this.xo, this.x) - cameraPos.x());
        float y = (float) (Mth.lerp(partialTicks, this.yo, this.y) - cameraPos.y());
        float z = (float) (Mth.lerp(partialTicks, this.zo, this.z) - cameraPos.z());

        float size = 0.1f * this.quadSize * bob;

        // Animated sprite (16 frames)
        int frame = this.age % 16;
        float u0 = frame / 64.0f;
        float u1 = u0 + 0.015625f;
        float v0 = 0.125f;
        float v1 = v0 + 0.015625f;

        int light = 0xF000F0; // Full brightness

        Quaternionf quaternion = camera.rotation();

        Vector3f[] vertices = new Vector3f[]{
                new Vector3f(-1.0F, -1.0F, 0.0F),
                new Vector3f(-1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, -1.0F, 0.0F)
        };

        for (int i = 0; i < 4; ++i) {
            Vector3f vertex = vertices[i];
            vertex.rotate(quaternion);
            vertex.mul(size);
            vertex.add(x, y, z);
        }

        buffer.addVertex(vertices[0].x(), vertices[0].y(), vertices[0].z())
                .setUv(u1, v1).setColor(this.rCol, this.gCol, this.bCol, 0.5f)
                .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
        buffer.addVertex(vertices[1].x(), vertices[1].y(), vertices[1].z())
                .setUv(u1, v0).setColor(this.rCol, this.gCol, this.bCol, 0.5f)
                .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
        buffer.addVertex(vertices[2].x(), vertices[2].y(), vertices[2].z())
                .setUv(u0, v0).setColor(this.rCol, this.gCol, this.bCol, 0.5f)
                .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
        buffer.addVertex(vertices[3].x(), vertices[3].y(), vertices[3].z())
                .setUv(u0, v1).setColor(this.rCol, this.gCol, this.bCol, 0.5f)
                .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0; // Full brightness
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    // ==================== Configuration Methods ====================

    @Override
    public void setColor(float r, float g, float b) {
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
    }

    public void setGravity(float gravity) {
        this.gravity = gravity;
    }

    public void setTarget(double x, double y, double z) {
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
    }
}




