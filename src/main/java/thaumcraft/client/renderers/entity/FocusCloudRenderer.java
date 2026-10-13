package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.common.entities.projectile.EntityFocusCloud;

/**
 * Renderer for Focus Cloud entities.
 * The cloud effect is rendered purely through particles, so this renderer does nothing.
 */
@OnlyIn(Dist.CLIENT)
public class FocusCloudRenderer extends EntityRenderer<EntityFocusCloud> {
    
    public FocusCloudRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }
    
    @Override
    public ResourceLocation getTextureLocation(EntityFocusCloud entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
    
    @Override
    public void render(EntityFocusCloud entity, float entityYaw, float partialTicks, 
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Cloud effect is rendered through particles, no model rendering needed
    }
}




