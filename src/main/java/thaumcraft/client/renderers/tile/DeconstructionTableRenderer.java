package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.tiles.crafting.TileDeconstructionTable;
import thaumcraft.init.ModItems;

/**
 * DeconstructionTableRenderer - renders the contents of the deconstruction table.
 *
 * Ported from OpenTC4 TileDeconstructionTableRenderer:
 * - a thaumometer sits on the table (static, TC4 rendered ConfigItems.itemThaumometer
 *   at y+0.92 with a 0.8 scale),
 * - the item being broken down floats and spins above the table (y+1.15, bobbed,
 *   TC4 rendered it in "item frame" mode with a small sine bob),
 * - the discovered primal aspect is shown as a flat rotating icon just above the
 *   tabletop (y+1.081, 16x16 quad at 0.024 scale, TC4 UtilsFX.drawTag).
 *
 * The table itself (top/base/legs) is a JSON block model
 * (models/block/deconstruction_table.json, texture models/decontable.png).
 */
@OnlyIn(Dist.CLIENT)
public class DeconstructionTableRenderer implements BlockEntityRenderer<TileDeconstructionTable> {

    /** TC4: GL11.glScaled(0.024, 0.024, 0.024) before drawing the 16x16 tag. */
    private static final float ASPECT_SCALE = 0.024f;

    /** TC4 scale of the static thaumometer on the table. */
    private static final float THAUMOMETER_SCALE = 0.8f;

    /** TC4 spin speed: 1 full turn per 360 ticks. */
    private static final float SPIN_TICKS_PER_TURN = 360.0f;

    private final ItemRenderer itemRenderer;
    private final ItemStack thaumometer = new ItemStack(ModItems.THAUMOMETER.get());

    public DeconstructionTableRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(TileDeconstructionTable tile, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Level level = tile.getLevel();
        if (level == null) {
            return;
        }
        float ticks = (float) level.getGameTime() + partialTicks;

        // 1. Static thaumometer lying on the table. TC4 uses y+0.92 with the
        //    thin ModelArcaneWorkbench tabletop; this port's table model is a
        //    full block (top surface at y=1.0), so the meter has to sit at
        //    1.0+ to stay visible.
        poseStack.pushPose();
        poseStack.translate(0.5, 1.01, 0.5);
        poseStack.scale(THAUMOMETER_SCALE, THAUMOMETER_SCALE, THAUMOMETER_SCALE);
        this.itemRenderer.renderStatic(this.thaumometer, ItemDisplayContext.FIXED,
                packedLight, packedOverlay, poseStack, buffer, level, 0);
        poseStack.popPose();

        // 2. The item being deconstructed - floats, bobs and spins (TC4: y+1.15,
        //    hoverStart = sin(ticks/14)*0.2 + 0.2)
        ItemStack input = tile.getItem(TileDeconstructionTable.SLOT_ITEM);
        if (!input.isEmpty()) {
            poseStack.pushPose();
            float hover = Mth.sin(ticks / 14.0f) * 0.2f + 0.2f;
            poseStack.translate(0.5, 1.15 + hover, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(ticks % SPIN_TICKS_PER_TURN));
            ItemStack single = input.copy();
            single.setCount(1);
            this.itemRenderer.renderStatic(single, ItemDisplayContext.FIXED,
                    packedLight, packedOverlay, poseStack, buffer, level, 0);
            poseStack.popPose();
        }

        // 3. The waiting primal aspect - flat icon rotating above the tabletop
        Aspect aspect = tile.aspect;
        if (aspect != null) {
            poseStack.pushPose();
            poseStack.translate(0.5, 1.081, 0.5);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
            poseStack.mulPose(Axis.ZP.rotationDegrees(ticks % SPIN_TICKS_PER_TURN));
            poseStack.scale(ASPECT_SCALE, ASPECT_SCALE, ASPECT_SCALE);
            renderAspectQuad(poseStack, buffer, aspect);
            poseStack.popPose();
        }
    }

    /**
     * Draw the 16x16 aspect icon as a flat, fully lit, tinted quad
     * (world space equivalent of TC4 UtilsFX.drawTag).
     */
    private void renderAspectQuad(PoseStack poseStack, MultiBufferSource buffer, Aspect aspect) {
        ResourceLocation texture = aspect.getImage();
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(texture));
        Matrix4f matrix = poseStack.last().pose();

        int color = aspect.getColor();
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = 0.8f; // TC4 drawTag alpha for the table tag

        float half = 8.0f;
        consumer.addVertex(matrix, -half, -half, 0).setColor(r, g, b, a)
                .setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(poseStack.last(), 0, 0, 1);
        consumer.addVertex(matrix, half, -half, 0).setColor(r, g, b, a)
                .setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(poseStack.last(), 0, 0, 1);
        consumer.addVertex(matrix, half, half, 0).setColor(r, g, b, a)
                .setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(poseStack.last(), 0, 0, 1);
        consumer.addVertex(matrix, -half, half, 0).setColor(r, g, b, a)
                .setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(poseStack.last(), 0, 0, 1);
    }
}
