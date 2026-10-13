package thaumcraft.common.lib.network.playerdata;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchEntry;
import thaumcraft.client.gui.ResearchToast;

/**
 * PacketSyncKnowledge - Syncs all player research/knowledge data from server to client.
 * 
 * Sent when:
 * - Player logs in
 * - Research is completed
 * - Knowledge is gained
 * 
 * Ported from 1.12.2
 */
public class PacketSyncKnowledge implements CustomPacketPayload {    
    public static final CustomPacketPayload.Type<PacketSyncKnowledge> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "sync_knowledge"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, PacketSyncKnowledge> STREAM_CODEC = StreamCodec.of((buf, p) -> PacketSyncKnowledge.encode(p, buf), PacketSyncKnowledge::decode);
    
    @Override
    public CustomPacketPayload.Type<PacketSyncKnowledge> type() {
        return TYPE;
    }

    
    private CompoundTag data;
    
    public PacketSyncKnowledge() {
        this.data = new CompoundTag();
    }
    
    public PacketSyncKnowledge(Player player) {
        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
        if (knowledge != null) {
            this.data = knowledge.serializeNBT(player.level().registryAccess());
            // Clear popup flags after sending
            for (String key : knowledge.getResearchList()) {
                knowledge.clearResearchFlag(key, IPlayerKnowledge.EnumResearchFlag.POPUP);
            }
        } else {
            this.data = new CompoundTag();
        }
    }
    
    public static void encode(PacketSyncKnowledge msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.data);
    }
    
    public static PacketSyncKnowledge decode(FriendlyByteBuf buf) {
        PacketSyncKnowledge msg = new PacketSyncKnowledge();
        msg.data = buf.readNbt();
        return msg;
    }
    
    public static void handle(PacketSyncKnowledge msg, IPayloadContext ctx) {
                ctx.enqueueWork(() -> handleOnClient(msg));
            }
    
    @OnlyIn(Dist.CLIENT)
    private static void handleOnClient(PacketSyncKnowledge msg) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        
        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
        if (knowledge != null && msg.data != null) {
            knowledge.deserializeNBT(player.level().registryAccess(), msg.data);
            
            // Show popup toasts for newly unlocked research
            for (String key : knowledge.getResearchList()) {
                if (knowledge.hasResearchFlag(key, IPlayerKnowledge.EnumResearchFlag.POPUP)) {
                    ResearchEntry entry = ResearchCategories.getResearch(key);
                    if (entry != null) {
                        // Show toast notification
                        Minecraft.getInstance().getToasts().addToast(new ResearchToast(entry));
                    }
                    knowledge.clearResearchFlag(key, IPlayerKnowledge.EnumResearchFlag.POPUP);
                }
            }
        }
    }
}




