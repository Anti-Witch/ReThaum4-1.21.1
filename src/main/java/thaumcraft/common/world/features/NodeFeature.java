package thaumcraft.common.world.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.common.world.biomes.BiomeHandler;
import thaumcraft.init.ModBlocks;

import java.util.ArrayList;
import java.util.List;

/**
 * NodeFeature - World generation feature for aura nodes.
 *
 * Generates aura nodes naturally in the world:
 * - Random wild nodes underground/caves
 * - Pure nodes inside silverwood trees
 * - Sinister nodes in totem circles/eldritch rings
 * - Special nodes near structures (barrows, towers, obelisks)
 *
 * Based on TC4 WorldGenAuraNode logic, adapted for 1.21.1 Feature system.
 */
public class NodeFeature extends Feature<NodeFeature.NodeFeatureConfiguration> {

    public NodeFeature(Codec<NodeFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NodeFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos pos = context.origin();
        RandomSource random = context.random();
        NodeFeatureConfiguration config = context.config();

        // Check if we should generate here based on config
        if (random.nextFloat() > config.chance()) {
            return false;
        }

        // Find suitable Y level for node
        int y = findSuitableY(level, pos, random, config);
        if (y <= level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) {
            return false;
        }

        BlockPos nodePos = new BlockPos(pos.getX(), y, pos.getZ());

        // Check if position is valid (air or replaceable)
        BlockState state = level.getBlockState(nodePos);
        if (!state.isAir() && !state.canBeReplaced()) {
            return false;
        }

        // Create the node
        createNodeAt(level, nodePos, random, config);
        return true;
    }

    /**
     * Find a suitable Y level for the node.
     * Prefers caves/underground areas.
     */
    private int findSuitableY(WorldGenLevel level, BlockPos pos, RandomSource random, NodeFeatureConfiguration config) {
        int baseY = pos.getY();

        if (config.preferCaves()) {
            // Look for air pockets underground (caves)
            for (int attempt = 0; attempt < 48; attempt++) {
                int y = baseY + random.nextInt(config.verticalRange()) - config.verticalRange() / 2;
                if (y <= level.getMinBuildHeight() + 5 || y >= level.getMaxBuildHeight() - 5) continue;

                BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
                if (level.isEmptyBlock(checkPos) && !level.isEmptyBlock(checkPos.below())) {
                    // Found a cave floor/ceiling
                    return y;
                }
            }
        }

        // Fallback: random Y in range
        return baseY + random.nextInt(config.verticalRange()) - config.verticalRange() / 2;
    }

    /**
     * Create a random node at the specified position outside world generation
     * (e.g. the creative-mode node item - TC4 BlockAiry.onBlockPlacedBy calls
     * ThaumcraftWorldGenerator.createRandomNodeAt).
     */
    public static void createRandomNodeAt(LevelAccessor level, BlockPos pos, RandomSource random,
                                          boolean silverwood, boolean sinister, boolean small) {
        createNodeAt(level, pos, random,
                new NodeFeatureConfiguration(1.0F, 0, false, 0.1F, silverwood, sinister, small));
    }

    /**
     * Create a node at the specified position with configuration.
     * This is the main logic ported from TC4 ThaumcraftWorldGenerator.createRandomNodeAt
     */
    public static void createNodeAt(LevelAccessor level, BlockPos pos, RandomSource random, NodeFeatureConfiguration config) {
        // Initialize aspect lists if needed
        if (basicAspects.isEmpty()) {
            initializeAspectLists();
        }

        // Determine node type
        NodeType type = determineNodeType(random, config);

        // Determine node modifier
        NodeModifier modifier = determineNodeModifier(random, config);

        // Get biome influence
        Holder<Biome> biome = level.getBiome(pos);
        int biomeAura = getBiomeAura(biome);

        // Check for taint biome conversion
        if (type != NodeType.PURE && isTaintBiome(biome)) {
            biomeAura = (int) (biomeAura * 1.5f);
            if (random.nextBoolean()) {
                type = NodeType.TAINTED;
                biomeAura = (int) (biomeAura * 1.5f);
            }
        }

        // Silverwood/small nodes have reduced aura
        if (config.silverwood() || config.small()) {
            biomeAura /= 4;
        }
        // Guard against a zero/negative biome aura (random.nextInt(0) throws)
        if (biomeAura < 10) {
            biomeAura = 10;
        }

        // Calculate total vis value
        int value = random.nextInt(biomeAura / 2) + biomeAura / 2;

        // Build aspect list
        AspectList al = buildAspectList(random, type, biome, value, config);

        // Apply environmental influences
        applyEnvironmentalInfluences(level, pos, random, al);

        // Normalize and scale aspects
        normalizeAspects(random, al, value);

        // Place the node block and initialize TileEntity
        placeNodeBlock(level, pos, type, modifier, al);
    }

    private static List<Aspect> basicAspects = new ArrayList<>();
    private static List<Aspect> complexAspects = new ArrayList<>();

    private static void initializeAspectLists() {
        for (Aspect as : Aspect.aspects.values()) {
            if (as.getComponents() != null && as.getComponents().length == 2) {
                complexAspects.add(as);
            } else {
                basicAspects.add(as);
            }
        }
    }

    private static NodeType determineNodeType(RandomSource random, NodeFeatureConfiguration config) {
        if (config.silverwood()) {
            return NodeType.PURE;
        }
        if (config.sinister()) {
            return NodeType.DARK;
        }

        // Check for special node chance (TC4 NodeTypePickers: DARK/UNSTABLE/PURE
        // share three of ten rolls each, HUNGRY gets the last one)
        if (random.nextFloat() < config.specialNodeChance()) {
            int roll = random.nextInt(10);
            if (roll <= 2) return NodeType.DARK;
            if (roll <= 5) return NodeType.UNSTABLE;
            if (roll <= 8) return NodeType.PURE;
            return NodeType.HUNGRY;
        }

        return NodeType.NORMAL;
    }

    private static NodeModifier determineNodeModifier(RandomSource random, NodeFeatureConfiguration config) {
        // TC4 NodeModifierPickers uses specialNodeRarity / 2 (i.e. twice the type
        // chance); do not allow the doubled chance to exceed certainty.
        if (random.nextFloat() < Math.min(1.0F, config.specialNodeChance() * 2.0F)) {
            int roll = random.nextInt(3);
            if (roll == 0) return NodeModifier.BRIGHT;
            if (roll == 1) return NodeModifier.PALE;
            return NodeModifier.FADING;
        }
        return null;
    }

    private static int getBiomeAura(Holder<Biome> biome) {
        // TC4 BiomeHandler.getBiomeAura (default 100); the port stores a
        // 0..2 aura modifier per biome (default 0.5), so scale it to the TC4
        // 100-based range.
        return (int) (BiomeHandler.getAuraModifier(biome) * 200.0F);
    }

    private static boolean isTaintBiome(Holder<Biome> biome) {
        return BiomeHandler.isTaintedBiome(biome);
    }

    private static AspectList buildAspectList(RandomSource random, NodeType type, Holder<Biome> biome, int value, NodeFeatureConfiguration config) {
        AspectList al = new AspectList();

        // Add biome-specific aspect
        Aspect biomeAspect = getRandomBiomeAspect(biome, random);
        if (biomeAspect != null) {
            al.add(biomeAspect, 2);
        } else {
            // Add random compound + primal
            Aspect aa = complexAspects.get(random.nextInt(complexAspects.size()));
            al.add(aa, 1);
            aa = basicAspects.get(random.nextInt(basicAspects.size()));
            al.add(aa, 1);
        }

        // Add 3 random additional aspects
        for (int a = 0; a < 3; a++) {
            if (random.nextBoolean()) {
                if (random.nextFloat() < config.specialNodeChance()) {
                    Aspect aa = complexAspects.get(random.nextInt(complexAspects.size()));
                    al.merge(aa, 1);
                } else {
                    Aspect aa = basicAspects.get(random.nextInt(basicAspects.size()));
                    al.merge(aa, 1);
                }
            }
        }

        // Type-specific aspects
        applyTypeSpecificAspects(random, al, type);

        return al;
    }

    private static void applyTypeSpecificAspects(RandomSource random, AspectList al, NodeType type) {
        switch (type) {
            case HUNGRY -> {
                al.merge(Aspect.HUNGER, 2);
                if (random.nextBoolean()) al.merge(Aspect.GREED, 1);
            }
            case PURE -> {
                if (random.nextBoolean()) al.merge(Aspect.LIFE, 2);
                else al.merge(Aspect.ORDER, 2);
            }
            case DARK -> {
                if (random.nextBoolean()) al.merge(Aspect.DEATH, 1);
                if (random.nextBoolean()) al.merge(Aspect.UNDEAD, 1);
                if (random.nextBoolean()) al.merge(Aspect.ENTROPY, 1);
                if (random.nextBoolean()) al.merge(Aspect.DARKNESS, 1);
            }
            case TAINTED -> {
                al.merge(Aspect.FLUX, 2);
            }
        }
    }

    private static Aspect getRandomBiomeAspect(Holder<Biome> biome, RandomSource random) {
        // TC4 BiomeHandler.getRandomBiomeTag: the biome's associated aspect, or
        // null when the biome has no entry (then a random compound+primal pair
        // is used, as in NodeAspectGenerators.DEFAULT_ASPECT_GENERATOR).
        return BiomeHandler.getBiomeAspect(biome);
    }

    private static void applyEnvironmentalInfluences(LevelAccessor level, BlockPos pos, RandomSource random, AspectList al) {
        int water = 0, lava = 0, stone = 0, foliage = 0;

        // Scan 11x11x11 area around node
        for (int xx = -5; xx <= 5; xx++) {
            for (int yy = -5; yy <= 5; yy++) {
                for (int zz = -5; zz <= 5; zz++) {
                    BlockPos checkPos = pos.offset(xx, yy, zz);
                    BlockState state = level.getBlockState(checkPos);

                    if (state.getFluidState().is(net.minecraft.tags.FluidTags.WATER)) water++;
                    else if (state.getFluidState().is(net.minecraft.tags.FluidTags.LAVA)) lava++;
                    else if (state.is(Blocks.STONE)) stone++;
                    else if (state.is(net.minecraft.tags.BlockTags.LEAVES) || state.is(net.minecraft.tags.BlockTags.LOGS)) foliage++;
                }
            }
        }

        if (water > 100) al.merge(Aspect.WATER, 1);
        if (lava > 100) { al.merge(Aspect.FIRE, 1); al.merge(Aspect.EARTH, 1); }
        if (stone > 500) al.merge(Aspect.EARTH, 1);
        if (foliage > 100) al.merge(Aspect.PLANT, 1);
    }

    private static void normalizeAspects(RandomSource random, AspectList al, int totalValue) {
        int[] spread = new int[al.size()];
        float total = 0f;

        for (int a = 0; a < spread.length; a++) {
            Aspect aspect = al.getAspectsSorted()[a];
            if (al.getAmount(aspect) == 2) {
                spread[a] = 50 + random.nextInt(25);
            } else {
                spread[a] = 25 + random.nextInt(50);
            }
            total += spread[a];
        }

        for (int a = 0; a < spread.length; a++) {
            Aspect aspect = al.getAspectsSorted()[a];
            int amount = (int) ((spread[a] / total) * totalValue);
            al.setAmount(aspect, amount);
        }
    }

    private static void placeNodeBlock(LevelAccessor level, BlockPos pos, NodeType type, NodeModifier modifier, AspectList aspects) {
        // Place the node block
        level.setBlock(pos, ModBlocks.NODE.get().defaultBlockState(), 2);

        // Initialize the tile entity
        if (level.getBlockEntity(pos) instanceof TileNode node) {
            node.setNodeType(type);
            node.setNodeModifier(modifier);
            node.setAspects(aspects);
            node.setChanged();
        }
    }

    // ==================== Configuration ====================

    public record NodeFeatureConfiguration(
            float chance,                    // Chance to attempt generation per chunk (0-1)
            int verticalRange,               // Vertical range to search for placement
            boolean preferCaves,             // Whether to prefer cave locations
            float specialNodeChance,         // Chance for special node types (0-1)
            boolean silverwood,              // Force pure node (for silverwood trees)
            boolean sinister,                // Force dark node (for totems/eldritch)
            boolean small                    // Small node (reduced vis)
    ) implements net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration {

        public static final Codec<NodeFeatureConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.fieldOf("chance").orElse(0.05f).forGetter(NodeFeatureConfiguration::chance),
                Codec.INT.fieldOf("vertical_range").orElse(64).forGetter(NodeFeatureConfiguration::verticalRange),
                Codec.BOOL.fieldOf("prefer_caves").orElse(true).forGetter(NodeFeatureConfiguration::preferCaves),
                Codec.FLOAT.fieldOf("special_node_chance").orElse(0.1f).forGetter(NodeFeatureConfiguration::specialNodeChance),
                Codec.BOOL.fieldOf("silverwood").orElse(false).forGetter(NodeFeatureConfiguration::silverwood),
                Codec.BOOL.fieldOf("sinister").orElse(false).forGetter(NodeFeatureConfiguration::sinister),
                Codec.BOOL.fieldOf("small").orElse(false).forGetter(NodeFeatureConfiguration::small)
        ).apply(instance, NodeFeatureConfiguration::new));
    }
}
