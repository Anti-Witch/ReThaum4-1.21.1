package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entities.monster.boss.EntityEldritchWarden;

/**
 * Renderer for Eldritch Warden - a powerful boss version of the Eldritch Guardian.
 * Uses the same humanoid model but with different texture and slightly larger scale.
 */
@OnlyIn(Dist.CLIENT)
public class EldritchWardenRenderer extends HumanoidMobRenderer<EntityEldritchWarden, HumanoidModel<EntityEldritchWarden>> {
    
    private static final ResourceLocation TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/eldritch_guardian.png");
    
    public EldritchWardenRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.6F);
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }
    
    @Override
    public ResourceLocation getTextureLocation(EntityEldritchWarden entity) {
        return TEXTURE;
    }
    
    @Override
    protected void scale(EntityEldritchWarden entity, PoseStack poseStack, float partialTicks) {
        // Warden is 20% larger than the guardian
        poseStack.scale(1.4F, 1.4F, 1.4F);
    }
}




