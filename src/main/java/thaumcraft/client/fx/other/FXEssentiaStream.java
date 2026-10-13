package thaumcraft.client.fx.other;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.client.fx.particles.ThaumcraftParticle;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Essentia stream particle effect - creates a flowing stream of essentia between two points.
 * Used for tube connections and essentia transport visualization.
 */
@OnlyIn(Dist.CLIENT)
public class FXEssentiaStream extends ThaumcraftParticle {
    
    private static final ResourceLocation ESSENTIA_TEX = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/essentia.png");
    private static final Map<String, FXEssentiaStream> activeStreams = new HashMap<>();
    
    private final double targetX, targetY, targetZ;
    private final double startX, startY, startZ;
    private final int count;
    private final String key;
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
    
    public FXEssentiaStream(ClientLevel level, double x, double y, double z,
                           double tx, double ty, double tz,
                           int count, int color, float scale, int extend, double my) {
        super(level, x, y, z);
        
        this.count = count;
        this.length = Math.max(20, extend);
        
        this.targetX = tx;
        this.targetY = ty;
        this.targetZ = tz;
        
        // Calculate lifetime based on distance
        double dx = tx - x;
        double dy = ty - y;
        double dz = tz - z;
        int base = (int)(Mth.sqrt((float)(dx * dx + dy * dy + dz * dz)) * 21.0f);
        if (base < 1) base = 1;
        this.lifetime = base;
        
        // Set up key for duplicate detection
        BlockPos bp1 = BlockPos.containing(x, y, z);
        BlockPos bp2 = BlockPos.containing(tx, ty, tz);
        this.key = bp1.asLong() + "" + bp2.asLong() + "" + color;
        
        // Check for existing stream
        if (activeStreams.containsKey(key)) {
            FXEssentiaStream existing = activeStreams.get(key);
            if (existing.isAlive() && existing.points.size() < existing.length) {
                existing.length += Math.max(extend, 5);
                existing.lifetime += Math.max(extend, 5);
                this.lifetime = 0;
            }
        }
        
        if (this.lifetime > 0) {
            activeStreams.put(key, this);
        }
        
        // Initial motion
        this.xd = Mth.sin(count / 4.0f) * 0.015;
        this.yd = my + Mth.sin(count / 3.0f) * 0.015;
        this.zd = Mth.sin(count / 2.0f) * 0.015;
        
        // Color from int
        Color c = new Color(color);
        this.rCol = c.getRed() / 255.0f;
        this.gCol = c.getGreen() / 255.0f;
        this.bCol = c.getBlue() / 255.0f;
        
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
            if (activeStreams.containsKey(key) && !activeStreams.get(key).isAlive()) {
                activeStreams.remove(key);
            }
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
        this.xd = Mth.clamp(this.xd, -0.05, 0.05);
        this.yd = Mth.clamp(this.yd, -0.05, 0.05);
        this.zd = Mth.clamp(this.zd, -0.05, 0.05);
        
        // Attract towards target
        double dx = this.targetX - this.x;
        double dy = this.targetY - this.y;
        double dz = this.targetZ - this.z;
        double dist = Mth.sqrt((float)(dx * dx + dy * dy + dz * dz));
        
        dx /= dist;
        dy /= dist;
        dz /= dist;
        
        double d13 = 0.01;
        this.xd += dx * (d13 / Math.min(1.0, dist));
        this.yd += dy * (d13 / Math.min(1.0, dist));
        this.zd += dz * (d13 / Math.min(1.0, dist));
        
        // Calculate scale with wave
        float scale = this.quadSize * (0.75f + Mth.sin((count + this.age) / 2.0f) * 0.25f);
        if (dist < 1.0) {
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
            
            // Spawn drop effect at target
            FXDispatcher.INSTANCE.essentiaDropFx(
                this.targetX + this.random.nextGaussian() * 0.075,
                this.targetY + this.random.nextGaussian() * 0.075,
                this.targetZ + this.random.nextGaussian() * 0.075,
                this.rCol, this.gCol, this.bCol, 0.5f
            );
        }
        
        // Trim old points
        while (this.points.size() > this.length) {
            this.points.remove(0);
        }
        
        // Spawn drops along stream
        if (this.points.size() > 2 && this.random.nextBoolean()) {
            int q = this.random.nextInt(Math.min(3, this.points.size()));
            if (this.random.nextBoolean() && this.points.size() > 2) {
                q = this.points.size() - 2;
            }
            StreamPoint p = this.points.get(q);
            FXDispatcher.INSTANCE.essentiaDropFx(
                p.x + this.startX, p.y + this.startY, p.z + this.startZ,
                this.rCol, this.gCol, this.bCol, 0.5f
            );
        }
    }
    
    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        if (this.points.size() < 3) return;

        // Set up our own rendering
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, ESSENTIA_TEX);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
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
            float xx = Mth.sin((i + this.age) / 6.0f) * 0.03f;
            float yy = Mth.sin((i + this.age) / 7.0f) * 0.03f;
            float zz = Mth.sin((i + this.age) / 8.0f) * 0.03f;
            
            float px = (float)(this.startX + p.x + xx - camX);
            float py = (float)(this.startY + p.y + yy - camY);
            float pz = (float)(this.startZ + p.z + zz - camZ);
            
            float size = p.scale * variance * 0.05f;
            
            // Color variation
            float v = 1.0f - Mth.sin((i + this.age) / 2.0f) * 0.1f;
            float r = this.rCol * v;
            float g = this.gCol * v;
            float b = this.bCol * v;
            
            // Simple billboard (always face camera)
            builder.addVertex(px - size, py - size, pz).setUv(0, 1).setColor(r, g, b, 1.0f);
            builder.addVertex(px - size, py + size, pz).setUv(0, 0).setColor(r, g, b, 1.0f);
            builder.addVertex(px + size, py + size, pz).setUv(1, 0).setColor(r, g, b, 1.0f);
            builder.addVertex(px + size, py - size, pz).setUv(1, 1).setColor(r, g, b, 1.0f);
        }
        
        BufferUploader.drawWithShader(builder.build());
        
        // Restart particle batch
        RenderSystem.setShaderTexture(0, TextureManager.INTENTIONAL_MISSING_TEXTURE);
        builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
    }
    
    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.CUSTOM;
    }
    
    public FXEssentiaStream setTCGravity(float value) {
        this.gravity = value;
        return this;
    }
}




