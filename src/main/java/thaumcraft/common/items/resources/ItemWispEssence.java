package thaumcraft.common.items.resources;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;

import java.util.List;

/**
 * Wisp Essence (TC4 ItemWispEssence, ItemResource damage 11/12 counterpart for
 * node drops): a single-aspect essentia item dropped by wisps and destroyed
 * aura nodes.
 *
 * The port registered a plain ItemMaterial which could not carry an aspect at
 * all; this restores the TC4 behaviour (aspect in NBT, coloured icon).
 */
public class ItemWispEssence extends Item implements IEssentiaContainerItem {

    public ItemWispEssence() {
        super(new Item.Properties()
                .stacksTo(64)
                .rarity(Rarity.UNCOMMON));
    }

    // ==================== IEssentiaContainerItem ====================

    @Override
    public AspectList getAspects(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            AspectList aspects = new AspectList();
            aspects.readFromNBT(data.copyTag());
            return aspects.size() > 0 ? aspects : null;
        }
        return null;
    }

    @Override
    public void setAspects(ItemStack stack, AspectList aspects) {
        CompoundTag tag = new CompoundTag();
        aspects.writeToNBT(tag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public boolean ignoreContainedAspects() {
        return false;
    }

    /**
     * Create a stack of wisp essence containing the given aspect.
     * TC4 stored two points of the aspect per drop.
     */
    public static ItemStack createStack(Item item, Aspect aspect, int amount) {
        ItemStack stack = new ItemStack(item);
        if (aspect != null) {
            ((IEssentiaContainerItem) item).setAspects(stack,
                    new AspectList().add(aspect, Math.max(1, amount)));
        }
        return stack;
    }

    // ==================== Display ====================

    @Override
    public Component getName(ItemStack stack) {
        AspectList aspects = getAspects(stack);
        if (aspects != null && aspects.size() > 0) {
            Aspect aspect = aspects.getAspects()[0];
            return Component.translatable(this.getDescriptionId(stack), aspect.getName());
        }
        return super.getName(stack);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        AspectList aspects = getAspects(stack);
        if (aspects != null) {
            for (Aspect aspect : aspects.getAspectsSorted()) {
                if (aspect != null) {
                    tooltip.add(Component.literal(aspect.getName())
                            .withStyle(style -> style.withColor(aspect.getColor())));
                }
            }
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
