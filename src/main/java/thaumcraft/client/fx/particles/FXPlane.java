package thaumcraft.client.fx.particles;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

/**
 * FXPlane - Flat plane particle that moves from point A to point B.
 * Used for projectile trails, streak effects, and directional magic visuals.
 * The plane is oriented to face along its movement direction.
 * 
 * Features:
 * - Oriented along travel path (not billboarded)
 * - Animated sprite sequence
 * - Fade in/out alpha animation
 * - Growing scale over lifetime
 * 
 * Ported from 1.12.2
 */
@OnlyIn(Dist.CLIENT)
public class FXPlane extends TextureSheetParticle {
    
    private static final ResourceLocation PARTICLE_TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/particles.png");
    
    private float angle;      // Random roll rotation
    private float angleYaw;   // Yaw to face target
    private float anglePitch; // Pitch to face target
    
    private int particleTextureIndexX = 22;
    private int particleTextureIndexY = 10;
    
    /**
     * Create a plane particle that travels from current position to target.
     * 
     * @param level The client level
     * @param x Start X
     * @param y Start Y  
     * @param z Start Z
     * @param targetX End X
     * @param targetY End Y
     * @param targetZ End Z
     * @param lifetime Particle lifetime in ticks
     */
    public FXPlane(ClientLevel level, double x, double y, double z,
                   double targetX, double targetY, double targetZ, int lifetime) {
        super(level, x, y, z, 0, 0, 0);
        
        this.rCol = 1.0f;
        this.gCol = 1.0f;
        this.bCol = 1.0f;
        
        this.gravity = 0.0f;
        this.lifetime = lifetime;
        this.setSize(0.01f, 0.01f);
        
        this.xo = x;
        this.yo = y;
        this.zo = z;
        
        this.quadSize = 1.0f;
        this.alpha = 0.0f;
        
        // Calculate velocity to reach target over lifetime
        double dx = targetX - x;
        double dy = targetY - y;
        double dz = targetZ - z;
        
        this.xd = dx / lifetime;
        this.yd = dy / lifetime;
        this.zd = dz / lifetime;
        
        // Calculate orientation angles
        double horizontalDist = Mth.sqrt((float)(dx * dx + dz * dz));
        this.angleYaw = 0.0f;
        this.anglePitch = 0.0f;
        
        if (horizontalDist >= 1.0E-7) {
            this.angleYaw = (float)(Mth.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
            this.anglePitch = (float)(-(Mth.atan2(dy, horizontalDist) * 180.0 / Math.PI));
        }
        
        // Random roll
        this.angle = (float)(random.nextGaussian() * 20.0);
    }
    
    @Override
    public void tick() {
        this.xo = x;
        this.yo = y;
        this.zo = z;
        
        // Fade in/out
        float threshold = lifetime / 5.0f;
        if (age <= threshold) {
            alpha = age / threshold;
        } else {
            alpha = (lifetime - age) / (float)lifetime;
        }
        
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        
        // Move toward target
        x += xd;
        y += yd;
        z += zd;
    }
    
    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        // Custom rendering for oriented plane
        renderPlane(camera, partialTicks);
    }
    
    protected void renderPlane(Camera camera, float partialTicks) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, PARTICLE_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        
        Vec3 camPos = camera.getPosition();
        float px = (float)(Mth.lerp(partialTicks, xo, x) - camPos.x());
        float py = (float)(Mth.lerp(partialTicks, yo, y) - camPos.y());
        float pz = (float)(Mth.lerp(partialTicks, zo, z) - camPos.z());
        
        // Animated frame based on lifetime progress
        int frame = 22 + Math.round((age + partialTicks) / lifetime * 8.0f);
        frame = Math.min(frame, 30);  // Clamp to valid range
        
        float u0 = frame / 32.0f;
        float u1 = u0 + 0.03125f;
        float v0 = particleTextureIndexY / 32.0f;
        float v1 = v0 + 0.03125f;
        
        // Growing scale
        float size = quadSize * (0.5f + (age + partialTicks) / lifetime);
        float renderAlpha = alpha / 2.0f;
        
        // Set up transformation
        PoseStack poseStack = new PoseStack();
        poseStack.pushPose();
        poseStack.translate(px, py, pz);
        
        // Rotate to face along movement direction
        poseStack.mulPose(Axis.YP.rotationDegrees(-angleYaw + 90.0f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(anglePitch + 90.0f));
        poseStack.mulPose(Axis.YP.rotationDegrees(angle));
        
        Matrix4f matrix = poseStack.last().pose();
        
        float halfSize = size * 0.5f;
        
        
        builder.addVertex(matrix, -halfSize, halfSize, 0)
                .setUv(u1, v1).setColor(rCol, gCol, bCol, renderAlpha);
        builder.addVertex(matrix, halfSize, halfSize, 0)
                .setUv(u1, v0).setColor(rCol, gCol, bCol, renderAlpha);
        builder.addVertex(matrix, halfSize, -halfSize, 0)
                .setUv(u0, v0).setColor(rCol, gCol, bCol, renderAlpha);
        builder.addVertex(matrix, -halfSize, -halfSize, 0)
                .setUv(u0, v1).setColor(rCol, gCol, bCol, renderAlpha);
        
        BufferUploader.drawWithShader(builder.build());
        
        poseStack.popPose();
        
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
    
    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.CUSTOM;
    }
    
    public void setColor(float r, float g, float b) {
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
    }
    
    public void setGravity(float value) {
        this.gravity = value;
    }
}




