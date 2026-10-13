package thaumcraft.common.items.baubles;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.capabilities.IPlayerKnowledge;

import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

/**
 * ItemAmuletFancy - Fancy Vis Amulet.
 * Stores vis like a normal amulet but with greater capacity and can recharge from aura.
 */
public class ItemAmuletFancy extends Item implements ICurioItem {

    public static final int MAX_VIS = 5000;
    public static final String VIS_KEY = "FancyVis";

    public ItemAmuletFancy() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.RARE)
                .fireResistant());
    }

    public static ItemAmuletFancy createFound() {
        return new ItemAmuletFancy();
    }

    public static ItemAmuletFancy createCrafted() {
        return new ItemAmuletFancy();
    }

    public int getVis(ItemStack stack) {
        if (stack.has(DataComponents.CUSTOM_DATA)) {
            CompoundTag tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
            return tag.getInt(VIS_KEY);
        }
        return 0;
    }

    public void setVis(ItemStack stack, int vis) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        tag.putInt(VIS_KEY, Math.max(0, Math.min(vis, MAX_VIS)));
        stack.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
    }

    public void addVis(ItemStack stack, int amount) {
        setVis(stack, getVis(stack) + amount);
    }

    public boolean consumeVis(ItemStack stack, int amount) {
        int current = getVis(stack);
        if (current >= amount) {
            setVis(stack, current - amount);
            return true;
        }
        return false;
    }

    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && entity instanceof Player player) {
            // Recharge from aura slowly
            if (level.getGameTime() % 200 == 0) {
                IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
                if (knowledge.isResearchComplete("VISAMULET")) {
                    int currentVis = getVis(stack);
                    if (currentVis < MAX_VIS) {
                        // Try to draw from aura
                        int rechargeAmount = 1;
                        // Simplified: just add 1 vis every 10 seconds if not full
                        setVis(stack, currentVis + rechargeAmount);
                    }
                }
            }
        }
        super.inventoryTick(stack, level, entity, slot, selected);
    }

    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        int vis = getVis(stack);
        tooltip.add(Component.literal("Vis: " + vis + " / " + MAX_VIS)
                .withStyle(ChatFormatting.AQUA));

        tooltip.add(Component.translatable("item.thaumcraft.amulet_vis.desc")
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return slotContext.identifier().equals("amulet");
    }

    @Override
    public ICurio.SoundInfo getEquipSound(SlotContext slotContext, ItemStack stack) {
        return new ICurio.SoundInfo(SoundEvents.ARMOR_EQUIP_GENERIC.value(), 1.0F, 1.0F);
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        inventoryTick(stack, slotContext.entity().level(), slotContext.entity(), slotContext.index(), false);
    }
}
