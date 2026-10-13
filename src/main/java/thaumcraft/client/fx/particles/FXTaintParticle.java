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
 * FXTaintParticle - Purple/pink taint corruption particle.
 * Used for taint spread, taint creature effects, and corruption visuals.
 * 
 * Features:
 * - Purple/pink coloration with variation
 * - Swirling motion
 * - Fades out over time
 * - Multiple animation frames
 * 
 * Ported from 1.12.2
 */
@OnlyIn(Dist.CLIENT)
public class FXTaintParticle extends TextureSheetParticle {
    
    private float rotationAngle;
    private float rotationSpeed;
    private int spriteFrame;
    
    // Taint purple color range
    private static final float BASE_R = 0.6f;
    private static final float BASE_G = 0.1f;
    private static final float BASE_B = 0.8f;
    
    /**
     * Create a taint particle at the given position.
     */
    public FXTaintParticle(ClientLevel level, double x, double y, double z,
                           double vx, double vy, double vz, float scale) {
        super(level, x, y, z, vx, vy, vz);
        
        // Vary the purple color slightly
        this.rCol = BASE_R + (random.nextFloat() - 0.5f) * 0.2f;
        this.gCol = BASE_G + random.nextFloat() * 0.15f;
        this.bCol = BASE_B + (random.nextFloat() - 0.5f) * 0.15f;
        this.alpha = 0.8f;
        
        this.quadSize = scale * (0.5f + random.nextFloat() * 0.5f);
        
        this.xd = vx + (random.nextFloat() - 0.5f) * 0.02;
        this.yd = vy + random.nextFloat() * 0.02;
        this.zd = vz + (random.nextFloat() - 0.5f) * 0.02;
        
        this.gravity = 0.01f;
        
        this.lifetime = 15 + random.nextInt(15);
        
        this.rotationAngle = random.nextFloat() * 360.0f;
        this.rotationSpeed = (random.nextFloat() - 0.5f) * 10.0f;
        
        this.spriteFrame = random.nextInt(4);
    }
    
    /**
     * Create a taint particle with custom color.
     */
    public FXTaintParticle(ClientLevel level, double x, double y, double z,
                           double vx, double vy, double vz, float scale,
                           float r, float g, float b) {
        this(level, x, y, z, vx, vy, vz, scale);
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
    }
    
    @Override
    public void tick() {
        this.xo = x;
        this.yo = y;
        this.zo = z;
        
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        
        // Swirling motion
        rotationAngle += rotationSpeed;
        float swirl = Mth.sin(age * 0.3f) * 0.01f;
        xd += swirl;
        zd += Mth.cos(age * 0.3f) * 0.01f;
        
        // Apply velocity
        x += xd;
        y += yd;
        z += zd;
        
        // Apply gravity (slight upward float)
        yd -= gravity * 0.5;
        yd += 0.005;  // Slight upward drift
        
        // Dampen motion
        xd *= 0.95;
        yd *= 0.95;
        zd *= 0.95;
        
        // Fade out
        float lifeProgress = (float)age / lifetime;
        if (lifeProgress > 0.7f) {
            alpha = 0.8f * (1.0f - (lifeProgress - 0.7f) / 0.3f);
        }
        
        // Animate sprite
        if (age % 3 == 0) {
            spriteFrame = (spriteFrame + 1) % 4;
        }
    }
    
    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        Vec3 camPos = camera.getPosition();
        float px = (float)(Mth.lerp(partialTicks, xo, x) - camPos.x());
        float py = (float)(Mth.lerp(partialTicks, yo, y) - camPos.y());
        float pz = (float)(Mth.lerp(partialTicks, zo, z) - camPos.z());
        
        // Billboard quad with rotation
        Quaternionf rotation = camera.rotation();
        
        // Add spin rotation
        float currentRot = rotationAngle + rotationSpeed * partialTicks;
        Quaternionf spinRot = new Quaternionf().rotateZ((float)Math.toRadians(currentRot));
        rotation = rotation.mul(spinRot, new Quaternionf());
        
        Vector3f[] vertices = new Vector3f[] {
            new Vector3f(-1.0f, -1.0f, 0.0f),
            new Vector3f(-1.0f, 1.0f, 0.0f),
            new Vector3f(1.0f, 1.0f, 0.0f),
            new Vector3f(1.0f, -1.0f, 0.0f)
        };
        
        for (Vector3f vertex : vertices) {
            vertex.rotate(rotation);
            vertex.mul(quadSize);
            vertex.add(px, py, pz);
        }
        
        // Taint sprite animation (using witch-like sprites, row 2 columns 8-11)
        int sprite = 72 + spriteFrame;  // Wispy sprites
        float u0 = (sprite % 64) / 64.0f;
        float u1 = u0 + 0.015625f;
        float v0 = (sprite / 64) / 64.0f;
        float v1 = v0 + 0.015625f;
        
        int light = 0xF000F0;  // Full brightness for glow effect
        
        buffer.addVertex(vertices[0].x(), vertices[0].y(), vertices[0].z())
                .setUv(u1, v1).setColor(rCol, gCol, bCol, alpha).setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
        buffer.addVertex(vertices[1].x(), vertices[1].y(), vertices[1].z())
                .setUv(u1, v0).setColor(rCol, gCol, bCol, alpha).setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
        buffer.addVertex(vertices[2].x(), vertices[2].y(), vertices[2].z())
                .setUv(u0, v0).setColor(rCol, gCol, bCol, alpha).setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
        buffer.addVertex(vertices[3].x(), vertices[3].y(), vertices[3].z())
                .setUv(u0, v1).setColor(rCol, gCol, bCol, alpha).setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
    }
    
    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_LIT;
    }
}




