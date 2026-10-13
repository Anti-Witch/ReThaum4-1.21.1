package thaumcraft.common.lib.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.StructureBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.CommonHooks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * BlockUtils - Utility methods for working with blocks.
 * 
 * Provides common operations like breaking blocks, checking block properties,
 * and finding blocks in areas.
 * 
 * Ported from 1.12.2
 */
@SuppressWarnings("deprecation")
public class BlockUtils {

    // State for tree felling algorithm
    private static BlockPos lastPos = BlockPos.ZERO;
    private static double lastDistance = 0.0;
    
    /** Blocks that cannot be moved by portable hole */
    public static ArrayList<String> portableHoleBlackList = new ArrayList<>();

    // ==================== Block Breaking ====================

    /**
     * Remove a block from the world (internal helper).
     */
    private static boolean removeBlock(Level level, Player player, BlockPos pos, boolean canHarvest) {
        BlockState state = level.getBlockState(pos);
        boolean removed = state.onDestroyedByPlayer(level, pos, player, canHarvest, level.getFluidState(pos));
        if (removed) {
            state.getBlock().destroy(level, pos, state);
        }
        return removed;
    }

    /**
     * Harvest a block as if broken by a player, skipping the break event.
     */
    public static boolean harvestBlockSkipCheck(Level level, Player player, BlockPos pos) {
        return harvestBlock(level, player, pos, false, false, 0, true);
    }

    /**
     * Harvest a block as if broken by a player.
     */
    public static boolean harvestBlock(Level level, Player player, BlockPos pos) {
        return harvestBlock(level, player, pos, false, false, 0, false);
    }

    /**
     * Harvest a block with full control over silk touch and fortune.
     * 
     * @param level The world
     * @param player The player harvesting
     * @param pos Position to harvest
     * @param alwaysDrop Always drop items even if player can't normally harvest
     * @param silkOverride Force silk touch behavior
     * @param fortuneOverride Override fortune level (0 = use tool's fortune)
     * @param skipEvent Skip the block break event (for recursive calls)
     * @return true if block was harvested
     */
    public static boolean harvestBlock(Level level, Player player, BlockPos pos, 
            boolean alwaysDrop, boolean silkOverride, int fortuneOverride, boolean skipEvent) {
        
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        // Fire break event unless skipping
        BlockState state = level.getBlockState(pos);
        if (!skipEvent) {
            var breakEvent = CommonHooks.fireBlockBreak(level, serverPlayer.gameMode.getGameModeForPlayer(), serverPlayer, pos, state);
            if (breakEvent.isCanceled()) {
                return false;
            }
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        Block block = state.getBlock();

        // Check for protected blocks
        if ((block instanceof CommandBlock || block instanceof StructureBlock) && !serverPlayer.canUseGameMasterBlocks()) {
            level.sendBlockUpdated(pos, state, state, 3);
            return false;
        }

        // Play break sound/particles
        level.levelEvent(null, 2001, pos, Block.getId(state));

        boolean removed;
        if (serverPlayer.isCreative()) {
            removed = removeBlock(level, player, pos, false);
            serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(level, pos));
        } else {
            ItemStack tool = player.getMainHandItem();
            boolean canHarvest = alwaysDrop || state.canHarvestBlock(level, pos, player);
            
            removed = removeBlock(level, player, pos, canHarvest);
            
            if (removed && canHarvest) {
                // Create a tool stack with modified enchantments if needed
                ItemStack effectiveTool = tool;
                
                int currentFortune = tool.getEnchantments().getLevel(TCCompat.enchantment(level, Enchantments.FORTUNE));
                boolean hasSilk = tool.getEnchantments().getLevel(TCCompat.enchantment(level, Enchantments.SILK_TOUCH)) > 0;
                
                if (silkOverride || fortuneOverride > currentFortune) {
                    effectiveTool = tool.copy();
                    
                    if (silkOverride && !hasSilk) {
                        effectiveTool.enchant(TCCompat.enchantment(level, Enchantments.SILK_TOUCH), 1);
                    }
                    
                    if (fortuneOverride > currentFortune) {
                        // Remove existing fortune and add new level
                        // Note: This is a simplified approach - in practice we'd need to manipulate NBT
                        effectiveTool.enchant(TCCompat.enchantment(level, Enchantments.FORTUNE), fortuneOverride);
                    }
                }
                
                // Drop resources
                block.playerDestroy(level, player, pos, state, blockEntity, effectiveTool);
            }
        }

        return removed;
    }

    /**
     * Break a block and drop its items, as if broken by a player.
     * 
     * @param level The world
     * @param pos Position to break
     * @param tool Tool to use (affects drops)
     * @param player Player breaking the block (can be null)
     * @return true if block was broken
     */
    public static boolean breakBlock(Level level, BlockPos pos, ItemStack tool, @Nullable Player player) {
        if (level.isClientSide) return false;
        
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || !isBreakable(level, pos)) {
            return false;
        }

        BlockEntity be = level.getBlockEntity(pos);

        // Drop items
        if (level instanceof ServerLevel serverLevel) {
            Block.dropResources(state, level, pos, be, player, tool);
        }

        // Remove block
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        return true;
    }

    /**
     * Break a block silently without drops.
     */
    public static boolean breakBlockNoDrops(Level level, BlockPos pos) {
        if (level.isClientSide) return false;
        
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;

        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        return true;
    }

    // ==================== Tree Felling ====================

    /**
     * Find connected blocks of the same type, tracking the furthest one.
     * Used for tree felling.
     */
    private static void findBlocks(Level level, BlockPos origin, BlockState targetBlock, int reach) {
        for (int xx = -reach; xx <= reach; ++xx) {
            for (int yy = reach; yy >= -reach; --yy) {
                for (int zz = -reach; zz <= reach; ++zz) {
                    // Check bounds
                    if (Math.abs(lastPos.getX() + xx - origin.getX()) > 24) return;
                    if (Math.abs(lastPos.getY() + yy - origin.getY()) > 48) return;
                    if (Math.abs(lastPos.getZ() + zz - origin.getZ()) > 24) return;
                    
                    BlockPos checkPos = lastPos.offset(xx, yy, zz);
                    BlockState bs = level.getBlockState(checkPos);
                    
                    // Check if same block type
                    boolean same = bs.is(targetBlock.getBlock());
                    
                    if (same && bs.getDestroySpeed(level, checkPos) >= 0.0f) {
                        double xd = checkPos.getX() - origin.getX();
                        double yd = checkPos.getY() - origin.getY();
                        double zd = checkPos.getZ() - origin.getZ();
                        double d = xd * xd + yd * yd + zd * zd;
                        
                        if (d > lastDistance) {
                            lastDistance = d;
                            lastPos = checkPos;
                            findBlocks(level, origin, targetBlock, reach);
                            return;
                        }
                    }
                }
            }
        }
    }

    /**
     * Break the furthest connected block of the same type.
     * Used for tree felling enchantment.
     * 
     * @param level The world
     * @param pos Starting position
     * @param block Block state to match
     * @param player Player doing the breaking
     * @return true if a block was broken
     */
    public static boolean breakFurthestBlock(Level level, BlockPos pos, BlockState block, Player player) {
        lastPos = pos;
        lastDistance = 0.0;
        
        // Use larger reach for logs (tree felling)
        int reach = isLog(level, pos) ? 2 : 1;
        findBlocks(level, pos, block, reach);
        
        boolean worked = harvestBlockSkipCheck(level, player, lastPos);
        
        // Trigger block updates
        level.sendBlockUpdated(pos, block, block, 3);
        
        // For logs, schedule leaf decay updates
        if (worked && isLog(level, pos)) {
            for (int xx = -3; xx <= 3; ++xx) {
                for (int yy = -3; yy <= 3; ++yy) {
                    for (int zz = -3; zz <= 3; ++zz) {
                        BlockPos updatePos = lastPos.offset(xx, yy, zz);
                        BlockState updateState = level.getBlockState(updatePos);
                        level.scheduleTick(updatePos, updateState.getBlock(), 50 + level.random.nextInt(75));
                    }
                }
            }
        }
        
        return worked;
    }

    // ==================== Block Exposure / Adjacency ====================

    /**
     * Check if a block position is exposed to air (has at least one air neighbor).
     */
    public static boolean isExposed(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            if (level.isEmptyBlock(pos.relative(dir))) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if a block has any exposed face (non-solid neighbor).
     * Similar to isExposed but checks for non-full blocks instead of just air.
     */
    public static boolean isBlockExposed(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighbor);
            if (!neighborState.isSolidRender(level, neighbor)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Count how many sides of a block are exposed to air.
     */
    public static int countExposedSides(Level level, BlockPos pos) {
        int count = 0;
        for (Direction dir : Direction.values()) {
            if (level.isEmptyBlock(pos.relative(dir))) {
                count++;
            }
        }
        return count;
    }

    /**
     * Check if a block is adjacent to any solid block.
     */
    public static boolean isAdjacentToSolidBlock(Level level, BlockPos pos) {
        for (Direction face : Direction.values()) {
            BlockPos neighbor = pos.relative(face);
            BlockState state = level.getBlockState(neighbor);
            if (state.isFaceSturdy(level, neighbor, face.getOpposite())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a block is touching a specific block state.
     */
    public static boolean isBlockTouching(Level level, BlockPos pos, BlockState targetState) {
        for (Direction face : Direction.values()) {
            if (level.getBlockState(pos.relative(face)) == targetState) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a block is touching a specific block type.
     */
    public static boolean isBlockTouching(Level level, BlockPos pos, Block targetBlock) {
        for (Direction face : Direction.values()) {
            if (level.getBlockState(pos.relative(face)).is(targetBlock)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the direction to a block that is touching this position.
     */
    @Nullable
    public static Direction getFaceBlockTouching(Level level, BlockPos pos, Block targetBlock) {
        for (Direction face : Direction.values()) {
            if (level.getBlockState(pos.relative(face)).is(targetBlock)) {
                return face;
            }
        }
        return null;
    }

    // ==================== Block Properties ====================

    /**
     * Check if a block can be broken by tools.
     */
    public static boolean isBreakable(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;
        if (state.getDestroySpeed(level, pos) < 0) return false; // Unbreakable
        return true;
    }

    /**
     * Check if a block is a plant (grass, flowers, crops, etc.).
     */
    public static boolean isPlant(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        
        return block instanceof net.minecraft.world.level.block.BushBlock ||
               block instanceof net.minecraft.world.level.block.CropBlock ||
               block instanceof net.minecraft.world.level.block.StemBlock ||
               block instanceof net.minecraft.world.level.block.VineBlock ||
               block instanceof net.minecraft.world.level.block.SaplingBlock;
    }

    /**
     * Check if a block is a log (wood).
     */
    public static boolean isLog(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(BlockTags.LOGS);
    }

    /**
     * Check if a block is leaves.
     */
    public static boolean isLeaves(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(BlockTags.LEAVES);
    }

    /**
     * Check if a block is ore.
     */
    public static boolean isOre(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(BlockTags.GOLD_ORES) ||
               state.is(BlockTags.IRON_ORES) ||
               state.is(BlockTags.COPPER_ORES) ||
               state.is(BlockTags.COAL_ORES) ||
               state.is(BlockTags.REDSTONE_ORES) ||
               state.is(BlockTags.LAPIS_ORES) ||
               state.is(BlockTags.DIAMOND_ORES) ||
               state.is(BlockTags.EMERALD_ORES);
    }

    /**
     * Get the axis of a block (for logs, pillars, etc.).
     */
    public static Direction.Axis getBlockAxis(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        
        // Check for AXIS property (logs, pillars)
        if (state.hasProperty(BlockStateProperties.AXIS)) {
            return state.getValue(BlockStateProperties.AXIS);
        }
        
        return Direction.Axis.Y; // Default
    }

    // ==================== Blacklist Checking ====================

    /**
     * Check if a block is on the portable hole blacklist.
     */
    public static boolean isPortableHoleBlackListed(BlockState state) {
        return isBlockListed(state, portableHoleBlackList);
    }

    /**
     * Check if a block is on a string-based blacklist.
     * Format: "modid:blockname" or "modid:blockname;property=value"
     */
    public static boolean isBlockListed(BlockState state, List<String> list) {
        String blockName = state.getBlock().builtInRegistryHolder().key().location().toString();
        String stateString = state.toString();
        
        for (String key : list) {
            String[] parts = key.split(";");
            
            if (parts[0].contains(":")) {
                // Full registry name match
                if (!blockName.equals(parts[0])) {
                    continue;
                }
                
                // No property requirements
                if (parts.length <= 1) {
                    return true;
                }
                
                // Check all property requirements
                int matches = 0;
                for (int i = 1; i < parts.length; i++) {
                    if (stateString.contains(parts[i])) {
                        matches++;
                    }
                }
                
                if (matches == parts.length - 1) {
                    return true;
                }
            } else {
                // Tag-based match
                var tag = BlockTags.create(net.minecraft.resources.ResourceLocation.parse(parts[0]));
                if (state.is(tag)) {
                    return true;
                }
            }
        }
        
        return false;
    }

    // ==================== Distance / Geometry ====================

    /**
     * Get squared distance between two block positions.
     */
    public static double distanceSq(BlockPos b1, BlockPos b2) {
        double dx = b1.getX() - b2.getX();
        double dy = b1.getY() - b2.getY();
        double dz = b1.getZ() - b2.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Get the facing direction from one position to another.
     */
    public static Direction getDirectionTo(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        int dz = to.getZ() - from.getZ();

        int ax = Math.abs(dx);
        int ay = Math.abs(dy);
        int az = Math.abs(dz);

        if (ay >= ax && ay >= az) {
            return dy > 0 ? Direction.UP : Direction.DOWN;
        } else if (ax >= az) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        } else {
            return dz > 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }

    /**
     * Find all block positions in a cubic area.
     */
    public static List<BlockPos> getBlocksInArea(BlockPos center, int radius) {
        List<BlockPos> positions = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    positions.add(center.offset(x, y, z));
                }
            }
        }
        return positions;
    }

    /**
     * Find all block positions in a spherical area.
     */
    public static List<BlockPos> getBlocksInSphere(BlockPos center, double radius) {
        List<BlockPos> positions = new ArrayList<>();
        int r = (int) Math.ceil(radius);
        double rSq = radius * radius;
        
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    if (x * x + y * y + z * z <= rSq) {
                        positions.add(center.offset(x, y, z));
                    }
                }
            }
        }
        return positions;
    }

    /**
     * Get the AABB for a block position.
     */
    public static AABB getBlockAABB(BlockPos pos) {
        return new AABB(pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
    }

    /**
     * Get the center point of a block position.
     */
    public static Vec3 getBlockCenter(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    // ==================== Line of Sight ====================

    /**
     * Check if there is line of sight between two block positions.
     */
    public static boolean hasLOS(Level level, BlockPos source, BlockPos target) {
        Vec3 sourceVec = new Vec3(source.getX() + 0.5, source.getY() + 0.5, source.getZ() + 0.5);
        Vec3 targetVec = new Vec3(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
        
        BlockHitResult result = level.clip(new ClipContext(
                sourceVec, targetVec,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, (net.minecraft.world.entity.Entity) null));
        
        if (result.getType() == HitResult.Type.MISS) {
            return true;
        }
        
        // Check if we hit the target block
        return result.getBlockPos().equals(target);
    }

    /**
     * Ray trace from an entity's eyes.
     */
    public static BlockHitResult getTargetBlock(Level level, Entity entity, boolean stopOnLiquid) {
        return getTargetBlock(level, entity, stopOnLiquid, stopOnLiquid, 10.0);
    }

    /**
     * Ray trace from an entity's eyes with options.
     */
    public static BlockHitResult getTargetBlock(Level level, Entity entity, 
            boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox, double range) {
        
        Vec3 eyePos = entity.getEyePosition();
        Vec3 lookVec = entity.getViewVector(1.0f);
        Vec3 endPos = eyePos.add(lookVec.scale(range));
        
        ClipContext.Fluid fluidMode = stopOnLiquid ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE;
        ClipContext.Block blockMode = ignoreBlockWithoutBoundingBox ? 
                ClipContext.Block.COLLIDER : ClipContext.Block.OUTLINE;
        
        return level.clip(new ClipContext(eyePos, endPos, blockMode, fluidMode, entity));
    }

    // ==================== Block Placement ====================

    /**
     * Place a block in the world.
     * 
     * @param level The world
     * @param pos Position to place
     * @param state Block state to place
     * @return true if block was placed
     */
    public static boolean placeBlock(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return false;
        
        BlockState existing = level.getBlockState(pos);
        if (!existing.canBeReplaced()) {
            return false;
        }

        level.setBlock(pos, state, Block.UPDATE_ALL);
        return true;
    }

    // ==================== Comparator ====================

    /**
     * Comparator for sorting BlockPos by distance from a source position.
     */
    public static class BlockPosComparator implements Comparator<BlockPos> {
        private final BlockPos source;
        
        public BlockPosComparator(BlockPos source) {
            this.source = source;
        }
        
        @Override
        public int compare(BlockPos a, BlockPos b) {
            if (a.equals(b)) {
                return 0;
            }
            double da = distanceSq(source, a);
            double db = distanceSq(source, b);
            return Double.compare(da, db);
        }
    }
}




