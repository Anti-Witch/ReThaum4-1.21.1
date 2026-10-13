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
import thaumcraft.common.tiles.essentia.TileTubeBuffer;

/**
 * Block entity renderer for the Essentia Buffer tube.
 * Renders colored valve indicators on sides based on choke state.
 */
@OnlyIn(Dist.CLIENT)
public class TubeBufferRenderer implements BlockEntityRenderer<TileTubeBuffer> {

    private static final ResourceLocation TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/valve.png");

    private final TubeValveModel model;

    public TubeBufferRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new TubeValveModel(context.bakeLayer(TubeValveModel.LAYER_LOCATION));
    }

    @Override
    public void render(TileTubeBuffer tile, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        
        if (tile.getLevel() == null) return;
        
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        
        // Render choke indicators on each side
        for (Direction dir : Direction.values()) {
            // Only render if side is choked and open to a connectable tile
            if (tile.chokedSides[dir.ordinal()] != 0 && tile.openSides[dir.ordinal()]) {
                BlockEntity connectedTile = ThaumcraftApiHelper.getConnectableTile(
                        tile.getLevel(), tile.getBlockPos(), dir);
                
                if (connectedTile != null) {
                    poseStack.pushPose();
                    poseStack.translate(0.5, 0.5, 0.5);
                    
                    // Rotate to face the direction
                    applyDirectionRotation(poseStack, dir.getOpposite());
                    
                    // Determine color based on choke state
                    float r, g, b;
                    if (tile.chokedSides[dir.ordinal()] == 2) {
                        // No suction - red
                        r = 1.0f;
                        g = 0.3f;
                        b = 0.3f;
                    } else {
                        // Weak suction - blue
                        r = 0.3f;
                        g = 0.3f;
                        b = 1.0f;
                    }
                    
                    // Scale and position
                    poseStack.scale(0.0625f, 0.0625f, 0.0625f);
                    poseStack.scale(2.0f, 1.0f, 2.0f);
                    poseStack.translate(0, -0.5 / 0.0625, 0);  // Move down
                    
                    model.renderRod(poseStack, vertexConsumer, packedLight, packedOverlay, r, g, b, 1.0f);
                    
                    poseStack.popPose();
                }
            }
        }
    }
    
    /**
     * Apply rotation to face a direction.
     */
    private void applyDirectionRotation(PoseStack poseStack, Direction dir) {
        if (dir.getAxis() != Direction.Axis.Y) {
            poseStack.mulPose(Axis.YP.rotationDegrees(90));
        } else {
            poseStack.mulPose(Axis.XN.rotationDegrees(90));
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0f * dir.getStepY()));
        }
        
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0f * dir.getStepX()));
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0f * dir.getStepY()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0f * dir.getStepZ()));
    }
}




