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
import thaumcraft.common.entities.monster.EntityMindSpider;

/**
 * Renderer for Mind Spiders - small cerebral parasites.
 * Uses the vanilla spider model scaled down with a custom texture.
 */
@OnlyIn(Dist.CLIENT)
public class MindSpiderRenderer extends MobRenderer<EntityMindSpider, SpiderModel<EntityMindSpider>> {
    
    private static final ResourceLocation TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/taint_spider.png");
    
    public MindSpiderRenderer(EntityRendererProvider.Context context) {
        super(context, new SpiderModel<>(context.bakeLayer(ModelLayers.SPIDER)), 0.3F);
        this.addLayer(new SpiderEyesLayer<>(this));
    }
    
    @Override
    public ResourceLocation getTextureLocation(EntityMindSpider entity) {
        return TEXTURE;
    }
    
    @Override
    protected void scale(EntityMindSpider entity, PoseStack poseStack, float partialTicks) {
        // Mind spiders are smaller than regular spiders
        poseStack.scale(0.5F, 0.5F, 0.5F);
    }
}




