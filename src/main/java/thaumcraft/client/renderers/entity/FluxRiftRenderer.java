package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import thaumcraft.common.entities.EntityFluxRift;

import java.util.List;

/**
 * Renderer for Flux Rifts - tears in the magical fabric of reality.
 * 
 * The original used the GLE library to render extruded polyline tubes with
 * end portal shaders. This simplified version uses standard vertex rendering
 * to create a similar effect using connected quads.
 * 
 * Features:
 * - Jagged tear appearance following stored point data
 * - Purple/void color scheme
 * - Stability-based wobble animation
 * - Emissive rendering for glow effect
 */
@OnlyIn(Dist.CLIENT)
public class FluxRiftRenderer extends EntityRenderer<EntityFluxRift> {
    
    private static final ResourceLocation RIFT_TEXTURE = 
            ResourceLocation.withDefaultNamespace("textures/entity/end_portal.png");
    
    public FluxRiftRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
        this.shadowStrength = 0.0F;
    }
    
    @Override
    public ResourceLocation getTextureLocation(EntityFluxRift entity) {
        return RIFT_TEXTURE;
    }
    
    @Override
    public void render(EntityFluxRift entity, float entityYaw, float partialTicks, 
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        List<Vec3> points = entity.getPoints();
        List<Float> widths = entity.getPointWidths();
        
        if (points == null || points.size() < 2 || widths == null || widths.size() < 2) {
            return;
        }
        
        poseStack.pushPose();
        
        // Calculate wobble based on stability
        float stability = entity.getRiftStability();
        float stab = Mth.clamp(1.0F - stability / 50.0F, 0.0F, 1.5F);
        float time = entity.tickCount + partialTicks;
        
        // Get render type - use end portal effect
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.endPortal());
        
        // Render the rift as connected segments
        for (int i = 0; i < points.size() - 1; i++) {
            Vec3 start = points.get(i);
            Vec3 end = points.get(i + 1);
            
            // Add wobble animation
            float varStart = time + (i > points.size() / 2 ? -i * 10 : i * 10);
            float varEnd = time + ((i + 1) > points.size() / 2 ? -(i + 1) * 10 : (i + 1) * 10);
            
            double wobbleX1 = Math.sin(varStart / 50.0) * 0.1 * stab;
            double wobbleY1 = Math.sin(varStart / 60.0) * 0.1 * stab;
            double wobbleZ1 = Math.sin(varStart / 70.0) * 0.1 * stab;
            
            double wobbleX2 = Math.sin(varEnd / 50.0) * 0.1 * stab;
            double wobbleY2 = Math.sin(varEnd / 60.0) * 0.1 * stab;
            double wobbleZ2 = Math.sin(varEnd / 70.0) * 0.1 * stab;
            
            float w1 = widths.get(i) * (float)(1.0 - Math.sin(varStart / 8.0) * 0.1 * stab);
            float w2 = widths.get(i + 1) * (float)(1.0 - Math.sin(varEnd / 8.0) * 0.1 * stab);
            
            // Render segment
            renderSegment(poseStack, vertexConsumer,
                    (float)(start.x + wobbleX1), (float)(start.y + wobbleY1), (float)(start.z + wobbleZ1), w1,
                    (float)(end.x + wobbleX2), (float)(end.y + wobbleY2), (float)(end.z + wobbleZ2), w2);
        }
        
        poseStack.popPose();
        
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
    
    /**
     * Renders a single segment of the rift as a rectangular tube.
     */
    private void renderSegment(PoseStack poseStack, VertexConsumer vertexConsumer,
                              float x1, float y1, float z1, float w1,
                              float x2, float y2, float z2, float w2) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        PoseStack.Pose normal = pose;
        
        // Calculate direction and perpendicular vectors
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float length = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        
        if (length < 0.001F) return;
        
        // Normalize direction
        dx /= length;
        dy /= length;
        dz /= length;
        
        // Get perpendicular vectors (cross with up, or fallback)
        float px, py, pz;
        if (Math.abs(dy) < 0.99F) {
            px = -dz;
            py = 0;
            pz = dx;
        } else {
            px = 1;
            py = 0;
            pz = 0;
        }
        
        float plen = Mth.sqrt(px * px + py * py + pz * pz);
        px /= plen;
        py /= plen;
        pz /= plen;
        
        // Second perpendicular (cross product of direction and first perp)
        float qx = dy * pz - dz * py;
        float qy = dz * px - dx * pz;
        float qz = dx * py - dy * px;
        
        // Render 4 faces of a rectangular tube
        for (int face = 0; face < 4; face++) {
            float angle1 = face * Mth.HALF_PI;
            float angle2 = (face + 1) * Mth.HALF_PI;
            
            float cos1 = Mth.cos(angle1);
            float sin1 = Mth.sin(angle1);
            float cos2 = Mth.cos(angle2);
            float sin2 = Mth.sin(angle2);
            
            // Calculate corner offsets
            float ox1a = (px * cos1 + qx * sin1) * w1;
            float oy1a = (py * cos1 + qy * sin1) * w1;
            float oz1a = (pz * cos1 + qz * sin1) * w1;
            
            float ox1b = (px * cos2 + qx * sin2) * w1;
            float oy1b = (py * cos2 + qy * sin2) * w1;
            float oz1b = (pz * cos2 + qz * sin2) * w1;
            
            float ox2a = (px * cos1 + qx * sin1) * w2;
            float oy2a = (py * cos1 + qy * sin1) * w2;
            float oz2a = (pz * cos1 + qz * sin1) * w2;
            
            float ox2b = (px * cos2 + qx * sin2) * w2;
            float oy2b = (py * cos2 + qy * sin2) * w2;
            float oz2b = (pz * cos2 + qz * sin2) * w2;
            
            // Render quad (end portal doesn't use normal UV mapping)
            vertexConsumer.addVertex(matrix, x1 + ox1a, y1 + oy1a, z1 + oz1a);
            vertexConsumer.addVertex(matrix, x1 + ox1b, y1 + oy1b, z1 + oz1b);
            vertexConsumer.addVertex(matrix, x2 + ox2b, y2 + oy2b, z2 + oz2b);
            vertexConsumer.addVertex(matrix, x2 + ox2a, y2 + oy2a, z2 + oz2a);
        }
    }
}




