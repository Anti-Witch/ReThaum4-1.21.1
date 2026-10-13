package thaumcraft.common.items.casters;

import thaumcraft.api.casters.FocusEngine;
import thaumcraft.common.items.casters.foci.*;

/**
 * Initializes and registers all Thaumcraft focus elements.
 * Called during mod initialization.
 */
public class FocusInit {
    
    /**
     * Register all focus elements with the FocusEngine.
     */
    public static void registerFoci() {
        // ==================== Root Mediums ====================
        FocusEngine.registerFocusNode("thaumcraft.TOUCH", FocusMediumTouch::new);
        
        // ==================== Mediums ====================
        FocusEngine.registerFocusNode("thaumcraft.BOLT", FocusMediumBolt::new);
        FocusEngine.registerFocusNode("thaumcraft.PROJECTILE", FocusMediumProjectile::new);
        FocusEngine.registerFocusNode("thaumcraft.CLOUD", FocusMediumCloud::new);
        FocusEngine.registerFocusNode("thaumcraft.MINE", FocusMediumMine::new);
        FocusEngine.registerFocusNode("thaumcraft.PLAN", FocusMediumPlan::new);
        FocusEngine.registerFocusNode("thaumcraft.SPELLBAT", FocusMediumSpellBat::new);
        
        // ==================== Effects ====================
        // Elemental effects
        FocusEngine.registerFocusNode("thaumcraft.FIRE", FocusEffectFire::new, 0xFF5A01);
        FocusEngine.registerFocusNode("thaumcraft.FROST", FocusEffectFrost::new, 0x00BFFF);
        FocusEngine.registerFocusNode("thaumcraft.AIR", FocusEffectAir::new, 0xFFFF7E);
        FocusEngine.registerFocusNode("thaumcraft.EARTH", FocusEffectEarth::new, 0x56C000);
        
        // Utility effects
        FocusEngine.registerFocusNode("thaumcraft.FLUX", FocusEffectFlux::new, 0x800080);
        FocusEngine.registerFocusNode("thaumcraft.HEAL", FocusEffectHeal::new, 0xDE0005);
        FocusEngine.registerFocusNode("thaumcraft.BREAK", FocusEffectBreak::new, 0x404040);
        FocusEngine.registerFocusNode("thaumcraft.EXCHANGE", FocusEffectExchange::new, 0x87CEEB);
        FocusEngine.registerFocusNode("thaumcraft.CURSE", FocusEffectCurse::new, 0x6A0005);
        FocusEngine.registerFocusNode("thaumcraft.RIFT", FocusEffectRift::new, 0xAA00AA);
        
        // ==================== Modifiers ====================
        FocusEngine.registerFocusNode("thaumcraft.SCATTER", FocusModScatter::new);
        FocusEngine.registerFocusNode("thaumcraft.SPLITTRAJECTORY", FocusModSplitTrajectory::new);
        FocusEngine.registerFocusNode("thaumcraft.SPLITTARGET", FocusModSplitTarget::new);
    }
    
    /**
     * Get a list of all registered focus keys for reference.
     */
    public static String[] getAllFocusKeys() {
        return new String[] {
            // Root Mediums
            "thaumcraft.TOUCH",
            
            // Mediums
            "thaumcraft.BOLT",
            "thaumcraft.PROJECTILE",
            "thaumcraft.CLOUD",
            "thaumcraft.MINE",
            "thaumcraft.PLAN",
            "thaumcraft.SPELLBAT",
            
            // Effects
            "thaumcraft.FIRE",
            "thaumcraft.FROST",
            "thaumcraft.AIR",
            "thaumcraft.EARTH",
            "thaumcraft.FLUX",
            "thaumcraft.HEAL",
            "thaumcraft.BREAK",
            "thaumcraft.EXCHANGE",
            "thaumcraft.CURSE",
            "thaumcraft.RIFT",
            
            // Modifiers
            "thaumcraft.SCATTER",
            "thaumcraft.SPLITTRAJECTORY",
            "thaumcraft.SPLITTARGET"
        };
    }
}




