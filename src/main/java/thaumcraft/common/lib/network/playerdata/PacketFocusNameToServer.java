package thaumcraft.common.lib.network.playerdata;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.common.tiles.crafting.TileFocalManipulator;

/**
 * Packet sent from client to update the name of a focus being crafted
 * in the Focal Manipulator.
 * 
 * Ported to 1.20.1
 */
public class PacketFocusNameToServer implements CustomPacketPayload {    
    public static final CustomPacketPayload.Type<PacketFocusNameToServer> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "focus_name_to_server"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, PacketFocusNameToServer> STREAM_CODEC = StreamCodec.of((buf, p) -> PacketFocusNameToServer.encode(p, buf), PacketFocusNameToServer::decode);
    
    @Override
    public CustomPacketPayload.Type<PacketFocusNameToServer> type() {
        return TYPE;
    }

    
    private final long pos;
    private final String name;
    
    public PacketFocusNameToServer(BlockPos pos, String name) {
        this.pos = pos.asLong();
        this.name = name;
    }
    
    private PacketFocusNameToServer(long pos, String name) {
        this.pos = pos;
        this.name = name;
    }
    
    public static void encode(PacketFocusNameToServer packet, FriendlyByteBuf buf) {
        buf.writeLong(packet.pos);
        buf.writeUtf(packet.name);
    }
    
    public static PacketFocusNameToServer decode(FriendlyByteBuf buf) {
        return new PacketFocusNameToServer(
            buf.readLong(),
            buf.readUtf(32767)
        );
    }
    
    public static void handle(PacketFocusNameToServer packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) return;
            
            Level level = player.level();
            BlockPos blockPos = BlockPos.of(packet.pos);
            
            BlockEntity be = level.getBlockEntity(blockPos);
            if (be instanceof TileFocalManipulator focalManipulator) {
                focalManipulator.focusName = packet.name;
                focalManipulator.setChanged();
            }
        });
    }
}




