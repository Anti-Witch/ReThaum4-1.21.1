package thaumcraft.common.lib.compat;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.api.items.IVisDiscountGear;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.ISlotType;
import top.theillusivec4.curios.common.slottype.SlotType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * CuriosCompat - Safe wrapper for Curios API integration.
 *
 * Provides methods that safely check if Curios is loaded before
 * attempting to use its API. This allows Thaumcraft to function
 * with or without Curios installed.
 */
public class CuriosCompat {

    private static Boolean curiosLoaded = null;

    /**
     * Curios 1.21 registers slot types in its own "curios:slot_type" registry.
     * The public constant was removed from {@link CuriosApi} in 9.x, so we
     * reconstruct the {@link ResourceKey} directly and register into it.
     */
    private static final ResourceKey<Registry<ISlotType>> SLOT_TYPE_REGISTRY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(CuriosApi.MODID, "slot_type"));

    /**
     * Check if Curios mod is loaded.
     */
    public static boolean isCuriosLoaded() {
        if (curiosLoaded == null) {
            curiosLoaded = ModList.get().isLoaded("curios");
        }
        return curiosLoaded;
    }

    /**
     * Get total vis discount from Curios-equipped items.
     *
     * @param player The player to check
     * @return Total vis discount from Curios items
     */
    public static int getVisDiscountFromCurios(Player player) {
        if (!isCuriosLoaded() || player == null) {
            return 0;
        }

        try {
            return CuriosHelper.getVisDiscount(player);
        } catch (NoClassDefFoundError | NoSuchMethodError e) {
            // Curios API not available or incompatible
            curiosLoaded = false;
            return 0;
        }
    }

    /**
     * Iterate over all items in Curios slots.
     *
     * @param player The player
     * @param consumer Consumer that receives each non-empty ItemStack
     */
    public static void forEachCuriosItem(Player player, Consumer<ItemStack> consumer) {
        if (!isCuriosLoaded() || player == null) {
            return;
        }

        try {
            CuriosHelper.forEachItem(player, consumer);
        } catch (NoClassDefFoundError | NoSuchMethodError e) {
            curiosLoaded = false;
        }
    }

    /**
     * Iterate over all items in Curios slots with slot info.
     *
     * @param player The player
     * @param consumer Consumer that receives (slotKey, ItemStack)
     */
    public static void forEachCuriosItemWithSlot(Player player, BiConsumer<String, ItemStack> consumer) {
        if (!isCuriosLoaded() || player == null) {
            return;
        }

        try {
            CuriosHelper.forEachItemWithSlot(player, consumer);
        } catch (NoClassDefFoundError | NoSuchMethodError e) {
            curiosLoaded = false;
        }
    }

    /**
     * Find all items in Curios slots matching a predicate.
     *
     * @param player The player
     * @param predicate The item filter
     * @return List of matching items
     */
    public static List<ItemStack> findCuriosItems(Player player, Predicate<ItemStack> predicate) {
        List<ItemStack> result = new ArrayList<>();
        if (!isCuriosLoaded() || player == null) {
            return result;
        }

        try {
            return CuriosHelper.findItems(player, predicate);
        } catch (NoClassDefFoundError | NoSuchMethodError e) {
            curiosLoaded = false;
            return result;
        }
    }

    /**
     * Find the first equipped item of a given class in a specific Curios slot.
     * Used by bauble items (e.g. Charm of Undying, Curiosity Band) to locate
     * themselves when worn in a Curios slot rather than the vanilla inventory.
     *
     * @param player   The player
     * @param slotId   The Curios slot identifier (e.g. "charm", "head")
     * @param itemClass The item class to match
     * @return The matching ItemStack, or ItemStack.EMPTY if none found
     */
    public static ItemStack findEquippedInSlot(Player player, String slotId, Class<? extends Item> itemClass) {
        if (!isCuriosLoaded() || player == null) {
            return ItemStack.EMPTY;
        }

        try {
            return CuriosHelper.findEquippedInSlot(player, slotId, itemClass);
        } catch (NoClassDefFoundError | NoSuchMethodError e) {
            curiosLoaded = false;
            return ItemStack.EMPTY;
        }
    }

    /**
     * Register Thaumcraft's Curios slot types (ring, charm, amulet, belt, head).
     * <p>
     * Must be called during mod construction (on the mod event bus) so the
     * DeferredRegister is wired before Curios' registry events fire. This is a
     * no-op when Curios is not installed, preserving optional-Curios behaviour.
     */
    public static void registerSlotTypes(IEventBus modEventBus) {
        if (!isCuriosLoaded() || modEventBus == null) {
            return;
        }

        try {
            DeferredRegister<ISlotType> slots = DeferredRegister.create(SLOT_TYPE_REGISTRY, Thaumcraft.MODID);
            slots.register("ring", () -> new SlotType.Builder("ring").size(2).build());
            slots.register("charm", () -> new SlotType.Builder("charm").size(2).build());
            slots.register("amulet", () -> new SlotType.Builder("amulet").size(1).build());
            slots.register("belt", () -> new SlotType.Builder("belt").size(1).build());
            slots.register("head", () -> new SlotType.Builder("head").size(1).build());
            slots.register(modEventBus);
        } catch (NoClassDefFoundError | NoSuchMethodError e) {
            // Curios API not available or incompatible
            curiosLoaded = false;
        }
    }

    /**
     * Inner helper class that actually uses Curios API.
     * Isolated to prevent class loading issues when Curios isn't present.
     */
    private static class CuriosHelper {

        static int getVisDiscount(Player player) {
            AtomicInteger total = new AtomicInteger(0);

            top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                handler.getCurios().forEach((slotId, slotHandler) -> {
                    for (int i = 0; i < slotHandler.getSlots(); i++) {
                        ItemStack stack = slotHandler.getStacks().getStackInSlot(i);
                        if (!stack.isEmpty() && stack.getItem() instanceof IVisDiscountGear gear) {
                            total.addAndGet(gear.getVisDiscount(stack, player));
                        }
                    }
                });
            });

            return total.get();
        }

        static void forEachItem(Player player, Consumer<ItemStack> consumer) {
            top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                handler.getCurios().forEach((slotId, slotHandler) -> {
                    for (int i = 0; i < slotHandler.getSlots(); i++) {
                        ItemStack stack = slotHandler.getStacks().getStackInSlot(i);
                        if (!stack.isEmpty()) {
                            consumer.accept(stack);
                        }
                    }
                });
            });
        }

        static void forEachItemWithSlot(Player player, BiConsumer<String, ItemStack> consumer) {
            top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                handler.getCurios().forEach((slotId, slotHandler) -> {
                    for (int i = 0; i < slotHandler.getSlots(); i++) {
                        ItemStack stack = slotHandler.getStacks().getStackInSlot(i);
                        if (!stack.isEmpty()) {
                            consumer.accept(slotId + ":" + i, stack);
                        }
                    }
                });
            });
        }

        static List<ItemStack> findItems(Player player, Predicate<ItemStack> predicate) {
            List<ItemStack> result = new ArrayList<>();

            top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                handler.getCurios().forEach((slotId, slotHandler) -> {
                    for (int i = 0; i < slotHandler.getSlots(); i++) {
                        ItemStack stack = slotHandler.getStacks().getStackInSlot(i);
                        if (!stack.isEmpty() && predicate.test(stack)) {
                            result.add(stack);
                        }
                    }
                });
            });

            return result;
        }

        static ItemStack findEquippedInSlot(Player player, String slotId, Class<? extends Item> itemClass) {
            var handler = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player);
            if (handler.isEmpty()) {
                return ItemStack.EMPTY;
            }
            var slotHandler = handler.get().getCurios().get(slotId);
            if (slotHandler == null) {
                return ItemStack.EMPTY;
            }
            for (int i = 0; i < slotHandler.getSlots(); i++) {
                ItemStack stack = slotHandler.getStacks().getStackInSlot(i);
                if (!stack.isEmpty() && itemClass.isInstance(stack.getItem())) {
                    return stack;
                }
            }
            return ItemStack.EMPTY;
        }
    }
}
