package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.SpiderEyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entities.monster.EntityEldritchCrab;

/**
 * Renderer for Eldritch Crabs - creepy void crabs.
 * Uses a modified spider model with custom texture.
 */
@OnlyIn(Dist.CLIENT)
public class EldritchCrabRenderer extends MobRenderer<EntityEldritchCrab, SpiderModel<EntityEldritchCrab>> {
    
    private static final ResourceLocation TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/crab.png");
    
    public EldritchCrabRenderer(EntityRendererProvider.Context context) {
        super(context, new SpiderModel<>(context.bakeLayer(ModelLayers.SPIDER)), 0.4F);
        this.addLayer(new SpiderEyesLayer<>(this));
    }
    
    @Override
    public ResourceLocation getTextureLocation(EntityEldritchCrab entity) {
        return TEXTURE;
    }
    
    @Override
    protected void scale(EntityEldritchCrab entity, PoseStack poseStack, float partialTicks) {
        // Crabs are wider and shorter than spiders
        poseStack.scale(0.8F, 0.6F, 0.8F);
    }
}




