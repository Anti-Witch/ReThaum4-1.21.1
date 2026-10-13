package thaumcraft.common.lib.network.fx;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.client.fx.FXDispatcher;

/**
 * PacketFXBlockBamf - Teleport/poof visual effect at a location.
 * Used for teleportation, warp effects, and magical displacement.
 * 
 * Server -> Client
 */
public class PacketFXBlockBamf implements CustomPacketPayload {    
    public static final CustomPacketPayload.Type<PacketFXBlockBamf> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "fx_block_bamf"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, PacketFXBlockBamf> STREAM_CODEC = StreamCodec.of((buf, p) -> PacketFXBlockBamf.encode(p, buf), PacketFXBlockBamf::decode);
    
    @Override
    public CustomPacketPayload.Type<PacketFXBlockBamf> type() {
        return TYPE;
    }

    
    private final double x;
    private final double y;
    private final double z;
    private final int color;
    private final byte flags;
    private final byte face;
    
    public PacketFXBlockBamf(double x, double y, double z, int color, boolean sound, boolean flair, Direction side) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.color = color;
        int f = 0;
        if (sound) f = setBit(f, 0);
        if (flair) f = setBit(f, 1);
        this.face = (side != null) ? (byte) side.ordinal() : -1;
        this.flags = (byte) f;
    }
    
    public PacketFXBlockBamf(BlockPos pos, int color, boolean sound, boolean flair, Direction side) {
        this(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, color, sound, flair, side);
    }
    
    private PacketFXBlockBamf(double x, double y, double z, int color, byte flags, byte face) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.color = color;
        this.flags = flags;
        this.face = face;
    }
    
    public static void encode(PacketFXBlockBamf packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeInt(packet.color);
        buffer.writeByte(packet.flags);
        buffer.writeByte(packet.face);
    }
    
    public static PacketFXBlockBamf decode(FriendlyByteBuf buffer) {
        return new PacketFXBlockBamf(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readInt(),
                buffer.readByte(),
                buffer.readByte()
        );
    }
    
    public static void handle(PacketFXBlockBamf packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            handleClient(packet);
        });
    }
    
    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PacketFXBlockBamf packet) {
        Direction side = null;
        if (packet.face >= 0 && packet.face < Direction.values().length) {
            side = Direction.values()[packet.face];
        }
        
        boolean sound = getBit(packet.flags, 0);
        boolean flair = getBit(packet.flags, 1);
        
        if (packet.color != -9999) {
            FXDispatcher.INSTANCE.drawBamf(packet.x, packet.y, packet.z, packet.color, sound, flair, side);
        } else {
            FXDispatcher.INSTANCE.drawBamf(packet.x, packet.y, packet.z, sound, flair, side);
        }
    }
    
    // Bit manipulation helpers
    private static int setBit(int value, int bit) {
        return value | (1 << bit);
    }
    
    private static boolean getBit(int value, int bit) {
        return (value & (1 << bit)) != 0;
    }
}




