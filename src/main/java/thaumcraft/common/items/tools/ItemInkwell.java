package thaumcraft.common.items.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.items.IScribeTools;
import thaumcraft.common.blocks.basic.BlockTable;
import thaumcraft.common.blocks.crafting.BlockResearchTable;
import thaumcraft.common.items.ItemTC;
import thaumcraft.common.tiles.crafting.TileResearchTable;
import thaumcraft.init.ModBlocks;

/**
 * Inkwell - TC4-authentic: right-clicking a wooden table that stands next to
 * another wooden table converts the pair into a Research Table. The inkwell
 * is consumed into the table's scribing-tools slot where its durability
 * (100) serves as the ink supply. There is no direct crafting recipe for the
 * research table - exactly like TC4.
 */
public class ItemInkwell extends ItemTC implements IScribeTools {

    public ItemInkwell() {
        super(new Properties()
                .stacksTo(1)
                .durability(100));
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }

    @Override
    public boolean isRepairable(ItemStack stack) {
        return false;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player == null) {
            return InteractionResult.PASS;
        }

        BlockState state = level.getBlockState(pos);

        // Never touch the research table itself - let the GUI open.
        if (!(state.getBlock() instanceof BlockTable table) || !table.isWooden()) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // TC4: an adjacent wooden table is required for the conversion.
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos adjPos = pos.relative(dir);
            BlockState adjState = level.getBlockState(adjPos);
            if (adjState.getBlock() instanceof BlockTable adjTable && adjTable.isWooden()) {
                // TC4 multiblock: BOTH tables become the research table
                // (previously the second table was deleted, which left a
                // single research table that nothing could open because the
                // GUI requires an adjacent partner block).
                level.setBlock(pos, ModBlocks.RESEARCH_TABLE.get().defaultBlockState()
                        .setValue(BlockResearchTable.FACING, dir), 3);
                level.setBlock(adjPos, ModBlocks.RESEARCH_TABLE.get().defaultBlockState()
                        .setValue(BlockResearchTable.FACING, dir.getOpposite()), 3);

                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof TileResearchTable tile) {
                    // The inkwell becomes the table's ink supply (TC4 behaviour).
                    tile.setItem(TileResearchTable.SLOT_SCRIBING_TOOLS, stack.copy());
                }

                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }

                level.playSound(null, pos, state.getSoundType().getPlaceSound(),
                        SoundSource.BLOCKS, 1.0f, 1.0f);
                player.displayClientMessage(
                        Component.translatable("tile.researchtable.created"), true);
                return InteractionResult.CONSUME;
            }
        }

        player.displayClientMessage(
                Component.translatable("tile.researchtable.needtwo"), true);
        return InteractionResult.FAIL;
    }
}
