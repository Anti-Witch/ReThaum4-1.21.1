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

/**
 * FXArc - Lightning arc effect between two points.
 * Creates a jagged beam with spark particles along its path.
 * Used for shock focus, lightning effects, and electrical discharges.
 */
@OnlyIn(Dist.CLIENT)
public class FXArc extends TextureSheetParticle {

    protected static final ResourceLocation BEAM_TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/beamh.png");

    protected List<Vec3> points = new ArrayList<>();
    protected double targetX;
    protected double targetY;
    protected double targetZ;
    protected float length;

    public FXArc(ClientLevel level, double x, double y, double z,
                 double targetX, double targetY, double targetZ,
                 float r, float g, float b, double heightGravity) {
        super(level, x, y, z, 0, 0, 0);

        this.rCol = r;
        this.gCol = g;
        this.bCol = b;

        this.setSize(0.02f, 0.02f);
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;

        // Store relative target position
        this.targetX = targetX - x;
        this.targetY = targetY - y;
        this.targetZ = targetZ - z;

        this.lifetime = 3;

        // Calculate arc path
        calculateArcPoints(heightGravity);
    }

    /**
     * Calculate the points along the arc path with noise for jagged effect
     */
    protected void calculateArcPoints(double heightGravity) {
        Vec3 start = Vec3.ZERO;
        Vec3 end = new Vec3(this.targetX, this.targetY, this.targetZ);

        this.length = (float) end.length();

        // Calculate velocity needed to reach target with gravity
        double gravity = 0.115;
        double noise = 0.25;

        Vec3 velocity = calculateVelocity(start, end, heightGravity, gravity);
        double stepLengthSq = velocity.lengthSqr();

        Vec3 current = start;
        points.add(start);

        // Generate points along the arc
        for (int i = 0; i < 50 && current.distanceToSqr(end) > stepLengthSq; i++) {
            Vec3 next = current.add(velocity);
            current = next;

            // Add noise for jagged appearance
            Vec3 noisyPoint = next.add(
                    (this.random.nextDouble() - this.random.nextDouble()) * noise,
                    (this.random.nextDouble() - this.random.nextDouble()) * noise,
                    (this.random.nextDouble() - this.random.nextDouble()) * noise
            );
            points.add(noisyPoint);

            // Apply gravity to velocity
            velocity = velocity.subtract(0, gravity / 1.9, 0);
        }

        points.add(end);
    }

    /**
     * Calculate initial velocity to reach target with given gravity
     */
    protected Vec3 calculateVelocity(Vec3 start, Vec3 end, double height, double gravity) {
        double dx = end.x - start.x;
        double dy = end.y - start.y;
        double dz = end.z - start.z;

        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        // Time to reach target
        double time = horizontalDist / 0.5;
        if (time < 1) time = 1;

        // Initial velocity
        double vx = dx / time;
        double vz = dz / time;
        double vy = (dy + 0.5 * gravity * time * time) / time + height;

        return new Vec3(vx, vy, vz);
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
        // Arc uses custom rendering with its own texture
        // We need to render this separately from the standard particle batch
        // For now, we'll render using the standard particle system with electric spark approximation

        if (points.size() < 2) return;

        Vec3 cameraPos = camera.getPosition();
        double px = Mth.lerp(partialTicks, this.xo, this.x) - cameraPos.x();
        double py = Mth.lerp(partialTicks, this.yo, this.y) - cameraPos.y();
        double pz = Mth.lerp(partialTicks, this.zo, this.z) - cameraPos.z();

        float alpha = 1.0f - (this.age + partialTicks) / this.lifetime;
        float size = 0.125f;

        int light = 0xF000F0; // Full brightness

        // Render line segments between points
        for (int i = 0; i < points.size() - 1; i++) {
            Vec3 p1 = points.get(i);
            Vec3 p2 = points.get(i + 1);

            float x1 = (float) (px + p1.x);
            float y1 = (float) (py + p1.y);
            float z1 = (float) (pz + p1.z);

            float x2 = (float) (px + p2.x);
            float y2 = (float) (py + p2.y);
            float z2 = (float) (pz + p2.z);

            // Midpoint for the quad
            float mx = (x1 + x2) / 2;
            float my = (y1 + y2) / 2;
            float mz = (z1 + z2) / 2;

            // Simple sprite at midpoint
            float u = (i % 16) / 64.0f;
            renderSegmentQuad(buffer, camera, mx, my, mz, size, u, alpha, light);
        }
    }

    protected void renderSegmentQuad(VertexConsumer buffer, Camera camera, 
                                      float x, float y, float z, float size,
                                      float u, float alpha, int light) {
        float u0 = u;
        float u1 = u + 0.015625f;
        float v0 = 0.0f;
        float v1 = 0.015625f;

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

    public List<Vec3> getPoints() {
        return points;
    }

    public float getLength() {
        return length;
    }
}




