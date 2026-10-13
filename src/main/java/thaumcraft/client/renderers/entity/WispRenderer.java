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
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.entities.monster.EntityWisp;

/**
 * Renderer for Wisps - glowing orbs of magical energy.
 * 
 * Wisps don't use a traditional model. Instead, they render as
 * animated billboard quads facing the camera, with different
 * layers for the core glow, inner orb, and outer aura.
 * 
 * The color is determined by the Wisp's aspect type.
 */
@OnlyIn(Dist.CLIENT)
public class WispRenderer extends EntityRenderer<EntityWisp> {
    
    // Texture atlas with wisp particles
    private static final ResourceLocation WISP_TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/wisp.png");
    
    public WispRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
        this.shadowStrength = 0.0F;
    }
    
    @Override
    public ResourceLocation getTextureLocation(EntityWisp entity) {
        return WISP_TEXTURE;
    }
    
    @Override
    public void render(EntityWisp entity, float entityYaw, float partialTicks, 
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (entity.isDeadOrDying()) {
            return;
        }
        
        // Get color from aspect type
        int color = 0xFFFFFF;
        Aspect aspect = entity.getAspect();
        if (aspect != null) {
            color = aspect.getColor();
        }
        
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        
        poseStack.pushPose();
        
        // Billboard rotation - always face camera
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        
        // Animation based on tick count
        int animFrame = (entity.tickCount + (int)(partialTicks)) % 16;
        float pulse = 0.8F + 0.2F * Mth.sin((entity.tickCount + partialTicks) * 0.2F);
        
        // Render glow layers
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(WISP_TEXTURE));
        
        // Outer glow (larger, more transparent)
        renderQuad(poseStack, vertexConsumer, 0.75F * pulse, red, green, blue, 0.25F, animFrame, packedLight);
        
        // Middle glow
        renderQuad(poseStack, vertexConsumer, 0.5F * pulse, red, green, blue, 0.5F, animFrame, packedLight);
        
        // Core (white-ish, bright)
        renderQuad(poseStack, vertexConsumer, 0.3F * pulse, 1.0F, 1.0F, 1.0F, 0.8F, animFrame, packedLight);
        
        poseStack.popPose();
        
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
    
    /**
     * Renders a billboard quad for one layer of the wisp.
     */
    private void renderQuad(PoseStack poseStack, VertexConsumer vertexConsumer, 
                           float size, float red, float green, float blue, float alpha,
                           int frame, int packedLight) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        PoseStack.Pose normal = pose;
        
        // Calculate UV coordinates for animation frame (4x4 grid)
        int frameX = frame % 4;
        int frameY = frame / 4;
        float u0 = frameX / 4.0F;
        float u1 = (frameX + 1) / 4.0F;
        float v0 = frameY / 4.0F;
        float v1 = (frameY + 1) / 4.0F;
        
        // Full brightness for emissive rendering
        int light = 0xF000F0;
        
        // Quad vertices
        vertexConsumer.addVertex(matrix, -size, -size, 0.0F)
            .setColor(red, green, blue, alpha)
            .setUv(u0, v1)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF)
            .setNormal(normal, 0.0F, 1.0F, 0.0F)
            ;
        
        vertexConsumer.addVertex(matrix, size, -size, 0.0F)
            .setColor(red, green, blue, alpha)
            .setUv(u1, v1)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF)
            .setNormal(normal, 0.0F, 1.0F, 0.0F)
            ;
        
        vertexConsumer.addVertex(matrix, size, size, 0.0F)
            .setColor(red, green, blue, alpha)
            .setUv(u1, v0)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF)
            .setNormal(normal, 0.0F, 1.0F, 0.0F)
            ;
        
        vertexConsumer.addVertex(matrix, -size, size, 0.0F)
            .setColor(red, green, blue, alpha)
            .setUv(u0, v0)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF)
            .setNormal(normal, 0.0F, 1.0F, 0.0F)
            ;
    }
}




