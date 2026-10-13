package thaumcraft.api.wands;

import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * FocusPackage - Stores the configuration of a wand focus (upgrades, complexity, etc.).
 * Ported from TC4 FocusPackage.
 */
public class FocusPackage {

    public UUID uuid = UUID.randomUUID();
    public int complexity = 0;
    private final Map<FocusUpgradeType, Integer> upgrades = new HashMap<>();

    public FocusPackage() {}

    public FocusPackage(FocusPackage other) {
        this.uuid = other.uuid;
        this.complexity = other.complexity;
        this.upgrades.putAll(other.upgrades);
    }

    public void addUpgrade(FocusUpgradeType type, int level) {
        upgrades.put(type, level);
        recalculateComplexity();
    }

    public void removeUpgrade(FocusUpgradeType type) {
        upgrades.remove(type);
        recalculateComplexity();
    }

    public int getUpgradeLevel(FocusUpgradeType type) {
        return upgrades.getOrDefault(type, 0);
    }

    public boolean hasUpgrade(FocusUpgradeType type) {
        return upgrades.containsKey(type) && upgrades.get(type) > 0;
    }

    public void recalculateComplexity() {
        int comp = 0;
        for (Map.Entry<FocusUpgradeType, Integer> entry : upgrades.entrySet()) {
            comp += entry.getValue() * entry.getKey().getCost().visSize();
        }
        this.complexity = comp;
    }

    public int getComplexity() {
        return complexity;
    }

    public java.util.Collection<FocusUpgradeType> getUpgrades() {
        return upgrades.keySet();
    }

    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putString("uuid", uuid.toString());
        tag.putInt("complexity", complexity);

        CompoundTag upgradesTag = new CompoundTag();
        for (Map.Entry<FocusUpgradeType, Integer> entry : upgrades.entrySet()) {
            upgradesTag.putInt(entry.getKey().getId() + "", entry.getValue());
        }
        tag.put("upgrades", upgradesTag);
        return tag;
    }

    public void deserialize(CompoundTag tag) {
        uuid = UUID.fromString(tag.getString("uuid"));
        complexity = tag.getInt("complexity");
        upgrades.clear();

        CompoundTag upgradesTag = tag.getCompound("upgrades");
        for (String key : upgradesTag.getAllKeys()) {
            int id = Integer.parseInt(key);
            int level = tag.getInt(key);
            // Find upgrade type by id (would need a registry)
        }
    }

    public int getSortingHelper() {
        return uuid.hashCode();
    }
}
