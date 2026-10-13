package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entities.monster.cult.EntityCultist;
import thaumcraft.common.entities.monster.cult.EntityCultistCleric;

import java.util.Random;

/**
 * Renderer for Cultists - hooded followers of the Crimson Cult.
 * Uses the humanoid biped model with custom textures.
 * Ritualist clerics float and have a ritual tether line.
 */
@OnlyIn(Dist.CLIENT)
public class CultistRenderer extends HumanoidMobRenderer<EntityCultist, HumanoidModel<EntityCultist>> {
    
    private static final ResourceLocation TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/cultist.png");
    
    public CultistRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        this.addLayer(new HumanoidArmorLayer<>(this, 
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
    }
    
    @Override
    public ResourceLocation getTextureLocation(EntityCultist entity) {
        return TEXTURE;
    }
    
    @Override
    public void render(EntityCultist entity, float entityYaw, float partialTicks, 
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        
        // Floating animation for ritualist clerics
        if (entity instanceof EntityCultistCleric cleric && cleric.isRitualist()) {
            int seed = new Random(entity.getId()).nextInt(1000);
            float time = entity.tickCount + partialTicks + seed;
            float bob = Mth.sin(time / 9.0F) * 0.1F + 0.21F;
            poseStack.translate(0.0, bob, 0.0);
        }
        
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        
        poseStack.popPose();
    }
}




