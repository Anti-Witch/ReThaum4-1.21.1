package thaumcraft.common.lib.network.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.Thaumcraft;
import thaumcraft.api.golems.seals.ISeal;
import thaumcraft.api.golems.seals.ISealConfigArea;
import thaumcraft.api.golems.seals.ISealConfigFilter;
import thaumcraft.api.golems.seals.ISealConfigToggles;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.seals.SealPos;
import thaumcraft.common.golems.seals.SealEntity;
import thaumcraft.common.golems.seals.SealHandler;

/**
 * PacketSealToClient - Syncs seal data from server to client.
 * 
 * Used when:
 * - A seal is placed in the world
 * - A seal configuration is changed
 * - A seal is removed (type = "REMOVE")
 * 
 * Ported from 1.12.2. Key changes:
 * - IMessage/IMessageHandler pattern replaced with static encode/decode/handle methods
 * - ByteBuf -> FriendlyByteBuf with built-in ItemStack support
 * - EnumFacing -> Direction
 * - Proxy.getClientWorld() -> thaumcraft.client.lib.ClientHelper.getClientWorld()
 */
public class PacketSealToClient implements CustomPacketPayload {    
    public static final CustomPacketPayload.Type<PacketSealToClient> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "seal_to_client"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, PacketSealToClient> STREAM_CODEC = StreamCodec.of((buf, p) -> PacketSealToClient.encode(p, buf), PacketSealToClient::decode);
    
    @Override
    public CustomPacketPayload.Type<PacketSealToClient> type() {
        return TYPE;
    }

    
    private BlockPos pos;
    private Direction face;
    private String type;
    private long area;
    private boolean[] props;
    private boolean blacklist;
    private byte filterSize;
    private NonNullList<ItemStack> filter;
    private NonNullList<Integer> filterStackSize;
    private byte priority;
    private byte color;
    private boolean locked;
    private boolean redstone;
    private String owner;
    
    /**
     * Default constructor for decoding
     */
    public PacketSealToClient() {
        props = null;
    }
    
    /**
     * Create a packet from a seal entity
     * @param se The seal entity to sync
     */
    public PacketSealToClient(ISealEntity se) {
        props = null;
        pos = se.getSealPos().pos;
        face = se.getSealPos().face;
        type = (se.getSeal() == null) ? "REMOVE" : se.getSeal().getKey();
        
        // Area configuration
        if (se.getSeal() != null && se.getSeal() instanceof ISealConfigArea) {
            area = se.getArea().asLong();
        }
        
        // Toggle configuration
        if (se.getSeal() != null && se.getSeal() instanceof ISealConfigToggles configToggles) {
            ISealConfigToggles.SealToggle[] toggles = configToggles.getToggles();
            props = new boolean[toggles.length];
            for (int i = 0; i < toggles.length; i++) {
                props[i] = toggles[i].getValue();
            }
        }
        
        // Filter configuration
        if (se.getSeal() != null && se.getSeal() instanceof ISealConfigFilter configFilter) {
            blacklist = configFilter.isBlacklist();
            filterSize = (byte) configFilter.getFilterSize();
            filter = configFilter.getInv();
            filterStackSize = configFilter.getSizes();
        } else {
            filterSize = 0;
            filter = NonNullList.create();
            filterStackSize = NonNullList.create();
        }
        
        priority = se.getPriority();
        color = se.getColor();
        locked = se.isLocked();
        redstone = se.isRedstoneSensitive();
        owner = se.getOwner();
    }
    
    /**
     * Encode the packet to buffer
     */
    public static void encode(PacketSealToClient msg, FriendlyByteBuf buf) {
        buf.writeLong(msg.pos.asLong());
        buf.writeByte(msg.face.ordinal());
        buf.writeByte(msg.priority);
        buf.writeByte(msg.color);
        buf.writeBoolean(msg.locked);
        buf.writeBoolean(msg.redstone);
        buf.writeUtf(msg.owner);
        buf.writeUtf(msg.type);
        buf.writeBoolean(msg.blacklist);
        buf.writeByte(msg.filterSize);
        
        // Write filter items and sizes
        for (int i = 0; i < msg.filterSize; i++) {
            ItemStack.STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf) buf, msg.filter.get(i));
            buf.writeShort(msg.filterStackSize.get(i));
        }
        
        // Write area if present
        if (msg.area != 0L) {
            buf.writeBoolean(true);
            buf.writeLong(msg.area);
        } else {
            buf.writeBoolean(false);
        }
        
        // Write toggles if present
        if (msg.props != null && msg.props.length > 0) {
            buf.writeByte(msg.props.length);
            for (boolean prop : msg.props) {
                buf.writeBoolean(prop);
            }
        } else {
            buf.writeByte(0);
        }
    }
    
    /**
     * Decode the packet from buffer
     */
    public static PacketSealToClient decode(FriendlyByteBuf buf) {
        PacketSealToClient msg = new PacketSealToClient();
        
        msg.pos = BlockPos.of(buf.readLong());
        msg.face = Direction.values()[buf.readByte()];
        msg.priority = buf.readByte();
        msg.color = buf.readByte();
        msg.locked = buf.readBoolean();
        msg.redstone = buf.readBoolean();
        msg.owner = buf.readUtf();
        msg.type = buf.readUtf();
        msg.blacklist = buf.readBoolean();
        msg.filterSize = buf.readByte();
        
        // Read filter items and sizes
        msg.filter = NonNullList.withSize(msg.filterSize, ItemStack.EMPTY);
        msg.filterStackSize = NonNullList.withSize(msg.filterSize, 0);
        for (int i = 0; i < msg.filterSize; i++) {
            msg.filter.set(i, ItemStack.STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buf));
            msg.filterStackSize.set(i, (int) buf.readShort());
        }
        
        // Read area if present
        boolean hasArea = buf.readBoolean();
        if (hasArea) {
            msg.area = buf.readLong();
        }
        
        // Read toggles if present
        int propsLength = buf.readByte();
        if (propsLength > 0) {
            msg.props = new boolean[propsLength];
            for (int i = 0; i < propsLength; i++) {
                msg.props[i] = buf.readBoolean();
            }
        }
        
        return msg;
    }
    
    /**
     * Handle the packet on client
     */
    public static void handle(PacketSealToClient msg, IPayloadContext ctx) {
                ctx.enqueueWork(() -> {
            // Handle on client main thread
            handleOnClient(msg);
        });
            }
    
    /**
     * Process the packet data on client side
     */
    private static void handleOnClient(PacketSealToClient msg) {
        try {
            if (msg.type.equals("REMOVE")) {
                // Remove seal from client
                SealHandler.removeSealEntity(
                    thaumcraft.client.lib.ClientHelper.getClientWorld(), 
                    new SealPos(msg.pos, msg.face), 
                    true
                );
            } else {
                // Create or update seal on client
                ISeal template = SealHandler.getSeal(msg.type);
                if (template == null) {
                    Thaumcraft.LOGGER.warn("Unknown seal type in packet: {}", msg.type);
                    return;
                }
                
                // Create new seal instance
                ISeal seal = template.getClass().getDeclaredConstructor().newInstance();
                SealEntity sealEntity = new SealEntity(
                    thaumcraft.client.lib.ClientHelper.getClientWorld(), 
                    new SealPos(msg.pos, msg.face), 
                    seal
                );
                
                // Apply area configuration
                if (msg.area != 0L) {
                    sealEntity.setArea(BlockPos.of(msg.area));
                }
                
                // Apply toggle configuration
                if (msg.props != null && seal instanceof ISealConfigToggles configToggles) {
                    for (int i = 0; i < msg.props.length; i++) {
                        configToggles.setToggle(i, msg.props[i]);
                    }
                }
                
                // Apply filter configuration
                if (seal instanceof ISealConfigFilter configFilter) {
                    configFilter.setBlacklist(msg.blacklist);
                    for (int i = 0; i < msg.filterSize; i++) {
                        configFilter.setFilterSlot(i, msg.filter.get(i));
                        configFilter.setFilterSlotSize(i, msg.filterStackSize.get(i));
                    }
                }
                
                // Apply common properties
                sealEntity.setPriority(msg.priority);
                sealEntity.setColor(msg.color);
                sealEntity.setLocked(msg.locked);
                sealEntity.setRedstoneSensitive(msg.redstone);
                sealEntity.setOwner(msg.owner);
                
                // Add to handler (replaces existing if present)
                SealHandler.addSealEntity(thaumcraft.client.lib.ClientHelper.getClientWorld(), sealEntity);
            }
        } catch (Exception e) {
            Thaumcraft.LOGGER.error("Error handling seal packet at {}", msg.pos, e);
        }
    }
}




