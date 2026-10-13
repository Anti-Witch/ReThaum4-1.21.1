package thaumcraft.common.lib.network.playerdata;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.common.tiles.crafting.FocusElementNode;
import thaumcraft.common.tiles.crafting.TileFocalManipulator;

import java.util.HashMap;
/**
 * Packet sent from client to server containing focus node data for the Focal Manipulator.
 * This syncs the entire focus tree structure along with the focus name.
 * 
 * Ported to 1.20.1
 */
public class PacketFocusNodesToServer implements CustomPacketPayload {    
    public static final CustomPacketPayload.Type<PacketFocusNodesToServer> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "focus_nodes_to_server"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, PacketFocusNodesToServer> STREAM_CODEC = StreamCodec.of((buf, p) -> PacketFocusNodesToServer.encode(p, buf), PacketFocusNodesToServer::decode);
    
    @Override
    public CustomPacketPayload.Type<PacketFocusNodesToServer> type() {
        return TYPE;
    }

    
    private final long pos;
    private final HashMap<Integer, FocusElementNode> data;
    private final String name;
    
    public PacketFocusNodesToServer(BlockPos pos, HashMap<Integer, FocusElementNode> data, String name) {
        this.pos = pos.asLong();
        this.data = data;
        this.name = name;
    }
    
    private PacketFocusNodesToServer(long pos, HashMap<Integer, FocusElementNode> data, String name) {
        this.pos = pos;
        this.data = data;
        this.name = name;
    }
    
    public static void encode(PacketFocusNodesToServer packet, FriendlyByteBuf buf) {
        buf.writeLong(packet.pos);
        buf.writeByte(packet.data.size());
        
        // Serialize each node
        for (FocusElementNode node : packet.data.values()) {
            CompoundTag nodeTag = node.serialize();
            buf.writeNbt(nodeTag);
        }
        
        buf.writeUtf(packet.name);
    }
    
    public static PacketFocusNodesToServer decode(FriendlyByteBuf buf) {
        long pos = buf.readLong();
        int count = buf.readByte();
        HashMap<Integer, FocusElementNode> data = new HashMap<>();
        
        for (int i = 0; i < count; i++) {
            CompoundTag nodeTag = buf.readNbt();
            if (nodeTag != null) {
                FocusElementNode node = new FocusElementNode();
                node.deserialize(nodeTag);
                data.put(node.id, node);
            }
        }
        
        String name = buf.readUtf(32767);
        
        return new PacketFocusNodesToServer(pos, data, name);
    }
    
    public static void handle(PacketFocusNodesToServer packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) return;
            
            Level level = player.level();
            BlockPos blockPos = BlockPos.of(packet.pos);
            
            // Validate position is reasonably close to player
            if (!level.isLoaded(blockPos)) return;
            if (player.distanceToSqr(blockPos.getX(), blockPos.getY(), blockPos.getZ()) > 64.0) return;
            
            BlockEntity be = level.getBlockEntity(blockPos);
            if (be instanceof TileFocalManipulator focalManipulator) {
                focalManipulator.data.clear();
                focalManipulator.data.putAll(packet.data);
                focalManipulator.focusName = packet.name;
                focalManipulator.setChanged();
            }
        });
    }
}




