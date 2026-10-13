package thaumcraft.common.lib.network.fx;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.client.fx.FXDispatcher;

/**
 * PacketFXZap - Electric arc/bolt visual effect between two points.
 * Used for shock effects, wand zaps, and electrical damage.
 *
 * Server -> Client
 */
public class PacketFXZap implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketFXZap> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "fx_zap"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, PacketFXZap> STREAM_CODEC = StreamCodec.of((buf, p) -> PacketFXZap.encode(p, buf), PacketFXZap::decode);

    @Override
    public CustomPacketPayload.Type<PacketFXZap> type() {
        return TYPE;
    }


    private final Vec3 source;
    private final Vec3 target;
    private final int color;
    private final float width;

    public PacketFXZap(Vec3 source, Vec3 target, int color, float width) {
        this.source = source;
        this.target = target;
        this.color = color;
        this.width = width;
    }

    public PacketFXZap(double sx, double sy, double sz, double tx, double ty, double tz, int color, float width) {
        this(new Vec3(sx, sy, sz), new Vec3(tx, ty, tz), color, width);
    }

    public static void encode(PacketFXZap packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.source.x);
        buffer.writeDouble(packet.source.y);
        buffer.writeDouble(packet.source.z);
        buffer.writeDouble(packet.target.x);
        buffer.writeDouble(packet.target.y);
        buffer.writeDouble(packet.target.z);
        buffer.writeInt(packet.color);
        buffer.writeFloat(packet.width);
    }

    public static PacketFXZap decode(FriendlyByteBuf buffer) {
        return new PacketFXZap(
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readInt(),
                buffer.readFloat()
        );
    }

    public static void handle(PacketFXZap packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            handleClient(packet);
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PacketFXZap packet) {
        // Extract RGB from color integer
        int color = packet.color;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        FXDispatcher.INSTANCE.arcBolt(
                packet.source.x, packet.source.y, packet.source.z,
                packet.target.x, packet.target.y, packet.target.z,
                r, g, b,
                packet.width);
    }
}