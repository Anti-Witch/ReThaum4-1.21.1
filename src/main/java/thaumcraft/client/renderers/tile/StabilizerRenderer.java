package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.common.tiles.devices.TileStabilizer;

/**
 * Block entity renderer for the Node Stabilizer, a 1:1 port of TC4's
 * {@code thaumcraft.client.renderers.tile.TileNodeStabilizerRenderer}.
 * <p>
 * TC4 loaded {@code textures/models/node_stabilizer.obj} with
 * {@code AdvancedModelLoader} and rendered the {@code piston} part four times
 * (once per 90 degrees, tilted 45 degrees outwards), sliding it up along its own
 * axis by {@code count / 100}. Each piston was drawn twice: once with
 * {@code node_stabilizer.png} at the block's light, then with
 * {@code node_stabilizer_over.png} at a boosted light level (the "charged"
 * glow). Finally a camera facing {@code node_bubble.png} quad was drawn above
 * the block.
 * <p>
 * The port cannot use {@code AdvancedModelLoader}; the {@code piston} group was
 * therefore split out of the original OBJ into
 * {@code models/obj/node_stabilizer_piston.obj} (geometry and UVs unchanged) and
 * is baked through NeoForge's {@code neoforge:obj} loader. The static base
 * ({@code lock} group) is a plain JSON model
 * ({@code models/block/stabilizer.json}).
 * <p>
 * TC4's {@code count} (0..37, driven by the node above the stabilizer) maps to
 * the port's {@code TileStabilizer#getEnergy()} (0..{@code getCapacity()}).
 */
@OnlyIn(Dist.CLIENT)
public class StabilizerRenderer implements BlockEntityRenderer<TileStabilizer> {

    /** Side loaded copies of the OBJ piston (base + overlay textures). */
    public static final ResourceLocation PISTON_MODEL_ID =
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "item/stabilizer_piston_model");
    public static final ModelResourceLocation PISTON_MODEL =
            ModelResourceLocation.standalone(PISTON_MODEL_ID);
    public static final ResourceLocation PISTON_OVER_MODEL_ID =
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "item/stabilizer_piston_over_model");
    public static final ModelResourceLocation PISTON_OVER_MODEL =
            ModelResourceLocation.standalone(PISTON_OVER_MODEL_ID);

    /** TC4: textures/misc/node_bubble.png */
    private static final ResourceLocation BUBBLE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/node_bubble.png");

    /** TC4: the piston slides up by {@code count / 100} with {@code count} maxing out at 37. */
    private static final float MAX_EXTENSION = 0.37F;

    /** Number of pistons around the base (TC4 hard coded four 90 degree steps). */
    private static final int PISTONS = 4;

    public StabilizerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TileStabilizer tile, float partialTicks, PoseStack pose, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        int energy = tile.getEnergy();
        int capacity = Math.max(1, tile.getCapacity());
        float charge = Mth.clamp((float) energy / capacity, 0.0F, 1.0F);
        float extension = charge * MAX_EXTENSION;

        float time = (tile.getLevel() != null ? tile.getLevel().getGameTime() : 0L) + partialTicks;

        for (int a = 0; a < PISTONS; a++) {
            pose.pushPose();
            // TC4: translate(x + 0.5, y, z + 0.5); rotate(90, -1, 0, 0);
            //      rotate(90 * a, 0, 0, 1); rotate(45, 0, 1, 0); translate(0, 0, count / 100)
            pose.translate(0.5F, 0.0F, 0.5F);
            pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(90.0F * a));
            pose.mulPose(Axis.YP.rotationDegrees(45.0F));
            pose.translate(0.0F, 0.0F, extension);

            // TC4: piston with node_stabilizer.png at the block's own light
            renderModel(pose, buffer, PISTON_MODEL, packedLight, packedOverlay);

            // TC4 always drew the overlay pass, its lightmap brightness scaled by count
            // (50 + 170 * count / 37 * pulse), so the coils glow brighter as it charges.
            float scale = Mth.sin((time + a * 5.0F) / 3.0F) * 0.1F + 0.9F;
            int bright = (int) (50.0F + 170.0F * charge * scale);
            renderModel(pose, buffer, PISTON_OVER_MODEL, packedLight(bright), packedOverlay);
            pose.popPose();
        }

        // TC4: a soft, camera facing bubble above the block while charged
        if (energy > 0) {
            renderBubble(pose, buffer, charge, time);
        }
    }

    /** TC4: {@code OpenGlHelper.setLightmapTextureCoords} with a raw 0..255 brightness. */
    private static int packedLight(int brightness) {
        int level = Mth.clamp(brightness >> 4, 0, 15);
        return LightTexture.pack(level, level);
    }

    /**
     * Draws a side loaded model's quads with the current pose. The model is baked into
     * the block atlas (see {@code assets/minecraft/atlases/blocks.json}), so the block
     * cutout sheet is used - exactly like {@code ThaumometerItemRenderer} does it.
     */
    private static void renderModel(PoseStack pose, MultiBufferSource buffer, ModelResourceLocation location,
                                    int packedLight, int packedOverlay) {
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(location);
        if (model == null || model.isCustomRenderer()) {
            return;
        }
        RenderType renderType = Sheets.cutoutBlockSheet();
        VertexConsumer vc = buffer.getBuffer(renderType);
        RandomSource rand = RandomSource.create(42L);
        for (Direction direction : Direction.values()) {
            for (BakedQuad quad : model.getQuads(null, direction, rand, net.neoforged.neoforge.client.model.data.ModelData.EMPTY, renderType)) {
                vc.putBulkData(pose.last(), quad, 1.0F, 1.0F, 1.0F, 1.0F, packedLight, packedOverlay);
            }
        }
        for (BakedQuad quad : model.getQuads(null, null, rand, net.neoforged.neoforge.client.model.data.ModelData.EMPTY, renderType)) {
            vc.putBulkData(pose.last(), quad, 1.0F, 1.0F, 1.0F, 1.0F, packedLight, packedOverlay);
        }
    }

    /**
     * TC4: {@code UtilsFX.renderFacingQuad(x + 0.5, y + 1.5, z + 0.5, 0, 0.9,
     * count / 37 * alpha, ...)} - a camera facing, additive looking glow.
     */
    private void renderBubble(PoseStack pose, MultiBufferSource buffer, float charge, float time) {
        float alpha = Mth.sin(time / 8.0F) * 0.1F + 0.5F;
        float size = charge * alpha;
        if (size <= 0.01F) {
            return;
        }

        pose.pushPose();
        pose.translate(0.5F, 1.5F, 0.5F);
        pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        pose.scale(size, size, size);

        Matrix4f matrix = pose.last().pose();
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucentEmissive(BUBBLE_TEXTURE));
        vc.addVertex(matrix, -0.5F, 0.5F, 0.0F).setColor(1.0F, 1.0F, 1.0F, alpha)
                .setUv(0.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(0.0F, 0.0F, 1.0F);
        vc.addVertex(matrix, 0.5F, 0.5F, 0.0F).setColor(1.0F, 1.0F, 1.0F, alpha)
                .setUv(1.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(0.0F, 0.0F, 1.0F);
        vc.addVertex(matrix, 0.5F, -0.5F, 0.0F).setColor(1.0F, 1.0F, 1.0F, alpha)
                .setUv(1.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(0.0F, 0.0F, 1.0F);
        vc.addVertex(matrix, -0.5F, -0.5F, 0.0F).setColor(1.0F, 1.0F, 1.0F, alpha)
                .setUv(0.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(0.0F, 0.0F, 1.0F);
        pose.popPose();
    }
}
