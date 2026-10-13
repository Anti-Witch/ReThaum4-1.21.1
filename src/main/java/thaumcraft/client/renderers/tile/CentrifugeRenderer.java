package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.client.models.block.CentrifugeModel;
import thaumcraft.common.tiles.essentia.TileCentrifuge;

/**
 * Block entity renderer for the Centrifuge.
 * Renders a custom model with a spinning inner mechanism.
 */
@OnlyIn(Dist.CLIENT)
public class CentrifugeRenderer implements BlockEntityRenderer<TileCentrifuge> {

    private static final ResourceLocation TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/centrifuge.png");

    private final CentrifugeModel model;

    public CentrifugeRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new CentrifugeModel(context.bakeLayer(CentrifugeModel.LAYER_LOCATION));
    }

    @Override
    public void render(TileCentrifuge tile, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        
        poseStack.pushPose();

        // Move to block center
        poseStack.translate(0.5, 0.5, 0.5);
        
        // Scale down to fit within block (model is in model space at 1/16 scale factor)
        poseStack.scale(0.0625f, 0.0625f, 0.0625f);

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));

        // Render static parts (top and bottom)
        model.renderStaticParts(poseStack, vertexConsumer, packedLight, packedOverlay, 1.0f, 1.0f, 1.0f, 1.0f);

        // Calculate interpolated rotation for smooth animation
        float rotation = Mth.lerp(partialTicks, tile.spinLast, tile.spin);
        
        // Apply rotation to the spinny bit
        model.setSpinRotation((float) Math.toRadians(rotation));
        
        // Render the spinning mechanism
        model.renderSpinnyBit(poseStack, vertexConsumer, packedLight, packedOverlay, 1.0f, 1.0f, 1.0f, 1.0f);

        poseStack.popPose();
    }
}




