package thaumcraft.common.world.features;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import thaumcraft.init.ModBlocks;

/**
 * Generates Greatwood trees - large magical trees with thick trunks.
 * 
 * Greatwood trees are characterized by:
 * - 2x2 trunk base
 * - Height of 11-22 blocks
 * - Sprawling branch structure
 * - Dense leaf canopy
 * - Optional spider nest variant with spawner and loot
 */
public class GreatwoodTreeFeature extends Feature<NoneFeatureConfiguration> {

    private static final byte[] OTHER_COORD_PAIRS = {2, 0, 0, 1, 2, 1};
    
    // Tree generation parameters
    private int heightLimit;
    private int height;
    private final double heightAttenuation = 0.618;
    private final double branchDensity = 1.0;
    private final double branchSlope = 0.38;
    private double scaleWidth = 1.2;
    private final double leafDensity = 0.9;
    private final int trunkSize = 2;
    private final int heightLimitLimit = 11;
    private final int leafDistanceLimit = 4;
    
    private int[][] leafNodes;
    private int[] basePos = new int[3];
    
    public GreatwoodTreeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }
    
    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos pos = context.origin();
        RandomSource random = context.random();
        
        // 1 in 8 chance for spider variant
        boolean spiders = random.nextInt(8) == 0;
        
        return generateTree(level, random, pos, spiders);
    }
    
    private boolean generateTree(WorldGenLevel level, RandomSource random, BlockPos pos, boolean spiders) {
        basePos[0] = pos.getX();
        basePos[1] = pos.getY();
        basePos[2] = pos.getZ();
        
        heightLimit = heightLimitLimit + random.nextInt(heightLimitLimit);
        
        // Check all trunk positions are valid
        for (int x = 0; x < trunkSize; x++) {
            for (int z = 0; z < trunkSize; z++) {
                if (!isValidTreeLocation(level, x, z)) {
                    return false;
                }
            }
        }
        
        // Clear the base position
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        
        // Generate the tree structure
        generateLeafNodeList(random);
        generateLeaves(level, random);
        generateLeafNodeBases(level);
        generateTrunk(level);
        
        // Generate upper canopy
        scaleWidth = 1.66;
        basePos[0] = pos.getX();
        basePos[1] = pos.getY() + height;
        basePos[2] = pos.getZ();
        
        generateLeafNodeList(random);
        generateLeaves(level, random);
        generateLeafNodeBases(level);
        generateTrunk(level);
        
        // Spider variant
        if (spiders) {
            generateSpiderNest(level, random, pos);
        }
        
        return true;
    }
    
    private void generateLeafNodeList(RandomSource random) {
        height = (int) (heightLimit * heightAttenuation);
        if (height >= heightLimit) {
            height = heightLimit - 1;
        }
        
        int numLeafGroups = (int) (1.382 + Math.pow(leafDensity * heightLimit / 13.0, 2.0));
        if (numLeafGroups < 1) {
            numLeafGroups = 1;
        }
        
        int[][] tempLeafNodes = new int[numLeafGroups * heightLimit][4];
        int leafY = basePos[1] + heightLimit - leafDistanceLimit;
        int nodeCount = 1;
        int branchStartY = basePos[1] + height;
        int layerIndex = leafY - basePos[1];
        
        tempLeafNodes[0][0] = basePos[0];
        tempLeafNodes[0][1] = leafY;
        tempLeafNodes[0][2] = basePos[2];
        tempLeafNodes[0][3] = branchStartY;
        leafY--;
        
        while (layerIndex >= 0) {
            float layerSize = layerSize(layerIndex);
            if (layerSize < 0.0f) {
                leafY--;
                layerIndex--;
                continue;
            }
            
            for (int i = 0; i < numLeafGroups; i++) {
                double radius = scaleWidth * layerSize * (random.nextFloat() + 0.328);
                double angle = random.nextFloat() * 2.0 * Math.PI;
                int nodeX = Mth.floor(radius * Math.sin(angle) + basePos[0] + 0.5);
                int nodeZ = Mth.floor(radius * Math.cos(angle) + basePos[2] + 0.5);
                int[] checkFrom = {nodeX, leafY, nodeZ};
                int[] checkTo = {nodeX, leafY + leafDistanceLimit, nodeZ};
                
                if (checkBlockLine(checkFrom, checkTo) == -1) {
                    int[] branchBase = {basePos[0], basePos[1], basePos[2]};
                    double dist = Math.sqrt(Math.pow(Math.abs(basePos[0] - checkFrom[0]), 2.0) +
                                           Math.pow(Math.abs(basePos[2] - checkFrom[2]), 2.0));
                    double slopeHeight = dist * branchSlope;
                    
                    if (checkFrom[1] - slopeHeight > branchStartY) {
                        branchBase[1] = branchStartY;
                    } else {
                        branchBase[1] = (int) (checkFrom[1] - slopeHeight);
                    }
                    
                    if (checkBlockLine(branchBase, checkFrom) == -1) {
                        tempLeafNodes[nodeCount][0] = nodeX;
                        tempLeafNodes[nodeCount][1] = leafY;
                        tempLeafNodes[nodeCount][2] = nodeZ;
                        tempLeafNodes[nodeCount][3] = branchBase[1];
                        nodeCount++;
                    }
                }
            }
            
            leafY--;
            layerIndex--;
        }
        
        leafNodes = new int[nodeCount][4];
        System.arraycopy(tempLeafNodes, 0, leafNodes, 0, nodeCount);
    }
    
    private float layerSize(int layer) {
        if (layer < (float) heightLimit * 0.3) {
            return -1.618f;
        }
        
        float halfHeight = heightLimit / 2.0f;
        float distFromMiddle = halfHeight - layer;
        float size;
        
        if (distFromMiddle == 0.0f) {
            size = halfHeight;
        } else if (Math.abs(distFromMiddle) >= halfHeight) {
            size = 0.0f;
        } else {
            size = (float) Math.sqrt(Math.pow(Math.abs(halfHeight), 2.0) - Math.pow(Math.abs(distFromMiddle), 2.0));
        }
        
        return size * 0.5f;
    }
    
    private float leafSize(int layer) {
        if (layer >= 0 && layer < leafDistanceLimit) {
            return (layer != 0 && layer != leafDistanceLimit - 1) ? 3.0f : 2.0f;
        }
        return -1.0f;
    }
    
    private void generateLeaves(WorldGenLevel level, RandomSource random) {
        for (int[] node : leafNodes) {
            generateLeafNode(level, node[0], node[1], node[2]);
        }
    }
    
    private void generateLeafNode(WorldGenLevel level, int x, int y, int z) {
        for (int ly = y; ly < y + leafDistanceLimit; ly++) {
            float size = leafSize(ly - y);
            generateLeafLayer(level, x, ly, z, size, (byte) 1);
        }
    }
    
    private void generateLeafLayer(WorldGenLevel level, int x, int y, int z, float radius, byte axis) {
        int intRadius = (int) (radius + 0.618);
        byte coord1 = OTHER_COORD_PAIRS[axis];
        byte coord2 = OTHER_COORD_PAIRS[axis + 3];
        int[] center = {x, y, z};
        int[] pos = new int[3];
        
        pos[axis] = center[axis];
        
        for (int i = -intRadius; i <= intRadius; i++) {
            pos[coord1] = center[coord1] + i;
            for (int j = -intRadius; j <= intRadius; j++) {
                double dist = Math.pow(Math.abs(i) + 0.5, 2.0) + Math.pow(Math.abs(j) + 0.5, 2.0);
                if (dist <= radius * radius) {
                    pos[coord2] = center[coord2] + j;
                    BlockPos blockPos = new BlockPos(pos[0], pos[1], pos[2]);
                    BlockState state = level.getBlockState(blockPos);
                    
                    if (state.isAir() || state.is(ModBlocks.GREATWOOD_LEAVES.get())) {
                        if (state.canBeReplaced()) {
                            level.setBlock(blockPos, ModBlocks.GREATWOOD_LEAVES.get().defaultBlockState(), 2);
                        }
                    }
                }
            }
        }
    }
    
    private void generateTrunk(WorldGenLevel level) {
        int[] start = {basePos[0], basePos[1], basePos[2]};
        int[] end = {basePos[0], basePos[1] + height, basePos[2]};
        
        placeBlockLine(level, start, end);
        
        if (trunkSize == 2) {
            // 2x2 trunk
            start[0]++;
            end[0]++;
            placeBlockLine(level, start, end);
            
            start[2]++;
            end[2]++;
            placeBlockLine(level, start, end);
            
            start[0]--;
            end[0]--;
            placeBlockLine(level, start, end);
        }
    }
    
    private void generateLeafNodeBases(WorldGenLevel level) {
        int[] branchBase = {basePos[0], basePos[1], basePos[2]};
        
        for (int[] node : leafNodes) {
            int[] leafPos = {node[0], node[1], node[2]};
            branchBase[1] = node[3];
            int distFromBase = branchBase[1] - basePos[1];
            
            if (leafNodeNeedsBase(distFromBase)) {
                placeBlockLine(level, branchBase, leafPos);
            }
        }
    }
    
    private boolean leafNodeNeedsBase(int distFromBase) {
        return distFromBase >= heightLimit * 0.2;
    }
    
    private void placeBlockLine(WorldGenLevel level, int[] from, int[] to) {
        int[] delta = new int[3];
        byte mainAxis = 0;
        
        for (byte i = 0; i < 3; i++) {
            delta[i] = to[i] - from[i];
            if (Math.abs(delta[i]) > Math.abs(delta[mainAxis])) {
                mainAxis = i;
            }
        }
        
        if (delta[mainAxis] == 0) {
            return;
        }
        
        byte coord1 = OTHER_COORD_PAIRS[mainAxis];
        byte coord2 = OTHER_COORD_PAIRS[mainAxis + 3];
        byte direction = (byte) (delta[mainAxis] > 0 ? 1 : -1);
        double slope1 = delta[coord1] / (double) delta[mainAxis];
        double slope2 = delta[coord2] / (double) delta[mainAxis];
        
        int[] pos = new int[3];
        
        for (int i = 0; i != delta[mainAxis] + direction; i += direction) {
            pos[mainAxis] = Mth.floor(from[mainAxis] + i + 0.5);
            pos[coord1] = Mth.floor(from[coord1] + i * slope1 + 0.5);
            pos[coord2] = Mth.floor(from[coord2] + i * slope2 + 0.5);
            
            BlockPos blockPos = new BlockPos(pos[0], pos[1], pos[2]);
            if (isReplaceable(level, blockPos)) {
                level.setBlock(blockPos, ModBlocks.GREATWOOD_LOG.get().defaultBlockState(), 2);
            }
        }
    }
    
    private int checkBlockLine(int[] from, int[] to) {
        // Stub for air check - returns -1 if path is clear
        return -1;
    }
    
    private boolean isValidTreeLocation(WorldGenLevel level, int offsetX, int offsetZ) {
        BlockPos groundPos = new BlockPos(basePos[0] + offsetX, basePos[1] - 1, basePos[2] + offsetZ);
        BlockState groundState = level.getBlockState(groundPos);
        
        // Check if ground can sustain plant
        if (!groundState.is(BlockTags.DIRT) && !groundState.is(Blocks.GRASS_BLOCK)) {
            return false;
        }
        
        // Check vertical clearance
        for (int y = basePos[1]; y < basePos[1] + heightLimit; y++) {
            BlockPos checkPos = new BlockPos(basePos[0] + offsetX, y, basePos[2] + offsetZ);
            BlockState state = level.getBlockState(checkPos);
            if (!state.isAir() && !state.is(BlockTags.LEAVES) && !state.canBeReplaced()) {
                if (y - basePos[1] < 6) {
                    return false;
                }
                heightLimit = y - basePos[1];
                break;
            }
        }
        
        return true;
    }
    
    private boolean isReplaceable(LevelAccessor level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.is(BlockTags.LEAVES) || state.canBeReplaced();
    }
    
    private void generateSpiderNest(WorldGenLevel level, RandomSource random, BlockPos treeBase) {
        // Place spawner below tree
        BlockPos spawnerPos = treeBase.below();
        level.setBlock(spawnerPos, Blocks.SPAWNER.defaultBlockState(), 2);
        
        if (level.getBlockEntity(spawnerPos) instanceof SpawnerBlockEntity spawner) {
            spawner.setEntityId(EntityType.CAVE_SPIDER, random);
        }
        
        // Add cobwebs around the tree
        for (int i = 0; i < 50; i++) {
            int wx = treeBase.getX() - 7 + random.nextInt(14);
            int wy = treeBase.getY() + random.nextInt(10);
            int wz = treeBase.getZ() - 7 + random.nextInt(14);
            BlockPos webPos = new BlockPos(wx, wy, wz);
            
            if (level.getBlockState(webPos).isAir() && isTouchingTreeBlock(level, webPos)) {
                level.setBlock(webPos, Blocks.COBWEB.defaultBlockState(), 2);
            }
        }
        
        // Place loot chest
        BlockPos chestPos = treeBase.below(2);
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
        
        if (level.getBlockEntity(chestPos) instanceof RandomizableContainerBlockEntity chest) {
            chest.setLootTable(BuiltInLootTables.SIMPLE_DUNGEON, random.nextLong());
        }
    }
    
    private boolean isTouchingTreeBlock(WorldGenLevel level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockState state = level.getBlockState(pos.relative(dir));
            if (state.is(ModBlocks.GREATWOOD_LOG.get()) || state.is(ModBlocks.GREATWOOD_LEAVES.get())) {
                return true;
            }
        }
        return false;
    }
}




