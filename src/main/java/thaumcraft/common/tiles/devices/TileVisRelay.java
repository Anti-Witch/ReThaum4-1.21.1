package thaumcraft.common.tiles.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.common.tiles.TileThaumcraft;
import thaumcraft.init.ModBlockEntities;

import java.util.ArrayList;
import java.util.List;

/**
 * TileVisRelay - Distributes vis across a network of relays.
 * 
 * Features:
 * - Links to other relays within range
 * - Balances vis between connected relays
 * - Can be used to extend vis access from high-aura areas
 * - Shows vis beam connections between linked relays
 * 
 * Ported from 1.12.2
 */
public class TileVisRelay extends TileThaumcraft {

    public static final int LINK_RANGE = 16;
    public static final int MAX_LINKS = 8;

    // Linked relay positions
    private List<BlockPos> linkedRelays = new ArrayList<>();

    // Cached vis for transfer
    public float storedVis = 0;
    public static final float MAX_VIS = 25.0f;

    // Tick counter
    private int tickCount = 0;

    // Animation (client-side)
    public float rotation = 0;
    public float rotationPrev = 0;

    public TileVisRelay(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public TileVisRelay(BlockPos pos, BlockState state) {
        this(ModBlockEntities.VIS_RELAY.get(), pos, state);
    }

    // ==================== NBT ====================

    @Override
    protected void writeSyncNBT(CompoundTag tag) {
        super.writeSyncNBT(tag);
        tag.putFloat("StoredVis", storedVis);
        
        // Save linked positions
        int[] linkX = new int[linkedRelays.size()];
        int[] linkY = new int[linkedRelays.size()];
        int[] linkZ = new int[linkedRelays.size()];
        for (int i = 0; i < linkedRelays.size(); i++) {
            BlockPos pos = linkedRelays.get(i);
            linkX[i] = pos.getX();
            linkY[i] = pos.getY();
            linkZ[i] = pos.getZ();
        }
        tag.putIntArray("LinkX", linkX);
        tag.putIntArray("LinkY", linkY);
        tag.putIntArray("LinkZ", linkZ);
    }

    @Override
    protected void readSyncNBT(CompoundTag tag) {
        super.readSyncNBT(tag);
        storedVis = tag.getFloat("StoredVis");
        
        // Load linked positions
        linkedRelays.clear();
        int[] linkX = tag.getIntArray("LinkX");
        int[] linkY = tag.getIntArray("LinkY");
        int[] linkZ = tag.getIntArray("LinkZ");
        for (int i = 0; i < linkX.length && i < linkY.length && i < linkZ.length; i++) {
            linkedRelays.add(new BlockPos(linkX[i], linkY[i], linkZ[i]));
        }
    }

    // ==================== Tick ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileVisRelay tile) {
        tile.tickCount++;

        // Auto-link to nearby relays periodically
        if (tile.tickCount % 100 == 0) {
            tile.autoLink();
        }

        // Pull vis from aura if not full
        if (tile.tickCount % 5 == 0 && tile.storedVis < MAX_VIS) {
            float toDrain = Math.min(1.0f, MAX_VIS - tile.storedVis);
            float drained = AuraHelper.drainVis(level, pos, toDrain, false);
            if (drained > 0) {
                tile.storedVis += drained;
                tile.markDirtyAndSync();
            }
        }

        // Transfer vis to linked relays with less vis
        if (tile.tickCount % 10 == 0 && tile.storedVis > 1.0f) {
            tile.balanceVis();
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileVisRelay tile) {
        tile.rotationPrev = tile.rotation;
        
        // Rotate faster when active
        float speed = 0.5f + (tile.storedVis / MAX_VIS) * 2.0f;
        tile.rotation += speed;
        
        if (tile.rotation >= 360.0f) {
            tile.rotation -= 360.0f;
            tile.rotationPrev -= 360.0f;
        }
    }

    /**
     * Auto-link to nearby relays within range.
     */
    private void autoLink() {
        if (level == null) return;

        // Find nearby relays
        List<BlockPos> nearby = new ArrayList<>();
        for (int x = -LINK_RANGE; x <= LINK_RANGE; x++) {
            for (int y = -LINK_RANGE; y <= LINK_RANGE; y++) {
                for (int z = -LINK_RANGE; z <= LINK_RANGE; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    
                    BlockPos checkPos = worldPosition.offset(x, y, z);
                    double dist = Math.sqrt(x * x + y * y + z * z);
                    if (dist > LINK_RANGE) continue;

                    BlockEntity te = level.getBlockEntity(checkPos);
                    if (te instanceof TileVisRelay) {
                        nearby.add(checkPos);
                    }
                }
            }
        }

        // Update links (keep up to MAX_LINKS closest)
        nearby.sort((a, b) -> {
            double distA = worldPosition.distSqr(a);
            double distB = worldPosition.distSqr(b);
            return Double.compare(distA, distB);
        });

        linkedRelays.clear();
        for (int i = 0; i < Math.min(nearby.size(), MAX_LINKS); i++) {
            linkedRelays.add(nearby.get(i));
        }

        markDirtyAndSync();
    }

    /**
     * Balance vis with linked relays.
     */
    private void balanceVis() {
        if (level == null || linkedRelays.isEmpty()) return;

        for (BlockPos linkPos : linkedRelays) {
            if (storedVis <= 1.0f) break;

            BlockEntity te = level.getBlockEntity(linkPos);
            if (te instanceof TileVisRelay other) {
                // Transfer if we have more vis
                if (storedVis > other.storedVis + 1.0f) {
                    float toTransfer = Math.min(1.0f, (storedVis - other.storedVis) / 2);
                    storedVis -= toTransfer;
                    other.storedVis += toTransfer;
                    other.markDirtyAndSync();
                }
            }
        }

        markDirtyAndSync();
    }

    /**
     * Manually link to another relay.
     * 
     * @param targetPos Position of target relay
     * @return true if link was successful
     */
    public boolean linkTo(BlockPos targetPos) {
        if (level == null) return false;
        if (worldPosition.equals(targetPos)) return false;
        if (linkedRelays.contains(targetPos)) return false;
        if (linkedRelays.size() >= MAX_LINKS) return false;
        
        double dist = Math.sqrt(worldPosition.distSqr(targetPos));
        if (dist > LINK_RANGE) return false;

        BlockEntity te = level.getBlockEntity(targetPos);
        if (te instanceof TileVisRelay) {
            linkedRelays.add(targetPos);
            markDirtyAndSync();
            return true;
        }
        return false;
    }

    /**
     * Remove a link.
     * 
     * @param targetPos Position to unlink
     */
    public void unlink(BlockPos targetPos) {
        if (linkedRelays.remove(targetPos)) {
            markDirtyAndSync();
        }
    }

    /**
     * Drain vis from this relay for use by devices.
     * 
     * @param amount Amount to drain
     * @return Amount actually drained
     */
    public float drainVis(float amount) {
        float drained = Math.min(amount, storedVis);
        if (drained > 0) {
            storedVis -= drained;
            markDirtyAndSync();
        }
        return drained;
    }

    // ==================== Getters ====================

    public List<BlockPos> getLinkedRelays() {
        return new ArrayList<>(linkedRelays);
    }

    public float getStoredVis() {
        return storedVis;
    }

    public float getVisPercent() {
        return storedVis / MAX_VIS;
    }
}




