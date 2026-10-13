package thaumcraft.common.lib.network.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.common.tiles.TileThaumcraft;

/**
 * PacketTileToServer - Sends custom tile entity messages from client to server.
 *
 * Used for:
 * - GUI interactions
 * - Button presses
 * - Configuration changes
 *
 * Ported from 1.12.2
 */
public class PacketTileToServer implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketTileToServer> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "tile_to_server"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, PacketTileToServer> STREAM_CODEC = StreamCodec.of((buf, p) -> PacketTileToServer.encode(p, buf), PacketTileToServer::decode);

    @Override
    public CustomPacketPayload.Type<PacketTileToServer> type() {
        return TYPE;
    }


    private long pos;
    private CompoundTag nbt;

    public PacketTileToServer() {
    }

    public PacketTileToServer(BlockPos pos, CompoundTag nbt) {
        this.pos = pos.asLong();
        this.nbt = nbt;
    }

    public static void encode(PacketTileToServer msg, FriendlyByteBuf buf) {
        buf.writeLong(msg.pos);
        buf.writeNbt(msg.nbt);
    }

    public static PacketTileToServer decode(FriendlyByteBuf buf) {
        PacketTileToServer msg = new PacketTileToServer();
        msg.pos = buf.readLong();
        msg.nbt = buf.readNbt();
        return msg;
    }

    public static void handle(PacketTileToServer msg, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
        ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
        if (player == null) return;

        Level world = player.level();
        BlockPos blockPos = BlockPos.of(msg.pos);

        // Security check: make sure player is close enough
        if (blockPos.distSqr(player.blockPosition()) > 64) {
            return;
        }

        BlockEntity te = world.getBlockEntity(blockPos);
        if (te instanceof TileThaumcraft thaumcraftTile) {
            thaumcraftTile.messageFromClient(msg.nbt != null ? msg.nbt : new CompoundTag(), player);
        }
    });
        }
}