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
import thaumcraft.common.tiles.devices.TileFluxScrubber;
import thaumcraft.init.ModBlockEntities;

import javax.annotation.Nullable;

/**
 * Flux Scrubber - Removes flux from the aura and converts it to vitium essentia.
 * 
 * Place this device in areas with high flux to clean the aura.
 * Connect tubes to collect the resulting vitium essentia.
 */
public class BlockFluxScrubber extends Block implements EntityBlock {

    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 14.0, 14.0);

    public BlockFluxScrubber() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(3.0f)
                .sound(SoundType.METAL)
                .noOcclusion()
                .lightLevel(state -> 3)
                .requiresCorrectToolForDrops());
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
        if (be instanceof TileFluxScrubber scrubber && scrubber.isActive()) {
            // Purple particles when active
            if (random.nextInt(3) == 0) {
                double x = pos.getX() + 0.3 + random.nextDouble() * 0.4;
                double y = pos.getY() + 0.5 + random.nextDouble() * 0.5;
                double z = pos.getZ() + 0.3 + random.nextDouble() * 0.4;
                
                // Use portal particles for magical effect
                level.addParticle(ParticleTypes.PORTAL, x, y, z,
                        (random.nextDouble() - 0.5) * 0.1,
                        random.nextDouble() * 0.1,
                        (random.nextDouble() - 0.5) * 0.1);
            }

            // Occasional bubble when processing
            if (random.nextInt(10) == 0 && scrubber.getStoredFlux() > 0) {
                double x = pos.getX() + 0.5;
                double y = pos.getY() + 1.0;
                double z = pos.getZ() + 0.5;
                level.addParticle(ParticleTypes.WITCH, x, y, z, 0, 0.05, 0);
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
        if (be instanceof TileFluxScrubber scrubber) {
            return (int) (scrubber.getStoredFluxPercent() * 15);
        }
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileFluxScrubber(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == ModBlockEntities.FLUX_SCRUBBER.get()) {
            if (level.isClientSide) {
                return (lvl, pos, st, be) -> TileFluxScrubber.clientTick(lvl, pos, st, (TileFluxScrubber) be);
            } else {
                return (lvl, pos, st, be) -> TileFluxScrubber.serverTick(lvl, pos, st, (TileFluxScrubber) be);
            }
        }
        return null;
    }
}




