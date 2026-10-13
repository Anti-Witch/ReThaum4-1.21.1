package thaumcraft.common.blocks.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.MenuProvider;
import thaumcraft.common.tiles.crafting.TileArcaneWorkbench;
import thaumcraft.init.ModBlocks;

/**
 * Arcane Workbench Charger - An accessory block placed on top of the Arcane Workbench.
 * 
 * This block can only be placed on top of an Arcane Workbench.
 * When the workbench below is removed, this block drops as an item.
 * 
 * Right-clicking opens the workbench GUI below.
 * The charger enhances the workbench's ability to draw vis from the aura.
 */
public class BlockArcaneWorkbenchCharger extends Block {
    
    // TC4: BlockMetalDevice meta 2 -> setBlockBounds(W5, 0.5F, W5, W11, 1.0F, W11)
    private static final VoxelShape SHAPE = Block.box(5, 8, 5, 11, 16, 11);
    
    public BlockArcaneWorkbenchCharger() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(1.25f, 10.0f)
                .sound(SoundType.WOOD)
                .noOcclusion());
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
    
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }
    
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos below = context.getClickedPos().below();
        if (!context.getLevel().getBlockState(below).isFaceSturdy(context.getLevel(), below, Direction.UP)) {
            return null; // Can't place on non-sturdy face
        }
        return defaultBlockState();
    }
    
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        
        // Notify the workbench below that a charger has been added
        BlockEntity be = level.getBlockEntity(pos.below());
        if (be instanceof TileArcaneWorkbench workbench) {
            workbench.syncTile(true);
        }
        // TileFocalManipulator also supports syncTile via its parent class
    }
    
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                   LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        if (direction == Direction.DOWN && !canSurvive(state, level, currentPos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
    }
    
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide) {
            if (!canSurvive(state, level, pos)) {
                level.destroyBlock(pos, true);
            }
        }
    }
    
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        
        // Open the workbench GUI below
        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);
        
        if (belowState.getBlock() instanceof BlockArcaneWorkbench || belowState.is(ModBlocks.ARCANE_WORKBENCH.get())) {
            // Open arcane workbench GUI
            BlockEntity be = level.getBlockEntity(belowPos);
            if (be instanceof TileArcaneWorkbench workbench && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(workbench, belowPos);
                return InteractionResult.CONSUME;
            }
        }
        
        return InteractionResult.PASS;
    }
    
    /**
     * Checks if a block state is a valid base for the charger.
     */
    private boolean isValidBase(BlockState state) {
        return state.getBlock() instanceof BlockArcaneWorkbench ||
               state.is(ModBlocks.ARCANE_WORKBENCH.get());
    }
    
    // Factory method
    public static BlockArcaneWorkbenchCharger create() {
        return new BlockArcaneWorkbenchCharger();
    }
}




