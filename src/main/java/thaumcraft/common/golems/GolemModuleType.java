package thaumcraft.common.golems;

/**
 * Enum representing the different types of golem modules.
 * Modules give golems specific capabilities/tasks they can perform.
 */
public enum GolemModuleType {
    VISION("vision", 0x4A90D9),
    AGGRESSION("aggression", 0xFF4444),
    HARVEST("harvest", 0x8FBC8F),
    LUMBER("lumber", 0x8B4513),
    BUTCHER("butcher", 0xCD5C5C),
    GUARD("guard", 0xFFD700),
    USE("use", 0xDA70D6),
    PROVIDE("provide", 0x32CD32),
    STOCK("stock", 0xFFA500),
    EMPTY("empty", 0x808080),
    FILL("fill", 0x1E90FF),
    BREAKER("breaker", 0xA0522D),
    PICKUP("pickup", 0xFF69B4),
    PLACE("place", 0x9370DB),
    FLUID("fluid", 0x00BFFF),
    REDSTONE("redstone", 0xFF0000);

    private final String name;
    private final int color;

    GolemModuleType(String name, int color) {
        this.name = name;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public int getColor() {
        return color;
    }

    public static GolemModuleType byName(String name) {
        for (GolemModuleType type : values()) {
            if (type.name.equalsIgnoreCase(name)) return type;
        }
        return VISION;
    }
}