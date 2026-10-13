package thaumcraft.common.lib.network;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import thaumcraft.Thaumcraft;

import thaumcraft.common.lib.network.misc.PacketAuraToClient;
import thaumcraft.common.lib.network.misc.PacketBiomeChange;
import thaumcraft.common.lib.network.misc.PacketKnowledgeGain;
import thaumcraft.common.lib.network.misc.PacketLogisticsRequestToServer;
import thaumcraft.common.lib.network.misc.PacketMiscEvent;
import thaumcraft.common.lib.network.misc.PacketMiscStringToServer;
import thaumcraft.common.lib.network.misc.PacketSealFilterToClient;
import thaumcraft.common.lib.network.misc.PacketSealToClient;
import thaumcraft.common.lib.network.misc.PacketSelectThaumotoriumRecipeToServer;
import thaumcraft.common.lib.network.misc.PacketAspectPlaceToServer;
import thaumcraft.common.lib.network.misc.PacketAspectCombinationToServer;
import thaumcraft.common.lib.network.misc.PacketScanItemToServer;
import thaumcraft.common.lib.network.playerdata.PacketFocusNameToServer;
import thaumcraft.common.lib.network.playerdata.PacketFocusNodesToServer;
import thaumcraft.common.lib.network.playerdata.PacketPlayerFlagToServer;
import thaumcraft.common.lib.network.playerdata.PacketSyncKnowledge;
import thaumcraft.common.lib.network.playerdata.PacketSyncProgressToServer;
import thaumcraft.common.lib.network.playerdata.PacketSyncResearchFlagsToServer;
import thaumcraft.common.lib.network.playerdata.PacketSyncWarp;
import thaumcraft.common.lib.network.playerdata.PacketWarpMessage;
import thaumcraft.common.lib.network.fx.PacketFXBlockArc;
import thaumcraft.common.lib.network.fx.PacketFXBlockBamf;
import thaumcraft.common.lib.network.fx.PacketFXBlockMist;
import thaumcraft.common.lib.network.fx.PacketFXBoreDig;
import thaumcraft.common.lib.network.fx.PacketFXEssentiaSource;
import thaumcraft.common.lib.network.fx.PacketFXFocusEffect;
import thaumcraft.common.lib.network.fx.PacketFXFocusPartImpact;
import thaumcraft.common.lib.network.fx.PacketFXFocusPartImpactBurst;
import thaumcraft.common.lib.network.fx.PacketFXInfusionSource;
import thaumcraft.common.lib.network.fx.PacketFXPollute;
import thaumcraft.common.lib.network.fx.PacketFXScanSource;
import thaumcraft.common.lib.network.fx.PacketFXShield;
import thaumcraft.common.lib.network.fx.PacketFXSlash;
import thaumcraft.common.lib.network.fx.PacketFXSonic;
import thaumcraft.common.lib.network.fx.PacketFXWispZap;
import thaumcraft.common.lib.network.fx.PacketFXZap;
import thaumcraft.common.lib.network.misc.PacketFocusChangeToServer;
import thaumcraft.common.lib.network.misc.PacketItemKeyToServer;
import thaumcraft.common.lib.network.tiles.PacketTileToClient;
import thaumcraft.common.lib.network.tiles.PacketTileToServer;

@EventBusSubscriber(modid = Thaumcraft.MODID, bus = EventBusSubscriber.Bus.MOD)
public class PacketHandler {

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");

        // Client-bound packets (server -> client)
        registrar.playToClient(PacketSealToClient.TYPE, PacketSealToClient.STREAM_CODEC, PacketSealToClient::handle);
        registrar.playToClient(PacketSyncKnowledge.TYPE, PacketSyncKnowledge.STREAM_CODEC, PacketSyncKnowledge::handle);
        registrar.playToClient(PacketSyncWarp.TYPE, PacketSyncWarp.STREAM_CODEC, PacketSyncWarp::handle);
        registrar.playToClient(PacketWarpMessage.TYPE, PacketWarpMessage.STREAM_CODEC, PacketWarpMessage::handle);
        registrar.playToClient(PacketAuraToClient.TYPE, PacketAuraToClient.STREAM_CODEC, PacketAuraToClient::handle);
        registrar.playToClient(PacketTileToClient.TYPE, PacketTileToClient.STREAM_CODEC, PacketTileToClient::handle);
        registrar.playToClient(PacketFXSlash.TYPE, PacketFXSlash.STREAM_CODEC, PacketFXSlash::handle);
        registrar.playToClient(PacketFXBlockArc.TYPE, PacketFXBlockArc.STREAM_CODEC, PacketFXBlockArc::handle);
        registrar.playToClient(PacketFXBlockBamf.TYPE, PacketFXBlockBamf.STREAM_CODEC, PacketFXBlockBamf::handle);
        registrar.playToClient(PacketFXZap.TYPE, PacketFXZap.STREAM_CODEC, PacketFXZap::handle);
        registrar.playToClient(PacketFXEssentiaSource.TYPE, PacketFXEssentiaSource.STREAM_CODEC, PacketFXEssentiaSource::handle);
        registrar.playToClient(PacketFXShield.TYPE, PacketFXShield.STREAM_CODEC, PacketFXShield::handle);
        registrar.playToClient(PacketFXWispZap.TYPE, PacketFXWispZap.STREAM_CODEC, PacketFXWispZap::handle);
        registrar.playToClient(PacketFXFocusEffect.TYPE, PacketFXFocusEffect.STREAM_CODEC, PacketFXFocusEffect::handle);
        registrar.playToClient(PacketFXFocusPartImpact.TYPE, PacketFXFocusPartImpact.STREAM_CODEC, PacketFXFocusPartImpact::handle);
        registrar.playToClient(PacketFXFocusPartImpactBurst.TYPE, PacketFXFocusPartImpactBurst.STREAM_CODEC, PacketFXFocusPartImpactBurst::handle);
        registrar.playToClient(PacketFXInfusionSource.TYPE, PacketFXInfusionSource.STREAM_CODEC, PacketFXInfusionSource::handle);
        registrar.playToClient(PacketFXPollute.TYPE, PacketFXPollute.STREAM_CODEC, PacketFXPollute::handle);
        registrar.playToClient(PacketFXBoreDig.TYPE, PacketFXBoreDig.STREAM_CODEC, PacketFXBoreDig::handle);
        registrar.playToClient(PacketFXScanSource.TYPE, PacketFXScanSource.STREAM_CODEC, PacketFXScanSource::handle);
        registrar.playToClient(PacketFXSonic.TYPE, PacketFXSonic.STREAM_CODEC, PacketFXSonic::handle);
        registrar.playToClient(PacketFXBlockMist.TYPE, PacketFXBlockMist.STREAM_CODEC, PacketFXBlockMist::handle);
        registrar.playToClient(PacketMiscEvent.TYPE, PacketMiscEvent.STREAM_CODEC, PacketMiscEvent::handle);
        registrar.playToClient(PacketKnowledgeGain.TYPE, PacketKnowledgeGain.STREAM_CODEC, PacketKnowledgeGain::handle);
        registrar.playToClient(PacketSealFilterToClient.TYPE, PacketSealFilterToClient.STREAM_CODEC, PacketSealFilterToClient::handle);
        registrar.playToClient(PacketBiomeChange.TYPE, PacketBiomeChange.STREAM_CODEC, PacketBiomeChange::handle);

        // Server-bound packets (client -> server)
        registrar.playToServer(PacketTileToServer.TYPE, PacketTileToServer.STREAM_CODEC, PacketTileToServer::handle);
        registrar.playToServer(PacketSyncProgressToServer.TYPE, PacketSyncProgressToServer.STREAM_CODEC, PacketSyncProgressToServer::handle);
        registrar.playToServer(PacketSyncResearchFlagsToServer.TYPE, PacketSyncResearchFlagsToServer.STREAM_CODEC, PacketSyncResearchFlagsToServer::handle);
        registrar.playToServer(PacketFocusChangeToServer.TYPE, PacketFocusChangeToServer.STREAM_CODEC, PacketFocusChangeToServer::handle);
        registrar.playToServer(PacketItemKeyToServer.TYPE, PacketItemKeyToServer.STREAM_CODEC, PacketItemKeyToServer::handle);
        registrar.playToServer(PacketFocusNodesToServer.TYPE, PacketFocusNodesToServer.STREAM_CODEC, PacketFocusNodesToServer::handle);
        registrar.playToServer(PacketPlayerFlagToServer.TYPE, PacketPlayerFlagToServer.STREAM_CODEC, PacketPlayerFlagToServer::handle);
        registrar.playToServer(PacketLogisticsRequestToServer.TYPE, PacketLogisticsRequestToServer.STREAM_CODEC, PacketLogisticsRequestToServer::handle);
        registrar.playToServer(PacketMiscStringToServer.TYPE, PacketMiscStringToServer.STREAM_CODEC, PacketMiscStringToServer::handle);
        registrar.playToServer(PacketAspectPlaceToServer.TYPE, PacketAspectPlaceToServer.STREAM_CODEC, PacketAspectPlaceToServer::handle);
        registrar.playToServer(PacketAspectCombinationToServer.TYPE, PacketAspectCombinationToServer.STREAM_CODEC, PacketAspectCombinationToServer::handle);
        registrar.playToServer(PacketSelectThaumotoriumRecipeToServer.TYPE, PacketSelectThaumotoriumRecipeToServer.STREAM_CODEC, PacketSelectThaumotoriumRecipeToServer::handle);
        registrar.playToServer(PacketFocusNameToServer.TYPE, PacketFocusNameToServer.STREAM_CODEC, PacketFocusNameToServer::handle);
        registrar.playToServer(PacketScanItemToServer.TYPE, PacketScanItemToServer.STREAM_CODEC, PacketScanItemToServer::handle);
    }

    public static <MSG> void sendToPlayer(MSG packet, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, (CustomPacketPayload) packet);
    }

    public static <MSG> void sendToAll(MSG packet) {
        PacketDistributor.sendToAllPlayers((CustomPacketPayload) packet);
    }

    public static <MSG> void sendToAllTracking(MSG packet, net.minecraft.world.entity.Entity entity) {
        PacketDistributor.sendToPlayersTrackingEntity(entity, (CustomPacketPayload) packet);
    }

    public static <MSG> void sendToServer(MSG packet) {
        PacketDistributor.sendToServer((CustomPacketPayload) packet);
    }

    public static <MSG> void sendToDimension(MSG packet, ResourceKey<Level> dimension) {
        net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ServerLevel level = server.getLevel(dimension);
            if (level != null) {
                PacketDistributor.sendToPlayersInDimension(level, (CustomPacketPayload) packet);
            }
        }
    }

    public static <MSG> void sendToAllTracking(MSG packet, BlockEntity blockEntity) {
        if (blockEntity.getLevel() instanceof ServerLevel serverLevel) {
            BlockPos pos = blockEntity.getBlockPos();
            PacketDistributor.sendToPlayersTrackingChunk(serverLevel, new net.minecraft.world.level.ChunkPos(pos), (CustomPacketPayload) packet);
        }
    }

    public static <MSG> void sendToAllTrackingChunk(MSG packet, ServerLevel level, BlockPos pos) {
        PacketDistributor.sendToPlayersTrackingChunk(level, new net.minecraft.world.level.ChunkPos(pos), (CustomPacketPayload) packet);
    }
}
