package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.common.entities.projectile.EntityBottleTaint;
import thaumcraft.init.ModItems;

/**
 * Renderer for thrown taint bottles - renders as a spinning item.
 */
@OnlyIn(Dist.CLIENT)
public class BottleTaintRenderer extends EntityRenderer<EntityBottleTaint> {
    
    private final ItemRenderer itemRenderer;
    private ItemStack cachedItem;
    
    public BottleTaintRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = Minecraft.getInstance().getItemRenderer();
        this.shadowRadius = 0.15F;
        this.shadowStrength = 0.75F;
    }
    
    private ItemStack getItemStack() {
        if (cachedItem == null) {
            cachedItem = new ItemStack(ModItems.BOTTLE_TAINT.get());
        }
        return cachedItem;
    }
    
    @Override
    public ResourceLocation getTextureLocation(EntityBottleTaint entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
    
    @Override
    public void render(EntityBottleTaint entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        
        poseStack.pushPose();
        
        // Spinning motion
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F - entityYaw));
        float spin = (entity.tickCount + partialTicks) * 20.0F;
        poseStack.mulPose(Axis.ZP.rotationDegrees(spin));
        
        // Scale down slightly
        poseStack.scale(0.5F, 0.5F, 0.5F);
        
        // Render the bottle taint item
        itemRenderer.renderStatic(
                getItemStack(),
                ItemDisplayContext.GROUND,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                entity.level(),
                entity.getId()
        );
        
        poseStack.popPose();
        
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}




