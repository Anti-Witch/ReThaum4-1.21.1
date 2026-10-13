package thaumcraft.common.lib.network.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.common.tiles.crafting.TileInfusionMatrix;
import thaumcraft.common.tiles.crafting.TilePedestal;

/**
 * PacketFXInfusionSource - Visual effect for infusion crafting.
 * Shows the stream of essentia/items flowing to the infusion matrix.
 * 
 * Server -> Client
 */
public class PacketFXInfusionSource implements CustomPacketPayload {    
    public static final CustomPacketPayload.Type<PacketFXInfusionSource> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "fx_infusion_source"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, PacketFXInfusionSource> STREAM_CODEC = StreamCodec.of((buf, p) -> PacketFXInfusionSource.encode(p, buf), PacketFXInfusionSource::decode);
    
    @Override
    public CustomPacketPayload.Type<PacketFXInfusionSource> type() {
        return TYPE;
    }

    
    private final long p1;
    private final long p2;
    private final int color;
    
    public PacketFXInfusionSource(BlockPos matrixPos, BlockPos sourcePos, int color) {
        this.p1 = matrixPos.asLong();
        this.p2 = sourcePos.asLong();
        this.color = color;
    }
    
    private PacketFXInfusionSource(long p1, long p2, int color) {
        this.p1 = p1;
        this.p2 = p2;
        this.color = color;
    }
    
    public static void encode(PacketFXInfusionSource packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.p1);
        buffer.writeLong(packet.p2);
        buffer.writeInt(packet.color);
    }
    
    public static PacketFXInfusionSource decode(FriendlyByteBuf buffer) {
        long p1 = buffer.readLong();
        long p2 = buffer.readLong();
        int color = buffer.readInt();
        return new PacketFXInfusionSource(p1, p2, color);
    }
    
    public static void handle(PacketFXInfusionSource packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            handleClient(packet);
        });
    }
    
    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PacketFXInfusionSource packet) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        
        BlockPos matrixPos = BlockPos.of(packet.p1);
        BlockPos sourcePos = BlockPos.of(packet.p2);
        
        String key = sourcePos.getX() + ":" + sourcePos.getY() + ":" + sourcePos.getZ() + ":" + packet.color;
        
        BlockEntity tile = level.getBlockEntity(matrixPos);
        if (tile instanceof TileInfusionMatrix matrix) {
            // Determine tick count - pedestals get longer effects
            int count = 15;
            BlockEntity sourceTile = level.getBlockEntity(sourcePos);
            if (sourceTile instanceof TilePedestal) {
                count = 60;
            }
            
            // Update or add the source FX entry
            if (matrix.sourceFX.containsKey(key)) {
                TileInfusionMatrix.SourceFX sf = matrix.sourceFX.get(key);
                sf.ticks = count;
                matrix.sourceFX.put(key, sf);
            } else {
                matrix.sourceFX.put(key, new TileInfusionMatrix.SourceFX(sourcePos, count, packet.color));
            }
        }
    }
}




