package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;

/**
 * Renderer for projectiles that should be invisible.
 * The visual effects come from particles spawned by the entity itself.
 * Used for: EntityFocusProjectile, EntityAlumentum
 */
@OnlyIn(Dist.CLIENT)
public class NoProjectileRenderer<T extends Entity> extends EntityRenderer<T> {
    
    private static final ResourceLocation BLANK = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/block/blank.png");
    
    public NoProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }
    
    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return BLANK;
    }
    
    @Override
    public void render(T entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Intentionally empty - these projectiles are invisible
        // Visual effects come from particles
    }
}




