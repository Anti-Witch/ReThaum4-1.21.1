package thaumcraft.common.lib.network.playerdata;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;

/**
 * Packet sent from client to server to update player flags.
 * Currently used to reset fall distance when landing with boots of the traveller
 * or similar items that prevent fall damage.
 * 
 * Flags:
 *   1 - Reset fall distance (used by boots of the traveller, cloud ring, etc.)
 * 
 * Ported to 1.20.1
 */
public class PacketPlayerFlagToServer implements CustomPacketPayload {    
    public static final CustomPacketPayload.Type<PacketPlayerFlagToServer> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "player_flag_to_server"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, PacketPlayerFlagToServer> STREAM_CODEC = StreamCodec.of((buf, p) -> PacketPlayerFlagToServer.encode(p, buf), PacketPlayerFlagToServer::decode);
    
    @Override
    public CustomPacketPayload.Type<PacketPlayerFlagToServer> type() {
        return TYPE;
    }

    
    // Flag constants
    public static final byte FLAG_RESET_FALL_DISTANCE = 1;
    
    private final byte flag;
    
    public PacketPlayerFlagToServer(int flag) {
        this.flag = (byte) flag;
    }
    
    private PacketPlayerFlagToServer(byte flag) {
        this.flag = flag;
    }
    
    public static void encode(PacketPlayerFlagToServer packet, FriendlyByteBuf buf) {
        buf.writeByte(packet.flag);
    }
    
    public static PacketPlayerFlagToServer decode(FriendlyByteBuf buf) {
        return new PacketPlayerFlagToServer(buf.readByte());
    }
    
    public static void handle(PacketPlayerFlagToServer packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) return;
            
            switch (packet.flag) {
                case FLAG_RESET_FALL_DISTANCE -> {
                    // Reset fall distance - used by items that negate fall damage
                    player.fallDistance = 0.0f;
                }
                // Add additional flag handlers here as needed
            }
        });
    }
}




