package thaumcraft.common.blocks.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.blocks.BlockTC;

/**
 * Table blocks for crafting stations and decoration.
 */
public class BlockTable extends BlockTC {

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0.0, 12.0, 0.0, 16.0, 16.0, 16.0),  // Table top
            Block.box(1.0, 0.0, 1.0, 4.0, 12.0, 4.0),     // Leg 1
            Block.box(12.0, 0.0, 1.0, 15.0, 12.0, 4.0),   // Leg 2
            Block.box(1.0, 0.0, 12.0, 4.0, 12.0, 15.0),   // Leg 3
            Block.box(12.0, 0.0, 12.0, 15.0, 12.0, 15.0)  // Leg 4
    );

    public BlockTable(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /**
     * Creates a wooden table.
     */
    public static BlockTable createWooden() {
        return new BlockTable(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.0f)
                .sound(SoundType.WOOD)
                .noOcclusion());
    }

    /**
     * Creates a stone table.
     */
    public static BlockTable createStone() {
        return new BlockTable(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(2.5f)
                .sound(SoundType.STONE)
                .noOcclusion()
                .requiresCorrectToolForDrops());
    }
}




