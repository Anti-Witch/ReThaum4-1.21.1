package thaumcraft.common.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * Base class for all Thaumcraft blocks.
 * Provides common functionality and default properties.
 */
public class BlockTC extends Block {

    public BlockTC(Properties properties) {
        super(properties);
    }

    /**
     * Creates default properties for a stone-type block.
     */
    public static Properties defaultStoneProperties() {
        return Properties.of()
                .mapColor(MapColor.STONE)
                .strength(2.0f, 10.0f)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops();
    }

    /**
     * Creates default properties for a wood-type block.
     */
    public static Properties defaultWoodProperties() {
        return Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.0f, 3.0f)
                .sound(SoundType.WOOD);
    }

    /**
     * Creates default properties for a metal-type block.
     */
    public static Properties defaultMetalProperties() {
        return Properties.of()
                .mapColor(MapColor.METAL)
                .strength(5.0f, 6.0f)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops();
    }

    /**
     * Creates default properties for a glass-type block.
     */
    public static Properties defaultGlassProperties() {
        return Properties.of()
                .mapColor(MapColor.NONE)
                .strength(0.3f)
                .sound(SoundType.GLASS)
                .noOcclusion();
    }
}




