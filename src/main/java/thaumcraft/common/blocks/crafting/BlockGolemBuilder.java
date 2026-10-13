package thaumcraft.common.blocks.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.server.level.ServerPlayer;
import thaumcraft.common.blocks.BlockTCDevice;
import thaumcraft.common.tiles.crafting.TileGolemBuilder;
import thaumcraft.init.ModBlockEntities;

import javax.annotation.Nullable;

/**
 * BlockGolemBuilder - Block for the Golem Builder multiblock structure.
 * 
 * The Golem Builder allows players to assemble golems from component parts
 * (material, head, arms, legs, addon). In the original mod, this was a
 * multiblock structure with a press animation.
 * 
 * This simplified version is a single block that opens a GUI for golem assembly.
 */
public class BlockGolemBuilder extends BlockTCDevice {
    
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);
    
    public BlockGolemBuilder() {
        super(Properties.of()
                .strength(2.0f, 6.0f)
                .sound(SoundType.STONE)
                .noOcclusion());
    }

    @Override
    protected boolean hasFacing() {
        return true;
    }

    @Override
    protected boolean hasEnabled() {
        return false;
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        // Use model rendering - invisible would require a TESR
        return RenderShape.MODEL;
    }
    
    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileGolemBuilder(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return createTickerHelper(type, ModBlockEntities.GOLEM_BUILDER.get(), TileGolemBuilder::clientTick);
        } else {
            return createTickerHelper(type, ModBlockEntities.GOLEM_BUILDER.get(), TileGolemBuilder::serverTick);
        }
    }
    
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TileGolemBuilder golemBuilder) {
            // Open the golem builder GUI
            ((ServerPlayer) player).openMenu(golemBuilder, pos);
            return InteractionResult.CONSUME;
        }
        
        return InteractionResult.PASS;
    }
    
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TileGolemBuilder golemBuilder) {
                golemBuilder.dropContents();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}




