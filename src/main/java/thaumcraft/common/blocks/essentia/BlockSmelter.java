package thaumcraft.common.blocks.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.common.tiles.essentia.TileSmelter;
import thaumcraft.init.ModBlockEntities;

import javax.annotation.Nullable;

/**
 * Essentia smelter (alchemical furnace) that breaks down items into essentia.
 * The main block of the smelter multiblock - connects with bellows and alembics.
 */
public class BlockSmelter extends Block implements EntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public BlockSmelter() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.0f)
                .sound(SoundType.METAL)
                .lightLevel(state -> state.getValue(LIT) ? 13 : 0)
                .requiresCorrectToolForDrops());
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(LIT, false);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof TileSmelter smelter && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(smelter, pos);
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof TileSmelter smelter) {
                // Drop inventory contents
                for (int i = 0; i < smelter.getContainerSize(); i++) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), smelter.getItem(i));
                }
                // Release stored vis as flux
                if (smelter.vis > 0) {
                    thaumcraft.api.aura.AuraHelper.polluteAura(level, pos, smelter.vis / 10, true);
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            Direction facing = state.getValue(FACING);
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.2 + random.nextFloat() * 0.3;
            double z = pos.getZ() + 0.5;
            
            double offset = 0.52;
            double spread = random.nextFloat() * 0.5 - 0.25;
            
            switch (facing) {
                case WEST -> {
                    level.addParticle(ParticleTypes.SMOKE, x - offset, y, z + spread, 0.0, 0.0, 0.0);
                    level.addParticle(ParticleTypes.FLAME, x - offset, y, z + spread, 0.0, 0.0, 0.0);
                }
                case EAST -> {
                    level.addParticle(ParticleTypes.SMOKE, x + offset, y, z + spread, 0.0, 0.0, 0.0);
                    level.addParticle(ParticleTypes.FLAME, x + offset, y, z + spread, 0.0, 0.0, 0.0);
                }
                case NORTH -> {
                    level.addParticle(ParticleTypes.SMOKE, x + spread, y, z - offset, 0.0, 0.0, 0.0);
                    level.addParticle(ParticleTypes.FLAME, x + spread, y, z - offset, 0.0, 0.0, 0.0);
                }
                case SOUTH -> {
                    level.addParticle(ParticleTypes.SMOKE, x + spread, y, z + offset, 0.0, 0.0, 0.0);
                    level.addParticle(ParticleTypes.FLAME, x + spread, y, z + offset, 0.0, 0.0, 0.0);
                }
            }
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        // TODO: Return inventory fill level when TileSmelter is implemented
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileSmelter(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == ModBlockEntities.SMELTER.get()) {
            if (!level.isClientSide) {
                return (lvl, pos, st, be) -> TileSmelter.serverTick(lvl, pos, st, (TileSmelter) be);
            }
        }
        return null;
    }
}




