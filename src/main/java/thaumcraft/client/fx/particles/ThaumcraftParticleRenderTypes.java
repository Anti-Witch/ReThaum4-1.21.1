package thaumcraft.client.fx.particles;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Custom particle render types that bind the Thaumcraft particle sheet
 * (textures/misc/particles.png) instead of the vanilla particle atlas.
 *
 * The Thaumcraft FX particles (FXGeneric, FXFireMote, etc.) compute their UV
 * coordinates against this 64x64-grid sheet, so they must be drawn with it
 * bound. Using a dedicated render type also keeps them batched separately from
 * vanilla PARTICLE_SHEET particles, so vanilla effects are unaffected.
 */
@OnlyIn(Dist.CLIENT)
public final class ThaumcraftParticleRenderTypes {

    private static final ResourceLocation PARTICLE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/particles.png");

    public static final ParticleRenderType THAUMCRAFT_PARTICLE_SHEET_TRANSLUCENT = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
            RenderSystem.depthMask(false);
            RenderSystem.setShaderTexture(0, PARTICLE_TEXTURE);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getParticleShader);
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public String toString() {
            return "thaumcraft_particle_sheet_translucent";
        }
    };

    public static final ParticleRenderType THAUMCRAFT_PARTICLE_SHEET_LIT = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
            RenderSystem.depthMask(false);
            RenderSystem.setShaderTexture(0, PARTICLE_TEXTURE);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getParticleShader);
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public String toString() {
            return "thaumcraft_particle_sheet_lit";
        }
    };

    private ThaumcraftParticleRenderTypes() {}
}
