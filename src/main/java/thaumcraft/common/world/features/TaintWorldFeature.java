package thaumcraft.common.world.features;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import thaumcraft.common.blocks.world.taint.BlockTaintFeature;
import thaumcraft.init.ModBlocks;

/**
 * TaintWorldFeature - Generates tainted ground formations in the tainted biome.
 *
 * Variants:
 * - CRUST: replaces the surface with taint crust patches
 * - GEYSER: taint geyser pillars rising from tainted soil
 * - TENDRILS: taint tendrils (BlockTaintFeature) attached to tainted surfaces
 */
public class TaintWorldFeature extends Feature<NoneFeatureConfiguration> {

    public enum TaintFeatureType {
        CRUST,
        GEYSER,
        TENDRILS
    }

    private final TaintFeatureType featureType;

    public TaintWorldFeature(Codec<NoneFeatureConfiguration> codec, TaintFeatureType type) {
        super(codec);
        this.featureType = type;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        switch (featureType) {
            case CRUST -> generateCrust(level, random, origin);
            case GEYSER -> generateGeyser(level, random, origin);
            case TENDRILS -> generateTendrils(level, random, origin);
        }
        return true;
    }

    /**
     * Replaces grass/dirt/stone surface tops with taint crust.
     */
    private static void generateCrust(WorldGenLevel level, RandomSource random, BlockPos origin) {
        if (ModBlocks.TAINT_CRUST == null) return;

        for (int i = 0; i < 24; i++) {
            BlockPos pos = origin.offset(
                    random.nextInt(12) - random.nextInt(12),
                    random.nextInt(6) - random.nextInt(6),
                    random.nextInt(12) - random.nextInt(12));

            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) continue;

            BlockState below = level.getBlockState(pos.below());
            if (below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.DIRT) ||
                    below.is(Blocks.STONE) || below.is(Blocks.COBBLESTONE) ||
                    below.is(Blocks.SAND) || below.is(Blocks.GRAVEL) ||
                    below.is(Blocks.ANDESITE) || below.is(Blocks.DIORITE) || below.is(Blocks.GRANITE)) {
                level.setBlock(pos, ModBlocks.TAINT_CRUST.get().defaultBlockState(), 2);
            }
        }
    }

    /**
     * Builds a taint geyser pillar with tendrils on top.
     */
    private static void generateGeyser(WorldGenLevel level, RandomSource random, BlockPos origin) {
        if (ModBlocks.TAINT_GEYSER == null) return;

        BlockPos pos = origin;
        for (int i = 0; i < 8; i++) {
            BlockPos check = origin.offset(
                    random.nextInt(8) - random.nextInt(8), 0,
                    random.nextInt(8) - random.nextInt(8));
            if (level.getBlockState(check).isAir()) {
                pos = check;
                break;
            }
        }

        BlockPos groundPos = pos.below();
        if (level.getBlockState(groundPos).isAir()) {
            for (int y = pos.getY(); y >= level.getMinBuildHeight() && y > pos.getY() - 16; y--) {
                BlockPos bp = new BlockPos(pos.getX(), y, pos.getZ());
                if (!level.getBlockState(bp).isAir()) {
                    groundPos = bp;
                    break;
                }
            }
        }
        if (level.getBlockState(groundPos).isAir()) return;

        int height = 2 + random.nextInt(3);
        for (int y = 0; y < height; y++) {
            level.setBlock(groundPos.above(y + 1), ModBlocks.TAINT_GEYSER.get().defaultBlockState(), 2);
        }

        BlockPos topPos = groundPos.above(height + 1);
        if (ModBlocks.TAINT_FEATURE != null && level.getBlockState(topPos).isAir()) {
            level.setBlock(topPos, ModBlocks.TAINT_FEATURE.get().defaultBlockState()
                    .setValue(BlockTaintFeature.FACING, Direction.UP), 2);
        }
    }

    /**
     * Attaches taint tendrils to tainted blocks on any face.
     */
    private static void generateTendrils(WorldGenLevel level, RandomSource random, BlockPos origin) {
        if (ModBlocks.TAINT_FEATURE == null) return;

        for (int i = 0; i < 10; i++) {
            BlockPos pos = origin.offset(
                    random.nextInt(10) - random.nextInt(10),
                    random.nextInt(5) - random.nextInt(5),
                    random.nextInt(10) - random.nextInt(10));

            if (!level.getBlockState(pos).isAir()) continue;

            Direction dir = Direction.getRandom(random);
            BlockPos attachPos = pos.relative(dir.getOpposite());
            BlockState attachState = level.getBlockState(attachPos);
            boolean tainted = (ModBlocks.TAINT_SOIL != null && attachState.is(ModBlocks.TAINT_SOIL.get())) ||
                    (ModBlocks.TAINT_ROCK != null && attachState.is(ModBlocks.TAINT_ROCK.get())) ||
                    (ModBlocks.TAINT_CRUST != null && attachState.is(ModBlocks.TAINT_CRUST.get())) ||
                    (ModBlocks.TAINT_LOG != null && attachState.is(ModBlocks.TAINT_LOG.get())) ||
                    (ModBlocks.TAINT_GEYSER != null && attachState.is(ModBlocks.TAINT_GEYSER.get()));
            if (!tainted) continue;

            level.setBlock(pos, ModBlocks.TAINT_FEATURE.get().defaultBlockState()
                    .setValue(BlockTaintFeature.FACING, dir), 2);
        }
    }
}
