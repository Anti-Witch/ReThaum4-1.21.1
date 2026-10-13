package thaumcraft.common.blocks.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.tiles.devices.TileSpa;
import thaumcraft.init.ModBlockEntities;

import javax.annotation.Nullable;

/**
 * Sanitizing Spa - A pool that cleanses negative effects and warp.
 * 
 * Players standing in the spa will have negative potion effects removed
 * and temporary warp slowly cleansed. Powered by aura vis.
 */
public class BlockSpa extends Block implements EntityBlock {

    // Pool shape - lower than a full block
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 10.0, 16.0);

    public BlockSpa() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_BLUE)
                .strength(2.0f)
                .sound(SoundType.STONE)
                .noOcclusion());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TileSpa spa && spa.isPowered()) {
            // Bubble particles when powered
            if (random.nextInt(5) == 0) {
                double x = pos.getX() + 0.2 + random.nextDouble() * 0.6;
                double y = pos.getY() + 0.5 + random.nextDouble() * 0.2;
                double z = pos.getZ() + 0.2 + random.nextDouble() * 0.6;
                level.addParticle(ParticleTypes.BUBBLE, x, y, z, 0, 0.05, 0);
            }
            
            // Occasional splash
            if (random.nextInt(20) == 0) {
                double x = pos.getX() + 0.3 + random.nextDouble() * 0.4;
                double y = pos.getY() + 0.6;
                double z = pos.getZ() + 0.3 + random.nextDouble() * 0.4;
                level.addParticle(ParticleTypes.SPLASH, x, y, z, 0, 0.1, 0);
            }
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TileSpa spa) {
            return (int) (spa.getChargePercent() * 15);
        }
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileSpa(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == ModBlockEntities.SPA.get()) {
            if (level.isClientSide) {
                return (lvl, pos, st, be) -> TileSpa.clientTick(lvl, pos, st, (TileSpa) be);
            } else {
                return (lvl, pos, st, be) -> TileSpa.serverTick(lvl, pos, st, (TileSpa) be);
            }
        }
        return null;
    }
}




