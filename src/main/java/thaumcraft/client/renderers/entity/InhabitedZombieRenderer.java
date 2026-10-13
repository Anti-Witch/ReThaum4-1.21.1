package thaumcraft.client.renderers.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;

/**
 * Renderer for Inhabited Zombies - zombies containing eldritch crabs.
 * Uses vanilla zombie model with custom texture.
 */
@OnlyIn(Dist.CLIENT)
public class InhabitedZombieRenderer extends ZombieRenderer {
    
    private static final ResourceLocation TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/czombie.png");
    
    public InhabitedZombieRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
    
    @Override
    public ResourceLocation getTextureLocation(net.minecraft.world.entity.monster.Zombie entity) {
        return TEXTURE;
    }
}




