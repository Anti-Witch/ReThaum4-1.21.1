package thaumcraft.api.expands.worldgen.node;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;

import java.util.List;

/**
 * NodeGenerationManager - Handles aura node world generation.
 * This is the API interface for generating nodes in the world.
 */
public class NodeGenerationManager {

    /**
     * Generate wild nodes in a chunk.
     * Called during world generation.
     *
     * @param level the world generation level
     * @param random random source
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @param auraGen whether a structure node was already generated
     * @param newGen whether this is new generation (vs retrogen)
     * @return true if a node was generated
     */
    public static boolean generateWildNodes(WorldGenLevel level, RandomSource random, int chunkX, int chunkZ, boolean auraGen, boolean newGen) {
        // Default implementation - can be overridden by mods
        return false;
    }

    /**
     * Create a random node at the specified position.
     *
     * @param level the world
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @param random random source
     * @param silverwood whether this is a silverwood tree node (pure)
     * @param eerie whether this is an eerie/sinister node
     * @param small whether this is a small node (reduced vis)
     */
    public static void createRandomNodeAt(WorldGenLevel level, int x, int y, int z, RandomSource random, boolean silverwood, boolean eerie, boolean small) {
        // Implementation provided by Thaumcraft internals
    }

    /**
     * Create a node with specific parameters at the specified position.
     *
     * @param level the world
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @param nodeType the node type
     * @param nodeModifier the node modifier
     * @param aspects the aspects for the node
     */
    public static void createNodeAt(WorldGenLevel level, int x, int y, int z, NodeType nodeType, NodeModifier nodeModifier, AspectList aspects) {
        // Implementation provided by Thaumcraft internals
    }

    /**
     * Get the list of basic (primal) aspects.
     */
    public static List<Aspect> getBasicAspects() {
        return Aspect.getPrimalAspects();
    }

    /**
     * Get the list of complex (compound) aspects.
     */
    public static List<Aspect> getComplexAspects() {
        return Aspect.getCompoundAspects();
    }

    /**
     * Get a random biome aspect for the given biome.
     */
    public static Aspect getRandomBiomeAspect(WorldGenLevel level, BlockPos pos, RandomSource random) {
        // Default implementation
        return Aspect.getPrimalAspects().get(random.nextInt(6));
    }
}