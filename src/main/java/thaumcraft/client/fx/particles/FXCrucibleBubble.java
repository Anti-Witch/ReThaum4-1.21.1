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
 * FXCrucibleBubble - Colored bubble particle for crucible effects.
 * Rises slowly, wobbles, and pops with the color of the current aspect.
 * 
 * Ported from 1.12.2
 */
@OnlyIn(Dist.CLIENT)
public class FXCrucibleBubble extends TextureSheetParticle {
    
    private float wobblePhase;
    private float wobbleSpeed;
    
    /**
     * Create a crucible bubble particle.
     * 
     * @param level Client level
     * @param x X position
     * @param y Y position
     * @param z Z position
     * @param r Red component (0-1)
     * @param g Green component (0-1)
     * @param b Blue component (0-1)
     */
    public FXCrucibleBubble(ClientLevel level, double x, double y, double z,
                            float r, float g, float b) {
        super(level, x, y, z, 0, 0, 0);
        
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
        this.alpha = 0.7f;
        
        this.quadSize = 0.05f + random.nextFloat() * 0.03f;
        
        // Slow upward drift with slight random horizontal motion
        this.xd = (random.nextFloat() - 0.5f) * 0.01;
        this.yd = 0.01 + random.nextFloat() * 0.015;
        this.zd = (random.nextFloat() - 0.5f) * 0.01;
        
        this.gravity = -0.001f;  // Slight upward buoyancy
        
        this.lifetime = 20 + random.nextInt(20);
        
        this.wobblePhase = random.nextFloat() * (float)Math.PI * 2;
        this.wobbleSpeed = 0.2f + random.nextFloat() * 0.1f;
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
        
        // Wobble motion
        wobblePhase += wobbleSpeed;
        xd += Mth.sin(wobblePhase) * 0.001;
        zd += Mth.cos(wobblePhase * 0.7f) * 0.001;
        
        // Apply velocity
        x += xd;
        y += yd;
        z += zd;
        
        // Dampen horizontal motion
        xd *= 0.95;
        zd *= 0.95;
        
        // Grow slightly then shrink before popping
        if (age < lifetime * 0.3f) {
            quadSize *= 1.02f;
        } else if (age > lifetime * 0.8f) {
            quadSize *= 0.95f;
            alpha *= 0.9f;
        }
    }
    
    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        Vec3 camPos = camera.getPosition();
        float px = (float)(Mth.lerp(partialTicks, xo, x) - camPos.x());
        float py = (float)(Mth.lerp(partialTicks, yo, y) - camPos.y());
        float pz = (float)(Mth.lerp(partialTicks, zo, z) - camPos.z());
        
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
            vertex.mul(quadSize);
            vertex.add(px, py, pz);
        }
        
        // Bubble sprite (using a circular sprite from the particle sheet)
        // Sprite 160 is a good bubble-like sprite
        int sprite = 160;
        float u0 = (sprite % 64) / 64.0f;
        float u1 = u0 + 0.015625f;
        float v0 = (sprite / 64) / 64.0f;
        float v1 = v0 + 0.015625f;
        
        int light = getLightColor(partialTicks);
        
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
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}




