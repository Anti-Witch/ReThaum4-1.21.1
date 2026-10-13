package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entities.monster.boss.EntityCultistLeader;

/**
 * Renderer for Cultist Leader - boss version of cultists.
 * Larger scale and uses standard cultist texture.
 */
@OnlyIn(Dist.CLIENT)
public class CultistLeaderRenderer extends HumanoidMobRenderer<EntityCultistLeader, HumanoidModel<EntityCultistLeader>> {
    
    private static final ResourceLocation TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/cultist.png");
    
    public CultistLeaderRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        this.addLayer(new HumanoidArmorLayer<>(this, 
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
    }
    
    @Override
    public ResourceLocation getTextureLocation(EntityCultistLeader entity) {
        return TEXTURE;
    }
    
    @Override
    protected void scale(EntityCultistLeader entity, PoseStack poseStack, float partialTicks) {
        super.scale(entity, poseStack, partialTicks);
        // Cultist leader is 15% larger than normal
        poseStack.scale(1.15F, 1.15F, 1.15F);
    }
}




