package thaumcraft.common.items.resources;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Crystal Essence - Crystallized vis that contains a single aspect.
 * Used as crafting ingredients and can be broken down for essentia.
 *
 * @author Azanor
 * Ported to 1.20.1
 */
public class ItemCrystalEssence extends Item implements IEssentiaContainerItem {

    /** Base amount of essentia stored in each crystal */
    protected final int baseAmount;

    public ItemCrystalEssence() {
        super(new Item.Properties().stacksTo(64));
        this.baseAmount = 1;
    }

    public ItemCrystalEssence(int baseAmount) {
        super(new Item.Properties().stacksTo(64));
        this.baseAmount = baseAmount;
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

    // ==================== Display Name ====================

    @Override
    public Component getName(ItemStack stack) {
        AspectList aspects = getAspects(stack);
        if (aspects != null && aspects.size() > 0) {
            Aspect aspect = aspects.getAspects()[0];
            // Format: "Aspect Crystal" or localized version
            return Component.translatable(this.getDescriptionId(stack), aspect.getName());
        }
        return super.getName(stack);
    }

    // ==================== Random Aspect Assignment ====================

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);

        // Assign random aspect if none set (for items spawned without NBT)
        if (!level.isClientSide && !stack.has(DataComponents.CUSTOM_DATA)) {
            assignRandomAspect(stack, level);
        }
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        super.onCraftedBy(stack, level, player);

        if (!level.isClientSide && !stack.has(DataComponents.CUSTOM_DATA)) {
            assignRandomAspect(stack, level);
        }
    }

    /**
     * Assign a random aspect to this crystal.
     */
    private void assignRandomAspect(ItemStack stack, Level level) {
        Collection<Aspect> aspects = Aspect.aspects.values();
        if (aspects.isEmpty()) return;

        Aspect[] aspectArray = aspects.toArray(new Aspect[0]);
        Aspect randomAspect = aspectArray[level.random.nextInt(aspectArray.length)];
        setAspects(stack, new AspectList().add(randomAspect, baseAmount));
    }

    // ==================== Factory Methods ====================

    /**
     * Create a crystal essence with a specific aspect.
     */
    public static ItemStack createStack(Item item, Aspect aspect, int count) {
        ItemStack stack = new ItemStack(item, count);
        if (item instanceof ItemCrystalEssence crystalItem) {
            crystalItem.setAspects(stack, new AspectList().add(aspect, crystalItem.baseAmount));
        }
        return stack;
    }

    /**
     * Get the aspect stored in a crystal essence stack.
     */
    @Nullable
    public static Aspect getAspect(ItemStack stack) {
        if (stack.getItem() instanceof ItemCrystalEssence crystalItem) {
            AspectList aspects = crystalItem.getAspects(stack);
            if (aspects != null && aspects.size() > 0) {
                return aspects.getAspects()[0];
            }
        }
        return null;
    }

    /**
     * Check if two crystal essence stacks contain the same aspect.
     */
    public static boolean isSameAspect(ItemStack stack1, ItemStack stack2) {
        Aspect a1 = getAspect(stack1);
        Aspect a2 = getAspect(stack2);
        if (a1 == null || a2 == null) return false;
        return a1.equals(a2);
    }
}
