package thaumcraft.common.lib.network.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.tiles.crafting.TileResearchTable;

/**
 * PacketAspectPlaceToServer - C2S packet placing (or removing) an aspect on a hex
 * of the research table note's hex-grid canvas.
 *
 * Ported from OpenTC4 PacketAspectPlaceToServer (TC4) - hex-grid mechanic is TC4-only.
 *
 * @author Azanor
 */
public class PacketAspectPlaceToServer implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketAspectPlaceToServer> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "aspect_place_to_server"));
    public static final StreamCodec<FriendlyByteBuf, PacketAspectPlaceToServer> STREAM_CODEC =
            StreamCodec.of((buf, p) -> PacketAspectPlaceToServer.encode(p, buf), PacketAspectPlaceToServer::decode);

    private final long pos;
    private final byte q;
    private final byte r;
    private final String aspectTag; // "null" or empty when removing

    public PacketAspectPlaceToServer(BlockPos pos, byte q, byte r, Aspect aspect) {
        this.pos = pos.asLong();
        this.q = q;
        this.r = r;
        this.aspectTag = aspect == null ? "" : aspect.getTag();
    }

    private PacketAspectPlaceToServer(long pos, byte q, byte r, String aspectTag) {
        this.pos = pos;
        this.q = q;
        this.r = r;
        this.aspectTag = aspectTag;
    }

    public static void encode(PacketAspectPlaceToServer packet, FriendlyByteBuf buf) {
        buf.writeLong(packet.pos);
        buf.writeByte(packet.q);
        buf.writeByte(packet.r);
        buf.writeUtf(packet.aspectTag);
    }

    public static PacketAspectPlaceToServer decode(FriendlyByteBuf buf) {
        return new PacketAspectPlaceToServer(buf.readLong(), buf.readByte(), buf.readByte(), buf.readUtf(32767));
    }

    public static void handle(PacketAspectPlaceToServer packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) return;
            BlockEntity be = player.level().getBlockEntity(BlockPos.of(packet.pos));
            if (be instanceof TileResearchTable table) {
                Aspect aspect = packet.aspectTag.isEmpty() ? null : Aspect.getAspect(packet.aspectTag);
                table.placeAspect(packet.q, packet.r, aspect, player);
            }
        });
    }

    @Override
    public CustomPacketPayload.Type<PacketAspectPlaceToServer> type() {
        return TYPE;
    }
}