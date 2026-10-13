package thaumcraft.common.world.structures;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import thaumcraft.init.ModBlocks;

/**
 * BarrowFeature - Generates ancient burial mound structures.
 * 
 * Barrows are underground stone chambers with a grass-covered mound entrance.
 * They contain:
 * - A central burial chamber with a chest
 * - Side spawner rooms with skeleton and zombie spawners
 * - Thaumcraft loot crates/urns
 * - Iron bar entrance corridor
 * 
 * Based on the original WorldGenMound from Thaumcraft 1.12.2.
 */
public class BarrowFeature extends Feature<NoneFeatureConfiguration> {
    
    public BarrowFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }
    
    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        
        // Check if location is valid for barrow placement
        if (!isValidLocation(level, origin)) {
            return false;
        }
        
        // Generate the barrow structure
        generateBarrow(level, origin, random);
        return true;
    }
    
    /**
     * Validates that the terrain is suitable for barrow placement.
     */
    private boolean isValidLocation(WorldGenLevel level, BlockPos pos) {
        // Check multiple points for valid surface
        BlockPos center = pos.offset(9, 9, 9);
        
        if (!isValidSurface(level, center)) return false;
        if (!isValidSurface(level, pos.offset(0, 9, 0))) return false;
        if (!isValidSurface(level, pos.offset(18, 9, 0))) return false;
        if (!isValidSurface(level, pos.offset(0, 9, 18))) return false;
        if (!isValidSurface(level, pos.offset(18, 9, 18))) return false;
        
        return true;
    }
    
    private boolean isValidSurface(WorldGenLevel level, BlockPos pos) {
        // Find distance to air
        int distanceToAir = 0;
        while (!level.isEmptyBlock(pos.above(distanceToAir)) && distanceToAir < 3) {
            distanceToAir++;
        }
        if (distanceToAir > 2) return false;
        
        BlockPos surfacePos = pos.above(distanceToAir - 1);
        BlockState blockState = level.getBlockState(surfacePos);
        BlockState belowState = level.getBlockState(surfacePos.below());
        
        // Check if there's air above
        if (!level.isEmptyBlock(surfacePos.above())) return false;
        
        // Check for valid ground blocks
        if (isValidGroundBlock(blockState)) return true;
        if ((blockState.is(Blocks.SNOW) || blockState.is(Blocks.SHORT_GRASS)) &&
                isValidGroundBlock(belowState)) return true;
        
        return false;
    }
    
    private boolean isValidGroundBlock(BlockState state) {
        return state.is(Blocks.STONE) || 
               state.is(Blocks.GRASS_BLOCK) || 
               state.is(Blocks.DIRT);
    }
    
    /**
     * Generates the complete barrow structure.
     */
    private void generateBarrow(WorldGenLevel level, BlockPos origin, RandomSource random) {
        int x = origin.getX();
        int y = origin.getY();
        int z = origin.getZ();
        
        // Generate the mound exterior (grass/dirt covering)
        generateMoundExterior(level, x, y, z);
        
        // Generate the stone interior
        generateStoneInterior(level, x, y, z, random);
        
        // Generate the burial chamber
        generateBurialChamber(level, x, y, z, random);
        
        // Generate entrance corridor
        generateEntrance(level, x, y, z);
        
        // Place loot and spawners
        placeLoot(level, x, y, z, random);
        placeSpawners(level, x, y, z, random);
    }
    
    /**
     * Generates the grass-covered mound exterior.
     */
    private void generateMoundExterior(WorldGenLevel level, int x, int y, int z) {
        // The mound is roughly 19x19 blocks and rises about 5 blocks above ground
        // Generate a dome-like shape with grass on top
        
        for (int dx = 0; dx <= 18; dx++) {
            for (int dz = 0; dz <= 18; dz++) {
                // Calculate height based on distance from center
                double distFromCenter = Math.sqrt(Math.pow(dx - 9, 2) + Math.pow(dz - 9, 2));
                int maxHeight = (int)(5 - distFromCenter * 0.4);
                
                if (maxHeight > 0 && distFromCenter < 10) {
                    // Place dirt/grass layers
                    for (int dy = 0; dy < maxHeight; dy++) {
                        BlockPos pos = new BlockPos(x + dx, y + 8 + dy, z + dz);
                        if (dy == maxHeight - 1) {
                            setBlock(level, pos, Blocks.GRASS_BLOCK.defaultBlockState());
                        } else {
                            setBlock(level, pos, Blocks.DIRT.defaultBlockState());
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Generates the cobblestone interior structure.
     */
    private void generateStoneInterior(WorldGenLevel level, int x, int y, int z, RandomSource random) {
        // Main chamber walls (roughly 13x13 area from 3,3 to 15,15)
        for (int dx = 3; dx <= 15; dx++) {
            for (int dz = 3; dz <= 15; dz++) {
                // Floor
                setBlock(level, new BlockPos(x + dx, y + 4, z + dz), Blocks.COBBLESTONE.defaultBlockState());
                
                // Walls (only on edges)
                if (dx == 3 || dx == 15 || dz == 3 || dz == 15) {
                    for (int dy = 5; dy <= 8; dy++) {
                        BlockState state = random.nextFloat() < 0.2f ? 
                                Blocks.MOSSY_COBBLESTONE.defaultBlockState() : 
                                Blocks.COBBLESTONE.defaultBlockState();
                        setBlock(level, new BlockPos(x + dx, y + dy, z + dz), state);
                    }
                }
                
                // Ceiling (at y+8)
                if (dx >= 4 && dx <= 14 && dz >= 4 && dz <= 14) {
                    setBlock(level, new BlockPos(x + dx, y + 8, z + dz), Blocks.COBBLESTONE.defaultBlockState());
                }
            }
        }
        
        // Clear interior air space
        for (int dx = 4; dx <= 14; dx++) {
            for (int dz = 4; dz <= 14; dz++) {
                for (int dy = 5; dy <= 7; dy++) {
                    setBlock(level, new BlockPos(x + dx, y + dy, z + dz), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }
    
    /**
     * Generates the central burial chamber with pillar and alcoves.
     */
    private void generateBurialChamber(WorldGenLevel level, int x, int y, int z, RandomSource random) {
        // Central pillar area (around 9,9)
        for (int dy = 0; dy <= 4; dy++) {
            for (int dx = 6; dx <= 12; dx++) {
                for (int dz = 6; dz <= 12; dz++) {
                    // Burial chamber floor
                    if (dy == 0) {
                        setBlock(level, new BlockPos(x + dx, y + dy, z + dz), Blocks.COBBLESTONE.defaultBlockState());
                    }
                    // Chamber walls
                    else if (dx == 6 || dx == 12 || dz == 6 || dz == 12) {
                        BlockState state = random.nextFloat() < 0.15f ? 
                                Blocks.MOSSY_COBBLESTONE.defaultBlockState() : 
                                Blocks.COBBLESTONE.defaultBlockState();
                        setBlock(level, new BlockPos(x + dx, y + dy, z + dz), state);
                    }
                    // Interior air
                    else {
                        setBlock(level, new BlockPos(x + dx, y + dy, z + dz), Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
        
        // Stairs leading down into chamber
        setBlock(level, new BlockPos(x + 6, y + 4, z + 7), 
                Blocks.STONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));
        setBlock(level, new BlockPos(x + 6, y + 4, z + 8), Blocks.AIR.defaultBlockState());
        setBlock(level, new BlockPos(x + 6, y + 4, z + 9), Blocks.AIR.defaultBlockState());
        setBlock(level, new BlockPos(x + 6, y + 4, z + 10), Blocks.AIR.defaultBlockState());
        setBlock(level, new BlockPos(x + 6, y + 4, z + 11), 
                Blocks.STONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));
    }
    
    /**
     * Generates the entrance corridor with iron bars.
     */
    private void generateEntrance(WorldGenLevel level, int x, int y, int z) {
        // Entrance corridor at the west side (x = 0 to 3)
        for (int dx = 0; dx <= 3; dx++) {
            // Floor
            setBlock(level, new BlockPos(x + dx, y + 8, z + 8), Blocks.COBBLESTONE.defaultBlockState());
            setBlock(level, new BlockPos(x + dx, y + 8, z + 9), Blocks.COBBLESTONE.defaultBlockState());
            setBlock(level, new BlockPos(x + dx, y + 8, z + 10), Blocks.COBBLESTONE.defaultBlockState());
            
            // Air passage
            setBlock(level, new BlockPos(x + dx, y + 9, z + 9), Blocks.AIR.defaultBlockState());
            setBlock(level, new BlockPos(x + dx, y + 10, z + 9), Blocks.AIR.defaultBlockState());
            
            // Stairs at entrance
            if (dx == 0) {
                setBlock(level, new BlockPos(x + dx, y + 9, z + 8), 
                        Blocks.STONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST));
                setBlock(level, new BlockPos(x + dx, y + 9, z + 10), 
                        Blocks.STONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST));
            }
        }
        
        // Iron bars at entrance
        setBlock(level, new BlockPos(x + 3, y + 9, z + 9), Blocks.IRON_BARS.defaultBlockState());
        setBlock(level, new BlockPos(x + 3, y + 10, z + 9), Blocks.IRON_BARS.defaultBlockState());
    }
    
    /**
     * Places loot chests and Thaumcraft loot containers.
     */
    private void placeLoot(WorldGenLevel level, int x, int y, int z, RandomSource random) {
        // Place main chest in burial chamber
        boolean isTrapped = random.nextInt(3) == 0;
        BlockState chestState = (isTrapped ? Blocks.TRAPPED_CHEST : Blocks.CHEST).defaultBlockState()
                .setValue(ChestBlock.FACING, Direction.WEST);
        
        BlockPos chestPos = new BlockPos(x + 10, y + 1, z + 9);
        setBlock(level, chestPos, chestState);
        
        // Set chest loot table
        BlockEntity be = level.getBlockEntity(chestPos);
        if (be instanceof ChestBlockEntity chest) {
            chest.setLootTable(BuiltInLootTables.SIMPLE_DUNGEON, random.nextLong());
        }
        
        // Place TNT under trapped chest
        if (isTrapped) {
            setBlock(level, chestPos.below().below(), Blocks.TNT.defaultBlockState());
        }
        
        // Place Thaumcraft loot crates/urns
        placeLootContainer(level, new BlockPos(x + 9, y + 1, z + 7), random);
        placeLootContainer(level, new BlockPos(x + 9, y + 1, z + 11), random);
    }
    
    /**
     * Places a random Thaumcraft loot container (crate or urn).
     */
    private void placeLootContainer(WorldGenLevel level, BlockPos pos, RandomSource random) {
        float rarity = random.nextFloat();
        int tier = rarity < 0.1f ? 2 : (rarity < 0.33f ? 1 : 0); // rare, uncommon, common
        boolean isCrate = random.nextFloat() < 0.3f;
        
        BlockState lootBlock;
        try {
            lootBlock = switch (tier) {
                case 2 -> isCrate ? ModBlocks.LOOT_CRATE_RARE.get().defaultBlockState() : 
                                   ModBlocks.LOOT_URN_RARE.get().defaultBlockState();
                case 1 -> isCrate ? ModBlocks.LOOT_CRATE_UNCOMMON.get().defaultBlockState() : 
                                   ModBlocks.LOOT_URN_UNCOMMON.get().defaultBlockState();
                default -> isCrate ? ModBlocks.LOOT_CRATE_COMMON.get().defaultBlockState() : 
                                    ModBlocks.LOOT_URN_COMMON.get().defaultBlockState();
            };
            setBlock(level, pos, lootBlock);
        } catch (Exception e) {
            // Fallback if loot blocks aren't registered yet
            setBlock(level, pos, Blocks.BARREL.defaultBlockState());
        }
    }
    
    /**
     * Places mob spawners in the side chambers.
     */
    private void placeSpawners(WorldGenLevel level, int x, int y, int z, RandomSource random) {
        // Skeleton spawner in one corner
        BlockPos spawner1Pos = new BlockPos(x + 4, y + 5, z + 4);
        setBlock(level, spawner1Pos, Blocks.SPAWNER.defaultBlockState());
        BlockEntity be1 = level.getBlockEntity(spawner1Pos);
        if (be1 instanceof SpawnerBlockEntity spawner) {
            spawner.setEntityId(net.minecraft.world.entity.EntityType.SKELETON, random);
        }
        
        // Zombie spawner in opposite corner
        BlockPos spawner2Pos = new BlockPos(x + 4, y + 5, z + 14);
        setBlock(level, spawner2Pos, Blocks.SPAWNER.defaultBlockState());
        BlockEntity be2 = level.getBlockEntity(spawner2Pos);
        if (be2 instanceof SpawnerBlockEntity spawner) {
            spawner.setEntityId(net.minecraft.world.entity.EntityType.ZOMBIE, random);
        }
    }
}




