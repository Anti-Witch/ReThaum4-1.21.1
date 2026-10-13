package thaumcraft.client.models.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;

/**
 * Model for the brain floating in a Brain in a Jar.
 * Based on the original 1.12.2 ModelBrain.
 */
@OnlyIn(Dist.CLIENT)
public class BrainModel extends Model {

    public static final ModelLayerLocation LAYER_LOCATION = 
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "brain"), "main");

    private final ModelPart mainBrain;
    private final ModelPart brainStem;
    private final ModelPart spinalCord;

    public BrainModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.mainBrain = root.getChild("main_brain");
        this.brainStem = root.getChild("brain_stem");
        this.spinalCord = root.getChild("spinal_cord");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // Main brain mass (12x10x16 at center)
        partdefinition.addOrReplaceChild("main_brain", CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-6.0f, -5.0f, -8.0f, 12.0f, 10.0f, 16.0f),
                PartPose.offset(0.0f, 13.0f, 0.0f));

        // Brain stem (8x3x7)
        partdefinition.addOrReplaceChild("brain_stem", CubeListBuilder.create()
                .texOffs(64, 0)
                .addBox(-4.0f, 0.0f, 0.0f, 8.0f, 3.0f, 7.0f),
                PartPose.offset(0.0f, 18.0f, 0.0f));

        // Spinal cord (2x6x2) with slight rotation
        partdefinition.addOrReplaceChild("spinal_cord", CubeListBuilder.create()
                .texOffs(0, 32)
                .addBox(-1.0f, 0.0f, -1.0f, 2.0f, 6.0f, 2.0f),
                PartPose.offsetAndRotation(0.0f, 18.0f, -1.0f, 0.4089647f, 0.0f, 0.0f));

        return LayerDefinition.create(meshdefinition, 128, 64);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, int color) {
        mainBrain.render(poseStack, buffer, packedLight, packedOverlay, color);
        brainStem.render(poseStack, buffer, packedLight, packedOverlay, color);
        spinalCord.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}




