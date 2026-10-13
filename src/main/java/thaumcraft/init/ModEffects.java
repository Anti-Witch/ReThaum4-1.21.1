package thaumcraft.init;

import net.minecraft.core.registries.Registries;

import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import thaumcraft.Thaumcraft;
import thaumcraft.api.potions.PotionFluxTaint;
import thaumcraft.api.potions.PotionVisExhaust;
import thaumcraft.common.lib.potions.PotionBlurredVision;
import thaumcraft.common.lib.potions.PotionDeathGaze;
import thaumcraft.common.lib.potions.PotionInfectiousVisExhaust;
import thaumcraft.common.lib.potions.PotionSunScorned;
import thaumcraft.common.lib.potions.PotionThaumarhia;
import thaumcraft.common.lib.potions.PotionUnnaturalHunger;
import thaumcraft.common.lib.potions.PotionWarpWard;

/**
 * Registry for all Thaumcraft mob effects (potions).
 * Uses DeferredRegister for 1.20.1 Forge.
 */
public class ModEffects {
    
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = 
            DeferredRegister.create(Registries.MOB_EFFECT, Thaumcraft.MODID);
    
    // ==================== API Effects ====================
    
    /**
     * Flux Taint - damages non-tainted entities, heals tainted ones
     */
    public static final DeferredHolder<MobEffect, MobEffect> FLUX_TAINT = 
            MOB_EFFECTS.register("flux_taint", PotionFluxTaint::new);
    
    /**
     * Vis Exhaustion - increases vis costs and slows vis regen
     */
    public static final DeferredHolder<MobEffect, MobEffect> VIS_EXHAUST = 
            MOB_EFFECTS.register("vis_exhaust", PotionVisExhaust::new);
    
    // ==================== Warp Effects ====================
    
    /**
     * Warp Ward - beneficial effect that reduces warp gain and improves soap effectiveness
     */
    public static final DeferredHolder<MobEffect, MobEffect> WARP_WARD = 
            MOB_EFFECTS.register("warp_ward", PotionWarpWard::new);
    
    /**
     * Unnatural Hunger - rapidly drains food/saturation
     */
    public static final DeferredHolder<MobEffect, MobEffect> UNNATURAL_HUNGER = 
            MOB_EFFECTS.register("unnatural_hunger", PotionUnnaturalHunger::new);
    
    /**
     * Thaumarhia - spawns flux goo at entity's location
     */
    public static final DeferredHolder<MobEffect, MobEffect> THAUMARHIA = 
            MOB_EFFECTS.register("thaumarhia", PotionThaumarhia::new);
    
    /**
     * Sun Scorned - burns in sunlight, heals in darkness
     */
    public static final DeferredHolder<MobEffect, MobEffect> SUN_SCORNED = 
            MOB_EFFECTS.register("sun_scorned", PotionSunScorned::new);
    
    /**
     * Death Gaze - applies wither to entities the player looks at
     */
    public static final DeferredHolder<MobEffect, MobEffect> DEATH_GAZE = 
            MOB_EFFECTS.register("death_gaze", PotionDeathGaze::new);
    
    /**
     * Blurred Vision - visual distortion effect
     */
    public static final DeferredHolder<MobEffect, MobEffect> BLURRED_VISION = 
            MOB_EFFECTS.register("blurred_vision", PotionBlurredVision::new);
    
    /**
     * Infectious Vis Exhaustion - spreads vis exhaust to nearby entities
     */
    public static final DeferredHolder<MobEffect, MobEffect> INFECTIOUS_VIS_EXHAUST = 
            MOB_EFFECTS.register("infectious_vis_exhaust", PotionInfectiousVisExhaust::new);
}






