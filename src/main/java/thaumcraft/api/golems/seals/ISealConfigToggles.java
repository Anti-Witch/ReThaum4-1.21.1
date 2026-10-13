package thaumcraft.api.golems.seals;

/**
 * Interface for seals that support toggle options.
 * Provides configurable boolean settings.
 */
public interface ISealConfigToggles {

    /**
     * @return Array of toggleable options
     */
    SealToggle[] getToggles();

    /**
     * Set a toggle value
     */
    void setToggle(int index, boolean value);

    /**
     * Represents a single toggle option
     */
    class SealToggle {
        public boolean value;
        public String key;
        public String name;

        public SealToggle(boolean value, String key, String name) {
            this.value = value;
            this.key = key;
            this.name = name;
        }

        public boolean getValue() {
            return value;
        }

        public void setValue(boolean value) {
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public String getName() {
            return name;
        }
    }
}




