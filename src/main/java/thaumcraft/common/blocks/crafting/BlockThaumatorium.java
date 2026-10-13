package thaumcraft.common.blocks.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.tiles.crafting.TileThaumatorium;
import thaumcraft.init.ModBlockEntities;
import thaumcraft.init.ModBlocks;

import javax.annotation.Nullable;

/**
 * Thaumatorium - Automated alchemical crafting machine.
 * 
 * Uses essentia from connected sources to craft alchemy recipes automatically.
 * Place catalyst items in the input slot and retrieve results from output.
 */
public class BlockThaumatorium extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    // Slightly smaller than a full block
    private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 14.0, 15.0);

    public BlockThaumatorium() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.0f)
                .sound(SoundType.METAL)
                .noOcclusion()
                .requiresCorrectToolForDrops());
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Check if there's room for the top block
        BlockPos above = context.getClickedPos().above();
        Level level = context.getLevel();
        if (!level.getBlockState(above).canBeReplaced(context)) {
            return null; // Can't place if there's no room for the top
        }
        
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
    
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        
        // Place the top block
        BlockPos above = pos.above();
        if (level.getBlockState(above).canBeReplaced()) {
            level.setBlock(above, ModBlocks.THAUMATORIUM_TOP.get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }
    
    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState,
                                   LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        // If the top block is removed, break this block too
        if (facing == Direction.UP && !facingState.is(ModBlocks.THAUMATORIUM_TOP.get())) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, facing, facingState, level, currentPos, facingPos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // Has animated model rendered by TESR
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof TileThaumatorium thaumatorium && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(thaumatorium, pos);
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof TileThaumatorium thaumatorium) {
                // Drop inventory contents
                for (int i = 0; i < thaumatorium.getContainerSize(); i++) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                            thaumatorium.getItem(i));
                }
            }
            
            // Remove the top block
            BlockPos above = pos.above();
            if (level.getBlockState(above).is(ModBlocks.THAUMATORIUM_TOP.get())) {
                level.removeBlock(above, false);
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
        if (blockEntity instanceof TileThaumatorium thaumatorium) {
            // Output signal based on stored essentia
            int totalStored = thaumatorium.getStoredAspects().visSize();
            return Math.min(15, totalStored / 4);
        }
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileThaumatorium(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == ModBlockEntities.THAUMATORIUM.get()) {
            if (level.isClientSide) {
                return (lvl, pos, st, be) -> TileThaumatorium.clientTick(lvl, pos, st, (TileThaumatorium) be);
            } else {
                return (lvl, pos, st, be) -> TileThaumatorium.serverTick(lvl, pos, st, (TileThaumatorium) be);
            }
        }
        return null;
    }
}




