package thaumcraft.client.renderers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.seals.SealPos;
import thaumcraft.common.golems.seals.SealEntity;
import thaumcraft.common.golems.seals.SealHandler;

import java.util.concurrent.ConcurrentHashMap;

/**
 * SealRenderer - Renders seals in the world when player holds ISealDisplayer items.
 * 
 * Renders:
 * - Seal icons on block faces
 * - Seal working area (for seals with ISealConfigArea)
 * - Inactive state indicator (when stopped by redstone)
 * 
 * Ported from 1.12.2 to 1.20.1 modern rendering.
 * Uses PoseStack and modern vertex buffer API instead of GL11 immediate mode.
 */
@OnlyIn(Dist.CLIENT)
public class SealRenderer {
    
    // Default seal texture for seals without custom icons
    private static final ResourceLocation DEFAULT_SEAL_TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/item/blank.png");
    
    // Maximum render distance squared (16 blocks)
    private static final double MAX_RENDER_DIST_SQ = 256.0;
    
    /**
     * Render all seals visible to the player.
     * Called from RenderLevelStageEvent.
     * 
     * @param poseStack The pose stack for transformations
     * @param partialTick Partial tick for smooth interpolation
     * @param player The local player
     */
    public static void renderSeals(PoseStack poseStack, float partialTick, Player player) {
        if (player == null || player.level() == null) return;
        
        // Get seals in player's dimension
        String dimKey = player.level().dimension().location().toString();
        ConcurrentHashMap<SealPos, SealEntity> seals = SealHandler.sealEntities.get(dimKey);
        
        if (seals == null || seals.isEmpty()) return;
        
        // Get camera position for distance calculations
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        
        // Setup render state
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        
        // If sneaking, disable depth test to see seals through blocks
        boolean sneaking = player.isShiftKeyDown();
        if (sneaking) {
            RenderSystem.disableDepthTest();
        }
        
        poseStack.pushPose();
        
        // Render each seal
        for (ISealEntity seal : seals.values()) {
            if (seal.getSeal() == null || seal.getSealPos() == null) continue;
            
            BlockPos pos = seal.getSealPos().pos;
            double distSq = cameraPos.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            
            if (distSq <= MAX_RENDER_DIST_SQ) {
                float alpha = 1.0f - (float)(distSq / MAX_RENDER_DIST_SQ);
                boolean inactive = seal.isStoppedByRedstone(player.level());
                
                renderSeal(poseStack, seal, cameraPos, alpha, inactive);
            }
        }
        
        poseStack.popPose();
        
        // Restore render state
        if (sneaking) {
            RenderSystem.enableDepthTest();
        }
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
    
    /**
     * Render a single seal.
     */
    private static void renderSeal(PoseStack poseStack, ISealEntity seal, Vec3 cameraPos, 
                                   float alpha, boolean inactive) {
        SealPos sealPos = seal.getSealPos();
        BlockPos pos = sealPos.pos;
        Direction face = sealPos.face;
        
        poseStack.pushPose();
        
        // Translate to seal position (relative to camera)
        poseStack.translate(
            pos.getX() + 0.5 - cameraPos.x,
            pos.getY() + 0.5 - cameraPos.y,
            pos.getZ() + 0.5 - cameraPos.z
        );
        
        // Rotate based on face direction
        applyFaceRotation(poseStack, face);
        
        // Move slightly off the surface to prevent z-fighting
        poseStack.translate(0, 0, 0.51);
        
        // Scale down
        poseStack.scale(0.5f, 0.5f, 0.5f);
        
        // Render the seal quad
        float brightness = inactive ? 0.5f : 1.0f;
        renderSealQuad(poseStack, seal.getSeal().getSealIcon(), brightness, brightness, brightness, alpha);
        
        poseStack.popPose();
    }
    
    /**
     * Apply rotation to face the correct direction.
     */
    private static void applyFaceRotation(PoseStack poseStack, Direction face) {
        switch (face) {
            case UP -> poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90));
            case DOWN -> poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));
            case NORTH -> { } // Default facing
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90));
            case EAST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90));
        }
    }
    
    /**
     * Render a textured quad for the seal icon.
     */
    private static void renderSealQuad(PoseStack poseStack, ResourceLocation texture, 
                                       float r, float g, float b, float a) {
        ResourceLocation actualTexture;
        if (texture == null) {
            actualTexture = DEFAULT_SEAL_TEXTURE;
        } else {
            // Convert from "items/seals/seal_xxx" format to full texture path
            String path = texture.getPath();
            if (!path.startsWith("textures/")) {
                path = "textures/" + path;
            }
            // Legacy 1.7.10 "items/" folder is "item/" in modern resource packs
            if (path.startsWith("textures/items/")) {
                path = "textures/item/" + path.substring("textures/items/".length());
            }
            if (!path.endsWith(".png")) {
                path = path + ".png";
            }
            actualTexture = ResourceLocation.fromNamespaceAndPath(texture.getNamespace(), path);
        }
        
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, actualTexture);
        
        Matrix4f matrix = poseStack.last().pose();
        
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        
        // Render a centered quad
        float size = 0.5f;
        int color = ((int)(a * 255) << 24) | ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
        
        buffer.addVertex(matrix, -size, -size, 0).setUv(0, 1).setColor(r, g, b, a);
        buffer.addVertex(matrix, size, -size, 0).setUv(1, 1).setColor(r, g, b, a);
        buffer.addVertex(matrix, size, size, 0).setUv(1, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, -size, size, 0).setUv(0, 0).setColor(r, g, b, a);
        
        BufferUploader.drawWithShader(buffer.build());
    }
}




