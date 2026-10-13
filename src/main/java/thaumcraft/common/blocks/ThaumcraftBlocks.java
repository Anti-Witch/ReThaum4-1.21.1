package thaumcraft.common.blocks;

import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;
import thaumcraft.init.ModBlocks;

/**
 * ThaumcraftBlocks - Wrapper for accessing registered blocks.
 * This is a compatibility layer for TC4-style block references.
 * Use ModBlocks directly for new code.
 */
public class ThaumcraftBlocks {

    public static final DeferredHolder<Block, Block> HOLE = ModBlocks.HOLE;

    // Warded blocks - placeholders for TC4 Warded block system
    public static final DeferredHolder<Block, Block> WARDED_STONE = ModBlocks.ARCANE_STONE; // placeholder
    public static final DeferredHolder<Block, Block> WARDED_GLASS = ModBlocks.ARCANE_STONE; // placeholder
    public static final DeferredHolder<Block, Block> WARDED_DOOR = ModBlocks.ARCANE_STONE; // placeholder
    public static final DeferredHolder<Block, Block> WARDED_TRAPDOOR = ModBlocks.ARCANE_STONE; // placeholder

    // Additional warded variants
    public static final DeferredHolder<Block, Block> WARDED_STAIRS = ModBlocks.ARCANE_STONE_STAIRS;
    public static final DeferredHolder<Block, Block> WARDED_SLAB = ModBlocks.ARCANE_STONE_SLAB;
}