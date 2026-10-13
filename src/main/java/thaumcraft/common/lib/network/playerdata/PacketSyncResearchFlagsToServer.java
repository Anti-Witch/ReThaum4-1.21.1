package thaumcraft.common.lib.network.playerdata;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;

/**
 * PacketSyncResearchFlagsToServer - Syncs research notification flags from client to server.
 * 
 * Sent when:
 * - Player views a research entry (clears NEW_RESEARCH flag)
 * - Player views a new page (clears NEW_PAGE flag)
 * 
 * This ensures the server knows the player has acknowledged notifications.
 * 
 * Ported from 1.12.2
 */
public class PacketSyncResearchFlagsToServer implements CustomPacketPayload {    
    public static final CustomPacketPayload.Type<PacketSyncResearchFlagsToServer> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "sync_research_flags_to_server"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, PacketSyncResearchFlagsToServer> STREAM_CODEC = StreamCodec.of((buf, p) -> PacketSyncResearchFlagsToServer.encode(p, buf), PacketSyncResearchFlagsToServer::decode);
    
    @Override
    public CustomPacketPayload.Type<PacketSyncResearchFlagsToServer> type() {
        return TYPE;
    }

    
    private String key;
    private byte flags;
    
    public PacketSyncResearchFlagsToServer() {
    }
    
    /**
     * Create a packet to sync current flag state for a research key.
     * @param player The player whose flags to send
     * @param key The research key
     */
    public PacketSyncResearchFlagsToServer(ServerPlayer player, String key) {
        this.key = key;
        
        // Pack flags into a single byte
        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
        boolean hasPage = knowledge.hasResearchFlag(key, IPlayerKnowledge.EnumResearchFlag.PAGE);
        boolean hasPopup = knowledge.hasResearchFlag(key, IPlayerKnowledge.EnumResearchFlag.POPUP);
        boolean hasResearch = knowledge.hasResearchFlag(key, IPlayerKnowledge.EnumResearchFlag.RESEARCH);
        this.flags = pack(hasPage, hasPopup, hasResearch);
    }
    
    /**
     * Create a packet with explicit flag values (for client-side use).
     * @param key The research key
     * @param hasPage Whether PAGE flag should be set
     * @param hasPopup Whether POPUP flag should be set  
     * @param hasResearch Whether RESEARCH flag should be set
     */
    public PacketSyncResearchFlagsToServer(String key, boolean hasPage, boolean hasPopup, boolean hasResearch) {
        this.key = key;
        this.flags = pack(hasPage, hasPopup, hasResearch);
    }
    
    public static void encode(PacketSyncResearchFlagsToServer msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.key);
        buf.writeByte(msg.flags);
    }
    
    public static PacketSyncResearchFlagsToServer decode(FriendlyByteBuf buf) {
        PacketSyncResearchFlagsToServer msg = new PacketSyncResearchFlagsToServer();
        msg.key = buf.readUtf(256);
        msg.flags = buf.readByte();
        return msg;
    }
    
    public static void handle(PacketSyncResearchFlagsToServer msg, IPayloadContext ctx) {
                ctx.enqueueWork(() -> {
            ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player == null) return;
            
            boolean[] unpacked = unpack(msg.flags);
            boolean hasPage = unpacked[0];
            boolean hasPopup = unpacked[1];
            boolean hasResearch = unpacked[2];
            
            IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
            // Set or clear PAGE flag
            if (hasPage) {
                knowledge.setResearchFlag(msg.key, IPlayerKnowledge.EnumResearchFlag.PAGE);
            } else {
                knowledge.clearResearchFlag(msg.key, IPlayerKnowledge.EnumResearchFlag.PAGE);
            }
            
            // Set or clear POPUP flag
            if (hasPopup) {
                knowledge.setResearchFlag(msg.key, IPlayerKnowledge.EnumResearchFlag.POPUP);
            } else {
                knowledge.clearResearchFlag(msg.key, IPlayerKnowledge.EnumResearchFlag.POPUP);
            }
            
            // Set or clear RESEARCH flag
            if (hasResearch) {
                knowledge.setResearchFlag(msg.key, IPlayerKnowledge.EnumResearchFlag.RESEARCH);
            } else {
                knowledge.clearResearchFlag(msg.key, IPlayerKnowledge.EnumResearchFlag.RESEARCH);
            }
        });
            }
    
    /**
     * Pack 3 booleans into a single byte.
     */
    private static byte pack(boolean b0, boolean b1, boolean b2) {
        byte result = 0;
        if (b0) result |= 1;
        if (b1) result |= 2;
        if (b2) result |= 4;
        return result;
    }
    
    /**
     * Unpack a byte into 3 booleans.
     */
    private static boolean[] unpack(byte packed) {
        return new boolean[] {
            (packed & 1) != 0,
            (packed & 2) != 0,
            (packed & 4) != 0
        };
    }
}




