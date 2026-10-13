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
 * FXBoreSparkle - Sparkling particles that fly from blocks to the arcane bore.
 * Creates a trail of glowing green particles that home toward the target.
 * 
 * Ported from 1.12.2
 */
@OnlyIn(Dist.CLIENT)
public class FXBoreSparkle extends TextureSheetParticle {
    
    private Entity target;
    private double targetX;
    private double targetY;
    private double targetZ;
    private int particle = 24;
    
    /**
     * Create a bore sparkle particle with fixed target position.
     */
    public FXBoreSparkle(ClientLevel level, double x, double y, double z, 
                         double tx, double ty, double tz) {
        super(level, x, y, z, 0, 0, 0);
        
        // Green color with variation
        this.rCol = 0.2f;
        this.gCol = 0.6f + random.nextFloat() * 0.3f;
        this.bCol = 0.2f;
        
        this.quadSize = random.nextFloat() * 0.5f + 0.5f;
        
        this.targetX = tx;
        this.targetY = ty;
        this.targetZ = tz;
        
        // Calculate lifetime based on distance
        double dx = tx - x;
        double dy = ty - y;
        double dz = tz - z;
        int base = (int)(Mth.sqrt((float)(dx * dx + dy * dy + dz * dz)) * 10.0f);
        if (base < 1) base = 1;
        this.lifetime = base / 2 + random.nextInt(base);
        
        // Small random initial velocity
        float f3 = 0.01f;
        this.xd = (float)random.nextGaussian() * f3;
        this.yd = (float)random.nextGaussian() * f3;
        this.zd = (float)random.nextGaussian() * f3;
        
        this.gravity = 0.2f;
        
        // Distance culling
        Minecraft mc = Minecraft.getInstance();
        int visibleDistance = mc.options.graphicsMode().get().getId() > 0 ? 64 : 32;
        if (mc.cameraEntity != null && mc.cameraEntity.distanceToSqr(x, y, z) > visibleDistance * visibleDistance) {
            this.lifetime = 0;
        }
    }
    
    /**
     * Create a bore sparkle particle targeting an entity.
     */
    public FXBoreSparkle(ClientLevel level, double x, double y, double z, Entity target) {
        this(level, x, y, z, target.getX(), target.getY() + target.getEyeHeight(), target.getZ());
        this.target = target;
    }
    
    @Override
    public void tick() {
        this.xo = x;
        this.yo = y;
        this.zo = z;
        
        // Update target position if tracking entity
        if (target != null) {
            targetX = target.getX();
            targetY = target.getY() + target.getEyeHeight();
            targetZ = target.getZ();
        }
        
        // Check if reached target or expired
        if (this.age++ >= this.lifetime || 
            (Mth.floor(x) == Mth.floor(targetX) && 
             Mth.floor(y) == Mth.floor(targetY) && 
             Mth.floor(z) == Mth.floor(targetZ))) {
            this.remove();
            return;
        }
        
        // Apply velocity
        move(xd, yd, zd);
        
        // Dampen motion
        xd *= 0.985;
        yd *= 0.95;
        zd *= 0.985;
        
        // Home toward target
        double dx = targetX - x;
        double dy = targetY - y;
        double dz = targetZ - z;
        double dist = Mth.sqrt((float)(dx * dx + dy * dy + dz * dz));
        double clamp = Math.min(0.25, dist / 15.0);
        
        // Shrink when close
        if (dist < 2.0) {
            quadSize *= 0.9f;
        }
        
        // Normalize direction
        dx /= dist;
        dy /= dist;
        dz /= dist;
        
        // Accelerate toward target
        xd += dx * clamp;
        yd += dy * clamp;
        zd += dz * clamp;
        
        // Clamp max speed
        xd = Mth.clamp((float)xd, (float)-clamp, (float)clamp);
        yd = Mth.clamp((float)yd, (float)-clamp, (float)clamp);
        zd = Mth.clamp((float)zd, (float)-clamp, (float)clamp);
        
        // Add some random wobble
        xd += random.nextGaussian() * 0.01;
        yd += random.nextGaussian() * 0.01;
        zd += random.nextGaussian() * 0.01;
    }
    
    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        Vec3 camPos = camera.getPosition();
        float px = (float)(Mth.lerp(partialTicks, xo, x) - camPos.x());
        float py = (float)(Mth.lerp(partialTicks, yo, y) - camPos.y());
        float pz = (float)(Mth.lerp(partialTicks, zo, z) - camPos.z());
        
        // Pulsing size
        float bob = Mth.sin(age / 3.0f) * 0.5f + 1.0f;
        float size = 0.1f * quadSize * bob;
        
        // Billboard quad
        Quaternionf rotation = camera.rotation();
        Vector3f[] vertices = new Vector3f[] {
            new Vector3f(-1.0f, -1.0f, 0.0f),
            new Vector3f(-1.0f, 1.0f, 0.0f),
            new Vector3f(1.0f, 1.0f, 0.0f),
            new Vector3f(1.0f, -1.0f, 0.0f)
        };
        
        for (Vector3f vertex : vertices) {
            vertex.rotate(rotation);
            vertex.mul(size);
            vertex.add(px, py, pz);
        }
        
        // Animated sprite (4 frames)
        int frame = age % 4;
        float u0 = frame / 64.0f;
        float u1 = u0 + 0.015625f;
        float v0 = 0.0625f;  // Row 4 of the 64x64 grid
        float v1 = v0 + 0.015625f;
        
        // Full brightness
        int light = 0xF000F0;
        
        buffer.addVertex(vertices[0].x(), vertices[0].y(), vertices[0].z())
                .setUv(u1, v1).setColor(rCol, gCol, bCol, 1.0f).setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
        buffer.addVertex(vertices[1].x(), vertices[1].y(), vertices[1].z())
                .setUv(u1, v0).setColor(rCol, gCol, bCol, 1.0f).setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
        buffer.addVertex(vertices[2].x(), vertices[2].y(), vertices[2].z())
                .setUv(u0, v0).setColor(rCol, gCol, bCol, 1.0f).setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
        buffer.addVertex(vertices[3].x(), vertices[3].y(), vertices[3].z())
                .setUv(u0, v1).setColor(rCol, gCol, bCol, 1.0f).setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
    }
    
    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_LIT;
    }
    
    public void setGravity(float value) {
        this.gravity = value;
    }
}




