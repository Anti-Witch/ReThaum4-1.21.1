package thaumcraft.client.renderers.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.common.items.tools.ItemThaumometer;
import thaumcraft.common.lib.research.ScanGeneric;

/**
 * Custom item renderer for the Thaumometer, a 1:1 port of TC4's
 * {@code thaumcraft.client.renderers.item.ItemThaumometerRenderer}.
 * <p>
 * Unlike TC4 (which used {@code IItemRenderer}) this is a
 * {@link BlockEntityWithoutLevelRenderer} installed through
 * {@code IClientItemExtensions#getCustomRenderer()}; the item model JSON uses
 * {@code parent = builtin/entity} so vanilla calls this renderer for every
 * display context.
 * <p>
 * Coordinate spaces:
 * <ul>
 *   <li>TC4 model space: the OBJ scanner lies in the XZ plane (thickness on Y),
 *       roughly x/z in [-1.4, 1.4], y in [0, 0.2]; 1 unit = 1 block after the
 *       0.4 scale baked into the 1.7.10 item transform.</li>
 *   <li>The item is rendered with a scale of 0.4 (1.7.10 first-person base) and
 *       2.0 (TC4 renderer), so 1 model unit = 0.8 blocks.</li>
 *   <li>{@code ItemRenderer} shifts the pose by (-0.5, -0.5, -0.5) before
 *       calling a custom renderer; TC4 had no such shift, so it is undone at
 *       the start of {@link #renderByItem}.</li>
 * </ul>
 * The scan result overlay is drawn exactly like TC4 did it: aspect icons with
 * their amounts on the scanner's screen (blue lens) instead of a screen HUD.
 */
@OnlyIn(Dist.CLIENT)
public class ThaumometerItemRenderer extends BlockEntityWithoutLevelRenderer {

    /** Side-loaded copy of the OBJ scanner model (registered in ClientSetupEvents). */
    public static final ResourceLocation SCANNER_MODEL_ID =
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "item/scanner_model");
    public static final ModelResourceLocation SCANNER_MODEL =
            ModelResourceLocation.standalone(SCANNER_MODEL_ID);

    /** TC4: textures/models/scanscreen.png - the translucent blue lens. */
    private static final ResourceLocation SCREEN_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/scanscreen.png");

    /** TC4: tags are drawn at scale 0.0075, the name at 0.005. */
    private static final float TAG_SCALE = 0.0075F;
    private static final float TEXT_SCALE = 0.005F;

    /** Non-first-person display transforms (values carried over from the old
     *  item model JSON so GUI/ground/entity rendering is unchanged).
     *  NOTE: {@code ItemTransform}'s JSON deserializer multiplies translations by
     *  1/16, so the raw values below are already in blocks. */
    private static final ItemTransform THIRD_PERSON = new ItemTransform(
            new Vector3f(0.0F, 45.0F, 0.0F), new Vector3f(0.0F, 2.0F / 16.0F, 0.5F / 16.0F),
            new Vector3f(0.45F, 0.45F, 0.45F));
    private static final ItemTransform GUI = new ItemTransform(
            new Vector3f(30.0F, 225.0F, 0.0F), new Vector3f(), new Vector3f(0.4F, 0.4F, 0.4F));
    private static final ItemTransform GROUND = new ItemTransform(
            new Vector3f(), new Vector3f(0.0F, 0.6F / 16.0F, 0.0F), new Vector3f(0.4F, 0.4F, 0.4F));
    private static final ItemTransform FIXED = new ItemTransform(
            new Vector3f(0.0F, 180.0F, 0.0F), new Vector3f(), new Vector3f(0.5F, 0.5F, 0.5F));
    private static final ItemTransform HEAD = new ItemTransform(
            new Vector3f(), new Vector3f(0.0F, 1.4F / 16.0F, 0.0F), new Vector3f(0.8F, 0.8F, 0.8F));

    public ThaumometerItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    // ==================== Hand transform (TC4 EQUIPPED_FIRST_PERSON prefix) ====================

    /**
     * Replicates the 1.7.10 first-person item base transform plus TC4's
     * {@code ItemThaumometerRenderer} first-person prefix (up to the point where
     * TC4 renders the two supporting arms). The remainder of TC4's transform
     * chain is applied in {@link #renderFirstPerson}.
     */
    public static void applyHandTransform(PoseStack pose, HumanoidArm arm, float equipProcess, float swingProcess) {
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;

        // 1.7.10 doItemUsedTransformations(swingProgress)
        float f = -0.4F * Mth.sin(Mth.sqrt(swingProcess) * (float) Math.PI);
        float f1 = 0.2F * Mth.sin(Mth.sqrt(swingProcess) * (float) Math.PI * 2.0F);
        float f2 = -0.2F * Mth.sin(swingProcess * (float) Math.PI);
        pose.translate(f, f1, f2);

        // 1.7.10 transformFirstPersonItem(equipProgress, swingProgress)
        pose.translate(i * 0.56F, -0.52F + equipProcess * -0.6F, -0.72F);
        pose.mulPose(Axis.YP.rotationDegrees(i * 45.0F));
        float f3 = Mth.sin(swingProcess * swingProcess * (float) Math.PI);
        float f4 = Mth.sin(Mth.sqrt(swingProcess) * (float) Math.PI);
        pose.mulPose(Axis.YP.rotationDegrees(i * f3 * -20.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(i * f4 * -20.0F));
        pose.mulPose(Axis.XP.rotationDegrees(f4 * -80.0F));
        pose.scale(0.4F, 0.4F, 0.4F);

        // TC4 ItemThaumometerRenderer, EQUIPPED_FIRST_PERSON:
        // translate(1.0, 0.75, -1.0); rotate(135, 0,-1,0);
        // translate(-0.7*0.8, 0.65*0.8 + (1-equipped)*1.5, 0.9*0.8);
        // rotate(90, 0,1,0); translate(0,0,-0.9*0.8); rotate(90, 0,1,0);
        pose.translate(1.0F, 0.75F, -1.0F);
        pose.mulPose(Axis.YP.rotationDegrees(-135.0F));
        pose.translate(-0.56F, 0.52F + equipProcess * 1.5F, 0.72F);
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        pose.translate(0.0F, 0.0F, -0.72F);
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
    }

    // ==================== Rendering ====================

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffer, int light, int overlay) {
        pose.pushPose();
        // ItemRenderer applied translate(-0.5,-0.5,-0.5); TC4's renderer worked
        // with the raw model origin, so undo it.
        pose.translate(0.5F, 0.5F, 0.5F);

        if (context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
            renderFirstPerson(pose, buffer, light, overlay);
        } else {
            renderOtherContext(pose, buffer, light, overlay, context);
        }

        pose.popPose();
    }

    private void renderFirstPerson(PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        LocalPlayer player = Minecraft.getInstance().player;

        // TC4: both arms cup the scanner. The second (fake mirrored) arm exists
        // because 1.7.10 had no offhand; if the player holds an offhand item,
        // only the right arm is drawn and vanilla renders the offhand item.
        if (player != null && !player.isInvisible()) {
            boolean offhandEmpty = player.getOffhandItem().isEmpty();
            renderArms(pose, buffer, light, player, offhandEmpty ? 2 : 1);
        }

        // TC4: rotate(90, 0,0,1); translate(0.4, -0.4, 0); scale(2)
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
        pose.translate(0.4F, -0.4F, 0.0F);
        pose.scale(2.0F, 2.0F, 2.0F);

        renderScanner(pose, buffer, light, overlay);

        // TC4: the lens quad + the scan result overlay share this sub-pose:
        // translate(0, 0.11, 0); rotate(90, 1,0,0); rotate(90, 0,0,1)
        pose.pushPose();
        pose.translate(0.0F, 0.11F, 0.0F);
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
        renderLens(pose, buffer);
        renderScanOverlay(pose, buffer);
        pose.popPose();

        pose.popPose();
    }

    /**
     * TC4 arm block (first person): scale(5) then, for each side, the mirrored
     * hand transform; TC4 rendered the right arm twice
     * ({@code RenderPlayer.renderFirstPersonArm} always used the right arm).
     *
     * @param hands how many mirrored hands to draw (1 = right only, 2 = both)
     */
    private void renderArms(PoseStack pose, MultiBufferSource buffer, int light, LocalPlayer player, int hands) {
        PlayerRenderer renderer;
        var entityRenderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
        if (!(entityRenderer instanceof PlayerRenderer playerRenderer)) {
            return;
        }
        renderer = playerRenderer;

        pose.pushPose();
        pose.scale(5.0F, 5.0F, 5.0F);
        // TC4 loop: var22 = -1 then +1; the +1 side is the right side.
        int start = hands == 2 ? 0 : 1;
        for (int step = start; step < 2; step++) {
            int side = step * 2 - 1;
            pose.pushPose();
            pose.translate(0.0F, -0.6F, 1.1F * side);
            pose.mulPose(Axis.XP.rotationDegrees(-45.0F * side));
            pose.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(59.0F));
            pose.mulPose(Axis.YP.rotationDegrees(-65.0F * side));
            renderer.renderRightHand(pose, buffer, light, player);
            pose.popPose();
        }
        pose.popPose();
    }

    private void renderOtherContext(PoseStack pose, MultiBufferSource buffer, int light, int overlay,
                                    ItemDisplayContext context) {
        // Recreate the old item model JSON display transforms. The pose already
        // had the (-0.5) shift undone in renderByItem; vanilla applied the JSON
        // transform before that shift, so re-apply the shift for the model.
        switch (context) {
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> THIRD_PERSON.apply(false, pose);
            // TC4 ItemThaumometerRenderer, ItemRenderType.INVENTORY:
            //   glScalef(0.5); glRotatef(60, 1,0,0); glRotatef(30, 0,0,-1);
            //   glRotatef(248, 0,-1,0)
            // (glRotatef order == PoseStack.mulPose order)
            case GUI -> {
                pose.scale(0.5F, 0.5F, 0.5F);
                pose.mulPose(Axis.XP.rotationDegrees(60.0F));
                pose.mulPose(Axis.ZN.rotationDegrees(30.0F));
                pose.mulPose(Axis.YN.rotationDegrees(248.0F));
            }
            case GROUND -> GROUND.apply(false, pose);
            case FIXED -> FIXED.apply(false, pose);
            case HEAD -> HEAD.apply(false, pose);
            default -> {
            }
        }
        // NOTE: no extra (-0.5) shift here. renderByItem already undid the
        // vanilla shift, so the scanner model is rendered around its own origin
        // exactly like TC4's IItemRenderer did (adding the shift moved the whole
        // model out of the item slot / off the table).

        renderScanner(pose, buffer, light, overlay);
        renderLensUnderModelTransform(pose, buffer);
    }

    /** TC4 rendered the model, then the lens quad in the same space. */
    private void renderLensUnderModelTransform(PoseStack pose, MultiBufferSource buffer) {
        pose.pushPose();
        pose.translate(0.0F, 0.11F, 0.0F);
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
        renderLens(pose, buffer);
        pose.popPose();
    }

    /**
     * Draws the side-loaded OBJ scanner model. Its texture is stitched into the
     * block atlas (see {@code assets/minecraft/atlases/blocks.json}).
     */
    private void renderScanner(PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(SCANNER_MODEL);
        if (model == null || model.isCustomRenderer()) {
            return;
        }
        RandomSource rand = RandomSource.create(42L);
        RenderType renderType = Sheets.cutoutBlockSheet();
        VertexConsumer vc = buffer.getBuffer(renderType);
        for (Direction direction : Direction.values()) {
            for (BakedQuad quad : model.getQuads(null, direction, rand, ModelData.EMPTY, renderType)) {
                vc.putBulkData(pose.last(), quad, 1.0F, 1.0F, 1.0F, 1.0F, light, overlay);
            }
        }
        for (BakedQuad quad : model.getQuads(null, null, rand, ModelData.EMPTY, renderType)) {
            vc.putBulkData(pose.last(), quad, 1.0F, 1.0F, 1.0F, 1.0F, light, overlay);
        }
    }

    /**
     * TC4: UtilsFX.renderQuadCenteredFromTexture(scanscreen.png, 2.5F, ...) -
     * the translucent blue screen inside the golden hexagonal frame.
     */
    private void renderLens(PoseStack pose, MultiBufferSource buffer) {
        pose.pushPose();
        pose.scale(2.5F, 2.5F, 2.5F);
        Matrix4f matrix = pose.last().pose();
        // The blue display is emissive like in TC4 (lightmap was forced to a
        // high value there), so use full brightness for the lens quads.
        int glow = LightTexture.FULL_BRIGHT;
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(SCREEN_TEXTURE));
        vc.addVertex(matrix, -0.5F, 0.5F, 0.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(0.0F, 1.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(glow).setNormal(0.0F, 0.0F, 1.0F);
        vc.addVertex(matrix, 0.5F, 0.5F, 0.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(1.0F, 1.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(glow).setNormal(0.0F, 0.0F, 1.0F);
        vc.addVertex(matrix, 0.5F, -0.5F, 0.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(1.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(glow).setNormal(0.0F, 0.0F, 1.0F);
        vc.addVertex(matrix, -0.5F, -0.5F, 0.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(0.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(glow).setNormal(0.0F, 0.0F, 1.0F);
        pose.popPose();
    }

    // ==================== Scan result overlay (TC4 1:1) ====================

    /**
     * Draws the live scan target info on the scanner screen, exactly like TC4:
     * aspects (icons + amounts, 5 per row, centred) are only shown once the
     * object was scanned; the object name is always shown.
     */
    private void renderScanOverlay(PoseStack pose, MultiBufferSource buffer) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        Level level = player.level();

        ItemThaumometer.ScanTarget target = ItemThaumometer.doScan(level, player);
        if (target == null) {
            return;
        }

        // TC4: only 1 block in front of the lens, still inside the lens sub-pose.
        pose.pushPose();
        pose.translate(0.0F, 0.0F, -0.01F);

        // TC4: aspects are only revealed for objects that were scanned before.
        AspectList aspects = null;
        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
        if (knowledge != null) {
            Object keyObject = target.entity != null ? target.entity : target.pos;
            String key = ScanGeneric.getScanKey(player, keyObject);
            if (key != null && knowledge.isScanned(key)) {
                aspects = target.aspects;
            }
        }

        if (aspects != null && aspects.size() > 0) {
            renderAspectTags(pose, buffer, aspects);
        }

        String text = target.name;
        if (text != null && !text.isEmpty()) {
            renderNameText(pose, buffer, text);
        }

        pose.popPose();
    }

    /**
     * TC4 ItemThaumometerRenderer aspect layout: 5 per row (rows shrink by one),
     * tagged aspect icon (16 model units) tinted with the aspect colour and the
     * amount drawn at 0.5 scale in the icon's bottom-right corner.
     */
    private void renderAspectTags(PoseStack pose, MultiBufferSource buffer, AspectList aspects) {
        Aspect[] sorted = aspects.getAspectsSorted();
        int posX = 0;
        int posY = 0;
        int remaining = sorted.length;
        int baseX = Math.min(5, remaining) * 8;

        for (Aspect aspect : sorted) {
            if (aspect != null) {
                int amount = aspects.getAmount(aspect);
                pose.pushPose();
                pose.translate((-baseX + posX * 16) * TAG_SCALE, (-8 + posY * 16) * TAG_SCALE, 0.0F);
                pose.scale(TAG_SCALE, TAG_SCALE, TAG_SCALE);
                renderTag(pose, buffer, aspect, amount);
                pose.popPose();
            }

            posX++;
            if (posX >= 5 - posY) {
                posX = 0;
                posY++;
                remaining -= 5 - posY;
                baseX = Math.min(Math.max(5 - posY, 0), Math.max(remaining, 0)) * 8;
            }
        }
    }

    /** One aspect tag: 16x16 tinted icon + amount text (TC4 UtilsFX.drawTag). */
    private void renderTag(PoseStack pose, MultiBufferSource buffer, Aspect aspect, int amount) {
        int color = aspect.getColor();
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        Matrix4f matrix = pose.last().pose();
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(aspect.getImage()));
        vc.addVertex(matrix, 0.0F, 16.0F, 0.0F).setColor(r, g, b, 1.0F).setUv(0.0F, 1.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0.0F, 0.0F, 1.0F);
        vc.addVertex(matrix, 16.0F, 16.0F, 0.0F).setColor(r, g, b, 1.0F).setUv(1.0F, 1.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0.0F, 0.0F, 1.0F);
        vc.addVertex(matrix, 16.0F, 0.0F, 0.0F).setColor(r, g, b, 1.0F).setUv(1.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0.0F, 0.0F, 1.0F);
        vc.addVertex(matrix, 0.0F, 0.0F, 0.0F).setColor(r, g, b, 1.0F).setUv(0.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0.0F, 0.0F, 1.0F);

        if (amount > 0) {
            pose.pushPose();
            pose.scale(0.5F, 0.5F, 0.5F);
            String amountStr = String.valueOf(amount);
            int width = Minecraft.getInstance().font.width(amountStr);
            Minecraft.getInstance().font.drawInBatch(amountStr, 32 - width, 32 - 9, 0xFFFFFF, false,
                    pose.last().pose(), buffer, net.minecraft.client.gui.Font.DisplayMode.NORMAL,
                    0, LightTexture.FULL_BRIGHT);
            pose.popPose();
        }
    }

    /** TC4: name is centred below the lens, scale 0.005 (shrinks for long names). */
    private void renderNameText(PoseStack pose, MultiBufferSource buffer, String text) {
        var font = Minecraft.getInstance().font;
        int width = font.width(text);
        float scale = TEXT_SCALE;
        if (width > 90) {
            scale -= 2.5E-5F * (width - 90);
        }
        if (scale <= 0.0F) {
            return;
        }
        pose.pushPose();
        pose.translate(0.0F, -0.25F, 0.0F);
        pose.scale(scale, scale, scale);
        font.drawInBatch(Component.literal(text), -width / 2.0F, 0.0F, 0xFFFFFF, false,
                pose.last().pose(), buffer, net.minecraft.client.gui.Font.DisplayMode.NORMAL,
                0, LightTexture.FULL_BRIGHT);
        pose.popPose();
    }
}
