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
import thaumcraft.common.entities.monster.tainted.EntityTaintSwarm;

import java.util.Random;

/**
 * Renderer for Taint Swarms - clouds of tainted insects.
 * Renders as a cluster of animated particles/sprites.
 */
@OnlyIn(Dist.CLIENT)
public class TaintSwarmRenderer extends EntityRenderer<EntityTaintSwarm> {
    
    private static final ResourceLocation TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/taint_spore.png");
    
    public TaintSwarmRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
    }
    
    @Override
    public ResourceLocation getTextureLocation(EntityTaintSwarm entity) {
        return TEXTURE;
    }
    
    @Override
    public void render(EntityTaintSwarm entity, float entityYaw, float partialTicks, 
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        
        // Billboard rotation
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        
        Random random = new Random(entity.getId());
        float time = entity.tickCount + partialTicks;
        
        // Render multiple swirling particles
        for (int i = 0; i < 12; i++) {
            poseStack.pushPose();
            
            // Random offset for each particle
            float angle = (i / 12.0F) * Mth.TWO_PI + time * 0.1F;
            float radius = 0.3F + random.nextFloat() * 0.4F;
            float bobY = Mth.sin(time * 0.15F + i) * 0.2F;
            
            float offsetX = Mth.cos(angle) * radius;
            float offsetY = bobY + (random.nextFloat() - 0.5F) * 0.5F;
            float offsetZ = Mth.sin(angle) * radius;
            
            poseStack.translate(offsetX, offsetY, offsetZ);
            
            // Individual particle rotation
            poseStack.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360 + time * 5));
            
            float size = 0.15F + random.nextFloat() * 0.1F;
            
            renderParticle(poseStack, vertexConsumer, size, packedLight);
            
            poseStack.popPose();
        }
        
        poseStack.popPose();
        
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
    
    private void renderParticle(PoseStack poseStack, VertexConsumer vertexConsumer, 
                               float size, int packedLight) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        PoseStack.Pose normal = pose;
        
        // Purple taint color
        int r = 128;
        int g = 64;
        int b = 160;
        int a = 200;
        
        vertexConsumer.addVertex(matrix, -size, -size, 0.0F)
            .setColor(r, g, b, a)
            .setUv(0.0F, 1.0F)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setUv2((packedLight) & 0xFFFF, ((packedLight) >> 16) & 0xFFFF)
            .setNormal(normal, 0.0F, 1.0F, 0.0F)
            ;
        
        vertexConsumer.addVertex(matrix, size, -size, 0.0F)
            .setColor(r, g, b, a)
            .setUv(1.0F, 1.0F)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setUv2((packedLight) & 0xFFFF, ((packedLight) >> 16) & 0xFFFF)
            .setNormal(normal, 0.0F, 1.0F, 0.0F)
            ;
        
        vertexConsumer.addVertex(matrix, size, size, 0.0F)
            .setColor(r, g, b, a)
            .setUv(1.0F, 0.0F)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setUv2((packedLight) & 0xFFFF, ((packedLight) >> 16) & 0xFFFF)
            .setNormal(normal, 0.0F, 1.0F, 0.0F)
            ;
        
        vertexConsumer.addVertex(matrix, -size, size, 0.0F)
            .setColor(r, g, b, a)
            .setUv(0.0F, 0.0F)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setUv2((packedLight) & 0xFFFF, ((packedLight) >> 16) & 0xFFFF)
            .setNormal(normal, 0.0F, 1.0F, 0.0F)
            ;
    }
}




