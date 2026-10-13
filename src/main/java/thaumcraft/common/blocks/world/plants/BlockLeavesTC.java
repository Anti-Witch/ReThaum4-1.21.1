package thaumcraft.common.blocks.world.plants;

import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * Leaves blocks for greatwood and silverwood trees.
 */
public class BlockLeavesTC extends LeavesBlock {

    private final boolean glows;

    public BlockLeavesTC(Properties properties, boolean glows) {
        super(properties);
        this.glows = glows;
    }

    /**
     * TC4 thaumcraft leaves never decay - the trees are not vanilla
     * log-and-leaf structures, so the vanilla distance/decay mechanic
     * would strip them bare over time.
     */
    @Override
    public boolean isRandomlyTicking(net.minecraft.world.level.block.state.BlockState state) {
        return false;
    }

    @Override
    public void randomTick(net.minecraft.world.level.block.state.BlockState state,
                           net.minecraft.server.level.ServerLevel level,
                           net.minecraft.core.BlockPos pos,
                           net.minecraft.util.RandomSource random) {
        // No decay, no sapling dropping from vanilla leaf logic
    }

    /**
     * Creates greatwood leaves.
     */
    public static BlockLeavesTC createGreatwood() {
        return new BlockLeavesTC(BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .strength(0.2f)
                .randomTicks()
                .sound(SoundType.GRASS)
                .noOcclusion()
                .isValidSpawn((state, level, pos, type) -> false)
                .isSuffocating((state, level, pos) -> false)
                .isViewBlocking((state, level, pos) -> false),
                false);
    }

    /**
     * Creates silverwood leaves - they glow slightly.
     */
    public static BlockLeavesTC createSilverwood() {
        return new BlockLeavesTC(BlockBehaviour.Properties.of()
                .mapColor(MapColor.QUARTZ)
                .strength(0.2f)
                .randomTicks()
                .sound(SoundType.GRASS)
                .noOcclusion()
                // TC4 BlockMagicalLeaves#getLightValue: silverwood (meta&1==1) emits light 7,
                // the same value the silverwood log uses here.
                .lightLevel(state -> 7)
                .isValidSpawn((state, level, pos, type) -> false)
                .isSuffocating((state, level, pos) -> false)
                .isViewBlocking((state, level, pos) -> false),
                true);
    }
}




