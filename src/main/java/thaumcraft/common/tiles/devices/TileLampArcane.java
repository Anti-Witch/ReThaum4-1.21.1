package thaumcraft.common.tiles.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.tiles.TileThaumcraft;
import thaumcraft.init.ModBlockEntities;
import thaumcraft.init.ModBlocks;

/**
 * Arcane lamp tile entity - places invisible light sources in dark areas.
 * Prevents mob spawning by keeping the area lit.
 */
public class TileLampArcane extends TileThaumcraft {

    private static final int RANGE = 16;

    public TileLampArcane(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public TileLampArcane(BlockPos pos, BlockState state) {
        this(ModBlockEntities.LAMP_ARCANE.get(), pos, state);
    }

    // ==================== Tick ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileLampArcane tile) {
        // Only run every 5 ticks when not powered
        if (level.getGameTime() % 5 != 0) return;
        if (tile.gettingPower()) return;

        // Pick a random position in range
        int x = level.random.nextInt(RANGE * 2 + 1) - RANGE;
        int y = level.random.nextInt(RANGE * 2 + 1) - RANGE;
        int z = level.random.nextInt(RANGE * 2 + 1) - RANGE;

        BlockPos targetPos = pos.offset(x, y, z);

        // Clamp to reasonable height
        int surfaceY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, targetPos.getX(), targetPos.getZ());
        if (targetPos.getY() > surfaceY + 4) {
            targetPos = new BlockPos(targetPos.getX(), surfaceY + 4, targetPos.getZ());
        }
        if (targetPos.getY() < level.getMinBuildHeight() + 5) {
            targetPos = new BlockPos(targetPos.getX(), level.getMinBuildHeight() + 5, targetPos.getZ());
        }

        // Check if we can place a light source
        if (level.isEmptyBlock(targetPos) && 
            level.getBrightness(LightLayer.BLOCK, targetPos) < 11 &&
            hasLineOfSight(level, pos, targetPos)) {
            
            // Place invisible light source
            level.setBlock(targetPos, ModBlocks.EFFECT_GLIMMER.get().defaultBlockState(), 3);
        }
    }

    /**
     * Check if there's line of sight between two positions.
     */
    private static boolean hasLineOfSight(Level level, BlockPos from, BlockPos to) {
        // Simple raycast check
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        
        if (distance < 1) return true;
        
        dx /= distance;
        dy /= distance;
        dz /= distance;
        
        double x = from.getX() + 0.5;
        double y = from.getY() + 0.5;
        double z = from.getZ() + 0.5;
        
        for (int i = 0; i < (int) distance; i++) {
            x += dx;
            y += dy;
            z += dz;
            
            BlockPos checkPos = new BlockPos((int) x, (int) y, (int) z);
            if (!level.isEmptyBlock(checkPos) && !checkPos.equals(from) && !checkPos.equals(to)) {
                BlockState state = level.getBlockState(checkPos);
                // Allow transparent blocks
                if (state.canOcclude()) {
                    return false;
                }
            }
        }
        
        return true;
    }

    /**
     * Remove all placed light sources when the lamp is broken.
     */
    public void removeLights() {
        if (level == null) return;

        for (int x = -RANGE; x <= RANGE; x++) {
            for (int y = -RANGE; y <= RANGE; y++) {
                for (int z = -RANGE; z <= RANGE; z++) {
                    BlockPos checkPos = worldPosition.offset(x, y, z);
                    BlockState state = level.getBlockState(checkPos);
                    if (state.is(ModBlocks.EFFECT_GLIMMER.get())) {
                        level.setBlock(checkPos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    /**
     * Check if this block is receiving redstone power.
     */
    protected boolean gettingPower() {
        return level != null && level.hasNeighborSignal(worldPosition);
    }
}




