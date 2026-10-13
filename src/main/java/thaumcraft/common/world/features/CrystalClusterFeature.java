package thaumcraft.common.world.features;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.world.ore.BlockCrystalTC;
import thaumcraft.init.ModBlocks;

/**
 * CrystalClusterFeature - Generates vis crystal clusters on cave walls.
 * 
 * Crystals spawn in air blocks that are adjacent to stone.
 * This creates a natural cave crystal effect similar to amethyst geodes
 * but scattered throughout underground caves.
 */
public class CrystalClusterFeature extends Feature<NoneFeatureConfiguration> {
    
    public enum CrystalType {
        AIR(ModBlocks.CRYSTAL_AIR),
        FIRE(ModBlocks.CRYSTAL_FIRE),
        WATER(ModBlocks.CRYSTAL_WATER),
        EARTH(ModBlocks.CRYSTAL_EARTH),
        ORDER(ModBlocks.CRYSTAL_ORDER),
        ENTROPY(ModBlocks.CRYSTAL_ENTROPY);
        
        private final java.util.function.Supplier<Block> blockSupplier;
        
        CrystalType(net.neoforged.neoforge.registries.DeferredHolder<Block, Block> block) {
            this.blockSupplier = block::get;
        }
        
        public Block getBlock() {
            return blockSupplier.get();
        }
    }
    
    private final CrystalType crystalType;
    
    public CrystalClusterFeature(Codec<NoneFeatureConfiguration> codec) {
        this(codec, CrystalType.AIR);
    }
    
    public CrystalClusterFeature(Codec<NoneFeatureConfiguration> codec, CrystalType type) {
        super(codec);
        this.crystalType = type;
    }
    
    // Debug logging counter to avoid spam - only log every N calls
    private static int placeCallCount = 0;
    private static final int LOG_INTERVAL = 100;
    
    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        
        placeCallCount++;
        boolean shouldLog = (placeCallCount % LOG_INTERVAL == 1);
        
        if (shouldLog) {
            Thaumcraft.LOGGER.info("[CrystalCluster] place() called for {} at {} (call #{})", 
                crystalType.name(), origin, placeCallCount);
        }
        
        // Search for a valid cave position nearby
        BlockPos cavePos = findCavePosition(level, origin, random);
        if (cavePos == null) {
            if (shouldLog) {
                Thaumcraft.LOGGER.debug("[CrystalCluster] No cave position found near {}", origin);
            }
            return false;
        }
        
        // Try to place a cluster of crystals
        int placed = 0;
        int attempts = 12 + random.nextInt(12);
        
        for (int i = 0; i < attempts; i++) {
            BlockPos checkPos = cavePos.offset(
                random.nextInt(5) - 2,
                random.nextInt(5) - 2,
                random.nextInt(5) - 2
            );
            
            if (tryPlaceCrystal(level, checkPos, random)) {
                placed++;
            }
        }
        
        if (placed > 0) {
            Thaumcraft.LOGGER.info("[CrystalCluster] Placed {} {} crystals near {}", 
                placed, crystalType.name(), cavePos);
        }
        
        return placed > 0;
    }
    
    /**
     * Find an air pocket underground where we can place crystals.
     */
    private BlockPos findCavePosition(WorldGenLevel level, BlockPos origin, RandomSource random) {
        // Search in a small area for air blocks adjacent to stone
        for (int attempt = 0; attempt < 16; attempt++) {
            BlockPos checkPos = origin.offset(
                random.nextInt(8) - 4,
                random.nextInt(8) - 4,
                random.nextInt(8) - 4
            );
            
            BlockState state = level.getBlockState(checkPos);
            if (state.isAir() || state.is(Blocks.CAVE_AIR)) {
                // Check if any adjacent block is stone
                for (Direction dir : Direction.values()) {
                    BlockState adjacent = level.getBlockState(checkPos.relative(dir));
                    if (isStone(adjacent)) {
                        return checkPos;
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Try to place a crystal at the given position.
     */
    private boolean tryPlaceCrystal(WorldGenLevel level, BlockPos pos, RandomSource random) {
        BlockState currentState = level.getBlockState(pos);
        
        // Must be air or cave air
        if (!currentState.isAir() && !currentState.is(Blocks.CAVE_AIR)) {
            return false;
        }
        
        // Find a stone face to attach to
        boolean hasStoneAdjacent = false;
        for (Direction dir : Direction.values()) {
            BlockState adjacent = level.getBlockState(pos.relative(dir));
            if (isStone(adjacent)) {
                hasStoneAdjacent = true;
                break;
            }
        }
        
        if (!hasStoneAdjacent) {
            return false;
        }
        
        // Get crystal block and place it
        Block crystalBlock = crystalType.getBlock();
        if (crystalBlock instanceof BlockCrystalTC crystal) {
            // Random size (0-2) and generation
            int size = random.nextInt(3);
            int generation = 1 + random.nextInt(3);
            
            BlockState crystalState = crystal.defaultBlockState()
                .setValue(BlockCrystalTC.SIZE, size)
                .setValue(BlockCrystalTC.GENERATION, generation);
            
            level.setBlock(pos, crystalState, 2);
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if a block is stone-like (valid attachment surface for crystals).
     */
    public static boolean isStone(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD) ||
               state.is(BlockTags.BASE_STONE_NETHER) ||
               state.is(Blocks.DEEPSLATE) ||
               state.is(Blocks.STONE) ||
               state.is(Blocks.GRANITE) ||
               state.is(Blocks.DIORITE) ||
               state.is(Blocks.ANDESITE) ||
               state.is(Blocks.TUFF) ||
               state.is(Blocks.CALCITE);
    }
}





