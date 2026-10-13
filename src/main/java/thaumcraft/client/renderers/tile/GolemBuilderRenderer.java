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
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.common.tiles.crafting.TileGolemBuilder;

/**
 * Block entity renderer for the Golem Builder.
 * Renders the press mechanism and lava pool.
 */
@OnlyIn(Dist.CLIENT)
public class GolemBuilderRenderer implements BlockEntityRenderer<TileGolemBuilder> {

    private static final ResourceLocation TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/block/golembuilder.png");

    public GolemBuilderRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TileGolemBuilder tile, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        
        if (tile.getLevel() == null) return;
        
        // Get facing direction
        Direction facing = Direction.NORTH;
        if (tile.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            facing = tile.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        
        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        
        // Rotate based on facing
        float rotation = switch (facing) {
            case EAST -> 270.0f;
            case WEST -> 90.0f;
            case SOUTH -> 180.0f;
            case NORTH -> 0.0f;
            default -> 0.0f;
        };
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        
        // Render the press mechanism
        renderPress(tile, poseStack, buffer, packedLight, packedOverlay);
        
        // Render the lava pool
        renderLavaPool(poseStack, buffer, packedLight);
        
        poseStack.popPose();
    }
    
    /**
     * Render the press mechanism.
     */
    private void renderPress(TileGolemBuilder tile, PoseStack poseStack, 
                             MultiBufferSource buffer, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        
        // Calculate press offset based on animation
        float pressHeight = tile.pressAnimation;
        double offset = Math.sin(Math.toRadians(pressHeight)) * 0.625;
        poseStack.translate(0, -offset, 0);
        
        // Render press as a simple box (placeholder - original uses OBJ model)
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutout(TEXTURE));
        Matrix4f matrix = poseStack.last().pose();
        
        // Press is a flat plate that moves down
        float width = 0.375f;
        float height = 0.125f;
        float depth = 0.375f;
        float yPos = 0.875f;
        
        // Top face
        vertexConsumer.addVertex(matrix, -width, yPos, -depth).setColor(150, 150, 150, 255)
                .setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((packedLight) & 0xFFFF, ((packedLight) >> 16) & 0xFFFF).setNormal(0, 1, 0);
        vertexConsumer.addVertex(matrix, -width, yPos, depth).setColor(150, 150, 150, 255)
                .setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((packedLight) & 0xFFFF, ((packedLight) >> 16) & 0xFFFF).setNormal(0, 1, 0);
        vertexConsumer.addVertex(matrix, width, yPos, depth).setColor(150, 150, 150, 255)
                .setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((packedLight) & 0xFFFF, ((packedLight) >> 16) & 0xFFFF).setNormal(0, 1, 0);
        vertexConsumer.addVertex(matrix, width, yPos, -depth).setColor(150, 150, 150, 255)
                .setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((packedLight) & 0xFFFF, ((packedLight) >> 16) & 0xFFFF).setNormal(0, 1, 0);
        
        // Bottom face
        vertexConsumer.addVertex(matrix, -width, yPos - height, depth).setColor(100, 100, 100, 255)
                .setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((packedLight) & 0xFFFF, ((packedLight) >> 16) & 0xFFFF).setNormal(0, -1, 0);
        vertexConsumer.addVertex(matrix, -width, yPos - height, -depth).setColor(100, 100, 100, 255)
                .setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((packedLight) & 0xFFFF, ((packedLight) >> 16) & 0xFFFF).setNormal(0, -1, 0);
        vertexConsumer.addVertex(matrix, width, yPos - height, -depth).setColor(100, 100, 100, 255)
                .setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((packedLight) & 0xFFFF, ((packedLight) >> 16) & 0xFFFF).setNormal(0, -1, 0);
        vertexConsumer.addVertex(matrix, width, yPos - height, depth).setColor(100, 100, 100, 255)
                .setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((packedLight) & 0xFFFF, ((packedLight) >> 16) & 0xFFFF).setNormal(0, -1, 0);
        
        poseStack.popPose();
    }
    
    /**
     * Render the lava pool at the bottom.
     */
    private void renderLavaPool(PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(-0.3125, 0.625, 0.3125 + 1.0);
        poseStack.mulPose(Axis.XN.rotationDegrees(90));
        
        // Get lava texture from the block atlas
        TextureAtlasSprite lavaSprite = Minecraft.getInstance().getBlockRenderer()
                .getBlockModelShaper().getTexture(Blocks.LAVA.defaultBlockState(), 
                        Minecraft.getInstance().level, null);
        
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entitySolid(TextureAtlas.LOCATION_BLOCKS));
        Matrix4f matrix = poseStack.last().pose();
        
        float size = 0.625f;
        int fullBright = 0x00F000F0; // Full brightness for lava
        
        float u0 = lavaSprite.getU0();
        float u1 = lavaSprite.getU1();
        float v0 = lavaSprite.getV0();
        float v1 = lavaSprite.getV1();
        
        vertexConsumer.addVertex(matrix, 0, 0, 0).setColor(255, 255, 255, 255)
                .setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((fullBright) & 0xFFFF, ((fullBright) >> 16) & 0xFFFF).setNormal(0, 0, 1);
        vertexConsumer.addVertex(matrix, size, 0, 0).setColor(255, 255, 255, 255)
                .setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((fullBright) & 0xFFFF, ((fullBright) >> 16) & 0xFFFF).setNormal(0, 0, 1);
        vertexConsumer.addVertex(matrix, size, size, 0).setColor(255, 255, 255, 255)
                .setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((fullBright) & 0xFFFF, ((fullBright) >> 16) & 0xFFFF).setNormal(0, 0, 1);
        vertexConsumer.addVertex(matrix, 0, size, 0).setColor(255, 255, 255, 255)
                .setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((fullBright) & 0xFFFF, ((fullBright) >> 16) & 0xFFFF).setNormal(0, 0, 1);
        
        poseStack.popPose();
    }
}




