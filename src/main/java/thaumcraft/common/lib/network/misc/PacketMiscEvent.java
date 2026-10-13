package thaumcraft.common.lib.network.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.init.ModSounds;

/**
 * Packet for miscellaneous client-side events.
 * Used for warp effects, mist/fog effects, etc.
 * 
 * Ported to 1.20.1
 */
public class PacketMiscEvent implements CustomPacketPayload {    
    public static final CustomPacketPayload.Type<PacketMiscEvent> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "misc_event"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, PacketMiscEvent> STREAM_CODEC = StreamCodec.of((buf, p) -> PacketMiscEvent.encode(p, buf), PacketMiscEvent::decode);
    
    @Override
    public CustomPacketPayload.Type<PacketMiscEvent> type() {
        return TYPE;
    }

    
    // Event type constants
    public static final byte WARP_EVENT = 0;
    public static final byte MIST_EVENT = 1;
    public static final byte MIST_EVENT_SHORT = 2;
    
    private final byte type;
    private final int value;
    
    public PacketMiscEvent(byte type) {
        this.type = type;
        this.value = 0;
    }
    
    public PacketMiscEvent(byte type, int value) {
        this.type = type;
        this.value = value;
    }
    
    public static void encode(PacketMiscEvent packet, FriendlyByteBuf buf) {
        buf.writeByte(packet.type);
        if (packet.value != 0) {
            buf.writeInt(packet.value);
        }
    }
    
    public static PacketMiscEvent decode(FriendlyByteBuf buf) {
        byte type = buf.readByte();
        int value = 0;
        if (buf.isReadable()) {
            value = buf.readInt();
        }
        return new PacketMiscEvent(type, value);
    }
    
    public static void handle(PacketMiscEvent packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> handleClient(packet));
    }
    
    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PacketMiscEvent packet) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;
        
        switch (packet.type) {
            case WARP_EVENT -> {
                // Play heartbeat sound for warp effects
                // TODO: Check ModConfig.CONFIG_GRAPHICS.nostress when config is implemented
                if (ModSounds.HEARTBEAT.get() != null) {
                    mc.level.playLocalSound(
                        player.getX(), player.getY(), player.getZ(),
                        ModSounds.HEARTBEAT.get(), SoundSource.AMBIENT,
                        1.0f, 1.0f, false
                    );
                }
            }
            case MIST_EVENT -> {
                // Long duration fog effect
                // TODO: Implement RenderEventHandler.fogFiddled when rendering is ported
                // RenderEventHandler.fogFiddled = true;
                // RenderEventHandler.fogDuration = 2400;
            }
            case MIST_EVENT_SHORT -> {
                // Short duration fog effect
                // TODO: Implement RenderEventHandler when rendering is ported
                // RenderEventHandler.fogFiddled = true;
                // if (RenderEventHandler.fogDuration < 200) {
                //     RenderEventHandler.fogDuration = 200;
                // }
            }
        }
    }
}




