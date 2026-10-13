package thaumcraft.common.blocks.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import thaumcraft.api.items.IRechargable;
import thaumcraft.common.tiles.devices.TileRechargePedestal;
import thaumcraft.init.ModBlockEntities;

import javax.annotation.Nullable;

/**
 * BlockRechargePedestal - Recharges vis-powered items from ambient aura.
 * 
 * Place a rechargeable item on the pedestal and it will slowly draw
 * vis from the surrounding aura to recharge the item.
 */
public class BlockRechargePedestal extends Block implements EntityBlock {
    
    // Pedestal shape - narrow base, wider top
    private static final VoxelShape SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);
    
    public BlockRechargePedestal() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
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
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TileRechargePedestal pedestal) {
            ItemStack heldItem = player.getItemInHand(player.getUsedItemHand());
            ItemStack pedestalItem = pedestal.getItem(0);
            
            // If pedestal is empty and player holds rechargeable item, place it
            if (pedestalItem.isEmpty() && heldItem.getItem() instanceof IRechargable) {
                ItemStack toPlace = heldItem.copy();
                toPlace.setCount(1);
                pedestal.setItem(0, toPlace);
                heldItem.shrink(1);
                player.getInventory().setChanged();
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS,
                        0.2f, ((level.random.nextFloat() - level.random.nextFloat()) * 0.7f + 1.0f) * 1.6f);
                return InteractionResult.SUCCESS;
            }
            
            // If pedestal has item, give it to player
            if (!pedestalItem.isEmpty()) {
                if (!player.getInventory().add(pedestalItem)) {
                    player.drop(pedestalItem, false);
                }
                pedestal.setItem(0, ItemStack.EMPTY);
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS,
                        0.2f, ((level.random.nextFloat() - level.random.nextFloat()) * 0.7f + 1.0f) * 1.5f);
                return InteractionResult.SUCCESS;
            }
        }
        
        return InteractionResult.PASS;
    }
    
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TileRechargePedestal pedestal) {
                ItemStack stack = pedestal.getItem(0);
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileRechargePedestal(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                    BlockEntityType<T> type) {
        if (type == ModBlockEntities.RECHARGE_PEDESTAL.get()) {
            if (level.isClientSide) {
                return (lvl, pos, st, be) -> TileRechargePedestal.clientTick(lvl, pos, st, (TileRechargePedestal) be);
            } else {
                return (lvl, pos, st, be) -> TileRechargePedestal.serverTick(lvl, pos, st, (TileRechargePedestal) be);
            }
        }
        return null;
    }
}




