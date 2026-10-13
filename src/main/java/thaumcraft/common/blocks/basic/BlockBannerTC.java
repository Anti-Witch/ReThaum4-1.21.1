package thaumcraft.common.blocks.basic;
import thaumcraft.common.lib.utils.NBTHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.common.tiles.misc.TileBanner;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * BlockBannerTC - Thaumcraft banner block.
 * 
 * Features:
 * - 16 color variants plus crimson cult banner
 * - Can display aspect symbols when decorated with phials
 * - Can be placed on floor (standing) or wall (hanging)
 * - 16 rotation values for standing banners
 * 
 * Ported from 1.12.2
 */
public class BlockBannerTC extends Block implements EntityBlock {
    
    public static final IntegerProperty ROTATION = BlockStateProperties.ROTATION_16;
    
    private final DyeColor dyeColor;
    
    // Shapes for standing banner
    private static final VoxelShape STANDING_SHAPE = Block.box(4, 0, 4, 12, 32, 12);
    
    // Shapes for wall-mounted banner (by facing)
    private static final VoxelShape WALL_NORTH = Block.box(0, -16, 0, 16, 16, 4);
    private static final VoxelShape WALL_SOUTH = Block.box(0, -16, 12, 16, 16, 16);
    private static final VoxelShape WALL_WEST = Block.box(0, -16, 0, 4, 16, 16);
    private static final VoxelShape WALL_EAST = Block.box(12, -16, 0, 16, 16, 16);
    
    /**
     * Create a colored banner.
     * @param color The dye color, or null for crimson cult banner
     */
    public BlockBannerTC(@Nullable DyeColor color) {
        super(BlockBehaviour.Properties.of()
                .mapColor(color != null ? color.getMapColor() : MapColor.COLOR_RED)
                .strength(1.0f)
                .sound(SoundType.WOOD)
                .noOcclusion()
                .noCollission());
        this.dyeColor = color;
        registerDefaultState(stateDefinition.any().setValue(ROTATION, 0));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROTATION);
    }
    
    @Nullable
    public DyeColor getDyeColor() {
        return dyeColor;
    }
    
    // ==================== Placement ====================
    
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Calculate rotation based on player facing
        int rotation = (int) Math.floor((context.getRotation() + 180.0f) * 16.0f / 360.0f + 0.5) & 15;
        return defaultBlockState().setValue(ROTATION, rotation);
    }
    
    // ==================== Shape ====================
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TileBanner banner) {
            if (!banner.getWall()) {
                return STANDING_SHAPE;
            }
            // Wall-mounted shape based on facing
            int facing = banner.getBannerFacing();
            return switch (facing) {
                case 0 -> WALL_NORTH;  // North
                case 8 -> WALL_SOUTH;  // South
                case 12 -> WALL_WEST;  // West
                case 4 -> WALL_EAST;   // East
                default -> STANDING_SHAPE;
            };
        }
        return STANDING_SHAPE;
    }
    
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }
    
    // ==================== Rendering ====================
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }
    
    // ==================== Block Entity ====================
    
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileBanner(pos, state);
    }
    
    // ==================== Interaction ====================
    
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TileBanner banner && dyeColor != null) {
                ItemStack heldItem = player.getItemInHand(player.getUsedItemHand());
                
                if (player.isShiftKeyDown()) {
                    // Clear aspect
                    banner.setAspect(null);
                    syncBanner(level, pos, state, banner);
                    level.playSound(null, pos, SoundEvents.WOOL_HIT, SoundSource.BLOCKS, 1.0f, 1.0f);
                    return InteractionResult.SUCCESS;
                } else if (heldItem.getItem() instanceof IEssentiaContainerItem container) {
                    // Apply aspect from phial
                    if (container.getAspects(heldItem) != null && container.getAspects(heldItem).size() > 0) {
                        Aspect aspect = container.getAspects(heldItem).getAspects()[0];
                        banner.setAspect(aspect);
                        if (!player.isCreative()) {
                            heldItem.shrink(1);
                        }
                        syncBanner(level, pos, state, banner);
                        level.playSound(null, pos, SoundEvents.WOOL_HIT, SoundSource.BLOCKS, 1.0f, 1.0f);
                        return InteractionResult.SUCCESS;
                    }
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    
    private void syncBanner(Level level, BlockPos pos, BlockState state, TileBanner banner) {
        banner.setChanged();
        level.sendBlockUpdated(pos, state, state, 3);
    }
    
    // ==================== Drops ====================
    
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        BlockEntity be = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        ItemStack drop = new ItemStack(this);
        
        if (be instanceof TileBanner banner && banner.getAspect() != null) {
            NBTHelper.putString(drop, "aspect", banner.getAspect().getTag());
        }
        
        return Collections.singletonList(drop);
    }
    
    @Override
    public ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state) {
        ItemStack stack = new ItemStack(this);
        BlockEntity be = level.getBlockEntity(pos);
        
        if (be instanceof TileBanner banner && banner.getAspect() != null) {
            NBTHelper.putString(stack, "aspect", banner.getAspect().getTag());
        }
        
        return stack;
    }
    
    // ==================== Factory Methods ====================
    
    public static BlockBannerTC createWhite() { return new BlockBannerTC(DyeColor.WHITE); }
    public static BlockBannerTC createOrange() { return new BlockBannerTC(DyeColor.ORANGE); }
    public static BlockBannerTC createMagenta() { return new BlockBannerTC(DyeColor.MAGENTA); }
    public static BlockBannerTC createLightBlue() { return new BlockBannerTC(DyeColor.LIGHT_BLUE); }
    public static BlockBannerTC createYellow() { return new BlockBannerTC(DyeColor.YELLOW); }
    public static BlockBannerTC createLime() { return new BlockBannerTC(DyeColor.LIME); }
    public static BlockBannerTC createPink() { return new BlockBannerTC(DyeColor.PINK); }
    public static BlockBannerTC createGray() { return new BlockBannerTC(DyeColor.GRAY); }
    public static BlockBannerTC createLightGray() { return new BlockBannerTC(DyeColor.LIGHT_GRAY); }
    public static BlockBannerTC createCyan() { return new BlockBannerTC(DyeColor.CYAN); }
    public static BlockBannerTC createPurple() { return new BlockBannerTC(DyeColor.PURPLE); }
    public static BlockBannerTC createBlue() { return new BlockBannerTC(DyeColor.BLUE); }
    public static BlockBannerTC createBrown() { return new BlockBannerTC(DyeColor.BROWN); }
    public static BlockBannerTC createGreen() { return new BlockBannerTC(DyeColor.GREEN); }
    public static BlockBannerTC createRed() { return new BlockBannerTC(DyeColor.RED); }
    public static BlockBannerTC createBlack() { return new BlockBannerTC(DyeColor.BLACK); }
    public static BlockBannerTC createCrimsonCult() { return new BlockBannerTC(null); }
}




