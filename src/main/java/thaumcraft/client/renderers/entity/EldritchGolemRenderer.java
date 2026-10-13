package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.client.models.entity.EldritchGolemModel;
import thaumcraft.common.entities.monster.boss.EntityEldritchGolem;

/**
 * Renderer for Eldritch Golem - a large armored boss entity.
 * Renders with transparency/blending for eldritch effect.
 */
@OnlyIn(Dist.CLIENT)
public class EldritchGolemRenderer extends MobRenderer<EntityEldritchGolem, EldritchGolemModel> {
    
    private static final ResourceLocation TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/eldritch_golem.png");
    
    public EldritchGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new EldritchGolemModel(context.bakeLayer(EldritchGolemModel.LAYER_LOCATION)), 0.7F);
    }
    
    @Override
    public ResourceLocation getTextureLocation(EntityEldritchGolem entity) {
        return TEXTURE;
    }
    
    @Override
    public void render(EntityEldritchGolem entity, float entityYaw, float partialTicks, 
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Render with slight transparency for eldritch effect
        poseStack.pushPose();
        
        // Use translucent render type
        RenderType renderType = RenderType.entityTranslucent(getTextureLocation(entity));
        VertexConsumer vertexConsumer = buffer.getBuffer(renderType);
        
        // Set up model animation
        this.model.setupAnim(entity, entity.walkAnimation.position(), entity.walkAnimation.speed(),
                entity.tickCount + partialTicks, entity.yHeadRot, entity.getXRot());
        
        // Render with slight transparency
        // 1.21.1: renderToBuffer takes an int ARGB color (0xE5FFFFFF = white @ 0.9 alpha)
        this.model.renderToBuffer(poseStack, vertexConsumer, packedLight,
                OverlayTexture.NO_OVERLAY, 0xE5FFFFFF);
        
        poseStack.popPose();
        
        // Render name tag and shadow normally
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
    
    @Override
    protected void scale(EntityEldritchGolem entity, PoseStack poseStack, float partialTicks) {
        // Eldritch golem is 70% larger than normal
        poseStack.scale(1.7F, 1.7F, 1.7F);
    }
}




