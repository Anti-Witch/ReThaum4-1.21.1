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

import net.minecraft.util.RandomSource;

/**
 * Packet to spawn focus impact particles at a specific location.
 * Used when a focus spell hits a block or entity.
 * 
 * Creates a burst of particles at the impact point, spreading outward.
 * 
 * Ported to 1.20.1
 */
public class PacketFXFocusPartImpact implements CustomPacketPayload {    
    public static final CustomPacketPayload.Type<PacketFXFocusPartImpact> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "fx_focus_part_impact"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, PacketFXFocusPartImpact> STREAM_CODEC = StreamCodec.of((buf, p) -> PacketFXFocusPartImpact.encode(p, buf), PacketFXFocusPartImpact::decode);
    
    @Override
    public CustomPacketPayload.Type<PacketFXFocusPartImpact> type() {
        return TYPE;
    }

    
    private final float x, y, z;
    private final String parts;
    
    public PacketFXFocusPartImpact(double x, double y, double z, String[] parts) {
        this.x = (float) x;
        this.y = (float) y;
        this.z = (float) z;
        
        // Join parts with % separator
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append("%");
            sb.append(parts[i]);
        }
        this.parts = sb.toString();
    }
    
    private PacketFXFocusPartImpact(float x, float y, float z, String parts) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.parts = parts;
    }
    
    public static void encode(PacketFXFocusPartImpact packet, FriendlyByteBuf buf) {
        buf.writeFloat(packet.x);
        buf.writeFloat(packet.y);
        buf.writeFloat(packet.z);
        buf.writeUtf(packet.parts);
    }
    
    public static PacketFXFocusPartImpact decode(FriendlyByteBuf buf) {
        return new PacketFXFocusPartImpact(
            buf.readFloat(),
            buf.readFloat(),
            buf.readFloat(),
            buf.readUtf(32767)
        );
    }
    
    public static void handle(PacketFXFocusPartImpact packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> handleClient(packet));
    }
    
    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PacketFXFocusPartImpact packet) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;
        
        String[] partKeys = packet.parts.split("%");
        int amount = Math.max(1, 15 / partKeys.length);
        RandomSource rand = level.random;
        
        for (String key : partKeys) {
            IFocusElement element = FocusEngine.getElement(key);
            if (element instanceof FocusEffect effect) {
                for (int i = 0; i < amount; i++) {
                    // Random outward motion for impact burst
                    double mx = rand.nextGaussian() * 0.15;
                    double my = rand.nextGaussian() * 0.15;
                    double mz = rand.nextGaussian() * 0.15;
                    
                    effect.renderParticleFX(level, packet.x, packet.y, packet.z, mx, my, mz);
                }
            }
        }
    }
}




