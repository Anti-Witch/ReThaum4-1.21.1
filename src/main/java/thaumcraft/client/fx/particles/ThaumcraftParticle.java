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
 * Base class for custom Thaumcraft particles.
 * Supports color interpolation, scale animation, rotation, and custom rendering.
 */
@OnlyIn(Dist.CLIENT)
public class ThaumcraftParticle extends TextureSheetParticle {

    // Color interpolation
    protected float startR, startG, startB;
    protected float endR, endG, endB;
    
    // Scale animation
    protected float startScale;
    protected float endScale;
    
    // Rotation
    protected float rotationSpeed;
    protected float rotation;
    
    // Physics
    protected double slowDown = 0.98;
    protected float windX, windZ;
    protected boolean noClip = false;
    
    // Sprite animation
    protected int spriteStart = 0;
    protected int spriteCount = 1;
    protected int spriteIncrement = 1;
    protected boolean spriteLoop = false;
    protected int gridSize = 64;
    
    // Render layer (0 = normal, 1 = additive)
    protected int layer = 0;

    public ThaumcraftParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        this.startR = this.rCol;
        this.startG = this.gCol;
        this.startB = this.bCol;
        this.endR = this.rCol;
        this.endG = this.gCol;
        this.endB = this.bCol;
        this.startScale = this.quadSize;
        this.endScale = this.quadSize;
    }

    public ThaumcraftParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
        super(level, x, y, z, vx, vy, vz);
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.startR = this.rCol;
        this.startG = this.gCol;
        this.startB = this.bCol;
        this.endR = this.rCol;
        this.endG = this.gCol;
        this.endB = this.bCol;
        this.startScale = this.quadSize;
        this.endScale = this.quadSize;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        // Update rotation
        this.oRoll = this.roll;
        this.roll += this.rotationSpeed;

        // Apply gravity
        this.yd -= 0.04 * this.gravity;

        // Move
        if (!noClip) {
            this.move(this.xd, this.yd, this.zd);
        } else {
            this.x += this.xd;
            this.y += this.yd;
            this.z += this.zd;
        }

        // Apply friction/slowdown
        this.xd *= this.slowDown;
        this.yd *= this.slowDown;
        this.zd *= this.slowDown;

        // Apply wind
        this.xd += this.windX;
        this.zd += this.windZ;

        // Ground friction
        if (this.onGround && slowDown != 1.0) {
            this.xd *= 0.7;
            this.zd *= 0.7;
        }

        // Update color interpolation
        float progress = (float) this.age / (float) this.lifetime;
        this.rCol = Mth.lerp(progress, this.startR, this.endR);
        this.gCol = Mth.lerp(progress, this.startG, this.endG);
        this.bCol = Mth.lerp(progress, this.startB, this.endB);

        // Update scale interpolation
        this.quadSize = Mth.lerp(progress, this.startScale, this.endScale);
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        Vec3 cameraPos = camera.getPosition();
        float x = (float) (Mth.lerp(partialTicks, this.xo, this.x) - cameraPos.x());
        float y = (float) (Mth.lerp(partialTicks, this.yo, this.y) - cameraPos.y());
        float z = (float) (Mth.lerp(partialTicks, this.zo, this.z) - cameraPos.z());

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
        float size = this.getQuadSize(partialTicks);

        for (int i = 0; i < 4; ++i) {
            Vector3f vertex = vertices[i];
            vertex.rotate(quaternion);
            vertex.mul(size);
            vertex.add(x, y, z);
        }

        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();
        int light = this.getLightColor(partialTicks);

        buffer.addVertex(vertices[0].x(), vertices[0].y(), vertices[0].z())
                .setUv(u1, v1).setColor(this.rCol, this.gCol, this.bCol, this.alpha)
                .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
        buffer.addVertex(vertices[1].x(), vertices[1].y(), vertices[1].z())
                .setUv(u1, v0).setColor(this.rCol, this.gCol, this.bCol, this.alpha)
                .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
        buffer.addVertex(vertices[2].x(), vertices[2].y(), vertices[2].z())
                .setUv(u0, v0).setColor(this.rCol, this.gCol, this.bCol, this.alpha)
                .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
        buffer.addVertex(vertices[3].x(), vertices[3].y(), vertices[3].z())
                .setUv(u0, v1).setColor(this.rCol, this.gCol, this.bCol, this.alpha)
                .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return layer == 0 ? ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT : ParticleRenderType.PARTICLE_SHEET_LIT;
    }

    // ==================== Configuration Methods ====================

    public ThaumcraftParticle setTCColor(float r, float g, float b) {
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
        this.startR = r;
        this.startG = g;
        this.startB = b;
        this.endR = r;
        this.endG = g;
        this.endB = b;
        return this;
    }

    public ThaumcraftParticle setColor(float r1, float g1, float b1, float r2, float g2, float b2) {
        this.rCol = r1;
        this.gCol = g1;
        this.bCol = b1;
        this.startR = r1;
        this.startG = g1;
        this.startB = b1;
        this.endR = r2;
        this.endG = g2;
        this.endB = b2;
        return this;
    }

    public ThaumcraftParticle setTCAlpha(float alpha) {
        this.alpha = alpha;
        return this;
    }

    public ThaumcraftParticle setTCAlphaRange(float startAlpha, float endAlpha) {
        this.alpha = startAlpha;
        // Note: For proper alpha interpolation, override tick() or use custom logic
        return this;
    }

    public ThaumcraftParticle setTCScale(float scale) {
        this.quadSize = scale;
        this.startScale = scale;
        this.endScale = scale;
        return this;
    }

    public ThaumcraftParticle setTCScaleRange(float startScale, float endScale) {
        this.quadSize = startScale;
        this.startScale = startScale;
        this.endScale = endScale;
        return this;
    }

    public ThaumcraftParticle setTCLifetime(int lifetime) {
        this.lifetime = lifetime;
        return this;
    }

    public ThaumcraftParticle setTCGravity(float gravity) {
        this.gravity = gravity;
        return this;
    }

    public ThaumcraftParticle setTCRotationSpeed(float speed) {
        this.rotationSpeed = speed;
        return this;
    }

    public ThaumcraftParticle setTCSlowDown(double slowDown) {
        this.slowDown = slowDown;
        return this;
    }

    public ThaumcraftParticle setTCNoClip(boolean noClip) {
        this.noClip = noClip;
        return this;
    }

    public ThaumcraftParticle setTCLayer(int layer) {
        this.layer = layer;
        return this;
    }

    public ThaumcraftParticle setTCWind(float windX, float windZ) {
        this.windX = windX;
        this.windZ = windZ;
        return this;
    }
}




