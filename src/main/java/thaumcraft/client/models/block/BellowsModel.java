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
 * Model for the Bellows block entity.
 * Features animated accordion-style bag that compresses/expands.
 */
@OnlyIn(Dist.CLIENT)
public class BellowsModel extends Model {

    public static final ModelLayerLocation LAYER_LOCATION = 
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "bellows"), "main");

    private final ModelPart bottomPlank;
    private final ModelPart middlePlank;
    private final ModelPart topPlank;
    private final ModelPart bag;
    private final ModelPart nozzle;

    public BellowsModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.bottomPlank = root.getChild("bottom_plank");
        this.middlePlank = root.getChild("middle_plank");
        this.topPlank = root.getChild("top_plank");
        this.bag = root.getChild("bag");
        this.nozzle = root.getChild("nozzle");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // Bottom plank: 12x2x12 at y=22 (near bottom of block)
        partdefinition.addOrReplaceChild("bottom_plank", CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-6.0f, 0.0f, -6.0f, 12.0f, 2.0f, 12.0f),
                PartPose.offset(0.0f, 22.0f, 0.0f));

        // Middle plank: 12x2x12 at y=16 (middle of block)
        partdefinition.addOrReplaceChild("middle_plank", CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-6.0f, -1.0f, -6.0f, 12.0f, 2.0f, 12.0f),
                PartPose.offset(0.0f, 16.0f, 0.0f));

        // Top plank: 12x2x12 at y=8 (near top of block)
        partdefinition.addOrReplaceChild("top_plank", CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-6.0f, 0.0f, -6.0f, 12.0f, 2.0f, 12.0f),
                PartPose.offset(0.0f, 8.0f, 0.0f));

        // Bag (accordion): 20x24x20 centered at y=16
        partdefinition.addOrReplaceChild("bag", CubeListBuilder.create()
                .texOffs(48, 0)
                .addBox(-10.0f, -12.0f, -10.0f, 20.0f, 24.0f, 20.0f),
                PartPose.offset(0.0f, 16.0f, 0.0f));

        // Nozzle: 4x4x2 at front
        partdefinition.addOrReplaceChild("nozzle", CubeListBuilder.create()
                .texOffs(0, 36)
                .addBox(-2.0f, -2.0f, 0.0f, 4.0f, 4.0f, 2.0f),
                PartPose.offset(0.0f, 16.0f, 6.0f));

        return LayerDefinition.create(meshdefinition, 128, 64);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, int color) {
        bottomPlank.render(poseStack, buffer, packedLight, packedOverlay, color);
        middlePlank.render(poseStack, buffer, packedLight, packedOverlay, color);
        topPlank.render(poseStack, buffer, packedLight, packedOverlay, color);
        bag.render(poseStack, buffer, packedLight, packedOverlay, color);
        nozzle.render(poseStack, buffer, packedLight, packedOverlay, color);
    }

    /**
     * Set the inflation/compression state of the bellows.
     * Mirrors TC4's {@code TileBellowsRenderer}: the bag is scaled by
     * {@code (inflation + 0.1) / 2} on Y (and 0.5 on X/Z) while the outer planks
     * slide by {@code +-(0.5 - tscale / 2)} blocks, {@code tscale = 0.125 + inflation * 0.875}.
     * @param inflation 0.0 = fully compressed, 1.0 = fully expanded
     */
    public void setInflation(float inflation) {
        float tscale = 0.125f + inflation * 0.875f;
        // Bag: TC4 glScalef(0.5, (scale + 0.1) / 2, 0.5)
        bag.xScale = 0.5f;
        bag.zScale = 0.5f;
        bag.yScale = (inflation + 0.1f) / 2.0f;

        // TC4: TopPlank translate(0, -tscale/2 + 0.5, 0); BottomPlank translate(0, tscale/2 - 0.5, 0)
        topPlank.y = 8.0f + (0.5f - tscale / 2.0f) * 16.0f;
        bottomPlank.y = 22.0f - (0.5f - tscale / 2.0f) * 16.0f;
    }

    /**
     * Get the top plank for custom rendering.
     */
    public ModelPart getTopPlank() {
        return topPlank;
    }

    /**
     * Get the bottom plank for custom rendering.
     */
    public ModelPart getBottomPlank() {
        return bottomPlank;
    }

    /**
     * Get the bag for custom rendering.
     */
    public ModelPart getBag() {
        return bag;
    }
}




