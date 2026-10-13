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
 * PacketAspectCombinationToServer - C2S packet telling the server that the player
 * combined two aspects on the research table's aspect-linking block. The target
 * research is then interacted with on the server side and the result is put into
 * the research pool. Ab1/ab2 mark whether the aspects came from the bonus list
 * (recalculated bonus data) or from the player's research pool.
 *
 * Ported from OpenTC4 PacketAspectCombinationToServer (TC4) - hex-grid mechanic is TC4-only.
 *
 * @author Azanor
 */
public class PacketAspectCombinationToServer implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketAspectCombinationToServer> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "aspect_combination_to_server"));
    public static final StreamCodec<FriendlyByteBuf, PacketAspectCombinationToServer> STREAM_CODEC =
            StreamCodec.of((buf, p) -> PacketAspectCombinationToServer.encode(p, buf), PacketAspectCombinationToServer::decode);

    private final long pos;
    private final String aspect1;
    private final String aspect2;
    private final boolean ab1;
    private final boolean ab2;

    public PacketAspectCombinationToServer(BlockPos pos, Aspect aspect1, Aspect aspect2, boolean ab1, boolean ab2) {
        this.pos = pos.asLong();
        this.aspect1 = aspect1 == null ? "" : aspect1.getTag();
        this.aspect2 = aspect2 == null ? "" : aspect2.getTag();
        this.ab1 = ab1;
        this.ab2 = ab2;
    }

    private PacketAspectCombinationToServer(long pos, String aspect1, String aspect2, boolean ab1, boolean ab2) {
        this.pos = pos;
        this.aspect1 = aspect1;
        this.aspect2 = aspect2;
        this.ab1 = ab1;
        this.ab2 = ab2;
    }

    public static void encode(PacketAspectCombinationToServer packet, FriendlyByteBuf buf) {
        buf.writeLong(packet.pos);
        buf.writeUtf(packet.aspect1);
        buf.writeUtf(packet.aspect2);
        buf.writeBoolean(packet.ab1);
        buf.writeBoolean(packet.ab2);
    }

    public static PacketAspectCombinationToServer decode(FriendlyByteBuf buf) {
        return new PacketAspectCombinationToServer(buf.readLong(), buf.readUtf(32767), buf.readUtf(32767), buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(PacketAspectCombinationToServer packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) return;
            BlockEntity be = player.level().getBlockEntity(BlockPos.of(packet.pos));
            if (be instanceof TileResearchTable table) {
                Aspect aspect1 = packet.aspect1.isEmpty() ? null : Aspect.getAspect(packet.aspect1);
                Aspect aspect2 = packet.aspect2.isEmpty() ? null : Aspect.getAspect(packet.aspect2);
                table.combineAspects(aspect1, aspect2, packet.ab1, packet.ab2, player);
            }
        });
    }

    @Override
    public CustomPacketPayload.Type<PacketAspectCombinationToServer> type() {
        return TYPE;
    }
}