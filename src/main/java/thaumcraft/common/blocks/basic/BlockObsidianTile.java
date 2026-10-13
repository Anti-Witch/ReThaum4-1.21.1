package thaumcraft.common.blocks.basic;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import thaumcraft.common.blocks.BlockTC;

/**
 * Obsidian Tile - decorative obsidian plate (TC4 blockCosmeticSolid meta 1).
 * Serves as the base slab of generated Obsidian Totems.
 */
public class BlockObsidianTile extends BlockTC {

    public BlockObsidianTile() {
        super(props());
    }

    public static BlockBehaviour.Properties props() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(30.0f, 999.0f)
                .sound(SoundType.STONE);
    }
}
