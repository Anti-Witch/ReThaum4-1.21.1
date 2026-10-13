package thaumcraft.common.blocks.world.taint;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.init.ModEffects;

/**
 * BlockFluxGas - TC4's flux gas: a rising, translucent cloud of raw flux.
 *
 * Ported 1:1 in spirit from OpenTC4 BlockFluxGas (BlockFluidFinite with
 * densityDir = 1, i.e. the gas rises):
 * - LEVEL 0..7 mirrors the TC4 metadata (meta 0 = 1/8 quantum, 7 = full cloud),
 *   matching the LEVEL convention of {@link BlockFluxGoo} (0 = wisp, 7 = dense).
 * - Touching it can give vis exhaustion or confusion and is consumed in the
 *   process (TC4 onEntityCollidedWithBlock).
 *
 * Deviations from TC4 (documented in subagent_flux_report.md):
 * - The 1.7.10 finite-fluid automaton is replaced by a simplified rise/merge
 *   flow (up only), like the port already does for BlockFluxGoo.
 * - TC4's gas only disappears at the build limit or when entities touch it;
 *   the port adds a slow random-tick dissipation that pollutes the aura,
 *   consistent with BlockFluxGoo evaporation.
 */
public class BlockFluxGas extends Block implements ITaintBlock {

    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 7);

    protected static final VoxelShape[] SHAPES = new VoxelShape[] {
            Block.box(0, 0, 0, 16, 2, 16),   // Level 0
            Block.box(0, 0, 0, 16, 4, 16),   // Level 1
            Block.box(0, 0, 0, 16, 6, 16),   // Level 2
            Block.box(0, 0, 0, 16, 8, 16),   // Level 3
            Block.box(0, 0, 0, 16, 10, 16),  // Level 4
            Block.box(0, 0, 0, 16, 12, 16),  // Level 5
            Block.box(0, 0, 0, 16, 14, 16),  // Level 6
            Block.box(0, 0, 0, 16, 16, 16)   // Level 7
    };

    public BlockFluxGas() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(0.0f)
                .noOcclusion()
                .pushReaction(PushReaction.DESTROY)
                .randomTicks()
                .noCollission()
                .sound(net.minecraft.world.level.block.SoundType.SLIME_BLOCK));
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 7));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(LEVEL)];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty(); // No collision - entities can walk through
    }

    // ==================== Entity interaction (TC4 onEntityCollidedWithBlock) ====================

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide) return;

        int gasLevel = state.getValue(LEVEL);
        if (level.random.nextInt(10) != 0) return;
        if (!(entity instanceof LivingEntity living)) return;
        if (entity instanceof ITaintedMob) return;
        if (living.isInvertedHealAndHarm()) return;
        if (living.hasEffect(ModEffects.VIS_EXHAUST)) return;
        if (living.hasEffect(MobEffects.CONFUSION)) return;

        if (level.random.nextBoolean()) {
            // Vis exhaustion for 1200 ticks, amplifier scales with density
            living.addEffect(new MobEffectInstance(ModEffects.VIS_EXHAUST, 1200, gasLevel / 3, true, true));
        } else {
            // Confusion for 80 + 20 ticks per level of density
            living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80 + gasLevel * 20, 0, false, true));
        }

        // The gas is consumed by the victim
        if (gasLevel > 0) {
            level.setBlock(pos, state.setValue(LEVEL, gasLevel - 1), 2);
        } else {
            level.removeBlock(pos, false);
        }
    }

    // ==================== Dissipation ====================

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int gasLevel = state.getValue(LEVEL);

        // Slowly dissipate, polluting the aura (mirrors BlockFluxGoo evaporation)
        if (random.nextInt(4) == 0) {
            if (gasLevel == 0) {
                AuraHelper.polluteAura(level, pos, 1.0f, true);
                level.removeBlock(pos, false);
            } else {
                level.setBlock(pos, state.setValue(LEVEL, gasLevel - 1), 2);
                AuraHelper.polluteAura(level, pos, 1.0f, true);
            }
        }
    }

    // ==================== Rising (TC4 densityDir = 1) ====================

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!level.isClientSide && !oldState.is(this)) {
            level.scheduleTick(pos, this, 5);
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        // Rise if there is space above
        if (direction == Direction.UP && neighborState.isAir()) {
            level.scheduleTick(pos, this, 5);
        }
        return state;
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos above = pos.above();
        if (level.isOutsideBuildHeight(above)) {
            // Gas escapes at the build limit (TC4 tryToFlowVerticallyInto removes it)
            level.removeBlock(pos, false);
            return;
        }
        BlockState aboveState = level.getBlockState(above);

        if (aboveState.isAir()) {
            // Move the gas up
            level.setBlock(above, state, 3);
            level.removeBlock(pos, false);
        } else if (aboveState.is(this)) {
            // Merge with the gas above (LEVEL tracks quanta - 1, cap 7)
            int currentLevel = state.getValue(LEVEL);
            int aboveLevel = aboveState.getValue(LEVEL);
            int totalLevel = currentLevel + aboveLevel + 1;

            if (totalLevel <= 7) {
                level.setBlock(above, aboveState.setValue(LEVEL, totalLevel), 3);
                level.removeBlock(pos, false);
            } else {
                level.setBlock(above, aboveState.setValue(LEVEL, 7), 3);
                int remaining = totalLevel - 8;
                if (remaining > 0) {
                    level.setBlock(pos, state.setValue(LEVEL, remaining), 3);
                } else {
                    level.removeBlock(pos, false);
                }
            }
        }
    }

    @Override
    public boolean canBeReplaced(BlockState state, net.minecraft.world.item.context.BlockPlaceContext context) {
        return state.getValue(LEVEL) < 2;
    }

    /**
     * Create flux gas with a specific level (0 = wisp, 7 = dense cloud).
     */
    public static BlockState withLevel(Block block, int level) {
        return block.defaultBlockState().setValue(LEVEL, Math.min(7, Math.max(0, level)));
    }
}
