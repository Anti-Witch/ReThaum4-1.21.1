package thaumcraft.common.lib.network.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.api.golems.seals.ISealConfigFilter;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.seals.SealPos;
import thaumcraft.common.golems.seals.SealHandler;

/**
 * Packet to sync seal filter configuration to the client.
 * Contains the filter inventory and stack size limits for filtered seals.
 * 
 * Ported to 1.20.1
 */
public class PacketSealFilterToClient implements CustomPacketPayload {    
    public static final CustomPacketPayload.Type<PacketSealFilterToClient> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "seal_filter_to_client"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, PacketSealFilterToClient> STREAM_CODEC = StreamCodec.of((buf, p) -> PacketSealFilterToClient.encode(p, buf), PacketSealFilterToClient::decode);
    
    @Override
    public CustomPacketPayload.Type<PacketSealFilterToClient> type() {
        return TYPE;
    }

    
    private final BlockPos pos;
    private final Direction face;
    private final byte filterSize;
    private final NonNullList<ItemStack> filter;
    private final NonNullList<Integer> filterStackSizes;
    
    public PacketSealFilterToClient(ISealEntity sealEntity) {
        this.pos = sealEntity.getSealPos().pos;
        this.face = sealEntity.getSealPos().face;
        
        if (sealEntity.getSeal() instanceof ISealConfigFilter configFilter) {
            this.filterSize = (byte) configFilter.getFilterSize();
            this.filter = configFilter.getInv();
            this.filterStackSizes = configFilter.getSizes();
        } else {
            this.filterSize = 0;
            this.filter = NonNullList.create();
            this.filterStackSizes = NonNullList.create();
        }
    }
    
    private PacketSealFilterToClient(BlockPos pos, Direction face, byte filterSize, 
                                     NonNullList<ItemStack> filter, NonNullList<Integer> filterStackSizes) {
        this.pos = pos;
        this.face = face;
        this.filterSize = filterSize;
        this.filter = filter;
        this.filterStackSizes = filterStackSizes;
    }
    
    public static void encode(PacketSealFilterToClient packet, FriendlyByteBuf buf) {
        buf.writeLong(packet.pos.asLong());
        buf.writeByte(packet.face.ordinal());
        buf.writeByte(packet.filterSize);
        
        for (int i = 0; i < packet.filterSize; i++) {
            ItemStack.STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf) buf, packet.filter.get(i));
            buf.writeShort(packet.filterStackSizes.get(i));
        }
    }
    
    public static PacketSealFilterToClient decode(FriendlyByteBuf buf) {
        BlockPos pos = BlockPos.of(buf.readLong());
        Direction face = Direction.values()[buf.readByte()];
        byte filterSize = buf.readByte();
        
        NonNullList<ItemStack> filter = NonNullList.withSize(filterSize, ItemStack.EMPTY);
        NonNullList<Integer> filterStackSizes = NonNullList.withSize(filterSize, 0);
        
        for (int i = 0; i < filterSize; i++) {
            filter.set(i, ItemStack.STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buf));
            filterStackSizes.set(i, (int) buf.readShort());
        }
        
        return new PacketSealFilterToClient(pos, face, filterSize, filter, filterStackSizes);
    }
    
    public static void handle(PacketSealFilterToClient packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> handleClient(packet));
    }
    
    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PacketSealFilterToClient packet) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;
        
        try {
            ISealEntity seal = SealHandler.getSealEntity(
                level.dimension(), 
                new SealPos(packet.pos, packet.face)
            );
            
            if (seal != null && seal.getSeal() instanceof ISealConfigFilter configFilter) {
                for (int i = 0; i < packet.filterSize; i++) {
                    configFilter.setFilterSlot(i, packet.filter.get(i));
                    configFilter.setFilterSlotSize(i, packet.filterStackSizes.get(i));
                }
            }
        } catch (Exception e) {
            // Silently ignore errors (seal may not exist on client yet)
        }
    }
}




