package thaumcraft.client.models.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entities.construct.EntityArcaneBore;

/**
 * ArcaneBoreModel - Model for the Arcane Bore mining construct.
 * 
 * Based on the original ModelArcaneBore from 1.12.2.
 * Features a tripod base with a rotating mining head that points at targets.
 */
@OnlyIn(Dist.CLIENT)
public class ArcaneBoreModel extends EntityModel<EntityArcaneBore> {
    
    public static final ModelLayerLocation LAYER_LOCATION = 
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "arcane_bore"), "main");
    
    // Tripod parts
    private final ModelPart tripod;
    private final ModelPart leg1;
    private final ModelPart leg2;
    private final ModelPart leg3;
    private final ModelPart leg4;
    
    // Main body (rotates to aim)
    private final ModelPart base;
    
    public ArcaneBoreModel(ModelPart root) {
        this.tripod = root.getChild("tripod");
        this.leg1 = root.getChild("leg1");
        this.leg2 = root.getChild("leg2");
        this.leg3 = root.getChild("leg3");
        this.leg4 = root.getChild("leg4");
        this.base = root.getChild("base");
    }
    
    /**
     * Create the layer definition for the arcane bore model.
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        
        // Tripod center
        partdefinition.addOrReplaceChild("tripod",
            CubeListBuilder.create()
                .texOffs(13, 0)
                .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 2.0F, 3.0F, CubeDeformation.NONE),
            PartPose.offset(0.0F, 12.0F, 0.0F));
        
        // Legs (4 legs on tripod)
        partdefinition.addOrReplaceChild("leg1",
            CubeListBuilder.create()
                .texOffs(20, 10)
                .addBox(-1.0F, 1.0F, -1.0F, 2.0F, 13.0F, 2.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, 12.0F, 0.0F, 0.5236F, 0.0F, 0.0F));
        
        partdefinition.addOrReplaceChild("leg2",
            CubeListBuilder.create()
                .texOffs(20, 10)
                .addBox(-1.0F, 1.0F, -1.0F, 2.0F, 13.0F, 2.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, 12.0F, 0.0F, 0.5236F, 1.5708F, 0.0F));
        
        partdefinition.addOrReplaceChild("leg3",
            CubeListBuilder.create()
                .texOffs(20, 10)
                .addBox(-1.0F, 1.0F, -1.0F, 2.0F, 13.0F, 2.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, 12.0F, 0.0F, 0.5236F, 3.1416F, 0.0F));
        
        partdefinition.addOrReplaceChild("leg4",
            CubeListBuilder.create()
                .texOffs(20, 10)
                .addBox(-1.0F, 1.0F, -1.0F, 2.0F, 13.0F, 2.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, 12.0F, 0.0F, 0.5236F, 4.7124F, 0.0F));
        
        // Main base body (rotates to aim)
        PartDefinition basePart = partdefinition.addOrReplaceChild("base",
            CubeListBuilder.create()
                .texOffs(32, 0)
                .addBox(-3.0F, -6.0F, -3.0F, 6.0F, 6.0F, 6.0F, CubeDeformation.NONE),
            PartPose.offset(0.0F, 13.0F, 0.0F));
        
        // Crystal on back
        basePart.addOrReplaceChild("crystal",
            CubeListBuilder.create()
                .texOffs(32, 25)
                .addBox(-1.0F, -4.0F, 5.0F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE),
            PartPose.ZERO);
        
        // Dome base
        basePart.addOrReplaceChild("domebase",
            CubeListBuilder.create()
                .texOffs(32, 19)
                .addBox(-2.0F, -5.0F, 3.0F, 4.0F, 4.0F, 1.0F, CubeDeformation.NONE),
            PartPose.ZERO);
        
        // Dome
        basePart.addOrReplaceChild("dome",
            CubeListBuilder.create()
                .texOffs(44, 16)
                .addBox(-2.0F, -5.0F, 4.0F, 4.0F, 4.0F, 4.0F, CubeDeformation.NONE),
            PartPose.ZERO);
        
        // Magnet base (front)
        basePart.addOrReplaceChild("magbase",
            CubeListBuilder.create()
                .texOffs(0, 18)
                .addBox(-1.0F, -4.0F, -6.0F, 2.0F, 2.0F, 3.0F, CubeDeformation.NONE),
            PartPose.ZERO);
        
        // Drill tip
        basePart.addOrReplaceChild("tip",
            CubeListBuilder.create()
                .texOffs(0, 9)
                .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 3.0F, 3.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, -3.0F, -6.0F, -1.5708F, 0.0F, 0.0F));
        
        return LayerDefinition.create(meshdefinition, 64, 32);
    }
    
    @Override
    public void setupAnim(EntityArcaneBore entity, float limbSwing, float limbSwingAmount, 
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // Base/head rotation (aims at target)
        this.base.yRot = netHeadYaw * ((float)Math.PI / 180F);
        this.base.xRot = headPitch * ((float)Math.PI / 180F);
    }
    
    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight,
                               int packedOverlay, int color) {
        // Render legs and tripod normally
        tripod.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        leg1.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        leg2.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        leg3.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        leg4.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);

        // Render base (potentially with transparency in full implementation)
        base.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}




