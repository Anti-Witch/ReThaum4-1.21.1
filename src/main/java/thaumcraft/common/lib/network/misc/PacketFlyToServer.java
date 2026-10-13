package thaumcraft.common.lib.network.misc;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import thaumcraft.Thaumcraft;
import thaumcraft.common.items.armor.Hover;

/**
 * TC4 PacketFlyToServer - the client tells the server that the Thaumostatic
 * Harness hover state changed (pressing 'H').
 */
public class PacketFlyToServer implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PacketFlyToServer> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "fly_to_server"));

    public static final StreamCodec<FriendlyByteBuf, PacketFlyToServer> STREAM_CODEC =
            StreamCodec.of(PacketFlyToServer::encode, PacketFlyToServer::decode);

    private final boolean hover;

    public PacketFlyToServer(boolean hover) {
        this.hover = hover;
    }

    @Override
    public CustomPacketPayload.Type<PacketFlyToServer> type() {
        return TYPE;
    }

    public static void encode(FriendlyByteBuf buf, PacketFlyToServer packet) {
        buf.writeBoolean(packet.hover);
    }

    public static PacketFlyToServer decode(FriendlyByteBuf buf) {
        return new PacketFlyToServer(buf.readBoolean());
    }

    public static void handle(PacketFlyToServer packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) {
                Hover.setHover(player.getUUID(), packet.hover);
            }
        });
    }
}
