package thaumcraft.common.items.casters;

import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import thaumcraft.common.menu.FocusPouchMenu;

import java.util.List;

/**
 * Focus Pouch - A portable container for storing foci.
 * Can be worn as a curio (belt slot) for quick access.
 * Holds up to 18 foci.
 */
public class ItemFocusPouch extends Item {

    public static final int INVENTORY_SIZE = 18;

    public ItemFocusPouch() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            final InteractionHand usedHand = hand;
            serverPlayer.openMenu(new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("container.thaumcraft.focus_pouch");
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player menuPlayer) {
                    return new FocusPouchMenu(containerId, playerInventory, usedHand);
                }
            }, (RegistryFriendlyByteBuf buf) -> {
                buf.writeEnum(usedHand);
            });
        }
        
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /**
     * Get the inventory contents of this pouch.
     */
    public NonNullList<ItemStack> getInventory(ItemStack item) {
        NonNullList<ItemStack> stackList = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
        net.minecraft.world.item.component.ItemContainerContents contents = item.get(net.minecraft.core.component.DataComponents.CONTAINER);
        if (contents != null) {
            contents.copyInto(stackList);
        }
        return stackList;
    }

    /**
     * Set the inventory contents of this pouch.
     */
    public void setInventory(ItemStack item, NonNullList<ItemStack> stackList) {
        item.set(net.minecraft.core.component.DataComponents.CONTAINER, 
                 net.minecraft.world.item.component.ItemContainerContents.fromItems(stackList));
    }

    /**
     * Count how many foci are stored in this pouch.
     */
    public int getFociCount(ItemStack item) {
        NonNullList<ItemStack> inv = getInventory(item);
        int count = 0;
        for (ItemStack stack : inv) {
            if (!stack.isEmpty() && stack.getItem() instanceof ItemFocus) {
                count++;
            }
        }
        return count;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int count = getFociCount(stack);
        if (count > 0) {
            tooltip.add(Component.translatable("item.thaumcraft.focus_pouch.contents", count, INVENTORY_SIZE)
                    .withStyle(style -> style.withColor(0x808080)));
        } else {
            tooltip.add(Component.translatable("item.thaumcraft.focus_pouch.empty")
                    .withStyle(style -> style.withColor(0x808080)));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }

    // TODO: Implement Curios integration for belt slot
    // This would allow wearing the pouch and accessing foci quickly
}




