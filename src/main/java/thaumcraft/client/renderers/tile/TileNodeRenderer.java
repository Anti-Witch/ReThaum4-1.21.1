package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;
import thaumcraft.common.tiles.TileNode;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.items.IRevealer;
import thaumcraft.common.items.tools.ItemThaumometer;

/**
 * TileNodeRenderer - Visual rendering of Aura Nodes in the world.
 *
 * Ported from TC4 (OpenTC4 TileNodeRenderer.renderNode): animated billboard
 * strips cut from textures/misc/nodes.png. The sheet is a 32x32 grid of 64px
 * cells: U selects the animation frame, V (row) selects the strip:
 *   0 = per-aspect colored swirls
 *   1 = NORMAL, 2 = DARK, 3 = HUNGRY, 4 = PURE, 5 = TAINTED, 6 = UNSTABLE
 * Node modifiers only affect opacity/pulsing (BRIGHT brighter, PALE dimmer,
 * FADING flicker), exactly like TC4.
 *
 * The same billboard is used by JarNodeRenderer for nodes trapped inside a jar
 * (TC4 TileJarRenderer delegates to TileNodeRenderer with size 0.7).
 */
public class TileNodeRenderer implements BlockEntityRenderer<TileNode> {

    public static final ResourceLocation NODES_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Thaumcraft.MODID, "textures/misc/nodes.png");

    private static final int FRAMES = 32;
    private static final float VIEW_DISTANCE = 64.0F;

    /** Accessor for protected vanilla render state shards. */
    private static final class NodeShards extends RenderStateShard {
        private NodeShards() {
            super("thaumcraft_node_shards", () -> {}, () -> {});
        }

        static final RenderStateShard.ShaderStateShard NODE_SHADER =
                RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER;
        static final RenderStateShard.TransparencyStateShard ADDITIVE =
                RenderStateShard.ADDITIVE_TRANSPARENCY;
        static final RenderStateShard.TransparencyStateShard TRANSLUCENT =
                RenderStateShard.TRANSLUCENT_TRANSPARENCY;
        static final RenderStateShard.WriteMaskStateShard COLOR_ONLY =
                RenderStateShard.COLOR_WRITE;
        static final RenderStateShard.CullStateShard NO_CULLING =
                RenderStateShard.NO_CULL;
        static final RenderStateShard.LightmapStateShard LIGHTMAP_STATE =
                RenderStateShard.LIGHTMAP;
        static final RenderStateShard.OverlayStateShard OVERLAY_STATE =
                RenderStateShard.OVERLAY;
    }

    private static RenderType createNodeType(String name, boolean additive, boolean depthTest) {
        return RenderType.create(
                name,
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                1024,
                false,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(NodeShards.NODE_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(NODES_TEXTURE, true, false))
                        .setTransparencyState(additive ? NodeShards.ADDITIVE : NodeShards.TRANSLUCENT)
                        .setWriteMaskState(NodeShards.COLOR_ONLY)
                        .setCullState(NodeShards.NO_CULLING)
                        .setDepthTestState(depthTest
                                ? RenderStateShard.LEQUAL_DEPTH_TEST
                                : RenderStateShard.NO_DEPTH_TEST)
                        .setLightmapState(NodeShards.LIGHTMAP_STATE)
                        .setOverlayState(NodeShards.OVERLAY_STATE)
                        .createCompositeState(false));
    }

    /** Additive blend - TC4 glBlendFunc(GL_SRC_ALPHA, GL_ONE). */
    private static final RenderType NODE_ADDITIVE = createNodeType("thaumcraft_node_additive", true, true);
    /** Standard alpha blend - TC4 glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA). */
    private static final RenderType NODE_TRANSLUCENT = createNodeType("thaumcraft_node_translucent", false, true);
    /** Same as above with depth test disabled - TC4 GL_DEPTH_TEST disable (x-ray through blocks). */
    private static final RenderType NODE_ADDITIVE_DEPTHLESS = createNodeType("thaumcraft_node_additive_xray", true, false);
    private static final RenderType NODE_TRANSLUCENT_DEPTHLESS = createNodeType("thaumcraft_node_translucent_xray", false, false);

    /** Strip (texture row) per node type - verbatim TC4 mapping. */
    private static int stripFor(NodeType type) {
        return switch (type) {
            case UNSTABLE -> 6;
            case DARK -> 2;
            case HUNGRY -> 3;
            case PURE -> 4;
            case TAINTED -> 5;
            default -> 1; // NORMAL
        };
    }

    public TileNodeRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public int getViewDistance() {
        return (int) VIEW_DISTANCE;
    }

    @Override
    public void render(TileNode tile, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (tile.getNodeType() == null) {
            return;
        }

        long nt = System.nanoTime();
        float age = tile.getLevel() != null ? tile.getLevel().getGameTime() + partialTick : 0.0F;

        // TC4 renderTileEntityAt: goggles/helm (IRevealer.showNodes on the helmet
        // slot) or the held thaumometer decide whether the real node is drawn.
        // Both also disable the depth test (x-ray through blocks); the
        // thaumometer additionally requires the node to be in the view cone and
        // only reveals within 48 blocks (goggles: 64).
        float viewDistance = VIEW_DISTANCE;
        boolean reveal = false;
        boolean depthIgnore = false;
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
            if (!helmet.isEmpty() && helmet.getItem() instanceof IRevealer revealer
                    && revealer.showNodes(helmet, player)) {
                reveal = true;
                depthIgnore = true;
            } else if (isHoldingThaumometer(player)
                    && isVisibleTo(0.44F, player, Vec3.atCenterOf(tile.getBlockPos()))) {
                reveal = true;
                depthIgnore = true;
                viewDistance = 48.0F;
            }
        }

        Vec3 center = Vec3.atCenterOf(tile.getBlockPos());
        double distance = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceTo(center);
        if (distance > viewDistance) {
            return;
        }

        // Base alpha fades with distance (TC4)
        float alpha = Mth.clamp((viewDistance - (float) distance) / viewDistance, 0.05F, 1.0F);

        renderNodeBillboard(poseStack, buffer, 0.5D, 0.5D, 0.5D, tile.getAspects(), tile.getNodeType(), tile.getNodeModifier(),
                nt, age, alpha, 1.0F, reveal, depthIgnore, tile.getBlockPos().getX());
    }

    /**
     * Draw the TC4 node billboard (aspect swirls plus the type-specific core)
     * centred on the given block-relative position. Used by the world node
     * renderer and by JarNodeRenderer (nodes trapped inside a jar).
     *
     * @param reveal      true when the node is fully revealed (goggles, thaumometer or a jar)
     * @param depthIgnore true to disable the depth test (x-ray through blocks)
     * @param size        TC4 "size" multiplier (1.0 for world nodes, 0.7 inside a jar)
     * @param phaseSeed   animation phase offset (TC4 uses the block x coordinate)
     */
    public static void renderNodeBillboard(PoseStack poseStack, MultiBufferSource buffer,
                                           double cx, double cy, double cz,
                                           AspectList aspects, NodeType type, NodeModifier mod,
                                           long nt, float age, float alpha, float size,
                                           boolean reveal, boolean depthIgnore, int phaseSeed) {
        if (type == null) {
            return;
        }

        RenderType additive = depthIgnore ? NODE_ADDITIVE_DEPTHLESS : NODE_ADDITIVE;
        RenderType translucent = depthIgnore ? NODE_TRANSLUCENT_DEPTHLESS : NODE_TRANSLUCENT;

        // Animation frame, phase-shifted by the block X coordinate like TC4
        int frame = (int) (Math.abs(nt / 40000000L + phaseSeed) % FRAMES);

        poseStack.pushPose();
        poseStack.translate(cx, cy, cz);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        if (aspects != null && aspects.size() > 0 && reveal) {
            // Modifier affects opacity only (TC4)
            if (mod == NodeModifier.BRIGHT) {
                alpha *= 1.5F;
            } else if (mod == NodeModifier.PALE) {
                alpha *= 0.66F;
            } else if (mod == NodeModifier.FADING) {
                alpha *= Mth.sin(age / 3.0F) * 0.25F + 0.33F;
            }
            alpha = Mth.clamp(alpha, 0.05F, 1.0F);

            // Per-aspect colored swirls (strip 0)
            int count = 0;
            float average = 0.0F;
            for (Aspect aspect : aspects.getAspects()) {
                if (aspect == null) {
                    continue;
                }
                int amount = aspects.getAmount(aspect);
                average += amount;

                boolean altBlend = aspect.getBlend() == 771;
                float aspectAlpha = Mth.clamp(altBlend ? alpha * 1.5F : alpha, 0.0F, 1.0F);

                float scale = Mth.sin(age / Math.max(1.0F, 14.0F - count)) * 0.25F + 0.5F;
                scale = (0.2F + scale * (amount / 50.0F)) * size;

                long period = 5000L + 500L * count;
                float angle = (nt / 5000000L % period) / (float) period * (float) (Math.PI * 2.0);

                drawStrip(poseStack, buffer, altBlend ? translucent : additive,
                        0, frame, scale, angle, aspect.getColor(),
                        aspectAlpha / Math.max(1.0F, aspects.size() / 2.0F));
                count++;
            }
            average /= aspects.size();

            // Core swirl, strip selected by node type
            float coreScale = 0.1F + average / 150.0F;
            coreScale = Math.min(coreScale, 1.75F) * size;
            if (type == NodeType.HUNGRY) {
                coreScale *= 0.75F;
            }
            boolean standardBlend = type == NodeType.DARK || type == NodeType.TAINTED;
            drawStrip(poseStack, buffer, standardBlend ? translucent : additive,
                    stripFor(type), frame, coreScale, 0.0F, 0xFFFFFF, alpha);
        } else {
            // Not revealed with goggles/thaumometer (or an empty node): TC4 draws
            // the faint white outline only - no aspect colors, no modifier pulse.
            drawStrip(poseStack, buffer, additive,
                    1, frame, 0.5F * size, 0.0F, 0xFFFFFF, 0.1F);
        }

        poseStack.popPose();
    }

    /**
     * TC4 UtilsFX.isVisibleTo: the point must be inside the view cone (half fov
     * + the supplied padding, in degrees) and closer than 400 blocks. Very
     * close points are always visible.
     */
    private static boolean isVisibleTo(float fov, Player player, Vec3 pos) {
        double dist = player.getEyePosition().distanceTo(pos);
        if (dist < 2.0D) {
            return true;
        }
        Vec3 look = player.getViewVector(1.0F).normalize();
        Vec3 dir = pos.subtract(player.getEyePosition()).normalize();
        double angle = Math.toDegrees(Math.acos(Mth.clamp(look.dot(dir), -1.0D, 1.0D)));
        double fovLimit = fov + Minecraft.getInstance().options.fov().get() / 2.0D;
        return angle < fovLimit && dist < 400.0D;
    }

    private static boolean isHoldingThaumometer(Player player) {
        return player.getMainHandItem().getItem() instanceof ItemThaumometer
                || player.getOffhandItem().getItem() instanceof ItemThaumometer;
    }

    private static void drawStrip(PoseStack poseStack, MultiBufferSource buffer, RenderType renderType,
                                  int strip, int frame, float scale, float angle, int color, float alpha) {
        poseStack.pushPose();
        if (angle != 0.0F) {
            poseStack.mulPose(Axis.ZP.rotation(angle));
        }

        float u0 = (float) frame / FRAMES;
        float u1 = (frame + 1) / (float) FRAMES;
        float v0 = (float) strip / FRAMES;
        float v1 = (strip + 1) / (float) FRAMES;

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (int) (alpha * 255.0F);

        Matrix4f matrix = poseStack.last().pose();
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer vc = buffer.getBuffer(renderType);

        vc.addVertex(matrix, -scale, -scale, 0).setColor(r, g, b, a)
                .setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY)
                .setUv2(240, 240).setNormal(pose, 0, 1, 0);
        vc.addVertex(matrix, scale, -scale, 0).setColor(r, g, b, a)
                .setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY)
                .setUv2(240, 240).setNormal(pose, 0, 1, 0);
        vc.addVertex(matrix, scale, scale, 0).setColor(r, g, b, a)
                .setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY)
                .setUv2(240, 240).setNormal(pose, 0, 1, 0);
        vc.addVertex(matrix, -scale, scale, 0).setColor(r, g, b, a)
                .setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY)
                .setUv2(240, 240).setNormal(pose, 0, 1, 0);

        poseStack.popPose();
    }
}
