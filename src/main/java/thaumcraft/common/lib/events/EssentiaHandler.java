package thaumcraft.common.lib.events;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IAspectSource;
import thaumcraft.api.internal.WorldCoordinates;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.fx.PacketFXEssentiaSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EssentiaHandler - Manages essentia transport between containers.
 * 
 * Provides methods to:
 * - Drain essentia from nearby sources to a destination
 * - Add essentia to nearby containers from a source
 * - Find essentia sources within range
 * - Check if containers can accept essentia
 * 
 * Caches source locations for performance, refreshing periodically.
 * 
 * Ported from Thaumcraft 1.12.2 to 1.20.1
 */
public class EssentiaHandler {
    
    // Delay in milliseconds before refreshing source cache
    private static final int DELAY = 10000;
    
    // Cached sources for each destination tile
    private static final HashMap<WorldCoordinates, ArrayList<WorldCoordinates>> sources = new HashMap<>();
    
    // Timestamps for when to refresh source cache
    private static final HashMap<WorldCoordinates, Long> sourcesDelay = new HashMap<>();
    
    // Last operation state for confirmation-based draining
    private static BlockEntity lastTarget = null;
    private static BlockEntity lastSource = null;
    private static Aspect lastAspect = null;
    private static int lastExt = 0;
    
    // Client-side FX tracking
    public static final ConcurrentHashMap<String, EssentiaSourceFX> sourceFX = new ConcurrentHashMap<>();
    
    /**
     * Drain essentia from nearby sources to a tile.
     * 
     * @param tile The destination tile
     * @param aspect The aspect to drain
     * @param direction Direction to search (null for all directions)
     * @param range Search range
     * @param ext Extra data for FX
     * @return true if essentia was drained
     */
    public static boolean drainEssentia(BlockEntity tile, Aspect aspect, Direction direction, int range, int ext) {
        return drainEssentia(tile, aspect, direction, range, false, ext);
    }
    
    /**
     * Drain essentia from nearby sources to a tile.
     * 
     * @param tile The destination tile
     * @param aspect The aspect to drain
     * @param direction Direction to search (null for all directions)
     * @param range Search range
     * @param ignoreMirror Whether to ignore essentia mirrors
     * @param ext Extra data for FX
     * @return true if essentia was drained
     */
    public static boolean drainEssentia(BlockEntity tile, Aspect aspect, Direction direction, int range, boolean ignoreMirror, int ext) {
        if (tile == null || tile.getLevel() == null) return false;
        
        WorldCoordinates tileLoc = new WorldCoordinates(tile);
        
        // Ensure we have a cached source list
        if (!sources.containsKey(tileLoc)) {
            getSources(tile.getLevel(), tileLoc, direction, range);
            if (!sources.containsKey(tileLoc)) return false;
            return drainEssentia(tile, aspect, direction, range, ignoreMirror, ext);
        }
        
        ArrayList<WorldCoordinates> sourceList = sources.get(tileLoc);
        
        for (WorldCoordinates source : sourceList) {
            BlockEntity sourceTile = tile.getLevel().getBlockEntity(source.pos);
            if (sourceTile == null || !(sourceTile instanceof IAspectSource)) {
                break;
            }
            
            IAspectSource as = (IAspectSource) sourceTile;
            if (as.isBlocked()) continue;
            
            // TODO: Add mirror check when TileMirrorEssentia is ported
            // if (ignoreMirror && sourceTile instanceof TileMirrorEssentia) continue;
            
            if (as.takeFromContainer(aspect, 1)) {
                // Send FX packet
                sendEssentiaFX(tile, source.pos, aspect.getColor(), ext);
                return true;
            }
        }
        
        // No source found, clear cache and set delay
        sources.remove(tileLoc);
        sourcesDelay.put(tileLoc, System.currentTimeMillis() + DELAY);
        return false;
    }
    
    /**
     * Drain essentia with confirmation - checks first, then confirms.
     * Call confirmDrain() after to actually drain.
     */
    public static boolean drainEssentiaWithConfirmation(BlockEntity tile, Aspect aspect, Direction direction, int range, boolean ignoreMirror, int ext) {
        if (tile == null || tile.getLevel() == null) return false;
        
        WorldCoordinates tileLoc = new WorldCoordinates(tile);
        
        if (!sources.containsKey(tileLoc)) {
            getSources(tile.getLevel(), tileLoc, direction, range);
            if (!sources.containsKey(tileLoc)) return false;
            return drainEssentiaWithConfirmation(tile, aspect, direction, range, ignoreMirror, ext);
        }
        
        ArrayList<WorldCoordinates> sourceList = sources.get(tileLoc);
        
        for (WorldCoordinates source : sourceList) {
            BlockEntity sourceTile = tile.getLevel().getBlockEntity(source.pos);
            if (sourceTile == null || !(sourceTile instanceof IAspectSource)) {
                break;
            }
            
            IAspectSource as = (IAspectSource) sourceTile;
            if (as.isBlocked()) continue;
            
            if (as.doesContainerContainAmount(aspect, 1)) {
                lastSource = sourceTile;
                lastAspect = aspect;
                lastTarget = tile;
                lastExt = ext;
                return true;
            }
        }
        
        sources.remove(tileLoc);
        sourcesDelay.put(tileLoc, System.currentTimeMillis() + DELAY);
        return false;
    }
    
    /**
     * Confirm the last drain operation.
     */
    public static void confirmDrain() {
        if (lastSource != null && lastAspect != null && lastTarget != null) {
            IAspectSource as = (IAspectSource) lastSource;
            if (as.takeFromContainer(lastAspect, 1)) {
                sendEssentiaFX(lastTarget, lastSource.getBlockPos(), lastAspect.getColor(), lastExt);
            }
        }
        lastSource = null;
        lastAspect = null;
        lastTarget = null;
    }
    
    /**
     * Add essentia to nearby containers from a tile.
     */
    public static boolean addEssentia(BlockEntity tile, Aspect aspect, Direction direction, int range, boolean ignoreMirror, int ext) {
        if (tile == null || tile.getLevel() == null) return false;
        
        WorldCoordinates tileLoc = new WorldCoordinates(tile);
        
        if (!sources.containsKey(tileLoc)) {
            getSources(tile.getLevel(), tileLoc, direction, range);
            if (!sources.containsKey(tileLoc)) return false;
            return addEssentia(tile, aspect, direction, range, ignoreMirror, ext);
        }
        
        ArrayList<WorldCoordinates> sourceList = sources.get(tileLoc);
        ArrayList<WorldCoordinates> empties = new ArrayList<>();
        
        // First pass - try non-empty containers that already have this aspect
        for (WorldCoordinates source : sourceList) {
            BlockEntity sourceTile = tile.getLevel().getBlockEntity(source.pos);
            if (sourceTile == null || !(sourceTile instanceof IAspectSource)) {
                break;
            }
            
            IAspectSource as = (IAspectSource) sourceTile;
            if (as.isBlocked()) continue;
            
            if (as.doesContainerAccept(aspect)) {
                if (as.getAspects() == null || as.getAspects().visSize() == 0) {
                    empties.add(source);
                } else if (as.addToContainer(aspect, 1) <= 0) {
                    sendEssentiaFX(sourceTile, tile.getBlockPos(), aspect.getColor(), ext);
                    return true;
                }
            }
        }
        
        // Second pass - try empty containers
        for (WorldCoordinates source : empties) {
            if (source == null || source.pos == null) continue;
            
            BlockEntity sourceTile = tile.getLevel().getBlockEntity(source.pos);
            if (sourceTile == null || !(sourceTile instanceof IAspectSource)) {
                break;
            }
            
            IAspectSource as = (IAspectSource) sourceTile;
            if (aspect != null && as.doesContainerAccept(aspect) && as.addToContainer(aspect, 1) <= 0) {
                sendEssentiaFX(sourceTile, tile.getBlockPos(), aspect.getColor(), ext);
                return true;
            }
        }
        
        sources.remove(tileLoc);
        sourcesDelay.put(tileLoc, System.currentTimeMillis() + DELAY);
        return false;
    }
    
    /**
     * Check if any source contains the specified aspect.
     */
    public static boolean findEssentia(BlockEntity tile, Aspect aspect, Direction direction, int range, boolean ignoreMirror) {
        if (tile == null || tile.getLevel() == null) return false;
        
        WorldCoordinates tileLoc = new WorldCoordinates(tile);
        
        if (!sources.containsKey(tileLoc)) {
            getSources(tile.getLevel(), tileLoc, direction, range);
            if (!sources.containsKey(tileLoc)) return false;
            return findEssentia(tile, aspect, direction, range, ignoreMirror);
        }
        
        ArrayList<WorldCoordinates> sourceList = sources.get(tileLoc);
        
        for (WorldCoordinates source : sourceList) {
            BlockEntity sourceTile = tile.getLevel().getBlockEntity(source.pos);
            if (sourceTile == null || !(sourceTile instanceof IAspectSource)) {
                break;
            }
            
            IAspectSource as = (IAspectSource) sourceTile;
            if (as.isBlocked()) continue;
            
            if (as.doesContainerContainAmount(aspect, 1)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if any container can accept the specified aspect.
     */
    public static boolean canAcceptEssentia(BlockEntity tile, Aspect aspect, Direction direction, int range, boolean ignoreMirror) {
        if (tile == null || tile.getLevel() == null) return false;
        
        WorldCoordinates tileLoc = new WorldCoordinates(tile);
        
        if (!sources.containsKey(tileLoc)) {
            getSources(tile.getLevel(), tileLoc, direction, range);
            if (!sources.containsKey(tileLoc)) return false;
        }
        
        ArrayList<WorldCoordinates> sourceList = sources.get(tileLoc);
        
        for (WorldCoordinates source : sourceList) {
            BlockEntity sourceTile = tile.getLevel().getBlockEntity(source.pos);
            if (sourceTile == null || !(sourceTile instanceof IAspectSource)) {
                break;
            }
            
            IAspectSource as = (IAspectSource) sourceTile;
            if (!as.isBlocked() && as.doesContainerAccept(aspect)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Scan for essentia sources in range.
     */
    private static void getSources(Level level, WorldCoordinates tileLoc, Direction direction, int range) {
        // Check if we're in delay period
        if (sourcesDelay.containsKey(tileLoc)) {
            long delay = sourcesDelay.get(tileLoc);
            if (delay > System.currentTimeMillis()) {
                return;
            }
            sourcesDelay.remove(tileLoc);
        }
        
        ArrayList<WorldCoordinates> sourceList = new ArrayList<>();
        
        int start = 0;
        Direction searchDir = direction;
        if (searchDir == null) {
            start = -range;
            searchDir = Direction.UP;
        }
        
        // Scan in 3D based on direction
        for (int aa = -range; aa <= range; ++aa) {
            for (int bb = -range; bb <= range; ++bb) {
                for (int cc = start; cc < range; ++cc) {
                    if (aa == 0 && bb == 0 && cc == 0) continue;
                    
                    int xx = tileLoc.pos.getX();
                    int yy = tileLoc.pos.getY();
                    int zz = tileLoc.pos.getZ();
                    
                    // Adjust coordinates based on search direction
                    if (searchDir.getAxis() == Direction.Axis.Y) {
                        xx += aa;
                        yy += cc * searchDir.getStepY();
                        zz += bb;
                    } else if (searchDir.getAxis() == Direction.Axis.Z) {
                        xx += aa;
                        yy += bb;
                        zz += cc * searchDir.getStepZ();
                    } else {
                        xx += cc * searchDir.getStepX();
                        yy += aa;
                        zz += bb;
                    }
                    
                    BlockEntity te = level.getBlockEntity(new BlockPos(xx, yy, zz));
                    if (te instanceof IAspectSource) {
                        sourceList.add(new WorldCoordinates(new BlockPos(xx, yy, zz), level));
                    }
                }
            }
        }
        
        if (!sourceList.isEmpty()) {
            // Sort by distance (closest first)
            ArrayList<WorldCoordinates> sortedList = new ArrayList<>();
            for (WorldCoordinates wc : sourceList) {
                double dist = wc.getDistanceSquaredToWorldCoordinates(tileLoc);
                boolean inserted = false;
                
                for (int a = 0; a < sortedList.size(); ++a) {
                    double d2 = sortedList.get(a).getDistanceSquaredToWorldCoordinates(tileLoc);
                    if (dist < d2) {
                        sortedList.add(a, wc);
                        inserted = true;
                        break;
                    }
                }
                
                if (!inserted) {
                    sortedList.add(wc);
                }
            }
            
            sources.put(tileLoc, sortedList);
        } else {
            sourcesDelay.put(tileLoc, System.currentTimeMillis() + DELAY);
        }
    }
    
    /**
     * Force refresh of sources for a tile.
     */
    public static void refreshSources(BlockEntity tile) {
        if (tile != null) {
            sources.remove(new WorldCoordinates(tile));
        }
    }
    
    /**
     * Send essentia visual effect packet.
     */
    private static void sendEssentiaFX(BlockEntity destTile, BlockPos sourcePos, int color, int ext) {
        if (destTile.getLevel() instanceof ServerLevel serverLevel) {
            BlockPos destPos = destTile.getBlockPos();
            PacketFXEssentiaSource packet = new PacketFXEssentiaSource(
                    destPos, sourcePos, color, ext);
            
            PacketHandler.sendToAllTrackingChunk(packet, serverLevel, destPos);
        }
    }
    
    /**
     * Client-side essentia source FX tracking.
     */
    public static class EssentiaSourceFX {
        public BlockPos start;
        public BlockPos end;
        public int color;
        public int ext;
        
        public EssentiaSourceFX(BlockPos start, BlockPos end, int color, int ext) {
            this.start = start;
            this.end = end;
            this.color = color;
            this.ext = ext;
        }
    }
}




