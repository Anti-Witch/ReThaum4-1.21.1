package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.joml.Matrix4f;
import thaumcraft.common.tiles.crafting.TileCrucible;


/**
 * Block entity renderer for the Crucible.
 * Renders the water/essentia fluid inside the crucible.
 * The fluid color shifts from blue (water) to purple as more aspects are added.
 */
@OnlyIn(Dist.CLIENT)
public class CrucibleRenderer implements BlockEntityRenderer<TileCrucible> {

    public CrucibleRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TileCrucible tile, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        
        // Only render if there's fluid in the crucible
        if (tile.getTank().getFluidAmount() <= 0) return;

        float fluidHeight = tile.getFluidHeight();
        
        // Calculate color shift based on aspect content
        // More aspects = more purple/magic colored
        float aspectRatio = (float) tile.getAspects().visSize() / TileCrucible.MAX_ASPECTS;
        aspectRatio = Math.min(1.0f, aspectRatio);
        
        // Base water color to purple magic color
        float r, g, b;
        if (tile.isHeated()) {
            // When heated, shift from blue toward purple based on aspects
            r = 0.3f + aspectRatio * 0.5f;  // More red when more aspects
            g = 0.3f - aspectRatio * 0.2f;  // Less green when more aspects
            b = 0.9f;                        // Keep blue high
        } else {
            // When cold, just regular water color
            r = 0.2f;
            g = 0.4f;
            b = 0.8f;
        }
        
        float alpha = 0.85f;

        poseStack.pushPose();

        // Get water texture from the fluid type
        IClientFluidTypeExtensions fluidExtensions = IClientFluidTypeExtensions.of(Fluids.WATER);
        ResourceLocation stillTexture = fluidExtensions.getStillTexture();
        
        @SuppressWarnings("deprecation")
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(stillTexture);

        // Render the fluid surface
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.translucent());
        Matrix4f matrix = poseStack.last().pose();

        // Fluid surface bounds (inside the crucible walls)
        float minX = 0.125f;  // 2/16
        float maxX = 0.875f;  // 14/16
        float minZ = 0.125f;
        float maxZ = 0.875f;
        float y = fluidHeight;

        // UV coordinates from the sprite
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        // Use combined light from block (make it glow slightly when heated)
        int light = tile.isHeated() ? 0x00F000A0 : packedLight;

        // Top surface (Y+)
        vertexConsumer.addVertex(matrix, minX, y, minZ).setColor(r, g, b, alpha)
                .setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY)
                .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF).setNormal(0, 1, 0);
        vertexConsumer.addVertex(matrix, minX, y, maxZ).setColor(r, g, b, alpha)
                .setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY)
                .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF).setNormal(0, 1, 0);
        vertexConsumer.addVertex(matrix, maxX, y, maxZ).setColor(r, g, b, alpha)
                .setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY)
                .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF).setNormal(0, 1, 0);
        vertexConsumer.addVertex(matrix, maxX, y, minZ).setColor(r, g, b, alpha)
                .setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY)
                .setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF).setNormal(0, 1, 0);

        // Render bubble particles when heated (visual enhancement)
        // This would be done via particle system, not here
        
        poseStack.popPose();
    }
}




