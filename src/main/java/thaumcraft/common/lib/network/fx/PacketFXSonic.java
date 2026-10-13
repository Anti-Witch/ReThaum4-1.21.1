package thaumcraft.common.lib.network.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.client.fx.FXDispatcher;

/**
 * PacketFXSonic - Sonic boom visual effect.
 * Used by the sonic focus and other sonic-based effects.
 * Creates an expanding ring particle effect around the source entity.
 * 
 * Server -> Client
 */
public class PacketFXSonic implements CustomPacketPayload {    
    public static final CustomPacketPayload.Type<PacketFXSonic> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "fx_sonic"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, PacketFXSonic> STREAM_CODEC = StreamCodec.of((buf, p) -> PacketFXSonic.encode(p, buf), PacketFXSonic::decode);
    
    @Override
    public CustomPacketPayload.Type<PacketFXSonic> type() {
        return TYPE;
    }

    
    private final int sourceId;
    
    public PacketFXSonic(Entity source) {
        this.sourceId = source.getId();
    }
    
    public PacketFXSonic(int sourceId) {
        this.sourceId = sourceId;
    }
    
    public static void encode(PacketFXSonic packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.sourceId);
    }
    
    public static PacketFXSonic decode(FriendlyByteBuf buffer) {
        int sourceId = buffer.readInt();
        return new PacketFXSonic(sourceId);
    }
    
    public static void handle(PacketFXSonic packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            handleClient(packet);
        });
    }
    
    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PacketFXSonic packet) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        
        Entity source = level.getEntity(packet.sourceId);
        if (source != null) {
            // Create sonic boom effect at entity position
            FXDispatcher.INSTANCE.sonicBoom(source.getX(), source.getY(), source.getZ(), source, 10);
        }
    }
}




