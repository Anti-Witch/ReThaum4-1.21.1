package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.common.entities.EntityFallingTaint;

/**
 * Renderer for Falling Taint blocks.
 * Renders the falling block state using the block model renderer.
 */
@OnlyIn(Dist.CLIENT)
public class FallingTaintRenderer extends EntityRenderer<EntityFallingTaint> {
    
    public FallingTaintRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
    }
    
    @Override
    public ResourceLocation getTextureLocation(EntityFallingTaint entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
    
    @Override
    public void render(EntityFallingTaint entity, float entityYaw, float partialTicks, 
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        BlockState blockState = entity.getBlockState();
        if (blockState == null || blockState.getRenderShape() == RenderShape.INVISIBLE) {
            return;
        }
        
        poseStack.pushPose();
        
        // Center the block
        poseStack.translate(-0.5, 0.0, -0.5);
        
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        
        // Render the block model
        dispatcher.getModelRenderer().tesselateBlock(
                entity.level(),
                dispatcher.getBlockModel(blockState),
                blockState,
                entity.blockPosition(),
                poseStack,
                buffer.getBuffer(net.minecraft.client.renderer.RenderType.cutout()),
                false,
                entity.level().random,
                blockState.getSeed(entity.blockPosition()),
                OverlayTexture.NO_OVERLAY
        );
        
        poseStack.popPose();
        
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}




