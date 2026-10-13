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
import thaumcraft.client.models.block.BellowsModel;
import thaumcraft.common.tiles.devices.TileBellows;

/**
 * Block entity renderer for the Bellows.
 * Renders an animated bellows model that inflates and deflates.
 */
@OnlyIn(Dist.CLIENT)
public class BellowsRenderer implements BlockEntityRenderer<TileBellows> {

    private static final ResourceLocation TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/bellows.png");

    private final BellowsModel model;

    public BellowsRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new BellowsModel(context.bakeLayer(BellowsModel.LAYER_LOCATION));
    }

    @Override
    public void render(TileBellows tile, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        
        poseStack.pushPose();

        // TC4 TileBellowsRenderer: planks/nozzle are drawn with the model origin at
        // (x+0.5, y-0.5, z+0.5) (the bag additionally sits 1 block higher, see setInflation).
        poseStack.translate(0.5, -0.5, 0.5);
        
        // Rotate based on facing direction
        Direction facing = TileBellows.getFacing(tile.getBlockState());
        applyRotation(poseStack, facing);

        // Scale to fit within block (model uses 1/16 units)
        poseStack.scale(0.0625f, 0.0625f, 0.0625f);

        // Set inflation state for animation
        model.setInflation(tile.inflation);

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        // 1.21.1: renderToBuffer takes an int ARGB color (0xFFFFFFFF = opaque white)
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, 0xFFFFFFFF);

        poseStack.popPose();
    }

    /**
     * Apply rotation based on the bellows facing direction.
     * Same table as TC4's {@code TileBellowsRenderer.translateFromOrientation}
     * (orientation ordinals: 0=DOWN, 1=UP, 2=NORTH, 3=SOUTH, 4=WEST, 5=EAST).
     * The model's nozzle points at +Z, so it ends up pointing at {@code facing}.
     */
    private void applyRotation(PoseStack poseStack, Direction facing) {
        switch (facing) {
            case DOWN -> {
                poseStack.translate(0, 1, -1);
                poseStack.mulPose(Axis.XP.rotationDegrees(90));
            }
            case UP -> {
                poseStack.translate(0, 1, 1);
                poseStack.mulPose(Axis.XP.rotationDegrees(270));
            }
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
            case SOUTH -> {
                // Default orientation, no rotation needed
            }
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(270));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
        }
    }
}




