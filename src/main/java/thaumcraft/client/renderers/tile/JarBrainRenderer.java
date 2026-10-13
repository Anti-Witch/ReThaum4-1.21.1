package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.client.models.block.BrainModel;
import thaumcraft.common.tiles.essentia.TileJarBrain;

/**
 * Block entity renderer for Brain in a Jar.
 * Renders a floating, rotating brain inside the jar.
 */
@OnlyIn(Dist.CLIENT)
public class JarBrainRenderer implements BlockEntityRenderer<TileJarBrain> {

    private static final ResourceLocation BRAIN_TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/brain2.png");

    private final BrainModel brainModel;

    public JarBrainRenderer(BlockEntityRendererProvider.Context context) {
        this.brainModel = new BrainModel(context.bakeLayer(BrainModel.LAYER_LOCATION));
    }

    @Override
    public void render(TileJarBrain tile, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        
        // Interpolate animation values for smooth motion
        float rotation = Mth.lerp(partialTicks, tile.brainRotationPrev, tile.brainRotation);
        float yOffset = Mth.lerp(partialTicks, tile.brainYPrev, tile.brainY);

        poseStack.pushPose();

        // Position brain in center of jar, slightly raised
        // The jar is roughly 1 block tall, brain should float in the middle-upper portion
        poseStack.translate(0.5, 0.35 + yOffset, 0.5);

        // Apply rotation around Y axis (slow spin)
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

        // Scale down the brain to fit inside the jar
        // Original model is designed at 1/16 scale, we need it smaller for the jar
        float scale = 0.03125f; // 1/32 scale
        poseStack.scale(scale, scale, scale);

        // Flip the model (models are often upside down)
        poseStack.mulPose(Axis.ZP.rotationDegrees(180));

        // Render the brain model
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(BRAIN_TEXTURE));
        
        // Use a slightly pink/flesh color tint
        float r = 1.0f;
        float g = 0.85f;
        float b = 0.85f;
        
        // 1.21.1: renderToBuffer takes an int ARGB color instead of 4 float components
        int _color = 0xFF000000 | ((int)(r * 255.0F) << 16) | ((int)(g * 255.0F) << 8) | ((int)(b * 255.0F));
        brainModel.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, _color);

        poseStack.popPose();
    }
}




