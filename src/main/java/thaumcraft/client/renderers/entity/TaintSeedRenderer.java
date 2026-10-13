package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.client.models.entity.TaintSeedModel;
import thaumcraft.common.entities.monster.tainted.EntityTaintSeed;

/**
 * Renderer for Taint Seeds - stationary taint-spreading entities with multiple tentacles.
 */
@OnlyIn(Dist.CLIENT)
public class TaintSeedRenderer extends MobRenderer<EntityTaintSeed, TaintSeedModel<EntityTaintSeed>> {
    
    private static final ResourceLocation TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/taint_spore.png");
    
    public TaintSeedRenderer(EntityRendererProvider.Context context) {
        super(context, new TaintSeedModel<>(context.bakeLayer(TaintSeedModel.LAYER_LOCATION)), 0.5F);
    }
    
    @Override
    public ResourceLocation getTextureLocation(EntityTaintSeed entity) {
        return TEXTURE;
    }
    
    @Override
    protected void scale(EntityTaintSeed entity, PoseStack poseStack, float partialTicks) {
        // Scale based on entity size
        float scale = entity.getBbWidth();
        poseStack.scale(scale, scale, scale);
    }
}




