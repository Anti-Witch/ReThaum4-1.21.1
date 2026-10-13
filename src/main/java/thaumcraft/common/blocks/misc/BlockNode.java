package thaumcraft.common.blocks.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
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
import thaumcraft.api.nodes.NodeType;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.init.ModBlockEntities;

/**
 * BlockNode - The aura node block.
 * This is a transparent, non-solid block that contains a TileNode entity.
 * The node's appearance (color, particles) is handled by the TileEntity renderer.
 */
public class BlockNode extends Block implements EntityBlock {

    public static final IntegerProperty NODE_TYPE = IntegerProperty.create("node_type", 0, 5);
    public static final IntegerProperty NODE_MODIFIER = IntegerProperty.create("node_modifier", 0, 2);

    private static final VoxelShape SHAPE = Shapes.block();

    public BlockNode() {
        super(BlockBehaviour.Properties.of()
                .noCollission()
                .instabreak()
                .noOcclusion()
                .lightLevel(state -> 10) // Nodes emit light
                .sound(net.minecraft.world.level.block.SoundType.GLASS)
                .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY));
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NODE_TYPE, NodeType.NORMAL.ordinal())
                .setValue(NODE_MODIFIER, 0));
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
        return new TileNode(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == ModBlockEntities.NODE.get()) {
            if (level.isClientSide) {
                return (lvl, pos, st, be) -> TileNode.clientTick(lvl, pos, st, (TileNode) be);
            } else {
                return (lvl, pos, st, be) -> TileNode.serverTick(lvl, pos, st, (TileNode) be);
            }
        }
        return null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NODE_TYPE, NODE_MODIFIER);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState();
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof TileNode node) {
            // Node type and modifier will be set by world generator or player action
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock()) {
            if (level.getBlockEntity(pos) instanceof TileNode node) {
                node.removeNode();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**
     * TC4 BlockAiry.onBlockHarvested: destroying a node drops wisp essence -
     * one item per 10 vis of each aspect (minimum 5 vis yields one), each
     * carrying 2 points of that aspect.
     */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.player.Player player) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof TileNode node) {
            thaumcraft.api.aspects.AspectList aspects = node.getAspects();
            if (aspects != null && aspects.size() > 0) {
                for (thaumcraft.api.aspects.Aspect aspect : aspects.getAspects()) {
                    if (aspect == null) continue;
                    int amount = aspects.getAmount(aspect);
                    if (amount < 5) continue;
                    int drops = amount / 10;
                    if (drops <= 0) {
                        drops = 1;
                    }
                    for (int a = 0; a < drops; a++) {
                        ItemStack ess = thaumcraft.common.items.resources.ItemWispEssence.createStack(
                                thaumcraft.init.ModItems.WISP_ESSENCE.get(), aspect, 2);
                        popResource(level, pos, ess);
                    }
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return false; // Nodes can only be removed with wand or jar
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0f;
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return 10; // Bright light from nodes
    }
}
