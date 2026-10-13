package thaumcraft.common.blocks.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.blocks.BlockTCDevice;
import thaumcraft.common.tiles.crafting.TileCrucible;
import thaumcraft.init.ModBlockEntities;

import javax.annotation.Nullable;

/**
 * The Crucible - a cauldron-like device for alchemical transmutation.
 * Items thrown in are converted to essentia when heated.
 */
public class BlockCrucible extends BlockTCDevice {

    // Collision shapes for the crucible (cauldron-like)
    private static final VoxelShape INSIDE = Block.box(2.0, 4.0, 2.0, 14.0, 16.0, 14.0);
    private static final VoxelShape SHAPE = Shapes.join(
            Shapes.block(),
            Shapes.or(
                    Block.box(0.0, 0.0, 4.0, 16.0, 3.0, 12.0),
                    Block.box(4.0, 0.0, 0.0, 12.0, 3.0, 16.0),
                    Block.box(2.0, 0.0, 2.0, 14.0, 3.0, 14.0),
                    INSIDE
            ),
            BooleanOp.ONLY_FIRST
    );

    private int collisionDelay = 0;

    public BlockCrucible() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(2.0f, 6.0f)
                .sound(SoundType.METAL)
                .noOcclusion());
    }

    @Override
    protected boolean hasFacing() {
        return false;
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
    public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return INSIDE;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof TileCrucible crucible) {
                if (entity instanceof ItemEntity itemEntity) {
                    // Smelt items in heated crucible
                    if (crucible.isHeated()) {
                        crucible.attemptSmelt(itemEntity);
                    }
                } else if (entity instanceof LivingEntity living) {
                    collisionDelay++;
                    if (collisionDelay >= 10) {
                        collisionDelay = 0;
                        // Damage living entities in heated crucible
                        if (crucible.isHeated() && crucible.getTank().getFluidAmount() > 0) {
                            living.hurt(level.damageSources().inFire(), 1.0f);
                            level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 
                                    0.4f, 2.0f + level.random.nextFloat() * 0.4f);
                        }
                    }
                }
            }
        }
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ItemStack heldItem = player.getItemInHand(player.getUsedItemHand());
        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (!(blockEntity instanceof TileCrucible crucible)) {
            return InteractionResult.PASS;
        }

        // Shift + empty player.getUsedItemHand() = dump contents
        if (player.isShiftKeyDown() && heldItem.isEmpty()) {
            if (crucible.aspects.visSize() > 0 || crucible.getTank().getFluidAmount() > 0) {
                crucible.spillAll();
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        // Handle fluid containers (buckets, etc.)
        if (!heldItem.isEmpty()) {
            // Try to fill crucible from held item
            IFluidHandlerItem itemFluidCap = Capabilities.FluidHandler.ITEM.getCapability(heldItem, null);
            if (itemFluidCap != null) {
                IFluidHandlerItem itemHandler = itemFluidCap;
                if (itemHandler != null) {
                    FluidStack contained = itemHandler.getFluidInTank(0);
                    if (contained.getFluid() == Fluids.WATER && contained.getAmount() > 0) {
                        // Fill crucible with water
                        int filled = crucible.getTank().fill(contained, IFluidHandler.FluidAction.SIMULATE);
                        if (filled > 0) {
                            FluidStack drained = itemHandler.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                            crucible.getTank().fill(drained, IFluidHandler.FluidAction.EXECUTE);
                            
                            // Handle bucket specifically
                            if (heldItem.is(Items.WATER_BUCKET)) {
                                if (!player.getAbilities().instabuild) {
                                    player.setItemInHand(player.getUsedItemHand(), new ItemStack(Items.BUCKET));
                                }
                            } else {
                                player.setItemInHand(player.getUsedItemHand(), itemHandler.getContainer());
                            }
                            
                            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
                            crucible.markDirtyAndSync();
                            return InteractionResult.SUCCESS;
                        }
                    } else if (contained.isEmpty() && crucible.getTank().getFluidAmount() > 0) {
                        // Take water from crucible
                        FluidStack inCrucible = crucible.getTank().getFluid();
                        int filled = itemHandler.fill(inCrucible, IFluidHandler.FluidAction.SIMULATE);
                        if (filled > 0) {
                            FluidStack drained = crucible.getTank().drain(filled, IFluidHandler.FluidAction.EXECUTE);
                            itemHandler.fill(drained, IFluidHandler.FluidAction.EXECUTE);
                            
                            // Handle bucket specifically
                            if (heldItem.is(Items.BUCKET)) {
                                if (!player.getAbilities().instabuild) {
                                    player.setItemInHand(player.getUsedItemHand(), new ItemStack(Items.WATER_BUCKET));
                                }
                            } else {
                                player.setItemInHand(player.getUsedItemHand(), itemHandler.getContainer());
                            }
                            
                            level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
                            crucible.markDirtyAndSync();
                            return InteractionResult.SUCCESS;
                        }
                    }
                }
            }
            
            // If crucible is heated and has water, try to smelt the held item
            if (crucible.isHeated() && crucible.getTank().getFluidAmount() > 0) {
                ItemStack result = crucible.attemptSmelt(heldItem.copy(), player.getName().getString());
                if (result == null || result.getCount() < heldItem.getCount()) {
                    // Something was smelted
                    if (!player.getAbilities().instabuild) {
                        if (result == null || result.isEmpty()) {
                            heldItem.shrink(1);
                        } else {
                            player.setItemInHand(player.getUsedItemHand(), result);
                        }
                    }
                    return InteractionResult.SUCCESS;
                }
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof TileCrucible crucible) {
                crucible.spillAll();
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
        if (blockEntity instanceof TileCrucible crucible) {
            float r = (float) crucible.aspects.visSize() / (float) TileCrucible.MAX_ASPECTS;
            return Math.min(15, (int) (r * 15));
        }
        return 0;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(10) == 0) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof TileCrucible crucible) {
                if (crucible.isHeated() && crucible.getTank().getFluidAmount() > 0) {
                    level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(),
                            SoundEvents.LAVA_POP, SoundSource.BLOCKS,
                            0.1f + random.nextFloat() * 0.1f, 1.2f + random.nextFloat() * 0.2f, false);
                }
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileCrucible(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return type == ModBlockEntities.CRUCIBLE.get() ?
                (lvl, pos, st, te) -> TileCrucible.serverTick(lvl, pos, st, (TileCrucible) te) : null;
    }
}




