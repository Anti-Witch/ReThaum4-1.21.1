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
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.client.fx.particles.ThaumcraftParticle;

/**
 * Sonic boom particle effect - creates an expanding ring effect around entities.
 * Used for sonic-based attacks and shock waves.
 * 
 * Simplified version using expanding ring quads.
 */
@OnlyIn(Dist.CLIENT)
public class FXSonic extends ThaumcraftParticle {
    
    private static final ResourceLocation[] RIPPLE_TEXTURES = new ResourceLocation[16];
    
    static {
        for (int i = 0; i < 16; i++) {
            RIPPLE_TEXTURES[i] = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/ripple" + (i + 1) + ".png");
        }
    }
    
    private final Entity target;
    private final float yaw;
    private final float pitch;
    
    public FXSonic(ClientLevel level, double x, double y, double z, Entity target, int maxAge) {
        super(level, x, y, z);
        
        this.target = target;
        this.yaw = target.getYHeadRot();
        this.pitch = target.getXRot();
        
        this.rCol = 1.0f;
        this.gCol = 1.0f;
        this.bCol = 1.0f;
        this.gravity = 0.0f;
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        
        this.lifetime = maxAge + this.random.nextInt(maxAge / 2);
        this.quadSize = 1.0f;
        
        // Position at entity
        this.x = target.getX();
        this.xo = this.x;
        this.y = target.getY() + target.getEyeHeight();
        this.yo = this.y;
        this.z = target.getZ();
        this.zo = this.z;
        
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
        
        // Follow target
        this.x = this.target.getX();
        this.y = this.target.getY() + this.target.getEyeHeight();
        this.z = this.target.getZ();
    }
    
    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        float fade = (this.age + partialTicks) / this.lifetime;
        int frame = Math.min(15, (int)(14.0f * fade) + 1);
        
        // Set up our own rendering
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, RIPPLE_TEXTURES[frame]);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        
        // Camera position
        float px = (float)(Mth.lerp(partialTicks, this.xo, this.x) - camera.getPosition().x);
        float py = (float)(Mth.lerp(partialTicks, this.yo, this.y) - camera.getPosition().y);
        float pz = (float)(Mth.lerp(partialTicks, this.zo, this.z) - camera.getPosition().z);
        
        // Size based on target
        float size = 0.25f * this.target.getBbHeight() * (1.0f + fade * 2.0f);
        
        // Calculate direction vectors based on yaw/pitch
        float yawRad = (float) Math.toRadians(-this.yaw + 90.0f);
        float pitchRad = (float) Math.toRadians(this.pitch + 90.0f);
        
        // Forward direction
        float fx = Mth.cos(yawRad) * Mth.sin(pitchRad);
        float fy = Mth.cos(pitchRad);
        float fz = Mth.sin(yawRad) * Mth.sin(pitchRad);
        
        // Move forward from entity
        float offset = 2.0f * this.target.getBbHeight() + this.target.getBbWidth() / 2.0f;
        px += fx * offset;
        py += fy * offset;
        pz += fz * offset;
        
        // Simple billboard facing camera
        float b = 0.5f;
        builder.addVertex(px - size, py - size, pz).setUv(0, 1).setColor(b, b, b, 1.0f);
        builder.addVertex(px - size, py + size, pz).setUv(0, 0).setColor(b, b, b, 1.0f);
        builder.addVertex(px + size, py + size, pz).setUv(1, 0).setColor(b, b, b, 1.0f);
        builder.addVertex(px + size, py - size, pz).setUv(1, 1).setColor(b, b, b, 1.0f);
        
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
}




