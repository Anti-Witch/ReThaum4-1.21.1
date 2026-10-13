package thaumcraft.api.casters;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.HitResult;
import thaumcraft.api.aspects.Aspect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Base class for all focus nodes (effects, mediums, modifiers).
 * Focus nodes are the building blocks of spell foci.
 */
public abstract class FocusNode implements IFocusElement {
    
    public FocusNode() {
        super();
        initialize();
    }
    
    public String getUnlocalizedName() {
        return getKey() + ".name";
    }

    public String getUnlocalizedText() {
        return getKey() + ".text";
    }

    /**
     * How complex this node is - affects total focus complexity.
     */
    public abstract int getComplexity();
    
    /**
     * The aspect associated with this node.
     */
    public abstract Aspect getAspect();
    
    /**
     * What types of input this node requires from parent nodes.
     */
    public abstract EnumSupplyType[] mustBeSupplied();
    
    /**
     * What types of output this node provides to child nodes.
     */
    public abstract EnumSupplyType[] willSupply();
    
    public boolean canSupply(EnumSupplyType type) {
        if (willSupply() != null) {
            for (EnumSupplyType st : willSupply()) {
                if (st == type) return true;
            }
        }
        return false;
    }
    
    /**
     * Types of data that can be supplied between nodes.
     */
    public enum EnumSupplyType {
        /** Target hit results */
        TARGET,
        /** Projectile trajectories */
        TRAJECTORY
    }
    
    /**
     * Supply target hit results (for effects that need targets).
     */
    public HitResult[] supplyTargets() {
        return null;
    }
    
    /**
     * Supply trajectories (for delivery mediums).
     */
    public Trajectory[] supplyTrajectories() {
        return null;
    }
    
    // ==================== Package Context ====================
    
    FocusPackage pack;
    
    public void setPackage(FocusPackage pack) {
        this.pack = pack;
    }
    
    public FocusPackage getPackage() {
        return pack;
    }
    
    /**
     * Get a new package containing only the nodes after this one.
     */
    public FocusPackage getRemainingPackage() {
        FocusPackage p = getPackage();
        if (p == null || p.nodes == null) return null;
        
        List<IFocusElement> remaining = p.nodes.subList(p.index + 1, p.nodes.size());
        List<IFocusElement> newList = Collections.synchronizedList(new ArrayList<>());
        newList.addAll(remaining);
        
        FocusPackage newPack = new FocusPackage();
        newPack.setUniqueID(p.getUniqueID());
        newPack.world = p.world;
        newPack.multiplyPower(p.getPower());
        newPack.nodes = newList;
        newPack.setCasterUUID(p.getCasterUUID());
        // Bind the carried nodes to the new package: effects read their world
        // and caster from getPackage() when they execute (TC4 contract).
        for (IFocusElement element : newList) {
            if (element instanceof FocusNode node) {
                node.setPackage(newPack);
            }
        }
        
        return newList.isEmpty() ? null : newPack;
    }
    
    // ==================== Parent Node ====================
    
    private FocusNode parent;
    
    public FocusNode getParent() {
        return parent;
    }

    public void setParent(FocusNode parent) {
        this.parent = parent;
    }
    
    // ==================== Settings ====================
    
    HashMap<String, NodeSetting> settings = new HashMap<>();
    
    public Set<String> getSettingList() {
        return settings.keySet();
    }
    
    public NodeSetting getSetting(String key) {
        return settings.get(key);
    }
    
    public int getSettingValue(String key) {
        return settings.containsKey(key) ? settings.get(key).getValue() : 0;
    }
    
    /**
     * Override to create custom settings for this node.
     */
    public NodeSetting[] createSettings() {
        return null;
    }
    
    public void initialize() {
        NodeSetting[] set = createSettings();
        if (set != null) {
            for (NodeSetting setting : set) {
                settings.put(setting.key, setting);
            }
        }
    }
    
    /**
     * Power multiplier applied by this node.
     */
    public float getPowerMultiplier() {
        return 1;
    }
    
    /**
     * Whether this node is exclusive (cannot be combined with similar nodes).
     */
    public boolean isExclusive() {
        return false;
    }
    
    public void serialize(CompoundTag tag) {
        // Serialize settings
        CompoundTag settingsTag = new CompoundTag();
        for (NodeSetting setting : settings.values()) {
            settingsTag.putInt(setting.key, setting.getValue());
        }
        tag.put("settings", settingsTag);
    }
    
    public void deserialize(CompoundTag tag) {
        // Deserialize settings
        if (tag.contains("settings")) {
            CompoundTag settingsTag = tag.getCompound("settings");
            for (String key : settings.keySet()) {
                if (settingsTag.contains(key)) {
                    settings.get(key).setValue(settingsTag.getInt(key));
                }
            }
        }
    }
}




