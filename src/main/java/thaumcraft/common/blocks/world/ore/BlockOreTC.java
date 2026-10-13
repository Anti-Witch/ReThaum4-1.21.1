package thaumcraft.common.blocks.world.ore;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * Ore blocks for Thaumcraft (amber, cinnabar, quartz).
 * These use loot tables for drops in 1.20.1.
 */
public class BlockOreTC extends Block {

    public BlockOreTC(Properties properties) {
        super(properties);
    }

    /**
     * Creates amber ore - drops amber items.
     */
    public static BlockOreTC createAmberOre() {
        return new BlockOreTC(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(1.5f, 5.0f)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops());
    }

    /**
     * Creates cinnabar ore - drops cinnabar and quicksilver.
     */
    public static BlockOreTC createCinnabarOre() {
        return new BlockOreTC(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(2.0f, 5.0f)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops());
    }

    /**
     * Creates quartz ore (overworld variant) - drops quartz.
     */
    public static BlockOreTC createQuartzOre() {
        return new BlockOreTC(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(3.0f, 5.0f)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops());
    }

    // ==================== Deepslate Variants ====================

    /**
     * Creates deepslate amber ore - drops amber items.
     */
    public static BlockOreTC createDeepslateAmberOre() {
        return new BlockOreTC(BlockBehaviour.Properties.of()
                .mapColor(MapColor.DEEPSLATE)
                .strength(3.0f, 6.0f)
                .sound(SoundType.DEEPSLATE)
                .requiresCorrectToolForDrops());
    }

    /**
     * Creates deepslate cinnabar ore - drops cinnabar and quicksilver.
     */
    public static BlockOreTC createDeepslateCinnabarOre() {
        return new BlockOreTC(BlockBehaviour.Properties.of()
                .mapColor(MapColor.DEEPSLATE)
                .strength(3.5f, 6.0f)
                .sound(SoundType.DEEPSLATE)
                .requiresCorrectToolForDrops());
    }

    /**
     * Creates deepslate quartz ore - drops quartz.
     */
    public static BlockOreTC createDeepslateQuartzOre() {
        return new BlockOreTC(BlockBehaviour.Properties.of()
                .mapColor(MapColor.DEEPSLATE)
                .strength(4.5f, 6.0f)
                .sound(SoundType.DEEPSLATE)
                .requiresCorrectToolForDrops());
    }
}




