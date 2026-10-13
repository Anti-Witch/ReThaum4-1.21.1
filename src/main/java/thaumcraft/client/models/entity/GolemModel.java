package thaumcraft.client.models.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.ArmedModel;
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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.common.golems.EntityThaumcraftGolem;

/**
 * GolemModel - Basic model for Thaumcraft golems.
 * 
 * A simplified biped-like model with:
 * - Head
 * - Body/Chest
 * - Two arms
 * - Two legs
 * 
 * This is a placeholder model. The original used OBJ models with
 * swappable parts based on golem configuration.
 */
@OnlyIn(Dist.CLIENT)
public class GolemModel extends EntityModel<EntityThaumcraftGolem> implements ArmedModel {
    
    public static final ModelLayerLocation LAYER_LOCATION = 
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "golem"), "main");
    
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    
    public GolemModel(ModelPart root) {
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }
    
    /**
     * Create the layer definition for the golem model.
     * This defines the geometry of all model parts.
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        
        // Head - cubic head on top
        partdefinition.addOrReplaceChild("head", 
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-3.0F, -6.0F, -3.0F, 6.0F, 6.0F, 6.0F, CubeDeformation.NONE),
            PartPose.offset(0.0F, 8.0F, 0.0F));
        
        // Body - main torso
        partdefinition.addOrReplaceChild("body", 
            CubeListBuilder.create()
                .texOffs(0, 12)
                .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 8.0F, 4.0F, CubeDeformation.NONE),
            PartPose.offset(0.0F, 8.0F, 0.0F));
        
        // Right Arm
        partdefinition.addOrReplaceChild("right_arm", 
            CubeListBuilder.create()
                .texOffs(24, 12)
                .addBox(-2.0F, -1.0F, -1.5F, 2.0F, 8.0F, 3.0F, CubeDeformation.NONE),
            PartPose.offset(-4.0F, 9.0F, 0.0F));
        
        // Left Arm
        partdefinition.addOrReplaceChild("left_arm", 
            CubeListBuilder.create()
                .texOffs(24, 12).mirror()
                .addBox(0.0F, -1.0F, -1.5F, 2.0F, 8.0F, 3.0F, CubeDeformation.NONE),
            PartPose.offset(4.0F, 9.0F, 0.0F));
        
        // Right Leg
        partdefinition.addOrReplaceChild("right_leg", 
            CubeListBuilder.create()
                .texOffs(0, 24)
                .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 8.0F, 3.0F, CubeDeformation.NONE),
            PartPose.offset(-2.0F, 16.0F, 0.0F));
        
        // Left Leg
        partdefinition.addOrReplaceChild("left_leg", 
            CubeListBuilder.create()
                .texOffs(0, 24).mirror()
                .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 8.0F, 3.0F, CubeDeformation.NONE),
            PartPose.offset(2.0F, 16.0F, 0.0F));
        
        return LayerDefinition.create(meshdefinition, 64, 64);
    }
    
    @Override
    public void setupAnim(EntityThaumcraftGolem entity, float limbSwing, float limbSwingAmount, 
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // Head rotation
        this.head.yRot = netHeadYaw * ((float)Math.PI / 180F);
        this.head.xRot = headPitch * ((float)Math.PI / 180F);
        
        // Reset rotations
        this.rightArm.xRot = 0.0F;
        this.leftArm.xRot = 0.0F;
        this.rightLeg.xRot = 0.0F;
        this.leftLeg.xRot = 0.0F;
        
        // Walking animation
        this.rightArm.xRot = Mth.cos(limbSwing * 0.6662F + (float)Math.PI) * 2.0F * limbSwingAmount * 0.5F;
        this.leftArm.xRot = Mth.cos(limbSwing * 0.6662F) * 2.0F * limbSwingAmount * 0.5F;
        this.rightLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        this.leftLeg.xRot = Mth.cos(limbSwing * 0.6662F + (float)Math.PI) * 1.4F * limbSwingAmount;
        
        // Slight arm swing for idle
        this.rightArm.zRot = Mth.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        this.leftArm.zRot = -Mth.cos(ageInTicks * 0.09F) * 0.05F - 0.05F;
        
        // If holding item, adjust arm
        if (!entity.getMainHandItem().isEmpty()) {
            this.rightArm.xRot = -0.5F;
            this.leftArm.xRot = -0.5F;
        }
        
        // Swing animation (for attacking/interacting)
        if (entity.swinging) {
            float swingProgress = entity.getAttackAnim(ageInTicks - (int)ageInTicks);
            float swing = Mth.sin(Mth.sqrt(swingProgress) * (float)Math.PI * 2.0F) * 0.2F;
            this.rightArm.xRot -= swing * 2.0F;
        }
    }
    
    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight,
                               int packedOverlay, int color) {
        head.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        body.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        rightArm.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        leftArm.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        rightLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        leftLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
    
    @Override
    public void translateToHand(HumanoidArm arm, PoseStack poseStack) {
        ModelPart modelPart = arm == HumanoidArm.LEFT ? this.leftArm : this.rightArm;
        modelPart.translateAndRotate(poseStack);
        // Translate to the end of the arm where items should be held
        poseStack.translate(0.0F, 0.4F, 0.0F);
    }
}




