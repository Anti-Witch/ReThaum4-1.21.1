package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.api.golems.parts.GolemMaterial;
import thaumcraft.client.models.entity.GolemModel;
import thaumcraft.common.golems.EntityThaumcraftGolem;

/**
 * GolemRenderer - Renders Thaumcraft golems in the world.
 * 
 * Features:
 * - Uses material-based textures
 * - Renders held items
 * - Scales based on golem size
 * 
 * This is a simplified version. The original used OBJ models
 * with swappable parts for heads, arms, legs, and addons.
 */
@OnlyIn(Dist.CLIENT)
public class GolemRenderer extends MobRenderer<EntityThaumcraftGolem, GolemModel> {
    
    // Default texture for golems without material
    private static final ResourceLocation DEFAULT_TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/golem_wood.png");
    
    public GolemRenderer(EntityRendererProvider.Context context) {
        super(context, new GolemModel(context.bakeLayer(GolemModel.LAYER_LOCATION)), 0.3F);
        
        // Add layer for held items
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }
    
    @Override
    public ResourceLocation getTextureLocation(EntityThaumcraftGolem entity) {
        // Get texture based on material
        if (entity.getProperties() != null && entity.getProperties().getMaterial() != null) {
            GolemMaterial material = entity.getProperties().getMaterial();
            if (material.texture != null) {
                return material.texture;
            }
        }
        
        return DEFAULT_TEXTURE;
    }
    
    @Override
    public void render(EntityThaumcraftGolem entity, float entityYaw, float partialTicks, 
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Scale based on golem properties
        // Default golems are small, some materials make them bigger
        float scale = 0.6F; // Base scale (golems are small)
        
        if (entity.getProperties() != null && entity.getProperties().getMaterial() != null) {
            // Adjust scale based on material or traits
            // For now, use base scale
        }
        
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        
        poseStack.popPose();
    }
    
    @Override
    protected void scale(EntityThaumcraftGolem entity, PoseStack poseStack, float partialTicks) {
        // Additional scaling can be done here based on entity state
        super.scale(entity, poseStack, partialTicks);
    }
    
    @Override
    protected boolean shouldShowName(EntityThaumcraftGolem entity) {
        // Show name if golem is looked at while sneaking
        return super.shouldShowName(entity);
    }
}




