package thaumcraft.common.items.essentia;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;

import java.util.List;

/**
 * ItemJarNode - The "Node in a Jar" item.
 * Contains a captured aura node that can be placed back into the world.
 * Right-click with wand on node -> captures node into jar (if player has empty jar).
 * Right-click with jar on block -> places jar with node inside.
 */
public class ItemJarNode extends BlockItem {

    public ItemJarNode(Block block, Properties properties) {
        super(block, properties.stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON));
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();

        if (!level.isClientSide && stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) {
            // Will be handled by onPlace in Block
        }
        return super.place(context);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) {
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

            // Node type
            if (tag.contains("nodetype")) {
                NodeType type = NodeType.values()[tag.getInt("nodetype")];
                tooltip.add(Component.translatable("nodetype." + type.name().toLowerCase() + ".name")
                        .withStyle(ChatFormatting.BLUE));
            }

            // Node modifier
            if (tag.contains("nodemod")) {
                NodeModifier mod = NodeModifier.values()[tag.getInt("nodemod")];
                tooltip.add(Component.translatable("nodemod." + mod.name().toLowerCase() + ".name")
                        .withStyle(ChatFormatting.GREEN));
            }

            // Aspects
            AspectList aspects = getAspects(stack);
            if (aspects != null && aspects.size() > 0) {
                tooltip.add(Component.literal("Aspects:").withStyle(ChatFormatting.GRAY));
                for (var aspect : aspects.getAspects()) {
                    int amount = aspects.getAmount(aspect);
                    tooltip.add(Component.literal("  " + aspect.getName() + ": " + amount)
                            .withStyle(net.minecraft.ChatFormatting.WHITE));
                }
            }
        }
    }

    /**
     * Get the aspects stored in this jar.
     */
    public AspectList getAspects(ItemStack itemstack) {
        if (itemstack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) {
            AspectList aspects = new AspectList();
            aspects.readFromNBT(itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag());
            return aspects.size() > 0 ? aspects : null;
        }
        return null;
    }

    /**
     * Set the aspects in this jar. NOTE: the NBT has to be written back onto the
     * item - writing into a copied tag silently discards the data.
     */
    public void setAspects(ItemStack itemstack, AspectList aspects) {
        CompoundTag tag = itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        aspects.writeToNBT(tag);
        itemstack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /**
     * Set node attributes (type, modifier, id).
     */
    public void setNodeAttributes(ItemStack itemstack, NodeType type, NodeModifier mod, String id) {
        CompoundTag tag = itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt("nodetype", type.ordinal());
        if (mod != null) {
            tag.putInt("nodemod", mod.ordinal());
        }
        tag.putString("nodeid", id);
        itemstack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public NodeType getNodeType(ItemStack itemstack) {
        if (!itemstack.has(DataComponents.CUSTOM_DATA)) return null;
        if (!itemstack.has(DataComponents.CUSTOM_DATA)) return null;
        CompoundTag tag = itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains("nodetype") ? NodeType.values()[tag.getInt("nodetype")] : null;
    }

    public NodeModifier getNodeModifier(ItemStack itemstack) {
        if (!itemstack.has(DataComponents.CUSTOM_DATA)) return null;
        if (!itemstack.has(DataComponents.CUSTOM_DATA)) return null;
        CompoundTag tag = itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains("nodemod") ? NodeModifier.values()[tag.getInt("nodemod")] : null;
    }

    public String getNodeId(ItemStack itemstack) {
        if (!itemstack.has(DataComponents.CUSTOM_DATA)) return "0";
        CompoundTag tag = itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getString("nodeid");
    }
}
