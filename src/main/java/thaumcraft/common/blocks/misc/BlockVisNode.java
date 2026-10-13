package thaumcraft.common.blocks.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.tiles.misc.TileVisNodeImpl;

/**
 * BlockVisNode - The vis network node block.
 * A transparent, non-solid block that contains a TileVisNode entity.
 * Connects to other vis nodes to form a vis network.
 */
public class BlockVisNode extends Block implements EntityBlock {

    public static final IntegerProperty NODE_TYPE = IntegerProperty.create("node_type", 0, 3);
    // 0 = Energized Node, 1 = Transducer, 2 = Relay, 3 = Condenser

    private static final VoxelShape SHAPE = Shapes.block();

    public BlockVisNode() {
        super(BlockBehaviour.Properties.of()
                .noCollission()
                .instabreak()
                .noOcclusion()
                .lightLevel(state -> 8)
                .sound(net.minecraft.world.level.block.SoundType.GLASS)
                .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY));
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NODE_TYPE, 0));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileVisNodeImpl(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return (lvl, pos, st, be) -> {
            if (be instanceof TileVisNodeImpl node) {
                node.tick();
            }
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NODE_TYPE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState();
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof TileVisNodeImpl node) {
            // Set node type based on placement context or item used
            // For now default to energized node
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock()) {
            if (level.getBlockEntity(pos) instanceof TileVisNodeImpl node) {
                node.setRemoved();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return false; // Can only be removed with wand
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0f;
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return 8;
    }
}
