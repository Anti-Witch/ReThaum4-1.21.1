package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.common.tiles.crafting.TilePedestal;

/**
 * Block entity renderer for Pedestals.
 * Renders the item placed on the pedestal with a floating/spinning animation.
 */
@OnlyIn(Dist.CLIENT)
public class PedestalRenderer implements BlockEntityRenderer<TilePedestal> {

    private final ItemRenderer itemRenderer;
    
    public PedestalRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = Minecraft.getInstance().getItemRenderer();
    }

    @Override
    public void render(TilePedestal tile, float partialTicks, PoseStack poseStack, 
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        
        ItemStack stack = tile.getItem(0);
        if (stack.isEmpty()) {
            return;
        }
        
        poseStack.pushPose();
        
        // Move to center of pedestal, above the base
        poseStack.translate(0.5, 1.1, 0.5);
        
        // Bobbing animation
        long time = tile.getLevel() != null ? tile.getLevel().getGameTime() : 0;
        float bob = (float) Math.sin((time + partialTicks) * 0.1) * 0.05F;
        poseStack.translate(0, bob, 0);
        
        // Rotation animation
        float rotation = (time + partialTicks) * 2.0F;
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        
        // Scale the item
        poseStack.scale(0.5F, 0.5F, 0.5F);
        
        // Render the item
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, 
                packedLight, packedOverlay, poseStack, buffer, 
                tile.getLevel(), 0);
        
        poseStack.popPose();
    }
}




