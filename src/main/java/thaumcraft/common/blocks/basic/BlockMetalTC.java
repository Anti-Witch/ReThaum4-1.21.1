package thaumcraft.common.blocks.basic;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import thaumcraft.common.blocks.BlockTC;

/**
 * Metal blocks used in Thaumcraft (brass, thaumium, void metal, etc.)
 */
public class BlockMetalTC extends BlockTC {

    public BlockMetalTC(Properties properties) {
        super(properties);
    }

    /**
     * Creates a standard metal block.
     */
    public static BlockMetalTC create() {
        return new BlockMetalTC(
            Properties.of()
                .mapColor(MapColor.METAL)
                .strength(5.0f, 6.0f)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops()
        );
    }

    /**
     * Creates a metal block with custom color.
     */
    public static BlockMetalTC create(MapColor color) {
        return new BlockMetalTC(
            Properties.of()
                .mapColor(color)
                .strength(5.0f, 6.0f)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops()
        );
    }
}




