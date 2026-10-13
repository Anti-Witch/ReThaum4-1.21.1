package thaumcraft.api.golems.seals;

/**
 * ISealGui - Interface for seals that have configurable GUI options.
 * 
 * Seals implementing this interface can specify which configuration
 * categories should appear in their GUI when opened with the golem bell.
 * 
 * GUI Categories:
 * - CAT_PRIORITY (0): Priority and color settings
 * - CAT_FILTER (1): Item filter configuration
 * - CAT_AREA (2): Area size configuration
 * - CAT_TOGGLES (3): Toggle options specific to the seal
 * - CAT_TAGS (4): Required/forbidden golem traits display
 */
public interface ISealGui {
    
    /** Priority and color settings category */
    int CAT_PRIORITY = 0;
    
    /** Item filter configuration category */
    int CAT_FILTER = 1;
    
    /** Area size configuration category */
    int CAT_AREA = 2;
    
    /** Seal-specific toggle options category */
    int CAT_TOGGLES = 3;
    
    /** Required/forbidden golem traits display category */
    int CAT_TAGS = 4;
    
    /**
     * Get the GUI categories that should be available for this seal.
     * 
     * @return Array of category IDs (CAT_* constants)
     */
    int[] getGuiCategories();
}




