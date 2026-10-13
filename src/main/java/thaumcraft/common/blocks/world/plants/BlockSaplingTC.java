package thaumcraft.common.blocks.world.plants;

import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.resources.ResourceKey;

import java.util.Optional;

/**
 * Sapling blocks for greatwood and silverwood trees.
 */
public class BlockSaplingTC extends SaplingBlock {

    public BlockSaplingTC(TreeGrower treeGrower, Properties properties) {
        super(treeGrower, properties);
    }

    /**
     * Creates greatwood sapling.
     */
    public static BlockSaplingTC createGreatwood() {
        return new BlockSaplingTC(
                new TreeGrower(
                        "greatwood",
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(net.minecraft.resources.ResourceKey.create(
                                net.minecraft.core.registries.Registries.CONFIGURED_FEATURE,
                                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                                        "thaumcraft", "big_magic_tree")))
                ),
                BlockBehaviour.Properties.of()
                        .mapColor(MapColor.PLANT)
                        .noCollission()
                        .randomTicks()
                        .instabreak()
                        .sound(SoundType.GRASS));
    }

    /**
     * Creates silverwood sapling.
     */
    public static BlockSaplingTC createSilverwood() {
        return new BlockSaplingTC(
                new TreeGrower(
                        "silverwood",
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(net.minecraft.resources.ResourceKey.create(
                                net.minecraft.core.registries.Registries.CONFIGURED_FEATURE,
                                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                                        "thaumcraft", "big_silverwood_tree")))
                ),
                BlockBehaviour.Properties.of()
                        .mapColor(MapColor.QUARTZ)
                        .noCollission()
                        .randomTicks()
                        .instabreak()
                        .sound(SoundType.GRASS)
                        .lightLevel(state -> 5));
    }
}




