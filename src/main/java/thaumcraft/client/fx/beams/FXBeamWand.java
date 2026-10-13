package thaumcraft.client.fx.beams;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * FXBeamWand - Continuous beam effect from a living entity to a target point.
 * Used for wand/gauntlet casting effects, creating a smooth beam that follows
 * the caster's position and points toward the target.
 * 
 * Features:
 * - Entity-attached source (follows caster)
 * - Multiple beam texture options
 * - Scrolling UV animation
 * - Pulse fade in/out effect
 * - Impact flash at target point
 * - 3 rotated quads for cylindrical appearance
 * 
 * Ported from 1.12.2
 */
@OnlyIn(Dist.CLIENT)
public class FXBeamWand extends TextureSheetParticle {
    
    private static final ResourceLocation BEAM_TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/beam.png");
    private static final ResourceLocation BEAM1_TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/beam1.png");
    private static final ResourceLocation BEAM2_TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/beam2.png");
    private static final ResourceLocation BEAM3_TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/beam3.png");
    private static final ResourceLocation PARTICLE_TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/particles.png");
    
    // Source entity
    protected LivingEntity sourceEntity;
    protected double offset;
    
    // Target position
    protected double targetX;
    protected double targetY;
    protected double targetZ;
    protected double prevTargetX;
    protected double prevTargetY;
    protected double prevTargetZ;
    
    // Beam geometry
    protected float length;
    protected float rotYaw;
    protected float rotPitch;
    protected float prevYaw;
    protected float prevPitch;
    
    // Configuration
    protected int beamType = 0;
    protected float endMod = 1.0f;
    protected boolean reverse = false;
    protected boolean pulse = true;
    protected int rotationSpeed = 5;
    
    // State
    protected float prevSize = 0.0f;
    public int impact = 0;
    
    /**
     * Create a beam from a living entity to a target point.
     * 
     * @param level The client level
     * @param source The source entity (caster)
     * @param tx Target X coordinate
     * @param ty Target Y coordinate
     * @param tz Target Z coordinate
     * @param r Red color component (0-1)
     * @param g Green color component (0-1)
     * @param b Blue color component (0-1)
     * @param maxAge Maximum age in ticks
     */
    public FXBeamWand(ClientLevel level, LivingEntity source, 
                      double tx, double ty, double tz,
                      float r, float g, float b, int maxAge) {
        super(level, source.getX(), source.getY(), source.getZ(), 0, 0, 0);
        
        this.sourceEntity = source;
        this.offset = source.getBbHeight() / 2.0f + 0.25;
        
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
        
        this.setSize(0.02f, 0.02f);
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        
        this.targetX = tx;
        this.targetY = ty;
        this.targetZ = tz;
        this.prevTargetX = tx;
        this.prevTargetY = ty;
        this.prevTargetZ = tz;
        
        // Calculate initial beam orientation
        calculateBeamGeometry();
        this.prevYaw = rotYaw;
        this.prevPitch = rotPitch;
        
        this.lifetime = maxAge;
        
        // Distance-based visibility culling
        Minecraft mc = Minecraft.getInstance();
        if (mc.cameraEntity != null) {
            int visibleDistance = mc.options.graphicsMode().get().getId() > 0 ? 50 : 25;
            if (mc.cameraEntity.distanceTo(source) > visibleDistance) {
                this.lifetime = 0;
            }
        }
    }
    
    /**
     * Update the beam's target position.
     * Call this to extend the beam's life and change the endpoint.
     */
    public void updateBeam(double x, double y, double z) {
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
        
        // Extend lifetime
        while (this.lifetime - this.age < 4) {
            this.lifetime++;
        }
    }
    
    /**
     * Calculate beam length, yaw, and pitch from source to target.
     */
    protected void calculateBeamGeometry() {
        double sx = sourceEntity.getX();
        double sy = sourceEntity.getY() + offset;
        double sz = sourceEntity.getZ();
        
        float dx = (float)(sx - targetX);
        float dy = (float)(sy - targetY);
        float dz = (float)(sz - targetZ);
        
        this.length = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        double horizontalDist = Mth.sqrt(dx * dx + dz * dz);
        
        this.rotYaw = (float)(Math.atan2(dx, dz) * 180.0 / Math.PI);
        this.rotPitch = (float)(Math.atan2(dy, horizontalDist) * 180.0 / Math.PI);
    }
    
    @Override
    public void tick() {
        // Store previous positions
        this.xo = sourceEntity.getX();
        this.yo = sourceEntity.getY() + offset;
        this.zo = sourceEntity.getZ();
        
        this.prevTargetX = targetX;
        this.prevTargetY = targetY;
        this.prevTargetZ = targetZ;
        
        this.prevYaw = rotYaw;
        this.prevPitch = rotPitch;
        
        // Recalculate geometry
        calculateBeamGeometry();
        
        // Normalize rotation changes
        while (rotPitch - prevPitch < -180.0f) prevPitch -= 360.0f;
        while (rotPitch - prevPitch >= 180.0f) prevPitch += 360.0f;
        while (rotYaw - prevYaw < -180.0f) prevYaw -= 360.0f;
        while (rotYaw - prevYaw >= 180.0f) prevYaw += 360.0f;
        
        // Impact countdown
        if (impact > 0) {
            impact--;
        }
        
        if (this.age++ >= this.lifetime) {
            this.remove();
        }
    }
    
    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        // Beams need custom rendering - we'll render directly here
        renderBeam(camera, partialTicks);
    }
    
    /**
     * Render the beam using custom rendering.
     */
    protected void renderBeam(Camera camera, float partialTicks) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        
        // Calculate size with pulse
        float size = 1.0f;
        if (pulse) {
            size = Math.min(age / 4.0f, 1.0f);
            size = prevSize + (size - prevSize) * partialTicks;
        }
        
        // Calculate opacity with fade
        float opacity = 0.4f;
        if (pulse && lifetime - age <= 4) {
            opacity = 0.4f - (4 - (lifetime - age)) * 0.1f;
        }
        
        // Bind beam texture
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        ResourceLocation texture = switch (beamType) {
            case 1 -> BEAM1_TEXTURE;
            case 2 -> BEAM2_TEXTURE;
            case 3 -> BEAM3_TEXTURE;
            default -> BEAM_TEXTURE;
        };
        RenderSystem.setShaderTexture(0, texture);
        
        // Set up render state
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(770, 1); // Additive blending
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        
        // Calculate source position with offset for hand position
        double prevSX = sourceEntity.xo;
        double prevSY = sourceEntity.yo + offset;
        double prevSZ = sourceEntity.zo;
        double currSX = sourceEntity.getX();
        double currSY = sourceEntity.getY() + offset;
        double currSZ = sourceEntity.getZ();
        
        // Apply hand offset based on yaw
        float yawRad = sourceEntity.getYRot() * ((float)Math.PI / 180.0f);
        float cosYaw = Mth.cos(yawRad);
        float sinYaw = Mth.sin(yawRad);
        
        prevSX -= cosYaw * 0.066f;
        prevSY -= 0.06;
        prevSZ -= sinYaw * 0.04f;
        
        currSX -= cosYaw * 0.066f;
        currSY -= 0.06;
        currSZ -= sinYaw * 0.04f;
        
        // Add look direction offset
        Vec3 look = sourceEntity.getLookAngle();
        prevSX += look.x * 0.3;
        prevSY += look.y * 0.3;
        prevSZ += look.z * 0.3;
        currSX += look.x * 0.3;
        currSY += look.y * 0.3;
        currSZ += look.z * 0.3;
        
        // Interpolate position
        Vec3 camPos = camera.getPosition();
        float sx = (float)(Mth.lerp(partialTicks, prevSX, currSX) - camPos.x());
        float sy = (float)(Mth.lerp(partialTicks, prevSY, currSY) - camPos.y());
        float sz = (float)(Mth.lerp(partialTicks, prevSZ, currSZ) - camPos.z());
        
        // Interpolate rotation
        float yaw = Mth.lerp(partialTicks, prevYaw, rotYaw);
        float pitch = Mth.lerp(partialTicks, prevPitch, rotPitch);
        
        // Calculate UV scroll
        float slide = (float)(Minecraft.getInstance().player.tickCount) + partialTicks;
        if (reverse) slide *= -1.0f;
        float uvOffset = -slide * 0.2f - Mth.floor(-slide * 0.1f);
        
        // Rotation for beam cylinder effect
        float rot = (level.getGameTime() % (360 / rotationSpeed)) * rotationSpeed + rotationSpeed * partialTicks;
        
        // Set up transformation
        PoseStack poseStack = new PoseStack();
        poseStack.pushPose();
        poseStack.translate(sx, sy, sz);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
        poseStack.mulPose(Axis.ZN.rotationDegrees(180.0f + yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.mulPose(Axis.YP.rotationDegrees(rot));
        
        Matrix4f matrix = poseStack.last().pose();
        
        // Beam dimensions
        float beamWidth = 0.15f * size;
        float beamWidthEnd = beamWidth * endMod;
        float beamLength = this.length * size;
        
        // Draw 3 quads rotated 60 degrees apart for cylindrical effect
        
        for (int t = 0; t < 3; t++) {
            float u0 = 0.0f;
            float u1 = 1.0f;
            float v0 = uvOffset + t / 3.0f;
            float v1 = beamLength * size + v0;
            
            // Quad vertices
            builder.addVertex(matrix, -beamWidthEnd, beamLength, 0)
                    .setUv(u1, v1).setColor(rCol, gCol, bCol, opacity);
            builder.addVertex(matrix, -beamWidth, 0, 0)
                    .setUv(u1, v0).setColor(rCol, gCol, bCol, opacity);
            builder.addVertex(matrix, beamWidth, 0, 0)
                    .setUv(u0, v0).setColor(rCol, gCol, bCol, opacity);
            builder.addVertex(matrix, beamWidthEnd, beamLength, 0)
                    .setUv(u0, v1).setColor(rCol, gCol, bCol, opacity);
            
            // Rotate for next quad
            poseStack.mulPose(Axis.YP.rotationDegrees(60.0f));
            matrix = poseStack.last().pose();
        }
        
        BufferUploader.drawWithShader(builder.build());
        
        poseStack.popPose();
        
        // Render impact flash if active
        if (impact > 0) {
            renderImpact(camera, partialTicks);
        }
        
        // Restore render state
        RenderSystem.depthMask(true);
        RenderSystem.blendFunc(770, 771);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        
        prevSize = size;
    }
    
    /**
     * Render the impact flash at the target point.
     */
    protected void renderImpact(Camera camera, float partialTicks) {
        RenderSystem.setShaderTexture(0, PARTICLE_TEXTURE);
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(770, 1);
        
        Vec3 camPos = camera.getPosition();
        float tx = (float)(Mth.lerp(partialTicks, prevTargetX, targetX) - camPos.x());
        float ty = (float)(Mth.lerp(partialTicks, prevTargetY, targetY) - camPos.y());
        float tz = (float)(Mth.lerp(partialTicks, prevTargetZ, targetZ) - camPos.z());
        
        float impactSize = endMod / 2.0f / (6 - impact);
        
        // UV coordinates for impact particle in sprite sheet
        int frame = age % 16;
        float u0 = frame / 16.0f;
        float u1 = u0 + 0.0625f;
        float v0 = 0.3125f;
        float v1 = v0 + 0.0625f;
        
        // Billboard the impact sprite
        Quaternionf rotation = camera.rotation();
        
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        
        // Create billboard quad
        float[][] offsets = {
            {-impactSize, -impactSize},
            {-impactSize, impactSize},
            {impactSize, impactSize},
            {impactSize, -impactSize}
        };
        float[][] uvs = {
            {u1, v1},
            {u1, v0},
            {u0, v0},
            {u0, v1}
        };
        
        for (int i = 0; i < 4; i++) {
            float ox = offsets[i][0];
            float oy = offsets[i][1];
            
            // Rotate offset by camera rotation
            float rx = ox * (1 - 2 * rotation.y() * rotation.y() - 2 * rotation.z() * rotation.z())
                     + oy * (2 * rotation.x() * rotation.y() - 2 * rotation.w() * rotation.z());
            float ry = ox * (2 * rotation.x() * rotation.y() + 2 * rotation.w() * rotation.z())
                     + oy * (1 - 2 * rotation.x() * rotation.x() - 2 * rotation.z() * rotation.z());
            float rz = ox * (2 * rotation.x() * rotation.z() - 2 * rotation.w() * rotation.y())
                     + oy * (2 * rotation.y() * rotation.z() + 2 * rotation.w() * rotation.x());
            
            builder.addVertex(tx + rx, ty + ry, tz + rz)
                    .setUv(uvs[i][0], uvs[i][1])
                    .setColor(rCol, gCol, bCol, 0.66f)
                    ;
        }
        
        BufferUploader.drawWithShader(builder.build());
        
        RenderSystem.blendFunc(770, 771);
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
    }
    
    @Override
    public ParticleRenderType getRenderType() {
        // Use custom render type that doesn't batch with other particles
        return ParticleRenderType.CUSTOM;
    }
    
    // ==================== Configuration Methods ====================
    
    public void setRGB(float r, float g, float b) {
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
    }
    
    public void setType(int type) {
        this.beamType = type;
    }
    
    public void setEndMod(float endMod) {
        this.endMod = endMod;
    }
    
    public void setReverse(boolean reverse) {
        this.reverse = reverse;
    }
    
    public void setPulse(boolean pulse) {
        this.pulse = pulse;
    }
    
    public void setRotationSpeed(int rotationSpeed) {
        this.rotationSpeed = rotationSpeed;
    }
    
    public LivingEntity getSourceEntity() {
        return sourceEntity;
    }
    
    public float getLength() {
        return length;
    }
}




