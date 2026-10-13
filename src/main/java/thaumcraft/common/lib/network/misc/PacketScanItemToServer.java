package thaumcraft.common.lib.network.misc;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import thaumcraft.Thaumcraft;
import thaumcraft.api.research.ScanningManager;
import thaumcraft.common.items.tools.ItemThaumometer;

/**
 * PacketScanItemToServer - client -> server request to scan an item shown in a
 * container GUI (the "compact inventory scanning" mode of the thaumometer).
 * <p>
 * TC4 did not have inventory scanning; this is a port feature requested by the
 * project owner. The server validates that the player actually holds a
 * thaumometer before applying the scan.
 */
public class PacketScanItemToServer implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PacketScanItemToServer> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "scan_item_to_server"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketScanItemToServer> STREAM_CODEC =
            StreamCodec.composite(ItemStack.STREAM_CODEC, PacketScanItemToServer::stack, PacketScanItemToServer::new);

    private final ItemStack stack;

    public PacketScanItemToServer(ItemStack stack) {
        this.stack = stack;
    }

    public ItemStack stack() {
        return this.stack;
    }

    @Override
    public CustomPacketPayload.Type<PacketScanItemToServer> type() {
        return TYPE;
    }

    public static void handle(PacketScanItemToServer packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) {
                return;
            }
            ItemStack stack = packet.stack();
            if (stack == null || stack.isEmpty()) {
                return;
            }
            // Only players holding the thaumometer may scan.
            boolean holding = player.getMainHandItem().getItem() instanceof ItemThaumometer
                    || player.getOffhandItem().getItem() instanceof ItemThaumometer;
            if (!holding) {
                return;
            }
            // Scan a single-item copy: the object aspects depend on the item type only.
            ItemStack target = stack.copyWithCount(1);
            ScanningManager.scanTheThing(player, target);
        });
    }
}
