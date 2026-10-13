package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entities.EntitySpecialItem;

import java.util.Random;

/**
 * Renderer for EntitySpecialItem - items with magical glowing effects.
 * Renders glowing tendrils around the item for a mystical appearance.
 */
@OnlyIn(Dist.CLIENT)
public class SpecialItemRenderer extends EntityRenderer<EntitySpecialItem> {
    
    private static final ResourceLocation BLANK = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/block/blank.png");
    
    private final ItemRenderer itemRenderer;
    private final Random random = new Random(187L);
    
    public SpecialItemRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.shadowRadius = 0.15F;
        this.shadowStrength = 0.75F;
    }
    
    @Override
    public ResourceLocation getTextureLocation(EntitySpecialItem entity) {
        return BLANK;
    }
    
    @Override
    public void render(EntitySpecialItem entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        
        // Bobbing motion
        float bob = Mth.sin((entity.getAge() + partialTicks) / 10.0F + entity.bobOffs) * 0.1F + 0.1F;
        
        poseStack.pushPose();
        poseStack.translate(0.0D, bob + 0.25D, 0.0D);
        
        // Render glowing tendrils effect
        renderGlowingTendrils(entity, partialTicks, poseStack, buffer);
        
        // Render the actual item
        poseStack.pushPose();
        
        // Spin the item
        float spin = (entity.getAge() + partialTicks) * 2.0F;
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        
        // Scale up slightly
        poseStack.scale(0.5F, 0.5F, 0.5F);
        
        // Render item with full brightness for magical glow
        itemRenderer.renderStatic(
                entity.getItem(),
                ItemDisplayContext.GROUND,
                0xF000F0, // Full brightness
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                entity.level(),
                entity.getId()
        );
        
        poseStack.popPose();
        poseStack.popPose();
        
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
    
    private void renderGlowingTendrils(EntitySpecialItem entity, float partialTicks,
                                        PoseStack poseStack, MultiBufferSource buffer) {
        random.setSeed(187L);
        
        int count = Minecraft.getInstance().options.graphicsMode().get().getId() >= 1 ? 10 : 5;
        float age = entity.getAge() / 500.0F;
        
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.lightning());
        
        for (int i = 0; i < count; i++) {
            poseStack.pushPose();
            
            // Random rotation for each tendril
            poseStack.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360.0F + age * 360.0F));
            
            // Scale based on entity age
            float scale = Math.min(entity.getAge(), 10) / 10.0F;
            float length = (random.nextFloat() * 20.0F + 5.0F) / 30.0F * scale;
            float width = (random.nextFloat() * 2.0F + 1.0F) / 30.0F * scale;
            
            Matrix4f matrix = poseStack.last().pose();
            
            // Draw tendril as a triangle fan
            // Center vertex (white, opaque)
            vertexConsumer.addVertex(matrix, 0.0F, 0.0F, 0.0F)
                    .setColor(255, 255, 255, 255)
                    ;
            
            // Outer vertices (purple, transparent)
            vertexConsumer.addVertex(matrix, (float)(-0.866 * width), length, (float)(-0.5 * width))
                    .setColor(255, 0, 255, 0)
                    ;
            
            vertexConsumer.addVertex(matrix, (float)(0.866 * width), length, (float)(-0.5 * width))
                    .setColor(255, 0, 255, 0)
                    ;
            
            vertexConsumer.addVertex(matrix, 0.0F, length, width)
                    .setColor(255, 0, 255, 0)
                    ;
            
            vertexConsumer.addVertex(matrix, (float)(-0.866 * width), length, (float)(-0.5 * width))
                    .setColor(255, 0, 255, 0)
                    ;
            
            poseStack.popPose();
        }
    }
}




