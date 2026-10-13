package thaumcraft.common.lib;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;

import java.util.function.Supplier;

/**
 * SoundsTC - Thaumcraft sound events.
 */
public class SoundsTC {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(
            net.minecraft.core.registries.Registries.SOUND_EVENT, Thaumcraft.MODID);

    public static final Supplier<SoundEvent> PAGE = SOUND_EVENTS.register("page",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "page")));

    public static final Supplier<SoundEvent> EVIL_PORTAL = SOUND_EVENTS.register("evil_portal",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "evil_portal")));
}
