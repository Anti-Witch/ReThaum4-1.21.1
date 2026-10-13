package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.common.tiles.devices.TileHungryChest;

/**
 * Block entity renderer for the Hungry Chest.
 * Renders a chest model with animated lid opening/closing.
 */
@OnlyIn(Dist.CLIENT)
public class HungryChestRenderer implements BlockEntityRenderer<TileHungryChest> {

    private static final ResourceLocation TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/chesthungry.png");

    private final ModelPart lid;
    private final ModelPart bottom;
    private final ModelPart lock;

    public HungryChestRenderer(BlockEntityRendererProvider.Context context) {
        // Use vanilla chest model
        ModelPart modelPart = context.bakeLayer(ModelLayers.CHEST);
        this.bottom = modelPart.getChild("bottom");
        this.lid = modelPart.getChild("lid");
        this.lock = modelPart.getChild("lock");
    }

    @Override
    public void render(TileHungryChest tile, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        
        poseStack.pushPose();
        
        // Get facing direction
        Direction facing = Direction.NORTH;
        if (tile.getLevel() != null && tile.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            facing = tile.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        
        // Position at center of block
        poseStack.translate(0.5f, 0.5f, 0.5f);
        
        // Rotate based on facing
        float rotation = switch (facing) {
            case NORTH -> 180.0f;
            case SOUTH -> 0.0f;
            case WEST -> 90.0f;
            case EAST -> -90.0f;
            default -> 0.0f;
        };
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        
        // Translate for chest model positioning
        poseStack.translate(-0.5f, -0.5f, -0.5f);
        
        // Calculate lid angle with smooth interpolation
        float lidAngle = tile.prevLidAngle + (tile.lidAngle - tile.prevLidAngle) * partialTicks;
        lidAngle = 1.0f - lidAngle;
        lidAngle = 1.0f - lidAngle * lidAngle * lidAngle;
        
        // Set lid rotation (opens upward)
        lid.xRot = -(lidAngle * ((float)Math.PI / 2.0f));
        lock.xRot = lid.xRot;
        
        // Render the chest model
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutout(TEXTURE));
        
        // Translate up by 1 block because chest model is positioned at y=0
        poseStack.translate(0.5, 1.0, 0.5);
        poseStack.scale(1.0f, -1.0f, -1.0f);
        
        // 1.21.1: ModelPart.render requires an int ARGB color as the 5th argument
        lid.render(poseStack, vertexConsumer, packedLight, packedOverlay, 0xFFFFFFFF);
        lock.render(poseStack, vertexConsumer, packedLight, packedOverlay, 0xFFFFFFFF);
        bottom.render(poseStack, vertexConsumer, packedLight, packedOverlay, 0xFFFFFFFF);
        
        poseStack.popPose();
    }
}




