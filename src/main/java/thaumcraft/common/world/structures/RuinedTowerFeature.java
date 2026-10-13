package thaumcraft.common.world.structures;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import thaumcraft.init.ModBlocks;

/**
 * RuinedTowerFeature - Generates abandoned wizard towers.
 * 
 * These crumbling stone towers are remnants of ancient thaumaturges.
 * They contain:
 * - A circular stone tower with partial collapse
 * - Research materials (bookshelves, research tables)
 * - Loot crates/urns with Thaumcraft items
 * - Possible ambient research notes
 * 
 * The towers are weathered and partially destroyed, with missing
 * sections and vegetation growing through the cracks.
 */
public class RuinedTowerFeature extends Feature<NoneFeatureConfiguration> {
    
    public RuinedTowerFeature(Codec<NoneFeatureConfiguration> codec) {
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
        
        // Tower parameters
        int radius = 3 + random.nextInt(2); // 3-4 block radius
        int height = 8 + random.nextInt(7); // 8-14 blocks tall
        int collapseDirection = random.nextInt(4); // Which side is collapsed
        float collapseAmount = 0.3f + random.nextFloat() * 0.3f; // 30-60% collapse
        
        // Check for enough horizontal space
        if (!checkSpace(level, groundPos, radius + 2, height + 5)) {
            return false;
        }
        
        // Build the tower
        buildFoundation(level, groundPos, radius, random);
        buildWalls(level, groundPos, radius, height, collapseDirection, collapseAmount, random);
        buildFloors(level, groundPos, radius, height, collapseDirection, collapseAmount, random);
        addDecorations(level, groundPos, radius, height, random);
        addVegetation(level, groundPos, radius, height, random);
        
        return true;
    }
    
    private BlockPos findGround(WorldGenLevel level, BlockPos pos) {
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
    
    private boolean checkSpace(WorldGenLevel level, BlockPos center, int radius, int height) {
        // Simple space check - ensure mostly air/replaceable above ground
        int solidCount = 0;
        int totalChecks = 0;
        
        for (int y = 1; y <= height; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + z * z <= radius * radius) {
                        totalChecks++;
                        if (!level.getBlockState(center.offset(x, y, z)).isAir()) {
                            solidCount++;
                        }
                    }
                }
            }
        }
        
        return solidCount < totalChecks * 0.3; // At least 70% must be air
    }
    
    /**
     * Build the stone foundation.
     */
    private void buildFoundation(WorldGenLevel level, BlockPos center, int radius, RandomSource random) {
        BlockState arcaneStone = ModBlocks.ARCANE_STONE.get().defaultBlockState();
        BlockState stoneBrick = ModBlocks.ARCANE_STONE_BRICK.get().defaultBlockState();
        
        // Circular foundation, slightly larger than tower
        int foundRadius = radius + 1;
        for (int x = -foundRadius; x <= foundRadius; x++) {
            for (int z = -foundRadius; z <= foundRadius; z++) {
                if (x * x + z * z <= foundRadius * foundRadius) {
                    BlockPos pos = center.offset(x, 0, z);
                    
                    // Fill under foundation too
                    for (int y = 0; y >= -2; y--) {
                        BlockPos fillPos = pos.above(y);
                        if (!level.getBlockState(fillPos).isSolid()) {
                            level.setBlock(fillPos, random.nextBoolean() ? arcaneStone : stoneBrick, 2);
                        }
                    }
                    
                    level.setBlock(pos, stoneBrick, 2);
                }
            }
        }
    }
    
    /**
     * Build the circular walls with partial collapse.
     */
    private void buildWalls(WorldGenLevel level, BlockPos center, int radius, int height, 
                           int collapseDir, float collapseAmount, RandomSource random) {
        BlockState arcaneStone = ModBlocks.ARCANE_STONE.get().defaultBlockState();
        BlockState stoneBrick = ModBlocks.ARCANE_STONE_BRICK.get().defaultBlockState();
        BlockState cobblestone = Blocks.COBBLESTONE.defaultBlockState();
        
        Direction collapse = Direction.from2DDataValue(collapseDir);
        
        for (int y = 1; y <= height; y++) {
            // Collapse increases with height
            float heightFactor = (float) y / height;
            float currentCollapse = collapseAmount * heightFactor;
            
            for (int angle = 0; angle < 360; angle += 15) {
                double rad = Math.toRadians(angle);
                int x = (int) Math.round(Math.cos(rad) * radius);
                int z = (int) Math.round(Math.sin(rad) * radius);
                
                BlockPos wallPos = center.offset(x, y, z);
                
                // Check if this part is collapsed
                boolean inCollapseZone = isInCollapseZone(x, z, collapse, radius, currentCollapse);
                
                if (inCollapseZone) {
                    // Partially collapsed - random gaps
                    if (random.nextFloat() > currentCollapse * 1.5f) {
                        // Damaged blocks lower in the wall
                        level.setBlock(wallPos, random.nextInt(3) == 0 ? cobblestone : stoneBrick, 2);
                    }
                } else {
                    // Normal wall section
                    BlockState wallBlock;
                    if (y == 1 || y == height || y % 4 == 0) {
                        wallBlock = arcaneStone; // Accent rows
                    } else {
                        wallBlock = random.nextInt(10) == 0 ? cobblestone : stoneBrick;
                    }
                    level.setBlock(wallPos, wallBlock, 2);
                }
            }
        }
    }
    
    private boolean isInCollapseZone(int x, int z, Direction collapse, int radius, float amount) {
        // Check if position is on the collapse side
        int cx = collapse.getStepX();
        int cz = collapse.getStepZ();
        
        if (cx != 0) {
            return (x * cx > 0) && Math.abs(x) > radius * (1 - amount);
        } else {
            return (z * cz > 0) && Math.abs(z) > radius * (1 - amount);
        }
    }
    
    /**
     * Build internal floors.
     */
    private void buildFloors(WorldGenLevel level, BlockPos center, int radius, int height,
                            int collapseDir, float collapseAmount, RandomSource random) {
        BlockState planks = Blocks.SPRUCE_PLANKS.defaultBlockState();
        
        Direction collapse = Direction.from2DDataValue(collapseDir);
        int floorRadius = radius - 1;
        
        // Add floors every 4 blocks
        for (int floorY = 4; floorY < height - 2; floorY += 4) {
            float heightFactor = (float) floorY / height;
            float currentCollapse = collapseAmount * heightFactor;
            
            for (int x = -floorRadius; x <= floorRadius; x++) {
                for (int z = -floorRadius; z <= floorRadius; z++) {
                    if (x * x + z * z <= floorRadius * floorRadius) {
                        BlockPos floorPos = center.offset(x, floorY, z);
                        
                        // Skip collapsed sections
                        if (isInCollapseZone(x, z, collapse, floorRadius, currentCollapse * 1.2f)) {
                            if (random.nextFloat() > 0.7f) {
                                level.setBlock(floorPos, planks, 2);
                            }
                        } else {
                            level.setBlock(floorPos, planks, 2);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Add decorations like bookshelves, tables, and loot.
     */
    private void addDecorations(WorldGenLevel level, BlockPos center, int radius, int height, RandomSource random) {
        // Ground floor decorations
        addGroundFloorDecorations(level, center, radius, random);
        
        // Upper floor decorations (if floors exist)
        for (int floorY = 4; floorY < height - 2; floorY += 4) {
            addFloorDecorations(level, center.above(floorY + 1), radius - 1, random);
        }
    }
    
    private void addGroundFloorDecorations(WorldGenLevel level, BlockPos center, int radius, RandomSource random) {
        // Add a few bookshelves against the walls
        int numBookshelves = 2 + random.nextInt(3);
        for (int i = 0; i < numBookshelves; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            int x = (int) Math.round(Math.cos(angle) * (radius - 1));
            int z = (int) Math.round(Math.sin(angle) * (radius - 1));
            
            BlockPos pos = center.offset(x, 1, z);
            if (level.getBlockState(pos).isAir()) {
                level.setBlock(pos, Blocks.BOOKSHELF.defaultBlockState(), 2);
                if (random.nextBoolean() && level.getBlockState(pos.above()).isAir()) {
                    level.setBlock(pos.above(), Blocks.BOOKSHELF.defaultBlockState(), 2);
                }
            }
        }
        
        // Maybe add a loot crate
        if (random.nextInt(3) == 0) {
            BlockPos cratePos = center.offset(
                    random.nextInt(radius) - radius/2,
                    1,
                    random.nextInt(radius) - radius/2);
            if (level.getBlockState(cratePos).isAir()) {
                level.setBlock(cratePos, ModBlocks.LOOT_CRATE_COMMON.get().defaultBlockState(), 2);
            }
        }
    }
    
    private void addFloorDecorations(WorldGenLevel level, BlockPos floorCenter, int radius, RandomSource random) {
        // Smaller decorations on upper floors
        if (random.nextInt(2) == 0) {
            BlockPos pos = floorCenter.offset(
                    random.nextInt(radius) - radius/2,
                    0,
                    random.nextInt(radius) - radius/2);
            if (level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isSolid()) {
                // Random urn or crate
                if (random.nextBoolean()) {
                    level.setBlock(pos, ModBlocks.LOOT_URN_COMMON.get().defaultBlockState(), 2);
                } else {
                    level.setBlock(pos, Blocks.BOOKSHELF.defaultBlockState(), 2);
                }
            }
        }
    }
    
    /**
     * Add vegetation growing through the ruins.
     */
    private void addVegetation(WorldGenLevel level, BlockPos center, int radius, int height, RandomSource random) {
        // Vines on the outside
        int vineCount = 3 + random.nextInt(5);
        for (int i = 0; i < vineCount; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            int x = (int) Math.round(Math.cos(angle) * (radius + 1));
            int z = (int) Math.round(Math.sin(angle) * (radius + 1));
            int startY = 3 + random.nextInt(height - 3);
            
            BlockPos vineStart = center.offset(x, startY, z);
            
            // Hang vines down
            for (int y = 0; y < 2 + random.nextInt(4); y++) {
                BlockPos vinePos = vineStart.below(y);
                if (level.getBlockState(vinePos).isAir()) {
                    // Determine vine facing
                    Direction facing = Direction.fromDelta(-x, 0, -z);
                    if (facing != null && facing.getAxis().isHorizontal()) {
                        level.setBlock(vinePos, Blocks.VINE.defaultBlockState()
                                .setValue(net.minecraft.world.level.block.VineBlock.getPropertyForFace(facing), true), 2);
                    }
                } else {
                    break;
                }
            }
        }
        
        // Moss on some floor blocks
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (random.nextInt(8) == 0 && x * x + z * z <= radius * radius) {
                    BlockPos pos = center.offset(x, 1, z);
                    if (level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isSolid()) {
                        level.setBlock(pos, Blocks.FERN.defaultBlockState(), 2);
                    }
                }
            }
        }
    }
}




