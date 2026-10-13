package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.client.models.block.TubeValveModel;
import thaumcraft.common.tiles.essentia.TileTubeValve;

/**
 * Block entity renderer for the Essentia Valve tube.
 * Renders a spinning valve mechanism that opens/closes.
 */
@OnlyIn(Dist.CLIENT)
public class TubeValveRenderer implements BlockEntityRenderer<TileTubeValve> {

    private static final ResourceLocation TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/valve.png");

    private final TubeValveModel model;

    public TubeValveRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new TubeValveModel(context.bakeLayer(TubeValveModel.LAYER_LOCATION));
    }

    @Override
    public void render(TileTubeValve tile, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        
        if (tile.getLevel() == null) return;
        
        Direction facing = tile.facing;
        
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        
        // Rotate based on facing direction
        applyFacingRotation(poseStack, facing);
        
        // Animate valve rotation when opening/closing
        float rotation = tile.rotation;
        poseStack.mulPose(Axis.YP.rotationDegrees(-rotation * 1.5f));
        
        // Move valve position based on rotation state (down when closed)
        float offset = -0.03f - (rotation / 360.0f) * 0.09f;
        poseStack.translate(0, offset, 0);
        
        // Scale for model size (model uses 1/16 scale internally)
        poseStack.scale(0.0625f, 0.0625f, 0.0625f);
        
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        
        // Render ring
        model.renderRing(poseStack, vertexConsumer, packedLight, packedOverlay, 1.0f, 1.0f, 1.0f, 1.0f);
        
        // Scale down rod slightly
        poseStack.scale(0.75f, 1.0f, 0.75f);
        model.renderRod(poseStack, vertexConsumer, packedLight, packedOverlay, 1.0f, 1.0f, 1.0f, 1.0f);
        
        poseStack.popPose();
    }
    
    /**
     * Apply rotation based on the facing direction of the valve.
     */
    private void applyFacingRotation(PoseStack poseStack, Direction facing) {
        if (facing.getAxis() != Direction.Axis.Y) {
            poseStack.mulPose(Axis.YP.rotationDegrees(90));
        } else {
            poseStack.mulPose(Axis.XN.rotationDegrees(90));
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0f * facing.getStepY()));
        }
        
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0f * facing.getStepX()));
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0f * facing.getStepY()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0f * facing.getStepZ()));
    }
}




