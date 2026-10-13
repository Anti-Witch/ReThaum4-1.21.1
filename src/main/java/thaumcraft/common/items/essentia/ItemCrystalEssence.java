package thaumcraft.common.items.essentia;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import thaumcraft.api.aspects.Aspect;

import java.util.List;

/**
 * ItemCrystalEssence - Crystal Essence item.
 * Stores a single aspect essence for use in crafting.
 */
public class ItemCrystalEssence extends Item {

    private final Aspect lockedAspect;

    public ItemCrystalEssence() {
        this(null);
    }

    public ItemCrystalEssence(Aspect lockedAspect) {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.RARE)
                .fireResistant());
        this.lockedAspect = lockedAspect;
    }

    public Aspect getLockedAspect(ItemStack stack) {
        if (lockedAspect != null) return lockedAspect;
        if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) {
            net.minecraft.nbt.CompoundTag tag = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();
            if (tag.contains("Aspect")) {
                return thaumcraft.api.aspects.Aspect.getAspect(tag.getString("Aspect"));
            }
        }
        return null;
    }

    public void setLockedAspect(ItemStack stack, thaumcraft.api.aspects.Aspect aspect) {
        net.minecraft.nbt.CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        if (aspect != null) {
            tag.putString("Aspect", aspect.getTag());
        } else {
            tag.remove("Aspect");
        }
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
    }

    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        thaumcraft.api.aspects.Aspect aspect = getLockedAspect(stack);
        if (aspect != null) {
            tooltip.add(net.minecraft.network.chat.Component.translatable("aspect." + aspect.getTag() + ".name")
                    .withStyle(net.minecraft.ChatFormatting.AQUA)
                    .withStyle(net.minecraft.ChatFormatting.WHITE));
        }

        tooltip.add(net.minecraft.network.chat.Component.translatable("item.thaumcraft.crystal_essence.desc")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
