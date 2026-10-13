package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.tiles.essentia.TileAlembic;

import java.awt.Color;

/**
 * Block entity renderer for the Alembic.
 * Renders the aspect filter label and connection nozzles to adjacent tubes.
 */
@OnlyIn(Dist.CLIENT)
public class AlembicRenderer implements BlockEntityRenderer<TileAlembic> {

    private static final ResourceLocation LABEL_TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/label.png");

    public AlembicRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TileAlembic tile, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        
        // Render aspect filter label if set
        if (tile.getAspectFilter() != null) {
            renderLabel(tile, tile.getAspectFilter(), poseStack, buffer, packedLight);
        }

        // Render connection nozzles to adjacent essentia transport blocks
        if (tile.getLevel() != null) {
            renderNozzles(tile, poseStack, buffer, packedLight);
        }
    }

    /**
     * Render the aspect filter label on the front of the alembic.
     */
    private void renderLabel(TileAlembic tile, Aspect aspect, PoseStack poseStack,
                             MultiBufferSource buffer, int packedLight) {
        
        poseStack.pushPose();

        // Move to block center
        poseStack.translate(0.5, 0.0, 0.5);

        // Rotate based on alembic facing. facing is the side the label sits on
        // (TC4 TileAlembic.facing); the label quad is built at -Z.
        int facing = tile.getFacing();
        switch (facing) {
            case 2 -> poseStack.mulPose(Axis.YP.rotationDegrees(0));    // North (-Z)
            case 3 -> poseStack.mulPose(Axis.YP.rotationDegrees(180));  // South (+Z)
            case 4 -> poseStack.mulPose(Axis.YP.rotationDegrees(90));   // West (-X)
            case 5 -> poseStack.mulPose(Axis.YP.rotationDegrees(270));  // East (+X)
        }

        // Move to label position on front face
        poseStack.translate(0, 0.5, -0.376);

        // Render label background
        VertexConsumer labelConsumer = buffer.getBuffer(RenderType.entityCutout(LABEL_TEXTURE));
        Matrix4f matrix = poseStack.last().pose();

        float size = 0.22f;
        labelConsumer.addVertex(matrix, -size, -size, 0).setColor(255, 255, 255, 255)
                .setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((packedLight) & 0xFFFF, ((packedLight) >> 16) & 0xFFFF).setNormal(0, 0, 1);
        labelConsumer.addVertex(matrix, size, -size, 0).setColor(255, 255, 255, 255)
                .setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((packedLight) & 0xFFFF, ((packedLight) >> 16) & 0xFFFF).setNormal(0, 0, 1);
        labelConsumer.addVertex(matrix, size, size, 0).setColor(255, 255, 255, 255)
                .setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((packedLight) & 0xFFFF, ((packedLight) >> 16) & 0xFFFF).setNormal(0, 0, 1);
        labelConsumer.addVertex(matrix, -size, size, 0).setColor(255, 255, 255, 255)
                .setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((packedLight) & 0xFFFF, ((packedLight) >> 16) & 0xFFFF).setNormal(0, 0, 1);

        // Render aspect icon on label
        poseStack.translate(0, 0, -0.001); // Slightly in front
        
        // Get aspect color and render colored quad
        Color color = new Color(aspect.getColor());
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;

        // Render aspect icon using the aspect's image
        VertexConsumer iconConsumer = buffer.getBuffer(RenderType.entityCutout(aspect.getImage()));
        Matrix4f iconMatrix = poseStack.last().pose();

        float iconSize = 0.15f;
        iconConsumer.addVertex(iconMatrix, -iconSize, -iconSize, 0).setColor(r, g, b, 1f)
                .setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((0x00F000F0) & 0xFFFF, ((0x00F000F0) >> 16) & 0xFFFF).setNormal(0, 0, 1);
        iconConsumer.addVertex(iconMatrix, iconSize, -iconSize, 0).setColor(r, g, b, 1f)
                .setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((0x00F000F0) & 0xFFFF, ((0x00F000F0) >> 16) & 0xFFFF).setNormal(0, 0, 1);
        iconConsumer.addVertex(iconMatrix, iconSize, iconSize, 0).setColor(r, g, b, 1f)
                .setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((0x00F000F0) & 0xFFFF, ((0x00F000F0) >> 16) & 0xFFFF).setNormal(0, 0, 1);
        iconConsumer.addVertex(iconMatrix, -iconSize, iconSize, 0).setColor(r, g, b, 1f)
                .setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((0x00F000F0) & 0xFFFF, ((0x00F000F0) >> 16) & 0xFFFF).setNormal(0, 0, 1);

        poseStack.popPose();
    }

    /**
     * Render nozzle connections to adjacent essentia transport blocks.
     */
    private void renderNozzles(TileAlembic tile, PoseStack poseStack,
                               MultiBufferSource buffer, int packedLight) {
        
        // Check each horizontal direction for connected tubes
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (!tile.canOutputTo(dir)) continue;

            BlockEntity te = tile.getLevel().getBlockEntity(tile.getBlockPos().relative(dir));
            if (te instanceof IEssentiaTransport transport) {
                if (transport.canInputFrom(dir.getOpposite())) {
                    renderNozzle(dir, poseStack, buffer, packedLight);
                }
            }
        }
    }

    /**
     * Render a single nozzle in the specified direction.
     */
    private void renderNozzle(Direction dir, PoseStack poseStack,
                              MultiBufferSource buffer, int packedLight) {
        
        poseStack.pushPose();

        // Move to block center
        poseStack.translate(0.5, 0.5, 0.5);

        // Rotate based on direction
        switch (dir) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
            case SOUTH -> { } // No rotation needed
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(-90));
            default -> { }
        }

        // Move to edge of block
        poseStack.translate(0, 0, 0.5);

        // Render a simple nozzle quad (placeholder - could use a model)
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.solid());
        Matrix4f matrix = poseStack.last().pose();

        float size = 0.125f;
        float depth = 0.0625f;
        
        // Nozzle is a small box extruding from the block
        // Front face
        vertexConsumer.addVertex(matrix, -size, -size, depth).setColor(100, 80, 60, 255)
                .setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((packedLight) & 0xFFFF, ((packedLight) >> 16) & 0xFFFF).setNormal(0, 0, 1);
        vertexConsumer.addVertex(matrix, size, -size, depth).setColor(100, 80, 60, 255)
                .setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((packedLight) & 0xFFFF, ((packedLight) >> 16) & 0xFFFF).setNormal(0, 0, 1);
        vertexConsumer.addVertex(matrix, size, size, depth).setColor(100, 80, 60, 255)
                .setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((packedLight) & 0xFFFF, ((packedLight) >> 16) & 0xFFFF).setNormal(0, 0, 1);
        vertexConsumer.addVertex(matrix, -size, size, depth).setColor(100, 80, 60, 255)
                .setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((packedLight) & 0xFFFF, ((packedLight) >> 16) & 0xFFFF).setNormal(0, 0, 1);

        poseStack.popPose();
    }
}




