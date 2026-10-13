package thaumcraft.common.lib.network.misc;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.common.world.aura.AuraChunk;

/**
 * PacketAuraToClient - Syncs aura data for a chunk from server to client.
 * 
 * Sent when:
 * - Player enters a new chunk
 * - Aura values change significantly
 * - Player uses a thaumometer or similar device
 * 
 * Ported from 1.12.2
 */
public class PacketAuraToClient implements CustomPacketPayload {    
    public static final CustomPacketPayload.Type<PacketAuraToClient> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "aura_to_client"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, PacketAuraToClient> STREAM_CODEC = StreamCodec.of((buf, p) -> PacketAuraToClient.encode(p, buf), PacketAuraToClient::decode);
    
    @Override
    public CustomPacketPayload.Type<PacketAuraToClient> type() {
        return TYPE;
    }

    
    private short base;
    private float vis;
    private float flux;
    
    // NOTE: no @OnlyIn client storage here on purpose. NeoForge's RuntimeDistCleaner
    // strips @OnlyIn(Dist.CLIENT) fields on the dedicated server, which made this
    // class's <clinit> fail with NoSuchFieldError ('currentAura'). Client state
    // lives in thaumcraft.client.lib.events.HudHandler instead.
    
    public PacketAuraToClient() {
    }

    public PacketAuraToClient(AuraChunk auraChunk) {
        this.base = auraChunk.getBase();
        this.vis = auraChunk.getVis();
        this.flux = auraChunk.getFlux();
    }
    
    public PacketAuraToClient(short base, float vis, float flux) {
        this.base = base;
        this.vis = vis;
        this.flux = flux;
    }
    
    public static void encode(PacketAuraToClient msg, FriendlyByteBuf buf) {
        buf.writeShort(msg.base);
        buf.writeFloat(msg.vis);
        buf.writeFloat(msg.flux);
    }
    
    public static PacketAuraToClient decode(FriendlyByteBuf buf) {
        PacketAuraToClient msg = new PacketAuraToClient();
        msg.base = buf.readShort();
        msg.vis = buf.readFloat();
        msg.flux = buf.readFloat();
        return msg;
    }
    
    public static void handle(PacketAuraToClient msg, IPayloadContext ctx) {
                ctx.enqueueWork(() -> handleOnClient(msg));
            }
    
    @OnlyIn(Dist.CLIENT)
    private static void handleOnClient(PacketAuraToClient msg) {
        // Store the current aura for HUD display (client-side class)
        thaumcraft.client.lib.events.HudHandler.updateAura(msg.base, msg.vis, msg.flux);
    }
}




