package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.common.tiles.devices.TileMirror;

/**
 * Block entity renderer for Magic Mirrors.
 * <p>
 * TC4's {@code TileMirrorRenderer} rendered the mirror "frame" (the block icon
 * as a 1/16 thick plate) plus a {@code mirrorpane}/{@code mirrorpanetrans} quad
 * on the block face; only the linked state added the swirling portal effect in
 * between. The static frame + pane now come from the block models
 * ({@code block/mirror_item[_on]}, {@code block/mirror_essentia[_on]}), so this
 * renderer only draws the end portal style tunnel of a linked mirror.
 */
@OnlyIn(Dist.CLIENT)
public class MirrorRenderer implements BlockEntityRenderer<TileMirror> {

    private static final ResourceLocation PORTAL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/tunnel.png");

    /** TC4: the portal quad was inset by 0.1875 blocks (10/16 across). */
    private static final float PORTAL_HALF_SIZE = 0.3125F;

    /** The pane quad of the block model sits at -0.5/16; the portal is just in front of it. */
    private static final float PORTAL_Z = -(0.5F / 16.0F) - 0.002F;

    public MirrorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TileMirror tile, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        // The frame and the (opaque or transparent) pane are part of the block model.
        if (!tile.linked || !isPlayerNearby(tile)) {
            return;
        }

        Direction facing = getFacing(tile.getBlockState());

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        applyRotation(poseStack, facing);
        renderActivePortal(poseStack, buffer, partialTicks, packedLight);
        poseStack.popPose();
    }

    private boolean isPlayerNearby(TileMirror tile) {
        if (tile.getLevel() == null) return false;
        return Minecraft.getInstance().player != null &&
               Minecraft.getInstance().player.distanceToSqr(
                       tile.getBlockPos().getX() + 0.5,
                       tile.getBlockPos().getY() + 0.5,
                       tile.getBlockPos().getZ() + 0.5) < 1024.0;
    }

    /**
     * Render the active portal with a swirling effect (TC4 drew sixteen layers of
     * tunnel.png/particlefield.png; this keeps a simplified version of that).
     */
    private void renderActivePortal(PoseStack poseStack, MultiBufferSource buffer,
                                    float partialTicks, int packedLight) {

        float time = (System.currentTimeMillis() % 100000L) / 1000.0f;

        for (int layer = 0; layer < 4; layer++) {
            poseStack.pushPose();

            float offset = 0.001f * layer;
            poseStack.translate(0, 0, PORTAL_Z - offset);

            // Each layer has a different rotation
            float rotation = time * (20 + layer * 10);
            poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));

            float alpha = 1.0f - (layer * 0.2f);
            float size = PORTAL_HALF_SIZE * (1.0f - layer * 0.05f);

            // Color shifts between layers
            float r = 0.3f + 0.2f * (float)Math.sin(time + layer);
            float g = 0.1f + 0.1f * (float)Math.sin(time * 1.3f + layer);
            float b = 0.5f + 0.3f * (float)Math.sin(time * 0.7f + layer);

            VertexConsumer vertexConsumer = buffer.getBuffer(
                    layer == 0 ? RenderType.entityTranslucent(PORTAL_TEXTURE)
                              : RenderType.entityTranslucentEmissive(PORTAL_TEXTURE));
            Matrix4f matrix = poseStack.last().pose();

            // Quad facing out of the block (the mirror's front); winding follows the
            // vanilla NORTH face order so back face culling keeps it visible.
            vertexConsumer.addVertex(matrix, size, size, 0).setColor(r, g, b, alpha)
                    .setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY)
                    .setUv2((packedLight) & 0xFFFF, ((packedLight) >> 16) & 0xFFFF).setNormal(0, 0, -1);
            vertexConsumer.addVertex(matrix, size, -size, 0).setColor(r, g, b, alpha)
                    .setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY)
                    .setUv2((packedLight) & 0xFFFF, ((packedLight) >> 16) & 0xFFFF).setNormal(0, 0, -1);
            vertexConsumer.addVertex(matrix, -size, -size, 0).setColor(r, g, b, alpha)
                    .setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY)
                    .setUv2((packedLight) & 0xFFFF, ((packedLight) >> 16) & 0xFFFF).setNormal(0, 0, -1);
            vertexConsumer.addVertex(matrix, -size, size, 0).setColor(r, g, b, alpha)
                    .setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY)
                    .setUv2((packedLight) & 0xFFFF, ((packedLight) >> 16) & 0xFFFF).setNormal(0, 0, -1);

            poseStack.popPose();
        }
    }

    /**
     * Rotates the local space so that -Z points away from the block's face.
     * The blockstate y rotations use the same convention (facing=east -> y 90).
     */
    private void applyRotation(PoseStack poseStack, Direction facing) {
        switch (facing) {
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(270));
            default -> { } // NORTH - default facing
        }
    }

    private Direction getFacing(BlockState state) {
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        return Direction.NORTH;
    }
}
