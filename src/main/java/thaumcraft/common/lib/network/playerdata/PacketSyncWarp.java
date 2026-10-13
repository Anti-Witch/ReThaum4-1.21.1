package thaumcraft.common.lib.network.playerdata;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;

/**
 * PacketSyncWarp - Syncs all player warp data from server to client.
 *
 * Sent when:
 * - Player logs in
 * - Warp is gained or lost
 *
 * Ported from 1.12.2
 */
public class PacketSyncWarp implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketSyncWarp> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "sync_warp"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, PacketSyncWarp> STREAM_CODEC = StreamCodec.of((buf, p) -> PacketSyncWarp.encode(p, buf), PacketSyncWarp::decode);

    @Override
    public CustomPacketPayload.Type<PacketSyncWarp> type() {
        return TYPE;
    }


    private CompoundTag data;

    public PacketSyncWarp() {
        this.data = new CompoundTag();
    }

    public PacketSyncWarp(Player player) {
        IPlayerWarp warp = ThaumcraftCapabilities.getWarp(player);
        if (warp != null) {
            this.data = warp.serializeNBT(player.level().registryAccess());
        } else {
            this.data = new CompoundTag();
        }
    }

    public static void encode(PacketSyncWarp msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.data);
    }

    public static PacketSyncWarp decode(FriendlyByteBuf buf) {
        PacketSyncWarp msg = new PacketSyncWarp();
        msg.data = buf.readNbt();
        return msg;
    }

    public static void handle(PacketSyncWarp msg, IPayloadContext ctx) {
            ctx.enqueueWork(() -> handleOnClient(msg));
        }

    @OnlyIn(Dist.CLIENT)
    private static void handleOnClient(PacketSyncWarp msg) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        IPlayerWarp warp = ThaumcraftCapabilities.getWarp(player);
        if (warp != null && msg.data != null) {
            warp.deserializeNBT(player.level().registryAccess(), msg.data);
        }
    }
}