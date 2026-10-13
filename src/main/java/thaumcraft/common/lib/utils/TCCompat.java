package thaumcraft.common.lib.utils;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

/**
 * Compatibility shims for APIs that changed between Forge 1.20.1 and NeoForge 1.21.1.
 * These keep the original behaviour where a direct equivalent exists, and are no-ops
 * where the runtime setter was removed (step height must now be configured via
 * EntityType.Builder.stepHeight(...) at registration time).
 */
public final class TCCompat {

    private TCCompat() {
    }

    /**
     * NeoForge 1.21.1 removed Entity.TCCompat_setMaxUpStep(float). Step height is now configured
     * at registration via EntityType.Builder.stepHeight(float). This shim is a no-op at
     * runtime and exists only so existing call sites compile; proper per-trait step height
     * (e.g. golem WHEELED) must be handled via the EntityType builder.
     */
    public static void setMaxUpStep(Entity entity, float stepHeight) {
        // no-op: configure via EntityType.Builder.stepHeight(...) instead
    }

    /**
     * NeoForge 1.21.1 changed {@code Enchantments.FORTUNE} etc. from {@code Holder<Enchantment>}
     * to {@code ResourceKey<Enchantment>}. Recipe/enchant code that needs a {@code Holder} can use
     * this to resolve one from a {@code ResourceKey} using the level's registry access.
     */
    public static net.minecraft.core.Holder<Enchantment> enchantment(Level level, ResourceKey<Enchantment> key) {
        Registry<Enchantment> reg = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        return reg.wrapAsHolder(reg.getOrThrow(key));
    }

    /**
     * Build a {@code Holder<Enchantment>} from a resolved {@code Enchantment} value.
     */
    public static net.minecraft.core.Holder<Enchantment> enchantment(Level level, Enchantment value) {
        return level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).wrapAsHolder(value);
    }

    /**
     * Get registry access for saving/loading item stacks with NBT.
     * In 1.21.1, item save/load requires RegistryAccess.
     * Returns the level's registry access.
     */
    public static net.minecraft.core.HolderLookup.Provider getRegistryAccess(Level level) {
        return level.registryAccess();
    }

    /**
     * Get registry access from an entity's level.
     */
    public static net.minecraft.core.HolderLookup.Provider getRegistryAccess(Entity entity) {
        return entity.level().registryAccess();
    }
}
