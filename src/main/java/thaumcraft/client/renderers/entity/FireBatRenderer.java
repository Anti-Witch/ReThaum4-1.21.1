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
import thaumcraft.common.entities.monster.EntityFireBat;

/**
 * Renderer for Fire Bats - fire elemental bats.
 * 
 * Fire bats are rendered as simple glowing sprites since the vanilla
 * BatModel doesn't support custom entity types. This creates a flame-like
 * animated appearance that fits the entity.
 */
@OnlyIn(Dist.CLIENT)
public class FireBatRenderer extends EntityRenderer<EntityFireBat> {
    
    private static final ResourceLocation TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/firebat.png");
    
    public FireBatRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.25F;
    }
    
    @Override
    public ResourceLocation getTextureLocation(EntityFireBat entity) {
        return TEXTURE;
    }
    
    @Override
    public void render(EntityFireBat entity, float entityYaw, float partialTicks, 
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        
        // Billboard rotation
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        
        // Wing flap animation
        float flapAnim = Mth.sin((entity.tickCount + partialTicks) * 0.75F) * 0.2F;
        float size = 0.5F + flapAnim * 0.1F;
        
        // Get render type
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
        
        // Render as billboard quad
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        PoseStack.Pose normal = pose;
        
        int light = 0xF000F0; // Full bright for fire
        
        // Simple quad
        vertexConsumer.addVertex(matrix, -size, -size, 0.0F)
            .setColor(255, 200, 100, 255)
            .setUv(0.0F, 1.0F)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF)
            .setNormal(normal, 0.0F, 1.0F, 0.0F)
            ;
        
        vertexConsumer.addVertex(matrix, size, -size, 0.0F)
            .setColor(255, 200, 100, 255)
            .setUv(1.0F, 1.0F)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF)
            .setNormal(normal, 0.0F, 1.0F, 0.0F)
            ;
        
        vertexConsumer.addVertex(matrix, size, size, 0.0F)
            .setColor(255, 200, 100, 255)
            .setUv(1.0F, 0.0F)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF)
            .setNormal(normal, 0.0F, 1.0F, 0.0F)
            ;
        
        vertexConsumer.addVertex(matrix, -size, size, 0.0F)
            .setColor(255, 200, 100, 255)
            .setUv(0.0F, 0.0F)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF)
            .setNormal(normal, 0.0F, 1.0F, 0.0F)
            ;
        
        poseStack.popPose();
        
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}




