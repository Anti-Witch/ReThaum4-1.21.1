package thaumcraft.api.nodes;

/**
 * NodeType - The 6 types of aura nodes in Thaumcraft.
 *
 * NORMAL    - Standard node, regenerates vis normally
 * UNSTABLE  - Randomly releases vis as aspect orbs, can stabilize
 * DARK      - Converts biome to Eerie, spawns Eldritch mobs
 * TAINTED   - Spreads Taint biome and fibres
 * HUNGRY    - Consumes nearby blocks/entities, grows from eating
 * PURE      - Cleanses Taint, converts biome to Magical Forest
 */
public enum NodeType {
    NORMAL,
    UNSTABLE,
    DARK,
    TAINTED,
    HUNGRY,
    PURE
}