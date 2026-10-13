package thaumcraft.common.lib.network.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;

/**
 * Packet to spawn shield rune visual effects on an entity.
 * Used when runic shielding blocks damage.
 * 
 * Target values:
 * - >= 0: Entity ID of attacker (shield faces attacker)
 * - -1: Shield above and below (fall damage, etc.)
 * - -2: Shield below only
 * - -3: Shield above only
 * 
 * Ported to 1.20.1
 */
public class PacketFXShield implements CustomPacketPayload {    
    public static final CustomPacketPayload.Type<PacketFXShield> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "fx_shield"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, PacketFXShield> STREAM_CODEC = StreamCodec.of((buf, p) -> PacketFXShield.encode(p, buf), PacketFXShield::decode);
    
    @Override
    public CustomPacketPayload.Type<PacketFXShield> type() {
        return TYPE;
    }

    
    private final int sourceEntityId;
    private final int targetEntityId;
    
    public PacketFXShield(int sourceEntityId, int targetEntityId) {
        this.sourceEntityId = sourceEntityId;
        this.targetEntityId = targetEntityId;
    }
    
    public static void encode(PacketFXShield packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.sourceEntityId);
        buf.writeInt(packet.targetEntityId);
    }
    
    public static PacketFXShield decode(FriendlyByteBuf buf) {
        return new PacketFXShield(buf.readInt(), buf.readInt());
    }
    
    public static void handle(PacketFXShield packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> handleClient(packet));
    }
    
    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PacketFXShield packet) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;
        
        Entity source = level.getEntity(packet.sourceEntityId);
        if (source == null) return;
        
        float pitch = 0.0f;
        float yaw = 0.0f;
        
        if (packet.targetEntityId >= 0) {
            // Shield facing an attacker
            Entity target = level.getEntity(packet.targetEntityId);
            if (target != null) {
                // Calculate direction from target to source
                double dx = source.getX() - target.getX();
                AABB sourceBB = source.getBoundingBox();
                AABB targetBB = target.getBoundingBox();
                double dy = (sourceBB.minY + sourceBB.maxY) / 2.0 - (targetBB.minY + targetBB.maxY) / 2.0;
                double dz = source.getZ() - target.getZ();
                double horizontal = Math.sqrt(dx * dx + dz * dz);
                
                yaw = (float)(Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
                pitch = (float)(-(Math.atan2(dy, horizontal) * 180.0 / Math.PI));
            } else {
                pitch = 90.0f;
                yaw = 0.0f;
            }
            
            // Spawn shield rune effect facing the attacker
            spawnShieldRunes(level, source, yaw, pitch);
        } else if (packet.targetEntityId == -1) {
            // Shield above and below (fall damage protection, etc.)
            spawnShieldRunes(level, source, 0.0f, 90.0f);
            spawnShieldRunes(level, source, 0.0f, 270.0f);
        } else if (packet.targetEntityId == -2) {
            // Shield below only
            spawnShieldRunes(level, source, 0.0f, 270.0f);
        } else if (packet.targetEntityId == -3) {
            // Shield above only
            spawnShieldRunes(level, source, 0.0f, 90.0f);
        }
    }
    
    /**
     * Spawn shield rune particles at the entity's position.
     */
    @OnlyIn(Dist.CLIENT)
    private static void spawnShieldRunes(Level level, Entity entity, float yaw, float pitch) {
        // TODO: Implement FXShieldRunes particle when particle system is ported
        // For now, spawn some basic particles as a placeholder
        // FXShieldRunes fb = new FXShieldRunes(level, entity.getX(), entity.getY(), entity.getZ(), 
        //                                       entity, 8, yaw, pitch);
        // Minecraft.getInstance().particleEngine.add(fb);
        
        // Placeholder: spawn some spark particles
        double x = entity.getX();
        double y = entity.getY() + entity.getBbHeight() / 2.0;
        double z = entity.getZ();
        
        // Calculate offset based on yaw and pitch
        float yawRad = yaw * Mth.DEG_TO_RAD;
        float pitchRad = pitch * Mth.DEG_TO_RAD;
        
        double offsetX = -Mth.sin(yawRad) * Mth.cos(pitchRad) * 0.5;
        double offsetY = -Mth.sin(pitchRad) * 0.5;
        double offsetZ = Mth.cos(yawRad) * Mth.cos(pitchRad) * 0.5;
        
        for (int i = 0; i < 8; i++) {
            double px = x + offsetX + (level.random.nextDouble() - 0.5) * 0.5;
            double py = y + offsetY + (level.random.nextDouble() - 0.5) * 0.5;
            double pz = z + offsetZ + (level.random.nextDouble() - 0.5) * 0.5;
            
            // Spawn enchantment glyph particles as placeholder
            level.addParticle(
                net.minecraft.core.particles.ParticleTypes.ENCHANT,
                px, py, pz,
                0, 0.1, 0
            );
        }
    }
}




