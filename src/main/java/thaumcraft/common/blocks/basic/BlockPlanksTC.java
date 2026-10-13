package thaumcraft.common.blocks.basic;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import thaumcraft.common.blocks.BlockTC;

/**
 * Wooden plank blocks for greatwood and silverwood.
 */
public class BlockPlanksTC extends BlockTC {

    public BlockPlanksTC(Properties properties) {
        super(properties);
    }

    /**
     * Creates greatwood planks.
     */
    public static BlockPlanksTC createGreatwood() {
        return new BlockPlanksTC(
            Properties.of()
                .mapColor(MapColor.COLOR_BROWN)
                .strength(2.0f, 3.0f)
                .sound(SoundType.WOOD)
        );
    }

    /**
     * Creates silverwood planks.
     */
    public static BlockPlanksTC createSilverwood() {
        return new BlockPlanksTC(
            Properties.of()
                .mapColor(MapColor.QUARTZ)
                .strength(2.0f, 3.0f)
                .sound(SoundType.WOOD)
        );
    }
}




