package thaumcraft.init;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import thaumcraft.Thaumcraft;

/**
 * Registry for custom Thaumcraft Data Components.
 *
 * These are used for TC4-style wand storage: the rod and cap tags are stored
 * as string Data Components on the wand ItemStack (written by the 99 wand
 * crafting_shaped JSON recipes and read by {@code WandItem}).
 */
public class ModDataComponents {

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Thaumcraft.MODID);

    /** Wand rod tag (e.g. "greatwood", "blaze"). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> WAND_ROD =
            registerString("wand_rod");

    /** Wand cap tag (e.g. "iron", "gold"). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> WAND_CAP =
            registerString("wand_cap");

    /**
     * Focus ItemStack installed in a wand (serialized CompoundTag).
     * Written by {@code WandItem.setFocus} / the F-key radial menu.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> WAND_FOCUS =
            registerCompound("wand_focus");

    /**
     * Vis storage of a wand: a CompoundTag holding a single "vis" float
     * (the TC6-style focus pipeline charges one aggregated amount, so the
     * TC4-style wand keeps one combined pool). Capacity is the rod's
     * {@code WandRod#getCapacity()}; costs are reduced by the cap's
     * {@code baseCostModifier}.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> WAND_VIS =
            registerCompound("wand_vis");

    /**
     * Sceptre flag (TC4 NBT "sceptre"): +50% capacity and -0.1 consumption
     * modifier for sceptres crafted with the ArcaneSceptreRecipe.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> WAND_SCEPTRE =
            DATA_COMPONENTS.register("wand_sceptre",
                    () -> DataComponentType.<Boolean>builder()
                            .persistent(Codec.BOOL)
                            .networkSynchronized(ByteBufCodecs.BOOL)
                            .build());

    /**
     * TC4 golem animation core variant (ItemGolemCore metadata 0-11, 100 = blank).
     * Stored as a component so a single item can hold every core type, mirroring
     * TC4's metadata-based {@code ItemGolemCore}.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> GOLEM_CORE_TYPE =
            DATA_COMPONENTS.register("golem_core_type",
                    () -> DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.VAR_INT)
                            .build());

    private static DeferredHolder<DataComponentType<?>, DataComponentType<String>> registerString(String name) {
        return DATA_COMPONENTS.register(name,
                () -> DataComponentType.<String>builder()
                        .persistent(Codec.STRING)
                        .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                        .build());
    }

    private static DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> registerCompound(String name) {
        return DATA_COMPONENTS.register(name,
                () -> DataComponentType.<CompoundTag>builder()
                        .persistent(CompoundTag.CODEC)
                        .networkSynchronized(ByteBufCodecs.COMPOUND_TAG)
                        .build());
    }
}