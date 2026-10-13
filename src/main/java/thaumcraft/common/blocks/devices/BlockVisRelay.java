package thaumcraft.common.blocks.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.tiles.devices.TileVisRelay;
import thaumcraft.init.ModBlockEntities;

import javax.annotation.Nullable;

/**
 * Vis Relay - Distributes vis across a network for distant access.
 * 
 * Place multiple relays to create a vis distribution network.
 * They automatically link to nearby relays and balance vis between them.
 */
public class BlockVisRelay extends Block implements EntityBlock {

    private static final VoxelShape SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 12.0, 12.0);

    public BlockVisRelay() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.QUARTZ)
                .strength(2.0f)
                .sound(SoundType.GLASS)
                .noOcclusion()
                .lightLevel(state -> 7)
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
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TileVisRelay relay) {
            // TODO: Open relay GUI or show vis network info
            // For now, display stored vis in chat
            float vis = relay.getStoredVis();
            int links = relay.getLinkedRelays().size();
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                            String.format("Vis: %.1f/25 | Links: %d", vis, links)),
                    true);
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TileVisRelay relay && relay.getStoredVis() > 0) {
            // Subtle vis particles
            if (random.nextInt(5) == 0) {
                double x = pos.getX() + 0.3 + random.nextDouble() * 0.4;
                double y = pos.getY() + 0.3 + random.nextDouble() * 0.5;
                double z = pos.getZ() + 0.3 + random.nextDouble() * 0.4;
                level.addParticle(ParticleTypes.ENCHANT, x, y, z,
                        0, 0.1, 0);
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
        if (be instanceof TileVisRelay relay) {
            return (int) (relay.getVisPercent() * 15);
        }
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileVisRelay(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == ModBlockEntities.VIS_RELAY.get()) {
            if (level.isClientSide) {
                return (lvl, pos, st, be) -> TileVisRelay.clientTick(lvl, pos, st, (TileVisRelay) be);
            } else {
                return (lvl, pos, st, be) -> TileVisRelay.serverTick(lvl, pos, st, (TileVisRelay) be);
            }
        }
        return null;
    }
}




