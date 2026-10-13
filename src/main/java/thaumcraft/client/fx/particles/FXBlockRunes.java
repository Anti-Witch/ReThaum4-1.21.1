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
 * FXBlockRunes - Magical rune particles that appear on blocks.
 * Used for ward effects, magical barriers, and enchantment visuals.
 */
@OnlyIn(Dist.CLIENT)
public class FXBlockRunes extends TextureSheetParticle {

    protected double offsetX;
    protected double offsetY;
    protected float rotation;
    protected int runeIndex;

    // Sprite tracking (64x64 grid)
    protected static final int GRID_SIZE = 64;

    public FXBlockRunes(ClientLevel level, double x, double y, double z, 
                        float r, float g, float b, int duration) {
        super(level, x, y, z, 0, 0, 0);

        this.offsetX = 0.0;
        this.offsetY = 0.0;
        this.rotation = 0.0f;
        this.runeIndex = 0;

        // Ensure non-zero color
        if (r == 0.0f) r = 1.0f;

        // Random 90-degree rotation
        this.rotation = this.random.nextInt(4) * 90.0f;

        this.rCol = r;
        this.gCol = g;
        this.bCol = b;

        this.gravity = 0.0f;
        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;

        this.lifetime = 3 * duration;
        this.setSize(0.01f, 0.01f);

        // Random rune index (224-240 range in sprite sheet)
        this.runeIndex = (int) (Math.random() * 16.0 + 224.0);

        // Random offsets for position variation
        this.offsetX = this.random.nextFloat() * 0.2;
        this.offsetY = -0.3 + this.random.nextFloat() * 0.6;

        this.quadSize = (float) (1.0 + this.random.nextGaussian() * 0.1);
        this.alpha = 0.0f;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        // Calculate alpha based on age
        float threshold = this.lifetime / 5.0f;
        if (this.age <= threshold) {
            this.alpha = this.age / threshold;
        } else {
            this.alpha = (this.lifetime - this.age) / (float) this.lifetime;
        }

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        // Apply gravity and movement
        this.yd -= 0.04 * this.gravity;
        this.x += this.xd;
        this.y += this.yd;
        this.z += this.zd;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        Vec3 cameraPos = camera.getPosition();
        float x = (float) (Mth.lerp(partialTicks, this.xo, this.x) - cameraPos.x());
        float y = (float) (Mth.lerp(partialTicks, this.yo, this.y) - cameraPos.y());
        float z = (float) (Mth.lerp(partialTicks, this.zo, this.z) - cameraPos.z());

        // Calculate UV coordinates for the rune sprite
        float u0 = (runeIndex % 16) / 64.0f;
        float u1 = u0 + 0.015625f;
        float v0 = 0.09375f; // Row 6 in sprite sheet
        float v1 = v0 + 0.015625f;

        float size = 0.3f * this.quadSize;
        float displayAlpha = this.alpha / 2.0f;

        int light = 0xF000F0; // Full brightness for runes

        // Create rotation quaternion for the rune orientation
        Quaternionf quaternion = new Quaternionf();
        quaternion.rotateY((float) Math.toRadians(rotation));
        quaternion.rotateZ((float) Math.toRadians(90.0f));

        // Apply offset
        float offsetXf = (float) this.offsetX;
        float offsetYf = (float) this.offsetY;

        Vector3f[] vertices = new Vector3f[]{
                new Vector3f(-0.5f * size, 0.5f * size, 0.0f),
                new Vector3f(0.5f * size, 0.5f * size, 0.0f),
                new Vector3f(0.5f * size, -0.5f * size, 0.0f),
                new Vector3f(-0.5f * size, -0.5f * size, 0.0f)
        };

        for (int i = 0; i < 4; ++i) {
            Vector3f vertex = vertices[i];
            vertex.rotate(quaternion);
            vertex.add(x + offsetXf, y + offsetYf, z - 0.51f);
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
    public int getLightColor(float partialTick) {
        return 0xF000F0; // Full brightness
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    // ==================== Configuration Methods ====================

    public void setScale(float scale) {
        this.quadSize = scale;
    }

    public void setOffsetX(double offset) {
        this.offsetX = offset;
    }

    public void setGravity(float gravity) {
        this.gravity = gravity;
    }
}




