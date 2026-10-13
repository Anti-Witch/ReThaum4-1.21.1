package thaumcraft.common.blocks.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
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
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import thaumcraft.common.tiles.devices.TileWaterJug;
import thaumcraft.init.ModBlockEntities;

import javax.annotation.Nullable;

/**
 * BlockWaterJug (Everfull Urn) - A device that converts vis into water.
 * 
 * - Converts vis from the aura into water (stored internally)
 * - Automatically fills nearby fluid handlers (crucibles, cauldrons, etc.)
 * - Can be filled using buckets or drained using bottles
 * 
 * Ported to 1.20.1
 */
public class BlockWaterJug extends Block implements EntityBlock {

    // Urn shape - narrower than a full block
    private static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 16.0, 13.0);

    public BlockWaterJug() {
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
        if (!(be instanceof TileWaterJug tile)) {
            return InteractionResult.PASS;
        }

        ItemStack heldItem = player.getItemInHand(player.getUsedItemHand());

        // Try standard fluid handler interaction (buckets)
        if (FluidUtil.interactWithFluidHandler(player, player.getUsedItemHand(), tile.tank)) {
            tile.setChanged();
            tile.syncTile(false);
            level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 
                    0.33f, 1.0f + (level.random.nextFloat() - level.random.nextFloat()) * 0.3f);
            return InteractionResult.CONSUME;
        }

        // Handle glass bottle filling
        if (heldItem.is(Items.GLASS_BOTTLE) && tile.tank.getFluidAmount() >= 333) {
            ItemStack waterBottle = PotionContents.createItemStack(Items.POTION, Potions.WATER);
            
            if (!player.getAbilities().instabuild) {
                heldItem.shrink(1);
            }
            
            if (heldItem.isEmpty()) {
                player.setItemInHand(player.getUsedItemHand(), waterBottle);
            } else if (!player.getInventory().add(waterBottle)) {
                player.drop(waterBottle, false);
            }
            
            tile.tank.drain(333, IFluidHandler.FluidAction.EXECUTE);
            tile.setChanged();
            tile.syncTile(false);
            level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 
                    0.33f, 1.0f + (level.random.nextFloat() - level.random.nextFloat()) * 0.3f);
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TileWaterJug tile && tile.isFull()) {
            // Water splash particles when full
            if (random.nextInt(5) == 0) {
                double x = pos.getX() + 0.3 + random.nextDouble() * 0.4;
                double y = pos.getY() + 0.95;
                double z = pos.getZ() + 0.3 + random.nextDouble() * 0.4;
                level.addParticle(ParticleTypes.SPLASH, x, y, z, 0, 0.05, 0);
            }
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TileWaterJug tile) {
            return (int) ((float) tile.getWaterLevel() / TileWaterJug.TANK_CAPACITY * 15);
        }
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileWaterJug(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == ModBlockEntities.WATER_JUG.get()) {
            if (level.isClientSide) {
                return (lvl, p, s, be) -> TileWaterJug.clientTick(lvl, p, s, (TileWaterJug) be);
            } else {
                return (lvl, p, s, be) -> TileWaterJug.serverTick(lvl, p, s, (TileWaterJug) be);
            }
        }
        return null;
    }
}




