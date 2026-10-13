package thaumcraft.client.fx.other;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.client.fx.particles.ThaumcraftParticle;

/**
 * Bore stream particle effect - creates a beam-like stream for the arcane bore.
 * Shows the direction of mining/energy flow.
 */
@OnlyIn(Dist.CLIENT)
public class FXBoreStream extends ThaumcraftParticle {
    
    private final double targetX, targetY, targetZ;
    private float beamLength;
    private float rotationAngle;
    
    public FXBoreStream(ClientLevel level, double x, double y, double z,
                        double tx, double ty, double tz, float scale) {
        super(level, x, y, z);
        
        this.targetX = tx;
        this.targetY = ty;
        this.targetZ = tz;
        
        // Calculate beam length
        double dx = tx - x;
        double dy = ty - y;
        double dz = tz - z;
        this.beamLength = Mth.sqrt((float)(dx * dx + dy * dy + dz * dz));
        
        this.rCol = 0.5f;
        this.gCol = 0.3f;
        this.bCol = 0.8f;
        this.alpha = 0.8f;
        
        this.quadSize = scale;
        this.lifetime = 10;
        this.gravity = 0;
        this.rotationAngle = this.random.nextFloat() * 360.0f;
        this.noClip = true;
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
        
        // Rotate slightly each tick
        this.rotationAngle += 5.0f;
        
        // Fade out
        float progress = (float) this.age / (float) this.lifetime;
        this.alpha = 0.8f * (1.0f - progress);
    }
    
    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        Vec3 cameraPos = camera.getPosition();
        
        float x1 = (float)(this.x - cameraPos.x());
        float y1 = (float)(this.y - cameraPos.y());
        float z1 = (float)(this.z - cameraPos.z());
        
        float x2 = (float)(this.targetX - cameraPos.x());
        float y2 = (float)(this.targetY - cameraPos.y());
        float z2 = (float)(this.targetZ - cameraPos.z());
        
        // Calculate direction
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float length = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 0.001f) return;
        
        dx /= length;
        dy /= length;
        dz /= length;
        
        // Calculate perpendicular vectors for beam width
        float perpX, perpY, perpZ;
        if (Math.abs(dy) < 0.9f) {
            // Cross with up vector
            perpX = -dz;
            perpY = 0;
            perpZ = dx;
        } else {
            // Cross with forward vector
            perpX = 0;
            perpY = dz;
            perpZ = -dy;
        }
        
        // Normalize
        float perpLen = Mth.sqrt(perpX * perpX + perpY * perpY + perpZ * perpZ);
        perpX /= perpLen;
        perpY /= perpLen;
        perpZ /= perpLen;
        
        // Rotate perpendicular around beam axis
        float rotRad = (float) Math.toRadians(this.rotationAngle);
        float cos = Mth.cos(rotRad);
        float sin = Mth.sin(rotRad);
        
        // Cross product for second perpendicular
        float perp2X = dy * perpZ - dz * perpY;
        float perp2Y = dz * perpX - dx * perpZ;
        float perp2Z = dx * perpY - dy * perpX;
        
        // Rotated perpendicular
        float rPerpX = perpX * cos + perp2X * sin;
        float rPerpY = perpY * cos + perp2Y * sin;
        float rPerpZ = perpZ * cos + perp2Z * sin;
        
        float width = this.quadSize * 0.05f;
        
        // Render beam as quad strip
        float u0 = 0;
        float u1 = 1;
        float v0 = 48.0f / 64.0f;
        float v1 = 49.0f / 64.0f;
        int light = 240;
        
        // Bottom left
        buffer.addVertex(x1 - rPerpX * width, y1 - rPerpY * width, z1 - rPerpZ * width)
              .setUv(u0, v1).setColor(this.rCol, this.gCol, this.bCol, this.alpha)
              .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
        // Top left
        buffer.addVertex(x1 + rPerpX * width, y1 + rPerpY * width, z1 + rPerpZ * width)
              .setUv(u0, v0).setColor(this.rCol, this.gCol, this.bCol, this.alpha)
              .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
        // Top right
        buffer.addVertex(x2 + rPerpX * width, y2 + rPerpY * width, z2 + rPerpZ * width)
              .setUv(u1, v0).setColor(this.rCol, this.gCol, this.bCol, this.alpha * 0.5f)
              .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
        // Bottom right
        buffer.addVertex(x2 - rPerpX * width, y2 - rPerpY * width, z2 - rPerpZ * width)
              .setUv(u1, v1).setColor(this.rCol, this.gCol, this.bCol, this.alpha * 0.5f)
              .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF);
    }
    
    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}




