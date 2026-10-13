package thaumcraft.client.fx.other;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.client.fx.particles.ThaumcraftParticle;

import java.util.ArrayList;
import java.util.List;

/**
 * Void stream particle effect - creates a dark, void-like flowing stream.
 * Used for void metal effects and eldritch connections.
 */
@OnlyIn(Dist.CLIENT)
public class FXVoidStream extends ThaumcraftParticle {
    
    private static final ResourceLocation VOID_TEX = ResourceLocation.withDefaultNamespace("textures/entity/end_portal.png");
    
    private final double targetX, targetY, targetZ;
    private final double startX, startY, startZ;
    private final int seed;
    public int length;
    
    private final List<StreamPoint> points = new ArrayList<>();
    private int growing = -1;
    
    private static class StreamPoint {
        double x, y, z;
        float scale;
        
        StreamPoint(double x, double y, double z, float scale) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.scale = scale;
        }
    }
    
    public FXVoidStream(ClientLevel level, double x, double y, double z,
                        double tx, double ty, double tz,
                        int seed, float scale) {
        super(level, x, y, z);
        
        this.seed = seed;
        this.length = 40;
        
        this.targetX = tx;
        this.targetY = ty;
        this.targetZ = tz;
        
        // Calculate lifetime based on distance
        double dx = tx - x;
        double dy = ty - y;
        double dz = tz - z;
        int base = (int)(Mth.sqrt((float)(dx * dx + dy * dy + dz * dz)) * 21.0f);
        if (base < 1) base = 1;
        this.lifetime = base * 2;
        
        // Initial motion
        this.xd = Mth.sin(seed / 4.0f) * 0.025;
        this.yd = Mth.sin(seed / 3.0f) * 0.025;
        this.zd = Mth.sin(seed / 2.0f) * 0.025;
        
        this.gravity = 0.2f;
        this.quadSize = (float)(scale * (1.0 + this.random.nextGaussian() * 0.15));
        
        // Initialize points
        this.points.add(new StreamPoint(0, 0, 0, 0.001f));
        this.points.add(new StreamPoint(0, 0, 0, 0.001f));
        
        this.startX = x;
        this.startY = y;
        this.startZ = z;
        
        this.noClip = true;
    }
    
    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        
        if (this.age++ >= this.lifetime || this.length < 1) {
            this.remove();
            return;
        }
        
        // Apply gravity and movement
        this.yd += 0.01 * this.gravity;
        this.x += this.xd;
        this.y += this.yd;
        this.z += this.zd;
        
        // Friction
        this.xd *= 0.985;
        this.yd *= 0.985;
        this.zd *= 0.985;
        
        // Clamp velocity
        this.xd = Mth.clamp(this.xd, -0.04, 0.04);
        this.yd = Mth.clamp(this.yd, -0.04, 0.04);
        this.zd = Mth.clamp(this.zd, -0.04, 0.04);
        
        // Attract towards target with randomness
        double dx = this.targetX - this.x;
        double dy = this.targetY - this.y;
        double dz = this.targetZ - this.z;
        double dist = Mth.sqrt((float)(dx * dx + dy * dy + dz * dz));
        
        dx /= dist;
        dy /= dist;
        dz /= dist;
        
        double d13 = 0.01;
        this.xd += dx * (d13 / Math.min(1.0, dist)) + this.random.nextGaussian() * 0.015;
        this.yd += dy * (d13 / Math.min(1.0, dist)) + this.random.nextGaussian() * 0.015;
        this.zd += dz * (d13 / Math.min(1.0, dist)) + this.random.nextGaussian() * 0.015;
        
        // Calculate scale with wave
        float scale = this.quadSize * (0.75f + Mth.sin((seed + this.age) / 2.0f) * 0.25f);
        if (dist < 0.5) {
            float f = Mth.sin((float)(dist * Math.PI / 2.0));
            scale *= f;
            this.quadSize *= f;
        }
        
        // Add point or start shrinking
        if (this.quadSize > 0.001f) {
            this.points.add(new StreamPoint(
                this.x - this.startX,
                this.y - this.startY,
                this.z - this.startZ,
                scale
            ));
        } else {
            if (this.growing < 0) {
                this.growing = this.age;
            }
            this.length--;
        }
        
        // Trim old points
        while (this.points.size() > this.length) {
            this.points.remove(0);
        }
    }
    
    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        if (this.points.size() < 3) return;

        // Set up our own rendering
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, VOID_TEX);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        
        // Camera position
        double camX = camera.getPosition().x;
        double camY = camera.getPosition().y;
        double camZ = camera.getPosition().z;
        
        // Render each segment as a billboarded quad
        for (int i = 1; i < this.points.size() - 1; i++) {
            StreamPoint p = this.points.get(i);
            
            // Wave animation
            float variance = 1.0f + Mth.sin((i + this.age) / 3.0f) * 0.2f;
            float xx = Mth.sin((i + this.age) / 6.0f) * 0.01f;
            float yy = Mth.sin((i + this.age) / 7.0f) * 0.01f;
            float zz = Mth.sin((i + this.age) / 8.0f) * 0.01f;
            
            float px = (float)(this.startX + p.x + xx - camX);
            float py = (float)(this.startY + p.y + yy - camY);
            float pz = (float)(this.startZ + p.z + zz - camZ);
            
            float size = p.scale * variance * 0.05f;
            
            // UV animation for void effect
            float time = (this.age + partialTicks) * 0.01f;
            float u0 = (i * 0.1f + time) % 1.0f;
            float v0 = (i * 0.1f) % 1.0f;
            float u1 = u0 + 0.25f;
            float v1 = v0 + 0.25f;
            
            // Simple billboard (always face camera)
            builder.addVertex(px - size, py - size, pz).setUv(u0, v1).setColor(1f, 1f, 1f, 1f);
            builder.addVertex(px - size, py + size, pz).setUv(u0, v0).setColor(1f, 1f, 1f, 1f);
            builder.addVertex(px + size, py + size, pz).setUv(u1, v0).setColor(1f, 1f, 1f, 1f);
            builder.addVertex(px + size, py - size, pz).setUv(u1, v1).setColor(1f, 1f, 1f, 1f);
        }
        
        BufferUploader.drawWithShader(builder.build());
        
        RenderSystem.depthMask(true);
        
        // Restart particle batch
        RenderSystem.setShaderTexture(0, TextureManager.INTENTIONAL_MISSING_TEXTURE);
        builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
    }
    
    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.CUSTOM;
    }
    
    public FXVoidStream setTCGravity(float value) {
        this.gravity = value;
        return this;
    }
}




