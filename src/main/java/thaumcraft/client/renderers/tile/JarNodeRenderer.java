package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.common.tiles.TileJarNode;

/**
 * Block entity renderer for the "Node in a Jar" block.
 *
 * TC4 TileJarRenderer renders a TileJarNode by delegating to the normal node
 * renderer (TileNodeRenderer.renderTileEntityAt) with the model translated 0.1
 * blocks down and a size of 0.7. The node inside a jar is always visible - no
 * goggles of revealing or thaumometer are needed (TC4 forces condition = true
 * for TileJarNode).
 */
@OnlyIn(Dist.CLIENT)
public class JarNodeRenderer implements BlockEntityRenderer<TileJarNode> {

    /** TC4: the jar node is drawn while the viewer is within 64 blocks. */
    private static final float VIEW_DISTANCE = 64.0F;
    /** TC4 TileJarRenderer: node size 0.7 inside the jar. */
    private static final float NODE_SIZE = 0.7F;
    /** TC4 TileJarRenderer: glTranslatef(0.0F, -0.1F, 0.0F) relative to the block centre. */
    private static final double NODE_Y = 0.4D;

    public JarNodeRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public int getViewDistance() {
        return (int) VIEW_DISTANCE;
    }

    @Override
    public void render(TileJarNode tile, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (tile.getNodeType() == null) {
            return;
        }

        Vec3 center = Vec3.atCenterOf(tile.getBlockPos());
        double distance = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceTo(center);
        if (distance > VIEW_DISTANCE) {
            return;
        }

        float alpha = Mth.clamp((VIEW_DISTANCE - (float) distance) / VIEW_DISTANCE, 0.05F, 1.0F);
        float age = tile.getLevel() != null ? tile.getLevel().getGameTime() + partialTick : 0.0F;

        TileNodeRenderer.renderNodeBillboard(poseStack, buffer, 0.5D, NODE_Y, 0.5D,
                tile.getAspects(), tile.getNodeType(), tile.getNodeModifier(),
                System.nanoTime(), age, alpha, NODE_SIZE, true, false, tile.getBlockPos().getX());
    }
}
