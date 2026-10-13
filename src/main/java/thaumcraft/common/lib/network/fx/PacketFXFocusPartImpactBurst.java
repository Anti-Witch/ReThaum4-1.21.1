package thaumcraft.common.lib.network.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.RandomSource;
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
 * Packet to spawn a burst of focus impact particles at a specific location.
 * Similar to PacketFXFocusPartImpact but with more spread/velocity for
 * dramatic explosion effects.
 * 
 * Used when a focus spell creates a large impact (e.g., explosion effects).
 * 
 * Ported to 1.20.1
 */
public class PacketFXFocusPartImpactBurst implements CustomPacketPayload {    
    public static final CustomPacketPayload.Type<PacketFXFocusPartImpactBurst> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "fx_focus_part_impact_burst"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, PacketFXFocusPartImpactBurst> STREAM_CODEC = StreamCodec.of((buf, p) -> PacketFXFocusPartImpactBurst.encode(p, buf), PacketFXFocusPartImpactBurst::decode);
    
    @Override
    public CustomPacketPayload.Type<PacketFXFocusPartImpactBurst> type() {
        return TYPE;
    }

    
    private final float x, y, z;
    private final String parts;
    
    public PacketFXFocusPartImpactBurst(double x, double y, double z, String[] parts) {
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
    
    private PacketFXFocusPartImpactBurst(float x, float y, float z, String parts) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.parts = parts;
    }
    
    public static void encode(PacketFXFocusPartImpactBurst packet, FriendlyByteBuf buf) {
        buf.writeFloat(packet.x);
        buf.writeFloat(packet.y);
        buf.writeFloat(packet.z);
        buf.writeUtf(packet.parts);
    }
    
    public static PacketFXFocusPartImpactBurst decode(FriendlyByteBuf buf) {
        return new PacketFXFocusPartImpactBurst(
            buf.readFloat(),
            buf.readFloat(),
            buf.readFloat(),
            buf.readUtf(32767)
        );
    }
    
    public static void handle(PacketFXFocusPartImpactBurst packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> handleClient(packet));
    }
    
    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PacketFXFocusPartImpactBurst packet) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;
        
        String[] partKeys = packet.parts.split("%");
        // More particles spread across parts
        int amount = Math.max(1, 20 / partKeys.length);
        RandomSource rand = level.random;
        
        for (String key : partKeys) {
            IFocusElement element = FocusEngine.getElement(key);
            if (element instanceof FocusEffect effect) {
                for (int i = 0; i < amount; i++) {
                    // Larger spread velocity for burst effect (0.4 vs 0.15 for normal impact)
                    double mx = rand.nextGaussian() * 0.4;
                    double my = rand.nextGaussian() * 0.4;
                    double mz = rand.nextGaussian() * 0.4;
                    
                    effect.renderParticleFX(level, packet.x, packet.y, packet.z, mx, my, mz);
                }
            }
        }
    }
}




