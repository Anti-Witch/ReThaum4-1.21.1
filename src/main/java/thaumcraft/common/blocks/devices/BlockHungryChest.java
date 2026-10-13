package thaumcraft.common.blocks.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.menu.HungryChestMenu;
import thaumcraft.common.tiles.devices.TileHungryChest;
import thaumcraft.init.ModBlockEntities;

import javax.annotation.Nullable;

/**
 * Hungry chest that automatically picks up items that touch it.
 * Works like a regular chest but "eats" items that collide with it.
 */
public class BlockHungryChest extends Block implements EntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 14.0, 15.0);

    public BlockHungryChest() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.5f)
                .sound(SoundType.WOOD)
                .noOcclusion());
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // Chest is rendered by tile entity special renderer (animated lid)
        return RenderShape.INVISIBLE;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof TileHungryChest chest) {
            // Open the chest menu
            ((ServerPlayer) player).openMenu(new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("container.thaumcraft.hungry_chest");
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
                    return new HungryChestMenu(containerId, playerInventory, chest);
                }
            }, pos);
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof ItemEntity itemEntity)) {
            return;
        }

        if (itemEntity.isRemoved() || itemEntity.hasPickUpDelay()) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof TileHungryChest chest) {
            ItemStack remaining = chest.insertItem(itemEntity.getItem());
            if (remaining.isEmpty()) {
                itemEntity.discard();
                level.playSound(null, pos, SoundEvents.GENERIC_EAT, SoundSource.BLOCKS, 0.25f, 
                        (level.random.nextFloat() - level.random.nextFloat()) * 0.2f + 1.0f);
            } else if (remaining.getCount() < itemEntity.getItem().getCount()) {
                itemEntity.setItem(remaining);
                level.playSound(null, pos, SoundEvents.GENERIC_EAT, SoundSource.BLOCKS, 0.25f, 
                        (level.random.nextFloat() - level.random.nextFloat()) * 0.2f + 1.0f);
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof TileHungryChest chest) {
                Containers.dropContents(level, pos, chest);
                level.updateNeighbourForOutputSignal(pos, this);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof TileHungryChest chest) {
            return AbstractContainerMenu.getRedstoneSignalFromContainer(chest);
        }
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileHungryChest(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == ModBlockEntities.HUNGRY_CHEST.get()) {
            return (lvl, pos, blockState, tile) -> {
                if (lvl.isClientSide) {
                    TileHungryChest.clientTick(lvl, pos, blockState, (TileHungryChest) tile);
                } else {
                    TileHungryChest.serverTick(lvl, pos, blockState, (TileHungryChest) tile);
                }
            };
        }
        return null;
    }

    @Override
    public boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        super.triggerEvent(state, level, pos, id, param);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity != null && blockEntity.triggerEvent(id, param);
    }
}




