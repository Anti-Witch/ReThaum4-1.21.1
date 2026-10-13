package thaumcraft.common.world.structures;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import thaumcraft.init.ModStructures;

import java.util.Optional;

/**
 * BarrowStructure - Ancient burial mound structure with loot and spawners.
 * 
 * Barrows are small dungeon-like structures that appear as grassy mounds
 * with a stone interior containing:
 * - A central chamber with chest and loot
 * - Side chambers with monster spawners (skeleton, zombie)
 * - Thaumcraft loot crates/urns
 * - Entrance corridor with iron bars
 * 
 * Structure placement preferences:
 * - Plains, forests, meadows
 * - Flat-ish terrain
 * - Away from villages/other structures
 */
public class BarrowStructure extends Structure {
    
    public static final MapCodec<BarrowStructure> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    settingsCodec(instance),
                    StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
                    Codec.intRange(1, 7).fieldOf("size").forGetter(s -> s.maxDepth)
            ).apply(instance, BarrowStructure::new)
    );
    
    private final Holder<StructureTemplatePool> startPool;
    private final int maxDepth;
    
    public BarrowStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool, int maxDepth) {
        super(settings);
        this.startPool = startPool;
        this.maxDepth = maxDepth;
    }
    
    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        // Get world info
        ChunkPos chunkPos = context.chunkPos();
        ChunkGenerator chunkGenerator = context.chunkGenerator();
        LevelHeightAccessor heightAccessor = context.heightAccessor();
        RandomState randomState = context.randomState();
        RandomSource random = context.random();
        
        // Get the center position of the chunk
        int x = chunkPos.getMiddleBlockX();
        int z = chunkPos.getMiddleBlockZ();
        
        // Get the surface height
        int surfaceY = chunkGenerator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, heightAccessor, randomState);
        
        // Check if terrain is suitable (not underwater, not too high/low)
        if (surfaceY < 60 || surfaceY > 200) {
            return Optional.empty();
        }
        
        // Check for relatively flat terrain - sample corners
        int[] cornerHeights = new int[4];
        cornerHeights[0] = chunkGenerator.getBaseHeight(x - 8, z - 8, Heightmap.Types.WORLD_SURFACE_WG, heightAccessor, randomState);
        cornerHeights[1] = chunkGenerator.getBaseHeight(x + 8, z - 8, Heightmap.Types.WORLD_SURFACE_WG, heightAccessor, randomState);
        cornerHeights[2] = chunkGenerator.getBaseHeight(x - 8, z + 8, Heightmap.Types.WORLD_SURFACE_WG, heightAccessor, randomState);
        cornerHeights[3] = chunkGenerator.getBaseHeight(x + 8, z + 8, Heightmap.Types.WORLD_SURFACE_WG, heightAccessor, randomState);
        
        int maxVariance = 0;
        for (int height : cornerHeights) {
            maxVariance = Math.max(maxVariance, Math.abs(height - surfaceY));
        }
        
        // Reject if terrain is too steep
        if (maxVariance > 4) {
            return Optional.empty();
        }
        
        // Check if the surface block is valid (grass, dirt, or stone)
        NoiseColumn column = chunkGenerator.getBaseColumn(x, z, heightAccessor, randomState);
        BlockState surfaceBlock = column.getBlock(surfaceY);
        if (!isValidSurfaceBlock(surfaceBlock)) {
            return Optional.empty();
        }
        
        // Position the structure at ground level, slightly underground
        // The mound rises above, chamber is below
        BlockPos structurePos = new BlockPos(x, surfaceY - 8, z);
        
        // Use jigsaw placement for the structure
        return JigsawPlacement.addPieces(
                context,
                this.startPool,
                Optional.empty(),
                this.maxDepth,
                structurePos,
                false,
                Optional.empty(),
                80, // max distance from start
                PoolAliasLookup.EMPTY,
                DimensionPadding.ZERO,
                LiquidSettings.IGNORE_WATERLOGGING
        );
    }
    
    /**
     * Check if the block is a valid surface type for barrow placement.
     */
    private boolean isValidSurfaceBlock(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) ||
               state.is(Blocks.DIRT) ||
               state.is(Blocks.PODZOL) ||
               state.is(Blocks.COARSE_DIRT) ||
               state.is(Blocks.MYCELIUM) ||
               state.is(Blocks.STONE);
    }
    
    @Override
    public StructureType<?> type() {
        return ModStructures.BARROW.get();
    }
}




