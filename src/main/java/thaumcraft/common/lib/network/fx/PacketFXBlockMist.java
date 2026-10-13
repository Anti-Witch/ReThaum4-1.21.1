package thaumcraft.common.lib.network.fx;

import net.minecraft.core.BlockPos;
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
 * PacketFXBlockMist - Misty/foggy particle effect at a block.
 * Used for various mystical effects on blocks like infusion pedestals,
 * flux goo, or magical fog.
 * 
 * Server -> Client
 */
public class PacketFXBlockMist implements CustomPacketPayload {    
    public static final CustomPacketPayload.Type<PacketFXBlockMist> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "fx_block_mist"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, PacketFXBlockMist> STREAM_CODEC = StreamCodec.of((buf, p) -> PacketFXBlockMist.encode(p, buf), PacketFXBlockMist::decode);
    
    @Override
    public CustomPacketPayload.Type<PacketFXBlockMist> type() {
        return TYPE;
    }

    
    private final long loc;
    private final int color;
    
    public PacketFXBlockMist(BlockPos pos, int color) {
        this.loc = pos.asLong();
        this.color = color;
    }
    
    private PacketFXBlockMist(long loc, int color) {
        this.loc = loc;
        this.color = color;
    }
    
    public static void encode(PacketFXBlockMist packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.loc);
        buffer.writeInt(packet.color);
    }
    
    public static PacketFXBlockMist decode(FriendlyByteBuf buffer) {
        long loc = buffer.readLong();
        int color = buffer.readInt();
        return new PacketFXBlockMist(loc, color);
    }
    
    public static void handle(PacketFXBlockMist packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            handleClient(packet);
        });
    }
    
    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PacketFXBlockMist packet) {
        BlockPos pos = BlockPos.of(packet.loc);
        FXDispatcher.INSTANCE.drawBlockMistParticles(pos, packet.color);
    }
}




