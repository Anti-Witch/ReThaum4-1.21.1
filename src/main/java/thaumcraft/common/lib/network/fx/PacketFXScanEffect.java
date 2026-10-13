package thaumcraft.common.lib.network.fx;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;

/**
 * PacketFXScanEffect - Network packet for scan effect visualization.
 */
public class PacketFXScanEffect implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketFXScanEffect> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "fx_scan_effect"));
    public static final StreamCodec<FriendlyByteBuf, PacketFXScanEffect> STREAM_CODEC =
            StreamCodec.of((buf, p) -> PacketFXScanEffect.encode(p, buf), PacketFXScanEffect::decode);

    @Override
    public CustomPacketPayload.Type<PacketFXScanEffect> type() {
        return TYPE;
    }

    private final int x, y, z;
    private final int color;

    public PacketFXScanEffect(int x, int y, int z, int color) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.color = color;
    }

    public PacketFXScanEffect(double x, double y, double z, double r, double g, double b) {
        this.x = (int) x;
        this.y = (int) y;
        this.z = (int) z;
        int ri = (int) (r * 255);
        int gi = (int) (g * 255);
        int bi = (int) (b * 255);
        this.color = (ri << 16) | (gi << 8) | bi;
    }

    public PacketFXScanEffect(FriendlyByteBuf buf) {
        this.x = buf.readInt();
        this.y = buf.readInt();
        this.z = buf.readInt();
        this.color = buf.readInt();
    }

    public static void encode(PacketFXScanEffect packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.x);
        buf.writeInt(packet.y);
        buf.writeInt(packet.z);
        buf.writeInt(packet.color);
    }

    public static PacketFXScanEffect decode(FriendlyByteBuf buf) {
        return new PacketFXScanEffect(buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
    }

    public static void handle(PacketFXScanEffect packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            handleClient(packet);
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PacketFXScanEffect packet) {
        // Client-side scan effect handling
    }
}
