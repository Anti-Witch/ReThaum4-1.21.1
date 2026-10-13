package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.common.tiles.crafting.TileThaumatorium;

/**
 * Block entity renderer for the Thaumatorium.
 * Renders the floating result item on the display shelf.
 */
@OnlyIn(Dist.CLIENT)
public class ThaumatoriumRenderer implements BlockEntityRenderer<TileThaumatorium> {

    private final ItemRenderer itemRenderer;

    public ThaumatoriumRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = Minecraft.getInstance().getItemRenderer();
    }

    @Override
    public void render(TileThaumatorium tile, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        
        if (tile.getLevel() == null) return;
        
        // Get facing direction
        Direction facing = Direction.NORTH;
        if (tile.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            facing = tile.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        
        // Get the result item to display
        ItemStack displayItem = tile.getRecipeResult();
        if (displayItem == null || displayItem.isEmpty()) {
            // If no recipe in progress, show output slot item
            displayItem = tile.getItem(TileThaumatorium.OUTPUT_SLOT);
        }
        
        if (displayItem != null && !displayItem.isEmpty()) {
            poseStack.pushPose();
            
            // Position at the front display shelf
            poseStack.translate(0.5, 1.125, 0.5);
            
            // Offset based on facing
            float offsetX = facing.getStepX() / 1.99f;
            float offsetZ = facing.getStepZ() / 1.99f;
            poseStack.translate(offsetX, 0, offsetZ);
            
            // Rotate to face outward
            float rotation = switch (facing) {
                case EAST -> 90.0f;
                case WEST -> 270.0f;
                case NORTH -> 180.0f;
                case SOUTH -> 0.0f;
                default -> 0.0f;
            };
            poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
            
            // Scale down item
            poseStack.scale(0.75f, 0.75f, 0.75f);
            
            // Render the item
            itemRenderer.renderStatic(displayItem, ItemDisplayContext.FIXED, packedLight, 
                    packedOverlay, poseStack, buffer, tile.getLevel(), 0);
            
            poseStack.popPose();
        }
    }
}




