package thaumcraft.common.world.features;

import com.google.common.collect.Lists;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import thaumcraft.init.ModBlocks;

import java.util.List;

/**
 * Generates Big Magic Trees - massive magical trees found in magical forest biomes.
 * 
 * Big Magic Trees are characterized by:
 * - Very tall height (11-22 blocks)
 * - Thick, often multi-block trunk with roots
 * - Sprawling branch structure
 * - Dense spherical leaf canopy
 * - Can use either Greatwood or Silverwood blocks based on configuration
 * 
 * This is the "fancy" tree variant for magical biomes, similar to vanilla's
 * big oak trees but with magical wood types.
 */
public class BigMagicTreeFeature extends Feature<NoneFeatureConfiguration> {

    // Coordinate index mapping for axis calculations
    private static final byte[] OTHER_COORD_PAIRS = {2, 0, 0, 1, 2, 1};
    
    // Tree type enum
    public enum TreeType {
        GREATWOOD,
        SILVERWOOD
    }
    
    // Tree generation parameters
    private RandomSource rand;
    private WorldGenLevel level;
    private BlockPos basePos;
    private int heightLimit;
    private int height;
    private double heightAttenuation = 0.618;
    private double branchSlope = 0.381;
    private double scaleWidth = 1.25;
    private double leafDensity = 0.9;
    private int trunkSize = 1;
    private int heightLimitLimit = 11;
    private int leafDistanceLimit = 4;
    private List<FoliageCoordinates> foliageCoords;
    
    private TreeType treeType = TreeType.GREATWOOD;
    
    public BigMagicTreeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }
    
    /**
     * Create a BigMagicTreeFeature with a specific tree type.
     */
    public BigMagicTreeFeature(Codec<NoneFeatureConfiguration> codec, TreeType type) {
        super(codec);
        this.treeType = type;
    }
    
    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos pos = context.origin();
        RandomSource random = context.random();
        
        return generate(level, random, pos);
    }
    
    public boolean generate(WorldGenLevel worldIn, RandomSource randomIn, BlockPos position) {
        this.level = worldIn;
        this.basePos = position;
        this.rand = randomIn;
        this.heightLimit = 0;
        
        if (heightLimit == 0) {
            heightLimit = heightLimitLimit + rand.nextInt(heightLimitLimit);
        }
        
        if (!validTreeLocation()) {
            level = null;
            return false;
        }
        
        generateLeafNodeList();
        generateLeaves();
        generateTrunk();
        generateLeafNodeBases();
        
        level = null;
        return true;
    }
    
    /**
     * Generate the list of positions where leaf clusters will be placed.
     */
    private void generateLeafNodeList() {
        height = (int) (heightLimit * heightAttenuation);
        if (height >= heightLimit) {
            height = heightLimit - 1;
        }
        
        int numLeafGroups = (int) (1.382 + Math.pow(leafDensity * heightLimit / 13.0, 2.0));
        if (numLeafGroups < 1) {
            numLeafGroups = 1;
        }
        
        int leafY = basePos.getY() + heightLimit - leafDistanceLimit;
        int branchStartY = basePos.getY() + height;
        int layerIndex = heightLimit - leafDistanceLimit;
        
        foliageCoords = Lists.newArrayList();
        foliageCoords.add(new FoliageCoordinates(basePos.above(layerIndex), branchStartY));
        
        while (layerIndex >= 0) {
            float layerSize = layerSize(layerIndex);
            
            if (layerSize >= 0.0f) {
                for (int i = 0; i < numLeafGroups; i++) {
                    double radius = scaleWidth * layerSize * (rand.nextFloat() + 0.328);
                    double angle = rand.nextFloat() * 2.0 * Math.PI;
                    double offsetX = radius * Math.sin(angle) + 0.5;
                    double offsetZ = radius * Math.cos(angle) + 0.5;
                    
                    BlockPos nodePos = basePos.offset((int) offsetX, layerIndex - 1, (int) offsetZ);
                    BlockPos checkPos = nodePos.above(leafDistanceLimit);
                    
                    if (checkBlockLine(nodePos, checkPos) == -1) {
                        int distX = basePos.getX() - nodePos.getX();
                        int distZ = basePos.getZ() - nodePos.getZ();
                        double dist = Math.sqrt(distX * distX + distZ * distZ);
                        double slopeHeight = nodePos.getY() - dist * branchSlope;
                        
                        int branchBaseY = (slopeHeight > branchStartY) ? branchStartY : (int) slopeHeight;
                        BlockPos branchBase = new BlockPos(basePos.getX(), branchBaseY, basePos.getZ());
                        
                        if (checkBlockLine(branchBase, nodePos) == -1) {
                            foliageCoords.add(new FoliageCoordinates(nodePos, branchBaseY));
                        }
                    }
                }
            }
            
            layerIndex--;
        }
    }
    
    /**
     * Generate a cross-section of leaves at a given position with radius.
     */
    private void crossSection(BlockPos center, float radius) {
        int intRadius = (int) (radius + 0.618);
        
        for (int dx = -intRadius; dx <= intRadius; dx++) {
            for (int dz = -intRadius; dz <= intRadius; dz++) {
                double dist = Math.pow(Math.abs(dx) + 0.5, 2.0) + Math.pow(Math.abs(dz) + 0.5, 2.0);
                
                if (dist <= radius * radius) {
                    BlockPos leafPos = center.offset(dx, 0, dz);
                    BlockState state = level.getBlockState(leafPos);
                    
                    if (state.isAir() || state.is(BlockTags.LEAVES)) {
                        level.setBlock(leafPos, getLeafBlock(), 2);
                    }
                }
            }
        }
    }
    
    /**
     * Calculate the size of a leaf layer at a given height.
     */
    private float layerSize(int layer) {
        if (layer < (float) heightLimit * 0.3f) {
            return -1.0f;
        }
        
        float halfHeight = heightLimit / 2.0f;
        float distFromMiddle = halfHeight - layer;
        float size;
        
        if (distFromMiddle == 0.0f) {
            size = halfHeight;
        } else if (Math.abs(distFromMiddle) >= halfHeight) {
            return 0.0f;
        } else {
            size = (float) Math.sqrt(halfHeight * halfHeight - distFromMiddle * distFromMiddle);
        }
        
        return size * 0.5f;
    }
    
    /**
     * Calculate the leaf radius for a given layer within a leaf node.
     */
    private float leafSize(int layer) {
        if (layer >= 0 && layer < leafDistanceLimit) {
            return (layer != 0 && layer != leafDistanceLimit - 1) ? 3.0f : 2.0f;
        }
        return -1.0f;
    }
    
    /**
     * Generate leaves at a single foliage node.
     */
    private void generateLeafNode(BlockPos pos) {
        for (int i = 0; i < leafDistanceLimit; i++) {
            crossSection(pos.above(i), leafSize(i));
        }
    }
    
    /**
     * Place a line of log blocks between two positions.
     */
    private void limb(BlockPos from, BlockPos to) {
        BlockPos delta = to.subtract(from);
        int maxDist = getGreatestDistance(delta);
        
        float stepX = delta.getX() / (float) maxDist;
        float stepY = delta.getY() / (float) maxDist;
        float stepZ = delta.getZ() / (float) maxDist;
        
        for (int i = 0; i <= maxDist; i++) {
            BlockPos logPos = from.offset(
                    Mth.floor(0.5f + i * stepX),
                    Mth.floor(0.5f + i * stepY),
                    Mth.floor(0.5f + i * stepZ)
            );
            
            Direction.Axis axis = getLogAxis(from, logPos);
            BlockState logState = getLogBlock(axis);
            
            if (isReplaceable(level, logPos)) {
                level.setBlock(logPos, logState, 2);
            }
        }
    }
    
    /**
     * Find the greatest absolute distance along any axis.
     */
    private int getGreatestDistance(BlockPos pos) {
        int x = Mth.abs(pos.getX());
        int y = Mth.abs(pos.getY());
        int z = Mth.abs(pos.getZ());
        return Math.max(z, Math.max(x, y));
    }
    
    /**
     * Determine the appropriate log axis based on direction between positions.
     */
    private Direction.Axis getLogAxis(BlockPos from, BlockPos to) {
        Direction.Axis axis = Direction.Axis.Y;
        int dx = Math.abs(to.getX() - from.getX());
        int dz = Math.abs(to.getZ() - from.getZ());
        int maxHoriz = Math.max(dx, dz);
        
        if (maxHoriz > 0) {
            if (dx == maxHoriz) {
                axis = Direction.Axis.X;
            } else if (dz == maxHoriz) {
                axis = Direction.Axis.Z;
            }
        }
        
        return axis;
    }
    
    /**
     * Generate all leaf nodes.
     */
    private void generateLeaves() {
        for (FoliageCoordinates coord : foliageCoords) {
            generateLeafNode(coord.pos);
        }
    }
    
    /**
     * Check if a leaf node needs a branch connecting it to the trunk.
     */
    private boolean leafNodeNeedsBase(int distFromBase) {
        return distFromBase >= heightLimit * 0.2;
    }
    
    /**
     * Generate the main trunk of the tree.
     */
    private void generateTrunk() {
        BlockPos trunkTop = basePos.above(height);
        
        limb(basePos, trunkTop);
        
        // For 2x2 trunk (larger trees)
        if (trunkSize == 2) {
            limb(basePos.east(), trunkTop.east());
            limb(basePos.east().south(), trunkTop.east().south());
            limb(basePos.south(), trunkTop.south());
        }
    }
    
    /**
     * Generate branches connecting leaf nodes to the trunk.
     */
    private void generateLeafNodeBases() {
        for (FoliageCoordinates coord : foliageCoords) {
            int branchBaseY = coord.branchBase;
            BlockPos branchBase = new BlockPos(basePos.getX(), branchBaseY, basePos.getZ());
            
            if (leafNodeNeedsBase(branchBaseY - basePos.getY())) {
                limb(branchBase, coord.pos);
            }
        }
    }
    
    /**
     * Check if a line between two positions is clear for placement.
     * Returns -1 if clear, otherwise returns the distance at which obstruction was found.
     */
    private int checkBlockLine(BlockPos from, BlockPos to) {
        BlockPos delta = to.subtract(from);
        int maxDist = getGreatestDistance(delta);
        
        if (maxDist == 0) {
            return -1;
        }
        
        float stepX = delta.getX() / (float) maxDist;
        float stepY = delta.getY() / (float) maxDist;
        float stepZ = delta.getZ() / (float) maxDist;
        
        for (int i = 0; i <= maxDist; i++) {
            BlockPos checkPos = from.offset(
                    Mth.floor(0.5f + i * stepX),
                    Mth.floor(0.5f + i * stepY),
                    Mth.floor(0.5f + i * stepZ)
            );
            
            if (!isReplaceable(level, checkPos)) {
                return i;
            }
        }
        
        return -1;
    }
    
    /**
     * Check if the tree location is valid.
     */
    private boolean validTreeLocation() {
        BlockPos groundPos = basePos.below();
        BlockState groundState = level.getBlockState(groundPos);
        
        // Check if ground can sustain plant
        if (!groundState.is(BlockTags.DIRT) && !groundState.is(Blocks.GRASS_BLOCK)) {
            return false;
        }
        
        // Check vertical clearance
        int checkResult = checkBlockLine(basePos, basePos.above(heightLimit - 1));
        
        if (checkResult == -1) {
            return true;
        }
        
        if (checkResult < 6) {
            return false;
        }
        
        // Reduce height to fit available space
        heightLimit = checkResult;
        return true;
    }
    
    /**
     * Check if a block can be replaced during tree generation.
     */
    private boolean isReplaceable(LevelAccessor level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.is(BlockTags.LEAVES) || state.canBeReplaced() ||
               state.is(BlockTags.LOGS) || state.is(BlockTags.SAPLINGS);
    }
    
    /**
     * Get the appropriate log block based on tree type and axis.
     */
    private BlockState getLogBlock(Direction.Axis axis) {
        BlockState log;
        
        if (treeType == TreeType.SILVERWOOD) {
            log = ModBlocks.SILVERWOOD_LOG.get().defaultBlockState();
        } else {
            log = ModBlocks.GREATWOOD_LOG.get().defaultBlockState();
        }
        
        // Apply axis if the block has the AXIS property
        if (log.hasProperty(BlockStateProperties.AXIS)) {
            log = log.setValue(BlockStateProperties.AXIS, axis);
        }
        
        return log;
    }
    
    /**
     * Get the appropriate leaf block based on tree type.
     */
    private BlockState getLeafBlock() {
        if (treeType == TreeType.SILVERWOOD) {
            return ModBlocks.SILVERWOOD_LEAVES.get().defaultBlockState();
        } else {
            return ModBlocks.GREATWOOD_LEAVES.get().defaultBlockState();
        }
    }
    
    /**
     * Set the tree type for this feature instance.
     */
    public BigMagicTreeFeature withTreeType(TreeType type) {
        this.treeType = type;
        return this;
    }
    
    /**
     * Configure for larger trees (2x2 trunk).
     */
    public BigMagicTreeFeature withLargeTrunk() {
        this.trunkSize = 2;
        return this;
    }
    
    /**
     * Configure height limits.
     */
    public BigMagicTreeFeature withHeightLimit(int limit) {
        this.heightLimitLimit = limit;
        return this;
    }
    
    /**
     * Inner class to store foliage node position and branch base height.
     */
    private static class FoliageCoordinates {
        final BlockPos pos;
        final int branchBase;
        
        FoliageCoordinates(BlockPos pos, int branchBase) {
            this.pos = pos;
            this.branchBase = branchBase;
        }
    }
}




