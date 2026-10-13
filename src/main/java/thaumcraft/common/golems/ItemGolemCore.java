package thaumcraft.common.golems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import thaumcraft.init.ModDataComponents;
import thaumcraft.init.ModItems;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * TC4 golem animation core ("the colored pill").
 *
 * TC4 used a single item ({@code thaumcraft.common.entities.golems.ItemGolemCore})
 * with 12 metadata variants (0-11) plus the blank core (100). This port keeps the
 * single-item design but stores the variant in a Data Component
 * ({@link ModDataComponents#GOLEM_CORE_TYPE}) instead of metadata.
 *
 * The item itself only carries the variant; the golem behaviour is driven by
 * seals in this port (TC6-style), so the TC4 core behaviours are not wired up
 * yet - see {@link GolemCoreType#hasGUI()}, {@link GolemCoreType#canSort()} and
 * {@link GolemCoreType#hasInventory()} for the flags the future AI will need.
 */
public class ItemGolemCore extends Item {

    public ItemGolemCore() {
        this(new Item.Properties().stacksTo(64));
    }

    public ItemGolemCore(Item.Properties properties) {
        super(properties);
    }

    // ==================== Variant access ====================

    /** Core variant of the stack; stacks without the component are blank cores. */
    public static GolemCoreType getCoreType(ItemStack stack) {
        Integer id = stack.get(ModDataComponents.GOLEM_CORE_TYPE.get());
        return id == null ? GolemCoreType.BLANK : GolemCoreType.byId(id);
    }

    /** Writes the variant into the stack and returns it. Blank cores are plain stacks. */
    public static ItemStack setCoreType(ItemStack stack, GolemCoreType type) {
        if (type.isBlank()) {
            stack.remove(ModDataComponents.GOLEM_CORE_TYPE.get());
        } else {
            stack.set(ModDataComponents.GOLEM_CORE_TYPE.get(), type.getId());
        }
        return stack;
    }

    /** Creates a core stack of the given variant. */
    public static ItemStack createStack(GolemCoreType type) {
        return setCoreType(new ItemStack(ModItems.GOLEM_CORE.get()), type);
    }

    /** Every variant, in TC4 creative order (the blank base first, then the cores). */
    public static List<ItemStack> createAllStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        stacks.add(createStack(GolemCoreType.BLANK));
        for (GolemCoreType type : GolemCoreType.functionalCores()) {
            stacks.add(createStack(type));
        }
        return stacks;
    }

    // ==================== Display ====================

    @Override
    public Component getName(ItemStack stack) {
        GolemCoreType type = getCoreType(stack);
        if (type.isBlank()) {
            return Component.translatable("item.thaumcraft.golem_core");
        }
        // TC4 registered the functional cores as uncommon items (yellow name).
        return Component.translatable("item.thaumcraft.golem_core")
                .append(": ")
                .append(Component.translatable(type.getTranslationKey()))
                .withStyle(Rarity.UNCOMMON.getStyleModifier());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        GolemCoreType type = getCoreType(stack);
        if (type.isBlank()) {
            tooltip.add(Component.translatable("item.thaumcraft.golem_core.blank.desc")
                    .withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, ctx, tooltip, flag);
    }
}
