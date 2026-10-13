package thaumcraft.common.world.features;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import thaumcraft.init.ModBlocks;

import java.util.function.Supplier;

/**
 * Generates clusters of Thaumcraft plants in the world.
 * 
 * Used for:
 * - Cinderpearl (desert plant)
 * - Shimmerleaf (near silverwood)
 * - Vishroom (cave mushroom)
 */
public class ThaumcraftPlantFeature extends Feature<NoneFeatureConfiguration> {
    
    public enum PlantType {
        CINDERPEARL(() -> ModBlocks.CINDERPEARL.get(), true, false, false),
        SHIMMERLEAF(() -> ModBlocks.SHIMMERLEAF.get(), false, false, false),
        VISHROOM(() -> ModBlocks.VISHROOM.get(), false, true, false),
        TAINT_WEED(() -> ModBlocks.TAINT_WEED.get(), false, false, true);
        
        private final Supplier<Block> blockSupplier;
        private final boolean requiresSand;
        private final boolean requiresCave;
        private final boolean requiresTaint;
        
        PlantType(Supplier<Block> blockSupplier, boolean requiresSand, boolean requiresCave, boolean requiresTaint) {
            this.blockSupplier = blockSupplier;
            this.requiresSand = requiresSand;
            this.requiresCave = requiresCave;
            this.requiresTaint = requiresTaint;
        }
        
        public Block getBlock() {
            return blockSupplier.get();
        }
        
        public boolean requiresSand() {
            return requiresSand;
        }
        
        public boolean requiresCave() {
            return requiresCave;
        }
        
        public boolean requiresTaint() {
            return requiresTaint;
        }
    }
    
    private final PlantType plantType;
    
    public ThaumcraftPlantFeature(Codec<NoneFeatureConfiguration> codec, PlantType plantType) {
        super(codec);
        this.plantType = plantType;
    }
    
    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        
        return generatePlantCluster(level, random, origin, plantType);
    }
    
    public static boolean generatePlantCluster(WorldGenLevel level, RandomSource random, 
            BlockPos origin, PlantType type) {
        
        int placed = 0;
        
        for (int i = 0; i < 18; i++) {
            int x = origin.getX() + random.nextInt(8) - random.nextInt(8);
            int y = origin.getY() + random.nextInt(4) - random.nextInt(4);
            int z = origin.getZ() + random.nextInt(8) - random.nextInt(8);
            
            BlockPos pos = new BlockPos(x, y, z);
            BlockPos groundPos = pos.below();
            
            if (!level.getBlockState(pos).isAir()) {
                continue;
            }
            
            BlockState groundState = level.getBlockState(groundPos);
            
            // Check placement requirements
            if (type.requiresSand()) {
                // Cinderpearl - needs sand
                if (!groundState.is(Blocks.SAND) && !groundState.is(Blocks.RED_SAND)) {
                    continue;
                }
            } else if (type.requiresTaint()) {
                // Taint weed - needs tainted ground
                if (ModBlocks.TAINT_SOIL != null && groundState.is(ModBlocks.TAINT_SOIL.get())) {
                    // valid
                } else if (ModBlocks.TAINT_ROCK != null && groundState.is(ModBlocks.TAINT_ROCK.get())) {
                    // valid
                } else if (ModBlocks.TAINT_CRUST != null && groundState.is(ModBlocks.TAINT_CRUST.get())) {
                    // valid
                } else if (ModBlocks.TAINT_LOG != null && groundState.is(ModBlocks.TAINT_LOG.get())) {
                    // valid
                } else {
                    continue;
                }
            } else if (type.requiresCave()) {
                // Vishroom - needs cave conditions (low light, stone/dirt ground)
                if (!groundState.is(BlockTags.BASE_STONE_OVERWORLD) && 
                    !groundState.is(BlockTags.DIRT) &&
                    !groundState.is(Blocks.GRAVEL)) {
                    continue;
                }
                // Check for cave (has a ceiling)
                if (!hasCeiling(level, pos)) {
                    continue;
                }
            } else {
                // Shimmerleaf - needs grass or dirt
                if (!groundState.is(Blocks.GRASS_BLOCK) && !groundState.is(BlockTags.DIRT)) {
                    continue;
                }
            }
            
            level.setBlock(pos, type.getBlock().defaultBlockState(), 2);
            placed++;
        }
        
        return placed > 0;
    }
    
    /**
     * Check if there's a ceiling above (cave detection)
     */
    private static boolean hasCeiling(WorldGenLevel level, BlockPos pos) {
        for (int y = pos.getY() + 1; y < pos.getY() + 32 && y < level.getMaxBuildHeight(); y++) {
            BlockState state = level.getBlockState(new BlockPos(pos.getX(), y, pos.getZ()));
            if (!state.isAir() && !state.canBeReplaced()) {
                return true;
            }
        }
        return false;
    }
}




