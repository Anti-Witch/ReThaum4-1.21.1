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
 * FXFireMote - Fire/flame particle used for alumentum, nitor, and fire effects.
 * Features scale fade-out and rotation animation.
 */
@OnlyIn(Dist.CLIENT)
public class FXFireMote extends TextureSheetParticle {

    protected float baseScale;
    protected float baseAlpha;
    protected int glowLayer;

    // Sprite index tracking (64x64 grid)
    protected int spriteIndexX = 0;
    protected int spriteIndexY = 0;
    protected static final int GRID_SIZE = 64;

    public FXFireMote(ClientLevel level, double x, double y, double z, 
                      double vx, double vy, double vz, 
                      float r, float g, float b, float scale, int layer) {
        super(level, x, y, z, 0, 0, 0);

        // Handle colors > 1 as 0-255 range
        float colorR = r;
        float colorG = g;
        float colorB = b;
        if (colorR > 1.0f) colorR /= 255.0f;
        if (colorG > 1.0f) colorG /= 255.0f;
        if (colorB > 1.0f) colorB /= 255.0f;

        this.rCol = colorR;
        this.gCol = colorG;
        this.bCol = colorB;

        this.glowLayer = layer;
        this.lifetime = 16;
        this.quadSize = scale;
        this.baseScale = scale;
        this.baseAlpha = 1.0f;

        this.xd = vx;
        this.yd = vy;
        this.zd = vz;

        // Initial rotation
        this.roll = (float) (Math.PI * 2);

        // Default fire sprite (index 7 in the grid)
        setParticleTextureIndex(7);
    }

    /**
     * Simplified constructor with default layer
     */
    public FXFireMote(ClientLevel level, double x, double y, double z,
                      double vx, double vy, double vz,
                      float r, float g, float b, float scale) {
        this(level, x, y, z, vx, vy, vz, r, g, b, scale, 0);
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
        super.tick();

        // Random chance to age faster (flickering effect)
        if (this.random.nextInt(6) == 0) {
            this.age++;
        }

        if (this.age >= this.lifetime) {
            this.remove();
            return;
        }

        // Calculate lifespan progress
        float lifespan = (float) this.age / (float) this.lifetime;

        // Shrink as it dies
        this.quadSize = baseScale - baseScale * lifespan;

        // Fade out
        this.baseAlpha = 1.0f - lifespan;

        // Rotate
        this.oRoll = this.roll;
        this.roll += 1.0f;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        Vec3 cameraPos = camera.getPosition();
        float x = (float) (Mth.lerp(partialTicks, this.xo, this.x) - cameraPos.x());
        float y = (float) (Mth.lerp(partialTicks, this.yo, this.y) - cameraPos.y());
        float z = (float) (Mth.lerp(partialTicks, this.zo, this.z) - cameraPos.z());

        float size = this.getQuadSize(partialTicks);

        // Get UV coordinates
        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();

        int light = this.getLightColor(partialTicks);

        // Calculate rotation
        Quaternionf quaternion;
        if (this.roll == 0.0F) {
            quaternion = camera.rotation();
        } else {
            quaternion = new Quaternionf(camera.rotation());
            float rollAngle = Mth.lerp(partialTicks, this.oRoll, this.roll);
            quaternion.rotateZ(rollAngle);
        }

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

        float alpha = this.alpha * this.baseAlpha;

        buffer.addVertex(vertices[0].x(), vertices[0].y(), vertices[0].z())
                .setUv(u1, v1).setColor(this.rCol, this.gCol, this.bCol, alpha)
                .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
        buffer.addVertex(vertices[1].x(), vertices[1].y(), vertices[1].z())
                .setUv(u1, v0).setColor(this.rCol, this.gCol, this.bCol, alpha)
                .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
        buffer.addVertex(vertices[2].x(), vertices[2].y(), vertices[2].z())
                .setUv(u0, v0).setColor(this.rCol, this.gCol, this.bCol, alpha)
                .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
        buffer.addVertex(vertices[3].x(), vertices[3].y(), vertices[3].z())
                .setUv(u0, v1).setColor(this.rCol, this.gCol, this.bCol, alpha)
                .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
    }

    @Override
    public int getLightColor(float partialTick) {
        // Fire motes are self-illuminating
        return 0xF000F0; // Full brightness
    }

    @Override
    public ParticleRenderType getRenderType() {
        return glowLayer == 0 ? ThaumcraftParticleRenderTypes.THAUMCRAFT_PARTICLE_SHEET_TRANSLUCENT : ThaumcraftParticleRenderTypes.THAUMCRAFT_PARTICLE_SHEET_LIT;
    }

    // ==================== Configuration Methods ====================

    @Override
    public void setColor(float r, float g, float b) {
        if (r > 1.0f) r /= 255.0f;
        if (g > 1.0f) g /= 255.0f;
        if (b > 1.0f) b /= 255.0f;
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
    }

    public void setScale(float scale) {
        this.quadSize = scale;
        this.baseScale = scale;
    }

    public void setMaxAge(int maxAge) {
        this.lifetime = maxAge;
    }

    @Override
    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    public void setLayer(int layer) {
        this.glowLayer = layer;
    }

    public void setGravity(float gravity) {
        this.gravity = gravity;
    }
}




