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
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entities.monster.tainted.EntityTaintSeed;

/**
 * TaintSeedModel - Model for the Taint Seed entity.
 * 
 * Features:
 * - Central core/body
 * - Multiple spreading tentacles radiating outward
 * - Animated tentacles that spread and retract
 * - Attack animation support
 * 
 * The TaintSeed is a stationary taint-spreading creature with
 * tentacles that extend outward from a central mass.
 */
@OnlyIn(Dist.CLIENT)
public class TaintSeedModel<T extends EntityTaintSeed> extends EntityModel<T> {
    
    public static final ModelLayerLocation LAYER_LOCATION = 
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "taint_seed"), "main");
    
    // Number of tentacles radiating from the center
    private static final int NUM_TENTACLES = 6;
    // Segments per tentacle
    private static final int SEGMENTS_PER_TENTACLE = 4;
    
    private final ModelPart core;
    private final ModelPart[][] tentacles; // [tentacle index][segment index]
    
    public TaintSeedModel(ModelPart root) {
        this.core = root.getChild("core");
        this.tentacles = new ModelPart[NUM_TENTACLES][SEGMENTS_PER_TENTACLE];
        
        for (int t = 0; t < NUM_TENTACLES; t++) {
            ModelPart current = this.core.getChild("tentacle" + t);
            for (int s = 0; s < SEGMENTS_PER_TENTACLE; s++) {
                this.tentacles[t][s] = current;
                if (s < SEGMENTS_PER_TENTACLE - 1) {
                    current = current.getChild("segment" + s);
                }
            }
        }
    }
    
    /**
     * Create the layer definition for the taint seed model.
     * 
     * Structure:
     * - core (central body mass)
     *   - tentacle0 through tentacle5 (radiating outward at different angles)
     *     - segment0 through segment2 (hierarchical chain per tentacle)
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        
        // Central core - fleshy mass at the center
        PartDefinition core = partdefinition.addOrReplaceChild("core",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-6.0F, -6.0F, -6.0F, 12.0F, 12.0F, 12.0F, CubeDeformation.NONE),
            PartPose.offset(0.0F, 18.0F, 0.0F));
        
        // Create tentacles radiating outward at different angles
        float angleStep = (float)(Math.PI * 2.0 / NUM_TENTACLES);
        
        for (int t = 0; t < NUM_TENTACLES; t++) {
            float angle = t * angleStep;
            float xOffset = Mth.cos(angle) * 5.0F;
            float zOffset = Mth.sin(angle) * 5.0F;
            
            // First segment of tentacle - attached to core
            PartDefinition tentacle = core.addOrReplaceChild("tentacle" + t,
                CubeListBuilder.create()
                    .texOffs(0, 24)
                    .addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(xOffset, 0.0F, zOffset, 
                    0.2F, // Slight downward tilt
                    angle, // Rotate to face outward
                    0.0F));
            
            // Chain of segments
            PartDefinition currentParent = tentacle;
            for (int s = 0; s < SEGMENTS_PER_TENTACLE - 1; s++) {
                float size = 5.0F - s * 1.0F; // Progressively smaller
                float halfSize = size / 2.0F;
                
                currentParent = currentParent.addOrReplaceChild("segment" + s,
                    CubeListBuilder.create()
                        .texOffs(0, 36)
                        .addBox(-halfSize, -halfSize, -halfSize, size, size, size, CubeDeformation.NONE),
                    PartPose.offset(0.0F, 0.0F, -5.0F + s)); // Extend outward
            }
        }
        
        return LayerDefinition.create(meshdefinition, 64, 64);
    }
    
    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, 
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // Get attack animation from entity
        float attackAnim = entity.attackAnim;
        float hurtTime = entity.hurtTime / 200.0f;
        
        // Base flail intensity for idle animation
        float flailIntensity = 0.1f * 3.0f;
        
        // Animate each tentacle
        for (int t = 0; t < NUM_TENTACLES; t++) {
            float tentacleOffset = t * 0.5f; // Offset phase for each tentacle
            
            for (int s = 0; s < SEGMENTS_PER_TENTACLE; s++) {
                ModelPart segment = this.tentacles[t][s];
                
                // Base spreading animation - tentacles spread outward
                float spreadAngle = 0.2f + 0.01f * s * s + hurtTime + attackAnim;
                segment.xRot = spreadAngle;
                
                // Side-to-side wave motion
                segment.zRot = 0.1f / flailIntensity * 
                        Mth.sin(ageInTicks * 0.05f - s / 2.0f + tentacleOffset) / 5.0f;
            }
        }
        
        // Pulse the core slightly
        // (Model parts don't have scale in 1.20.1 setupAnim, so we use rotation)
        float pulse = Mth.sin(ageInTicks * 0.1f) * 0.02f;
        this.core.xRot = pulse;
        this.core.zRot = -pulse;
    }
    
    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer,
                               int packedLight, int packedOverlay,
                               int color) {
        this.core.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }

    /**
     * Render with transparency for taint effect.
     */
    public void renderWithAlpha(PoseStack poseStack, VertexConsumer vertexConsumer,
                                int packedLight, int packedOverlay, float alpha) {
        int _c = ((int)(alpha*255.0F)<<24)|((int)(0.82F*255.0F)<<16)|((int)(0.82F*255.0F)<<8)|((int)(0.82F*255.0F));
        renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, _c);
    }
}




