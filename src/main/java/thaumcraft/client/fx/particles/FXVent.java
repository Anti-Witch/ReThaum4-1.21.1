package thaumcraft.client.fx.particles;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.awt.Color;

/**
 * FXVent - Smoke/steam particle for essentia vents and similar effects.
 * Grows in size then fades out, with slight upward drift.
 */
@OnlyIn(Dist.CLIENT)
public class FXVent extends TextureSheetParticle {

    protected float targetScale;
    protected static final int GRID_SIZE = 64;

    public FXVent(ClientLevel level, double x, double y, double z, 
                  double vx, double vy, double vz, int color) {
        super(level, x, y, z, vx, vy, vz);

        this.targetScale = 1.0f;
        this.setSize(0.02f, 0.02f);
        this.quadSize = this.random.nextFloat() * 0.1f + 0.05f;

        this.xd = vx;
        this.yd = vy;
        this.zd = vz;

        // Extract color from int
        Color c = new Color(color);
        this.rCol = c.getRed() / 255.0f;
        this.gCol = c.getGreen() / 255.0f;
        this.bCol = c.getBlue() / 255.0f;

        // Set heading with some randomness
        setHeading(vx, vy, vz, 0.125f, 5.0f);

        // Check distance for visibility culling
        Entity viewEntity = Minecraft.getInstance().getCameraEntity();
        int visibleDistance = Minecraft.getInstance().options.graphicsMode().get().getId() > 0 ? 50 : 25;
        if (viewEntity != null && viewEntity.distanceToSqr(x, y, z) > visibleDistance * visibleDistance) {
            this.lifetime = 0;
        }

        this.alpha = 1.0f;
    }

    /**
     * Set heading direction with spread
     */
    public void setHeading(double vx, double vy, double vz, float speed, float spread) {
        double length = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (length > 0) {
            vx /= length;
            vy /= length;
            vz /= length;
        }

        // Add randomness to direction
        vx += this.random.nextGaussian() * (this.random.nextBoolean() ? -1 : 1) * 0.0075 * spread;
        vy += this.random.nextGaussian() * (this.random.nextBoolean() ? -1 : 1) * 0.0075 * spread;
        vz += this.random.nextGaussian() * (this.random.nextBoolean() ? -1 : 1) * 0.0075 * spread;

        this.xd = vx * speed;
        this.yd = vy * speed;
        this.zd = vz * speed;
    }

    public void setScale(float scale) {
        this.quadSize *= scale;
        this.targetScale *= scale;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        this.age++;

        // Die when reached target scale
        if (this.quadSize >= this.targetScale) {
            this.remove();
            return;
        }

        // Slight upward drift
        this.yd += 0.0025;

        // Move
        this.move(this.xd, this.yd, this.zd);

        // Friction
        this.xd *= 0.85;
        this.yd *= 0.85;
        this.zd *= 0.85;

        // Grow
        if (this.quadSize < this.targetScale) {
            this.quadSize *= 1.15f;
        }
        if (this.quadSize > this.targetScale) {
            this.quadSize = this.targetScale;
        }

        // Ground friction
        if (this.onGround) {
            this.xd *= 0.7;
            this.zd *= 0.7;
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        Vec3 cameraPos = camera.getPosition();
        float x = (float) (Mth.lerp(partialTicks, this.xo, this.x) - cameraPos.x());
        float y = (float) (Mth.lerp(partialTicks, this.yo, this.y) - cameraPos.y());
        float z = (float) (Mth.lerp(partialTicks, this.zo, this.z) - cameraPos.z());

        // Animated sprite based on scale progress
        int spriteIndex = (int) (1.0f + this.quadSize / this.targetScale * 4.0f);
        float u0 = (spriteIndex % 16) / 64.0f;
        float u1 = u0 + 0.015625f;
        float v0 = (spriteIndex / 16) / 64.0f;
        float v1 = v0 + 0.015625f;

        float size = 0.3f * this.quadSize;
        
        // Alpha fades as it grows
        float displayAlpha = this.alpha * ((this.targetScale - this.quadSize) / this.targetScale);

        int light = this.getLightColor(partialTicks);

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
                .setUv(u1, v1).setColor(this.rCol, this.gCol, this.bCol, displayAlpha)
                .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
        buffer.addVertex(vertices[1].x(), vertices[1].y(), vertices[1].z())
                .setUv(u1, v0).setColor(this.rCol, this.gCol, this.bCol, displayAlpha)
                .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
        buffer.addVertex(vertices[2].x(), vertices[2].y(), vertices[2].z())
                .setUv(u0, v0).setColor(this.rCol, this.gCol, this.bCol, displayAlpha)
                .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
        buffer.addVertex(vertices[3].x(), vertices[3].y(), vertices[3].z())
                .setUv(u0, v1).setColor(this.rCol, this.gCol, this.bCol, displayAlpha)
                .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_LIT;
    }

    // ==================== Configuration Methods ====================

    public void setRGB(float r, float g, float b) {
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
    }
}




