package thaumcraft.common.items.casters.foci;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusMedium;
import thaumcraft.api.casters.NodeSetting;
import thaumcraft.api.casters.Trajectory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Plan Medium - Targets multiple blocks based on a selection pattern.
 * Can operate in "full" mode (3D volume) or "surface" mode (connected surface blocks).
 * Used with block-affecting effects like Break or Exchange.
 */
public class FocusMediumPlan extends FocusMedium {

    /** Target all blocks in a 3D volume */
    public static final int METHOD_FULL = 0;
    /** Target only exposed surface blocks of the same type */
    public static final int METHOD_SURFACE = 1;
    
    // Checked positions for flood-fill algorithms
    private final Set<BlockPos> checked = new HashSet<>();

    @Override
    public String getResearch() {
        return "FOCUSPLAN";
    }

    @Override
    public String getKey() {
        return "thaumcraft.PLAN";
    }

    @Override
    public int getComplexity() {
        return 4;
    }

    @Override
    public Aspect getAspect() {
        return Aspect.CRAFT;
    }

    @Override
    public EnumSupplyType[] mustBeSupplied() {
        return new EnumSupplyType[] { EnumSupplyType.TRAJECTORY };
    }

    @Override
    public EnumSupplyType[] willSupply() {
        return new EnumSupplyType[] { EnumSupplyType.TARGET };
    }

    @Override
    public HitResult[] supplyTargets() {
        if (getParent() == null || getPackage() == null || getPackage().world == null) {
            return new HitResult[0];
        }
        
        Player player = getCasterPlayer();
        if (player == null) {
            return new HitResult[0];
        }
        
        Level world = getPackage().world;
        List<HitResult> targets = new ArrayList<>();
        
        Trajectory[] parentTrajectories = getParent().supplyTrajectories();
        if (parentTrajectories == null) {
            return new HitResult[0];
        }
        
        for (Trajectory trajectory : parentTrajectories) {
            Vec3 start = trajectory.source;
            Vec3 direction = trajectory.direction.normalize();
            Vec3 end = start.add(direction.scale(16.0));
            
            // Raycast to find the target block
            BlockHitResult blockHit = world.clip(new ClipContext(
                    start, end,
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE,
                    player));
            
            if (blockHit.getType() == HitResult.Type.BLOCK) {
                // Get all affected blocks based on method
                List<BlockPos> affectedBlocks = getAffectedBlocks(world, blockHit.getBlockPos(), 
                        blockHit.getDirection(), player);
                
                // Sort by distance from hit point
                BlockPos hitPos = blockHit.getBlockPos();
                affectedBlocks.sort(Comparator.comparingDouble(pos -> pos.distSqr(hitPos)));
                
                // Convert to hit results
                for (BlockPos pos : affectedBlocks) {
                    targets.add(new BlockHitResult(
                            Vec3.atCenterOf(pos),
                            blockHit.getDirection(),
                            pos,
                            false));
                }
            }
        }
        
        return targets.toArray(new HitResult[0]);
    }

    @Override
    public NodeSetting[] createSettings() {
        int[] method = { METHOD_FULL, METHOD_SURFACE };
        String[] methodDesc = { "focus.plan.full", "focus.plan.surface" };
        
        return new NodeSetting[] {
            new NodeSetting("method", "focus.plan.method", 
                new NodeSetting.NodeSettingIntList(method, methodDesc))
        };
    }

    @Override
    public boolean isExclusive() {
        // Only one plan medium can be in a focus
        return true;
    }

    /**
     * Get all blocks affected by this plan medium.
     */
    private List<BlockPos> getAffectedBlocks(Level world, BlockPos hitPos, Direction side, Player player) {
        List<BlockPos> result = new ArrayList<>();
        checked.clear();
        
        // TODO: Get area size from caster item (CasterManager.getAreaX/Y/Z)
        // For now, use default 1x1x1 area
        int sizeX = 1;
        int sizeY = 1;
        int sizeZ = 1;
        
        if (getSettingValue("method") == METHOD_FULL) {
            checkNeighboursFull(world, hitPos, hitPos, side, sizeX, sizeY, sizeZ, result);
        } else {
            BlockState targetState = world.getBlockState(hitPos);
            checkNeighboursSurface(world, hitPos, targetState, hitPos, side, sizeX, sizeY, sizeZ, result);
        }
        
        return result;
    }

    /**
     * Flood-fill to find all blocks in a 3D volume.
     */
    private void checkNeighboursFull(Level world, BlockPos origin, BlockPos current, Direction side,
                                      int sizeX, int sizeY, int sizeZ, List<BlockPos> result) {
        if (checked.contains(current)) {
            return;
        }
        checked.add(current);
        
        if (!world.getBlockState(current).isAir()) {
            result.add(current);
        }
        
        // Calculate bounds offset by the hit side
        int xs = origin.getX() - sizeX - sizeX * side.getStepX();
        int xe = origin.getX() + sizeX - sizeX * side.getStepX();
        int ys = origin.getY() - sizeY - sizeY * side.getStepY();
        int ye = origin.getY() + sizeY - sizeY * side.getStepY();
        int zs = origin.getZ() - sizeZ - sizeZ * side.getStepZ();
        int ze = origin.getZ() + sizeZ - sizeZ * side.getStepZ();
        
        // Check all neighbors within bounds
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = current.relative(dir);
            if (neighbor.getX() >= xs && neighbor.getX() <= xe &&
                neighbor.getY() >= ys && neighbor.getY() <= ye &&
                neighbor.getZ() >= zs && neighbor.getZ() <= ze) {
                checkNeighboursFull(world, origin, neighbor, side, sizeX, sizeY, sizeZ, result);
            }
        }
    }

    /**
     * Flood-fill to find connected surface blocks of the same type.
     */
    private void checkNeighboursSurface(Level world, BlockPos origin, BlockState targetState, BlockPos current,
                                         Direction side, int sizeX, int sizeY, int sizeZ, List<BlockPos> result) {
        if (checked.contains(current)) {
            return;
        }
        checked.add(current);
        
        // Check distance based on side axis
        switch (side.getAxis()) {
            case Y:
                if (Math.abs(current.getX() - origin.getX()) > sizeX) return;
                if (Math.abs(current.getZ() - origin.getZ()) > sizeZ) return;
                break;
            case Z:
                if (Math.abs(current.getX() - origin.getX()) > sizeX) return;
                if (Math.abs(current.getY() - origin.getY()) > sizeY) return;
                break;
            case X:
                if (Math.abs(current.getY() - origin.getY()) > sizeY) return;
                if (Math.abs(current.getZ() - origin.getZ()) > sizeZ) return;
                break;
        }
        
        BlockState currentState = world.getBlockState(current);
        
        // Must be same block type, not air, and exposed to air
        if (currentState.is(targetState.getBlock()) && 
            !currentState.isAir() && 
            isBlockExposed(world, current)) {
            
            result.add(current);
            
            // Check neighbors on the surface plane (not through the surface)
            for (Direction dir : Direction.values()) {
                if (dir != side && dir != side.getOpposite()) {
                    checkNeighboursSurface(world, origin, targetState, current.relative(dir), 
                            side, sizeX, sizeY, sizeZ, result);
                }
            }
        }
    }

    /**
     * Check if a block has at least one air neighbor (is exposed).
     */
    private boolean isBlockExposed(Level world, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            if (world.getBlockState(pos.relative(dir)).isAir()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the caster player from the focus package.
     */
    private Player getCasterPlayer() {
        if (getPackage() == null || getPackage().getCasterUUID() == null) {
            return null;
        }
        if (getPackage().world != null) {
            for (Player player : getPackage().world.players()) {
                if (player.getUUID().equals(getPackage().getCasterUUID())) {
                    return player;
                }
            }
        }
        return null;
    }
}




