package thaumcraft.api.crafting;

/**
 * Represents a part in a multiblock crafting blueprint.
 * Used by DustTriggerMultiblock to define the structure.
 * 
 * Source: The block/material/state that must be present
 * Target: The block/item to transform into (null for air)
 * 
 * @author Azanor
 * Ported to 1.20.1
 */
public class Part {
    
    /** Source material - can be Block, BlockState, Material, or ItemStack */
    private Object source;
    
    /** Target result - Block or ItemStack (null = air) */
    private Object target;
    
    /** If true, use opposite facing direction */
    private boolean opp;
    
    /** Priority for transformation order (lower = earlier) */
    private int priority;
    
    /** If true, apply the player's facing to the target block */
    private boolean applyPlayerFacing;
    
    public Part(Object source, Object target, boolean opp, int priority) {
        this.source = source;
        this.target = target;
        this.opp = opp;
        this.priority = priority;
    }
    
    public Part(Object source, Object target, boolean opp) {
        this(source, target, opp, 50);
    }
    
    public Part(Object source, Object target) {
        this(source, target, false, 50);
    }
    
    // === Getters and Setters ===
    
    public Object getSource() {
        return source;
    }
    
    public void setSource(Object source) {
        this.source = source;
    }
    
    public Object getTarget() {
        return target;
    }
    
    public void setTarget(Object target) {
        this.target = target;
    }
    
    public boolean isOpp() {
        return opp;
    }
    
    public void setOpp(boolean opp) {
        this.opp = opp;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public void setPriority(int priority) {
        this.priority = priority;
    }
    
    public boolean getApplyPlayerFacing() {
        return applyPlayerFacing;
    }
    
    public Part setApplyPlayerFacing(boolean applyFacing) {
        this.applyPlayerFacing = applyFacing;
        return this;
    }
}




