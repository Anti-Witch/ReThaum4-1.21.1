package thaumcraft.common.lib.network.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.tags.BlockTags;
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
import thaumcraft.common.lib.utils.Utils;

import java.awt.Color;
import java.util.ArrayList;
/**
 * PacketFXScanSource - Visual effect for ore/resource scanning.
 * Used by the Thaumometer's ore scan mode to highlight ore veins.
 * Groups adjacent ores of the same type and displays colored particles
 * at the center of each group.
 * 
 * Server -> Client
 */
public class PacketFXScanSource implements CustomPacketPayload {    
    public static final CustomPacketPayload.Type<PacketFXScanSource> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "fx_scan_source"));
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, PacketFXScanSource> STREAM_CODEC = StreamCodec.of((buf, p) -> PacketFXScanSource.encode(p, buf), PacketFXScanSource::decode);
    
    @Override
    public CustomPacketPayload.Type<PacketFXScanSource> type() {
        return TYPE;
    }

    
    // Ore type color constants
    private static final int C_QUARTZ = 15064789;    // Light pink/white
    private static final int C_IRON = 14200723;      // Tan/brown
    private static final int C_LAPIS = 1328572;      // Blue
    private static final int C_GOLD = 16576075;      // Gold/yellow
    private static final int C_DIAMOND = 6155509;    // Cyan/teal
    private static final int C_EMERALD = 1564002;    // Green
    private static final int C_REDSTONE = 16711680;  // Red
    private static final int C_COAL = 1052688;       // Dark gray
    private static final int C_SILVER = 14342653;    // Light gray
    private static final int C_TIN = 15724539;       // Light gray/white
    private static final int C_COPPER = 16620629;    // Orange/copper
    private static final int C_AMBER = 16626469;     // Amber/orange
    private static final int C_CINNABAR = 10159368;  // Red/maroon
    private static final int C_DEFAULT = 12632256;   // Gray
    
    private final long loc;
    private final int size;
    
    public PacketFXScanSource(BlockPos pos, int size) {
        this.loc = pos.asLong();
        this.size = size;
    }
    
    private PacketFXScanSource(long loc, int size) {
        this.loc = loc;
        this.size = size;
    }
    
    public static void encode(PacketFXScanSource packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.loc);
        buffer.writeByte(packet.size);
    }
    
    public static PacketFXScanSource decode(FriendlyByteBuf buffer) {
        long loc = buffer.readLong();
        int size = buffer.readByte();
        return new PacketFXScanSource(loc, size);
    }
    
    public static void handle(PacketFXScanSource packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            handleClient(packet);
        });
    }
    
    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PacketFXScanSource packet) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        
        BlockPos center = BlockPos.of(packet.loc);
        startScan(level, center, packet.size);
    }
    
    /**
     * Scan for ores in the area and create visual effects for ore groups.
     */
    @OnlyIn(Dist.CLIENT)
    private static void startScan(Level level, BlockPos pos, int r) {
        int range = 4 + r * 4;
        ArrayList<BlockPos> positions = new ArrayList<>();
        
        // Find all ore blocks in range
        for (int xx = -range; xx <= range; xx++) {
            for (int yy = -range; yy <= range; yy++) {
                for (int zz = -range; zz <= range; zz++) {
                    BlockPos p = pos.offset(xx, yy, zz);
                    if (Utils.isOreBlock(level, p)) {
                        positions.add(p);
                    }
                }
            }
        }
        
        // Group adjacent ores and create effects
        while (!positions.isEmpty()) {
            BlockPos start = positions.get(0);
            ArrayList<BlockPos> group = new ArrayList<>();
            group.add(start);
            positions.remove(0);
            
            // Find all connected ores of the same type
            calcGroup(level, start, group, positions);
            
            if (!group.isEmpty()) {
                int color = getOreColor(level, start);
                
                // Calculate center of the ore group
                double x = 0.0;
                double y = 0.0;
                double z = 0.0;
                for (BlockPos p : group) {
                    x += p.getX() + 0.5;
                    y += p.getY() + 0.5;
                    z += p.getZ() + 0.5;
                }
                x /= group.size();
                y /= group.size();
                z /= group.size();
                
                // Calculate distance for delay
                double dis = Math.sqrt(pos.distToCenterSqr(x, y, z));
                
                // Create visual effect at the group center
                // Using FXDispatcher to create a glowing indicator
                Color c = new Color(color);
                FXDispatcher.INSTANCE.drawGenericParticles(
                        x, y, z, 0, 0, 0,
                        c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f,
                        1.0f, true, 240, 15, 1,
                        44, (int)(dis * 3), 9.0f, 0f, 
                        (c.getRed() / 255f + c.getGreen() / 255f + c.getBlue() / 255f) / 3f < 0.25f ? 3 : 2
                );
            }
        }
    }
    
    /**
     * Recursively find all connected blocks of the same type.
     */
    @OnlyIn(Dist.CLIENT)
    private static void calcGroup(Level level, BlockPos start, ArrayList<BlockPos> group, ArrayList<BlockPos> positions) {
        BlockState startState = level.getBlockState(start);
        
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos neighbor = start.offset(x, y, z);
                    BlockState neighborState = level.getBlockState(neighbor);
                    
                    // Check if this is the same ore type and hasn't been grouped yet
                    if (neighborState.equals(startState) && positions.contains(neighbor)) {
                        positions.remove(neighbor);
                        group.add(neighbor);
                        
                        if (positions.isEmpty()) {
                            return;
                        }
                        
                        // Recursively add connected ores
                        calcGroup(level, neighbor, group, positions);
                    }
                }
            }
        }
    }
    
    /**
     * Get the color to use for an ore block based on its type.
     * Uses block tags to determine ore type in 1.20.1.
     */
    @OnlyIn(Dist.CLIENT)
    private static int getOreColor(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        
        if (state.isAir() || state.is(Blocks.BEDROCK)) {
            return C_DEFAULT;
        }
        
        // Check by block tags (1.20.1 way)
        if (state.is(BlockTags.IRON_ORES)) {
            return C_IRON;
        }
        if (state.is(BlockTags.COAL_ORES)) {
            return C_COAL;
        }
        if (state.is(BlockTags.REDSTONE_ORES)) {
            return C_REDSTONE;
        }
        if (state.is(BlockTags.GOLD_ORES)) {
            return C_GOLD;
        }
        if (state.is(BlockTags.LAPIS_ORES)) {
            return C_LAPIS;
        }
        if (state.is(BlockTags.DIAMOND_ORES)) {
            return C_DIAMOND;
        }
        if (state.is(BlockTags.EMERALD_ORES)) {
            return C_EMERALD;
        }
        if (state.is(BlockTags.COPPER_ORES)) {
            return C_COPPER;
        }
        
        // Check for nether quartz
        if (state.is(Blocks.NETHER_QUARTZ_ORE)) {
            return C_QUARTZ;
        }
        
        // Check registry name for modded ores
        String regName = state.getBlock().getDescriptionId().toLowerCase();
        if (regName.contains("silver")) return C_SILVER;
        if (regName.contains("tin")) return C_TIN;
        if (regName.contains("amber")) return C_AMBER;
        if (regName.contains("cinnabar")) return C_CINNABAR;
        if (regName.contains("quartz")) return C_QUARTZ;
        
        return C_DEFAULT;
    }
}




