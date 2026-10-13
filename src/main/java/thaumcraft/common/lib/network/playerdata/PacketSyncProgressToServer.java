package thaumcraft.common.lib.network.playerdata;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchEntry;
import thaumcraft.api.research.ResearchStage;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.common.lib.research.ResearchManager;

/**
 * PacketSyncProgressToServer - Client requests research progress from server.
 * 
 * Sent when:
 * - Player clicks on research to start it
 * - Player completes a research stage
 * 
 * Server validates requirements before progressing research.
 * 
 * Ported from 1.12.2
 */
public class PacketSyncProgressToServer implements CustomPacketPayload {    
    public static final CustomPacketPayload.Type<PacketSyncProgressToServer> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "sync_progress_to_server"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, PacketSyncProgressToServer> STREAM_CODEC = StreamCodec.of((buf, p) -> PacketSyncProgressToServer.encode(p, buf), PacketSyncProgressToServer::decode);
    
    @Override
    public CustomPacketPayload.Type<PacketSyncProgressToServer> type() {
        return TYPE;
    }

    
    private String key;
    private boolean first;      // true if starting research for first time
    private boolean checks;     // true if server should verify requirements
    private boolean noFlags;    // true to suppress popup flags
    
    public PacketSyncProgressToServer() {
    }
    
    public PacketSyncProgressToServer(String key, boolean first) {
        this(key, first, false, true);
    }
    
    public PacketSyncProgressToServer(String key, boolean first, boolean checks, boolean noFlags) {
        this.key = key;
        this.first = first;
        this.checks = checks;
        this.noFlags = noFlags;
    }
    
    public static void encode(PacketSyncProgressToServer msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.key);
        buf.writeBoolean(msg.first);
        buf.writeBoolean(msg.checks);
        buf.writeBoolean(msg.noFlags);
    }
    
    public static PacketSyncProgressToServer decode(FriendlyByteBuf buf) {
        PacketSyncProgressToServer msg = new PacketSyncProgressToServer();
        msg.key = buf.readUtf(256);
        msg.first = buf.readBoolean();
        msg.checks = buf.readBoolean();
        msg.noFlags = buf.readBoolean();
        return msg;
    }
    
    public static void handle(PacketSyncProgressToServer msg, IPayloadContext ctx) {
                ctx.enqueueWork(() -> {
            ServerPlayer player = ((net.minecraft.server.level.ServerPlayer) ctx.player());
            if (player == null) return;
            
            // Validate: check if this is a valid state change
            boolean knowsResearch = ThaumcraftCapabilities.isResearchKnown(player, msg.key);
            if (msg.first != knowsResearch) {
                // If checks are requested, verify all requirements
                if (msg.checks && !checkRequisites(player, msg.key)) {
                    return;
                }
                
                // Suppress popup flags if requested
                if (msg.noFlags) {
                    ResearchManager.noFlags = true;
                }

                // TC4 hex-grid mechanic: when a research that has hex-puzzle data
                // (aspects + complexity) is first started, hand the player a fresh
                // research note to solve on the Research Table. TC4 required scribing
                // tools with ink + paper to write the note.
                if (msg.first) {
                    ResearchEntry re = ResearchCategories.getResearch(msg.key);
                    if (re != null && re.getRequiredAspects() != null
                            && re.getRequiredAspects().size() > 0) {
                        ResearchManager.createResearchNoteForPlayer(player, msg.key);
                    }
                }

                // Progress the research
                ResearchManager.progressResearch(player, msg.key);
            }
        });
            }
    
    /**
     * Verify that the player has all requirements to progress research.
     * This includes checking for items to obtain, crafting requirements, 
     * prerequisite research, and knowledge costs.
     */
    private static boolean checkRequisites(ServerPlayer player, String key) {
        ResearchEntry research = ResearchCategories.getResearch(key);
        if (research == null || research.getStages() == null) {
            return true;
        }
        
        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
        if (knowledge == null) return false;
        
        int currentStage = knowledge.getResearchStage(key) - 1;
        if (currentStage < 0) {
            return false;
        }
        if (currentStage >= research.getStages().length) {
            return true; // Already complete
        }
        
        ResearchStage stage = research.getStages()[currentStage];
        
        // Check item requirements (obtain)
        Object[] obtain = stage.getObtain();
        if (obtain != null) {
            for (Object o : obtain) {
                ItemStack required = ItemStack.EMPTY;
                if (o instanceof ItemStack) {
                    required = (ItemStack) o;
                } else if (o instanceof String) {
                    // Tag-based requirement - simplified check
                    // In 1.20.1, ore dictionary is replaced with tags
                    // For now, skip tag checking - would need proper tag lookup
                    continue;
                }
                
                if (!required.isEmpty() && !isPlayerCarryingAmount(player, required)) {
                    return false;
                }
            }
            
            // Consume items if all checks pass
            for (Object o : obtain) {
                if (o instanceof ItemStack required) {
                    consumePlayerItem(player, required);
                }
            }
        }
        
        // Check crafting requirements
        Object[] craft = stage.getCraft();
        if (craft != null) {
            int[] craftRef = stage.getCraftReference();
            for (int i = 0; i < craft.length; i++) {
                // craftReference contains hash codes of items that need to be crafted
                String refKey = "[#]" + craftRef[i];
                if (!knowledge.isResearchKnown(refKey)) {
                    return false;
                }
            }
        }
        
        // Check research requirements
        String[] researchReqs = stage.getResearch();
        if (researchReqs != null) {
            for (String req : researchReqs) {
                if (!ThaumcraftCapabilities.isResearchComplete(player, req)) {
                    return false;
                }
            }
        }
        
        // Check and consume knowledge requirements
        ResearchStage.Knowledge[] knowReqs = stage.getKnow();
        if (knowReqs != null) {
            // First check if player has enough
            for (ResearchStage.Knowledge k : knowReqs) {
                int playerKnow = knowledge.getKnowledge(k.type, k.category != null ? k.category.key : null);
                if (playerKnow < k.amount) {
                    return false;
                }
            }
            
            // Then consume it
            for (ResearchStage.Knowledge k : knowReqs) {
                String catKey = k.category != null ? k.category.key : null;
                knowledge.addKnowledge(k.type, catKey, -k.amount * k.type.getProgression());
            }
        }
        
        return true;
    }
    
    /**
     * Check if player has the required amount of an item.
     */
    private static boolean isPlayerCarryingAmount(ServerPlayer player, ItemStack required) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (ItemStack.isSameItemSameComponents(stack, required)) {
                count += stack.getCount();
                if (count >= required.getCount()) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Consume an item from the player's inventory.
     */
    private static void consumePlayerItem(ServerPlayer player, ItemStack required) {
        int remaining = required.getCount();
        for (int i = 0; i < player.getInventory().items.size() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (ItemStack.isSameItemSameComponents(stack, required)) {
                int toRemove = Math.min(remaining, stack.getCount());
                stack.shrink(toRemove);
                remaining -= toRemove;
                if (stack.isEmpty()) {
                    player.getInventory().items.set(i, ItemStack.EMPTY);
                }
            }
        }
    }
}




