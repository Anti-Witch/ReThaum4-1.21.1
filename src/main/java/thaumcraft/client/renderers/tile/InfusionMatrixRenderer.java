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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.common.tiles.crafting.TileInfusionMatrix;
import thaumcraft.init.ModBlocks;

import java.util.Random;

/**
 * Block entity renderer for the Infusion Matrix (Runic Matrix).
 *
 * Faithful port of TC4's TileInfusionMatrixRenderer:
 * - 8 floating cubes with a wobble that scales with the current stability
 * - additive purple glow overlay while the altar is active
 * - additive "halo" of light spikes while an infusion is running
 * - cube texture depends on the material of the altar pillars
 *   (arcane -> normal, ancient -> ancient, eldritch -> eldritch)
 */
@OnlyIn(Dist.CLIENT)
public class InfusionMatrixRenderer implements BlockEntityRenderer<TileInfusionMatrix> {

    private static final ResourceLocation TEXTURE_NORMAL =
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/infuser.png");
    private static final ResourceLocation TEXTURE_ANCIENT =
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/infuser_ancient.png");
    private static final ResourceLocation TEXTURE_ELDRITCH =
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/infuser_eldritch.png");

    public InfusionMatrixRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TileInfusionMatrix tile, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        float ticks = Minecraft.getInstance().player != null ?
                Minecraft.getInstance().player.tickCount + partialTicks : 0;

        poseStack.pushPose();

        // Move to block center
        poseStack.translate(0.5, 0.5, 0.5);

        // Apply startup rotation
        if (tile.startUp > 0) {
            poseStack.mulPose(Axis.YP.rotationDegrees(ticks % 360.0f * tile.startUp));
            poseStack.mulPose(Axis.XP.rotationDegrees(35.0f * tile.startUp));
            poseStack.mulPose(Axis.ZP.rotationDegrees(45.0f * tile.startUp));
        }

        // Calculate instability wobble (TC4: 1 + |negative stability| * 0.66 * progress)
        float instability = Math.min(6.0f, 1.0f + (tile.stability < 0 ? -tile.stability * 0.66f : 1.0f) *
                (Math.min(tile.craftCount, 50) / 50.0f));

        // Choose texture based on the pillar material in the back-left corner (TC4 behaviour)
        ResourceLocation texture = TEXTURE_NORMAL;
        if (tile.getLevel() != null) {
            Block pillar = tile.getLevel().getBlockState(tile.getWorldPosition().offset(-1, -2, -1)).getBlock();
            if (pillar == ModBlocks.ANCIENT_PILLAR.get()) {
                texture = TEXTURE_ANCIENT;
            } else if (pillar == ModBlocks.ELDRITCH_PILLAR.get()) {
                texture = TEXTURE_ELDRITCH;
            }
        }

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));

        // Render 8 cubes at corners
        for (int a = 0; a < 2; a++) {
            for (int b = 0; b < 2; b++) {
                for (int c = 0; c < 2; c++) {
                    // Calculate wobble offset during crafting
                    float wobbleX = 0, wobbleY = 0, wobbleZ = 0;
                    if (tile.active) {
                        wobbleX = Mth.sin((ticks + a * 10) / 15.0f) * 0.01f * tile.startUp * instability;
                        wobbleY = Mth.sin((ticks + b * 10) / 14.0f) * 0.01f * tile.startUp * instability;
                        wobbleZ = Mth.sin((ticks + c * 10) / 13.0f) * 0.01f * tile.startUp * instability;
                    }

                    int signA = (a == 0) ? -1 : 1;
                    int signB = (b == 0) ? -1 : 1;
                    int signC = (c == 0) ? -1 : 1;

                    poseStack.pushPose();
                    poseStack.translate(wobbleX + signA * 0.25f, wobbleY + signB * 0.25f, wobbleZ + signC * 0.25f);

                    // Rotation for visual variety
                    if (a > 0) poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
                    if (b > 0) poseStack.mulPose(Axis.YP.rotationDegrees(90.0f));
                    if (c > 0) poseStack.mulPose(Axis.ZP.rotationDegrees(90.0f));

                    poseStack.scale(0.45f, 0.45f, 0.45f);
                    renderCube(poseStack, vertexConsumer, packedLight, packedOverlay, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f);
                    poseStack.popPose();
                }
            }
        }

        // Render glow overlay when active
        if (tile.active) {
            VertexConsumer glowConsumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(texture));

            for (int a = 0; a < 2; a++) {
                for (int b = 0; b < 2; b++) {
                    for (int c = 0; c < 2; c++) {
                        float wobbleX = Mth.sin((ticks + a * 10) / 15.0f) * 0.01f * tile.startUp * instability;
                        float wobbleY = Mth.sin((ticks + b * 10) / 14.0f) * 0.01f * tile.startUp * instability;
                        float wobbleZ = Mth.sin((ticks + c * 10) / 13.0f) * 0.01f * tile.startUp * instability;

                        int signA = (a == 0) ? -1 : 1;
                        int signB = (b == 0) ? -1 : 1;
                        int signC = (c == 0) ? -1 : 1;

                        poseStack.pushPose();
                        poseStack.translate(wobbleX + signA * 0.25f, wobbleY + signB * 0.25f, wobbleZ + signC * 0.25f);

                        if (a > 0) poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
                        if (b > 0) poseStack.mulPose(Axis.YP.rotationDegrees(90.0f));
                        if (c > 0) poseStack.mulPose(Axis.ZP.rotationDegrees(90.0f));

                        poseStack.scale(0.45f, 0.45f, 0.45f);

                        // Pulsing purple glow (TC4 used the overlay half of the texture, tinted)
                        float pulse = (Mth.sin((ticks + a * 2 + b * 3 + c * 4) / 4.0f) * 0.1f + 0.2f) * tile.startUp;
                        renderCube(poseStack, glowConsumer, 0x00F000F0, packedOverlay, 0.8f, 0.1f, 1.0f, pulse, 0.5f);

                        poseStack.popPose();
                    }
                }
            }
        }

        poseStack.popPose();

        // Render halo effect during crafting
        if (tile.crafting && tile.craftCount > 0) {
            renderHalo(poseStack, buffer, tile.craftCount);
        }
    }

    /**
     * Render a simple cube using the TC4 ModelCube texture layout.
     *
     * The model texture is 64x64 and holds a 16px cube UV layout in its top half
     * and the matching additive overlay in its bottom half (vOffset = 0.5).
     */
    private void renderCube(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
                            float r, float g, float b, float a, float vOffset) {
        Matrix4f matrix = poseStack.last().pose();
        float size = 0.125f; // Half size
        float v0 = vOffset;
        float v1 = vOffset + 0.25f;
        float v2 = vOffset + 0.5f;

        // Top face (Y+)      : u 16-32, v 0-16
        addQuad(consumer, matrix, -size, size, -size, size, size, size, 0, 1, 0,
                r, g, b, a, packedLight, 0.25f, v0, 0.5f, v1);
        // Bottom face (Y-)   : u 32-48, v 0-16
        addQuad(consumer, matrix, -size, -size, size, size, -size, -size, 0, -1, 0,
                r, g, b, a, packedLight, 0.5f, v0, 0.75f, v1);
        // North face (Z-)    : u 16-32, v 16-32
        addQuad(consumer, matrix, size, -size, -size, -size, size, -size, 0, 0, -1,
                r, g, b, a, packedLight, 0.25f, v1, 0.5f, v2);
        // South face (Z+)    : u 48-64, v 16-32
        addQuad(consumer, matrix, -size, -size, size, size, size, size, 0, 0, 1,
                r, g, b, a, packedLight, 0.75f, v1, 1.0f, v2);
        // West face (X-)     : u 32-48, v 16-32
        addQuad(consumer, matrix, -size, -size, -size, -size, size, size, -1, 0, 0,
                r, g, b, a, packedLight, 0.5f, v1, 0.75f, v2);
        // East face (X+)     : u 0-16, v 16-32
        addQuad(consumer, matrix, size, -size, size, size, size, -size, 1, 0, 0,
                r, g, b, a, packedLight, 0.0f, v1, 0.25f, v2);
    }

    private void addQuad(VertexConsumer consumer, Matrix4f matrix,
                         float x1, float y1, float z1, float x2, float y2, float z2,
                         float nx, float ny, float nz, float r, float g, float b, float a,
                         int light, float u0, float v0, float u1, float v1) {
        if (ny != 0) {
            // Horizontal face
            consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF).setNormal(nx, ny, nz);
            consumer.addVertex(matrix, x1, y1, z2).setColor(r, g, b, a).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF).setNormal(nx, ny, nz);
            consumer.addVertex(matrix, x2, y1, z2).setColor(r, g, b, a).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF).setNormal(nx, ny, nz);
            consumer.addVertex(matrix, x2, y1, z1).setColor(r, g, b, a).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF).setNormal(nx, ny, nz);
        } else if (nz != 0) {
            // Z-facing face
            consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF).setNormal(nx, ny, nz);
            consumer.addVertex(matrix, x2, y1, z1).setColor(r, g, b, a).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF).setNormal(nx, ny, nz);
            consumer.addVertex(matrix, x2, y2, z1).setColor(r, g, b, a).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF).setNormal(nx, ny, nz);
            consumer.addVertex(matrix, x1, y2, z1).setColor(r, g, b, a).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF).setNormal(nx, ny, nz);
        } else {
            // X-facing face
            consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF).setNormal(nx, ny, nz);
            consumer.addVertex(matrix, x1, y1, z2).setColor(r, g, b, a).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF).setNormal(nx, ny, nz);
            consumer.addVertex(matrix, x1, y2, z2).setColor(r, g, b, a).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF).setNormal(nx, ny, nz);
            consumer.addVertex(matrix, x1, y2, z1).setColor(r, g, b, a).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((light) & 0xFFFF, ((light) >> 16) & 0xFFFF).setNormal(nx, ny, nz);
        }
    }

    /**
     * Render the energy halo effect during crafting.
     *
     * TC4 drew a triangle fan (GL_TRIANGLE_FAN) per spike with additive blending,
     * a white translucent centre and magenta transparent tips. We reproduce that
     * with three additive triangles per spike (RenderType.dragonRays).
     */
    private void renderHalo(PoseStack poseStack, MultiBufferSource buffer, int craftCount) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        int numSpikes = Minecraft.getInstance().options.graphicsMode().get().getId() >= 1 ? 20 : 10;
        float intensity = craftCount / 500.0f;
        float progress = Math.min(craftCount, 50) / 50.0f;
        if (progress <= 0.0f) {
            poseStack.popPose();
            return;
        }

        Random random = new Random(245L);
        VertexConsumer consumer = buffer.getBuffer(RenderType.dragonRays());
        float alpha = 1.0f - intensity;

        for (int i = 0; i < numSpikes; i++) {
            poseStack.pushPose();

            // TC4 applies six random rotations per spike, the last one spins with the craft progress
            poseStack.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360.0f));
            poseStack.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360.0f));
            poseStack.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360.0f));
            poseStack.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360.0f));
            poseStack.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360.0f));
            poseStack.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360.0f + intensity * 360.0f));

            float length = (random.nextFloat() * 20.0f + 5.0f) * progress / 20.0f;
            float width = (random.nextFloat() * 2.0f + 1.0f) * progress / 20.0f;

            Matrix4f matrix = poseStack.last().pose();
            float x1 = -0.866f * width, z1 = -0.5f * width;
            float x2 = 0.866f * width, z2 = -0.5f * width;
            float x3 = 0.0f, z3 = width;

            // Triangle fan: (c, v1, v2), (c, v2, v3), (c, v3, v1)
            addFanTriangle(consumer, matrix, 0, 0, 0, x1, length, z1, x2, length, z2, alpha);
            addFanTriangle(consumer, matrix, 0, 0, 0, x2, length, z2, x3, length, z3, alpha);
            addFanTriangle(consumer, matrix, 0, 0, 0, x3, length, z3, x1, length, z1, alpha);

            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private void addFanTriangle(VertexConsumer consumer, Matrix4f matrix,
                                float cx, float cy, float cz,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float alpha) {
        consumer.addVertex(matrix, cx, cy, cz).setColor(1.0f, 1.0f, 1.0f, alpha);
        consumer.addVertex(matrix, x1, y1, z1).setColor(0.8f, 0.0f, 1.0f, 0.0f);
        consumer.addVertex(matrix, x2, y2, z2).setColor(0.8f, 0.0f, 1.0f, 0.0f);
    }

    @Override
    public boolean shouldRenderOffScreen(TileInfusionMatrix tile) {
        return tile.crafting;
    }
}
