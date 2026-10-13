package thaumcraft.common.lib.network.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.client.fx.FXDispatcher;

/**
 * PacketFXBoreDig - Visual effect for arcane bore digging blocks.
 * Creates particle streams from the bore to the block being mined,
 * with delayed effects matching the mining progress.
 * 
 * Server -> Client
 */
public class PacketFXBoreDig implements CustomPacketPayload {    
    public static final CustomPacketPayload.Type<PacketFXBoreDig> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "fx_bore_dig"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, PacketFXBoreDig> STREAM_CODEC = StreamCodec.of((buf, p) -> PacketFXBoreDig.encode(p, buf), PacketFXBoreDig::decode);
    
    @Override
    public CustomPacketPayload.Type<PacketFXBoreDig> type() {
        return TYPE;
    }

    
    private final int x;
    private final int y;
    private final int z;
    private final int boreId;
    private final int delay;
    
    public PacketFXBoreDig(BlockPos pos, Entity bore, int delay) {
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.boreId = bore.getId();
        this.delay = delay;
    }
    
    private PacketFXBoreDig(int x, int y, int z, int boreId, int delay) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.boreId = boreId;
        this.delay = delay;
    }
    
    public static void encode(PacketFXBoreDig packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.x);
        buffer.writeInt(packet.y);
        buffer.writeInt(packet.z);
        buffer.writeInt(packet.boreId);
        buffer.writeInt(packet.delay);
    }
    
    public static PacketFXBoreDig decode(FriendlyByteBuf buffer) {
        int x = buffer.readInt();
        int y = buffer.readInt();
        int z = buffer.readInt();
        int boreId = buffer.readInt();
        int delay = buffer.readInt();
        return new PacketFXBoreDig(x, y, z, boreId, delay);
    }
    
    public static void handle(PacketFXBoreDig packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            handleClient(packet);
        });
    }
    
    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PacketFXBoreDig packet) {
        try {
            Level level = Minecraft.getInstance().level;
            if (level == null) return;
            
            BlockPos pos = new BlockPos(packet.x, packet.y, packet.z);
            Entity bore = level.getEntity(packet.boreId);
            
            if (bore == null) {
                return;
            }
            
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.AIR)) {
                return;
            }
            
            // Create delayed digging effects
            // In the original, this used ServerEvents.addRunnableClient for delayed execution
            // For 1.20.1, we'll spawn the effect directly - the FXDispatcher handles the visual
            FXDispatcher.INSTANCE.boreDigFx(
                    pos.getX(), pos.getY(), pos.getZ(),
                    bore, state, 0, packet.delay
            );
        } catch (Exception ignored) {
            // Silently ignore client-side FX errors
        }
    }
}




