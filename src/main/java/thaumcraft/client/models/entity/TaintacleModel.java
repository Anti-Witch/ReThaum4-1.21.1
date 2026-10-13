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
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entities.monster.tainted.EntityTaintacle;

/**
 * TaintacleModel - Model for Taintacles (tainted tentacles).
 * 
 * Features:
 * - Segmented tentacle body with hierarchical structure
 * - Wave-based animation using sin functions
 * - Tip orb and head segment for larger taintacles
 * - Supports variable length through animation scaling
 * 
 * The original model used a custom ModelRendererTaintacle that scaled
 * children progressively smaller. In 1.20.1, we achieve this with
 * careful positioning and animation.
 */
@OnlyIn(Dist.CLIENT)
public class TaintacleModel<T extends Entity> extends EntityModel<T> {
    
    public static final ModelLayerLocation LAYER_LOCATION = 
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "taintacle"), "main");
    
    // Number of segments in the tentacle
    private static final int NUM_SEGMENTS = 8;
    
    private final ModelPart base;
    private final ModelPart[] segments;
    private final ModelPart tipOrb;
    private final ModelPart head;
    
    public TaintacleModel(ModelPart root) {
        this.base = root.getChild("base");
        this.segments = new ModelPart[NUM_SEGMENTS];
        
        // Build segment chain - each segment is child of previous
        ModelPart current = this.base;
        for (int i = 0; i < NUM_SEGMENTS; i++) {
            this.segments[i] = current.getChild("segment" + i);
            current = this.segments[i];
        }
        
        // Tip orb and head are children of the last segment
        this.tipOrb = this.segments[NUM_SEGMENTS - 1].getChild("tip_orb");
        this.head = this.segments[NUM_SEGMENTS - 1].getChild("head");
    }
    
    /**
     * Create the layer definition for the taintacle model.
     * 
     * Structure:
     * - base (8x8x8 cube at ground level)
     *   - segment0 (8x8x8, offset up)
     *     - segment1
     *       - segment2
     *         - ... (hierarchical chain)
     *           - segmentN-1
     *             - tip_orb (4x4x4 small orb)
     *             - head (12x12x12 large head)
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        
        // Base segment - anchored at ground level
        PartDefinition base = partdefinition.addOrReplaceChild("base",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F, CubeDeformation.NONE),
            PartPose.offset(0.0F, 12.0F, 0.0F));
        
        // Build segment chain hierarchically
        PartDefinition currentParent = base;
        for (int i = 0; i < NUM_SEGMENTS; i++) {
            // Each segment is slightly smaller and offset upward from its parent
            float scale = 1.0F - (i * 0.05F); // Gradually smaller
            float size = 8.0F * scale;
            float halfSize = size / 2.0F;
            
            currentParent = currentParent.addOrReplaceChild("segment" + i,
                CubeListBuilder.create()
                    .texOffs(0, 16)
                    .addBox(-halfSize, -size, -halfSize, size, size, size, CubeDeformation.NONE),
                PartPose.offset(0.0F, -4.0F, 0.0F)); // Offset up from parent center
        }
        
        // Tip orb - small glowing orb near the tip
        currentParent.addOrReplaceChild("tip_orb",
            CubeListBuilder.create()
                .texOffs(0, 56)
                .addBox(-2.0F, -4.0F, -2.0F, 4.0F, 4.0F, 4.0F, CubeDeformation.NONE),
            PartPose.offset(0.0F, -4.0F, 0.0F));
        
        // Head - larger segment at the very tip
        currentParent.addOrReplaceChild("head",
            CubeListBuilder.create()
                .texOffs(0, 32)
                .addBox(-6.0F, -12.0F, -6.0F, 12.0F, 12.0F, 12.0F, CubeDeformation.NONE),
            PartPose.offset(0.0F, -4.0F, 0.0F));
        
        return LayerDefinition.create(meshdefinition, 64, 64);
    }
    
    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, 
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // Get flail intensity from entity if it's a Taintacle
        float flailIntensity = 1.0F;
        float hurtTime = 0.0F;
        
        if (entity instanceof EntityTaintacle taintacle) {
            flailIntensity = taintacle.flailIntensity;
            hurtTime = taintacle.hurtTime;
        }
        
        // Calculate animation modifiers
        float mod = 0.2F;
        float speedFactor = (flailIntensity > 1.0F) ? 3.0F : (1.0F + ((flailIntensity > 1.0F) ? mod : -mod));
        float intensityFactor = flailIntensity + ((hurtTime > 0.0F) ? mod : -mod);
        
        // Reset base rotation
        this.base.xRot = 0.0F;
        this.base.zRot = 0.0F;
        
        // Animate each segment with wave motion
        // Each segment rotates slightly based on time, creating a wave effect
        for (int i = 0; i < NUM_SEGMENTS; i++) {
            // X rotation creates the main wave motion (forward/back swaying)
            this.segments[i].xRot = 0.15F * intensityFactor * 
                    Mth.sin(ageInTicks * 0.1F * speedFactor - i / 2.0F);
            
            // Z rotation adds secondary side-to-side motion
            this.segments[i].zRot = 0.1F / intensityFactor * 
                    Mth.sin(ageInTicks * 0.15F - i / 2.0F);
        }
        
        // Head/tip follows the wave but with slight delay
        float tipWave = Mth.sin(ageInTicks * 0.1F * speedFactor - NUM_SEGMENTS / 2.0F);
        this.tipOrb.xRot = tipWave * 0.1F;
        this.head.xRot = tipWave * 0.1F;
    }
    
    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer,
                               int packedLight, int packedOverlay,
                               int color) {
        // Render the base, which recursively renders all children
        this.base.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }

    /**
     * Render with transparency for taint effect.
     * Called by the renderer for the semi-transparent taint look.
     */
    public void renderWithAlpha(PoseStack poseStack, VertexConsumer vertexConsumer,
                                int packedLight, int packedOverlay, float alpha) {
        // Tainted purple-ish color with transparency
        int _c = ((int)(alpha*255.0F)<<24)|((int)(0.88F*255.0F)<<16)|((int)(0.88F*255.0F)<<8)|((int)(0.88F*255.0F));
        renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, _c);
    }
}




