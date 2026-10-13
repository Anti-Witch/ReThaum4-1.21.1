package thaumcraft.common.blocks.world.plants;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
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
 * Vishroom - A mystical purple mushroom that grows in dark caves.
 * 
 * Properties:
 * - Emits soft purple light
 * - Causes nausea when touched
 * - Shows mystical particle effects
 * - Grows on stone in dark areas (cave plant)
 * - Used in magical recipes and vis battery
 * 
 * Ported to 1.20.1
 */
public class BlockVishroom extends BushBlock {

    @Override
    protected com.mojang.serialization.MapCodec<? extends BlockVishroom> codec() {
        return net.minecraft.world.level.block.state.BlockBehaviour.simpleCodec(properties -> new BlockVishroom());
    }

    protected static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 10.0, 13.0);

    public BlockVishroom() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .noCollission()
                .instabreak()
                .sound(SoundType.GRASS)
                .lightLevel(state -> 5)
                .offsetType(BlockBehaviour.OffsetType.XZ));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        // Can grow on stone, mycelium, or in dark dirt areas
        return state.is(Blocks.STONE) || 
               state.is(Blocks.DEEPSLATE) ||
               state.is(Blocks.MYCELIUM) ||
               state.is(BlockTags.BASE_STONE_OVERWORLD) ||
               state.is(BlockTags.MUSHROOM_GROW_BLOCK);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        
        // Check for valid surface
        if (!mayPlaceOn(belowState, level, below)) {
            return false;
        }
        
        // Vishroom prefers dark areas (like mushrooms)
        return level.getRawBrightness(pos, 0) < 13;
    }
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        // Cause nausea when entities walk through
        if (!level.isClientSide && entity instanceof LivingEntity living) {
            if (level.random.nextInt(5) == 0) {
                living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            // Purple mystical motes floating around the mushroom
            double x = pos.getX() + 0.5 + (random.nextFloat() - random.nextFloat()) * 0.4;
            double y = pos.getY() + 0.3;
            double z = pos.getZ() + 0.5 + (random.nextFloat() - random.nextFloat()) * 0.4;
            
            // Purple/enchanted particles
            level.addParticle(ParticleTypes.WITCH,
                    x, y, z,
                    0.0, 0.0, 0.0);
        }
    }
}




