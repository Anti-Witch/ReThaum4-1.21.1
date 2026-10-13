package thaumcraft.common.blocks.world.plants;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Shimmerleaf - A magical glowing plant that spawns near silverwood trees.
 * 
 * Properties:
 * - Emits soft blue/cyan light
 * - Shows wispy particle effects
 * - Drops quicksilver when harvested
 * - Grows on grass and dirt
 * 
 * Ported to 1.20.1
 */
public class BlockShimmerleaf extends BushBlock {

    @Override
    protected com.mojang.serialization.MapCodec<? extends BlockShimmerleaf> codec() {
        return net.minecraft.world.level.block.state.BlockBehaviour.simpleCodec(properties -> new BlockShimmerleaf());
    }

    protected static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 13.0, 14.0);

    public BlockShimmerleaf() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .noCollission()
                .instabreak()
                .sound(SoundType.GRASS)
                .lightLevel(state -> 6)
                .offsetType(BlockBehaviour.OffsetType.XZ));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || 
               state.is(Blocks.COARSE_DIRT) || state.is(Blocks.PODZOL) ||
               state.is(BlockTags.DIRT);
    }
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            // Wispy cyan/blue motes floating up from the plant
            double x = pos.getX() + 0.5 + random.nextGaussian() * 0.1;
            double y = pos.getY() + 0.4 + random.nextGaussian() * 0.1;
            double z = pos.getZ() + 0.5 + random.nextGaussian() * 0.1;
            
            // Use end rod particles for a mystical effect
            level.addParticle(ParticleTypes.END_ROD,
                    x, y, z,
                    random.nextGaussian() * 0.01,
                    random.nextGaussian() * 0.01 + 0.02,
                    random.nextGaussian() * 0.01);
        }
    }
}




