package thaumcraft.common.golems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import javax.annotation.Nullable;

import java.util.List;

/**
 * Item representing a golem module.
 * Modules give golems specific capabilities/tasks they can perform.
 */
public class ItemGolemModule extends Item {
    private final GolemModuleType moduleType;

    public ItemGolemModule(GolemModuleType moduleType) {
        this(moduleType, new Item.Properties());
    }

    public ItemGolemModule(GolemModuleType moduleType, Item.Properties properties) {
        super(properties);
        this.moduleType = moduleType;
    }

    public GolemModuleType getModuleType() {
        return moduleType;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.thaumcraft.golem_module." + moduleType.getName() + ".desc")
                .withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, ctx, tooltip, flag);
    }
}
