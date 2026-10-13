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
import thaumcraft.common.entities.monster.EntityEldritchGuardian;

/**
 * Renderer for Eldritch Guardians - tall humanoid void creatures.
 * Uses the humanoid model as a base with custom texture and scaling.
 */
@OnlyIn(Dist.CLIENT)
public class EldritchGuardianRenderer extends HumanoidMobRenderer<EntityEldritchGuardian, HumanoidModel<EntityEldritchGuardian>> {
    
    private static final ResourceLocation TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/eldritch_guardian.png");
    
    public EldritchGuardianRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }
    
    @Override
    public ResourceLocation getTextureLocation(EntityEldritchGuardian entity) {
        return TEXTURE;
    }
    
    @Override
    protected void scale(EntityEldritchGuardian entity, PoseStack poseStack, float partialTicks) {
        // Guardians are taller than regular players
        poseStack.scale(1.2F, 1.25F, 1.2F);
    }
}




