package thaumcraft.common.world.structures;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import thaumcraft.init.ModBlocks;

/**
 * EldritchObeliskFeature - Generates mysterious eldritch obelisks.
 * 
 * These tall, dark stone monuments spawn rarely in the overworld,
 * hinting at the existence of eldritch knowledge. They feature:
 * - A tall central pillar made of eldritch stone
 * - Glowing crimson runes at certain levels
 * - An obsidian-lined base
 * - Scattered ancient stones around the perimeter
 * 
 * Finding an obelisk can provide research hints and spawn eldritch mobs.
 */
public class EldritchObeliskFeature extends Feature<NoneFeatureConfiguration> {
    
    public EldritchObeliskFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }
    
    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        
        // Find solid ground
        BlockPos groundPos = findGround(level, origin);
        if (groundPos == null) {
            return false;
        }
        
        // Don't generate in water
        if (level.getBlockState(groundPos).is(Blocks.WATER)) {
            return false;
        }
        
        // Check for enough space (needs ~15 blocks vertically)
        for (int y = 1; y <= 15; y++) {
            BlockState state = level.getBlockState(groundPos.above(y));
            if (!state.isAir() && !state.canBeReplaced()) {
                return false;
            }
        }
        
        int height = 10 + random.nextInt(6); // 10-15 blocks tall
        
        // Build the obelisk
        buildBase(level, groundPos, random);
        buildPillar(level, groundPos, height, random);
        buildTop(level, groundPos.above(height), random);
        scatterDebris(level, groundPos, random);
        
        return true;
    }
    
    private BlockPos findGround(WorldGenLevel level, BlockPos pos) {
        // Search downward for solid ground
        for (int y = pos.getY(); y > level.getMinBuildHeight() + 10; y--) {
            BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
            BlockState state = level.getBlockState(checkPos);
            BlockState above = level.getBlockState(checkPos.above());
            
            if (state.isSolid() && (above.isAir() || above.canBeReplaced())) {
                return checkPos;
            }
        }
        return null;
    }
    
    /**
     * Build the obsidian-lined base platform.
     */
    private void buildBase(WorldGenLevel level, BlockPos center, RandomSource random) {
        BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState();
        BlockState eldritchStone = ModBlocks.ELDRITCH_STONE_TILE.get().defaultBlockState();
        
        // 5x5 base platform
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos pos = center.offset(x, 0, z);
                
                // Corner pillars
                if (Math.abs(x) == 2 && Math.abs(z) == 2) {
                    level.setBlock(pos, eldritchStone, 2);
                    level.setBlock(pos.above(), eldritchStone, 2);
                }
                // Edge obsidian
                else if (Math.abs(x) == 2 || Math.abs(z) == 2) {
                    level.setBlock(pos, obsidian, 2);
                }
                // Inner floor
                else {
                    level.setBlock(pos, eldritchStone, 2);
                }
            }
        }
    }
    
    /**
     * Build the main obelisk pillar.
     */
    private void buildPillar(WorldGenLevel level, BlockPos base, int height, RandomSource random) {
        BlockState eldritchStone = ModBlocks.ELDRITCH_STONE_TILE.get().defaultBlockState();
        BlockState pillar = ModBlocks.ELDRITCH_PILLAR.get().defaultBlockState();
        BlockState ancientStone = ModBlocks.ANCIENT_STONE.get().defaultBlockState();
        
        // Main central pillar
        for (int y = 1; y <= height; y++) {
            BlockPos pos = base.above(y);
            
            // Use pillars for the shaft
            if (y % 3 == 0) {
                // Decorative ring every 3 blocks
                level.setBlock(pos.north(), ancientStone, 2);
                level.setBlock(pos.south(), ancientStone, 2);
                level.setBlock(pos.east(), ancientStone, 2);
                level.setBlock(pos.west(), ancientStone, 2);
            }
            
            level.setBlock(pos, pillar, 2);
        }
        
        // Widen the base of the pillar
        for (int y = 1; y <= 3; y++) {
            float widen = (4 - y) * 0.3f;
            if (random.nextFloat() < widen) {
                level.setBlock(base.above(y).north(), eldritchStone, 2);
            }
            if (random.nextFloat() < widen) {
                level.setBlock(base.above(y).south(), eldritchStone, 2);
            }
            if (random.nextFloat() < widen) {
                level.setBlock(base.above(y).east(), eldritchStone, 2);
            }
            if (random.nextFloat() < widen) {
                level.setBlock(base.above(y).west(), eldritchStone, 2);
            }
        }
    }
    
    /**
     * Build the pointed top of the obelisk.
     */
    private void buildTop(WorldGenLevel level, BlockPos top, RandomSource random) {
        BlockState pillar = ModBlocks.ELDRITCH_PILLAR.get().defaultBlockState();
        BlockState ancientStone = ModBlocks.ANCIENT_STONE.get().defaultBlockState();
        
        // Stepped pyramid top
        level.setBlock(top.north(), ancientStone, 2);
        level.setBlock(top.south(), ancientStone, 2);
        level.setBlock(top.east(), ancientStone, 2);
        level.setBlock(top.west(), ancientStone, 2);
        
        level.setBlock(top.above(), pillar, 2);
        level.setBlock(top.above(2), pillar, 2);
        
        // Capstone
        level.setBlock(top.above(3), ancientStone, 2);
    }
    
    /**
     * Scatter debris and smaller stones around the base.
     */
    private void scatterDebris(WorldGenLevel level, BlockPos center, RandomSource random) {
        BlockState arcaneStone = ModBlocks.ARCANE_STONE.get().defaultBlockState();
        BlockState eldritchStone = ModBlocks.ELDRITCH_STONE_TILE.get().defaultBlockState();
        
        // Scatter stone debris in a radius around the obelisk
        int radius = 4 + random.nextInt(3);
        int debrisCount = 3 + random.nextInt(4);
        
        for (int i = 0; i < debrisCount; i++) {
            int dx = random.nextInt(radius * 2 + 1) - radius;
            int dz = random.nextInt(radius * 2 + 1) - radius;
            
            // Skip center area
            if (Math.abs(dx) <= 2 && Math.abs(dz) <= 2) continue;
            
            BlockPos debrisPos = center.offset(dx, 0, dz);
            
            // Find ground at this position
            for (int y = 3; y >= -3; y--) {
                BlockPos checkPos = debrisPos.above(y);
                if (level.getBlockState(checkPos.below()).isSolid() && 
                    level.getBlockState(checkPos).isAir()) {
                    
                    // Place a small stone or pillar fragment
                    if (random.nextBoolean()) {
                        level.setBlock(checkPos, arcaneStone, 2);
                    } else {
                        level.setBlock(checkPos, eldritchStone, 2);
                        if (random.nextInt(3) == 0) {
                            level.setBlock(checkPos.above(), eldritchStone, 2);
                        }
                    }
                    break;
                }
            }
        }
    }
}




