package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entities.monster.cult.EntityCultistPortalLesser;

/**
 * Renderer for Cultist Portals (Lesser).
 * Renders as an animated billboard quad that faces the player.
 */
@OnlyIn(Dist.CLIENT)
public class CultistPortalRenderer extends EntityRenderer<EntityCultistPortalLesser> {
    
    private static final ResourceLocation TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/cultist_portal.png");
    
    public CultistPortalRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }
    
    @Override
    public ResourceLocation getTextureLocation(EntityCultistPortalLesser entity) {
        return TEXTURE;
    }
    
    @Override
    public void render(EntityCultistPortalLesser entity, float entityYaw, float partialTicks, 
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (!entity.isActive()) {
            return;
        }
        
        renderPortal(entity, entity.activeCounter, entity.hurtTime, entity.pulse, 
                entity.getHealth(), entity.getMaxHealth(), entity.getBbHeight(),
                partialTicks, poseStack, buffer);
        
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
    
    protected void renderPortal(Entity entity, int activeCounter, int hurtTime, int pulse,
                              float health, float maxHealth, float height,
                              float partialTicks, PoseStack poseStack, MultiBufferSource buffer) {
        poseStack.pushPose();
        
        // Calculate animation values
        long time = System.nanoTime() / 50000000L;
        float scaley = 1.4F;
        int activeFrames = (int) Math.min(50.0F, activeCounter + partialTicks);
        
        // Hurt wobble effect
        if (hurtTime > 0) {
            double hurtWobble = Math.sin(hurtTime * 72 * Math.PI / 180.0);
            scaley -= (float)(hurtWobble / 4.0);
            activeFrames += (int)(6.0 * hurtWobble);
        }
        
        // Pulse effect
        if (pulse > 0) {
            double pulseWobble = Math.sin(pulse * 36 * Math.PI / 180.0);
            scaley += (float)(pulseWobble / 4.0);
            activeFrames += (int)(12.0 * pulseWobble);
        }
        
        float scale = activeFrames / 50.0F * 1.25F;
        
        // Health-based wobble
        float healthPercent = 1.0F - health / maxHealth;
        float m = healthPercent / 3.0F;
        float bob = Mth.sin(activeCounter / (5.0F - 12.0F * m)) * m + m;
        float bob2 = Mth.sin(activeCounter / (6.0F - 15.0F * m)) * m + m;
        float alpha = 1.0F - bob;
        scaley -= bob / 4.0F;
        scale -= bob2 / 3.0F;
        
        // Position at center of entity
        poseStack.translate(0.0, height / 2.0F, 0.0);
        
        // Billboard - face the camera
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        
        // Get texture frame (16 frame animation)
        int frame = 15 - (int)(time % 16);
        float minU = frame / 16.0F;
        float maxU = minU + 0.0625F;
        float minV = 0.0F;
        float maxV = 1.0F;
        
        // Render the quad
        RenderType renderType = RenderType.entityTranslucent(TEXTURE);
        VertexConsumer vertexConsumer = buffer.getBuffer(renderType);
        
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        PoseStack.Pose normal = pose;
        
        int light = 0xF000F0; // Full bright
        
        // Quad vertices
        vertex(vertexConsumer, matrix, normal, -scale, -scaley, 0, maxU, minV, alpha, light);
        vertex(vertexConsumer, matrix, normal, -scale, scaley, 0, maxU, maxV, alpha, light);
        vertex(vertexConsumer, matrix, normal, scale, scaley, 0, minU, maxV, alpha, light);
        vertex(vertexConsumer, matrix, normal, scale, -scaley, 0, minU, minV, alpha, light);
        
        poseStack.popPose();
    }
    
    private void vertex(VertexConsumer consumer, Matrix4f matrix, PoseStack.Pose normal,
                        float x, float y, float z, float u, float v, float alpha, int light) {
        consumer.addVertex(matrix, x, y, z)
                .setColor(1.0F, 1.0F, 1.0F, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF)
                .setNormal(normal, 0.0F, 0.0F, -1.0F)
                ;
    }
}




