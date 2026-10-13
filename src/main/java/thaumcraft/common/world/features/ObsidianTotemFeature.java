package thaumcraft.common.world.features;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.init.ModBlocks;

/**
 * Obsidian Totem - world generation feature (TC4 ThaumcraftWorldGenerator.generateTotem).
 *
 * Structure: an Obsidian Tile base sunk into the surface plus a 1x1 pillar of
 * 2-5 Obsidian Totem segments. One segment (25% per level after the first) or,
 * failing that, a cap above the 4th segment becomes a Charged Obsidian Totem
 * hosting an artificial DARK ("sinister") node.
 *
 * The dark node itself converts surroundings to the Eerie biome and spawns
 * Giant Brainy Zombies near players (see TileNode.handleDarkNode).
 */
public class ObsidianTotemFeature extends Feature<NoneFeatureConfiguration> {

    public ObsidianTotemFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(net.minecraft.world.level.levelgen.feature.FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        int x = origin.getX();
        int z = origin.getZ();

        // TC4: topmost solid block (minus one for the block itself)
        int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
        if (topY <= level.getMinBuildHeight() + 1 || topY >= level.getMaxBuildHeight() - 8) {
            return false;
        }

        // Descend through leaves (TC4 walked down to grass under canopies)
        while (topY > level.getMinBuildHeight() + 1
                && level.getBlockState(new BlockPos(x, topY, z)).is(BlockTags.LEAVES)) {
            topY--;
        }

        BlockState ground = level.getBlockState(new BlockPos(x, topY, z));
        if (ground.is(Blocks.SNOW) || ground.is(Blocks.SHORT_GRASS)
                || ground.is(Blocks.TALL_GRASS) || ground.is(Blocks.FERN)) {
            topY--;
            ground = level.getBlockState(new BlockPos(x, topY, z));
        }

        boolean validGround = ground.is(BlockTags.DIRT) || ground.is(Blocks.SAND) || ground.is(Blocks.RED_SAND)
                || ground.is(Blocks.STONE) || ground.is(Blocks.NETHERRACK);
        if (!validGround) {
            return false;
        }

        // TC4: need at least 2 free blocks of air above
        int freeAir = 0;
        while (freeAir < 3) {
            BlockState s = level.getBlockState(new BlockPos(x, topY + 1 + freeAir, z));
            if (s.isAir() || s.is(Blocks.SNOW) || s.is(Blocks.SHORT_GRASS)
                    || s.is(Blocks.TALL_GRASS) || s.is(Blocks.FERN)) {
                freeAir++;
            } else {
                break;
            }
        }
        if (freeAir < 2) {
            return false;
        }

        // Base: obsidian tile replaces the surface block (TC4 meta 1)
        level.setBlock(new BlockPos(x, topY, z), ModBlocks.OBSIDIAN_TILE.get().defaultBlockState(), 2);

        boolean nodePlaced = false;
        int placedSegments = 0;
        for (int c = 1; c <= 4; c++) {
            BlockPos p = new BlockPos(x, topY + c, z);
            BlockState s = level.getBlockState(p);
            if (!s.isAir() && !isReplaceablePlant(s)) {
                break;
            }
            if (c > 1 && random.nextInt(4) == 0) {
                placeChargedTotem(level, p, c > 1, random);
                nodePlaced = true;
                break;
            }
            BlockState totem = ModBlocks.OBSIDIAN_TOTEM.get().defaultBlockState()
                    .setValue(thaumcraft.common.blocks.basic.BlockObsidianTotem.VARIANT, random.nextInt(4))
                    .setValue(thaumcraft.common.blocks.basic.BlockObsidianTotem.DOWN, c > 1)
                    .setValue(thaumcraft.common.blocks.basic.BlockObsidianTotem.UP, false);
            level.setBlock(p, totem, 2 | 16);
            placedSegments = c;
        }

        // Full-height pillar without a mid-node: charged totem caps it at +5 (TC4 quirk)
        if (!nodePlaced && placedSegments == 4) {
            placeChargedTotem(level, new BlockPos(x, topY + 5, z), true, random);
        }

        return true;
    }

    private void placeChargedTotem(WorldGenLevel level, BlockPos pos, boolean connectedBelow, RandomSource random) {
        BlockState state = ModBlocks.CHARGED_OBSIDIAN_TOTEM.get().defaultBlockState()
                .setValue(thaumcraft.common.blocks.basic.BlockObsidianTotem.VARIANT, random.nextInt(4))
                .setValue(thaumcraft.common.blocks.basic.BlockObsidianTotem.DOWN, connectedBelow)
                .setValue(thaumcraft.common.blocks.basic.BlockObsidianTotem.UP, false);
        level.setBlock(pos, state, 2 | 16);

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TileNode node) {
            node.setNodeType(NodeType.DARK);
            node.setNodeModifier(random.nextInt(5) == 0 ? pickModifier(random) : null);
            node.setAspects(buildDarkAspects(level, pos, random));
            node.setChanged();
        }
    }

    private NodeModifier pickModifier(RandomSource random) {
        return switch (random.nextInt(3)) {
            case 0 -> NodeModifier.BRIGHT;
            case 1 -> NodeModifier.PALE;
            default -> NodeModifier.FADING;
        };
    }

    /**
     * Simplified port of TC4 createRandomNodeAt(eerie=true): dark aspect pool
     * with environmental influences from a +/-5 scan.
     */
    private AspectList buildDarkAspects(WorldGenLevel level, BlockPos pos, RandomSource random) {
        java.util.List<Aspect> basicAspects = new java.util.ArrayList<>();
        java.util.List<Aspect> complexAspects = new java.util.ArrayList<>();
        for (Aspect as : Aspect.aspects.values()) {
            if (as.getComponents() != null) {
                complexAspects.add(as);
            } else {
                basicAspects.add(as);
            }
        }

        int value = 50 + random.nextInt(50); // biome aura ~100 like TC4 average

        AspectList al = new AspectList();
        if (random.nextFloat() < 0.7f) {
            al.add(basicAspects.get(random.nextInt(basicAspects.size())), 2);
        } else {
            al.add(complexAspects.get(random.nextInt(complexAspects.size())), 1);
            al.add(basicAspects.get(random.nextInt(basicAspects.size())), 1);
        }

        for (int a = 0; a < 3; a++) {
            if (random.nextBoolean()) {
                Aspect aa = random.nextInt(10) == 0
                        ? complexAspects.get(random.nextInt(complexAspects.size()))
                        : basicAspects.get(random.nextInt(basicAspects.size()));
                al.merge(aa, 1);
            }
        }

        // TC4 dark node aspects
        if (random.nextBoolean()) al.merge(Aspect.DEATH, 1);
        if (random.nextBoolean()) al.merge(Aspect.UNDEAD, 1);
        if (random.nextBoolean()) al.merge(Aspect.ENTROPY, 1);
        if (random.nextBoolean()) al.merge(Aspect.DARKNESS, 1);

        // Environmental influences (11x11x11 scan)
        int water = 0, lava = 0, stone = 0, foliage = 0;
        for (int xx = -5; xx <= 5; xx++) {
            for (int yy = -5; yy <= 5; yy++) {
                for (int zz = -5; zz <= 5; zz++) {
                    BlockState s = level.getBlockState(pos.offset(xx, yy, zz));
                    if (s.getFluidState().is(net.minecraft.tags.FluidTags.WATER)) water++;
                    else if (s.getFluidState().is(net.minecraft.tags.FluidTags.LAVA)) lava++;
                    else if (s.is(Blocks.STONE)) stone++;
                    else if (s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS)) foliage++;
                }
            }
        }
        if (water > 100) al.merge(Aspect.WATER, 1);
        if (lava > 100) { al.merge(Aspect.FIRE, 1); al.merge(Aspect.EARTH, 1); }
        if (stone > 500) al.merge(Aspect.EARTH, 1);
        if (foliage > 100) al.merge(Aspect.PLANT, 1);

        // Normalize proportionally to total value
        int[] spread = new int[al.size()];
        float total = 0f;
        Aspect[] sorted = al.getAspectsSorted();
        for (int a = 0; a < spread.length; a++) {
            spread[a] = al.getAmount(sorted[a]) >= 2 ? 50 + random.nextInt(25) : 25 + random.nextInt(50);
            total += spread[a];
        }
        for (int a = 0; a < spread.length; a++) {
            al.setAmount(sorted[a], (int) ((spread[a] / total) * value));
        }
        return al;
    }

    private static boolean isReplaceablePlant(BlockState state) {
        return state.is(Blocks.SNOW) || state.is(Blocks.SHORT_GRASS)
                || state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN);
    }
}
