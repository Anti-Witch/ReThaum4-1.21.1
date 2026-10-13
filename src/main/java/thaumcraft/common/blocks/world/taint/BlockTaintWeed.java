package thaumcraft.common.blocks.world.taint;


import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.init.ModBlocks;
import thaumcraft.init.ModEffects;

/**
 * BlockTaintWeed - Small tainted plant that grows on tainted ground.
 *
 * Properties:
 * - Grows only on taint blocks (soil, rock, crust, log)
 * - Spreads slowly on tainted soil via random ticks
 * - Applies flux taint effect when stepped on
 */
public class BlockTaintWeed extends BushBlock {

    @Override
    protected MapCodec<? extends BlockTaintWeed> codec() {
        return BlockBehaviour.simpleCodec(properties -> new BlockTaintWeed());
    }

    protected static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 14.0, 14.0);

    public BlockTaintWeed() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .noCollission()
                .instabreak()
                .sound(SoundType.GRASS)
                .offsetType(BlockBehaviour.OffsetType.XZ));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        if (ModBlocks.TAINT_SOIL != null && state.is(ModBlocks.TAINT_SOIL.get())) return true;
        if (ModBlocks.TAINT_ROCK != null && state.is(ModBlocks.TAINT_ROCK.get())) return true;
        if (ModBlocks.TAINT_CRUST != null && state.is(ModBlocks.TAINT_CRUST.get())) return true;
        return ModBlocks.TAINT_LOG != null && state.is(ModBlocks.TAINT_LOG.get());
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity living &&
                !living.isInvertedHealAndHarm() && level.random.nextInt(250) == 0) {
            if (ModEffects.FLUX_TAINT != null) {
                living.addEffect(new MobEffectInstance(ModEffects.FLUX_TAINT, 200, 0, false, true));
            }
        }
        super.stepOn(level, pos, state, entity);
    }
}
