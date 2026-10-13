package thaumcraft.common.blocks.misc;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.tiles.misc.TileHole;
import thaumcraft.init.ModBlockEntities;

import javax.annotation.Nullable;

/**
 * BlockHole - The portable hole block.
 * Temporarily replaces blocks and restores them after a duration.
 * Stores the original block in its tile entity.
 * 
 * @author Azanor
 * Ported to 1.20.1
 */
public class BlockHole extends BaseEntityBlock {

    @Override
    protected com.mojang.serialization.MapCodec<? extends BlockHole> codec() {
        return net.minecraft.world.level.block.state.BlockBehaviour.simpleCodec(properties -> new BlockHole());
    }
    
    public BlockHole() {
        super(BlockBehaviour.Properties.of()
                .strength(-1.0f, 6000000.0f) // Unbreakable
                .sound(SoundType.WOOL)
                .lightLevel(state -> 11)
                .noLootTable()
                .noOcclusion()
                .pushReaction(PushReaction.BLOCK));
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        // Render as invisible - the hole effect is shown via particles/rendering
        return RenderShape.INVISIBLE;
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Full block shape for selection
        return Shapes.block();
    }
    
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // No collision - entities can pass through
        return Shapes.empty();
    }
    
    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // No visual shape for selection highlight
        return Shapes.empty();
    }
    
    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }
    
    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0f;
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileHole(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return createTickerHelper(type, ModBlockEntities.HOLE.get(), TileHole::clientTick);
        } else {
            return createTickerHelper(type, ModBlockEntities.HOLE.get(), TileHole::serverTick);
        }
    }
    
    /**
     * Create a hole at the specified position.
     * 
     * @param level the world
     * @param pos the position
     * @param originalBlock the block being replaced
     * @param duration how long the hole lasts in ticks
     * @param depth how many blocks deep the hole propagates
     * @param direction the direction of propagation, or null
     * @return true if the hole was created
     */
    public static boolean createHole(Level level, BlockPos pos, BlockState originalBlock, 
                                     short duration, byte depth, @Nullable net.minecraft.core.Direction direction) {
        if (level.isClientSide) return false;
        
        // Don't replace air or existing holes
        if (originalBlock.isAir()) return false;
        
        // Can't replace unbreakable blocks
        if (originalBlock.getDestroySpeed(level, pos) < 0) return false;
        
        // Store any existing tile entity data
        net.minecraft.nbt.CompoundTag tileData = null;
        BlockEntity existingTile = level.getBlockEntity(pos);
        if (existingTile != null) {
            tileData = existingTile.saveWithoutMetadata(existingTile.getLevel().registryAccess());
        }
        
        // Place the hole block
        // We need to get the block from the registry since ModBlocks.HOLE might cause circular reference
        Block holeBlock = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft", "hole"));
        
        if (holeBlock == null) return false;
        
        level.setBlock(pos, holeBlock.defaultBlockState(), 3);
        
        // Configure the tile entity
        if (level.getBlockEntity(pos) instanceof TileHole holeTile) {
            holeTile.configure(originalBlock, duration, depth, direction);
            if (tileData != null) {
                holeTile.setTileEntityCompound(tileData);
            }
            return true;
        }
        
        return false;
    }
}




