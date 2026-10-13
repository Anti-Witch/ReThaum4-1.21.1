package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.basic.BlockCandle;
import thaumcraft.common.tiles.devices.TileCandle;

/**
 * Block entity renderer for the 16 dyed candles.
 *
 * Direct port of TC4's BlockCandleRenderer#renderWorldBlock:
 * <ul>
 *   <li>wax block:      x/z 6..10, y 0..0.5</li>
 *   <li>1..5 extra candles: laid out from a position seeded Random
 *       (TC4: {@code new Random(x + (long) y * z)}), alternating north/south
 *       and west/east rows, 1..3 px tall</li>
 *   <li>wick + flame:   x/z 0.475..0.525, y 0.5..0.625, drawn untinted</li>
 * </ul>
 * The wax is tinted with the dye colour of the block (same colour the
 * BlockColors handler gives the item model), the wick keeps the texture colour.
 * Both textures live in the block atlas, so the animated {@code block/candle.png}
 * flickers exactly like it did in TC4.
 */
@OnlyIn(Dist.CLIENT)
public class CandleRenderer implements BlockEntityRenderer<TileCandle> {

    private static final ResourceLocation TEX_WAX =
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "block/candle");
    private static final ResourceLocation TEX_STUB =
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "block/candlestub");

    /** TC4 W1: one texture pixel in block units. */
    private static final float P = 1.0F / 16.0F;

    public CandleRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TileCandle tile, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        BlockState state = tile.getBlockState();
        DyeColor dye = state.getBlock() instanceof BlockCandle candle ? candle.getColor() : DyeColor.WHITE;
        int tint = dye.getTextColor();
        float red = ((tint >> 16) & 0xFF) / 255.0F;
        float green = ((tint >> 8) & 0xFF) / 255.0F;
        float blue = (tint & 0xFF) / 255.0F;

        TextureAtlasSprite wax = sprite(TEX_WAX);
        TextureAtlasSprite stub = sprite(TEX_STUB);
        VertexConsumer consumer = buffer.getBuffer(RenderType.cutout());

        BlockPos pos = tile.getBlockPos();
        // TC4: Random rr = new Random(x + (long) y * z);
        RandomSource rand = RandomSource.create(pos.getX() + (long) pos.getY() * pos.getZ());

        // The main wax block: block.setBlockBounds(W6, 0, W6, W10, 0.5F, W10)
        box(poseStack, consumer, wax, 6.0F * P, 0.0F, 6.0F * P, 10.0F * P, 0.5F, 10.0F * P,
                red, green, blue, packedLight, packedOverlay);

        // 1..5 extra candles around the base
        int count = 1 + rand.nextInt(5);
        for (int a = 0; a < count; a++) {
            boolean side = rand.nextBoolean();
            int loc = 2 + rand.nextInt(2);
            float height = P * (1 + rand.nextInt(3));
            float x0;
            float x1;
            float z0;
            float z1;
            if (a % 2 == 0) {
                x0 = P * (5 + loc);
                x1 = P * (6 + loc);
                z0 = P * (side ? 5 : 10);
                z1 = P * (side ? 6 : 11);
            } else {
                x0 = P * (side ? 5 : 10);
                x1 = P * (side ? 6 : 11);
                z0 = P * (5 + loc);
                z1 = P * (6 + loc);
            }
            box(poseStack, consumer, wax, x0, 0.0F, z0, x1, height, z1,
                    red, green, blue, packedLight, packedOverlay);
        }

        // Central wick + flame, TC4 draws it with an override texture and no tint
        box(poseStack, consumer, stub, 0.475F, 0.5F, 0.475F, 0.525F, 10.0F * P, 0.525F,
                1.0F, 1.0F, 1.0F, packedLight, packedOverlay);
    }

    private static TextureAtlasSprite sprite(ResourceLocation texture) {
        return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(texture);
    }

    /**
     * Draws one axis aligned, fully textured box (vanilla cube UV layout: the
     * whole sprite is stretched over every face, exactly like TC4's drawFaces).
     */
    private static void box(PoseStack poseStack, VertexConsumer consumer, TextureAtlasSprite sprite,
                            float x0, float y0, float z0, float x1, float y1, float z1,
                            float red, float green, float blue, int packedLight, int packedOverlay) {
        Matrix4f matrix = poseStack.last().pose();
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        // down
        vertex(consumer, matrix, x0, y0, z1, u0, v1, red, green, blue, packedLight, packedOverlay, 0, -1, 0);
        vertex(consumer, matrix, x1, y0, z1, u1, v1, red, green, blue, packedLight, packedOverlay, 0, -1, 0);
        vertex(consumer, matrix, x1, y0, z0, u1, v0, red, green, blue, packedLight, packedOverlay, 0, -1, 0);
        vertex(consumer, matrix, x0, y0, z0, u0, v0, red, green, blue, packedLight, packedOverlay, 0, -1, 0);
        // up
        vertex(consumer, matrix, x0, y1, z0, u0, v0, red, green, blue, packedLight, packedOverlay, 0, 1, 0);
        vertex(consumer, matrix, x0, y1, z1, u0, v1, red, green, blue, packedLight, packedOverlay, 0, 1, 0);
        vertex(consumer, matrix, x1, y1, z1, u1, v1, red, green, blue, packedLight, packedOverlay, 0, 1, 0);
        vertex(consumer, matrix, x1, y1, z0, u1, v0, red, green, blue, packedLight, packedOverlay, 0, 1, 0);
        // north
        vertex(consumer, matrix, x0, y0, z0, u0, v1, red, green, blue, packedLight, packedOverlay, 0, 0, -1);
        vertex(consumer, matrix, x0, y1, z0, u0, v0, red, green, blue, packedLight, packedOverlay, 0, 0, -1);
        vertex(consumer, matrix, x1, y1, z0, u1, v0, red, green, blue, packedLight, packedOverlay, 0, 0, -1);
        vertex(consumer, matrix, x1, y0, z0, u1, v1, red, green, blue, packedLight, packedOverlay, 0, 0, -1);
        // south
        vertex(consumer, matrix, x0, y0, z1, u1, v1, red, green, blue, packedLight, packedOverlay, 0, 0, 1);
        vertex(consumer, matrix, x1, y0, z1, u0, v1, red, green, blue, packedLight, packedOverlay, 0, 0, 1);
        vertex(consumer, matrix, x1, y1, z1, u0, v0, red, green, blue, packedLight, packedOverlay, 0, 0, 1);
        vertex(consumer, matrix, x0, y1, z1, u1, v0, red, green, blue, packedLight, packedOverlay, 0, 0, 1);
        // west
        vertex(consumer, matrix, x0, y0, z0, u0, v1, red, green, blue, packedLight, packedOverlay, -1, 0, 0);
        vertex(consumer, matrix, x0, y0, z1, u1, v1, red, green, blue, packedLight, packedOverlay, -1, 0, 0);
        vertex(consumer, matrix, x0, y1, z1, u1, v0, red, green, blue, packedLight, packedOverlay, -1, 0, 0);
        vertex(consumer, matrix, x0, y1, z0, u0, v0, red, green, blue, packedLight, packedOverlay, -1, 0, 0);
        // east
        vertex(consumer, matrix, x1, y0, z0, u1, v1, red, green, blue, packedLight, packedOverlay, 1, 0, 0);
        vertex(consumer, matrix, x1, y1, z0, u1, v0, red, green, blue, packedLight, packedOverlay, 1, 0, 0);
        vertex(consumer, matrix, x1, y1, z1, u0, v0, red, green, blue, packedLight, packedOverlay, 1, 0, 0);
        vertex(consumer, matrix, x1, y0, z1, u0, v1, red, green, blue, packedLight, packedOverlay, 1, 0, 0);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix,
                               float x, float y, float z, float u, float v,
                               float red, float green, float blue,
                               int packedLight, int packedOverlay,
                               float normalX, float normalY, float normalZ) {
        consumer.addVertex(matrix, x, y, z)
                .setColor(red, green, blue, 1.0F)
                .setUv(u, v)
                .setOverlay(packedOverlay)
                .setUv2(packedLight & 0xFFFF, (packedLight >> 16) & 0xFFFF)
                .setNormal(normalX, normalY, normalZ);
    }
}
