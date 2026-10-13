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
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.entities.monster.EntityWisp;

import java.awt.Color;
/**
 * Packet to spawn a wisp zap lightning effect between two entities.
 * Used when wisps attack their targets.
 * 
 * The color of the zap is based on the wisp's aspect type.
 * 
 * Ported to 1.20.1
 */
public class PacketFXWispZap implements CustomPacketPayload {    
    public static final CustomPacketPayload.Type<PacketFXWispZap> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "fx_wisp_zap"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, PacketFXWispZap> STREAM_CODEC = StreamCodec.of((buf, p) -> PacketFXWispZap.encode(p, buf), PacketFXWispZap::decode);
    
    @Override
    public CustomPacketPayload.Type<PacketFXWispZap> type() {
        return TYPE;
    }

    
    private final int sourceEntityId;
    private final int targetEntityId;
    
    public PacketFXWispZap(int sourceEntityId, int targetEntityId) {
        this.sourceEntityId = sourceEntityId;
        this.targetEntityId = targetEntityId;
    }
    
    public static void encode(PacketFXWispZap packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.sourceEntityId);
        buf.writeInt(packet.targetEntityId);
    }
    
    public static PacketFXWispZap decode(FriendlyByteBuf buf) {
        return new PacketFXWispZap(buf.readInt(), buf.readInt());
    }
    
    public static void handle(PacketFXWispZap packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> handleClient(packet));
    }
    
    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PacketFXWispZap packet) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;
        
        Entity source = getEntityById(packet.sourceEntityId, mc);
        Entity target = getEntityById(packet.targetEntityId, mc);
        
        if (source == null || target == null) return;
        
        // Determine the color based on the wisp's aspect
        float r = 1.0f;
        float g = 1.0f;
        float b = 1.0f;
        
        if (source instanceof EntityWisp wisp) {
            Aspect aspect = wisp.getAspect();
            if (aspect != null) {
                Color c = new Color(aspect.getColor());
                r = c.getRed() / 255.0f;
                g = c.getGreen() / 255.0f;
                b = c.getBlue() / 255.0f;
            }
        }
        
        // Spawn arc bolt effect
        // TODO: Use FXDispatcher.arcBolt when implemented
        // FXDispatcher.INSTANCE.arcBolt(
        //     source.getX(), source.getY(), source.getZ(),
        //     target.getX(), target.getY(), target.getZ(),
        //     r, g, b, 0.6f
        // );
        
        // Placeholder: spawn electric spark particles along the path
        spawnArcParticles(level, source, target, r, g, b);
    }
    
    @OnlyIn(Dist.CLIENT)
    private static Entity getEntityById(int entityId, Minecraft mc) {
        if (mc.player != null && entityId == mc.player.getId()) {
            return mc.player;
        }
        return mc.level != null ? mc.level.getEntity(entityId) : null;
    }
    
    /**
     * Spawn arc particles between two entities as a placeholder for the full arc effect.
     */
    @OnlyIn(Dist.CLIENT)
    private static void spawnArcParticles(Level level, Entity source, Entity target, float r, float g, float b) {
        double sx = source.getX();
        double sy = source.getY() + source.getBbHeight() / 2.0;
        double sz = source.getZ();
        
        double tx = target.getX();
        double ty = target.getY() + target.getBbHeight() / 2.0;
        double tz = target.getZ();
        
        // Calculate distance and number of particles
        double dx = tx - sx;
        double dy = ty - sy;
        double dz = tz - sz;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int particles = Math.max(5, (int)(dist * 4));
        
        for (int i = 0; i <= particles; i++) {
            double t = (double) i / particles;
            double x = sx + dx * t + (level.random.nextDouble() - 0.5) * 0.2;
            double y = sy + dy * t + (level.random.nextDouble() - 0.5) * 0.2;
            double z = sz + dz * t + (level.random.nextDouble() - 0.5) * 0.2;
            
            // Use electric spark particle
            level.addParticle(
                net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                x, y, z,
                0, 0, 0
            );
        }
    }
}




