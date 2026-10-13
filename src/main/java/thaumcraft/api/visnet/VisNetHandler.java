package thaumcraft.api.visnet;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.api.internal.WorldCoordinates;
import thaumcraft.api.aspects.Aspect;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * VisNetHandler - Manages the Vis network system.
 * Handles node connections, vis transfer, and network calculations.
 *
 * @author Azanor
 * Ported to 1.21.1
 */
public class VisNetHandler {

    // Node storage per dimension
    public static final ConcurrentHashMap<Integer, ConcurrentHashMap<WorldCoordinates, WeakReference<TileVisNode>>> sources = new ConcurrentHashMap<>();

    // Cache for nearby nodes during vis draining
    private static final ConcurrentHashMap<WorldCoordinates, CopyOnWriteArrayList<WeakReference<TileVisNode>>> nearbyNodes = new ConcurrentHashMap<>();

    // Cache for pathfinding
    private static final ArrayList<WorldCoordinates> cache = new ArrayList<>();

    /**
     * Drain vis from the network at a location.
     * Finds nearby source nodes and drains vis from them.
     */
    public static int drainVis(LevelAccessor world, int x, int y, int z, Aspect aspect, int amount) {
        int drainedAmount = 0;
        WorldCoordinates drainer = new WorldCoordinates(new BlockPos(x, y, z), world);

        if (!nearbyNodes.containsKey(drainer)) {
            calculateNearbyNodes(world, x, y, z);
        }

        CopyOnWriteArrayList<WeakReference<TileVisNode>> nodes = nearbyNodes.get(drainer);
        if (nodes != null && !nodes.isEmpty()) {
            for (WeakReference<TileVisNode> noderef : nodes) {
                TileVisNode node = noderef.get();
                if (node == null) continue;

                int a = node.consumeVis(aspect, amount);
                drainedAmount += a;
                amount -= a;
                if (a > 0) {
                    // Generate vis effect particles
                    WorldCoordinates loc = node.getLocationWC();
                    generateVisEffect(world, x, y, z, loc.getX(), loc.getY(), loc.getZ(), Aspect.getPrimalAspects().indexOf(aspect));
                }
                if (amount <= 0) break;
            }
        }

        return drainedAmount;
    }

    /**
     * Generate vis effect particles between two points.
     */
    public static void generateVisEffect(LevelAccessor world, int x, int y, int z, int x2, int y2, int z2, int color) {
        // TODO: Implement particle effect packet
    }

    /**
     * Register a new vis source (node, energized node, etc).
     */
    public static void addSource(LevelAccessor world, TileVisNode vs) {
        int dim = vs.getDimension();
        ConcurrentHashMap<WorldCoordinates, WeakReference<TileVisNode>> sourcelist = sources.computeIfAbsent(dim, k -> new ConcurrentHashMap<>());

        sourcelist.put(vs.getLocationWC(), new WeakReference<>(vs));
        sources.put(dim, sourcelist);
        nearbyNodes.clear();
    }

    /**
     * Remove a source from the network.
     */
    public static void removeSource(LevelAccessor world, TileVisNode vs) {
        int dim = vs.getDimension();
        ConcurrentHashMap<WorldCoordinates, WeakReference<TileVisNode>> sourcelist = sources.get(dim);
        if (sourcelist != null) {
            sourcelist.remove(vs.getLocationWC());
            nearbyNodes.clear();
        }
    }

    /**
     * Check if a node reference is valid.
     */
    public static boolean isNodeValid(WeakReference<TileVisNode> node) {
        if (node == null) return false;
        TileVisNode visNode = node.get();
        if (visNode == null) return false;
        return !visNode.isInvalid();
    }

    /**
     * Add a new node to the network and connect it to nearby sources.
     * Returns the parent node it connected to.
     */
    public static WeakReference<TileVisNode> addNode(LevelAccessor world, TileVisNode vn) {
        WeakReference<TileVisNode> ref = new WeakReference<>(vn);
        int dim = vn.getDimension();

        ConcurrentHashMap<WorldCoordinates, WeakReference<TileVisNode>> sourcelist = sources.computeIfAbsent(dim, k -> new ConcurrentHashMap<>());
        if (!sourcelist.containsKey(vn.getLocationWC())) {
            sourcelist.put(vn.getLocationWC(), ref);
        }

        CopyOnWriteArrayList<Object[]> nearby = new CopyOnWriteArrayList<>();

        for (WeakReference<TileVisNode> root : sourcelist.values()) {
            if (!isNodeValid(root)) continue;
            TileVisNode source = root.get();
            if (source == null) continue;

            float r = inRange(world, vn.getLocationWC(), source.getLocationWC(), vn.getRange());
            if (r > 0) {
                nearby.add(new Object[] { source, r - vn.getRange() * 2 });
            }

            nearby = findClosestNodes(vn, source, nearby);
            cache.clear();
        }

        float dist = Float.MAX_VALUE;
        TileVisNode closest = null;
        if (!nearby.isEmpty()) {
            for (Object[] o : nearby) {
                if ((Float) o[1] < dist &&
                    (vn.getAttunement() == -1 || ((TileVisNode) o[0]).getAttunement() == -1 ||
                     vn.getAttunement() == ((TileVisNode) o[0]).getAttunement())
                     && canNodeBeSeen(world, vn, (TileVisNode) o[0])) {
                    dist = (Float) o[1];
                    closest = (TileVisNode) o[0];
                }
            }
        }

        if (closest != null) {
            closest.addChild(vn);
            vn.setParent(closest);
            nearbyNodes.clear();
            return new WeakReference<>(closest);
        }

        return null;
    }

    private static CopyOnWriteArrayList<Object[]> findClosestNodes(TileVisNode target, TileVisNode parent, CopyOnWriteArrayList<Object[]> in) {
        if (cache.size() > 512 || cache.contains(parent.getLocationWC())) return in;
        cache.add(parent.getLocationWC());

        for (WeakReference<TileVisNode> childWR : parent.getChildren()) {
            TileVisNode child = childWR.get();
            if (child != null && !child.equals(target) && !child.equals(parent)) {
                float r2 = inRange(child.getWorldObj(), child.getLocationWC(), target.getLocationWC(), target.getRange());
                if (r2 > 0) {
                    in.add(new Object[] { child, r2 });
                }
                in = findClosestNodes(target, child, in);
            }
        }
        return in;
    }

    private static float inRange(LevelAccessor world, WorldCoordinates cc1, WorldCoordinates cc2, int range) {
        float distance = (float) cc1.getDistanceSquaredToWorldCoordinates(cc2);
        return distance > range * range ? -1 : distance;
    }

    private static void calculateNearbyNodes(LevelAccessor world, int x, int y, int z) {
        int dim = 0;
        if (world instanceof Level level) {
            dim = level.dimension().location().toString().hashCode();
        }
        ConcurrentHashMap<WorldCoordinates, WeakReference<TileVisNode>> sourcelist = sources.get(dim);
        if (sourcelist == null) return;

        CopyOnWriteArrayList<WeakReference<TileVisNode>> cn = new CopyOnWriteArrayList<>();
        WorldCoordinates drainer = new WorldCoordinates(new BlockPos(x, y, z), world);

        CopyOnWriteArrayList<Object[]> nearby = new CopyOnWriteArrayList<>();

        for (WeakReference<TileVisNode> root : sourcelist.values()) {
            if (!isNodeValid(root)) continue;
            TileVisNode source = root.get();
            if (source == null) continue;

            TileVisNode closest = null;
            float range = Float.MAX_VALUE;

            float r = inRange(world, drainer, source.getLocationWC(), source.getRange());
            if (r > 0) {
                range = r;
                closest = source;
            }

            CopyOnWriteArrayList<WeakReference<TileVisNode>> children = new CopyOnWriteArrayList<>();
            children = getAllChildren(source, children);

            for (WeakReference<TileVisNode> child : children) {
                TileVisNode n = child.get();
                if (n != null && !n.equals(source)) {
                    float r2 = inRange(n.getWorldObj(), n.getLocationWC(), drainer, n.getRange());
                    if (r2 > 0 && r2 < range) {
                        range = r2;
                        closest = n;
                    }
                }
            }

            if (closest != null) {
                cn.add(new WeakReference<>(closest));
            }
        }

        nearbyNodes.put(drainer, cn);
    }

    private static CopyOnWriteArrayList<WeakReference<TileVisNode>> getAllChildren(TileVisNode source, CopyOnWriteArrayList<WeakReference<TileVisNode>> list) {
        for (WeakReference<TileVisNode> child : source.getChildren()) {
            TileVisNode n = child.get();
            if (n != null && n.getWorldObj() != null && isChunkLoaded(n.getWorldObj(), n.getLocationWC().getX(), n.getLocationWC().getZ())) {
                list.add(child);
                list = getAllChildren(n, list);
            }
        }
        return list;
    }

    private static boolean isChunkLoaded(LevelAccessor world, int x, int z) {
        if (world instanceof Level level) {
            return level.hasChunkAt(new BlockPos((x >> 4) << 4, 0, (z >> 4) << 4));
        }
        return false;
    }

    public static boolean isChunkLoaded(BlockEntity be, int x, int z) {
        return isChunkLoaded(be.getLevel(), x, z);
    }

    /**
     * Check if a node can be seen from another (line of sight check).
     * Uses ray tracing to check for blocking blocks.
     */
    public static boolean canNodeBeSeen(LevelAccessor world, TileVisNode source, TileVisNode target) {
        // Simplified - for now just return true
        // Full implementation would ray trace from source to target
        return true;
    }

    /**
     * Get all registered sources in a dimension.
     */
    public static ConcurrentHashMap<WorldCoordinates, WeakReference<TileVisNode>> getSources(int dimension) {
        return sources.get(dimension);
    }

    /**
     * Clear the nearby nodes cache.
     */
    public static void clearNearbyCache() {
        nearbyNodes.clear();
    }

    /**
     * Read network data from NBT.
     */
    public static void readFromNBT(CompoundTag tag) {
        // TODO: Implement network persistence
    }

    /**
     * Write network data to NBT.
     */
    public static void writeToNBT(CompoundTag tag) {
        // TODO: Implement network persistence
    }
}
