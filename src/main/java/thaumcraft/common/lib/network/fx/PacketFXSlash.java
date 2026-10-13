package thaumcraft.common.lib.network.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.client.fx.FXDispatcher;

/**
 * PacketFXSlash - Sends slash visual effect between two entities.
 * Used by ARCING infusion enchantment to show chain lightning.
 *
 * Server -> Client
 */
public class PacketFXSlash implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketFXSlash> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "fx_slash"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, PacketFXSlash> STREAM_CODEC = StreamCodec.of((buf, p) -> PacketFXSlash.encode(p, buf), PacketFXSlash::decode);

    @Override
    public CustomPacketPayload.Type<PacketFXSlash> type() {
        return TYPE;
    }


    private final int sourceId;
    private final int targetId;

    public PacketFXSlash(int sourceId, int targetId) {
        this.sourceId = sourceId;
        this.targetId = targetId;
    }

    public PacketFXSlash(Entity source, Entity target) {
        this.sourceId = source.getId();
        this.targetId = target.getId();
    }

    public static void encode(PacketFXSlash packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.sourceId);
        buffer.writeInt(packet.targetId);
    }

    public static PacketFXSlash decode(FriendlyByteBuf buffer) {
        return new PacketFXSlash(buffer.readInt(), buffer.readInt());
    }

    public static void handle(PacketFXSlash packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            handleClient(packet);
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PacketFXSlash packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Entity source = mc.level.getEntity(packet.sourceId);
        Entity target = mc.level.getEntity(packet.targetId);

        if (source != null && target != null) {
            double sourceY = source.getBoundingBox().minY + source.getBbHeight() / 2.0f;
            double targetY = target.getBoundingBox().minY + target.getBbHeight() / 2.0f;

            FXDispatcher.INSTANCE.drawSlash(
                    source.getX(), sourceY, source.getZ(),
                    target.getX(), targetY, target.getZ(),
                    8);
        }
    }
}