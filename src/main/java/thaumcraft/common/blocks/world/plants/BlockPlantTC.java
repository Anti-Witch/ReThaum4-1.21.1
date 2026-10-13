package thaumcraft.common.blocks.world.plants;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Base class for Thaumcraft plants (shimmerleaf, cinderpearl, vishroom).
 */
public class BlockPlantTC extends BushBlock {

    @Override
    protected com.mojang.serialization.MapCodec<? extends BlockPlantTC> codec() {
        return net.minecraft.world.level.block.state.BlockBehaviour.simpleCodec(properties -> new BlockPlantTC(properties));
    }

    protected static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 13.0, 14.0);

    public BlockPlantTC(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
    /**
     * Creates shimmerleaf - glowing magical plant.
     */
    public static BlockPlantTC createShimmerleaf() {
        return new BlockPlantTC(BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .noCollission()
                .instabreak()
                .sound(SoundType.GRASS)
                .lightLevel(state -> 6)
                .offsetType(BlockBehaviour.OffsetType.XZ));
    }

    /**
     * Creates cinderpearl - fire-resistant magical plant.
     */
    public static BlockPlantTC createCinderpearl() {
        return new BlockPlantTC(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_ORANGE)
                .noCollission()
                .instabreak()
                .sound(SoundType.GRASS)
                .lightLevel(state -> 8)
                .offsetType(BlockBehaviour.OffsetType.XZ));
    }

    /**
     * Creates vishroom - magical mushroom that grows in dark places.
     */
    public static BlockPlantTC createVishroom() {
        return new BlockPlantTC(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .noCollission()
                .instabreak()
                .sound(SoundType.GRASS)
                .lightLevel(state -> 3)
                .offsetType(BlockBehaviour.OffsetType.XZ)) {
        };
    }
}




