package thaumcraft.api.casters;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A FocusPackage contains the configuration of a focus's spell effect.
 * It holds a list of focus elements (medium, modifiers, effect) that
 * define how the spell behaves.
 */
public class FocusPackage {
    
    /** The elements that make up this focus package */
    public List<IFocusElement> nodes = new ArrayList<>();
    
    /** The world this package is executing in */
    public Level world;
    
    /** Current processing index */
    public int index = 0;
    
    /** Cached complexity value */
    private int complexity = -1;
    
    /** Power multiplier for this package */
    private float power = 1.0f;
    
    /** Unique ID for tracking this spell cast */
    private long uniqueID;
    
    /** UUID of the entity that cast this spell */
    private UUID casterUUID;
    
    public FocusPackage() {
        this.uniqueID = System.currentTimeMillis();
    }
    
    /**
     * Calculate the complexity (vis cost) of this focus package.
     * Higher complexity = higher vis cost.
     */
    public int getComplexity() {
        if (complexity < 0) {
            complexity = 0;
            for (IFocusElement element : nodes) {
                // Base complexity for each element
                complexity += 5;
                // TODO: Add element-specific complexity calculations
            }
        }
        return Math.max(1, complexity);
    }
    
    /**
     * Get a sorting helper string for comparing focus configurations.
     */
    public int getSortingHelper() {
        StringBuilder sb = new StringBuilder();
        for (IFocusElement element : nodes) {
            sb.append(element.getKey()).append(";");
        }
        return sb.toString().hashCode();
    }
    
    /**
     * Serialize this package to NBT for storage.
     */
    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        ListTag nodeList = new ListTag();
        
        for (IFocusElement element : nodes) {
            CompoundTag nodeTag = new CompoundTag();
            nodeTag.putString("key", element.getKey());
            nodeTag.putString("type", element.getType().name());
            // TODO: Serialize element-specific settings
            nodeList.add(nodeTag);
        }
        
        tag.put("nodes", nodeList);
        tag.putInt("complexity", getComplexity());
        return tag;
    }
    
    /**
     * Deserialize this package from NBT.
     */
    public void deserialize(CompoundTag tag) {
        nodes.clear();
        complexity = tag.getInt("complexity");
        
        if (tag.contains("nodes", Tag.TAG_LIST)) {
            ListTag nodeList = tag.getList("nodes", Tag.TAG_COMPOUND);
            for (int i = 0; i < nodeList.size(); i++) {
                CompoundTag nodeTag = nodeList.getCompound(i);
                String key = nodeTag.getString("key");
                
                FocusNode node = FocusEngine.createFocusNode(key);
                if (node != null) {
                    node.deserialize(nodeTag);
                    node.setPackage(this);
                    nodes.add(node);
                }
            }
        }
    }
    
    /**
     * Get all effect elements in this package.
     */
    public List<IFocusElement> getEffects() {
        List<IFocusElement> effects = new ArrayList<>();
        for (IFocusElement element : nodes) {
            if (element.getType() == IFocusElement.EnumUnitType.EFFECT) {
                effects.add(element);
            }
        }
        return effects;
    }
    
    /**
     * Create a simple focus package with basic settings.
     * This is a helper for creating default focus configurations.
     */
    public static FocusPackage createSimple(String effectKey) {
        FocusPackage pack = new FocusPackage();
        // TODO: Create actual focus elements
        // For now, return an empty package with set complexity
        pack.complexity = 10;
        return pack;
    }
    
    // ==================== Node Management ====================
    
    /**
     * Add a focus node to this package.
     * <p>
     * The node is bound to this package (TC4 FocusNode.setPackage contract):
     * nodes look up their package via {@link FocusNode#getPackage()} /
     * {@link FocusNode#getRemainingPackage()} when the spell executes, so a
     * node without a package can never deliver its effect.
     */
    public void addNode(FocusNode node) {
        if (node != null) {
            node.setPackage(this);
            nodes.add(node);
        }
    }
    
    /**
     * Set the complexity directly (used during crafting).
     */
    public void setComplexity(int complexity) {
        this.complexity = complexity;
    }
    
    // ==================== Power ====================
    
    public float getPower() {
        return power;
    }
    
    public void setPower(float power) {
        this.power = power;
    }
    
    public void multiplyPower(float multiplier) {
        this.power *= multiplier;
    }
    
    // ==================== Unique ID ====================
    
    public long getUniqueID() {
        return uniqueID;
    }
    
    public void setUniqueID(long id) {
        this.uniqueID = id;
    }
    
    // ==================== Caster ====================
    
    public UUID getCasterUUID() {
        return casterUUID;
    }
    
    public void setCasterUUID(UUID uuid) {
        this.casterUUID = uuid;
    }
    
    /**
     * Get the caster entity from the world.
     * Returns null if the caster cannot be found.
     */
    public LivingEntity getCaster() {
        if (casterUUID == null || world == null) {
            return null;
        }
        if (world instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(casterUUID);
            if (entity instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }
    
    /**
     * Set the caster for this focus package.
     */
    public void setCaster(LivingEntity caster) {
        if (caster != null) {
            this.casterUUID = caster.getUUID();
        }
    }
}




