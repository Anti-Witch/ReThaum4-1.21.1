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
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import thaumcraft.common.tiles.misc.TileHole;
import thaumcraft.init.ModBlocks;

/**
 * Block entity renderer for the Portable Hole.
 * Renders an end portal-like void effect on faces adjacent to solid blocks.
 */
@OnlyIn(Dist.CLIENT)
public class HoleRenderer implements BlockEntityRenderer<TileHole> {

    private static final ResourceLocation END_PORTAL_TEXTURE = 
            ResourceLocation.withDefaultNamespace("textures/entity/end_portal.png");

    public HoleRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TileHole tile, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        
        if (tile.getLevel() == null) return;
        
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        
        // Render void face on each side adjacent to a solid block
        for (Direction face : Direction.values()) {
            BlockState adjacentState = tile.getLevel().getBlockState(tile.getBlockPos().relative(face));
            
            // Only render the void face if the adjacent block is opaque and not another hole
            if (adjacentState.isSolidRender(tile.getLevel(), tile.getBlockPos().relative(face)) 
                    && !adjacentState.is(ModBlocks.HOLE.get())) {
                
                poseStack.pushPose();
                
                // Rotate to face the correct direction
                switch (face) {
                    case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
                    case UP -> poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                    case NORTH -> { /* Default facing */ }
                    case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
                    case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
                    case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(-90));
                }
                
                // Move to the face
                poseStack.translate(0, 0, 0.499);
                
                renderVoidFace(poseStack, buffer);
                
                poseStack.popPose();
            }
        }
        
        poseStack.popPose();
    }
    
    /**
     * Render a single void/portal-like face.
     */
    private void renderVoidFace(PoseStack poseStack, MultiBufferSource buffer) {
        // Use end portal render type for the starfield effect
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.endPortal());
        Matrix4f matrix = poseStack.last().pose();
        
        float half = 0.5f;
        
        // Render quad facing +Z (towards the solid block)
        // End portal render type handles its own texturing
        vertexConsumer.addVertex(matrix, -half, -half, 0);
        vertexConsumer.addVertex(matrix, half, -half, 0);
        vertexConsumer.addVertex(matrix, half, half, 0);
        vertexConsumer.addVertex(matrix, -half, half, 0);
    }
    
    /**
     * Alternative method using regular texture for simpler effect.
     */
    private void renderSimpleVoidFace(PoseStack poseStack, MultiBufferSource buffer) {
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entitySolid(END_PORTAL_TEXTURE));
        Matrix4f matrix = poseStack.last().pose();
        
        float half = 0.5f;
        int fullLight = 0x00F000F0; // Full brightness
        
        vertexConsumer.addVertex(matrix, -half, -half, 0).setColor(0, 0, 0, 255)
                .setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((fullLight) & 0xFFFF, ((fullLight) >> 16) & 0xFFFF).setNormal(0, 0, 1);
        vertexConsumer.addVertex(matrix, half, -half, 0).setColor(0, 0, 0, 255)
                .setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((fullLight) & 0xFFFF, ((fullLight) >> 16) & 0xFFFF).setNormal(0, 0, 1);
        vertexConsumer.addVertex(matrix, half, half, 0).setColor(0, 0, 0, 255)
                .setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((fullLight) & 0xFFFF, ((fullLight) >> 16) & 0xFFFF).setNormal(0, 0, 1);
        vertexConsumer.addVertex(matrix, -half, half, 0).setColor(0, 0, 0, 255)
                .setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((fullLight) & 0xFFFF, ((fullLight) >> 16) & 0xFFFF).setNormal(0, 0, 1);
    }
}




