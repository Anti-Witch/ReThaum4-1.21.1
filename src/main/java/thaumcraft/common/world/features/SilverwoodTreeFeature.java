package thaumcraft.common.world.features;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
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
import thaumcraft.init.ModBlocks;

/**
 * Generates Silverwood trees - magical pale trees with special properties.
 * 
 * Silverwood trees are characterized by:
 * - Plus-shaped trunk (cross pattern)
 * - Height of 7-11 blocks
 * - Spherical leaf canopy
 * - Pale silver-white coloring
 * - Shimmerleaf flowers spawn around them during worldgen
 */
public class SilverwoodTreeFeature extends Feature<NoneFeatureConfiguration> {

    private static final int MIN_HEIGHT = 7;
    private static final int RANDOM_HEIGHT = 4;
    
    public SilverwoodTreeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }
    
    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos pos = context.origin();
        RandomSource random = context.random();
        
        return generateTree(level, random, pos, true);
    }
    
    public boolean generateTree(WorldGenLevel level, RandomSource random, BlockPos pos, boolean worldGen) {
        int height = random.nextInt(RANDOM_HEIGHT) + MIN_HEIGHT;
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        
        // Check world bounds
        if (y < 1 || y + height + 1 > level.getMaxBuildHeight()) {
            return false;
        }
        
        // Check for space
        if (!hasSpaceForTree(level, x, y, z, height)) {
            return false;
        }
        
        // Check ground
        BlockState groundState = level.getBlockState(new BlockPos(x, y - 1, z));
        if (!canSustainTree(groundState)) {
            return false;
        }
        
        if (y >= level.getMaxBuildHeight() - height - 1) {
            return false;
        }
        
        // Generate tree
        generateLeafCanopy(level, random, x, y, z, height);
        generateTrunk(level, random, x, y, z, height);

        // TC6: silverwood trees always contain a small pure node inside the trunk
        if (worldGen && height >= 4) {
            int nodeY = y + 1 + random.nextInt(height - 3);
            placeNodeInTrunk(level, random, new BlockPos(x, nodeY, z));
        }

        // Generate shimmerleaf around the tree during worldgen
        if (worldGen) {
            generateShimmerleaf(level, random, pos);
        }

        return true;
    }

    /**
     * Replaces a trunk log with a small pure aura node (TC6 behaviour).
     */
    private void placeNodeInTrunk(WorldGenLevel level, RandomSource random, BlockPos pos) {
        level.setBlock(pos, thaumcraft.init.ModBlocks.NODE.get().defaultBlockState(), 2);
        if (level.getBlockEntity(pos) instanceof thaumcraft.common.tiles.TileNode node) {
            thaumcraft.api.aspects.AspectList al = new thaumcraft.api.aspects.AspectList();
            al.add(thaumcraft.api.aspects.Aspect.ORDER, 10 + random.nextInt(10));
            al.add(thaumcraft.api.aspects.Aspect.AIR, 10 + random.nextInt(10));
            if (random.nextBoolean()) {
                al.add(thaumcraft.api.aspects.Aspect.WATER, 5 + random.nextInt(10));
            } else {
                al.add(thaumcraft.api.aspects.Aspect.EARTH, 5 + random.nextInt(10));
            }
            node.setNodeType(thaumcraft.api.nodes.NodeType.PURE);
            node.setNodeModifier(null);
            node.setAspects(al);
        }
    }
    
    private boolean hasSpaceForTree(WorldGenLevel level, int x, int y, int z, int height) {
        for (int cy = y; cy <= y + 1 + height; cy++) {
            int spread = 1;
            if (cy == y) {
                spread = 0;
            }
            if (cy >= y + 1 + height - 2) {
                spread = 3;
            }
            
            for (int cx = x - spread; cx <= x + spread; cx++) {
                for (int cz = z - spread; cz <= z + spread; cz++) {
                    if (cy < 0 || cy >= level.getMaxBuildHeight()) {
                        return false;
                    }
                    
                    BlockPos checkPos = new BlockPos(cx, cy, cz);
                    BlockState state = level.getBlockState(checkPos);
                    
                    if (!state.isAir() && !state.is(BlockTags.LEAVES) && !state.canBeReplaced()) {
                        if (cy > y) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }
    
    private boolean canSustainTree(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(Blocks.GRASS_BLOCK);
    }
    
    private void generateLeafCanopy(WorldGenLevel level, RandomSource random, int x, int y, int z, int height) {
        int leafStart = y + height - 5;
        int leafEnd = y + height + 3 + random.nextInt(3);
        
        for (int ly = leafStart; ly <= leafEnd; ly++) {
            int centerY = Mth.clamp(ly, y + height - 3, y + height);
            
            for (int lx = x - 5; lx <= x + 5; lx++) {
                for (int lz = z - 5; lz <= z + 5; lz++) {
                    double dx = lx - x;
                    double dy = ly - centerY;
                    double dz = lz - z;
                    double dist = dx * dx + dy * dy + dz * dz;
                    
                    BlockPos leafPos = new BlockPos(lx, ly, lz);
                    BlockState state = level.getBlockState(leafPos);
                    
                    if (dist < 10 + random.nextInt(8) && canPlaceLeaf(level, state, leafPos)) {
                        setBlockSafe(level, leafPos, ModBlocks.SILVERWOOD_LEAVES.get().defaultBlockState());
                    }
                }
            }
        }
    }
    
    private void generateTrunk(WorldGenLevel level, RandomSource random, int x, int y, int z, int height) {
        // Main trunk
        for (int ty = 0; ty < height; ty++) {
            BlockPos pos = new BlockPos(x, y + ty, z);
            BlockState state = level.getBlockState(pos);
            
            if (isReplaceable(level, state, pos)) {
                // Center column and plus-shape
                setBlockSafe(level, pos, ModBlocks.SILVERWOOD_LOG.get().defaultBlockState());
                setBlockSafe(level, new BlockPos(x - 1, y + ty, z), ModBlocks.SILVERWOOD_LOG.get().defaultBlockState());
                setBlockSafe(level, new BlockPos(x + 1, y + ty, z), ModBlocks.SILVERWOOD_LOG.get().defaultBlockState());
                setBlockSafe(level, new BlockPos(x, y + ty, z - 1), ModBlocks.SILVERWOOD_LOG.get().defaultBlockState());
                setBlockSafe(level, new BlockPos(x, y + ty, z + 1), ModBlocks.SILVERWOOD_LOG.get().defaultBlockState());
            }
        }
        
        // Top of trunk
        setBlockSafe(level, new BlockPos(x, y + height, z), ModBlocks.SILVERWOOD_LOG.get().defaultBlockState());
        
        // Base corner extensions
        setBlockSafe(level, new BlockPos(x - 1, y, z - 1), ModBlocks.SILVERWOOD_LOG.get().defaultBlockState());
        setBlockSafe(level, new BlockPos(x + 1, y, z + 1), ModBlocks.SILVERWOOD_LOG.get().defaultBlockState());
        setBlockSafe(level, new BlockPos(x - 1, y, z + 1), ModBlocks.SILVERWOOD_LOG.get().defaultBlockState());
        setBlockSafe(level, new BlockPos(x + 1, y, z - 1), ModBlocks.SILVERWOOD_LOG.get().defaultBlockState());
        
        // Random corner extensions at y+1
        if (random.nextInt(3) != 0) {
            setBlockSafe(level, new BlockPos(x - 1, y + 1, z - 1), ModBlocks.SILVERWOOD_LOG.get().defaultBlockState());
        }
        if (random.nextInt(3) != 0) {
            setBlockSafe(level, new BlockPos(x + 1, y + 1, z + 1), ModBlocks.SILVERWOOD_LOG.get().defaultBlockState());
        }
        if (random.nextInt(3) != 0) {
            setBlockSafe(level, new BlockPos(x - 1, y + 1, z + 1), ModBlocks.SILVERWOOD_LOG.get().defaultBlockState());
        }
        if (random.nextInt(3) != 0) {
            setBlockSafe(level, new BlockPos(x + 1, y + 1, z - 1), ModBlocks.SILVERWOOD_LOG.get().defaultBlockState());
        }
        
        // Root extensions (horizontal)
        setBlockSafe(level, new BlockPos(x - 2, y, z), ModBlocks.SILVERWOOD_LOG.get().defaultBlockState());
        setBlockSafe(level, new BlockPos(x + 2, y, z), ModBlocks.SILVERWOOD_LOG.get().defaultBlockState());
        setBlockSafe(level, new BlockPos(x, y, z - 2), ModBlocks.SILVERWOOD_LOG.get().defaultBlockState());
        setBlockSafe(level, new BlockPos(x, y, z + 2), ModBlocks.SILVERWOOD_LOG.get().defaultBlockState());
        
        // Root extensions (underground)
        setBlockSafe(level, new BlockPos(x - 2, y - 1, z), ModBlocks.SILVERWOOD_LOG.get().defaultBlockState());
        setBlockSafe(level, new BlockPos(x + 2, y - 1, z), ModBlocks.SILVERWOOD_LOG.get().defaultBlockState());
        setBlockSafe(level, new BlockPos(x, y - 1, z - 2), ModBlocks.SILVERWOOD_LOG.get().defaultBlockState());
        setBlockSafe(level, new BlockPos(x, y - 1, z + 2), ModBlocks.SILVERWOOD_LOG.get().defaultBlockState());
        
        // Upper branch flares
        int flareY = y + (height - 4);
        setBlockSafe(level, new BlockPos(x - 1, flareY, z - 1), ModBlocks.SILVERWOOD_LOG.get().defaultBlockState());
        setBlockSafe(level, new BlockPos(x + 1, flareY, z + 1), ModBlocks.SILVERWOOD_LOG.get().defaultBlockState());
        setBlockSafe(level, new BlockPos(x - 1, flareY, z + 1), ModBlocks.SILVERWOOD_LOG.get().defaultBlockState());
        setBlockSafe(level, new BlockPos(x + 1, flareY, z - 1), ModBlocks.SILVERWOOD_LOG.get().defaultBlockState());
        
        // Random branch flares at height-5
        int lowerFlareY = y + (height - 5);
        if (random.nextInt(3) == 0) {
            setBlockSafe(level, new BlockPos(x - 1, lowerFlareY, z - 1), ModBlocks.SILVERWOOD_LOG.get().defaultBlockState());
        }
        if (random.nextInt(3) == 0) {
            setBlockSafe(level, new BlockPos(x + 1, lowerFlareY, z + 1), ModBlocks.SILVERWOOD_LOG.get().defaultBlockState());
        }
        if (random.nextInt(3) == 0) {
            setBlockSafe(level, new BlockPos(x - 1, lowerFlareY, z + 1), ModBlocks.SILVERWOOD_LOG.get().defaultBlockState());
        }
        if (random.nextInt(3) == 0) {
            setBlockSafe(level, new BlockPos(x + 1, lowerFlareY, z - 1), ModBlocks.SILVERWOOD_LOG.get().defaultBlockState());
        }
        
        // Horizontal branch extensions at top
        setBlockSafe(level, new BlockPos(x - 2, flareY, z), ModBlocks.SILVERWOOD_LOG.get().defaultBlockState());
        setBlockSafe(level, new BlockPos(x + 2, flareY, z), ModBlocks.SILVERWOOD_LOG.get().defaultBlockState());
        setBlockSafe(level, new BlockPos(x, flareY, z - 2), ModBlocks.SILVERWOOD_LOG.get().defaultBlockState());
        setBlockSafe(level, new BlockPos(x, flareY, z + 2), ModBlocks.SILVERWOOD_LOG.get().defaultBlockState());
    }
    
    private void generateShimmerleaf(WorldGenLevel level, RandomSource random, BlockPos treePos) {
        // Scatter shimmerleaf plants around the tree
        for (int i = 0; i < 18; i++) {
            int fx = treePos.getX() + random.nextInt(8) - random.nextInt(8);
            int fy = treePos.getY() + random.nextInt(4) - random.nextInt(4);
            int fz = treePos.getZ() + random.nextInt(8) - random.nextInt(8);
            
            BlockPos flowerPos = new BlockPos(fx, fy, fz);
            BlockPos groundPos = flowerPos.below();
            
            if (level.getBlockState(flowerPos).isAir()) {
                BlockState ground = level.getBlockState(groundPos);
                if (ground.is(Blocks.GRASS_BLOCK) || ground.is(BlockTags.DIRT)) {
                    level.setBlock(flowerPos, ModBlocks.SHIMMERLEAF.get().defaultBlockState(), 2);
                }
            }
        }
    }
    
    private boolean canPlaceLeaf(LevelAccessor level, BlockState state, BlockPos pos) {
        return state.isAir() || state.is(BlockTags.LEAVES) || state.canBeReplaced();
    }
    
    private boolean isReplaceable(LevelAccessor level, BlockState state, BlockPos pos) {
        return state.isAir() || state.is(BlockTags.LEAVES) || state.canBeReplaced();
    }
    
    private void setBlockSafe(WorldGenLevel level, BlockPos pos, BlockState state) {
        BlockState existing = level.getBlockState(pos);
        if (existing.isAir() || existing.is(BlockTags.LEAVES) || existing.canBeReplaced()) {
            level.setBlock(pos, state, 2);
        }
    }
}




