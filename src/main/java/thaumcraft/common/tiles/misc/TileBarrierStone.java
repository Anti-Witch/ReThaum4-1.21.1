package thaumcraft.common.tiles.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.tiles.TileThaumcraft;
import thaumcraft.init.ModBlockEntities;
import thaumcraft.init.ModBlocks;

import java.util.List;

/**
 * TileBarrierStone - Block entity for paving stone barriers.
 * 
 * Creates invisible barrier blocks above it that block non-player entities
 * but allow players to pass through. Can be disabled with redstone.
 * 
 * Features:
 * - Spawns invisible barrier blocks 1-2 blocks above
 * - Pushes non-player entities away from the barrier
 * - Disabled when receiving redstone power
 * 
 * Ported from 1.12.2
 */
public class TileBarrierStone extends TileThaumcraft {

    private int tickCount = 0;

    public TileBarrierStone(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BARRIER_STONE.get(), pos, state);
    }

    /**
     * Check if this block is receiving redstone power.
     */
    public boolean isGettingPower() {
        return level != null && level.hasNeighborSignal(worldPosition);
    }

    /**
     * Server tick - manages barrier blocks and pushes entities.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, TileBarrierStone tile) {
        if (tile.tickCount == 0) {
            tile.tickCount = level.random.nextInt(100);
        }

        // Push entities every 5 ticks when not powered
        if (tile.tickCount % 5 == 0 && !tile.isGettingPower()) {
            // Check for non-player living entities in the barrier area
            AABB area = new AABB(
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + 3, pos.getZ() + 1
            ).inflate(0.1);

            List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, area);
            for (LivingEntity entity : entities) {
                // Only push non-player entities that are in the air
                if (!entity.onGround() && !(entity instanceof Player)) {
                    float yaw = entity.getYRot() + 180.0f;
                    float radians = yaw * (float) Math.PI / 180.0f;
                    
                    // Push entity backward and down slightly
                    Vec3 push = new Vec3(
                        -Math.sin(radians) * 0.2,
                        -0.1,
                        Math.cos(radians) * 0.2
                    );
                    entity.setDeltaMovement(entity.getDeltaMovement().add(push));
                }
            }
        }

        // Spawn/maintain barrier blocks every 100 ticks
        if (++tile.tickCount % 100 == 0) {
            Block barrierBlock = ModBlocks.BARRIER.get();
            BlockState barrierState = barrierBlock.defaultBlockState();

            BlockPos pos1 = pos.above(1);
            BlockPos pos2 = pos.above(2);

            // Place barrier at pos+1 if not already there and air
            if (!level.getBlockState(pos1).is(barrierBlock) && level.getBlockState(pos1).isAir()) {
                level.setBlock(pos1, barrierState, Block.UPDATE_ALL);
            }

            // Place barrier at pos+2 if not already there and air
            if (!level.getBlockState(pos2).is(barrierBlock) && level.getBlockState(pos2).isAir()) {
                level.setBlock(pos2, barrierState, Block.UPDATE_ALL);
            }
        }
    }
}




