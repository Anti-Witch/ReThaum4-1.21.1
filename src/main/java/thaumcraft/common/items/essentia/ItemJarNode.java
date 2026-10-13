package thaumcraft.common.items.essentia;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;

import java.util.List;

/**
 * ItemJarNode - The "Node in a Jar" item (TC4 ItemJarNode).
 *
 * Carries a captured aura node in its NBT: the aspects (vis), the node type,
 * the node modifier and the node id (TC4 keys: "Aspects", "nodetype",
 * "nodemod", "nodeid"). Placing it creates a {@link thaumcraft.common.tiles.TileJarNode}
 * via BlockJar; breaking that jar returns this item with the same data.
 */
public class ItemJarNode extends BlockItem implements IEssentiaContainerItem {

    public ItemJarNode(Block block, Properties properties) {
        super(block, properties.stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON));
    }

    @Override
    public String getDescriptionId() {
        return "item.thaumcraft.jar_node";
    }

    // ==================== Display ====================

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // TC4 ItemJarNode.addInformation: "§9<node type>[, <modifier>]" plus aspects
        NodeType type = getNodeType(stack);
        if (type != null) {
            MutableComponent desc = Component.translatable("nodetype." + type.name() + ".name")
                    .withStyle(ChatFormatting.BLUE);
            NodeModifier mod = getNodeModifier(stack);
            if (mod != null) {
                desc.append(", ").append(Component.translatable("nodemod." + mod.name() + ".name"));
            }
            tooltip.add(desc);
        }

        AspectList aspects = getAspects(stack);
        if (aspects != null) {
            for (Aspect aspect : aspects.getAspectsSorted()) {
                if (aspect != null) {
                    tooltip.add(Component.literal(aspect.getName() + " x " + aspects.getAmount(aspect))
                            .withStyle(style -> style.withColor(aspect.getColor())));
                }
            }
        }

        super.appendHoverText(stack, context, tooltip, flag);
    }

    // ==================== IEssentiaContainerItem (aspects stored in NBT) ====================

    @Override
    public AspectList getAspects(ItemStack itemstack) {
        if (itemstack.has(DataComponents.CUSTOM_DATA)) {
            AspectList aspects = new AspectList();
            aspects.readFromNBT(itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag());
            return aspects.size() > 0 ? aspects : null;
        }
        return null;
    }

    @Override
    public void setAspects(ItemStack itemstack, AspectList aspects) {
        CompoundTag tag = itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        aspects.writeToNBT(tag);
        itemstack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public boolean ignoreContainedAspects() {
        return false;
    }

    // ==================== Node attributes ====================

    /**
     * Set node attributes (type, modifier, id). TC4 keys: nodetype/nodemod/nodeid.
     */
    public void setNodeAttributes(ItemStack itemstack, NodeType type, NodeModifier mod, String id) {
        CompoundTag tag = itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt("nodetype", type == null ? NodeType.NORMAL.ordinal() : type.ordinal());
        if (mod != null) {
            tag.putInt("nodemod", mod.ordinal());
        } else {
            tag.remove("nodemod");
        }
        tag.putString("nodeid", id == null ? "" : id);
        itemstack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public NodeType getNodeType(ItemStack itemstack) {
        CompoundTag tag = getCustomTag(itemstack);
        if (tag == null || !tag.contains("nodetype")) {
            return null;
        }
        NodeType[] types = NodeType.values();
        int ordinal = tag.getInt("nodetype");
        return ordinal >= 0 && ordinal < types.length ? types[ordinal] : null;
    }

    public NodeModifier getNodeModifier(ItemStack itemstack) {
        CompoundTag tag = getCustomTag(itemstack);
        if (tag == null || !tag.contains("nodemod")) {
            return null;
        }
        NodeModifier[] modifiers = NodeModifier.values();
        int ordinal = tag.getInt("nodemod");
        return ordinal >= 0 && ordinal < modifiers.length ? modifiers[ordinal] : null;
    }

    public String getNodeId(ItemStack itemstack) {
        CompoundTag tag = getCustomTag(itemstack);
        return tag == null ? "" : tag.getString("nodeid");
    }

    private static CompoundTag getCustomTag(ItemStack itemstack) {
        if (itemstack == null || !itemstack.has(DataComponents.CUSTOM_DATA)) {
            return null;
        }
        return itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
}
