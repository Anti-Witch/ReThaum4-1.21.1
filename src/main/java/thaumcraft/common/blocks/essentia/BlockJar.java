package thaumcraft.common.blocks.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
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
import thaumcraft.common.tiles.essentia.TileJar;
import thaumcraft.common.tiles.essentia.TileJarBrain;
import thaumcraft.common.tiles.essentia.TileJarVoid;
import thaumcraft.init.ModBlockEntities;
import thaumcraft.init.ModItems;
import thaumcraft.init.ModSounds;

import javax.annotation.Nullable;

/**
 * Warded jars for storing essentia.
 * Different variants:
 * - Normal jar: Standard essentia storage (250 capacity)
 * - Void jar: Destroys excess essentia when full
 * - Brain jar: Stores XP orbs
 */
public class BlockJar extends Block implements EntityBlock {

    private static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 12.0, 13.0);

    public enum JarType {
        NORMAL(250),
        VOID(250),
        BRAIN(0); // Brain jar stores XP, not essentia

        private final int capacity;

        JarType(int capacity) {
            this.capacity = capacity;
        }

        public int getCapacity() {
            return capacity;
        }
    }

    private final JarType jarType;

    public BlockJar(JarType type) {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.NONE)
                .strength(0.3f)
                .sound(SoundType.GLASS)
                .noOcclusion());
        this.jarType = type;
    }

    public JarType getJarType() {
        return jarType;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockEntity blockEntity = level.getBlockEntity(pos);

        // Brain jar: right-clicking ejects a random amount of the stored XP (TC4)
        if (blockEntity instanceof TileJarBrain brain) {
            if (!level.isClientSide) {
                if (brain.xp > 0) {
                    int amount = level.random.nextInt(Math.min(brain.xp + 1, 64));
                    if (amount > 0) {
                        brain.xp -= amount;
                        brain.markDirtyAndSync();
                        while (amount > 0) {
                            int split = getXPSplit(amount);
                            amount -= split;
                            level.addFreshEntity(new ExperienceOrb(level,
                                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, split));
                        }
                    }
                }
                TileJar.playJarSound(level, pos, 0.2F);
            }
            return InteractionResult.SUCCESS;
        }

        if (blockEntity instanceof TileJar jar) {
            ItemStack held = player.getMainHandItem();
            if (player.isShiftKeyDown()) {
                // Shift + right-click on the side carrying the label removes the filter
                if (jar.getAspectFilter() != null && hit.getDirection().ordinal() == jar.getFacing()) {
                    if (!level.isClientSide) {
                        jar.removeAspectFilter();
                        dropLabel(level, pos, hit.getDirection());
                        TileJar.playJarSound(level, pos, 1.0F);
                    }
                    return InteractionResult.SUCCESS;
                }
                // Shift + right-click with an empty hand empties the jar
                if (held.isEmpty()) {
                    if (!level.isClientSide) {
                        jar.clearContents();
                        TileJar.playJarSound(level, pos, 0.4F);
                        level.playSound(null, pos, ModSounds.BUBBLE.get(), SoundSource.BLOCKS, 0.5F,
                                1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.3F);
                    }
                    return InteractionResult.SUCCESS;
                }
            }
            // Labels are applied by ItemLabel through ILabelable, phials by ItemPhial
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    /**
     * Spawn a label item where the player removed it (TC4 drops itemResource:13).
     */
    private static void dropLabel(Level level, BlockPos pos, Direction side) {
        Containers.dropItemStack(level,
                pos.getX() + 0.5D + side.getStepX() / 3.0D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D + side.getStepZ() / 3.0D,
                new ItemStack(ModItems.LABEL_BLANK.get()));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        // TC4 BlockJar.onBlockPlacedBy: the label side faces the player who placed the jar
        if (!level.isClientSide && placer != null && level.getBlockEntity(pos) instanceof TileJar jar) {
            jar.setFacing(placer.getDirection().getOpposite().ordinal());
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())
                && !level.isClientSide
                && level.getBlockEntity(pos) instanceof TileJar jar
                && jar.getAspectFilter() != null) {
            // TC4 stores the filter on the dropped jar item; the port has no filled-jar
            // item yet, so return the label instead of losing it.
            dropLabel(level, pos, Direction.UP);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof TileJarBrain brain) {
            return brain.getComparatorOutput();
        }
        if (blockEntity instanceof TileJar jar) {
            float r = (float) jar.getAmount() / (float) TileJar.CAPACITY;
            return Mth.floor(r * 14.0F) + (jar.getAmount() > 0 ? 1 : 0);
        }
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (jarType) {
            case BRAIN -> new TileJarBrain(pos, state);
            case VOID -> new TileJarVoid(pos, state);
            default -> new TileJar(pos, state);
        };
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide && jarType != JarType.BRAIN) {
            return null;
        }
        
        return switch (jarType) {
            case BRAIN -> {
                if (type == ModBlockEntities.JAR_BRAIN.get()) {
                    if (level.isClientSide) {
                        yield (lvl, pos, st, te) -> TileJarBrain.clientTick(lvl, pos, st, (TileJarBrain) te);
                    } else {
                        yield (lvl, pos, st, te) -> TileJarBrain.serverTick(lvl, pos, st, (TileJarBrain) te);
                    }
                }
                yield null;
            }
            case VOID -> type == ModBlockEntities.JAR_VOID.get() ?
                    (lvl, pos, st, te) -> TileJarVoid.serverTick(lvl, pos, st, (TileJarVoid) te) : null;
            default -> type == ModBlockEntities.JAR.get() ? 
                    (lvl, pos, st, te) -> TileJar.serverTick(lvl, pos, st, (TileJar) te) : null;
        };
    }

    /**
     * Create a normal warded jar.
     */
    public static BlockJar createNormal() {
        return new BlockJar(JarType.NORMAL);
    }

    /**
     * Create a void jar that destroys excess essentia.
     */
    public static BlockJar createVoid() {
        return new BlockJar(JarType.VOID);
    }

    /**
     * Create a brain-in-a-jar that stores XP.
     */
    public static BlockJar createBrain() {
        return new BlockJar(JarType.BRAIN);
    }

    /**
     * Split an XP amount into orb-sized chunks (vanilla ExperienceOrb#getXPSplit
     * was removed in 1.21, so the classic thresholds are reproduced here).
     */
    private static int getXPSplit(int amount) {
        if (amount >= 2477) return 2477;
        if (amount >= 1237) return 1237;
        if (amount >= 617) return 617;
        if (amount >= 307) return 307;
        if (amount >= 149) return 149;
        if (amount >= 73) return 73;
        if (amount >= 37) return 37;
        if (amount >= 17) return 17;
        if (amount >= 7) return 7;
        if (amount >= 3) return 3;
        return 1;
    }
}




