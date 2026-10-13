package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.client.models.entity.ArcaneBoreModel;
import thaumcraft.common.entities.construct.EntityArcaneBore;

/**
 * Renderer for the Arcane Bore mining construct.
 * 
 * Features:
 * - Rotating head that aims at mining targets
 * - Mining beam effect when actively digging
 * - Glow effect on the front crystal
 */
@OnlyIn(Dist.CLIENT)
public class ArcaneBoreRenderer extends MobRenderer<EntityArcaneBore, ArcaneBoreModel> {
    
    private static final ResourceLocation TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/bore.png");
    
    private static final ResourceLocation BEAM_TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/beam1.png");
    
    public ArcaneBoreRenderer(EntityRendererProvider.Context context) {
        super(context, new ArcaneBoreModel(context.bakeLayer(ArcaneBoreModel.LAYER_LOCATION)), 0.5F);
    }
    
    @Override
    public ResourceLocation getTextureLocation(EntityArcaneBore entity) {
        return TEXTURE;
    }
    
    @Override
    public void render(EntityArcaneBore entity, float entityYaw, float partialTicks, 
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Reset yaw offset (bore rotates head, not body)
        entity.yBodyRot = 0.0F;
        entity.yBodyRotO = 0.0F;
        
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        
        // Render mining beam if actively digging
        if (entity.isClientDigging() && entity.isActive() && entity.hasValidInventory()) {
            renderMiningBeam(entity, partialTicks, poseStack, buffer, packedLight);
        }
    }
    
    /**
     * Renders the mining beam effect when the bore is digging.
     */
    private void renderMiningBeam(EntityArcaneBore entity, float partialTicks, 
                                  PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        
        // Calculate beam start position (from the front of the bore)
        float yaw = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        
        // Offset from center to tip
        Vec3 offset = new Vec3(0.5, 0.075, 0.0);
        offset = rotateAroundZ(offset, pitch * Mth.DEG_TO_RAD);
        offset = rotateAroundY(offset, -((yaw + 90.0F) * Mth.DEG_TO_RAD));
        
        poseStack.translate(offset.x, entity.getEyeHeight() + offset.y, offset.z);
        
        // Render glow at beam origin
        renderGlow(poseStack, buffer, entity.tickCount, partialTicks);
        
        // Render the beam
        renderBeam(poseStack, buffer, entity, partialTicks, packedLight);
        
        poseStack.popPose();
    }
    
    /**
     * Renders a glow effect at the beam origin.
     */
    private void renderGlow(PoseStack poseStack, MultiBufferSource buffer, int tickCount, float partialTicks) {
        // TODO: Implement proper glow rendering using node texture
        // This is a placeholder - the full implementation would use UtilsFX.renderBillboardQuad
    }
    
    /**
     * Renders the mining beam itself.
     */
    private void renderBeam(PoseStack poseStack, MultiBufferSource buffer, 
                           EntityArcaneBore entity, float partialTicks, int packedLight) {
        // Beam parameters
        float beamLength = 5.0F;
        float beamWidth = 0.15F;
        float opacity = 0.4F;
        
        // Animation
        float rotation = (entity.level().getGameTime() % 72L) * 5L + 5.0F * partialTicks;
        float scroll = -(entity.tickCount + partialTicks) * 0.2F;
        scroll = scroll - Mth.floor(scroll);
        
        // Get beam rotation
        float yaw = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        
        poseStack.pushPose();
        
        // Rotate to face correct direction
        poseStack.mulPose(Axis.XN.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.ZN.rotationDegrees(180.0F + yaw));
        poseStack.mulPose(Axis.XN.rotationDegrees(pitch));
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        
        // Get render type for beam
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(BEAM_TEXTURE));
        
        // Draw 3 beam quads rotated 60 degrees apart
        for (int i = 0; i < 3; i++) {
            poseStack.mulPose(Axis.YP.rotationDegrees(60.0F));
            
            PoseStack.Pose pose = poseStack.last();
            
            // Beam quad vertices
            float u0 = 0.0F;
            float u1 = 1.0F;
            float v0 = scroll + i / 3.0F;
            float v1 = beamLength + v0;
            
            // Green tint (0, 1, 0.4)
            int r = (int)(0.0F * 255);
            int g = (int)(1.0F * 255);
            int b = (int)(0.4F * 255);
            int a = (int)(opacity * 255);
            
            // Draw quad
            vertexConsumer.addVertex(pose.pose(), 0.0F, beamLength, 0.0F)
                .setColor(r, g, b, a)
                .setUv(u1, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setUv2((packedLight) & 0xFFFF, ((packedLight) >> 16) & 0xFFFF)
                .setNormal(pose, 0.0F, 1.0F, 0.0F)
                ;
            
            vertexConsumer.addVertex(pose.pose(), -beamWidth, 0.0F, 0.0F)
                .setColor(r, g, b, a)
                .setUv(u1, v0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setUv2((packedLight) & 0xFFFF, ((packedLight) >> 16) & 0xFFFF)
                .setNormal(pose, 0.0F, 1.0F, 0.0F)
                ;
            
            vertexConsumer.addVertex(pose.pose(), beamWidth, 0.0F, 0.0F)
                .setColor(r, g, b, a)
                .setUv(u0, v0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setUv2((packedLight) & 0xFFFF, ((packedLight) >> 16) & 0xFFFF)
                .setNormal(pose, 0.0F, 1.0F, 0.0F)
                ;
            
            vertexConsumer.addVertex(pose.pose(), 0.0F, beamLength, 0.0F)
                .setColor(r, g, b, a)
                .setUv(u0, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setUv2((packedLight) & 0xFFFF, ((packedLight) >> 16) & 0xFFFF)
                .setNormal(pose, 0.0F, 1.0F, 0.0F)
                ;
        }
        
        poseStack.popPose();
    }
    
    /**
     * Rotate a vector around the Y axis.
     */
    private static Vec3 rotateAroundY(Vec3 vec, float angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x = vec.x * cos + vec.z * sin;
        double z = -vec.x * sin + vec.z * cos;
        return new Vec3(x, vec.y, z);
    }
    
    /**
     * Rotate a vector around the Z axis.
     */
    private static Vec3 rotateAroundZ(Vec3 vec, float angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x = vec.x * cos - vec.y * sin;
        double y = vec.x * sin + vec.y * cos;
        return new Vec3(x, y, vec.z);
    }
    
    @Override
    protected boolean shouldShowName(EntityArcaneBore entity) {
        return false; // Bores don't show names
    }
}




