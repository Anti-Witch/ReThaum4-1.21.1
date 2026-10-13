package thaumcraft.common.world.structures;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import thaumcraft.init.ModBlocks;

/**
 * AncientStoneCircleFeature - Generates mysterious stone circles/obelisks.
 * 
 * These structures are ancient monuments left by forgotten civilizations.
 * They appear as circles of standing stones or small obelisks made of
 * ancient stone and eldritch materials.
 * 
 * Variants:
 * - Small circle: 4-6 pillars in a ring
 * - Large circle: 8-12 pillars with central altar
 * - Single obelisk: Tall pillar with glyphed stone
 */
public class AncientStoneCircleFeature extends Feature<NoneFeatureConfiguration> {
    
    public AncientStoneCircleFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }
    
    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        
        // Find valid ground position
        BlockPos groundPos = findGroundPosition(level, origin);
        if (groundPos == null) {
            return false;
        }
        
        // Choose structure variant
        int variant = random.nextInt(10);
        
        if (variant < 4) {
            // 40% - Small stone circle
            return generateSmallCircle(level, groundPos, random);
        } else if (variant < 7) {
            // 30% - Single obelisk
            return generateObelisk(level, groundPos, random);
        } else {
            // 30% - Large stone circle with altar
            return generateLargeCircle(level, groundPos, random);
        }
    }
    
    /**
     * Finds a valid ground position for the structure.
     */
    private BlockPos findGroundPosition(WorldGenLevel level, BlockPos origin) {
        // Search downward for solid ground
        BlockPos pos = origin;
        for (int i = 0; i < 10; i++) {
            BlockState state = level.getBlockState(pos);
            BlockState below = level.getBlockState(pos.below());
            
            if (isAirOrReplaceable(state) && isValidGround(below)) {
                return pos;
            }
            pos = pos.below();
        }
        
        // Search upward if needed
        pos = origin;
        for (int i = 0; i < 10; i++) {
            BlockState state = level.getBlockState(pos);
            BlockState below = level.getBlockState(pos.below());
            
            if (isAirOrReplaceable(state) && isValidGround(below)) {
                return pos;
            }
            pos = pos.above();
        }
        
        return null;
    }
    
    private boolean isAirOrReplaceable(BlockState state) {
        return state.isAir() || state.canBeReplaced() || state.is(BlockTags.REPLACEABLE);
    }
    
    private boolean isValidGround(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(Blocks.GRASS_BLOCK) || 
               state.is(Blocks.STONE) || state.is(Blocks.SAND) ||
               state.is(BlockTags.BASE_STONE_OVERWORLD);
    }
    
    /**
     * Generates a small stone circle (4-6 pillars).
     */
    private boolean generateSmallCircle(WorldGenLevel level, BlockPos center, RandomSource random) {
        int numPillars = 4 + random.nextInt(3); // 4-6 pillars
        double radius = 3.0 + random.nextDouble() * 2.0; // 3-5 block radius
        
        for (int i = 0; i < numPillars; i++) {
            double angle = (2 * Math.PI * i) / numPillars + random.nextDouble() * 0.3;
            int px = center.getX() + (int) Math.round(Math.cos(angle) * radius);
            int pz = center.getZ() + (int) Math.round(Math.sin(angle) * radius);
            
            // Find ground at pillar position
            BlockPos pillarBase = findPillarBase(level, new BlockPos(px, center.getY(), pz));
            if (pillarBase != null) {
                int height = 2 + random.nextInt(3); // 2-4 blocks tall
                generatePillar(level, pillarBase, height, random);
            }
        }
        
        return true;
    }
    
    /**
     * Generates a single tall obelisk.
     */
    private boolean generateObelisk(WorldGenLevel level, BlockPos base, RandomSource random) {
        int height = 5 + random.nextInt(4); // 5-8 blocks tall
        
        // Base platform (2x2)
        for (int dx = -1; dx <= 0; dx++) {
            for (int dz = -1; dz <= 0; dz++) {
                BlockPos pos = base.offset(dx, -1, dz);
                setBlock(level, pos, getBaseBlock(random));
            }
        }
        
        // Main pillar
        for (int y = 0; y < height; y++) {
            BlockState pillarState;
            if (y == 0) {
                // Base is solid ancient stone
                pillarState = ModBlocks.ANCIENT_STONE.get().defaultBlockState();
            } else if (y == height - 1) {
                // Top has glyphed stone
                pillarState = ModBlocks.ANCIENT_STONE_GLYPHED.get().defaultBlockState();
            } else {
                // Middle sections are pillar blocks
                pillarState = getPillarBlock(random);
            }
            setBlock(level, base.above(y), pillarState);
        }
        
        // Add decorative stones at base
        if (random.nextBoolean()) {
            Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            setBlock(level, base.relative(dir), getBaseBlock(random));
        }
        
        return true;
    }
    
    /**
     * Generates a large stone circle with central altar.
     */
    private boolean generateLargeCircle(WorldGenLevel level, BlockPos center, RandomSource random) {
        int numPillars = 8 + random.nextInt(5); // 8-12 pillars
        double radius = 5.0 + random.nextDouble() * 3.0; // 5-8 block radius
        
        // Generate outer ring of pillars
        for (int i = 0; i < numPillars; i++) {
            double angle = (2 * Math.PI * i) / numPillars + random.nextDouble() * 0.2;
            int px = center.getX() + (int) Math.round(Math.cos(angle) * radius);
            int pz = center.getZ() + (int) Math.round(Math.sin(angle) * radius);
            
            BlockPos pillarBase = findPillarBase(level, new BlockPos(px, center.getY(), pz));
            if (pillarBase != null) {
                int height = 3 + random.nextInt(3); // 3-5 blocks tall
                generatePillar(level, pillarBase, height, random);
            }
        }
        
        // Generate central altar
        generateCentralAltar(level, center, random);
        
        return true;
    }
    
    /**
     * Generates a single standing stone pillar.
     */
    private void generatePillar(WorldGenLevel level, BlockPos base, int height, RandomSource random) {
        for (int y = 0; y < height; y++) {
            BlockState state;
            if (y == 0) {
                // Base block
                state = getBaseBlock(random);
            } else if (y == height - 1 && random.nextFloat() < 0.3f) {
                // Top sometimes has glyphed stone
                state = ModBlocks.ANCIENT_STONE_GLYPHED.get().defaultBlockState();
            } else {
                // Regular pillar
                state = getPillarBlock(random);
            }
            setBlock(level, base.above(y), state);
        }
    }
    
    /**
     * Generates a central altar/pedestal area.
     */
    private void generateCentralAltar(WorldGenLevel level, BlockPos center, RandomSource random) {
        // Platform base (3x3)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                setBlock(level, center.offset(dx, -1, dz), ModBlocks.ANCIENT_STONE_TILE.get().defaultBlockState());
            }
        }
        
        // Central pedestal
        setBlock(level, center, ModBlocks.ANCIENT_STONE.get().defaultBlockState());
        setBlock(level, center.above(), ModBlocks.ANCIENT_STONE_GLYPHED.get().defaultBlockState());
        
        // Small pillars at corners
        BlockPos[] corners = {
            center.offset(-1, 0, -1),
            center.offset(1, 0, -1),
            center.offset(-1, 0, 1),
            center.offset(1, 0, 1)
        };
        
        for (BlockPos corner : corners) {
            if (random.nextFloat() < 0.7f) {
                setBlock(level, corner, ModBlocks.ANCIENT_PILLAR.get().defaultBlockState());
            }
        }
    }
    
    /**
     * Finds the ground position for a pillar at the given XZ coordinates.
     */
    private BlockPos findPillarBase(WorldGenLevel level, BlockPos approx) {
        // Search within 3 blocks vertically
        for (int dy = -3; dy <= 3; dy++) {
            BlockPos pos = approx.offset(0, dy, 0);
            BlockState state = level.getBlockState(pos);
            BlockState below = level.getBlockState(pos.below());
            
            if (isAirOrReplaceable(state) && isValidGround(below)) {
                return pos;
            }
        }
        return null;
    }
    
    /**
     * Gets a random base block (ancient or eldritch stone).
     */
    private BlockState getBaseBlock(RandomSource random) {
        if (random.nextFloat() < 0.2f) {
            return ModBlocks.ELDRITCH_STONE_TILE.get().defaultBlockState();
        } else if (random.nextFloat() < 0.5f) {
            return ModBlocks.ANCIENT_STONE_TILE.get().defaultBlockState();
        } else {
            return ModBlocks.ANCIENT_STONE.get().defaultBlockState();
        }
    }
    
    /**
     * Gets a random pillar block with vertical axis.
     */
    private BlockState getPillarBlock(RandomSource random) {
        BlockState state;
        if (random.nextFloat() < 0.15f) {
            state = ModBlocks.ELDRITCH_PILLAR.get().defaultBlockState();
        } else if (random.nextFloat() < 0.5f) {
            state = ModBlocks.ANCIENT_PILLAR.get().defaultBlockState();
        } else {
            state = ModBlocks.ARCANE_PILLAR.get().defaultBlockState();
        }
        
        // Set vertical axis if property exists
        if (state.hasProperty(BlockStateProperties.AXIS)) {
            state = state.setValue(BlockStateProperties.AXIS, Direction.Axis.Y);
        }
        
        return state;
    }
}




