package thaumcraft.common.items.misc;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.common.world.features.NodeFeature;

import java.util.List;

/**
 * ItemBlockNode - A placeable item for the Aura Node block.
 * Used in Creative mode / /give to place nodes into the world.
 * TC4 BlockAiry.onBlockPlacedBy: "Place a randomly generated node" - the node
 * receives a random type, modifier and aspect list (creative mode only).
 */
public class ItemBlockNode extends BlockItem {

    public ItemBlockNode(Block block) {
        super(block, new Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        InteractionResult result = super.place(context);
        if (result.consumesAction() && !context.getLevel().isClientSide) {
            BlockPos pos = context.getClickedPos();
            Level level = context.getLevel();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TileNode) {
                // TC4: ThaumcraftWorldGenerator.createRandomNodeAt(world, x, y, z,
                // world.rand, false, false, false) - random type/modifier/aspects.
                NodeFeature.createRandomNodeAt(level, pos, level.random, false, false, false);
                level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
            }
        }
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.thaumcraft.node.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.thaumcraft.node.random.desc").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("item.thaumcraft.node.creative.desc").withStyle(ChatFormatting.ITALIC));
    }
}
