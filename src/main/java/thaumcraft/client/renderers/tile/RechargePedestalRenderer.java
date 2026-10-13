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
import thaumcraft.common.tiles.devices.TileRechargePedestal;

/**
 * Block entity renderer for the Recharge Pedestal.
 * Renders a floating, spinning item on top of the pedestal, larger than regular pedestals.
 */
@OnlyIn(Dist.CLIENT)
public class RechargePedestalRenderer implements BlockEntityRenderer<TileRechargePedestal> {

    private final ItemRenderer itemRenderer;

    public RechargePedestalRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(TileRechargePedestal tile, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        
        ItemStack stack = tile.getItem(0);
        if (stack.isEmpty()) return;

        float ticks = Minecraft.getInstance().player != null ?
                Minecraft.getInstance().player.tickCount + partialTicks : 0;

        poseStack.pushPose();

        // Position above pedestal
        poseStack.translate(0.5, 0.85, 0.5);
        
        // Larger scale for recharge pedestal
        poseStack.scale(1.5f, 1.5f, 1.5f);

        // Spin the item
        poseStack.mulPose(Axis.YP.rotationDegrees(ticks % 360.0f));

        // Slight bob
        float bob = (float) Math.sin(ticks * 0.1f) * 0.05f;
        poseStack.translate(0, bob, 0);

        // Render the item
        ItemStack renderStack = stack.copy();
        renderStack.setCount(1);
        itemRenderer.renderStatic(renderStack, ItemDisplayContext.GROUND, packedLight,
                packedOverlay, poseStack, buffer, tile.getLevel(), 0);

        poseStack.popPose();
    }
}




