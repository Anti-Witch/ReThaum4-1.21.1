package thaumcraft.client.fx.beams;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * FXBolt - Lightning bolt effect between two points.
 * Creates a wavy, animated beam with variable width along its path.
 * Used for flux effects, lightning, and magical beam attacks.
 */
@OnlyIn(Dist.CLIENT)
public class FXBolt extends TextureSheetParticle {

    protected static final ResourceLocation BEAM_TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/essentia.png");

    protected float beamWidth;
    protected List<Vec3> points = new ArrayList<>();
    protected List<Float> pointWidths = new ArrayList<>();
    protected float distanceRandom;
    protected long seed;

    protected double targetX;
    protected double targetY;
    protected double targetZ;
    protected float length;

    public FXBolt(ClientLevel level, double x, double y, double z,
                  double targetX, double targetY, double targetZ,
                  float r, float g, float b, float width) {
        super(level, x, y, z, 0, 0, 0);

        this.rCol = r;
        this.gCol = g;
        this.bCol = b;

        this.beamWidth = width;
        this.setSize(0.02f, 0.02f);
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;

        // Store relative target position
        this.targetX = targetX - x;
        this.targetY = targetY - y;
        this.targetZ = targetZ - z;

        this.lifetime = 3;

        // Calculate length
        Vec3 start = Vec3.ZERO;
        Vec3 end = new Vec3(this.targetX, this.targetY, this.targetZ);
        this.length = (float) (end.length() * Math.PI);

        // Random seed for consistent animation
        this.seed = this.random.nextInt(1000);
        this.distanceRandom = (float) (this.random.nextInt(50) * Math.PI);

        // Generate initial points
        calculatePoints(0);
    }

    /**
     * Calculate wavy points along the bolt path
     */
    protected void calculatePoints(float partialTicks) {
        Random rr = new Random(seed);
        points.clear();
        pointWidths.clear();

        Vec3 start = Vec3.ZERO;
        Vec3 end = new Vec3(this.targetX, this.targetY, this.targetZ);

        int steps = (int) this.length;
        if (steps < 2) steps = 2;

        // Start point
        points.add(start);
        pointWidths.add(this.beamWidth);

        // Amplitude increases over lifetime for a "dissolving" effect
        float amplitude = (this.age + partialTicks) / 10.0f;

        // Generate intermediate points with wave distortion
        for (int i = 1; i < steps - 1; i++) {
            float dist = i * (this.length / steps) + this.distanceRandom;

            // Wave distortion
            double dx = this.targetX / steps * i + Mth.sin(dist / 4.0f) * amplitude;
            double dy = this.targetY / steps * i + Mth.sin(dist / 3.0f) * amplitude;
            double dz = this.targetZ / steps * i + Mth.sin(dist / 2.0f) * amplitude;

            // Random jitter
            dx += (rr.nextFloat() - rr.nextFloat()) * 0.1f;
            dy += (rr.nextFloat() - rr.nextFloat()) * 0.1f;
            dz += (rr.nextFloat() - rr.nextFloat()) * 0.1f;

            points.add(new Vec3(dx, dy, dz));

            // Random width variation for electrical effect
            float widthMod = (rr.nextInt(4) == 0) ? (1.0f - this.age * 0.25f) : 1.0f;
            pointWidths.add(this.beamWidth * widthMod);
        }

        // End point
        points.add(end);
        pointWidths.add(this.beamWidth);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        // Recalculate points with animation
        calculatePoints(partialTicks);

        if (points.size() < 2) return;

        Vec3 cameraPos = camera.getPosition();
        double px = Mth.lerp(partialTicks, this.xo, this.x) - cameraPos.x();
        double py = Mth.lerp(partialTicks, this.yo, this.y) - cameraPos.y();
        double pz = Mth.lerp(partialTicks, this.zo, this.z) - cameraPos.z();

        float alpha = Mth.clamp(1.0f - this.age / (float) this.lifetime, 0.1f, 1.0f);
        int light = 0xF000F0; // Full brightness

        // Render each segment as a billboard quad
        for (int i = 0; i < points.size() - 1; i++) {
            Vec3 p1 = points.get(i);
            Vec3 p2 = points.get(i + 1);

            float width1 = pointWidths.get(i) / 10.0f;
            float width2 = pointWidths.get(i + 1) / 10.0f;
            float avgWidth = (width1 + width2) / 2.0f;

            // Midpoint of segment
            float mx = (float) (px + (p1.x + p2.x) / 2);
            float my = (float) (py + (p1.y + p2.y) / 2);
            float mz = (float) (pz + (p1.z + p2.z) / 2);

            // Segment length for UV
            float segmentU = i / (float) points.size();

            renderBoltSegment(buffer, camera, mx, my, mz, avgWidth, segmentU, alpha, light);
        }
    }

    protected void renderBoltSegment(VertexConsumer buffer, Camera camera,
                                      float x, float y, float z, float size,
                                      float u, float alpha, int light) {
        float u0 = u;
        float u1 = u + 0.03125f;
        float v0 = 0.0f;
        float v1 = 0.0625f;

        Vector3f[] vertices = new Vector3f[]{
                new Vector3f(-1.0F, -1.0F, 0.0F),
                new Vector3f(-1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, -1.0F, 0.0F)
        };

        for (int i = 0; i < 4; ++i) {
            Vector3f vertex = vertices[i];
            vertex.rotate(camera.rotation());
            vertex.mul(size);
            vertex.add(x, y, z);
        }

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
        return 0xF000F0; // Full brightness for lightning
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    // ==================== Configuration Methods ====================

    public void setRGB(float r, float g, float b) {
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
    }

    public void setWidth(float width) {
        this.beamWidth = width;
    }

    public List<Vec3> getPoints() {
        return points;
    }

    public float getLength() {
        return length;
    }
}




