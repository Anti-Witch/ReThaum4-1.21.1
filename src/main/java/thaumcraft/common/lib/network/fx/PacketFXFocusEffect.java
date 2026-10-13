package thaumcraft.common.lib.network.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.IFocusElement;

/**
 * Packet to spawn focus effect particles along a trajectory.
 * Used when a focus spell is traveling through the air.
 * 
 * Multiple effect parts can be combined (e.g., fire + air creates
 * a fiery wind effect).
 * 
 * Ported to 1.20.1
 */
public class PacketFXFocusEffect implements CustomPacketPayload {    
    public static final CustomPacketPayload.Type<PacketFXFocusEffect> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "fx_focus_effect"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, PacketFXFocusEffect> STREAM_CODEC = StreamCodec.of((buf, p) -> PacketFXFocusEffect.encode(p, buf), PacketFXFocusEffect::decode);
    
    @Override
    public CustomPacketPayload.Type<PacketFXFocusEffect> type() {
        return TYPE;
    }

    
    private final float x, y, z;
    private final float motionX, motionY, motionZ;
    private final String parts;
    
    public PacketFXFocusEffect(float x, float y, float z, float motionX, float motionY, float motionZ, String[] parts) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.motionX = motionX;
        this.motionY = motionY;
        this.motionZ = motionZ;
        
        // Join parts with % separator
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append("%");
            sb.append(parts[i]);
        }
        this.parts = sb.toString();
    }
    
    private PacketFXFocusEffect(float x, float y, float z, float motionX, float motionY, float motionZ, String parts) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.motionX = motionX;
        this.motionY = motionY;
        this.motionZ = motionZ;
        this.parts = parts;
    }
    
    public static void encode(PacketFXFocusEffect packet, FriendlyByteBuf buf) {
        buf.writeFloat(packet.x);
        buf.writeFloat(packet.y);
        buf.writeFloat(packet.z);
        buf.writeFloat(packet.motionX);
        buf.writeFloat(packet.motionY);
        buf.writeFloat(packet.motionZ);
        buf.writeUtf(packet.parts);
    }
    
    public static PacketFXFocusEffect decode(FriendlyByteBuf buf) {
        return new PacketFXFocusEffect(
            buf.readFloat(),
            buf.readFloat(),
            buf.readFloat(),
            buf.readFloat(),
            buf.readFloat(),
            buf.readFloat(),
            buf.readUtf(32767)
        );
    }
    
    public static void handle(PacketFXFocusEffect packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> handleClient(packet));
    }
    
    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PacketFXFocusEffect packet) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;
        
        String[] partKeys = packet.parts.split("%");
        int amount = Math.max(1, 10 / partKeys.length);
        
        for (String key : partKeys) {
            IFocusElement element = FocusEngine.getElement(key);
            if (element instanceof FocusEffect effect) {
                for (int i = 0; i < amount; i++) {
                    // Add some randomness to the motion
                    double mx = packet.motionX + level.random.nextGaussian() / 20.0;
                    double my = packet.motionY + level.random.nextGaussian() / 20.0;
                    double mz = packet.motionZ + level.random.nextGaussian() / 20.0;
                    
                    effect.renderParticleFX(level, packet.x, packet.y, packet.z, mx, my, mz);
                }
            }
        }
    }
}




