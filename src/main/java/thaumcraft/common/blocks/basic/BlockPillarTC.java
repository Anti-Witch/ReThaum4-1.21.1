package thaumcraft.common.blocks.basic;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import thaumcraft.common.lib.utils.TCCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Pillar blocks that can be oriented on different axes.
 * Used for arcane pillar, ancient pillar, eldritch pillar, etc.
 * When broken without silk touch, they drop 2x of their associated stone block.
 */
@SuppressWarnings("deprecation")
public class BlockPillarTC extends RotatedPillarBlock {

    private final Block dropBlock;

    public BlockPillarTC(Properties properties, Block dropBlock) {
        super(properties);
        this.dropBlock = dropBlock;
        this.registerDefaultState(this.defaultBlockState().setValue(AXIS, Direction.Axis.Y));
    }

    /**
     * Creates a standard arcane pillar (drops 2x arcane stone).
     */
    public static BlockPillarTC createArcane(Block dropBlock) {
        return new BlockPillarTC(
            Properties.of()
                .mapColor(MapColor.STONE)
                .strength(2.0f, 10.0f)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops(),
            dropBlock
        );
    }

    /**
     * Creates an ancient pillar (drops 2x ancient stone).
     */
    public static BlockPillarTC createAncient(Block dropBlock) {
        return new BlockPillarTC(
            Properties.of()
                .mapColor(MapColor.STONE)
                .strength(2.0f, 10.0f)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops(),
            dropBlock
        );
    }

    /**
     * Creates an eldritch pillar (drops 2x eldritch stone tile).
     * Note: Eldritch pillar has NORMAL hardness like other pillars, NOT reinforced hardness.
     * The eldritch stone TILE is reinforced, but the pillar itself is not.
     */
    public static BlockPillarTC createEldritch(Block dropBlock) {
        return new BlockPillarTC(
            Properties.of()
                .mapColor(MapColor.STONE)
                .strength(2.0f, 10.0f)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops(),
            dropBlock
        );
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(AXIS, context.getClickedFace().getAxis());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = new ArrayList<>();

        ItemStack tool = builder.getOptionalParameter(LootContextParams.TOOL);
        boolean silkTouch = tool != null && EnchantmentHelper.getItemEnchantmentLevel(
            TCCompat.enchantment(builder.getLevel(), Enchantments.SILK_TOUCH), tool) > 0;

        if (silkTouch || dropBlock == null) {
            // Silk touch drops the pillar itself
            drops.add(new ItemStack(this));
        } else {
            // Normal drop: 2x of the associated stone block
            drops.add(new ItemStack(dropBlock, 2));
        }

        return drops;
    }
}
