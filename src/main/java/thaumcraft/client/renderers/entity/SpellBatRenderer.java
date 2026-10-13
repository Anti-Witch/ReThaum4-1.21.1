package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import thaumcraft.common.entities.monster.EntitySpellBat;

/**
 * Renderer for SpellBats - magical summoned bats with colored transparency.
 * Renders as a simple billboard sprite since the vanilla bat model doesn't
 * work well with our custom SpellBat entity.
 */
@OnlyIn(Dist.CLIENT)
public class SpellBatRenderer extends EntityRenderer<EntitySpellBat> {
    
    private static final ResourceLocation TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/firebat.png");
    
    public SpellBatRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.25F;
    }
    
    @Override
    public ResourceLocation getTextureLocation(EntitySpellBat entity) {
        return TEXTURE;
    }
    
    @Override
    public void render(EntitySpellBat entity, float entityYaw, float partialTicks, 
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        
        // Get color from entity
        int color = entity.color;
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        float alpha = 0.6F;
        
        // Scale down
        poseStack.scale(0.35F, 0.35F, 0.35F);
        
        // Billboard - face the camera
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
        
        // Wing flap animation
        float flapAmount = Mth.sin((entity.tickCount + partialTicks) * 0.7F) * 0.3F;
        
        // Render as quad
        RenderType renderType = RenderType.entityTranslucent(TEXTURE);
        VertexConsumer vertexConsumer = buffer.getBuffer(renderType);
        
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        PoseStack.Pose normal = pose;
        
        float size = 1.0F;
        
        // Simple quad for the bat
        vertex(vertexConsumer, matrix, normal, -size, -size, 0, 0, 0, r, g, b, alpha, packedLight);
        vertex(vertexConsumer, matrix, normal, -size, size, 0, 0, 1, r, g, b, alpha, packedLight);
        vertex(vertexConsumer, matrix, normal, size, size, 0, 1, 1, r, g, b, alpha, packedLight);
        vertex(vertexConsumer, matrix, normal, size, -size, 0, 1, 0, r, g, b, alpha, packedLight);
        
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




