package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.tiles.crafting.TileFocalManipulator;

import java.awt.Color;

/**
 * Block entity renderer for the Focal Manipulator.
 * Renders floating focus item and orbiting crystal elements.
 */
@OnlyIn(Dist.CLIENT)
public class FocalManipulatorRenderer implements BlockEntityRenderer<TileFocalManipulator> {

    private static final ResourceLocation PARTICLE_TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/particles.png");

    private final ItemRenderer itemRenderer;

    public FocalManipulatorRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = Minecraft.getInstance().getItemRenderer();
    }

    @Override
    public void render(TileFocalManipulator tile, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        
        if (tile.getLevel() == null) return;
        
        Minecraft mc = Minecraft.getInstance();
        float ticks = (mc.player != null ? mc.player.tickCount : 0) + partialTicks;
        
        // Render floating focus item
        ItemStack focusStack = tile.getItem(0);
        if (!focusStack.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(0.5, 0.8, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(ticks % 360.0f));
            
            // Bobbing animation
            float bob = Mth.sin(ticks / 14.0f) * 0.2f + 0.2f;
            poseStack.translate(0, bob * 0.1, 0);
            
            poseStack.scale(0.5f, 0.5f, 0.5f);
            itemRenderer.renderStatic(focusStack, ItemDisplayContext.FIXED, packedLight, 
                    packedOverlay, poseStack, buffer, tile.getLevel(), 0);
            
            poseStack.popPose();
        }
        
        // Render orbiting crystals during crafting
        Aspect[] aspects = tile.crystalsSync.getAspects();
        if (aspects != null && aspects.length > 0) {
            int count = aspects.length;
            float angleStep = 360.0f / count;
            
            for (int a = 0; a < count; a++) {
                Aspect aspect = aspects[a];
                float angle = (ticks % 720.0f / 2.0f) + angleStep * a;
                float bob = Mth.sin((ticks + a * 10) / 12.0f) * 0.02f + 0.02f;
                
                // Render glowing particle
                poseStack.pushPose();
                poseStack.translate(0.5, 1.3, 0.5);
                poseStack.mulPose(Axis.YP.rotationDegrees(angle));
                poseStack.translate(0, bob, 0.4);
                poseStack.mulPose(Axis.YP.rotationDegrees(-angle));
                
                Color c = new Color(aspect.getColor());
                float r = c.getRed() / 255.0f;
                float g = c.getGreen() / 255.0f;
                float b = c.getBlue() / 255.0f;
                
                renderGlowingOrb(poseStack, buffer, 0.175f, r, g, b, 0.66f);
                
                poseStack.popPose();
                
                // Render crystal item
                poseStack.pushPose();
                poseStack.translate(0.5, 1.05, 0.5);
                poseStack.mulPose(Axis.YP.rotationDegrees(angle));
                poseStack.translate(0, bob, 0.4);
                poseStack.scale(0.5f, 0.5f, 0.5f);
                
                // Render ray effect
                renderRay(poseStack, buffer, angle, a, bob, r, g, b, ticks);
                
                // Render crystal
                ItemStack crystalStack = ThaumcraftApiHelper.makeCrystal(aspect);
                itemRenderer.renderStatic(crystalStack, ItemDisplayContext.FIXED, packedLight,
                        packedOverlay, poseStack, buffer, tile.getLevel(), 0);
                
                poseStack.popPose();
            }
        }
    }
    
    /**
     * Render a glowing orb particle.
     */
    private void renderGlowingOrb(PoseStack poseStack, MultiBufferSource buffer,
                                   float size, float r, float g, float b, float a) {
        poseStack.pushPose();
        
        // Face camera
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
        
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(PARTICLE_TEXTURE));
        Matrix4f matrix = poseStack.last().pose();
        
        int fullLight = 0x00F000F0;
        float half = size / 2.0f;
        
        // UV coordinates for glow particle (somewhere in particle atlas)
        float u0 = 0.0f;
        float u1 = 0.0625f;
        float v0 = 0.0f;
        float v1 = 0.0625f;
        
        vertexConsumer.addVertex(matrix, -half, -half, 0).setColor(r, g, b, a)
                .setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((fullLight) & 0xFFFF, ((fullLight) >> 16) & 0xFFFF).setNormal(0, 0, 1);
        vertexConsumer.addVertex(matrix, half, -half, 0).setColor(r, g, b, a)
                .setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((fullLight) & 0xFFFF, ((fullLight) >> 16) & 0xFFFF).setNormal(0, 0, 1);
        vertexConsumer.addVertex(matrix, half, half, 0).setColor(r, g, b, a)
                .setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((fullLight) & 0xFFFF, ((fullLight) >> 16) & 0xFFFF).setNormal(0, 0, 1);
        vertexConsumer.addVertex(matrix, -half, half, 0).setColor(r, g, b, a)
                .setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setUv2((fullLight) & 0xFFFF, ((fullLight) >> 16) & 0xFFFF).setNormal(0, 0, 1);
        
        poseStack.popPose();
    }
    
    /**
     * Render a ray effect from crystal to focus.
     */
    private void renderRay(PoseStack poseStack, MultiBufferSource buffer,
                           float angle, int num, float lift, float r, float g, float b, float ticks) {
        poseStack.pushPose();
        
        float pan = Mth.sin((ticks + num * 10) / 15.0f) * 15.0f;
        float aperture = Mth.sin((ticks + num * 10) / 14.0f) * 2.0f;
        
        poseStack.translate(0, 0.475f + lift, 0);
        poseStack.mulPose(Axis.XN.rotationDegrees(90));
        poseStack.mulPose(Axis.YP.rotationDegrees(angle));
        poseStack.mulPose(Axis.XP.rotationDegrees(pan));
        
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();
        
        // Randomize ray shape
        java.util.Random random = new java.util.Random(187L + (long)num * num);
        float fa = random.nextFloat() * 20.0f + 10.0f;
        float f4 = random.nextFloat() * 4.0f + 6.0f + aperture;
        fa /= 30.0f / (Math.min(ticks, 10.0f) / 10.0f);
        f4 /= 30.0f / (Math.min(ticks, 10.0f) / 10.0f);
        
        // Scale down the ray
        fa *= 0.02f;
        f4 *= 0.02f;
        
        // Render cone/ray using triangle fan pattern
        vertexConsumer.addVertex(matrix, 0, 0, 0).setColor(r, g, b, 0.66f);
        vertexConsumer.addVertex(matrix, -0.8f * f4, fa, -0.5f * f4).setColor(r, g, b, 0.0f);
        vertexConsumer.addVertex(matrix, 0.8f * f4, fa, -0.5f * f4).setColor(r, g, b, 0.0f);
        vertexConsumer.addVertex(matrix, 0, fa, 1.0f * f4).setColor(r, g, b, 0.0f);
        
        poseStack.popPose();
    }
}




