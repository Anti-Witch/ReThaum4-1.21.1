package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.SlimeOuterLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entities.monster.EntityThaumicSlime;

/**
 * Renderer for Thaumic Slimes - magical slimes with aspect-colored innards.
 * Uses the vanilla slime model with a custom texture and optional tinting.
 */
@OnlyIn(Dist.CLIENT)
public class ThaumicSlimeRenderer extends MobRenderer<EntityThaumicSlime, SlimeModel<EntityThaumicSlime>> {
    
    private static final ResourceLocation TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/tslime.png");
    
    public ThaumicSlimeRenderer(EntityRendererProvider.Context context) {
        super(context, new SlimeModel<>(context.bakeLayer(ModelLayers.SLIME)), 0.25F);
        this.addLayer(new SlimeOuterLayer<>(this, context.getModelSet()));
    }
    
    @Override
    public ResourceLocation getTextureLocation(EntityThaumicSlime entity) {
        return TEXTURE;
    }
    
    @Override
    public void render(EntityThaumicSlime entity, float entityYaw, float partialTicks, 
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        this.shadowRadius = 0.25F * entity.getSize();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
    
    @Override
    protected void scale(EntityThaumicSlime entity, PoseStack poseStack, float partialTicks) {
        float scale = 0.999F;
        poseStack.scale(scale, scale, scale);
        
        float size = entity.getSize();
        float squish = Mth.lerp(partialTicks, entity.oSquish, entity.squish) / (size * 0.5F + 1.0F);
        float factor = 1.0F / (squish + 1.0F);
        poseStack.scale(factor * size, 1.0F / factor * size, factor * size);
    }
}




