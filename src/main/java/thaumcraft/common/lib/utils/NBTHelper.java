package thaumcraft.common.lib.utils;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Compatibility helper for ItemStack NBT access.
 *
 * NeoForge 1.21.1 removed ItemStack.getTag()/hasTag()/getOrCreateTag(). Tag data now
 * lives in the DataComponents.CUSTOM_DATA component (a CustomData wrapping a CompoundTag).
 * These helpers read/write that component so existing call sites keep working and the
 * data actually persists on the stack.
 */
public final class NBTHelper {

    private NBTHelper() {
    }

    public static boolean hasTag(ItemStack stack) {
        return stack.has(DataComponents.CUSTOM_DATA);
    }

    public static CompoundTag getTag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static void update(ItemStack stack, java.util.function.Consumer<CompoundTag> op) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        op.accept(tag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void putString(ItemStack stack, String key, String value) {
        update(stack, t -> t.putString(key, value));
    }

    public static void putInt(ItemStack stack, String key, int value) {
        update(stack, t -> t.putInt(key, value));
    }

    public static void putLong(ItemStack stack, String key, long value) {
        update(stack, t -> t.putLong(key, value));
    }

    public static void putByte(ItemStack stack, String key, byte value) {
        update(stack, t -> t.putByte(key, value));
    }

    public static void putBoolean(ItemStack stack, String key, boolean value) {
        update(stack, t -> t.putBoolean(key, value));
    }

    public static void putFloat(ItemStack stack, String key, float value) {
        update(stack, t -> t.putFloat(key, value));
    }

    public static void putCompound(ItemStack stack, String key, CompoundTag value) {
        update(stack, t -> t.put(key, value));
    }

    public static void put(ItemStack stack, String key, CompoundTag value) {
        update(stack, t -> t.put(key, value));
    }

    public static ItemStack fromNbt(CompoundTag tag) {
        return ItemStack.parse(net.minecraft.core.RegistryAccess.EMPTY, tag)
                .orElse(ItemStack.EMPTY);
    }

    /**
     * Returns the CUSTOM_DATA tag, creating an empty one if absent (1.21.1 replacement for
     * the removed ItemStack.getOrCreateTag()). The returned CompoundTag is the live tag held
     * by the component, so callers may mutate it in place and the change persists on the
     * stack (no setTag() round-trip required). Uses CustomData.getUnsafe() to obtain the
     * stored (mutable) tag rather than a throwaway copy.
     */
    public static CompoundTag getOrCreateTag(ItemStack stack) {
        if (!stack.has(DataComponents.CUSTOM_DATA)) {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(new CompoundTag()));
        }
        return stack.get(DataComponents.CUSTOM_DATA).getUnsafe();
    }

    /**
     * Sets the CUSTOM_DATA tag (1.21.1 replacement for the removed ItemStack.setTag(CompoundTag)).
     */
    public static void setTag(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
