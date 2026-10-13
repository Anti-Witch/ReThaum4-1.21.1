package thaumcraft.common.blocks.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.tiles.devices.TileInfernalFurnace;
import thaumcraft.init.ModBlockEntities;

import javax.annotation.Nullable;

/**
 * Infernal Furnace - A magical furnace powered by aura.
 * 
 * Features:
 * - Smelts items without fuel
 * - Bonus outputs with attached bellows
 * - Uses vis from aura for speed boost
 * - Ejects smelted items automatically
 * - Large multiblock structure rendered with special renderer
 */
public class BlockInfernalFurnace extends Block implements EntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    // The furnace is larger than a single block visually
    private static final VoxelShape SHAPE = Shapes.box(0, 0, 0, 1, 1.5, 1);

    public BlockInfernalFurnace() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.NETHER)
                .strength(5.0f, 100.0f)
                .sound(SoundType.STONE)
                .lightLevel(state -> 15)
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
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // Rendered by BlockEntityRenderer
        return RenderShape.INVISIBLE;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof TileInfernalFurnace furnace) {
            ItemStack heldItem = player.getItemInHand(player.getUsedItemHand());
            if (!heldItem.isEmpty()) {
                // Try to add item to furnace
                ItemStack remaining = furnace.addItemsToInventory(heldItem.copy());
                if (remaining.isEmpty() || remaining.getCount() < heldItem.getCount()) {
                    if (!player.getAbilities().instabuild) {
                        heldItem.setCount(remaining.isEmpty() ? 0 : remaining.getCount());
                    }
                    return InteractionResult.CONSUME;
                }
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof TileInfernalFurnace furnace) {
                // Drop all items in furnace
                for (int i = 0; i < furnace.getContainerSize(); i++) {
                    ItemStack stack = furnace.getItem(i);
                    if (!stack.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                    }
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Direction facing = state.getValue(FACING);
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.1;
        double z = pos.getZ() + 0.5;

        // Offset towards facing direction (where items come out)
        Direction opposite = facing.getOpposite();
        double offsetX = opposite.getStepX() * 0.6;
        double offsetZ = opposite.getStepZ() * 0.6;

        // Fire and smoke particles from the mouth
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.LARGE_SMOKE,
                    x + offsetX + (random.nextDouble() - 0.5) * 0.3,
                    y + 0.5 + random.nextDouble() * 0.3,
                    z + offsetZ + (random.nextDouble() - 0.5) * 0.3,
                    0, 0.02, 0);
        }

        if (random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.FLAME,
                    x + offsetX + (random.nextDouble() - 0.5) * 0.2,
                    y + 0.3 + random.nextDouble() * 0.2,
                    z + offsetZ + (random.nextDouble() - 0.5) * 0.2,
                    0, 0.01, 0);
        }

        // Lava drip from bottom
        if (random.nextInt(10) == 0) {
            level.addParticle(ParticleTypes.DRIPPING_LAVA,
                    x + (random.nextDouble() - 0.5) * 0.5,
                    y,
                    z + (random.nextDouble() - 0.5) * 0.5,
                    0, 0, 0);
        }
    }

    /**
     * Handle items thrown into the furnace by picking them up from nearby.
     */
    public void entityInside(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.Entity entity) {
        if (!level.isClientSide && entity instanceof ItemEntity itemEntity) {
            if (itemEntity.isAlive() && !itemEntity.getItem().isEmpty()) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof TileInfernalFurnace furnace) {
                    ItemStack stack = itemEntity.getItem();
                    ItemStack remaining = furnace.addItemsToInventory(stack.copy());
                    if (remaining.isEmpty()) {
                        itemEntity.discard();
                    } else {
                        itemEntity.setItem(remaining);
                    }
                }
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileInfernalFurnace(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == ModBlockEntities.INFERNAL_FURNACE.get()) {
            if (level.isClientSide) {
                return null; // No client tick needed for now
            } else {
                return (lvl, pos, st, be) -> TileInfernalFurnace.serverTick(lvl, pos, st, (TileInfernalFurnace) be);
            }
        }
        return null;
    }
}




