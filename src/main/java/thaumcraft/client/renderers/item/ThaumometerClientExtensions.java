package thaumcraft.client.renderers.item;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * Client extension of the thaumometer (registered from
 * {@code RegisterClientExtensionsEvent}):
 * <ul>
 *   <li>{@link #getCustomRenderer()} installs {@link ThaumometerItemRenderer}
 *       (TC4 {@code ItemThaumometerRenderer});</li>
 *   <li>{@link #applyForgeHandTransform} replaces the vanilla first-person item
 *       transform with TC4's scanner transform.</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public final class ThaumometerClientExtensions implements IClientItemExtensions {

    public static final ThaumometerClientExtensions INSTANCE = new ThaumometerClientExtensions();

    private ThaumometerItemRenderer renderer;

    private ThaumometerClientExtensions() {
    }

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        if (this.renderer == null) {
            this.renderer = new ThaumometerItemRenderer();
        }
        return this.renderer;
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm,
                                           ItemStack itemInHand, float partialTick,
                                           float equipProcess, float swingProcess) {
        // TC4 renders the scanner itself instead of using the vanilla
        // item-in-hand transform; replicate the 1.7.10 first-person item base
        // plus TC4's transform prefix here.
        ThaumometerItemRenderer.applyHandTransform(poseStack, arm, equipProcess, swingProcess);
        return true;
    }
}
