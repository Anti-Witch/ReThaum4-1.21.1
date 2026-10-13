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
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.init.ModSounds;

/**
 * Packet sent to client when player gains knowledge (observation, theory, etc).
 * Triggers the knowledge gain visual effect and sound on the client.
 * 
 * Ported to 1.20.1
 */
public class PacketKnowledgeGain implements CustomPacketPayload {    
    public static final CustomPacketPayload.Type<PacketKnowledgeGain> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "knowledge_gain"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, PacketKnowledgeGain> STREAM_CODEC = StreamCodec.of((buf, p) -> PacketKnowledgeGain.encode(p, buf), PacketKnowledgeGain::decode);
    
    @Override
    public CustomPacketPayload.Type<PacketKnowledgeGain> type() {
        return TYPE;
    }

    
    private final byte type;
    private final String category;
    
    public PacketKnowledgeGain(IPlayerKnowledge.EnumKnowledgeType type, String category) {
        this.type = (byte) type.ordinal();
        this.category = (category == null) ? "" : category;
    }
    
    private PacketKnowledgeGain(byte type, String category) {
        this.type = type;
        this.category = category;
    }
    
    public static void encode(PacketKnowledgeGain packet, FriendlyByteBuf buf) {
        buf.writeByte(packet.type);
        buf.writeUtf(packet.category);
    }
    
    public static PacketKnowledgeGain decode(FriendlyByteBuf buf) {
        return new PacketKnowledgeGain(
            buf.readByte(),
            buf.readUtf(32767)
        );
    }
    
    public static void handle(PacketKnowledgeGain packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> handleClient(packet));
    }
    
    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PacketKnowledgeGain packet) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;
        
        IPlayerKnowledge.EnumKnowledgeType type = IPlayerKnowledge.EnumKnowledgeType.values()[packet.type];
        ResearchCategory cat = (packet.category.length() > 0) 
            ? ResearchCategories.getResearchCategory(packet.category) 
            : null;
        
        // TODO: Add HUD handler integration when client rendering is implemented
        // RenderEventHandler.hudHandler.knowledgeGainTrackers.add(
        //     new HudHandler.KnowledgeGainTracker(type, cat, 40 + rand.nextInt(20), rand.nextLong())
        // );
        
        // Play knowledge gain sound
        if (ModSounds.LEARN.get() != null) {
            mc.level.playLocalSound(
                player.getX(), player.getY(), player.getZ(),
                ModSounds.LEARN.get(), SoundSource.AMBIENT,
                1.0f, 1.0f, false
            );
        }
    }
}




