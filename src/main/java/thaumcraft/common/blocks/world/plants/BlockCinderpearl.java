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
 * Cinderpearl - A fiery magical plant that grows in deserts and near heat.
 * 
 * Properties:
 * - Emits warm orange light
 * - Produces flame and smoke particles
 * - Grows on sand and terracotta
 * - Used in fire-related recipes
 * 
 * Ported to 1.20.1
 */
public class BlockCinderpearl extends BushBlock {

    @Override
    protected com.mojang.serialization.MapCodec<? extends BlockCinderpearl> codec() {
        return net.minecraft.world.level.block.state.BlockBehaviour.simpleCodec(properties -> new BlockCinderpearl());
    }

    protected static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 13.0, 14.0);

    public BlockCinderpearl() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_ORANGE)
                .noCollission()
                .instabreak()
                .sound(SoundType.GRASS)
                .lightLevel(state -> 8)
                .offsetType(BlockBehaviour.OffsetType.XZ));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(Blocks.SAND) || 
               state.is(Blocks.RED_SAND) ||
               state.is(Blocks.DIRT) || 
               state.is(Blocks.TERRACOTTA) ||
               state.is(BlockTags.TERRACOTTA) ||
               state.is(BlockTags.SAND);
    }
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextBoolean()) {
            // Fire and smoke particles rising from the plant
            double x = pos.getX() + 0.5 + (random.nextFloat() - random.nextFloat()) * 0.1;
            double y = pos.getY() + 0.6 + (random.nextFloat() - random.nextFloat()) * 0.1;
            double z = pos.getZ() + 0.5 + (random.nextFloat() - random.nextFloat()) * 0.1;
            
            level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.0, 0.0);
            level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.0, 0.0);
        }
    }
}




