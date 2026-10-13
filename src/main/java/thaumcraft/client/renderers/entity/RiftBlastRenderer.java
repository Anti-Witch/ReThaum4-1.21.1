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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import thaumcraft.common.entities.projectile.EntityRiftBlast;

/**
 * Renderer for Rift Blast projectiles.
 * Renders as a glowing orb with a wispy trail effect.
 * The original used an end portal shader, this version uses a simpler approach.
 */
@OnlyIn(Dist.CLIENT)
public class RiftBlastRenderer extends EntityRenderer<EntityRiftBlast> {
    
    private static final ResourceLocation TEXTURE = 
            ResourceLocation.withDefaultNamespace("textures/entity/end_portal.png");
    
    public RiftBlastRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }
    
    @Override
    public ResourceLocation getTextureLocation(EntityRiftBlast entity) {
        return TEXTURE;
    }
    
    @Override
    public void render(EntityRiftBlast entity, float entityYaw, float partialTicks, 
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        
        // Billboard rotation
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        
        // Pulsing scale
        float pulse = 1.0F + Mth.sin((entity.tickCount + partialTicks) * 0.5F) * 0.1F;
        float size = 0.5F * pulse;
        
        // Color based on red variant
        float r = entity.isRed() ? 1.0F : 0.3F;
        float g = entity.isRed() ? 0.2F : 0.1F;
        float b = entity.isRed() ? 0.3F : 0.4F;
        float alpha = 0.8F;
        
        // Render the orb
        RenderType renderType = RenderType.entityTranslucent(TEXTURE);
        VertexConsumer vertexConsumer = buffer.getBuffer(renderType);
        
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        PoseStack.Pose normal = pose;
        
        int light = 0xF000F0; // Full bright
        
        // Core quad
        vertex(vertexConsumer, matrix, normal, -size, -size, 0, 0, 0, r, g, b, alpha, light);
        vertex(vertexConsumer, matrix, normal, -size, size, 0, 0, 1, r, g, b, alpha, light);
        vertex(vertexConsumer, matrix, normal, size, size, 0, 1, 1, r, g, b, alpha, light);
        vertex(vertexConsumer, matrix, normal, size, -size, 0, 1, 0, r, g, b, alpha, light);
        
        // Outer glow (larger, more transparent)
        float glowSize = size * 1.5F;
        float glowAlpha = 0.3F;
        vertex(vertexConsumer, matrix, normal, -glowSize, -glowSize, 0.01F, 0, 0, r, g, b, glowAlpha, light);
        vertex(vertexConsumer, matrix, normal, -glowSize, glowSize, 0.01F, 0, 1, r, g, b, glowAlpha, light);
        vertex(vertexConsumer, matrix, normal, glowSize, glowSize, 0.01F, 1, 1, r, g, b, glowAlpha, light);
        vertex(vertexConsumer, matrix, normal, glowSize, -glowSize, 0.01F, 1, 0, r, g, b, glowAlpha, light);
        
        poseStack.popPose();
        
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
    
    private void vertex(VertexConsumer consumer, Matrix4f matrix, PoseStack.Pose normal,
                        float x, float y, float z, float u, float v,
                        float r, float g, float b, float alpha, int light) {
        consumer.addVertex(matrix, x, y, z)
                .setColor(r, g, b, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF)
                .setNormal(normal, 0.0F, 0.0F, -1.0F)
                ;
    }
}




