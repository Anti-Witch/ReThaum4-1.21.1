package thaumcraft.api.casters;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Represents a configurable setting on a focus node.
 * Players can adjust these settings in the focus crafting GUI.
 */
public class NodeSetting {
    
    int value;
    public String key;
    String description;
    INodeSettingType type;
    String research;
    
    public NodeSetting(String key, String description, INodeSettingType setting, String research) {
        this.key = key;
        this.type = setting;
        this.value = setting.getDefault();
        this.description = description;
        this.research = research;
    }
    
    public NodeSetting(String key, String description, INodeSettingType setting) {
        this(key, description, setting, null);
    }
    
    public int getValue() {
        return type.getValue(value);
    }
    
    public void setValue(int trueValue) {
        int lastValue = -1;
        value = 0;
        while (getValue() != trueValue && lastValue != value) {
            lastValue = value;
            increment();
        }
    }
    
    public int getRawValue() {
        return value;
    }
    
    public void setRawValue(int raw) {
        this.value = type.clamp(raw);
    }
    
    /**
     * This setting will only be visible if this research is unlocked.
     * If not, the default will be used.
     */
    public String getResearch() {
        return research;
    }

    public String getValueText() {
        return Component.translatable(type.getValueText(value)).getString();
    }

    public void increment() {
        value++;
        value = getType().clamp(value);
    }
    
    public void decrement() {
        value--;
        value = getType().clamp(value);
    }

    public INodeSettingType getType() {
        return type;
    }

    public String getLocalizedName() {
        return Component.translatable(description).getString();
    }

    /**
     * Interface for different types of node settings.
     */
    public interface INodeSettingType {
        int getDefault();
        int clamp(int i);
        int getValue(int value);
        String getValueText(int value);
    }
    
    /**
     * A setting that chooses from a list of predefined integer values.
     */
    public static class NodeSettingIntList implements INodeSettingType {
        int[] values;
        String[] descriptions;

        public NodeSettingIntList(int[] values, String[] descriptions) {
            this.values = values;
            this.descriptions = descriptions;
        }
        
        @Override
        public int getDefault() {
            return 0;
        }
        
        @Override
        public int clamp(int old) {
            return Mth.clamp(old, 0, values.length - 1);
        }

        @Override
        public int getValue(int value) {
            return values[clamp(value)];
        }

        @Override
        public String getValueText(int value) {
            return descriptions[clamp(value)];
        }
    }
    
    /**
     * A setting that ranges between a min and max integer value.
     */
    public static class NodeSettingIntRange implements INodeSettingType {
        int min, max;

        public NodeSettingIntRange(int min, int max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public int getDefault() {
            return min;
        }
        
        @Override
        public int clamp(int old) {
            return Mth.clamp(old, min, max);
        }

        @Override
        public int getValue(int value) {
            return clamp(value);
        }

        @Override
        public String getValueText(int value) {
            return String.valueOf(getValue(value));
        }
    }
}




