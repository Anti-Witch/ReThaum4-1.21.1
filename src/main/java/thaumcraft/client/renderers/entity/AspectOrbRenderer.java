package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.entities.EntityAspectOrb;

/**
 * Renderer for {@link EntityAspectOrb}.
 * <p>
 * Port of OpenTC4 {@code client.renderers.entity.RenderAspectOrb}: the orb is a
 * camera-facing sprite taken from the animated particle sheet
 * ({@code textures/misc/particles.png}, 16x16 grid), tinted with the orb's
 * aspect colour (50% alpha) and scaled between 0.1 and 0.4 as the orb ages.
 * The sprite is drawn emissive (TC4 boosted the lightmap by +15*0.5 so the orb
 * glows in the dark) and is additionally backed by coloured
 * {@code ENTITY_EFFECT} particles spawned by the entity itself.
 */
@OnlyIn(Dist.CLIENT)
public class AspectOrbRenderer extends EntityRenderer<EntityAspectOrb> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/particles.png");

    /** TC4 animated particle sheet is a 16x16 grid of 16x16 sprites. */
    private static final float FRAME_STEP = 1.0f / 16.0f;
    /** TC4 RenderAspectOrb sampled row 8 (v 0.5 .. 0.5625) of the sheet. */
    private static final float V0 = 0.5f;
    private static final float V1 = 0.5625f;

    public AspectOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.1f;
    }

    @Override
    public ResourceLocation getTextureLocation(EntityAspectOrb entity) {
        return TEXTURE;
    }

    @Override
    public void render(EntityAspectOrb entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Aspect aspect = entity.getAspect();

        poseStack.pushPose();

        // Billboard orientation
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));

        // TC4: f11 = 0.1 + 0.3 * (orbMaxAge - orbAge) / orbMaxAge
        int maxAge = Math.max(1, entity.orbMaxAge);
        float scale = 0.1f + 0.3f * ((float) (maxAge - entity.orbAge) / (float) maxAge);
        poseStack.scale(scale, scale, scale);

        // Animated frame (TC4: System.nanoTime() / 25000000 % 16)
        int frame = (int) (System.nanoTime() / 25000000L % 16L);
        float u0 = frame * FRAME_STEP;
        float u1 = u0 + FRAME_STEP;

        int color = aspect != null ? aspect.getColor() : 0xFFFFFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = 128; // TC4 setColorRGBA_I(color, 128)

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
        Matrix4f matrix = poseStack.last().pose();

        // TC4 quad: 1.0 x 1.0 units centred on the entity origin (the pose scale
        // shrinks it to 0.1 .. 0.4 blocks)
        float x0 = -0.5f;
        float x1 = 0.5f;
        float y0 = -0.25f;
        float y1 = 0.75f;

        consumer.addVertex(matrix, x0, y0, 0.0f)
                .setColor(r, g, b, a)
                .setUv(u0, V1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0)
                .setNormal(0.0f, 1.0f, 0.0f);
        consumer.addVertex(matrix, x1, y0, 0.0f)
                .setColor(r, g, b, a)
                .setUv(u1, V1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0)
                .setNormal(0.0f, 1.0f, 0.0f);
        consumer.addVertex(matrix, x1, y1, 0.0f)
                .setColor(r, g, b, a)
                .setUv(u1, V0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0)
                .setNormal(0.0f, 1.0f, 0.0f);
        consumer.addVertex(matrix, x0, y1, 0.0f)
                .setColor(r, g, b, a)
                .setUv(u0, V0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0)
                .setNormal(0.0f, 1.0f, 0.0f);

        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}
