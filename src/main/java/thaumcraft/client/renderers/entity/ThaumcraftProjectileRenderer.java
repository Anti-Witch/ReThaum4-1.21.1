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
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;

/**
 * Generic renderer for Thaumcraft projectiles.
 * Renders as a glowing billboard sprite.
 * Can be configured with different textures and colors.
 */
@OnlyIn(Dist.CLIENT)
public class ThaumcraftProjectileRenderer<T extends Entity> extends EntityRenderer<T> {
    
    private final ResourceLocation texture;
    private final float size;
    private final int color;
    private final boolean emissive;
    
    public ThaumcraftProjectileRenderer(EntityRendererProvider.Context context, 
                                        ResourceLocation texture, float size, int color, boolean emissive) {
        super(context);
        this.texture = texture;
        this.size = size;
        this.color = color;
        this.emissive = emissive;
        this.shadowRadius = 0.0F;
    }
    
    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }
    
    @Override
    public void render(T entity, float entityYaw, float partialTicks, 
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        
        // Billboard rotation
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        
        // Spin animation
        float spin = (entity.tickCount + partialTicks) * 10.0F;
        poseStack.mulPose(Axis.ZP.rotationDegrees(spin));
        
        // Get render type
        RenderType renderType = emissive ? 
                RenderType.entityTranslucentEmissive(texture) : 
                RenderType.entityTranslucent(texture);
        VertexConsumer vertexConsumer = buffer.getBuffer(renderType);
        
        // Extract color components
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = 255;
        
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        PoseStack.Pose normal = pose;
        
        int light = emissive ? 0xF000F0 : packedLight;
        
        // Render quad
        vertexConsumer.addVertex(matrix, -size, -size, 0.0F)
            .setColor(r, g, b, a)
            .setUv(0.0F, 1.0F)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF)
            .setNormal(normal, 0.0F, 1.0F, 0.0F)
            ;
        
        vertexConsumer.addVertex(matrix, size, -size, 0.0F)
            .setColor(r, g, b, a)
            .setUv(1.0F, 1.0F)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF)
            .setNormal(normal, 0.0F, 1.0F, 0.0F)
            ;
        
        vertexConsumer.addVertex(matrix, size, size, 0.0F)
            .setColor(r, g, b, a)
            .setUv(1.0F, 0.0F)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF)
            .setNormal(normal, 0.0F, 1.0F, 0.0F)
            ;
        
        vertexConsumer.addVertex(matrix, -size, size, 0.0F)
            .setColor(r, g, b, a)
            .setUv(0.0F, 0.0F)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF)
            .setNormal(normal, 0.0F, 1.0F, 0.0F)
            ;
        
        poseStack.popPose();
        
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
    
    /**
     * Factory for creating common projectile renderers.
     */
    public static class Factory {
        private static final ResourceLocation ORB_TEXTURE = 
                ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/vcrystal.png");
        private static final ResourceLocation DART_TEXTURE = 
                ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/block/blank.png");
        
        public static <T extends Entity> ThaumcraftProjectileRenderer<T> orb(
                EntityRendererProvider.Context context, int color) {
            return new ThaumcraftProjectileRenderer<>(context, ORB_TEXTURE, 0.25F, color, true);
        }
        
        public static <T extends Entity> ThaumcraftProjectileRenderer<T> dart(
                EntityRendererProvider.Context context) {
            return new ThaumcraftProjectileRenderer<>(context, DART_TEXTURE, 0.15F, 0xFFFFFF, false);
        }
        
        public static <T extends Entity> ThaumcraftProjectileRenderer<T> magic(
                EntityRendererProvider.Context context, int color) {
            return new ThaumcraftProjectileRenderer<>(context, ORB_TEXTURE, 0.2F, color, true);
        }
    }
}




