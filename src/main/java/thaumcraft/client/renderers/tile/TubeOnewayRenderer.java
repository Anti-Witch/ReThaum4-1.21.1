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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.client.models.block.TubeValveModel;
import thaumcraft.common.tiles.essentia.TileTubeOneway;

/**
 * Block entity renderer for the One-way Essentia Tube.
 * Renders a directional indicator showing flow direction.
 */
@OnlyIn(Dist.CLIENT)
public class TubeOnewayRenderer implements BlockEntityRenderer<TileTubeOneway> {

    private static final ResourceLocation TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/valve.png");

    private final TubeValveModel model;

    public TubeOnewayRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new TubeValveModel(context.bakeLayer(TubeValveModel.LAYER_LOCATION));
    }

    @Override
    public void render(TileTubeOneway tile, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        
        if (tile.getLevel() == null) return;
        
        // Check if there's a connectable tile in the opposite direction
        BlockEntity connectedTile = ThaumcraftApiHelper.getConnectableTile(
                tile.getLevel(), tile.getBlockPos(), tile.facing.getOpposite());
        
        if (connectedTile == null) return;
        
        Direction facing = tile.facing;
        
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        
        // Rotate based on facing direction
        applyFacingRotation(poseStack, facing);
        
        // Color: blue/cyan for flow indicator
        float r = 0.45f;
        float g = 0.5f;
        float b = 1.0f;
        
        // Scale and position
        poseStack.scale(0.0625f, 0.0625f, 0.0625f);
        poseStack.scale(2.0f, 2.0f, 2.0f);
        poseStack.translate(0, -0.32 / 0.0625 / 2, 0);  // Adjust position
        
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        model.renderRod(poseStack, vertexConsumer, packedLight, packedOverlay, r, g, b, 1.0f);
        
        poseStack.popPose();
    }
    
    /**
     * Apply rotation based on the facing direction.
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




