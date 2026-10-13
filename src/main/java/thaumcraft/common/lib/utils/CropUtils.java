package thaumcraft.common.lib.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.HashSet;
import java.util.Set;

/**
 * CropUtils - Utilities for determining crop growth states.
 * Used by harvest golems and growth lamps.
 */
public class CropUtils {
    
    // Crops that are fully grown at specific states (registered by block ID + state)
    private static final Set<String> standardCrops = new HashSet<>();
    
    // Crops that can be right-clicked to harvest
    private static final Set<String> clickableCrops = new HashSet<>();
    
    // Crops that stack vertically (like sugar cane, cactus)
    private static final Set<String> stackedCrops = new HashSet<>();
    
    // Crops blacklisted from growth lamp effects
    private static final Set<String> lampBlacklist = new HashSet<>();
    
    /**
     * Add a standard crop (harvested by breaking).
     */
    public static void addStandardCrop(ItemStack stack, int grownAge) {
        Block block = Block.byItem(stack.getItem());
        if (block == Blocks.AIR) return;
        addStandardCrop(block, grownAge);
    }
    
    /**
     * Add a standard crop (harvested by breaking).
     */
    public static void addStandardCrop(Block block, int grownAge) {
        String blockId = getBlockId(block);
        if (blockId == null) return;
        
        if (grownAge == 32767) {
            // Wildcard - all states are considered grown
            for (int a = 0; a < 16; a++) {
                standardCrops.add(blockId + ":" + a);
            }
        } else {
            standardCrops.add(blockId + ":" + grownAge);
        }
        
        // CropBlocks default to max age 7
        if (block instanceof CropBlock && grownAge != 7) {
            standardCrops.add(blockId + ":7");
        }
    }
    
    /**
     * Add a clickable crop (harvested by right-click).
     */
    public static void addClickableCrop(ItemStack stack, int grownAge) {
        Block block = Block.byItem(stack.getItem());
        if (block == Blocks.AIR) return;
        
        String blockId = getBlockId(block);
        if (blockId == null) return;
        
        if (grownAge == 32767) {
            for (int a = 0; a < 16; a++) {
                clickableCrops.add(blockId + ":" + a);
            }
        } else {
            clickableCrops.add(blockId + ":" + grownAge);
        }
        
        if (block instanceof CropBlock && grownAge != 7) {
            clickableCrops.add(blockId + ":7");
        }
    }
    
    /**
     * Add a stacked crop (like sugar cane - break top, leave bottom).
     */
    public static void addStackedCrop(ItemStack stack, int grownAge) {
        Block block = Block.byItem(stack.getItem());
        if (block == Blocks.AIR) return;
        addStackedCrop(block, grownAge);
    }
    
    /**
     * Add a stacked crop.
     */
    public static void addStackedCrop(Block block, int grownAge) {
        String blockId = getBlockId(block);
        if (blockId == null) return;
        
        if (grownAge == 32767) {
            for (int a = 0; a < 16; a++) {
                stackedCrops.add(blockId + ":" + a);
            }
        } else {
            stackedCrops.add(blockId + ":" + grownAge);
        }
        
        if (block instanceof CropBlock && grownAge != 7) {
            stackedCrops.add(blockId + ":7");
        }
    }
    
    /**
     * Blacklist a crop from growth lamp effects.
     */
    public static void blacklistLamp(ItemStack stack, int age) {
        Block block = Block.byItem(stack.getItem());
        if (block == Blocks.AIR) return;
        
        String blockId = getBlockId(block);
        if (blockId == null) return;
        
        if (age == 32767) {
            for (int a = 0; a < 16; a++) {
                lampBlacklist.add(blockId + ":" + a);
            }
        } else {
            lampBlacklist.add(blockId + ":" + age);
        }
    }
    
    /**
     * Check if a block is a fully grown crop ready for harvest.
     */
    public static boolean isGrownCrop(Level level, BlockPos pos) {
        if (level.isEmptyBlock(pos)) return false;
        
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        String blockId = getBlockId(block);
        
        if (blockId == null) return false;
        
        // Get age/state value for comparison
        int age = getBlockAge(state);
        String key = blockId + ":" + age;
        
        // Check registered crops
        if (standardCrops.contains(key) || clickableCrops.contains(key)) {
            return true;
        }
        
        // Check stacked crops (must have same block below)
        if (stackedCrops.contains(key)) {
            Block below = level.getBlockState(pos.below()).getBlock();
            if (below == block) {
                return true;
            }
        }
        
        // Generic crop detection via BonemealableBlock interface
        if (block instanceof BonemealableBlock bonemealable) {
            // If it can't grow anymore, it's fully grown
            // (unless it's a stem which never stops being bonemealable)
            if (!(block instanceof StemBlock)) {
                if (!bonemealable.isValidBonemealTarget(level, pos, state)) {
                    return true;
                }
            }
        }
        
        // CropBlock with max age
        if (block instanceof CropBlock cropBlock) {
            if (cropBlock.isMaxAge(state)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if a crop is clickable (harvested by right-click).
     */
    public static boolean isClickableCrop(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        String blockId = getBlockId(block);
        
        if (blockId == null) return false;
        
        int age = getBlockAge(state);
        return clickableCrops.contains(blockId + ":" + age);
    }
    
    /**
     * Check if a crop is stacked (like sugar cane).
     */
    public static boolean isStackedCrop(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        String blockId = getBlockId(block);
        
        if (blockId == null) return false;
        
        int age = getBlockAge(state);
        return stackedCrops.contains(blockId + ":" + age);
    }
    
    /**
     * Check if growth lamp can affect this block.
     */
    public static boolean doesLampGrow(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        String blockId = getBlockId(block);
        
        if (blockId == null) return true;
        
        int age = getBlockAge(state);
        return !lampBlacklist.contains(blockId + ":" + age);
    }
    
    /**
     * Get the block's registry ID as a string.
     */
    private static String getBlockId(Block block) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
        return key != null ? key.toString() : null;
    }
    
    /**
     * Get the age/state value for crop comparison.
     * For CropBlocks, uses the AGE property.
     * For other blocks, returns 0.
     */
    private static int getBlockAge(BlockState state) {
        if (state.getBlock() instanceof CropBlock cropBlock) {
            return cropBlock.getAge(state);
        }
        // For non-crop blocks, try to find an age-like property
        for (var prop : state.getProperties()) {
            if (prop.getName().equalsIgnoreCase("age")) {
                Object value = state.getValue(prop);
                if (value instanceof Integer intVal) {
                    return intVal;
                }
            }
        }
        return 0;
    }
    
    /**
     * Initialize default crop registrations.
     */
    public static void init() {
        // Stacked crops
        addStackedCrop(Blocks.SUGAR_CANE, 0);
        addStackedCrop(Blocks.CACTUS, 0);
        addStackedCrop(Blocks.BAMBOO, 32767);
        addStackedCrop(Blocks.KELP_PLANT, 32767);
        
        // Standard crops are auto-detected via CropBlock interface
        // but we can add specific ones here if needed
        
        // Note: Most crops are auto-detected via BonemealableBlock.isValidBonemealTarget()
    }
}




