package thaumcraft.client.events;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.client.renderers.item.ThaumometerClientExtensions;
import thaumcraft.client.renderers.item.ThaumometerItemRenderer;
import thaumcraft.client.renderers.tile.StabilizerRenderer;
import thaumcraft.init.ModItems;

/**
 * Client setup (mod event bus).
 * <p>
 * The thaumometer is rendered by {@link ThaumometerItemRenderer}, which needs
 * the OBJ scanner model to be baked even though it is no longer referenced by
 * the item model JSON (that one is now {@code builtin/entity}). Side-load it
 * here and install the client extension.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Thaumcraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetupEvents {

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(ThaumometerItemRenderer.SCANNER_MODEL);
        Thaumcraft.LOGGER.info("Registered additional model {}", ThaumometerItemRenderer.SCANNER_MODEL_ID);
        // TC4's node stabilizer pistons come from node_stabilizer.obj; the group was
        // split into its own file and is side loaded for StabilizerRenderer.
        event.register(StabilizerRenderer.PISTON_MODEL);
        event.register(StabilizerRenderer.PISTON_OVER_MODEL);
        Thaumcraft.LOGGER.info("Registered additional model {}", StabilizerRenderer.PISTON_MODEL_ID);
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(ThaumometerClientExtensions.INSTANCE, ModItems.THAUMOMETER.get());
    }
}
