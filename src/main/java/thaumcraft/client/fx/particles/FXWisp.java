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

/**
 * FXWisp - Wispy trailing particle that follows an entity.
 * Used for eldritch guardian effects and other ghostly visuals.
 */
@OnlyIn(Dist.CLIENT)
public class FXWisp extends TextureSheetParticle {

    protected Entity target;
    protected int blendMode = 1;

    // Sprite tracking
    protected int spriteIndexX = 0;
    protected int spriteIndexY = 0;
    protected static final int GRID_SIZE = 64;

    public FXWisp(ClientLevel level, double x, double y, double z, Entity target) {
        super(level, x, y, z, 0, 0, 0);

        this.target = target;

        // Random initial velocity
        this.xd = this.random.nextGaussian() * 0.03;
        this.yd = -0.05;
        this.zd = this.random.nextGaussian() * 0.03;

        this.quadSize *= 0.4f;
        this.lifetime = (int) (40.0 / (Math.random() * 0.3 + 0.7));

        this.setSize(0.01f, 0.01f);

        this.blendMode = 771;

        // Dark wispy color
        this.rCol = this.random.nextFloat() * 0.05f;
        this.gCol = this.random.nextFloat() * 0.05f;
        this.bCol = this.random.nextFloat() * 0.05f;
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

        // Follow target entity if present
        if (target != null && !this.onGround) {
            this.x += target.getDeltaMovement().x();
            this.z += target.getDeltaMovement().z();
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        Entity viewEntity = Minecraft.getInstance().getCameraEntity();
        if (viewEntity == null) return;

        // Calculate age-based alpha scale
        float ageScale = 1.0f - (float) this.age / (float) this.lifetime;

        // Distance-based fade
        float maxDist = 1024.0f;
        double dist = viewEntity.position().distanceToSqr(this.x, this.y, this.z);
        float distFade = (float) (1.0 - Math.min(maxDist, dist) / maxDist);

        float finalAlpha = 0.2f * ageScale * distFade;
        if (finalAlpha <= 0) return;

        Vec3 cameraPos = camera.getPosition();
        float x = (float) (Mth.lerp(partialTicks, this.xo, this.x) - cameraPos.x());
        float y = (float) (Mth.lerp(partialTicks, this.yo, this.y) - cameraPos.y());
        float z = (float) (Mth.lerp(partialTicks, this.zo, this.z) - cameraPos.z());

        float size = 0.5f * this.quadSize;

        // Animated sprite (13 frames)
        int frame = this.age % 13;
        float u0 = frame / 64.0f;
        float u1 = u0 + 0.015625f;
        float v0 = 0.046875f;
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
                .setUv(u1, v1).setColor(this.rCol, this.gCol, this.bCol, finalAlpha)
                .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
        buffer.addVertex(vertices[1].x(), vertices[1].y(), vertices[1].z())
                .setUv(u1, v0).setColor(this.rCol, this.gCol, this.bCol, finalAlpha)
                .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
        buffer.addVertex(vertices[2].x(), vertices[2].y(), vertices[2].z())
                .setUv(u0, v0).setColor(this.rCol, this.gCol, this.bCol, finalAlpha)
                .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
        buffer.addVertex(vertices[3].x(), vertices[3].y(), vertices[3].z())
                .setUv(u0, v1).setColor(this.rCol, this.gCol, this.bCol, finalAlpha)
                .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0; // Full brightness
    }

    @Override
    public ParticleRenderType getRenderType() {
        return blendMode != 1 ? ParticleRenderType.PARTICLE_SHEET_LIT : ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    // ==================== Configuration Methods ====================

    @Override
    public void setColor(float r, float g, float b) {
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
    }

    public void setScale(float scale) {
        this.quadSize = scale;
    }

    public void setMaxAge(int maxAge) {
        this.lifetime = maxAge;
    }

    public void setBlendMode(int mode) {
        this.blendMode = mode;
    }
}




