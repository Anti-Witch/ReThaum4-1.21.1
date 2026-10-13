package thaumcraft.common.config;

/**
 * ModConfig - Configuration values for Thaumcraft.
 * 
 * TODO: This is a placeholder with hardcoded defaults.
 * Should be replaced with proper Forge config system.
 */
public class ModConfig {
    
    // ==================== World Generation ====================
    
    /** Area of influence for taint seeds (in blocks) */
    public static int taintSpreadArea = 32;
    
    /** Taint spread rate (percentage, 0-100) */
    public static float taintSpreadRate = 5.0f;
    
    /** Enable/disable world generation features */
    public static boolean generateOre = true;
    public static boolean generateCrystals = true;
    public static boolean generateTrees = true;
    public static boolean generateStructures = true;
    public static boolean generateAura = true;
    
    // ==================== Gameplay ====================
    
    /** Wuss mode - disables dangerous features like taint spread */
    public static boolean wussMode = false;
    
    /** Vis regeneration rate multiplier */
    public static float visRegenRate = 1.0f;
    
    /** Flux dissipation rate multiplier */
    public static float fluxDissipationRate = 1.0f;
    
    /** Maximum warp before permanent side effects */
    public static int maxPermWarp = 100;
    
    /** Warp event frequency multiplier */
    public static float warpEventFrequency = 1.0f;
    
    // ==================== Research ====================
    
    /** Research difficulty multiplier */
    public static float researchDifficulty = 1.0f;
    
    /** Enable hard-mode research (requires more scanning) */
    public static boolean hardResearch = false;
    
    // ==================== Golems ====================

    /** Maximum golems per player */
    public static int maxGolemsPerPlayer = 64;

    /** Golem task range multiplier */
    public static float golemRangeMultiplier = 1.0f;

    // ==================== Graphics ====================

    public static class CONFIG_GRAPHICS {
        /** Show golem emotes */
        public static boolean showGolemEmotes = true;
    }
    
    // ==================== Aura ====================
    
    /** Base vis per chunk */
    public static int baseVisPerChunk = 500;
    
    /** Flux rift spawn threshold */
    public static float fluxRiftThreshold = 0.75f;
    
    // ==================== Performance ====================
    
    /** Ticks between aura calculations */
    public static int auraTickRate = 20;
    
    /** Maximum entities affected by area effects */
    public static int maxAreaEffectEntities = 32;
    
    // ==================== Dimension Settings ====================
    
    /** Dimensions where Thaumcraft features are disabled */
    public static String[] dimensionBlacklist = {};
    
    // ==================== Methods ====================
    
    /**
     * Check if a dimension allows Thaumcraft features.
     */
    public static boolean isDimensionAllowed(String dimensionName) {
        for (String blacklisted : dimensionBlacklist) {
            if (blacklisted.equals(dimensionName)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Load configuration from file.
     * TODO: Implement Forge config loading
     */
    public static void load() {
        // Placeholder - will be replaced with Forge config
    }
    
    /**
     * Save configuration to file.
     * TODO: Implement Forge config saving
     */
    public static void save() {
        // Placeholder - will be replaced with Forge config
    }
}




