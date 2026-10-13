package thaumcraft.api.damagesource;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import thaumcraft.Thaumcraft;

/**
 * Custom damage sources for Thaumcraft.
 * 
 * Ported from 1.12.2. Major API changes:
 * - DamageSource is now created from DamageType registry entries
 * - Static damage sources replaced with factory methods
 * - DamageType properties (bypasses armor, magic, etc.) defined in data
 * 
 * For now, we use vanilla damage types as placeholders until
 * custom damage types are registered via datapacks.
 */
public class DamageSourceThaumcraft {
    
    // Resource keys for custom damage types
    public static final ResourceKey<DamageType> TAINT = ResourceKey.create(
            Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "taint"));
    public static final ResourceKey<DamageType> TENTACLE = ResourceKey.create(
            Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "tentacle"));
    public static final ResourceKey<DamageType> SWARM = ResourceKey.create(
            Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "swarm"));
    public static final ResourceKey<DamageType> DISSOLVE = ResourceKey.create(
            Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "dissolve"));
    
    /**
     * Create taint damage source (bypasses armor, magic damage).
     */
    public static DamageSource createTaint(Level level) {
        return new DamageSource(
                level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(TAINT)
        );
    }
    
    /**
     * Create tentacle damage source from an entity.
     */
    public static DamageSource causeTentacleDamage(LivingEntity attacker) {
        return new DamageSource(
                attacker.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(TENTACLE),
                attacker
        );
    }
    
    /**
     * Create swarm damage source from an entity.
     */
    public static DamageSource causeSwarmDamage(LivingEntity attacker) {
        return new DamageSource(
                attacker.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SWARM),
                attacker
        );
    }
    
    /**
     * Create dissolve damage source (bypasses armor).
     */
    public static DamageSource createDissolve(Level level) {
        return new DamageSource(
                level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DISSOLVE)
        );
    }
    
    /**
     * Generic magic damage (uses vanilla MAGIC type).
     */
    public static DamageSource createMagic(Level level, Entity source, Entity attacker) {
        return level.damageSources().indirectMagic(source, attacker);
    }
}




