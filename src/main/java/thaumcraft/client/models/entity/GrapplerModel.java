package thaumcraft.client.models.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;

/**
 * GrapplerModel - Simple model for grapple hook and focus mines.
 * A central core with three prongs extending in different directions.
 */
@OnlyIn(Dist.CLIENT)
public class GrapplerModel extends Model {
    
    public static final ModelLayerLocation LAYER_LOCATION = 
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "grappler"), "main");
    
    private final ModelPart core;
    private final ModelPart prong1;
    private final ModelPart prong2;
    private final ModelPart prong3;
    
    public GrapplerModel(ModelPart root) {
        super(RenderType::entityCutout);
        this.core = root.getChild("core");
        this.prong1 = root.getChild("prong1");
        this.prong2 = root.getChild("prong2");
        this.prong3 = root.getChild("prong3");
    }
    
    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        
        // Central core - small cube
        partdefinition.addOrReplaceChild("core",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F, CubeDeformation.NONE),
            PartPose.ZERO);
        
        // Prong 1 - along Z axis
        partdefinition.addOrReplaceChild("prong1",
            CubeListBuilder.create()
                .texOffs(0, 10)
                .addBox(-0.5F, -0.5F, -2.5F, 1.0F, 1.0F, 5.0F, CubeDeformation.NONE),
            PartPose.ZERO);
        
        // Prong 2 - along X axis (rotated 90 degrees around Y)
        partdefinition.addOrReplaceChild("prong2",
            CubeListBuilder.create()
                .texOffs(0, 10)
                .addBox(-0.5F, -0.5F, -2.5F, 1.0F, 1.0F, 5.0F, CubeDeformation.NONE),
            PartPose.rotation(0.0F, (float)(Math.PI / 2), 0.0F));
        
        // Prong 3 - along Y axis (rotated 90 degrees around X and Y)
        partdefinition.addOrReplaceChild("prong3",
            CubeListBuilder.create()
                .texOffs(0, 10)
                .addBox(-0.5F, -0.5F, -2.5F, 1.0F, 1.0F, 5.0F, CubeDeformation.NONE),
            PartPose.rotation((float)(Math.PI / 2), (float)(Math.PI / 2), 0.0F));
        
        return LayerDefinition.create(meshdefinition, 64, 32);
    }
    
    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer,
                               int packedLight, int packedOverlay,
                               int color) {
        core.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        prong1.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        prong2.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        prong3.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}




